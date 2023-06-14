#!/bin/bash

STARTTIME=$(date +%s)
echo
echo "------------------------------------------Copying input folder----------------------------------------------------------"
echo

#performercontainer-taskinput.sh ${experimentperformerpath,,} ${evaluatorname,,} ${experimentname,,} ${performername,,} ${tasktype,,} ${evaluationdataset}
echo "Performer Path - $1"
echo "evaluator - $2"
echo "experiment - $3"
echo "performer - $4"
tasktype=$5
tasktype=${tasktype,,}
echo "Task type - $tasktype"
evaluationdataset=$6
evaluationdataset=${evaluationdataset,,}
echo "Evaluation dataset - $evaluationdataset"
s3bucket="kairos-experiment-data/$evaluationdataset/experiment-input/$tasktype"
echo "Source S3 bucket - $s3bucket"

# create input folder
echo "Deleting $1/input folder..."
rm -rf $1/input
echo "Done"; echo
mkdir -pv $1/input
mkdir -pv $1/input/$tasktype

cd $1/input/$tasktype; echo; pwd; echo;
aws s3 cp s3://$s3bucket . --recursive


ENDTIME=$(date +%s)

echo ""
echo ""
echo ""
secs=$((ENDTIME-STARTTIME))
echo "It took $secs seconds to complete this task..."
echo ""
hrs=$(($secs/3600))
min=$(($secs/60))
secsleft=$(($secs-$min*60))

printf '%dh:%dm:%ds\n' $hrs $min $(($secsleft))
echo ""
echo ""

