#! /bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

images=$(aws ecr list-images --repository-name protozeus | jq '.imageIds | .[]' )
echo $images
echo "${#images}"

docker build -t 130602597458.dkr.ecr.us-east-1.amazonaws.com/protozeus:1.0.0 -f $DIR/Dockerfile $DIR/..

$DIR/../gradlew ECRLogin
docker push 130602597458.dkr.ecr.us-east-1.amazonaws.com/protozeus:1.0.0
$DIR/../gradlew ECRLogout
