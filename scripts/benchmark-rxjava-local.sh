#!/bin/bash
set -e

# You can run it from any directory.
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

pushd "$DIR/.."

printenv

gradle-profiler \
--benchmark \
--warmups 5 \
--iterations 10 \
--project-dir "$DIR/../rxjava" \
--scenario-file scenarios/rxjava.scenario
