#!/bin/bash

usage() {
        echo "usage: ./one-input-message.sh [params]"
        echo ""
        echo -e "\tParams -"
        echo -e ""
        echo -e "\t\t-cf | --experimentconfigfile\t[string]\tExperiment Config file"
        echo -e ""
        echo -e "\t\t-run | --runId\t[string]\tRun ID"
        echo -e ""
        echo -e "\t\t-curi | --contentUri\t[string]\tcontent URI (s3 bucket path to schemas)"
        echo
}

function workProcessGroup() {
        state=$1

        if [[ $state == "START" ]]; then
                activestate="RUNNING"
        elif [[ $state == "STOP" ]]; then
                activestate="STOPPED"
        fi

        allpgs=$(curl -s -H 'Content-Type: application/json' -X GET $nifiurl:$nifiport/nifi-api/process-groups/root/process-groups)
        iterpgid=$(echo $allpgs | jq -r --arg pgname "$pgname" '.processGroups[] | select(.component.name==$pgname).component.id')
        #echo $iterpgid
        #echo ""

        # this is the pgid
        pgcontroltemplate=$(cat k8s-runtimes/nifi-template/pgcontrol.json)
        #echo $pgcontroltemplate | python -m json.tool
        pgcontroltemplate=$(sed "s~{{pgid}}~$iterpgid~g" <<<$pgcontroltemplate)
        pgcontroltemplate=$(sed "s~{{state}}~$activestate~g" <<<$pgcontroltemplate)
        #echo $pgcontroltemplate
        pgcontrol="'"$pgcontroltemplate"'"
        cmd="curl -s -H 'Content-type: application/json' -X PUT $nifiurl:$nifiport/nifi-api/flow/process-groups/$iterpgid -d $pgcontrol"
        #echo $cmd
        result=$(eval $cmd 2>&1)
        #echo

        #
        # implement wait-for
        #
        status=""
        echo "waiting for $pgname to $state.."
        while true; do
                echo -n "." >/dev/tty
                sleep 5

                allpgs=$(curl -s -H 'Content-Type: application/json' -X GET $nifiurl:$nifiport/nifi-api/process-groups/root/process-groups)
                #echo $allpgs
                runningCount=$(echo $allpgs | jq -r --arg pgname "$pgname" '.processGroups[] | select(.component.name==$pgname).runningCount')
                stoppedCount=$(echo $allpgs | jq -r --arg pgname "$pgname" '.processGroups[] | select(.component.name==$pgname).stoppedCount')
                echo "Running - $runningCount, stopped - $stoppedCount"
                if [[ -z $runningCount || -z $stoppedCount ]]; then
                        echo "Processor status unknown"
                        break
                fi
                if [[ $state == "START" && $runningCount == "2" ]]; then
                        break
                elif [[ $state == "STOP" && $stoppedCount == "2" ]]; then
                        break
                fi
        done
        echo "$pgname $activestate!"
        echo
}

function componentStatus() {
        # get id of the processgroup
        allpgs=$(curl -s -H 'Content-Type: application/json' -X GET $nifiurl:$nifiport/nifi-api/process-groups/root/process-groups)
        iterpgid=$(echo $allpgs | jq -r --arg pgname "$pgname" '.processGroups[] | select(.component.name==$pgname).component.id')
        #echo "Process group id: "$iterpgid
        #echo

        procname="kairos-single-input-message-generator"
        allprocs=$(curl -s -H 'Content-Type: application/json' -X GET $nifiurl:$nifiport/nifi-api/process-groups/$iterpgid/processors)
        #echo $allprocs
        genprocid=$(echo $allprocs | jq -r --arg procname "$procname" '.processors[] | select(.component.name==$procname).component.id')
        genrunstatus=$(echo $allprocs | jq -r --arg procname "$procname" '.processors[] | select(.component.name==$procname).status.aggregateSnapshot.runStatus')
        genoutflowfiles=$(echo $allprocs | jq -r --arg procname "$procname" '.processors[] | select(.component.name==$procname).status.aggregateSnapshot.flowFilesOut')

        echo
        echo "Gen Proc - $genprocid"
        echo "run status - $genrunstatus"
        echo "# of flowfiles out - $genoutflowfiles"
        echo
        echo

        procname="kairos-single-input-message-kafka-publisher"
        kafkaprocid=$(echo $allprocs | jq -r --arg procname "$procname" '.processors[] | select(.component.name==$procname).component.id')
        kafkarunstatus=$(echo $allprocs | jq -r --arg procname "$procname" '.processors[] | select(.component.name==$procname).status.aggregateSnapshot.runStatus')
        kafkaoutflowfiles=$(echo $allprocs | jq -r --arg procname "$procname" '.processors[] | select(.component.name==$procname).status.aggregateSnapshot.flowFilesOut')

        #echo
        #echo "Kafka Proc - $kafkaprocid"
        #echo "run status - $kafkarunstatus"
        #echo "# of flowfiles out - $kafkaoutflowfiles"
        #echo
        #echo
}

