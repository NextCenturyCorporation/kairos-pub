#!/bin/bash

usage() {
        echo "usage: ./setup-enclave-ingest.sh [params]"
        echo ""
        echo -e "\tParams -"
        echo -e ""
        echo -e "\t\t-pgm | --Program\t[string]\tProgram name"
        echo -e ""
        echo -e "\t\t-env | --environment\t[string]\tenvironment name"
        echo -e ""
        echo -e "\t\t-ev | --evaluatorname\t[string]\tevaluator name"
        echo -e ""
        echo -e "\t\t-xn | --experimentname\t[string]\texperiment name"
        echo -e ""
        echo -e "\t\t-run | --runId\t[string]\tRun ID"
        echo -e ""
        echo -e "\t\t-curi | --contentUri\t[string]\tOptional\tcontent URI (s3)"
        echo
}

function updateGenProcessor() {
        procname=$1
        procid=$2
        revision=$3
        clientId=$4
        echo "Updating ..."

        updateproctemplate=$(cat k8s-runtimes/nifi-template/create-execscript.json)
        #echo $updateproctemplate
        #
        # important - this prevents "*" expansion to file list !!!!!!!!!
        #
        set -f
        updateproctemplate=$(echo $updateproctemplate | jq '.component.bundle.version = "'$nificomponentversion'"')
        updateproctemplate=$(echo $updateproctemplate | jq '.component.name = "'$procname'"')
        updateproctemplate=$(echo $updateproctemplate | jq '.component.id = "'$procid'"')
        revision=$((revision + 1))
        updateproctemplate=$(echo $updateproctemplate | jq '.revision.version = "'$revision'"')
        updateproctemplate=$(echo $updateproctemplate | jq '.revision.clientId = "'$clientId'"')

        # params
        updateproctemplate=$(echo $updateproctemplate | jq '.component.config.properties.program = "'${program,,}'"')
        updateproctemplate=$(echo $updateproctemplate | jq '.component.config.properties.environment = "'${environment,,}'"')
        updateproctemplate=$(echo $updateproctemplate | jq '.component.config.properties.evaluator = "'${evaluatorname,,}'"')
        updateproctemplate=$(echo $updateproctemplate | jq '.component.config.properties.experiment = "'${experimentname,,}'"')
        updateproctemplate=$(echo $updateproctemplate | jq '.component.config.properties.runId = "'$runId'"')
        updateproctemplate=$(echo $updateproctemplate | jq '.component.config.properties.contentUri = "'$contentUri'"')

        #echo $updateproctemplate
        updateproc="'"$updateproctemplate"'"
        cmd="curl -s -H 'Content-type: application/json' -X PUT \"$nifiendpoint/nifi-api/processors/$procid\" -d $updateproc"
        #echo $cmd
        echo
        genresult=$(eval $cmd 2>&1)

        #debug
        #echo $genresult
        #echo $genresult | python -m json.tool
        set +f
        echo
}

function updateTerminalProcessor() {
        procname=$1
        procid=$2
        revision=$3
        clientId=$4
        echo "Updating ..."
        updateproctemplate=$(cat k8s-runtimes/nifi-template/create-kafka-processor.json)
        updateproctemplate=$(echo $updateproctemplate | jq '.component.bundle.version = "'$nificomponentversion'"')
        updateproctemplate=$(echo $updateproctemplate | jq '.component.name = "'$procname'"')
        updateproctemplate=$(echo $updateproctemplate | jq '.component.id = "'$procid'"')
        revision=$((revision + 1))
        updateproctemplate=$(echo $updateproctemplate | jq '.revision.version = "'$revision'"')
        updateproctemplate=$(echo $updateproctemplate | jq '.revision.clientId = "'$clientId'"')
        updateproctemplate=$(echo $updateproctemplate | jq '.component.config.properties.topic = "'$inputtopic'"')
        updateproctemplate=$(sed "s~{{kafkabrokers}}~${kafkabrokers}~g" <<<$updateproctemplate)

        updateproc="'"$updateproctemplate"'"
        cmd="curl -s -H 'Content-type: application/json' -X PUT \"$nifiendpoint/nifi-api/processors/$procid\" -d $updateproc"
        #echo $cmd
        echo
        kafkaresult=$(eval $cmd 2>&1)

        #debug
        #echo $kafkaresult
        #echo $kafkaresult | python -m json.tool
        echo
}

