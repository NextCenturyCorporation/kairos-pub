#!/bin/bash

usage()
{
    echo "usage: ./delete-enclave.sh [params]"
    echo ""
    echo -e "\tParams -"
    echo -e ""
    echo -e "\t\t-cf | --experimentconfigfile\t[string]\texperiment config file of the experiment being performed"
}

beginHeader() {
        echo -e ""
        echo -e "########################################################################################################################"
        echo -e "\tEnclave tear-down begin...."
        echo -e "########################################################################################################################"
        echo -e ""
}

endHeader() {
        echo -e ""
        echo -e "########################################################################################################################"
        echo -e "\tEnclave teardown complete...."
        echo -e "#######################################################################################################################"
        echo -e ""
}

switchKubeContextPerfEval() {
        echo "Switching context to $perfevalclusterarn"

        # go to that cluster
        val=$(kubectl config use-context $perfevalclusterarn 2>&1 | grep error)
        if [[ ! -z $val ]]
        then
                echo -e ""
                echo -e "Error switching kubectl context $perfevalclusterarn"
                echo -e ""
                echo -e ""
                exit 1
        fi
}

switchKubeContextSubmissionIngest() {
        echo "Switching context to $submissioningestclusterarn"

        # go to that cluster
        val=$(kubectl config use-context $submissioningestclusterarn 2>&1 | grep error)
        if [[ ! -z $val ]]
        then
                echo -e ""
                echo -e "Error switching kubectl context $submissioningestclusterarn"
                echo -e ""
                echo -e ""
                exit 1
        fi
}

#########
########## Main - Entry point
#########

#
# global defaults
#
clustername=""

STARTTIME=$(date +%s)


while [ "$1" != "" ]; do
    case $1 in
        -cf | --experimentconfigfile )   shift
                           experimentconfigfile=$1
                           ;;
        -h | --help )           usage
                                exit
                                ;;
        * )                     usage
                                exit 1
    esac
    shift
done

if [[ -z $experimentconfigfile ]]
then
        usage; exit 1
fi

stsidentity=$(aws sts get-caller-identity)
awsaccount=$(echo $stsidentity | jq -r '.Account')
#echo "AWS Account - "$awsaccount
#region="us-east-1"
region=$(aws ec2 describe-availability-zones --output text --query 'AvailabilityZones[0].[RegionName]')
#echo "AWS Region - "$region

experimentFileContents=$(cat $experimentconfigfile)
program=$(echo $experimentFileContents | jq -r '.programName')
experiment=$(echo $experimentFileContents | jq -r '.experiment')
environment=$(echo $experimentFileContents | jq -r '.environment')

perfevalclustername=$program"-"$environment"-PerfEvalk8s"
#perfevalclustername=$program"-"$environment"-GPU-PerfEvalk8s"
perfevalclusterarn="arn:aws:eks:$region:$awsaccount:cluster/$perfevalclustername"

submissioningestclustername=$program"-"$environment"-SubmissionIngestk8s"
submissioningestclusterarn="arn:aws:eks:$region:$awsaccount:cluster/$submissioningestclustername"

# go to that cluster
val=$(kubectl config use-context $perfevalclusterarn | grep error)
if [[ ! -z $val ]]
then
        echo -e ""
        echo -e "Error switching kubectl context $perfevalclusterarn"
        echo -e ""
        echo -e ""
        exit 1
fi

configFolder="infrastructure-config/"$program"-"${environment^^}"-config"
kafkaconfigFile=$configFolder"/kafka-config.json"
config=$(cat $kafkaconfigFile)

mskclusterarn=$(echo $config | jq -r '.ClusterArn')
if [[ -z $mskclusterarn || $mskclusterarn == "" || $mskclusterarn == null ]]
then
        echo -e ""
        echo -e "Could not find kafka cluster information $mskclusterarn"
        echo -e ""
        exit 1
