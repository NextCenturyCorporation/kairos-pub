#!/bin/bash

nifiec2ip=$1
if [[ -z $nifiec2ip ]]; then
        echo
        echo
        echo "Nifi standalone EC2 ip not set!!"
        echo
        exit 1
fi

function modNifiProperty() {
        thekey=$1
        newvalue=$2

        echo "Filename - $filename: Key - $thekey, Value - $newvalue"
        echo
        if ! grep -R "^[#]*\s*${thekey}=.*" $filename > /dev/null; then
                echo "APPENDING because '${thekey}' not found"
                echo "$thekey=$newvalue" >> $filename
        else
                echo "SETTING because '${thekey}' found already"
                sed -ir "s/^[#]*\s*${thekey}=.*/$thekey=$newvalue/" $filename
        fi
}

function remoteexecute() {
        #pem file path
        pemfile="~/kairos-key.pem"

        #install amazon linux extras
        ssh -i $pemfile ec2-user@$nifiec2ip $1
}
############################################
# Main Entry Point
############################################

echo "Deploying nifi to this instance...."

configfile="global-config.json"
if [[ ! -f "$configfile" ]]; then
	echo
	echo
	echo "Kairos global config file $configfile does not exist. Please check the Kairos gitlab folder at https://gitlab.kairos.nextcentury.com/darpa-kairos/moirai-infrastructure/tree/k8snamespace/k8s"
	echo
	exit 1
fi

#read in the config file
config=$(cat $configfile)

#pem file path
pemfile="~/kairos-key.pem"

#install amazon linux extras
remoteexecute "sudo yum -y install -y amazon-linux-extras"

# install java remote
javajdk=$(echo $config | jq -r '.Versions.java.jdk')
javaversion=$(echo $config | jq -r '.Versions.java.version')
echo "Installing jdk"
remoteexecute "sudo amazon-linux-extras install -y java-$javajdk$javaversion"
echo "done"

echo "Setting java_home"
remoteexecute "export JAVA_HOME=\"/usr/lib/jvm/java-11-openjdk-11.0.9.11-0.amzn2.0.1.x86_64\""
remoteexecute "echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc"
remoteexecute "echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bash_profile"
echo


echo "Installing nifi.."
nifiversion=$(echo $config | jq -r '.Versions.nifi.standalone')
ssh -i $pemfile ec2-user@$nifiec2ip "wget https://apache.claz.org/nifi/$nifiversion/nifi-$nifiversion-bin.tar.gz; tar -xvf nifi-$nifiversion-bin.tar.gz"
echo "done"

nifihome="nifi-$nifiversion"
filepath="$nifihome/conf/"
filename="nifi.properties"
fqfilename="/home/ec2-user/$filepath$filename"

echo "Download nifi.properties.."
# bring the file down
scp -i $pemfile ec2-user@$nifiec2ip:$fqfilename .

echo "Modifying nifi properties..."
modNifiProperty "nifi.web.http.host" $nifiec2ip
modNifiProperty "nifi.web.http.port" 8080
modNifiProperty "nifi.provenance.repository.max.storage.time" "3 days"
modNifiProperty "nifi.content.repository.archive.max.retention.period" "3 days"
echo "Done"
echo

echo "Upload nifi.properties..."
# upload the modified file back
scp -i $pemfile $filename ec2-user@$nifiec2ip:$filepath
# cleanup local copy of nifi.properties
rm -rfv nifi.properties

echo "Copy one-input-message.script to ec2 instance"
scp -i $pemfile k8s-runtimes/nifi-template/one-input-message.script ec2-user@$nifiec2ip:/tmp/

#Start/stop nifi
#----------------
#bin/nifi.sh start --wait-for-init - Launches the application to run the background
#bin/nifi.sh status - Check the status
#bin/nifi.sh stop - Shutdown the application
#bin/nifi.sh run - Launches the application to run in the foreground

echo
echo "Starting nifi.."
ssh -i $pemfile ec2-user@$nifiec2ip "$nifihome/bin/nifi.sh start --wait-for-init"

echo ""
echo "All Done - nifi url - http://$nifiec2ip:8080/nifi/"
echo ""
echo ""


