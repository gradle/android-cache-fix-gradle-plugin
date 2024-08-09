#!/bin/bash

homeDir=build/HOME

# Set 'task' to the first argument or 'help' if no arguments are provided
task=${1:-help}

# Initialize empty Gradle User Home with settings to run build, including Develocity access keys
echo "Initializing Gradle User Home directory at $homeDir"
rm -rf $homeDir
mkdir -p $homeDir
mkdir -p $homeDir/caches/8.9/
cp ~/.gradle/gradle.properties $homeDir
cp -r ~/.gradle/caches/8.9/generated-gradle-jars $homeDir/caches/8.9/

# Having the key set in the environment is expected - note that remote build cache write is requiered
#export DEVELOCITY_ACCESS_KEY=ge.solutions-team.gradle.com=<access-key>
export GRADLE_CACHE_REMOTE_PUSH=true
export GRADLE_CACHE_REMOTE_PATH="cache/$USER-exp-non-task"
export GRADLE_CACHE_REMOTE_URL="https://ge.solutions-team.gradle.com/cache/$USER-exp-non-task"

echo "------------------------------------------------------------"
echo "Priming build with task '$task' and HOME=$homeDir"
echo "------------------------------------------------------------"
set -x
./gradlew $task -g $homeDir -Dscan.tag.remote-cache-experiment-init --no-configuration-cache -Ddevelocity.deprecation.muteWarnings=true -Dscan.uploadInBackground=false -Dgradle.enterprise.url=https://ge.solutions-team.gradle.com/
set +x

runs='transforms kotlin-dsl'
for run in $runs
do
    # Set args based on cache
    if [ "$run" == 'transforms' ]
    then
        cache='transforms'
        disabledCacheArgs='-Dorg.gradle.internal.transform-caching-disabled'
    elif [ "$run" == 'kotlin-dsl' ]
    then
        cache='kotlin-dsl'
        disabledCacheArgs='-Dorg.gradle.internal.kotlin-script-caching-disabled'
    fi
    for args in "-Dscan.tag.baseline-$run" "-Dscan.tag.disabled-cache-$run $disabledCacheArgs"
    do
        echo "------------------------------------------------------------"
        echo "Test caches/*/$cache removal with $args"
        echo "------------------------------------------------------------"
        set -x
        ./gradlew --stop
        killall -9 java

        # git clean -dfx -e HOME -e cleanup-help.sh
        echo "Removing $cache from $homeDir/caches"
        rm -rf $homeDir/caches/*/$cache
        rm -rf $homeDir/caches/$cache-* # Also remove the transforms for Gradle 8.7

        # Always remove the local build cache, since we are testing connection with remote build cache
        rm -rf $homeDir/caches/build-cache-1

        ./gradlew $task -g $homeDir --no-configuration-cache -Ddevelocity.deprecation.muteWarnings=true -Dscan.uploadInBackground=false -Dgradle.enterprise.url=https://ge.solutions-team.gradle.com/ -Dorg.gradle.dependency.verification=lenient $args

        set +x
        echo ""
    done
done