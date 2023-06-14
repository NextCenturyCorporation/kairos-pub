#! /bin/bash

curdir=$(pwd)
SOURCE=$(dirname ${BASH_SOURCE[0]})
cd $SOURCE
SOURCE_DIR=$(pwd)
cd $SOURCE_DIR/experiment-env
terraform apply --auto-approve
cd $SOURCE_DIR/experiment-perfeval
terraform apply --auto-approve
cd $SOURCE_DIR/experiment-perfeval-framework
terraform apply --auto-approve

cd $curdir