switchKubeContextNifi() {
        echo "Switching context to $nificlusterarn"

        # go to that cluster
        val=$(kubectl config use-context $nificlusterarn | grep error)
        if [[ ! -z $val ]]; then
                echo -e ""
                echo -e "Error switching kubectl context $nificlusterarn"
                echo -e ""
                echo -e ""
                exit 1
        fi

}

function deleteProcessGroup() {
        echo "Deleting existing process group...."
        # get id of the processgroup
        allpgs=$(curl -s -H 'Content-Type: application/json' -X GET $nifiurl:$nifiport/nifi-api/process-groups/root/process-groups)
        iterpgid=$(echo $allpgs | jq -r --arg pgname "$pgname" '.processGroups[] | select(.component.name==$pgname).component.id')
        revision=$(echo $allpgs | jq -r --arg pgname "$pgname" '.processGroups[] | select(.component.name==$pgname).revision.version')
        clientid=$(echo $allpgs | jq -r --arg pgname "$pgname" '.processGroups[] | select(.component.name==$pgname).revision.clientId')
        #echo "Process group id: "$iterpgid
        #echo "Revision: $revision"
        #echo "ClientId: $clientid"
        echo

        result=$(
                curl -s -H 'Content-Type: application/json' -X DELETE $nifiurl:$nifiport/nifi-api/process-groups/$iterpgid?version=$revision &
                clientId=$clientid
        )
        #echo $result
        echo "Done"
        echo
}

#########
########## Main - Entry point
#########

STARTTIME=$(date +%s)

while [ "$1" != "" ]; do
        case $1 in
        -cf | --experimentconfigfile)
                shift
                experimentconfigfile=$1
                ;;
        -run | --runid)
                shift
                runid=$1
                ;;
        -curi | --contenturi)
                shift
                contenturi=$1
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
        echo "-cf param is null"
        usage
        exit 1
fi
if [[ -z $runid ]]; then
        echo "-run param is null"
        usage
        runid=$(uuidgen)
fi
if [[ -z $contenturi ]]; then
        echo "-curi param is null"
        usage
        exit 1
fi

experimentFileContents=$(cat $experimentconfigfile)
program=$(echo $experimentFileContents | jq -r '.programName')
environment=$(echo $experimentFileContents | jq -r '.environment')

stsidentity=$(aws sts get-caller-identity)
awsaccount=$(echo $stsidentity | jq -r '.Account')
#echo "AWS Account - "$awsaccount
region=$(aws ec2 describe-availability-zones --output text --query 'AvailabilityZones[0].[RegionName]')
#echo "AWS Region - "$region
echo
echo "Program - $program"
echo "Environment - $environment"

nifiurl=""
nifiport="8080"
configFolder="infrastructure-config/"$program"-"${environment^^}"-config"
nificonfigFile=$configFolder"/nificonfig.json"
nificonfig=$(cat $nificonfigFile)
nifiurl=$(echo $nificonfig | jq -r '.NifiConfig.PublicDnsName')
if [[ -z $nifiurl || $nifiurl == "" || $nifiurl == null ]]; then
        echo -e ""
        echo -e "Could not find the nifi url"
        echo -e ""
        exit 1
fi

# copy the groovy script to the nifi ec2 instance
cmd="scp k8s-runtimes/nifi-template/one-input-message.script ec2-user@$nifiurl:/tmp"
echo $cmd
eval $cmd

pgname="kairos-single-input-message-nifi-process-group"
experimentsarray=($(jq -c '.experimentConfigurations[]' $experimentconfigfile))
for i in "${experimentsarray[@]}"; do
        experimentconfig=$i
        evaluator=$(echo $experimentconfig | jq -r '.evaluator')
        evaluator=${evaluator,,}
        echo "Evaluator - $evaluator"

        experiment=$(echo $experimentconfig | jq -r '.experiment.name')
        experiment=${experiment,,}
        echo "Experiment - $experiment"

        echo
        echo "Stopping..."
        # stop the process group
        workProcessGroup "STOP"

        # delete the process group
        deleteProcessGroup

        # setup the ingest
        ./setup-enclave-ingest.sh -pgm $program -env $environment -ev $evaluator -xn $experiment -run $runid -curi $contenturi

        echo "Starting ..."
        # start the process group
        workProcessGroup "START"
        componentStatus

        # wait for a few seconds and stop the components
        # we do this to avoid creating a lot of provenance data in nifi
        sleep 2
        echo
        echo "Stopping..."
        # stop the process group
        workProcessGroup "STOP"

        echo "Gen out flow file count - $genoutflowfiles"
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
