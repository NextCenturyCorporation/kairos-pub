#!/usr/bin/env bash

sudo apt-get -y update
sudo apt-get -y install awscli docker-compose

sudo sysctl -w vm.max_map_count=262144