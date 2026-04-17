# Android Gradle Plugin Version Upgrades

## Patch upgrades

- Merge the Renovate PR once all checks have completed successfully
- If checks fail, review for necessary changes (see [Investigating Failures](#investigating-failures))

## Minor or major upgrades

1. Review the Renovate PR. A post-upgrade hook will have already added the new version to `src/test/resources/versions.json` and scaffolded a new `expectedOutcomes/<major>.<minor>_outcomes.json` file from the previous minor's outcomes.
2. If the new AGP requires a newer Gradle baseline than the previous minor, widen the Gradle versions array for the new entry in `versions.json` accordingly.
3. If checks fail, review for necessary changes (see [Investigating Failures](#investigating-failures))
4. Make sure the latest stable, beta, and alpha are present in `versions.json`
5. Merge the PR once all checks have completed successfully

## Investigating failures

Checks run integration tests against every supported AGP version, on each Gradle version supported for that AGP version. Outcomes JSON files are used by these tests to verify a build's tasks' outcomes. A Test task will be registered automatically for each file. If the task for the new version fails, review the differences reported in the stdout. Some may be simple fixes like adjusting for a renamed AGP task. Others, like a new cache miss, may require changes to the actual plugin.
