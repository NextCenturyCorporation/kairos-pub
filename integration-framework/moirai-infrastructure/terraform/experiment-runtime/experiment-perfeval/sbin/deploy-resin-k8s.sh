#!/bin/bash

experimentfolder=$1
namespacename=$2
currentDir=$(dirname "$0")

i=0
while read line; do
	manifestarray[$i]="$line"
	((i++))
done < <(ls -1 $experimentfolder/*.yaml)

# Deploy edl first
for j in "${manifestarray[@]}"; do
	if [[ $j == *"edl"* ]]; then
		kubectl apply -f $j -n $namespacename
	fi
done
sleep 15

# Deploy everything thats not edl
for j in "${manifestarray[@]}"; do
	if [[ $j != *"edl"* ]]; then
		kubectl apply -f $j -n $namespacename
	fi
done
