#!/bin/bash -x

# VERSION=0.13.7
VERSION=1.3.3

# exists="$(command -v terraform)"
if [ -z $exists ]; then
        if [ $(command -v apt-get) ]; then
                sudo apt-get update && sudo apt-get install -y gnupg software-properties-common curl
                curl -fsSL https://apt.releases.hashicorp.com/gpg | sudo apt-key add -
                sudo apt-add-repository "deb [arch=amd64] https://apt.releases.hashicorp.com $(lsb_release -cs) main"
                sudo apt-get update && sudo apt-get install terraform=$VERSION
        elif [ $(command -v yum) ]; then
                sudo yum install -y yum-utils
                sudo yum-config-manager --add-repo https://rpm.releases.hashicorp.com/AmazonLinux/hashicorp.repo
                sudo yum -y install terraform-$VERSION
        elif [ $(command -v unzip) ]; then
                TYPE=$(uname)
                ARCH=""
                case $(uname -m) in
                    i386 | i686)   ARCH="386" ;;
                    x86_64) ARCH="amd64" ;;
                    arm)    dpkg --print-architecture | grep -q "arm64" && ARCH="arm64" || ARCH="arm" ;;
                esac

                URI=$(echo https://releases.hashicorp.com/terraform/{{VERSION}}/terraform_{{VERSION}}_{{TYPE}}_{{ARCH}}.zip | sed "s/{{VERSION}}/$VERSION/g" | sed "s/{{TYPE}}/$TYPE/g" | sed "s/{{ARCH}}/$ARCH/g" | tr '[:upper:]' '[:lower:]')
                ECHO "Getting file $URI"
                curl $URI --output terraform.zip
                unzip -j terraform.zip && rm terraform.zip
                sudo mv ./terraform /usr/local/bin
        else
                echo "Terraform failed to install"
                exit 1
        fi
else
        echo "* terraform already installed"
fi
terraform --version
