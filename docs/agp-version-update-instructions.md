# Android Gradle Plugin Version Upgrades

## Patch upgrades

- Merge the Renovate PR once all checks have completed successfully
- If checks fail, review for necessary changes (see [Investigating Failures](#investigating-failures))

## Minor or major upgrades

1. Update the PR from Renovate (that should be simply updating the `org.gradle.android.latestKnownAgpVersion` property in `gradle.properties`)
2. Add the new version to `src/test/resources/versions.json` including the Gradle versions it should be tested with
3. Copy the previous version's `expectedOutcomes` JSON to a new file named as the new version (without the patch number, e.g. `9.0_outcomes.json`, **not** `9.0.0_outcomes.json`). The outcomes files live in `src/test/resources/expectedOutcomes/`.
4. If checks fail, review for necessary changes (see [Investigating Failures](#investigating-failures))
5. Make sure the latest stable, beta, and alpha are present in `versions.json`
6. Merge the PR once all checks have completed successfully

## Investigating failures

Checks run integration tests against every supported AGP version, on each Gradle version supported for that AGP version. Outcomes JSON files are used by these tests to verify a build's tasks' outcomes. A Test task will be registered automatically for each file. If the task for the new version fails, review the differences reported in the stdout. Some may be simple fixes like adjusting for a renamed AGP task. Others, like a new cache miss, may require changes to the actual plugin.
