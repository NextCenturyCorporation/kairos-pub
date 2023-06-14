#! /bin/bash -x

function copyexperiment() {
    scp -i $pem $1 ec2-user@controlbox.kairosq9.kairos.nextcentury.com:/home/ec2-user/experiment-config/$1
}

function create_enclave() {
    file=$1
    experiment=$2
    name=$(echo $experiment | jq -r '.name' )
    ssh -i $pem ec2-user@controlbox.kairosq9.kairos.nextcentury.com sudo rm -rf /var/kairosfs/$name/*
    ssh -i $pem ec2-user@controlbox.kairosq9.kairos.nextcentury.com /home/ec2-user/scripts/create-enclave.sh -cf /home/ec2-user/experiment-config/$1
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

experiments=$(jq -c '.experimentConfigurations[]' $experimentconfigfile)
if [[ -z $experiments ]]; then
	echo no experiments found
	exit 1
fi

pem=$(echo $curdir | sed s@moirai-infrastructure.*@moirai-infrastructure/key-pairs/moirai-machine.pem@)
copyexperiment $experimentconfigfile
create_enclave $experimentconfigfile $experiment
ssh -i $pem ec2-user@controlbox.kairosq9.kairos.nextcentury.com /home/ec2-user/scripts/run-enclave.sh -cf /home/ec2-user/experiment-config/$experimentconfigfile

