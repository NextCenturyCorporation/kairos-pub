#!/bin/bash

kairosHeader() {
    echo "Header"
}

getExternalUrls() {
	list=$(kubectl get svc --all-namespaces | grep LoadBalancer | awk '{split($6,a,":"); print $2" "$5":"a[1]}')
	echo $list
}

getKafdropUrl() {
    kafdropurl=$(kubectl get svc kafdrop -n default -o jsonpath='{.status.loadBalancer.ingress[*].hostname}')
    echo "Kafka browser url - http://$kafdropurl:9000"
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

function readExperimentConfig() {
	experimentConfig=$(cat $1)
	# readConfigHeader $1

	# unittest=$(echo $experimentConfig | jq -r '.test')
	# if [[ $unittest == "true" ]]; then
	# 	# data "unit test"
	# fi

	# Variables from experiment
	experiment=$(echo $experimentConfig | jq -r '.experiment')
	manifests=($(echo $experimentConfig | jq -c '.manifests[]'))
    enclave="{}"

	evaluatorname="c"
    experimentname=$(echo $experiment | jq -r '.name')

    # Update experiment
    experiment=$(echo $experiment | jq --arg evaluatorname $evaluatorname '.evaluatorname += $evaluatorname')
    
    
    # Generated
    enclavename=$evaluatorname-$experimentname-enclave
    pv=$evaluatorname-$experimentname-efs-pv
    inputTopic=${evaluatorname,,}"-"${experimentname,,}"-input-topic"
    outputTopic=${evaluatorname,,}"-"${experimentname,,}"-output-topic"

    enclave=$(echo $enclave | jq --arg enclavename $enclavename '.enclavename += $enclavename')
    enclave=$(echo $enclave | jq --arg persistentvolume $pv '.persistentvolume += $persistentvolume')
    enclave=$(echo $enclave | jq --arg inputTopic $inputTopic '.inputTopic += $inputTopic')
    enclave=$(echo $enclave | jq --arg outputTopic $outputTopic '.outputTopic += $outputTopic')

    # echo $experiment

    output=[
    for manifest in "${manifests[@]}"; do
        if [[ $output != "[" ]]; then
            output+=","
        fi
        manifest=$(echo $manifest | jq --arg experiment '$experiment' ".experiment = $experiment")
        manifest=$(echo $manifest | jq --arg enclave '$enclave' ".enclave = $enclave")
        output+=$manifest
    done
    output+=]
    echo $output
}

function waitForNamespace() {
	NAMESPACE=$1

	NAMESPACE_STRING=""
	if [[ -z $NAMESPACE ]];
	then
		echo "no namespace used"
	else
		NAMESPACE_STRING="-n $NAMESPACE"
	fi

	kubectl get deploy,sts,ds -o name $NAMESPACE_STRING | xargs -r -n1 -t kubectl rollout status $NAMESPACE_STRING
}

function getKafkaBrokers() {
    FILE="/home/ec2-user/infrastructure-config/enclave.config"
    JSON=$(yq eval -j $FILE)
    kafkabrokers=$(echo $JSON | jq -r .kafkabrokers)
	echo $kafkabrokers
}

function deleteExperiment() {
	EXPERIMENT=$1

    NAME=$(echo $EXPERIMENT | jq -r '.experiment.name')
    NAMESPACE=$(echo $EXPERIMENT | jq -r '.enclave.enclavename')
    PV=$(echo $EXPERIMENT | jq -r '.enclave.persistentvolume')
    INPUT_TOPIC=$(echo $EXPERIMENT | jq -r '.enclave.inputTopic')
    OUTPUT_TOPIC=$(echo $EXPERIMENT | jq -r '.enclave.outputTopic')

    if [[ -z $NAME ]];
    then
		echo "no name found in $EXPERIMENT"
        exit 1
	fi

	echo "Deleting folder /var/kairosfs/$NAME"
    sudo rm -rf /var/kairosfs/$NAME

    # check if ns still exists
    nscheck=$(kubectl get namespace | grep $NAMESPACE)
	if [ ! -z "$nscheck" ]; then
		echo "Deleting namespace volume $NAMESPACE"
		kubectl delete namespace $NAMESPACE
	fi

	# check if pv still exists - pv's do not exist inside the namespace
	pvcheck=$(kubectl get pv | grep $PV)
	if [ ! -z "$pvcheck" ]; then
		echo "Deleting preexisting persistant volume $PV"
		kubectl delete pv $PV
	fi

    # delete kafka topics
    KafkaDeleteTopic $INPUT_TOPIC
    KafkaDeleteTopic $OUTPUT_TOPIC
}

function KafkaDeleteTopic() {
	TOPIC=$1
	BROKERS=$(getKafkaBrokers)
	FIND_COMMAND="/kafka/bin/kafka-topics.sh --list --bootstrap-server $BROKERS --topic $TOPIC | grep -v ^$ | wc -l"
	FOUND=$(eval $FIND_COMMAND)
	if [[ FOUND -gt 0 ]]; then
		echo "Deleting topic $TOPIC"
		DELETE_COMMAND="/kafka/bin/kafka-topics.sh --delete --bootstrap-server $BROKERS --topic $TOPIC"
		# echo $DELETE_COMMAND
		eval $DELETE_COMMAND
		echo
	fi
}

function KafkaCreateTopic() {
	TOPIC=$1
	BROKERS=$(getKafkaBrokers)
	
	echo "Creating topic $TOPIC"
	CREATE_COMMAND="/kafka/bin/kafka-topics.sh --create --bootstrap-server $BROKERS --topic $TOPIC"
	# echo $CREATE_COMMAND
	eval $CREATE_COMMAND
	echo
}

function KafkaConfigTopic() {
	TOPIC=$1
	BROKERS=$(getKafkaBrokers)
	
	echo "Configuring topic $TOPIC"
	CONFIG_COMMAND="/kafka/bin/kafka-configs.sh --bootstrap-server $BROKERS --entity-type topics --alter --add-config retention.ms=-1 --entity-name $TOPIC"
	# echo $CONFIG_COMMAND
	eval $CONFIG_COMMAND
	echo
}
