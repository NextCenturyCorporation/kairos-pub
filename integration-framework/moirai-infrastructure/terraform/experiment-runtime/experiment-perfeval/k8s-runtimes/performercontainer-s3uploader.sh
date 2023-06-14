#!/bin/bash

date
echo -e "---------------------------------Executing performercontainer-s3uploader ------------------------------------"
echo
echo

# {{experimentperformerpath}} {{evaluator}} {{experiment}} {{performer}} {{start_time_id}}
echo "Path - $1"
echo "Performer - "$4
echo "STARTID - $5"

s3bucket="kairos-experiment-output"
echo "s3 bucket - "$s3bucket

s3base=$s3bucket"/"$3/$5/$4
echo "s3 basefolder - "$s3base

s3logfolder=$s3base"/log"
echo "S3 log folder - "$s3logfolder

s3persistfolder=$s3base"/persist"
echo "S3 persist folder - "$s3persistfolder

enclave=$2"-"$3"-enclave"
echo "Collecting logs for $enclave"
allpods=($(kubectl get pods -n $enclave --no-headers | awk '{print $1}'))
echo $allpods
for i in "${allpods[@]}"; do
        #echo $i
        #if [[ $i == "NAME" || $i == *"erroregress"* || $i == *"ingest"* || $i == *"msgprocessor"* || $i == *"s3uploader"* || $i == *"resultegress"*  ]];
        if [[ $i == "NAME" ]]; then
                continue
        else
                echo "Container - "$i
                logfilename=$(echo $i | sed 's~s3uploader.*~s3uploader~')
                logfilename=$logfilename.log

                #get the logs for this pod
                kubectl logs $i -n $enclave >$1/log/$logfilename
                echo "Copied log $1/log/$logfilename"
        fi
done

echo "aws s3 sync $1/log s3://$s3logfolder"
aws s3 sync $1/log s3://$s3logfolder
echo "synced with s3"

echo Uploading persistfolder - from $1/persist to s3://$s3persistfolder
echo
cd $1/persist
pwd
echo
ls -lR
echo
echo "Running cmd - aws s3 sync $1/persist s3://$s3persistfolder --recursive"
aws s3 sync $1/persist s3://$s3persistfolder
echo done
echo

echo All Done
echo
echo
echo -e "---------------------------------Executing performercontainer-s3uploader ------------------------------------"
