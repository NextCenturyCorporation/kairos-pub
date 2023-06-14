#!/usr/bin/env bash

docker build -f Kibana.yml -t kibana:elisa .
sudo sysctl -w vm.max_map_count=262144
