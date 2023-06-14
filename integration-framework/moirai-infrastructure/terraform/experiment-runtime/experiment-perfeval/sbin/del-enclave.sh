#! /bin/bash

usage() {
    echo "usage: ./delete-enclave.sh [params]"
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

echo "Deleting experiments"
for experiment in $(echo "${experiments}" | jq -c '.[]' ); do
    deleteExperiment $experiment
done
