#!/bin/bash
set -e

#####
# NOTE THAT JPROFILER IS A PRODUCT OF ej-technologies.com
# YOU MUST HAVE A PROPER LICENSE FOR JPROFILER TO USE IT
# YOU MUST AGREE WITH JPROFILER TERMS OF USE PROVIDED BY ej-technologies.com
# REPOSITORY MAINTANERS ARE NOT LIABLE FOR YOUR VIOLATIONS OF LICENSE OR TERMS OF USE.
####

# You can run it from any directory.
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

pushd "$DIR/../docker/oracle-jdk9"
echo "Building Docker image for Oracle JDK 9..."
docker build -t error-prone-performance-oracle-jdk9:latest .
popd

echo "Starting Docker container for the profiling (Oracle JDK 9)..."

docker run \
--interactive \
--tty \
--rm \
--volume "$DIR/..:/opt/projects/error-prone-performance" \
--env LOCAL_USER_ID=$(id -u "$USER") \
error-prone-performance-oracle-jdk9:latest \
bash -c \
"echo && echo && echo && \
echo '!! Installing JProfiler (interactive), YOU MUST HAVE PROPER LICENSE TO USE IT !!' && \
echo '!! Installing JProfiler (interactive), YOU MUST ACCEPT JPROFILER TERMS OF USE !!' && \
echo '!! Installing JProfiler (interactive), REPOSITORY MAINTANERS ARE NOT RESPONSIBLE FOR YOUR VIOLATION OF LICENSE OR TERMS OF USE !!' && \
echo && echo && \
echo '!! Please select default installation directory. !!' && \
echo '! JProfiler will be started by gradle-profiler, do not start it after installation.' && \
echo && echo && \
curl --location --fail https://download-keycdn.ej-technologies.com/jprofiler/jprofiler_linux_10_1.sh --output /tmp/jprofiler.sh && \
chmod +x /tmp/jprofiler.sh && \
/tmp/jprofiler.sh && \
echo 'Isolating mounted dir from changes generated during profiling (can take a few moments on macOS)...' && \
cp -r /opt/projects/error-prone-performance /opt/projects/error-prone-performance-copy && \
cd /opt/projects/error-prone-performance && \
echo 'Starting error-prone profiling on rxjava project...' && \
/opt/projects/gradle-profiler/build/install/gradle-profiler/bin/gradle-profiler \
--profile jprofiler \
--jprofiler-home /home/build_user/jprofiler10 \
--jprofiler-config sampling-all \
--warmups 5 \
--iterations 1 \
--project-dir /opt/projects/error-prone-performance-copy/rxjava \
--scenario-file /opt/projects/error-prone-performance/scenarios/rxjava-javac-plugin.scenario"
