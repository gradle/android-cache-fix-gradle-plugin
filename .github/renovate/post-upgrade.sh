#!/usr/bin/env bash
#
# Renovate post-upgrade hook for AGP version bumps.
#
# WHEN THIS RUNS
# --------------
# Invoked by the shared Renovate agent (gradle/renovate-agent) after it writes
# a Renovate-driven change. It is scoped via packageRules in .github/renovate.json5
# to only fire when `com.android.tools.build:gradle` is bumped inside
# gradle.properties (i.e. the `org.gradle.android.latestKnownAgpVersion` property).
#
# The scoping matters: there is a separate Renovate custom-regex manager that
# also bumps AGP version *keys* inside src/test/resources/versions.json for
# patch releases. This hook must NOT run for those, or it would recursively
# scaffold entries for already-known minors. Keep the packageRule's
# matchFileNames pinned to gradle.properties only.
#
# WHAT IT DOES
# ------------
# Two cases, determined by comparing the new AGP <major>.<minor> to the highest
# <major>.<minor> already tracked in versions.json:
#
#   1. SAME minor (e.g. 9.3.0-alpha01 -> 9.3.0-alpha02, or 9.3.0-beta01 -> 9.3.0):
#      Rewrite the existing key in versions.json in place. Renovate's regex
#      manager on versions.json would normally do this itself, but we keep the
#      major/minor bump disabled there (see packageRule in renovate.json5), so
#      we have to handle the rename ourselves when the leading minor advances
#      through prereleases.
#
#   2. NEW minor or major (e.g. 9.2.0-alpha07 -> 9.3.0-alpha01, or 9.x -> 10.0):
#      - Prepend a new top-level entry to versions.json keyed by the new AGP
#        version, copying the Gradle versions array from the previous highest
#        minor as a sensible default. A maintainer may still need to widen the
#        Gradle versions list if the new AGP requires a newer Gradle baseline.
#      - Copy src/test/resources/expectedOutcomes/<prev>_outcomes.json to a new
#        file named <new>_outcomes.json so the integration-test task for the
#        new version has a starting point. Diffs surfaced by failing tests are
#        how the maintainer tunes the file.
#
# The file naming convention for outcomes files uses <major>.<minor> only
# (e.g. 9.3_outcomes.json, never 9.3.0_outcomes.json) — the test harness
# registers a Test task per file and maps it back to the AGP minor.
#
# REQUIREMENTS
# ------------
# - jq (preinstalled on GitHub-hosted runners; the renovate-agent image also
#   has it).
# - The allowlist entry `^bash .github/renovate/post-upgrade.sh$` in
#   gradle/renovate-agent's renovate_agent_config.js. Do not rename or move
#   this script without updating that allowlist.

set -euo pipefail

GRADLE_PROPERTIES="gradle.properties"
VERSIONS_JSON="src/test/resources/versions.json"
OUTCOMES_DIR="src/test/resources/expectedOutcomes"

# Extract the full AGP version string Renovate just wrote, e.g. "9.3.0-alpha01".
new_agp=$(sed -n 's/^org\.gradle\.android\.latestKnownAgpVersion=\(.*\)$/\1/p' "$GRADLE_PROPERTIES")
if [[ -z "$new_agp" ]]; then
  echo "post-upgrade: could not read latestKnownAgpVersion from $GRADLE_PROPERTIES" >&2
  exit 1
fi

# Reduce to <major>.<minor>. We deliberately ignore patch + prerelease suffixes
# here because outcomes files and the "latest known minor" concept are both
# keyed on minor granularity.
minor_of() {
  # Strips the third dot-segment and any -alpha/-beta/-rc suffix.
  # 9.3.0-alpha01 -> 9.3
  # 10.0.1        -> 10.0
  echo "$1" | sed -E 's/^([0-9]+\.[0-9]+).*/\1/'
}

new_mm=$(minor_of "$new_agp")

# Find the highest minor currently tracked in versions.json. We want the full
# key (e.g. "9.2.0-alpha07") because we need it both to replace in place
# (same-minor case) and to look up its Gradle versions array (new-minor case).
#
# `sort -V` handles the version-aware ordering — "9.2.0-alpha07" sorts below
# "9.2.0" and above "9.1.1", and "10.x" beats "9.x". Plain numeric sort on
# the major.minor pair is not enough: it chokes on prerelease suffixes and
# GNU/BSD sort disagree on how to tokenize dot-separated strings.
prev_key=$(jq -r '.testedVersions | keys_unsorted[]' "$VERSIONS_JSON" \
  | sort -V \
  | tail -n1)

if [[ -z "$prev_key" ]]; then
  echo "post-upgrade: no existing keys found in $VERSIONS_JSON" >&2
  exit 1
fi

prev_mm=$(minor_of "$prev_key")

# Case 1: same <major>.<minor> — just rename the key. The Gradle versions
# array stays untouched. jq's `with_entries` preserves key ordering, which
# matters because versions.json is human-read and kept sorted newest-first.
if [[ "$new_mm" == "$prev_mm" ]]; then
  # Nothing to do if the key already matches (Renovate may have already
  # written it through a different path; idempotency keeps this safe to
  # re-run).
  if [[ "$prev_key" == "$new_agp" ]]; then
    echo "post-upgrade: $VERSIONS_JSON already has key $new_agp; nothing to do"
    exit 0
  fi

  tmp=$(mktemp)
  jq --arg old "$prev_key" --arg new "$new_agp" '
    .testedVersions |= with_entries(
      if .key == $old then .key = $new else . end
    )
  ' "$VERSIONS_JSON" > "$tmp"
  mv "$tmp" "$VERSIONS_JSON"
  echo "post-upgrade: renamed $prev_key -> $new_agp in $VERSIONS_JSON"
  exit 0
fi

# Case 2: new minor (or major). Prepend a new entry with the previous minor's
# Gradle versions array as a default, and scaffold the outcomes file.
#
# We reconstruct the object with the new key first so that maintainers
# scanning the file top-down see the newest AGP line first (matching the
# existing convention).
tmp=$(mktemp)
jq --arg new "$new_agp" --arg prev "$prev_key" '
  .testedVersions = (
    { ($new): .testedVersions[$prev] } + .testedVersions
  )
' "$VERSIONS_JSON" > "$tmp"
mv "$tmp" "$VERSIONS_JSON"
echo "post-upgrade: added $new_agp to $VERSIONS_JSON (Gradle versions copied from $prev_key)"

# Scaffold the outcomes file. If the previous minor's file is missing we bail
# loudly — it almost certainly means someone deleted it or the previous
# scaffold run never ran, and silently writing an empty file would make the
# test task pass vacuously.
prev_outcomes="$OUTCOMES_DIR/${prev_mm}_outcomes.json"
new_outcomes="$OUTCOMES_DIR/${new_mm}_outcomes.json"

if [[ ! -f "$prev_outcomes" ]]; then
  echo "post-upgrade: expected previous outcomes file $prev_outcomes not found" >&2
  exit 1
fi

if [[ -f "$new_outcomes" ]]; then
  echo "post-upgrade: $new_outcomes already exists; leaving as-is"
else
  cp "$prev_outcomes" "$new_outcomes"
  echo "post-upgrade: copied $prev_outcomes -> $new_outcomes"
fi
