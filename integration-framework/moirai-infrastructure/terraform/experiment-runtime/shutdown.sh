#! /bin/bash

curdir=$(pwd)
SOURCE=$(dirname ${BASH_SOURCE[0]})

cd $SOURCE
SOURCE_DIR=$(pwd)

cd $SOURCE_DIR/experiment-perfeval-framework
terraform destroy --auto-approve

cd $SOURCE_DIR/experiment-perfeval
terraform destroy --auto-approve

# cd $SOURCE_DIR/experiment-env
# terraform destroy --auto-approve

cd $curdir
