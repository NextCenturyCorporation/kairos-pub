#!/bin/bash

date; 
echo -e "---------------------------------Executing kairos-msgprocessor-init-script------------------------------------"
echo 
echo 

rm -rfv $1/log
echo Creating performer log folder; echo;
mkdir -pv $1/log; echo;

rm -rfv $1/persist
echo Creating performer persist folder; echo; 
mkdir -pv $1/persist; 

echo Creating submission-results folder; echo;
mkdir -pv $1/submission-results

experiment=$2
# this is cleanup just in case we are in test mode
if [[ ${experiment,,} == "expk" || ${experiment,,} == "expa" ]]
then
        rm -v $1/log/*.*
        rm -v $1/persist/*.*
fi
echo


echo 
echo Done
echo 
echo
echo -e "---------------------------------Executing kairos-msgprocessor-init-script------------------------------------"
