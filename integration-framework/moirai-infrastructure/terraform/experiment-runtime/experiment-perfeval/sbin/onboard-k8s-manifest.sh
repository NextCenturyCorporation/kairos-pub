#!/bin/bash

log() {
	echo -e $1 >/dev/tty
}

declare -A gpumap

function processManifestFile() {
	#gpumap=$2

	if [[ $1 == *"docker-compose"* ]]; then
		#echo "***************Skipping docker-compose*******************"
		#echo
		return
	fi

	nodeframeworklabel="framework"
	nodeworkerlabel="worker"
	if [[ $2 != "" ]]; then
		nodeframeworklabel="$2-framework"
		nodeworkerlabel="$2-worker"
	fi

	file=$folder/$1
	echo "Processing file $file.."

	# type
	type=$(yq e '.kind' $file)
	echo "Type: $type"

	# name
	name=$(yq e '.metadata.name' $file)
	echo "Name: $name"
	echo

	#convert to json first
	json=$(yq eval -j $file)

	if [[ $type == "Deployment" ]]; then
		if [[ $name == "redis" ]]; then
			echo "Adding kairosnodetype framework annotation, Skipping Redis deployment manifest.."
			json=$(echo $json | jq '.spec.template.spec += {"nodeSelector"}')
			json=$(echo $json | jq --arg v "$nodeframeworklabel" '.spec.template.spec.nodeSelector += {"kairosnodetype" : $v}')
		elif [[ $name == "edl-db" ]]; then
			echo "Adding kairosnodetype framework annotation, Skipping edl-db mongo deployment manifest.."
			json=$(echo $json | jq '.spec.template.spec += {"nodeSelector"}')
			json=$(echo $json | jq --arg v "$nodeframeworklabel" '.spec.template.spec.nodeSelector += {"kairosnodetype" : $v}')
		else
			if [[ $name == "main" ]]; then
				echo "Annotating main deployment - $folder...."
				json=$(echo $json | jq '.metadata.name = "{{performer}}-main"')
				json=$(echo $json | jq '.metadata.labels["io.kompose.service"] = "{{performer}}-main"')
				json=$(echo $json | jq '.spec.selector.matchLabels["io.kompose.service"] = "{{performer}}-main"')
				json=$(echo $json | jq '.spec.template.metadata.labels["io.kompose.service"] = "{{performer}}-main"')
			fi
			# env
			#yq e '.spec.template.spec.containers.[] | keys' $file

			#yq e '.spec.template.spec.containers.[]' $file

			#yq eval -j ~/temp/jhu/ltf-to-communication-deployment.yaml | yq eval -P > test.yaml

			#echo $json

			# add framework env variables
			json=$(echo $json | jq '.spec.template.spec.containers[].env += [{"name" : "EVALUATOR", "value": "{{evaluator}}"}]')
			json=$(echo $json | jq '.spec.template.spec.containers[].env += [{"name" : "EXPERIMENT", "value": "{{experiment}}"}]')
			json=$(echo $json | jq '.spec.template.spec.containers[].env += [{"name" : "PERFORMER_NAME", "value": "{{performer}}"}]')
			json=$(echo $json | jq '.spec.template.spec.containers[].env += [{"name" : "SUBMISSION_URL", "value": "{{submissionurl}}"}]')
			json=$(echo $json | jq '.spec.template.spec.containers[].env += [{"name" : "KAIROS_LIB", "value": "{{kairosfsmountpath}}"}]')
			json=$(echo $json | jq '.spec.template.spec.containers[].env += [{"name" : "TASK_TYPE", "value": "{{task_type}}"}]')

			#echo $name"-"${gpumap[$name]}

			annotations=$(echo $json | jq '.metadata.annotations')
			isgpu=$(echo $annotations | jq 'has("gpu")')

			if [[ $isgpu == true ]]; then
				json=$(echo $json | jq '.spec.template.spec += {"nodeSelector"}')
				json=$(echo $json | jq --arg v "$nodeworkerlabel" '.spec.template.spec.nodeSelector += {"kairosnodetype" : $v}')
			else
				json=$(echo $json | jq '.spec.template.spec += {"nodeSelector"}')
				json=$(echo $json | jq --arg v "$nodeframeworklabel" '.spec.template.spec.nodeSelector += {"kairosnodetype" : $v}')
			fi

			#logs termination policy
			json=$(echo $json | jq '.spec.template.spec.containers[] += {"terminationMessagePolicy"}')
			json=$(echo $json | jq '.spec.template.spec.containers[].terminationMessagePolicy = "FallbackToLogsOnError"')

			#{{evaluator}}-{{experiment}}-efs-pv

			# volume mounts
			json=$(echo $json | jq '.spec.template.spec.containers[] += {"volumeMounts"}')
			json=$(echo $json | jq '.spec.template.spec.containers[].volumeMounts += [{"mountPath" : "/var/kairosfs", "name": "{{evaluator}}-{{experiment}}-efs-pv"}]')

			#volumes
			json=$(echo $json | jq '.spec.template.spec.volumes += [{"name" : "{{evaluator}}-{{experiment}}-efs-pv", "persistentVolumeClaim"}]')
			json=$(echo $json | jq '.spec.template.spec.volumes[].persistentVolumeClaim += {"claimName" : "kairosfilesystem"}')

			#imagepullpolicy set to always
			json=$(echo $json | jq '.spec.template.spec.containers[].imagePullPolicy = "Always"')

			# add imagepullsecrets
			json=$(echo $json | jq '.spec.template.spec += {"imagePullSecrets"}')
			json=$(echo $json | jq '.spec.template.spec.imagePullSecrets += [{"name" : "{{imagepullsecret}}"}]')
		fi
	elif [[ $type == "Service" ]]; then
		if [[ $name == "redis" ]]; then
			echo "Skipping Redis service manifest.."
			echo
		elif [[ $name == "main" ]]; then
			echo "Annotating main service..."
			json=$(echo $json | jq '.metadata.labels["io.kompose.service"] = "{{performer}}-main"')
			json=$(echo $json | jq '.metadata.name = "{{performer}}-main"')
			json=$(echo $json | jq '.spec.selector["io.kompose.service"] = "{{performer}}-main"')
		elif [[ $name == "edl-db" ]]; then
			echo "Skipping edl-db mongo service manifest.."
			echo
		# else
		# 	json=$(echo $json | jq '.spec.ports[].name = "80"')
		# 	json=$(echo $json | jq '.spec.ports[].port |= 80')
		fi
	fi

	# write back the yaml file
	echo
	echo "Writing to $folder/processed/$1"
	echo $json | yq eval -P >$folder/processed/$1
	echo
	echo
}