createNifiComponents() {

        # create a process group if one has not already been created
        echo
        allpgs=$(curl -s -H 'Content-Type: application/json' -X GET $nifiendpoint/nifi-api/process-groups/root/process-groups)
        iterpgname=$(echo $allpgs | jq -r --arg pgname "$pgname" '.processGroups[] | select(.component.name==$pgname).component.name')
        if [[ -z $iterpgname || $iterpgname == null || $iterpgname == "" ]]; then
                echo "Process group $pgname does not exist, creating..."
                echo
                createpgtemplate="$(cat k8s-runtimes/nifi-template/create-process-group.json)"
                createpgtemplate=$(echo $createpgtemplate | jq '.component.name = "'$pgname'"')
                createpg="'"$createpgtemplate"'"
                cmd="curl -s -H 'Content-type: application/json' -X POST \"$nifiendpoint/nifi-api/process-groups/root/process-groups\" -d $createpg"
                result=$(eval $cmd 2>&1)

                #debug
                #echo $result | python -m json.tool
        else
                echo
                echo "Process group $pgname already exists!!"
                echo
        fi

        # get id of the processgroup
        allpgs=$(curl -s -H 'Content-Type: application/json' -X GET $nifiendpoint/nifi-api/process-groups/root/process-groups)
        iterpgid=$(echo $allpgs | jq -r --arg pgname "$pgname" '.processGroups[] | select(.component.name==$pgname).component.id')
        echo "Process group id: "$iterpgid
        echo

        genprocid=""
        kafkaprocid=""
        needconnection=false

        # create generateFlow processor
        procname="kairos-single-input-message-generator"
        allprocs=$(curl -s -H 'Content-Type: application/json' -X GET $nifiendpoint/nifi-api/process-groups/$iterpgid/processors)
        #echo $allprocs
        iterprocname=$(echo $allprocs | jq -r --arg procname "$procname" '.processors[] | select(.component.name==$procname).component.name')
        if [[ -z $iterprocname || $iterprocname == null || $iterprocname == "" ]]; then
                echo "Processor $procname in processgroup $pgname does not exist, creating..."
                createproctemplate=$(cat k8s-runtimes/nifi-template/create-execscript.json)
                #echo $createproctemplate
                #
                # important - this prevents "*" expansion to file list !!!!!!!!!
                #
                set -f
                createproctemplate=$(echo $createproctemplate | jq '.component.bundle.version = "'$nificomponentversion'"')
                createproctemplate=$(echo $createproctemplate | jq '.component.name = "'$procname'"')
                # params
                createproctemplate=$(echo $createproctemplate | jq '.component.config.properties.program = "'${program,,}'"')
                createproctemplate=$(echo $createproctemplate | jq '.component.config.properties.environment = "'${environment,,}'"')
                createproctemplate=$(echo $createproctemplate | jq '.component.config.properties.evaluator = "'${evaluatorname,,}'"')
                createproctemplate=$(echo $createproctemplate | jq '.component.config.properties.experiment = "'${experimentname,,}'"')
                createproctemplate=$(echo $createproctemplate | jq '.component.config.properties.runId = "'$runId'"')

                #createproctemplate=$(echo $createproctemplate | jq '.component.config.properties.contentUri = "'$contentUri'"')
                if ! [[ -z $contentUri || $contentUri == null || $contentUri == "" ]]; then
                        createproctemplate=$(echo $createproctemplate | jq '.component.config.properties += {"contentUri" : "'$contentUri'"}')
                fi

                #echo $createproctemplate
                createproc="'"$createproctemplate"'"
                cmd="curl -s -H 'Content-type: application/json' -X POST \"$nifiendpoint/nifi-api/process-groups/$iterpgid/processors\" -d $createproc"
                #echo $cmd
                echo
                genresult=$(eval $cmd 2>&1)

                #debug
                #echo $genresult
                #echo $genresult | python -m json.tool

                set +f

                genprocid=$(echo $genresult | jq -r '.id')
                genprocgroupid=$(echo $genresult | jq -r '.component.parentGroupId')
                needconnection=true
                echo "Done"
                echo
        else
                echo
                echo "Processor $iterprocname in process group $pgname already exists!!"

                genprocid=$(echo $allprocs | jq -r --arg procname "$procname" '.processors[] | select(.component.name==$procname).component.id')
                genprocgroupid=$(echo $allprocs | jq -r --arg procname "$procname" '.processors[] | select(.component.name==$procname).component.parentGroupId')
                revision=$(echo $allprocs | jq -r --arg procname "$procname" '.processors[] | select(.component.name==$procname).revision.version')
                clientid=$(echo $allprocs | jq -r --arg procname "$procname" '.processors[] | select(.component.name==$procname).revision.clientId')

                updateGenProcessor $procname $genprocid $revision $clientid
        fi
        echo

        # create terminal processor
        procname="kairos-single-input-message-kafka-publisher"
        # create a processor if one has not already been created
        allprocs=$(curl -s -H 'Content-Type: application/json' -X GET $nifiendpoint/nifi-api/process-groups/$iterpgid/processors)
        iterprocname=$(echo $allprocs | jq -r --arg procname "$procname" '.processors[] | select(.component.name==$procname).component.name')
        if [[ -z $iterprocname || $iterprocname == null || $iterprocname == "" ]]; then
                echo "Processor $procname in processgroup $pgname does not exist, creating..."
                createproctemplate=$(cat k8s-runtimes/nifi-template/create-kafka-processor.json)

                #createproctemplate=$(sed "s~{{kafkapublisher}}~${procname}~g" <<< $createproctemplate)
                #createproctemplate=$(sed "s~{{kafkabrokers}}~${kafkabrokers}~g" <<< $createproctemplate)
                #createproctemplate=$(sed "s~{{experimentinputtopic}}~${inputtopic}~g" <<< $createproctemplate)

                createproctemplate=$(echo $createproctemplate | jq '.component.bundle.version = "'$nificomponentversion'"')
                createproctemplate=$(echo $createproctemplate | jq '.component.name = "'$procname'"')
                createproctemplate=$(echo $createproctemplate | jq '.component.config.properties.topic = "'$inputtopic'"')
                createproctemplate=$(sed "s~{{kafkabrokers}}~${kafkabrokers}~g" <<<$createproctemplate)

                createproc="'"$createproctemplate"'"
                cmd="curl -s -H 'Content-type: application/json' -X POST \"$nifiendpoint/nifi-api/process-groups/$iterpgid/processors\" -d $createproc"
                #echo $cmd
                echo
                kafkaresult=$(eval $cmd 2>&1)

                #debug
                #echo $kafkaresult | python -m json.tool

                kafkaprocid=$(echo $kafkaresult | jq -r '.id')
                kafkaprocgroupid=$(echo $kafkaresult | jq -r '.component.parentGroupId')
                needconnection=true
                echo "Done"
                echo
        else
                echo
                echo "Processor $iterprocname in process group $pgname already exists!!"

                kafkaprocid=$(echo $allprocs | jq -r --arg procname "$procname" '.processors[] | select(.component.name==$procname).component.id')
                kafkaprocgroupid=$(echo $allprocs | jq -r --arg procname "$procname" '.processors[] | select(.component.name==$procname).component.parentGroupId')
                revision=$(echo $allprocs | jq -r --arg procname "$procname" '.processors[] | select(.component.name==$procname).revision.version')
                clientid=$(echo $allprocs | jq -r --arg procname "$procname" '.processors[] | select(.component.name==$procname).revision.clientId')

                updateTerminalProcessor $procname $kafkaprocid $revision $clientid
        fi
        echo

        if [[ $needconnection == true ]]; then
                echo
                echo "Connecting up the \"success\" gen processor $genprocid and kafka processor $kafkaprocid"
                #"sourceId": "value",
                #"destinationId": "value",
                # connect the generate flow processot and the terminal kafka processor
                createconntemplate=$(cat k8s-runtimes/nifi-template/create-success-connection.json)
                createconntemplate=$(echo $createconntemplate | jq '.component.source.id = "'$genprocid'"')
                createconntemplate=$(echo $createconntemplate | jq '.component.source.groupId = "'$genprocgroupid'"')
                createconntemplate=$(echo $createconntemplate | jq '.component.destination.id = "'$kafkaprocid'"')
                createconntemplate=$(echo $createconntemplate | jq '.component.destination.groupId = "'$kafkaprocgroupid'"')
                createconn="'"$createconntemplate"'"
                cmd="curl -s -H 'Content-type: application/json' -X POST \"$nifiendpoint/nifi-api/process-groups/$iterpgid/connections\" -d $createconn"
                result=$(eval $cmd 2>&1)
                #echo $result | python -m json.tool
        else
                echo
                echo "Connection exists, no need to create new one!!"
                echo
        fi

        echo "Done"
        echo
        echo
}

