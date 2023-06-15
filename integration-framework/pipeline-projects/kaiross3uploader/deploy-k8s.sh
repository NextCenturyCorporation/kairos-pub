#!/bin/bash

#./gradlew clean build

mkdir -p docker; cd docker; 

echo "Building docker image"
docker build -t kairos-s3uploader:latest .

echo ""

echo "Authenticating docker to AWS"
#dockerAuth=$(aws ecr get-login-password)
#echo $dockerAuth
#eval $dockerAuth
aws ecr get-login-password | docker login --username AWS --password-stdin 130602597458.dkr.ecr.us-east-1.amazonaws.com
echo ""


echo "Tagging and pushing docker image to AWS ECR - 130602597458.dkr.ecr.us-east-1.amazonaws.com/kairos-runtimes:kairos-s3uploader-1.0"
docker tag kairos-s3uploader:latest 130602597458.dkr.ecr.us-east-1.amazonaws.com/kairos-runtimes:kairos-s3uploader-1.0
docker push 130602597458.dkr.ecr.us-east-1.amazonaws.com/kairos-runtimes:kairos-s3uploader-1.0

# change back to the root folder
#cd ..

#kubectl create namespace kairos-namespace;

#echo -e ""
#echo -e "Deploying to k8s...."
#echo -e ""


# apply everything in the docker folder
#kubectl apply -f docker -n kairos-namespace

#skaffold deploy -n lorelei-namespace -f skaffold.yaml
#skaffold run -n lorelei-namespace -f skaffold.yaml

echo -e ""
echo -e "All done"
echo -e ""

