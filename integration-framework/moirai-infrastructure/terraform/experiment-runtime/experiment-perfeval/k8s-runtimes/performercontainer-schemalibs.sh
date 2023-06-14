#!/bin/bash

STARTTIME=$(date +%s)

echo
echo "-------------------------------------------Copying schema libraries--------------------------------------------------"
echo
# /scripts/performercontainer-schemalibs.sh {{experimentperformerpath}} {{evaluator}} {{experiment}} {{performer}} {{evaluationdataset}} {{schemalibraries}}
echo "Performer Path - $1"
echo "evaluator - $2"
echo "experiment - $3"
echo "performer - $4"
evaluationdataset=$5
echo "Evaluation dataset - $evaluationdataset"
echo "schema libraries - "
for var in "$@"; do
	if [[ $var == schemalib:* ]]; then
		echo $var
	fi
done
echo

# create schemas folder
echo "Deleting $1/schemas folder..."
rm -rf $1/schemas
echo "Done"
echo
mkdir -pv $1/schemas
cd $1/schemas
echo
pwd
echo
s3bucket="kairos-experiment-data/$evaluationdataset/experiment-input/ta_1"
echo "Source S3 bucket - $s3bucket"
echo

# loop thru the params to get schemalibs to be copied
for var in "$@"; do
	if [[ $var == schemalib:* ]]; then
		schemalib=$(echo $var | cut -d ":" -f2)
		schemalib=${schemalib,,}
		schemalib=$(echo "$schemalib" | tr -d '"')

		mkdir -pv $1/schemas/$schemalib
		cd $1/schemas/$schemalib

		# for this schemalib, do the copy
		aws s3 cp s3://$s3bucket/$schemalib . --recursive
	fi
done

echo

ENDTIME=$(date +%s)

echo ""
echo ""
echo ""
secs=$((ENDTIME - STARTTIME))
echo "It took $secs seconds to complete this task..."
echo ""
hrs=$(($secs / 3600))
min=$(($secs / 60))
secsleft=$(($secs - $min * 60))

printf '%dh:%dm:%ds\n' $hrs $min $(($secsleft))
echo ""
echo ""