beginHeader() {
        echo -e ""
        echo -e "########################################################################################################################"
        echo -e "\tEnclave Nifi setup begin...."
        echo -e "########################################################################################################################"
        echo -e ""
}

endHeader() {
        echo -e ""
        echo -e "########################################################################################################################"
        echo -e "\tEnclave Nifi setup complete...."
        echo -e "#######################################################################################################################"
        echo -e ""
}

#########
########## Main - Entry point
#########

#
# global defaults
#
nificomponentversion="1.13.2"

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
        -ev | --evaluator)
                shift
                evaluatorname=$1
                ;;
        -xn | --experimentname)
                shift
                experimentname=$1
                ;;
        -run | --runId)
                shift
                runId=$1
                ;;
        -curi | --contentUri)
                shift
                contentUri=$1
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

# lowercase
experimentname=${experimentname,,}
evaluatorname=${evaluatorname,,}

if [[ -z $program || -z $environment || -z $experimentname || -z $evaluatorname || -z $runId ]]; then
        usage
        exit 1
fi

nifiurl="nifi.${program,,}${environment,,}.kairos.nextcentury.com"
nifiport="8080"
nifiendpoint=$nifiurl:$nifiport

configFolder="infrastructure-config/"$program"-"${environment^^}"-config"

kafkaconfigFile=$configFolder"/kafka-config.json"
config=$(cat $kafkaconfigFile)

