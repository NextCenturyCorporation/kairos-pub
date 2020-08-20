#!/bin/bash

# build src code
./gradlew clean build

# docker prep area
mkdir -p docker; cd docker; cp ../TA3AidaPerfStarter.jar .

echo "Building docker image"
docker build -t ta3aidaperfstarter:latest .

# enter performername and userid/pwd provided by the NextCentury Kairos team
performername=""
userid=""
password=""

# kairos docker repo
server="${performername}.docker-registry.kairos.nextcentury.com"
docker logout $server 
docker login -u ${userid} -p ${password} $server

# tag the image with a version
image="ta3aidaperfstarter-1.0"

echo "Tagging docker image to Kairos Docker Repo - $server/$image"
docker tag ta3aidaperfstarter:latest $server"/"$image

echo "Pushing"
docker push $server"/"$image
echo ""

echo ""
echo -e ""
echo -e "All done"
echo -e ""

