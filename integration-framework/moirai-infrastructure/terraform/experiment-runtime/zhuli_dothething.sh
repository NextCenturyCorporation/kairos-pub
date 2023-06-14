#! /bin/bash
usage() {
	echo "usage: ./create-enclave.sh [params]"
	echo ""
	echo -e "\tParams -"
	echo -e ""
	echo -e "\t\t-cf | --experimentconfigfile\t[string]\texperiment config file of the experiment being performed"
}

function deploy_nodes() {
	curdir=$(pwd)

	name=$(echo $experiment | jq -r '.experiment.name')
	cpu_count=$(echo $experiment | jq -r '.cluster.cpu.desired')
	cpu_type=$(echo $experiment | jq -r '.cluster.cpu.type')
	gpu_count=$(echo $experiment | jq -r '.cluster.gpu.desired')
	gpu_type=$(echo $experiment | jq -r '.cluster.gpu.type')
	cd ./experiment-perfeval-nodes
	terraform init -backend-config="key=experiment-perfeval-$name" -reconfigure
	terraform apply --auto-approve -var="experiment=$name" -var="cpu_nodesmin=$cpu_count" -var="cpu_nodetype=$cpu_type" -var="gpu_nodesmin=$gpu_count" -var="gpu_nodetype=$gpu_type"
	RESULT=$?
	terraform init -backend-config="key=experiment-perfeval-nodes" -reconfigure
	cd $curdir

	if [ $RESULT != 0 ]; then
		exit $RESULT
	fi
}

function copyexperiment() {
	filename=$(basename $1)
	scp -i $pem $1 ec2-user@control.hippodrome.kairos.nextcentury.com:/home/ec2-user/experiment-config/$filename
	ces=$(pwd)/experiment-perfeval/sbin/create-enclave.sh
	scp -i $pem $ces ec2-user@control.hippodrome.kairos.nextcentury.com:/home/ec2-user/scripts/create-enclave.sh
}

function create_enclave() {
	filename=$(basename $1)
	file=$1
	experiment=$2
	name=$(echo $experiment | jq -r '.name')
	ssh -i $pem ec2-user@control.hippodrome.kairos.nextcentury.com sudo rm -rf /var/kairosfs/$name/*
	ssh -i $pem ec2-user@control.hippodrome.kairos.nextcentury.com /home/ec2-user/scripts/create-enclave.sh -cf /home/ec2-user/experiment-config/$filename
}

function addcontrolfingerprint() {
	touch ~/.ssh/known_hosts
	ssh-keygen -R control.hippodrome.kairos.nextcentury.com
	ssh-keyscan -H control.hippodrome.kairos.nextcentury.com >> ~/.ssh/known_hosts
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
experiment=$(cat $experimentconfigfile)
if [[ -z $experiment ]]; then
	echo no experiment found
	exit 1
fi

# ./setup.sh
deploy_nodes
filename=$(basename $experimentconfigfile)
pem=$(echo $curdir | sed s@moirai-infrastructure.*@moirai-infrastructure/key-pairs/moirai-machine.pem@)
addcontrolfingerprint
copyexperiment $experimentconfigfile
create_enclave $experimentconfigfile
# exit 1
ssh -i $pem ec2-user@control.hippodrome.kairos.nextcentury.com /home/ec2-user/scripts/run-enclave.sh -cf /home/ec2-user/experiment-config/$filename
