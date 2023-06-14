#!/bin/bash

#Install pip Update this for python3 and pip3
exists="$(command -v pip)"
if [ -z $exists ]; then
	# Install python3 if possible
	if [ $(command -v apt-get) ]; then
		sudo apt-get update
		sudo apt-get install -y python3-pip
	elif [ $(command -v yum) ]; then
		sudo yum install -y epel-release
		sudo yum install -u python-pip3
		sudo yum install -y gcc
	elif [ $(command -v brew) ]; then
		brew install python3
	else
		echo "Python3 must be installed prior to running this script"
		exit 1
	fi
else
	echo "* pip already installed"
fi

#Install ansible and boto
sudo pip3 install --upgrade pip
sudo pip3 install ansible==2.9.22
sudo pip3 install boto3
sudo pip3 install boto

SOURCE=$(dirname "$0")
ansible-galaxy install -r $SOURCE/../ansible/requirements.yml

echo "Ansible installed"
