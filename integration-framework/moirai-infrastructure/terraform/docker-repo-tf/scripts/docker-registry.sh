#!/bin/sh

# User auth setup
sudo mkdir /auth/$2
sudo touch /auth/$2/htpasswd
sudo chown ubuntu:ubuntu -R /auth
sudo chmod 550 /auth/$2/htpasswd
sudo htpasswd -cBb /auth/$2/htpasswd admin PASSWORD_TO_REPLACE_THIS_WOULD_BE_A_BAD_PASSWORD
sudo htpasswd -Bb /auth/$2/htpasswd $2 $3

# Main registry run command
docker run -P -d --restart=always --net nginx-network --name $2 \
-v /home/ubuntu/config.yml:/etc/docker/registry/config.yml \
-v /etc/letsencrypt:/etc/letsencrypt \
-v /auth:/auth \
-e "REGISTRY_STORAGE=s3" \
-e "REGISTRY_STORAGE_S3_REGION=us-east-1" \
-e "REGISTRY_STORAGE_S3_BUCKET=$1" \
-e "REGISTRY_STORAGE_S3_ROOTDIRECTORY=/repo/$2" \
-e "REGISTRY_STORAGE_CACHE_BLOBDESCRIPTOR=inmemory" \
-e "REGISTRY_AUTH=htpasswd" \
-e "REGISTRY_AUTH_HTPASSWD_REALM=Registry Realm" \
-e "REGISTRY_AUTH_HTPASSWD_PATH=/auth/$2/htpasswd" \
registry:2
