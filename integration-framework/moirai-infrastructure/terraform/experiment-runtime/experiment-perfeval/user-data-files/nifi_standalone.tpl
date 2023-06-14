#cloud-config
hostname: k8s-beachhead

package_update: true
package_upgrade: true
packages:

runcmd:
- 'sudo yum -y update'
- 'sudo yum -y install -y amazon-linux-extras'
- 'sudo yum -y install epel-release'
- 'sudo amazon-linux-extras install -y java-${javajdk}${javaversion}'
- "JH=$(java -XshowSettings:properties -version 2>&1 > /dev/null | grep 'java.home' | awk '{ print $3 }')"
- "echo 'export JAVA_HOME='$JH >> /etc/bashrc"
- "echo 'export PATH=$JAVA_HOME/bin:$PATH' >> /etc/bashrc"
- "echo 'export PATH=$JAVA_HOME/bin:$PATH' >> /etc/bash_profile"

- "aws s3 cp s3://kairos-safe/sbin/nifi-${nifiversion}-bin.tar.gz ."
- 'tar -xvf nifi-${nifiversion}-bin.tar.gz'
- 'mv nifi-${nifiversion} /home/ec2-user/nifi'
- "echo 'export NIFI_HOME=/home/ec2-user/nifi' >> /etc/bashrc"