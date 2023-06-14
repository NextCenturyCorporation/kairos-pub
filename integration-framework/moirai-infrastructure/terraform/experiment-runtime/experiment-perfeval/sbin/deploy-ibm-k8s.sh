#!/bin/bash

experimentfolder=$1
namespacename=$2
currentDir=$(dirname "$0")

sbin="$(dirname "${BASH_SOURCE}")"
source $sbin/kairos-functions.sh

kubectl apply -f $experimentfolder/redis-deployment.yaml -n $namespacename
kubectl apply -f $experimentfolder/redis-service.yaml -n $namespacename
waitForNamespace $namespacename

i=0
while read line; do
        manifestarray[$i]="$line"
        ((i++))
done < <(ls -1 $experimentfolder/*.yaml)

for j in "${manifestarray[@]}"; do
        if [[ $j != *"edl"* ]]; then
                kubectl apply -f $j -n $namespacename
        fi
done
