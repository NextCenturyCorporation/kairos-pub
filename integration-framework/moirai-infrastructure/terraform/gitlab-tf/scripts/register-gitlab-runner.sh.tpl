#!/usr/bin/env bash

until curl -o runner-token -sf http://${GITLAB-IP}:8000/token
do
  echo "Waiting for GitLab to serve registration token"
  sleep 10
done

if grep "name = default-runner" /etc/gitlab-runner/config.toml
  then
    echo "Runner already added. Grabbing token so server doesn't hang."
  else
    REGISTRATION_TOKEN=$(cat runner-token)

    # export CI_SERVER_URL=http://${GITLAB-IP}
    # export RUNNER_NAME=default-runner
    # export REGISTRATION_TOKEN


    until gitlab-runner register --non-interactive --url "http://gitlab.kairos.nextcentury.com" --registration-token $REGISTRATION_TOKEN --description "default-runner" --executor "shell"
    # until gitlab-runner register --non-interactive /
    #     --url "http://gitlab.kairos.nextcentury.com" /
    #     --registration-token $REGISTRATION_TOKEN /
    #     --description "docker-runner" /
    #     --executor "docker" /
    #     --docker-image lawerencem/gradle7-jdk11-node16
    do
      echo "Waiting until successful registration of token"
      sleep 60
    done
fi

rm runner-token