usage() {
	echo "This script will take a k8s manifest (generated out of 'kompose convert') and add kairos-specific constructs to it"
	echo "for example - volumes, imagepullpolicy values etc"
	echo
	echo "usage: ./onboard-k8s-manifest.sh [params]"
	echo ""
	echo -e "\tParams -"
	echo -e ""
	echo -e "\t\t-f | --folder\t[String]\tRequired\tFolder containing kompose converted yaml manifests"
	echo -e ""
	echo -e "\t\t-exp | --experiment\t[String]\tNon-Required\tExperiment name used for spinning up a dedicated node group"
	echo -e ""
	echo ""
}

#############################################
##
## Entry point
##
#############################################

STARTTIME=$(date +%s)

while [ "$1" != "" ]; do
	case $1 in
	-f | --folder)
		shift
		folder=$1
		;;
	-exp | --experiment)
		shift
		experiment=$1
		;;
	-h | --help)
		usage
		exit
		;;
	-*|--*)
      	echo "Unknown option $1"
		usage
		exit 1
		;;
	esac
	shift
done

if [[ -z $folder ]]; then
	usage
	exit 1
fi

curdir=$(pwd)
# run kompose
cd $folder
kompose convert
cd $curdir

# dockerfile=$folder"/docker-compose.yaml"
# echo "Reading docker file $dockerfile"
# #convert to json first
# dockerjson=$(yq eval -j $dockerfile)

# #declare -A gpumap
# #nodeselector
# mapNodeSelector

processedFolder="$folder/processed"
rm -rf $processedFolder
mkdir -pv $processedFolder

i=0
while read line; do
	manifestarray[$i]="$line"
	((i++))
	#done < <(ls -1 $folder/*.yaml)
done < <(ls $folder/*.yaml | xargs -n 1 basename)

for j in "${manifestarray[@]}"; do
	processManifestFile $j $experiment
done

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
