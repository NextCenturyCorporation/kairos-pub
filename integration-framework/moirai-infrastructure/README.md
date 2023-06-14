# Application Setup

## Application Deployment

New repo Install order

### Important Configurations (before you get started)

1. all resources generated from this project white list IP addresses for an extra layer of security. Add any corporate/personal IP adresses to allow access terraform\environment-tf\variables.tf 

2. If using the Docker-repo-tf set the registry admin password insid terraform\docker-repo-tf\scripts\docker-registry.sh

3. For Zeus (Beachhead) application the Relational Database Server password needs to be set. There are two different files, differing in the tier (i.e. Test, Stage, Production)
    * For lower tiers (Test, Staging) set the variable default 'db_secret_lower_tier' in terraform\rds-lower-tiers-tf\variables.tf
    * For production tier set the variable default 'db_secret' in terraform\rds-tf\variables.tf

4. In ansible/group_vars/vars_file.yml set any missing variables such as cert_email. These need to be filled out before the scripts can run

### How to setup your local Unix environment setup. (You probably need this)

1. First you need to create an account in aws that has acccess to run scripts, preferably an admin. Setup awscli locally with the credential keys for this user under the profile name "kairos"
    "kairos" is the profile many of the scripts are set to use.

2. install mysql-client (not mandatory but useful)

3. Install ansible by running 'install/install_ansible.sh' (This includes python3, pip3, gcc, epel-release)

4. Install terraform by running 'install/install_terraform.sh' It is imperative that you use the version in the script!

4. Run 'ansible-playbook -i inventory kairos-keys.yml' 

5. You also need to install jq to run the k8s stuff, there is no script for this yet


### How to setup the aws environment (You probably don't need this, this is once per project).

Locally run the following...

1. Run 'ansible/kairos-infrastructure.yml' either using the full command 'ansible-playbook -i inventory kairos-infrastructure.yml' or simply './kairos-infrastructure.yml'
    This script create the s3 buckets to store terraform states.

2. Run 'ansible/kairos-keys.yml' either using the full command 'ansible-playbook -i inventory kairos-keys.yml' or simply './kairos-keys.yml'

2. Go to environment-tf, run 'terraform init' and 'terraform apply'
    This terraform scripts sets up all the roles/policies, security groups, additional s3 buckets
    * Note: To add personal IP addresses modify 'employee_cidrs' in the variables-tf file
    * Note: To add work related IP addresses modify 'employee_cidrs' in the variables-tf file

3. Go to rds-tf, run 'terraform init' and 'terraform apply' (Production)
    This terraform script will setup the database used for the main Zeus application to store information

4. Go to rds-lower-tiers, run 'terraform init' and 'terraform apply'
    This terraform script will setup the database used for the main Zeus application to store informatio

5. Go to beachhead-tf, run 'terraform init' and 'terraform apply'
    This terraform script sets up our beachhead which 
    1. sets up the environment for services
    2. starts zeus and ui applications