fi
mskdescription=$(aws kafka get-bootstrap-brokers --cluster-arn $mskclusterarn)
kafkabrokers=$(echo $mskdescription | jq -r '.BootstrapBrokerString')

#configFolder="infrastructure-config/"$program"-"${environment^^}"-config"
#esdomainconfigFile=$configFolder"/esdomain-config.json"
#config=$(cat $esdomainconfigFile)
#domainname=$(echo $config | jq -r '.DomainStatus.DomainName')
#esdomainendpoint=$(echo $config | jq -r '.DomainStatus.Endpoints.vpc')

# for each experiment in the experiment config file
experimentsarray=( $(jq -c '.experimentConfigurations[]' $experimentconfigfile) )
for i in "${experimentsarray[@]}"
do
	beginHeader

        experiment=$i
	evaluator=$(echo $experiment | jq -r '.evaluator')
	echo "Evaluator - "$evaluator
	experimentname=$(echo $experiment | jq -r '.experiment.name')
	echo "Experiment - "$experimentname
	
	if [[ -z $evaluator || -z $experimentname ]]
	then
        	echo "Evaluator and/or experiment not found!"; exit 1
	fi

	namespacename="${evaluator,,}-${experimentname,,}-enclave"

	performersarray=( $(echo $experiment | jq -c '.performers[]') )
        for j in "${performersarray[@]}"
        do
                echo ""
                performer=$j
                #echo $performer
                performername=$(echo $performer | jq -r '.performername')
                echo -e "performername - $performername"
		kubectl delete secret ${performername,,}dockerrepocred -n $namespacename
	done

	echo ""
	echo "Deleting services, claims, cronjobs in namespace $namespacename"
	#kubectl delete --all pvc --namespace=$namespacename --grace-period=0 --force
	#kubectl delete --all svc --namespace=$namespacename
	#kubectl delete --all job --namespace=$namespacename  --grace-period=0 --force
	kubectl -n $namespacename delete job,pod --all --grace-period=0 --force


	# edit the pv to make it available again
	#kubectl patch pv ${evaluator,,}"-"${experimentname,,}"-efs-pv" -p '{"spec":{"claimRef": null}}'
	echo ""

	echo ""
	echo "Deleting enclave namespace - $namespacename"
	kubectl delete namespace $namespacename #--grace-period=0 --force

	echo ""
	echo "Deleting pv.."
        kubectl delete pv ${evaluator,,}-${experimentname,,}-efs-pv
	echo ""

	# delete kafka topics
	#./delete-kafka-topics.sh -arn $mskclusterarn -en $evaluator -xn $experimentname

	echo "Deleting Submission ingest containers...."
        # switch to submissioningest cluster
        switchKubeContextSubmissionIngest
        ns="${evaluator,,}-${experimentname,,}-submissioningest"
        echo "Deleting enclave namespace $ns"
        kubectl delete namespace $ns
        echo "Done"
        # switch back to perfeval cluster
        switchKubeContextPerfEval
        echo ""

	# delete the enclaves folder
        rm -rfv "enclaves/$environment/"$evaluator"-"$experimentname

	endHeader
done

# need to delete the experimentmonitor?
#experimentsleft=$(kubectl get namespaces | grep "-enclave" | awk '{print $1}')
#if [[ -z $experimentsleft || $experimentsleft == "" || $experimentsleft == null ]]
#then
#	echo "No more experiments remaining on this cluster, cleaning up experiment monitor also"
#	kubectl delete --all deployments,pods,svc,ing -n kairos-experimentmonitor
#	kubectl delete namespace kairos-experimentmonitor
#fi

ENDTIME=$(date +%s)

echo ""
echo ""
echo ""
secs=$((ENDTIME-STARTTIME))
echo "It took $secs seconds to complete this task..."
echo ""
hrs=$(($secs/3600))
min=$(($secs/60))
secsleft=$(($secs-$min*60))

printf '%dh:%dm:%ds\n' $hrs $min $(($secsleft))
echo ""
echo ""
