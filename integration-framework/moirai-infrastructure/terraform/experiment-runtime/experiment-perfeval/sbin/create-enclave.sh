#!/bin/bash

usage() {
	echo "usage: ./create-enclave.sh [params]"
	echo ""
	echo -e "\tParams -"
	echo -e ""
	echo -e "\t\t-cf | --experimentconfigfile\t[string]\texperiment config file of the experiment being performed"
}

dataheader() {
	echo
	echo -e -n " __"
	echo -n $1
	seq ${#1} 147 | xargs -I {} echo -n "_"
	echo
}

data() {
	if [ $# == 1 ]; then
		cmd="echo -e \"|\t$1\t= \${$1[*]}\""
		eval $cmd
		return
	elif [ $# == 0 ]; then
		return
	fi

	longest=0
	for word in "$@"; do
		curlen=${#word}
		if [ $((curlen)) -gt $((longest)) ]; then
			longest=$curlen
		fi
	done

	# seq 1 100 | xargs -I {} echo -n "_"; echo
	# echo "| "
	for datapoint in "$@"; do
		curlen=${#datapoint}
		echo -n -e "|\t"
		echo -n $datapoint
		seq $curlen $longest | xargs -I {} echo -n " "

		#This needs to be set as a command first then evaluated.
		#The first like changs the $1 to the variable name and the \$ to $
		#The second command evaluates the variable at that name
		cmd="echo -e \"  =  \${$datapoint[*]}\""
		eval $cmd
	done
	echo " ‾‾‾‾‾‾‾"
	# echo "| "
	# seq 1 100 | xargs -I {} echo -n "‾"; echo
}

header() {
	echo -e ""
	echo -e " ########################################################################################################################"
	echo -e "#"
	echo -e "#\t$1 ...."
	echo -e "#"
	echo -e "########################################################################################################################"
	echo -e ""
}

readConfigHeader() {
	header "Reading config file $1 ...."
}

endHeader() {
	inputtopic=$1
	outputtopic=$2
	errortopic=$3

	echo -e ""
	echo -e "########################################################################################################################"
	echo -e "#\tEnclave setup complete...."
	echo -e "#"
	echo -e "#\t\t Post kairos input messages to '$inputtopic'"
	echo -e "#\t\t Results are posted to '$outputtopic', and errors to '$errortopic'"
	echo -e "#"
	echo -e "#\t\t Experiment S3 bucket - s3://kairos-experiment-output/${experimentname,,}/${start_time_id}/${performername}/"
	echo -e "#######################################################################################################################"
	echo -e ""
}

function ensureVariable() {
	cmd="var=\$(echo -e \"\$$1\")"
	eval $cmd
	if [[ -z $var || $var == "" || $var == null ]]; then
		echo -e ""
		echo -e $2
		echo -e ""
		exit 1
	fi
}

function setIfEmpty() {
	cmd="var=\$(echo -e \"\$$1\")"
	eval $cmd
	if [[ -z $var || $var == "" || $var == null ]]; then
		echo
		echo -e "Using default value $2 for $1"
		cmd="$1=$2"
		eval $cmd
	fi
}

function assertS3Exists() {
	path=$1
	result=$(test "$(aws s3 ls $path)" == "" || echo 'exists')
	if [[ $result != "exists" ]]; then
		echo
		echo "Path $path not found - invalid value provided - $path"
		echo
		exit 1
	fi
}

function validation() {
	# ensure valid tasktype
	if [[ ${tasktype,,} = task1* ]]
	then
		tasktype="task1"
	elif [[ ${tasktype,,} = task2* ]]
	then
		tasktype="task2"
	else
		echo
		echo "Task type should be either 'task1' or 'task2' - invalid value provided - $tasktype"
		echo
		exit 1
	fi
	# ensure valid evaluationdataset exists in s3
	assertS3Exists $taskLocation
	#TODO assertS3Exists for each schema and uri in manifests
}

function init() {
	kubectl config set-context --current --namespace=default
	# patch the gp2 storage class, so that it can be dynamically expanded
	kubectl patch storageclass gp2 -p '{"allowVolumeExpansion":true}'
	switchKubeContextPerfEval

	mkdir -pv $enclavesfolder

	header "cleanup $namespacename"
	
	# check if this enclave already exists
	enclavecheck=$(kubectl get namespace -A | grep $namespacename)
	if [ ! -z "$enclavecheck" ]; then
		echo "Deleting preexisting namespace $namespacename"
		kubectl delete namespace $namespacename
	fi

	# check if pv still exists - pv's do not exist inside the namespace
	pvcheck=$(kubectl get pv | grep $uniqueprefix-efs-pv)
	if [ ! -z "$pvcheck" ]; then
		echo "Deleting preexisting persistant volume $pvcheck"
		kubectl delete pv $uniqueprefix-efs-pv
	fi


	echo "Creating enclave namespace"
	kubectl create namespace $namespacename

	header "cleanup $experimentfolder"
	rm -rf $experimentfolder
	mkdir -pv $experimentfolder

	# edit the pv to make it available again if it doesnt exist yet this will fail, thats ok
	kubectl patch pv ${evaluatorname,,}"-"${experimentname,,}"-efs-pv" -p '{"spec":{"claimRef": null}}'

	echo
	echo "Setting up AWS secret... in enclave - $namespacename"
	kubectl create secret generic awsconfig --from-literal=awsaccesskey=$awsaccesskey --from-literal=awssecretaccesskey=$awssecretaccesskey --from-literal=awsregion=$region -n $namespacename
	echo

	createKafkaTopics

	# experiment input ingest
	#experimentinputingesttype=$(echo $experiment | jq -r '.experiment.inputingest')
	#echo "inputingesttype - $experimentinputingesttype"
	#if [[ ${experimentinputingesttype} != "direct" && ${experimentinputingesttype} != "nifi" ]]
	#then
	#        echo
	#        echo "Input Ingest type should be either 'direct' or 'nifi' - invalid value provided - $experimentinputingesttype"
	#        echo
	#        exit 1
	#fi
	#if [[ $experimentinputingesttype == "direct" ]]
	#then
	#        servicetype="type: LoadBalancer"
	#else
	#        servicetype=""
	#fi
}

function readEnclaveConfig() {
	echo
	FILE="/home/ec2-user/infrastructure-config/enclave.config"
	readConfigHeader $FILE
	JSON=$(yq eval -j $FILE)

	efsId=$(echo $JSON | jq -r .efsId)
	region=$(echo $JSON | jq -r .region)
	perfevalclusterarn=$(echo $JSON | jq -r .perfevalclusterarn)
	perfevalclustername=$(echo $JSON | jq -r .perfevalclustername)
	submissioningestclusterarn=$(echo $JSON | jq -r .submissioningestclusterarn)
	submissioningestclustername=$(echo $JSON | jq -r .submissioningestclustername)
	mskclusterarn=$(echo $JSON | jq -r .mskclusterarn)
	kafkabrokers=$(echo $JSON | jq -r .kafkabrokers)
	nifiurl=$(echo $JSON | jq -r .nifi.dnsname)
	nifiport=$(echo $JSON | jq -r .nifi.port)
	awsaccesskey=$(echo $JSON | jq -r .aws.accesskey)
	awssecretaccesskey=$(echo $JSON | jq -r .aws.secretaccesskey)

	# the from elastic search config - was commented out but saving here
	#esdomainconfigFile=$configFolder"/esdomain-config.json"
	#config=$(cat $esdomainconfigFile)
	#esdomainendpoint=$(echo $config | jq -r '.DomainStatus.Endpoints.vpc')

	# Check that mandatory variable exist before continuing
	ensureVariable efsId "Could not find efsId"
	ensureVariable awsaccesskey "AWS access key not set!!"
	ensureVariable awssecretaccesskey "AWS secret access key not set!!"
	ensureVariable mskclusterarn "Could not find kafka cluster information"

	data efsId \
		region \
		awsaccesskey \
		awssecretaccesskey \
		mskclusterarn
	# ensureVariable $nifiurl "Could not find the nifi url"
}

function readExperimentConfigLocal() {
	experimentConfig=$(cat $1)
	readConfigHeader $1

	unittest=$(echo $experimentConfig | jq -r '.test')
	if [[ $unittest == "true" ]]; then
		data "unit test"
	fi

	# Variables from experiment
	experimentname=$(echo $experimentConfig | jq -r '.experiment.name')
	evaluatorname="c"
	tasktype=$(echo $experimentConfig | jq -r '.experiment.type')
	taskLocation=$(echo $experimentConfig | jq -r '.experiment.tasklocation')
	evaluationdataset=$(echo $experimentConfig | jq -r '.experiment.evaluationdataset')
	start_time_id=$(echo $experimentConfig | jq -r '.experiment.startTime')
	manifests=($(echo $experimentConfig | jq -c '.manifests[]'))


	# dataheader "Experiment Variables"
	data experimentname \
		evaluatorname \
		tasktype \
		taskLocation \
		evaluationdataset \
		manifests \
		start_time_id
}

function generateManifests() {
	header "Generating Manifests"
	external_file=$1
	tmpDir=tmp/manifestGeneration
	envFile="$tmpDir/.env"

	echo "Making $tmpDir"
	mkdir -p $tmpDir

	echo "Making .env file"
	envFile="$tmpDir/.env"
	echo "EXPERIMENT=${experimentname,,}" >$envFile
	echo "EVALUATOR=${evaluatorname,,}" >>$envFile
	echo "PERFORMER_NAME=${performername,,}" >>$envFile
	echo "SUBMISSION_URL=$submissionurl" >>$envFile
	echo "KAIROS_LIB=/var/kairosfs" >>$envFile

	echo "Pulling docker-compose file"
	if [[ $external_file == s3://* ]]; then
		# Download compose file to temp directory
		composeFile=$tmpDir/docker-compose.yaml
		aws s3 cp $external_file $composeFile

		# Determine final location based on filename
		komposeFolder=$2/$performername
	elif [[ $external_file == http://* || $external_file == https://* ]]; then
		echo "update generate Manifests method in create-enclave.sh to handle pulling from http/https"
	else
		echo "$1 is not a valid manifest location"
		exit 1
	fi

	echo "Running onboard-k8s-manifest.sh $komposeFolder"
	scripts/onboard-k8s-manifest.sh -f $tmpDir -exp $experimentname

	echo "Cleaning up tmp folder"
	rm -rf $komposeFolder
	mkdir -p $komposeFolder
	mv $tmpDir/processed/* $komposeFolder
	rm -rf $tmpDir
}

function getSchemaLibs() {
	# $1 = performer folder path
	# $2+ = schema s3 locations
	originalPath=$(pwd)

	header "copy schema libs"
	path=$1/schemas
	shift
	schemas=$@

	sudo rm -rf $path
	sudo mkdir -pv $path
	cd $path

	for schema in $schemas; do
		schemaname=$(basename $schema)
		subpath=$path/$schemaname
		sudo mkdir -pv $subpath
		sudo aws s3 cp $schema $subpath --recursive --exclude "*.validation"
	done

	cd $originalPath
}


function getInputFiles() {
	# $1 = performer folder path
	# $2 = task name
	# $3 = task s3 location

	header "copy input folder"
	path=$1/input/$2
	taskUri=$3

	sudo rm -rf $path
	sudo mkdir -pv $path

	sudo aws s3 cp $taskLocation $path --recursive
}

function performSubstitutions() {
	folder=$1
	header "perform substitutions in $(pwd)/$folder"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{evaluator}}~${evaluatorname,,}~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{experiment}}~${experimentname,,}~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{task_type}}~${tasktype,,}~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{performer}}~${performername,,}~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{fqexperimentname}}~${fqexperimentname,,}~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{kafkabrokers}}~${kafkabrokers,,}~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{esdomainendpoint}}~${esdomainendpoint,,}~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{kairosperformerexists}}~${kairosperformerexists}~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{aidaperformerexists}}~${aidaperformerexists}~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{start_time_id}}~${start_time_id}~g"

	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{username}}~${username,,}~g"

	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{imagepullsecret}}~${performername,,}dockerrepocred~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{imagespec}}~${imagespec}~g"

	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{servicename}}~${servicename}~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{servicetype}}~${servicetype}~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{extraparams}}~${envitems}~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{kairosfsmountpath}}~${kairosfsmountpath}~g"

	# kairos-specific stuff
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{experimentperformerpath}}~${experimentperformerpath}~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{entrypointpathspec}}~${entrypointpathspec}~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{readinesscheckpathspec}}~${readinesscheckpathspec}~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{livenesscheckpathspec}}~${livenesscheckpathspec}~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{kairospersistpath}}~${persistpathspec}~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{kairoslogpath}}~${logpathspec}~g"
	# kairos-specific stuff

	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{port}}~${port}~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{schemalibraries}}~${schemalibs,,}~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{tasktype}}~${tasktype,,}~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{evaluationdataset}}~${evaluationdataset,,}~g"

	# nist-expk-performera-msgprocessor.nist-expk-enclave.svc.cluster.local/kairos/submission
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{submissionurl}}~${submissionurl,,}~g"
	find $folder -type f -name '*.yaml' | xargs sed -i "s~{{efsid}}~${efsId}~g"
}

function createKafkaTopic() {
	topic=${evaluatorname,,}"-"${experimentname,,}"-$1-topic"

	KafkaDeleteTopic $topic
	KafkaCreateTopic $topic
	KafkaConfigTopic $topic
}

function createKafkaTopics() {
	header "Creating kafka topics - kafka arn - $mskclusterarn"

	createKafkaTopic input
	createKafkaTopic output
	createKafkaTopic error
	createKafkaTopic submissioningest-error
}

function configMapExists() {
	name=$1
	namespace=$2

	if [[ $namespace == null || -z $namespace ]]; then
		result=$(kubectl get configmap $name -n default 2>&1 | grep "NotFound")
	else
		result=$(kubectl get configmap $name -n $namespace -o json 2>&1 | grep "NotFound")
	fi

	echo "$result"
}

switchKubeContext() {
	arn=$1
	echo "Switching context to $arn"

	# go to that cluster
	val=$(kubectl config use-context $arn 2>&1 | grep error)
	if [[ ! -z $val ]]; then
		echo -e ""
		echo -e "Error switching kubectl context $arn"
		echo -e ""
		echo -e ""
		exit 1
	fi
}

switchKubeContextPerfEval() {
	switchKubeContext $perfevalclusterarn
}

switchKubeContextSubmissionIngest() {
	switchKubeContext $submissioningestclusterarn
}

setupKairosPerformer() {
	header "setupKairosPerformer $performername"

	#create performer container s3 uploader configmap
	kubectl create configmap performercontainer-s3uploader-script --from-file=k8s-runtimes/performercontainer-s3uploader.sh -n $namespacename -o yaml --dry-run | kubectl apply -f -
	cp -fv k8s-runtimes/performercontainer-s3uploader.yaml.template $experimentfolder/$performername-s3uploader.yaml

	# create the init script config map
	kubectl create configmap kairos-msgprocessor-init-script --from-file=k8s-runtimes/kairos-msgprocessor-init.sh -n $namespacename -o yaml --dry-run | kubectl apply -f -
	cp -fv k8s-runtimes/msgingest-service.yaml.template $experimentfolder/msgingest-service.yaml

	# use the kairos deployment template
	cp -fv k8s-runtimes/msgprocessor-deployment.yaml.template $experimentfolder/$performername-msgprocessor-deployment.yaml
	cp -fv k8s-runtimes/msgprocessor-service.yaml.template $experimentfolder/$performername-msgprocessor-service.yaml

	# cp -fv k8s-runtimes/performercontainer-deployment.yaml.template $experimentfolder/$performername-performercontainer-deployment.yaml
	# cp -fv k8s-runtimes/performercontainer-service.yaml.template $experimentfolder/$performername-performercontainer-service.yaml
	#cp -f k8s-runtimes/performercontainer-ingress.yaml.template $experimentfolder/$performername-performercontainer-ingress.yaml
	cp -fv k8s-runtimes/persistent-volume.yaml.template $experimentfolder/$performername-kairos-pv.yaml
}

#########
########## Main - Entry point
#########

STARTTIME=$(date +%s)

#
# global defaults
#
while [ "$1" != "" ]; do
	case $1 in
	-cf | --experimentconfigfile)
		shift
		experimentconfigfile=$1
		;;
	-h | --help)
		usage
		exit
		;;
	*)
		usage
		exit 1
		;;
	esac
	shift
done

if [[ -z $experimentconfigfile ]]; then
	usage
	exit 1
fi

sbin="$(dirname "${BASH_SOURCE}")"
source $sbin/kairos-functions.sh

readEnclaveConfig
readExperimentConfigLocal $experimentconfigfile

# dynamic variables
enclavesfolder="enclaves"
kairosfsmountpath="/var/kairosfs"
uniqueprefix="${evaluatorname,,}-${experimentname,,}"
namespacename="${uniqueprefix}-enclave"
experimentfolder=$enclavesfolder"/"$experimentname
kairosperformerexists=true
aidaperformerexists=false

dataheader "Generated Experiment variables"
data namespacename \
	experimentfolder \

validation
init

header "Setting up enclave performer components in enclave..."
for manifest in "${manifests[@]}"; do
	# performer variables
	performername=$(echo $manifest | jq -r '.performername')
	manifestfileslocation=$(echo $manifest | jq -r '.uri')
	schemalibraries=$(echo $manifest | jq -c '.schemalibraries[]' | tr -d '"')
	port=$(echo $manifest | jq -r '.service_port')

	setIfEmpty port 10100
	setIfEmpty performerinvocationstyle kairos
	ensureVariable manifestfileslocation "No location specified for manifests for performer $performername!"

	dataheader "Performer variables"
	data performername \
		performerinvocationstyle \
		manifestfileslocation \
		port \
		schemalibraries

	# generated variables
	fqexperimentname="${evaluatorname,,}-${experimentname,,}-${performername,,}"
	experimentperformerpath="$kairosfsmountpath/${experimentname,,}/${performername,,}"
	persistpathspec=$experimentperformerpath"/persist"
	logpathspec=$experimentperformerpath"/log"
	submissionurl="http://"${evaluatorname,,}"-"${experimentname,,}"-"${performername,,}"-msgprocessor."${evaluatorname,,}"-"${experimentname,,}"-enclave.svc.cluster.local/kairos/submission"
	schemalibs=$(echo "${schemalibraries[@]}" | tr -d '"' | xargs printf 'schemalib:%s ')

	entrypointpathspec="/kairos/entrypoint"
	readinesscheckpathspec="/kairos/ready"
	livenesscheckpathspec="/kairos/alive"

	servicename=${performername,,}"-main"

	# performer docker runtime parameters
	runtimeparamsarray=($(echo $performer | jq -c '.dockerimageconfig.runtimeparams[]?'))
	envitems=""
	for k in "${runtimeparamsarray[@]}"; do
		kvpair=$(echo $k | sed 's~"~~g' | sed 's~}~~g' | sed 's~{~~g')
		#echo -e "\t\tdocker container runtime param item - $kvpair"
		key=$(echo $kvpair | cut -d ":" -f1)
		value=$(echo $kvpair | cut -d ":" -f2)
		#echo -e "\t\tkey - $key, value - $value"
		envitem="\n          - name: $key\n            value: \"$value\""
		envitems=$envitems$envitem
	done

	dataheader "Generated Performer Variables"
	data fqexperimentname \
		experimentperformerpath \
		persistpathspec \
		logpathspec \
		submissionurl \
		schemalibs \
		envitems \
		servicename
	

	header "Performer gateway service - http://$servicename.$namespacename.svc.cluster.local$entrypointpathspec"
	# Do work
	generateManifests $manifestfileslocation $experimentfolder
	setupKairosPerformer

	getSchemaLibs ${experimentperformerpath,,} $schemalibraries
	getInputFiles ${experimentperformerpath,,} $tasktype $taskLocation

	# copy the username/password from the main secret - todo get this from config file
	result=$(kubectl get secret perfdockerreposecret --output="jsonpath={.data.\.dockerconfigjson}" -n default | base64 --decode)
	authqualifier="https://index.docker.io/v1/"
	usernamefilter=".auths.\"$authqualifier\".username"
	username=$(echo $result | jq -r "$usernamefilter")
	passwordfilter=".auths.\"$authqualifier\".password"
	password=$(echo $result | jq -r "$passwordfilter")
	dockerserver="https://${performername,,}.docker.kairos.nextcentury.com"

	kubectl create secret docker-registry ${performername,,}dockerrepocred --docker-server=$dockerserver --docker-username=$username --docker-password=$password -n $namespacename -o yaml --dry-run | kubectl apply -f -
	
	# TODO Set this during beachhead creation in terraform
	sudo aws configure set aws_access_key_id $awsaccesskey
	sudo aws configure set aws_secret_access_key $awssecretaccesskey
	sudo aws configure set region $region

	# perform substitutions
	performSubstitutions $experimentfolder
 
	header "Deploying PerfEval k8s manifests..."
	data experimentname
	
	# create the pv first
	kubectl apply -f $experimentfolder/${performername,,}-kairos-pv.yaml -n $namespacename
	sleep 1
	kubectl apply -f $experimentfolder -n $namespacename
	sleep 1
	
	# now deploy
	scripts/deploy-${performername,,}-k8s.sh $experimentfolder/${performername,,} $namespacename
	echo "Done"
	waitForNamespace $namespacename
	echo ""

	# echo "Deploying Submission ingest containers...."
	# switch to submissioningest cluster

	# Not ready to do the following
	# switchKubeContextSubmissionIngest

	# cp -fv k8s-runtimes/submissioningest-service.yaml.template $experimentfolder/submissioningest-service.yaml

	# # # substitution
	# find $experimentfolder -type f -name '*.yaml' | xargs sed -i "s~{{evaluator}}~${evaluatorname,,}~g"
	# find $experimentfolder -type f -name '*.yaml' | xargs sed -i "s~{{experiment}}~${experimentname,,}~g"
	# find $experimentfolder -type f -name '*.yaml' | xargs sed -i "s~{{performer}}~${performername,,}~g"
	# find $experimentfolder -type f -name '*.yaml' | xargs sed -i "s~{{kafkabrokers}}~${kafkabrokers,,}~g"

	# ns="${evaluatorname,,}-${experimentname,,}-submissioningest"
	# kubectl create namespace $ns
	# echo "Creating enclave namespace $ns"
	# echo "Setting up AWS secret... in enclave - $namespacename"
	# kubectl create secret generic awsconfig --from-literal=awsaccesskey=$awsaccesskey --from-literal=awssecretaccesskey=$awssecretaccesskey --from-literal=awsregion=$region -n $ns
	# echo
	# kubectl apply -f $experimentfolder/submissioningest-service.yaml -n $ns
	# echo "Done"
	# # switch back to perfeval cluster
	switchKubeContextPerfEval
	echo ""

done

header "save the current config"
kubecontext=$(kubectl config current-context)
experimentconfigfolder=$experimentfolder"/config"
mkdir -pv $experimentconfigfolder
matexperimentconfigfile=$experimentconfigfolder"/experiment-config.json"
echo "{ \"experimentname\": \"${experimentname,,}\"," >>$matexperimentconfigfile
echo "\"kafkaclusterarn\": \"$mskclusterarn\"," >>$matexperimentconfigfile
echo "\"kafkabrokers\": \"$kafkabrokers\"," >>$matexperimentconfigfile
echo "\"perfevalcontext\": \"$kubecontext\"," >>$matexperimentconfigfile

inputtopic=${evaluatorname,,}"-"${experimentname,,}"-input-topic"
outputtopic=${evaluatorname,,}"-"${experimentname,,}"-output-topic"
errortopic=${evaluatorname,,}"-"${experimentname,,}"-error-topic"
echo "\"inputtopic\": \"$inputtopic\"," >>$matexperimentconfigfile
echo "\"outputtopic\": \"$outputtopic\"," >>$matexperimentconfigfile
echo "\"errortopic\": \"$errortopic\"," >>$matexperimentconfigfile
echo "\"experimentstatusendpoint\": \"$fqurl\"" >>$matexperimentconfigfile
echo "}" >>$matexperimentconfigfile
echo "Saved experiment config to $matexperimentconfigfile"
filecontents=$(cat $matexperimentconfigfile)
echo $filecontents | python -m json.tool
echo ""

endHeader $inputtopic $outputtopic $errortopic
echo ""

# setup nifi
#./setup-enclave-ingest.sh -cf $experimentconfigfile

# change back to perfeval kubecontext
# go to perfeval cluster
switchKubeContextPerfEval

ENDTIME=$(date +%s)

echo ""
secs=$((ENDTIME - STARTTIME))
hrs=$(($secs / 3600))
min=$(($secs / 60))
secsleft=$(($secs - $min * 60))
echo "Total time taken - "
printf '%dh:%dm:%ds\n' $hrs $min $(($secsleft))
echo ""
