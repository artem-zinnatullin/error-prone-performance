FROM ubuntu:16.04

# sudo for user switching
# curl for downloads
RUN apt-get update && \
    apt-get --assume-yes --quiet install sudo curl

# Install OpenJDK 9.
RUN apt-get update && \
    apt-get --assume-yes --quiet --option Dpkg::Options::="--force-overwrite" install openjdk-9-jdk && \
    java -version

# Build gradle-profiler
RUN mkdir -p /opt/projects && \
    cd /opt/projects && \
    apt-get --assume-yes --quiet install git && \
    git clone https://github.com/artem-zinnatullin/gradle-profiler.git gradle-profiler && \
    cd gradle-profiler && \
    git checkout d245947f29ee8a22c533785ef49d71ce955737e6 && \
    ./gradlew installDist --stacktrace

# Entrypoint script will allow us run as non-root in the container.
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
