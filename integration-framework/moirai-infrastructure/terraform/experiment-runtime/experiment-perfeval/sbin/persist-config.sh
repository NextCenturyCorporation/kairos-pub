#!/bin/bash

getval() {
	value="$1"
	if [[ -z $value || $value == "" || $value == "null" ]]; then
		value="\"\""
	fi

	echo $value
}

usage() {
	echo "usage: ./persist-config.sh [params]"
	echo ""
	echo -e "\tParams -"
	echo -e ""
	echo -e "\t\t-pgm | --program\t[String]\tRequired\tProgram name"
	echo -e ""
	echo -e "\t\t-env | --environment\t[String]\tRequired\tEnvironment name"
	echo -e ""
	echo ""
}

#########
########## Main - Entry point
#########

while [ "$1" != "" ]; do
	case $1 in
	-pgm | --program)
		shift
		program=$1
		;;
	-env | --environment)
		shift
		environment=$1
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

stsidentity=$(aws sts get-caller-identity)
awsaccount=$(echo $stsidentity | jq -r '.Account')
#echo "AWS Account - "$awsaccount
#region="us-east-1"
region=$(aws ec2 describe-availability-zones --output text --query 'AvailabilityZones[0].[RegionName]')
#echo "AWS Region - "$region

vpcname=$program"-"$environment"-VPC"
vpcdesc=$(aws ec2 describe-vpcs --filters Name=isDefault,Values=false Name=tag:Name,Values=$vpcname)
vpcId=$(echo $vpcdesc | jq -r '.Vpcs[].VpcId')

if [[ -z $environment || -z $program || -z $vpcId || -z $region ]]; then
	usage
	exit 1
fi

echo ""
echo ""
echo "Persisting configuration for resources created..."

programconfigfolder="infrastructure-config/"$program"-"${environment^^}"-config"

# create a folder for the program
rm -rfv $programconfigfolder
mkdir -p $programconfigfolder

configfile="vpc-config.json"
echo -e "Creating $programconfigfolder/$configfile..."
echo -e "Persisting VPC Info..."
echo "{ \"VpcConfig\": " >>$programconfigfolder/$configfile
value=$(aws ec2 describe-vpcs --vpc-ids $vpcId)
value=$(getval "$value")
echo $value >>$programconfigfolder/$configfile
echo "," >>$programconfigfolder/$configfile
echo -e "Done"

echo -e "Persisting VPC subnets..."
echo "\"VpcSubnets\": " >>$programconfigfolder/$configfile
value=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$vpcId")
value=$(getval "$value")
echo $value >>$programconfigfolder/$configfile
echo "}" >>$programconfigfolder/$configfile
echo -e "Done"

echo -e "Persisting kubernetes clusters..."
# persist k8s clusters
clusters=($(aws eks list-clusters 2>&1 | jq -r '.clusters[]' | grep -i "${environment^^}"))
for i in "${clusters[@]}"; do
	value=$(echo $i | cut -d "-" -f3)
	configfile="$value-config.json"
	echo -e "Creating $programconfigfolder/$configfile..."
	value=$(aws eks describe-cluster --name $i)
	value=$(getval "$value")
	echo $value >>$programconfigfolder/$configfile
done
echo -e "Done"

echo -e "Persisting nifi instance..."
result=$(aws ec2 describe-instances --filters Name=vpc-id,Values=$vpcId)
instances=($(echo $result | jq -c '.Reservations[].Instances[]'))
configfile="nificonfig.json"
echo -e "Creating $programconfigfolder/$configfile..."
echo "{ \"NifiConfig\": {" >>$programconfigfolder/$configfile
for i in "${instances[@]}"; do
	tagsarray=($(echo $i | jq -c '.Tags[]'))
	for j in "${tagsarray[@]}"; do
		keyvalue=$j
		key=$(echo $keyvalue | jq -r '.Key')
		value=$(echo $keyvalue | jq -r '.Value')
		if [[ $key == "Name" ]]; then
			if [[ $value == $program"-"${environment^^}"-Nifi" ]]; then
				instanceId=$(echo $i | jq -r '.InstanceId')
				value=$(getval "$instanceId")
				echo "\"InstanceId\" : \"$value\"," >>$programconfigfolder/$configfile

				publicDns=$(echo $i | jq -r '.PublicDnsName')
				value=$(getval "$publicDns")
				echo "\"PublicDnsName\" : \"$value\"" >>$programconfigfolder/$configfile
			fi
		fi
	done
done
echo "}}" >>$programconfigfolder/$configfile
echo -e "Done"

echo -e "Persisting Kafka cluster config..."
configfile="kafka-config.json"
echo -e "Creating $programconfigfolder/$configfile..."
allclusters=($(aws kafka list-clusters 2>&1 | jq -c '.ClusterInfoList[]'))
for i in "${allclusters[@]}"; do
	clusterName=$(echo $i | jq -r '.ClusterName')
	clusterArn=$(echo $i | jq -r '.ClusterArn')
	if [[ "${clusterName^^}" == *"${environment^^}"* ]]; then
		value=$(getval "$i")
		echo $value >>$programconfigfolder/$configfile

		# now persist the kafka broker info
		echo -e "Persisting Kafka cluster broker config..."
		configfile="kafka-broker-config.json"
		value=$(aws kafka get-bootstrap-brokers --cluster-arn $clusterArn)
		value=$(getval "$value")
		echo $value >>$programconfigfolder/$configfile
	fi
done
echo -e "Done"

echo -e "Persisting ES domain config..."
clusters=($(aws es list-domain-names 2>&1 | jq -r '.DomainNames[].DomainName'))
for i in "${clusters[@]}"; do
	if [[ "$i" == *"${environment,,}"* ]]; then
		configfile="esdomain-config.json"
		echo -e "Creating $programconfigfolder/$configfile..."
		value=$(aws es describe-elasticsearch-domain --domain-name $i)
		value=$(getval "$value")
		echo $value >>$programconfigfolder/$configfile
	fi
done
echo -e "Done"

#efs file system
echo -e "Persisting EFS filesystem config..."
configfile="efs-config.json"
echo -e "Creating $programconfigfolder/$configfile..."
allfilesystems=($(aws efs describe-file-systems | jq -c '.FileSystems[]'))
for i in "${allfilesystems[@]}"; do
	filesystemname=$(echo $i | jq -r '.Name')
	if [[ "$filesystemname" == *"${environment,,}"* ]]; then
		efsId=$(echo $i | jq -r '.FileSystemId')
		echo "Filesystemid - "$efsId
		result=$(aws efs describe-file-systems --file-system-id $efsId)
		value=$(echo $result | jq -r '.FileSystems[0]')
		value=$(getval "$value")
		echo $value >>$programconfigfolder/$configfile
	fi
done
echo -e "Done"

configfile="graphdb-cluster-config.json"
echo -e "Creating $programconfigfolder/$configfile..."
dbclusters=$(aws neptune describe-db-clusters)
echo -e "Persisting graphDB cluster config..."
echo $dbclusters >>$programconfigfolder/$configfile
echo -e "Done"

configfile="graphdb-instance-config.json"
echo -e "Creating $programconfigfolder/$configfile..."
dbinstances=$(aws neptune describe-db-instances)
echo -e "Persisting graphDB instance config..."
echo $dbinstances >>$programconfigfolder/$configfile
echo -e "Done"

echo "Infrastructure Config persistence - All Done"
echo ""
echo "Infrastructure configuration saved in $programconfigfolder"
echo ""
echo ""
