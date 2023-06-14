#!/bin/bash

mapfile -t nodearray < <( kubectl get nodes )

for j in "${nodearray[@]:1}"
do
    nodeinfo=$j
    nodename=$(echo $nodeinfo | awk '{ print $1 }')
    echo "Designating framework node - $nodename"
    kubectl label nodes $nodename kairosnodetype=framework
done