mskclusterarn=$(echo $config | jq -r '.ClusterArn')
if [[ -z $mskclusterarn || $mskclusterarn == "" || $mskclusterarn == null ]]; then
        echo -e ""
        echo -e "Could not find kafka cluster information $mskclusterarn"
        echo -e ""
        exit 1
fi
mskdescription=$(aws kafka get-bootstrap-brokers --cluster-arn $mskclusterarn)
kafkabrokers=$(echo $mskdescription | jq -r '.BootstrapBrokerString')

echo ""

#beginHeader

# for each evaluator-experiment, establish a nifi bucket
echo "Evaluator - $evaluatorname"
echo "Experiment - $experimentname"

pgname="kairos-single-input-message-nifi-process-group"

inputtopic=${evaluatorname,,}"-"${experimentname,,}"-input-topic"
outputtopic=${evaluatorname,,}"-"${experimentname,,}"-output-topic"
errortopic=${evaluatorname,,}"-"${experimentname,,}"-error-topic"

# upload exec script to nifi nodes
#
# TBD: make this dynamic
#
#
#kubectl cp k8s-runtimes/nifi-template/one-input-message.script nifi/nifi-0:/tmp -c server
#kubectl cp k8s-runtimes/nifi-template/one-input-message.script nifi/nifi-1:/tmp -c server
#kubectl cp k8s-runtimes/nifi-template/one-input-message.script nifi/nifi-2:/tmp -c server

createNifiComponents

#endHeader
echo
echo ""
echo ""
