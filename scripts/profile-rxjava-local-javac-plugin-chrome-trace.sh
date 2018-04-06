#!/bin/bash
set -e

# You can run it from any directory.
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

pushd "$DIR/.."

gradle-profiler \
--profile chrome-trace \
--warmups 5 \
--iterations 1 \
--project-dir "$DIR/../rxjava" \
--scenario-file scenarios/rxjava-javac-plugin.scenario
