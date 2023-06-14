#! /bin/bash

usage() {
    echo "usage: ./run-enclave.sh [params]"
    echo ""
    echo -e "\tParams -"
    echo -e ""
    echo -e "\t\t-cf | --experimentconfigfile\t[string]\texperiment config file of the experiment being performed"
}

#########
########## Main - Entry point
#########

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

experiments=$(readExperimentConfig $experimentconfigfile)
kafkabrokers=$(getKafkaBrokers)

echo "Deploying experiments"
for experiment in $(echo "${experiments}" | jq -c '.[]' ); do
    echo $experiment | python3 -m json.tool

    experimentname=$(echo $experiment | jq -r '.experiment.name')
    evaluatorname=$(echo $experiment | jq -r '.experiment.evaluatorname')
    tasktype=$(echo $experiment | jq -r '.experiment.type')
    performername=$(echo $experiment | jq -r '.performername')
    enclave=$(echo $experiment | jq -r '.enclave.enclavename')
    inputTopic=$(echo $experiment | jq -r '.enclave.inputTopic')
    
    waitForNamespace kairos-redis
    waitForNamespace $enclave

    contentUri="/var/kairosfs/${experimentname,,}/${performername,,}/input/${tasktype,,}"
    runId=$experimentname-$(date +%s)

    msg="{ \
        \\\"id\\\": \\\"$(uuidgen)\\\", \
        \\\"runId\\\": \\\"$runId\\\", \
        \\\"sender\\\": \\\"kairos\\\", \
        \\\"time\\\": \\\"$(date '+%m/%d/%Y %H:%M:%S')\\\", \
        \\\"contentUri\\\": \\\"$contentUri\\\", \
        \\\"content\\\": {\\\"data\\\": \\\"null\\\"} \
    }"

    echo "================Sending the following to topic $inputTopic ======================"
    cmd="echo \"$msg\" | /kafka/bin/kafka-console-producer.sh --broker-list $kafkabrokers --topic \"$inputTopic\""

    echo $cmd
    eval $cmd
done
