#! /bin/bash
usage() {
	echo "usage: ./create-enclave.sh [params]"
	echo ""
	echo -e "\tParams -"
	echo -e ""
	echo -e "\t\t-cf | --experimentconfigfile\t[string]\texperiment config file of the experiment being performed"
}

function delete_experiment() {
	name=$(echo $experiment | jq -r '.experiment.name')
	if [ -z $name ]; then
		echo "experiment name is null"
		exit 1
	fi
	enclave=c-$name-enclave
	enclave=$(echo $enclave | tr '[:upper:]' '[:lower:]')
	echo Deleting enclave $enclave
	ssh -i $pem ec2-user@control.hippodrome.kairos.nextcentury.com kubectl delete namespace $enclave
	echo Deleting filesystem /var/kairosfs/$name
	ssh -i $pem ec2-user@control.hippodrome.kairos.nextcentury.com sudo rm -rf /var/kairosfs/$name
}

function delete_nodes() {
	curdir=$(pwd)
	name=$(echo $experiment | jq -r '.experiment.name')
	if [ -z $name ]; then
		echo "experiment name is null"
		exit 1
	fi
	cd ./experiment-perfeval-nodes
	terraform init -backend-config="key=experiment-perfeval-$name"
	terraform destroy --auto-approve
	terraform init -backend-config="key=experiment-perfeval-nodes"
	cd $curdir
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

curdir=$(pwd)
pem=$(echo $curdir | sed s@moirai-infrastructure.*@moirai-infrastructure/key-pairs/moirai-machine.pem@)

experiment=$(cat $experimentconfigfile)
if [[ -z $experiment ]]; then
	echo no experiment found
	exit 1
fi

delete_experiment
delete_nodes
