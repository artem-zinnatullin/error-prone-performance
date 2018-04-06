#!/bin/bash
set -e

# You can run it from any directory.
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

pushd "$DIR/../docker/oracle-jdk9"
echo "Building Docker image for Oracle JDK 9..."
docker build -t error-prone-performance-oracle-jdk9:latest .
popd

echo "Starting Docker container for the benchmark (Oracle JDK 9)..."

docker run \
--tty \
--rm \
--volume "$DIR/..:/opt/projects/error-prone-performance" \
--env LOCAL_USER_ID=$(id -u "$USER") \
error-prone-performance-oracle-jdk9:latest \
bash -c \
"echo 'Isolating mounted dir from changes generated during benchmark (can take a few moments on macOS)...' && \
cp -r /opt/projects/error-prone-performance /opt/projects/error-prone-performance-copy && \
cd /opt/projects/error-prone-performance && \
echo 'Starting error-prone benchmark on rxjava project...' && \
/opt/projects/gradle-profiler/build/install/gradle-profiler/bin/gradle-profiler \
--benchmark \
--warmups 5 \
--iterations 10 \
--project-dir /opt/projects/error-prone-performance-copy/rxjava \
--scenario-file /opt/projects/error-prone-performance/scenarios/rxjava-javac-plugin.scenario"
