#!/bin/bash
set -e

# See https://denibertovic.com/posts/handling-permissions-with-docker-volumes/

if [ -z "$LOCAL_USER_ID" ]; then
    echo "Please pass UID of user that starts Docker container as 'LOCAL_USER_ID' env variable."
    echo "Otherwise files generated in container will have wrong owner UID and would cause problems for host machine."
    exit 1
fi

USER_ID="$LOCAL_USER_ID"

# Add local user.
echo "Starting with UID : $USER_ID"
groupadd --gid "$USER_ID" build_user
useradd --shell /bin/bash --uid "$USER_ID" --gid "$USER_ID" --comment "User for container" --create-home build_user

# Give build_user access to working dir.
chown build_user:build_user /opt/projects

# Print Java/JDK version.
java -version

# Run original docker run command as build_user.
sudo --set-home --preserve-env -u build_user "$@"
