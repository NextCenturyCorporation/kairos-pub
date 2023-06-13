#!/bin/bash


cd ~

echo -e "Installing prerequisites..."
echo -e ""

yum update #  yum update
yum -y install inxi #  yum -y install inxi, did not work
yum -y install python3-pip #  yum -y install python3-pip
yum -y install jq #  yum -y install jq
yum -y install git
#yum -y install java-1.8.0
wget https://archive.apache.org/dist/kafka/2.2.1/kafka_2.12-2.2.1.tgz
tar -xzf kafka_2.12-2.2.1.tgz

echo -e ""
# verification
pip3 --version


echo ""
echo -e "Installing AWS-CLI"
# Install 
yum -y install awscli #  yum -y install awscli
pip3 install awscli --upgrade --user 
echo -e "AWS-CLI installation complete"
echo -e ""

if [ -z "$AWS_ACCESS_KEY_ID" ]
then
  echo "No Access Key found"
  exit 1
else
  aws configure set aws_access_key_id $AWS_ACCESS_KEY_ID
fi

if [ -z "$AWS_SECRET_ACCESS_KEY" ]
then
  echo "No Access Key found"
  exit 1
else
  aws configure set aws_secret_access_key $AWS_SECRET_ACCESS_KEY
fi


# Configure AWS-CLI
#---------------
#aws configure
#AWS Access Key ID [None]: [Enter aws account access key id]
#AWS Secret Access Key [None]: [Enter aws account secret access key]
#Default region name [None]: us-east-1
#Default output format [None]: json


echo "Configuring EKSCTL...."
curl --silent --location "https://github.com/weaveworks/eksctl/releases/download/latest_release/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
mv /tmp/eksctl /usr/local/bin

# verification
eksctl version


echo "Downloading Kubectl...."
#curl -o kubectl https://amazon-eks.s3-us-west-2.amazonaws.com/1.13.8/2019-08-14/bin/linux/amd64/kubectl
curl -o kubectl https://amazon-eks.s3-us-west-2.amazonaws.com/1.14.6/2019-08-22/bin/linux/amd64/kubectl

echo "Configuring KUBECTL"
# put kubectl on the path
chmod +x ./kubectl
# mkdir -p $HOME/bin && cp ./kubectl $HOME/bin/kubectl && export PATH=$HOME/bin:$PATH
echo "Copying kubectl to /usr/bin"
cp ./kubectl /usr/bin/kubectl
echo 'export PATH=$HOME/bin:$PATH' >> ~/.bashrc
echo 'export PATH=$HOME/bin:$PATH' >> ~/.bash_profile

#echo ""
#echo "Download policy file"
#curl -O https://raw.githubusercontent.com/kubernetes-sigs/aws-alb-ingress-controller/v1.1.3/docs/examples/iam-policy.json
# create ALB policy
#aws iam create-policy --policy-name ALBIngressControllerIAMPolicy --policy-document file://iam-policy.json
#echo ""


echo ""
echo ""
# verification
kubectl version
echo ""
echo ""



echo "Installing Helm 3"
curl https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 > get_helm.sh
chmod 700 get_helm.sh
./get_helm.sh


echo ""

echo "Creating AWS Lambda execution role"
aws iam create-role --role-name kairos-lambda-role --assume-role-policy-document ./k8s/kairos-lambda-role.json

# {
#    "Role": {
#        "Path": "/",
#        "RoleName": "kairos-lambda-role",
#        "RoleId": "AROAR42EFPRJBTGHKS2IK",
#        "Arn": "arn:aws:iam::130602597458:role/kairos-lambda-role",
#        "CreateDate": "2020-01-16T17:40:13Z",
#        "AssumeRolePolicyDocument": {
#            "Version": "2012-10-17",
#            "Statement": [
#                {
#                    "Effect": "Allow",
#                    "Principal": {
#                        "AWS": "*"
#                    },
#                    "Action": "sts:AssumeRole"
#                }
#            ]
#        }
#    }
# }


echo "Attaching lambda policies to role"
aws iam attach-role-policy --role-name kairos-lambda-role --policy-arn arn:aws:iam::aws:policy/AWSLambdaFullAccess
aws iam attach-role-policy --role-name kairos-lambda-role --policy-arn arn:aws:iam::aws:policy/service-role/AWSCodeDeployRoleForLambda
aws iam attach-role-policy --role-name kairos-lambda-role --policy-arn arn:aws:iam::aws:policy/AWSLambdaExecute
aws iam attach-role-policy --role-name kairos-lambda-role --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaSQSQueueExecutionRole
aws iam attach-role-policy --role-name kairos-lambda-role --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
aws iam attach-role-policy --role-name kairos-lambda-role --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole
aws iam attach-role-policy --role-name kairos-lambda-role --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaRole
aws iam attach-role-policy --role-name kairos-lambda-role --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaENIManagementAccess
echo ""

echo ""
echo "Installing eksctl"
curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
mv /tmp/eksctl /usr/local/bin

echo ""
eksctl version
echo ""
    
echo ""
echo "Done"
echo ""
echo ""
echo "Prerequisites installation complete."
echo ""


