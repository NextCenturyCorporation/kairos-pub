#cloud-config
hostname: k8s-beachhead

package_update: true
package_upgrade: true
packages:
- python3-boto3

runcmd:
- 'sudo yum -y update'
- 'sudo yum -y install inxi'
- 'sudo yum -y install python${pythonversion}-pip'
- 'sudo yum -y install jq'
- 'sudo wget https://github.com/mikefarah/yq/releases/download/v${yqversion}/yq_linux_386 -O /usr/bin/yq && sudo chmod +x /usr/bin/yq'

- 'sudo yum -y install git'

- 'sudo amazon-linux-extras install -y java-${javajdk}${javaversion}'

- 'sudo yum -y install htop'

- 'wget https://archive.apache.org/dist/kafka/2.8.0/kafka_${kafkaversion}.tgz && tar -xzf kafka_${kafkaversion}.tgz'
- 'sudo mv kafka_${kafkaversion} /kafka'

- 'curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscli${awscliversion}.zip"'
- 'unzip -o awscli${awscliversion}.zip'
- 'sudo ./aws/install'
- 'echo "export PATH=$PATH:/usr/local/bin/aws"  >> /etc/environment'

- 'curl --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp'
- 'sudo mv /tmp/eksctl /usr/local/bin'

- 'curl -o kubectl https://amazon-eks.s3.us-west-2.amazonaws.com/${kubectlversion}/${kubectldate}/bin/linux/amd64/kubectl'
- 'chmod +x ./kubectl'
- 'sudo mv ./kubectl /usr/local/bin/kubectl'

- 'curl https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-${helmversion} > get-helm.sh'
- 'chmod 700 get-helm.sh'
- './get-helm.sh'
- 'rm -rf get-helm.sh'

- 'curl -L https://github.com/kubernetes-incubator/kompose/releases/download/v${komposeversion}/kompose-linux-amd64 -o kompose'
- 'chmod +x kompose'
- 'sudo mv ./kompose /usr/local/bin/kompose'

- 'sudo yum -y install gcc'
- 'wget http://download.redis.io/redis-stable.tar.gz'
- 'tar xvzf redis-stable.tar.gz'
- 'cd redis-stable'
- 'make redis-cli'
- 'sudo mv src/redis-cli /usr/local/bin'
- 'cd ..'
- 'rm -rf redis-stable/'
- 'rm redis-stable.tar.gz'

- 'sudo yum install -y amazon-efs-utils'