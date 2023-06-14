#cloud-config
hostname: kairos-gitlab-runner
package_update: true
package_upgrade: true

write_files:
    - encoding: b64
      content: ${REGISTER-GITLAB-RUNNER-SCRIPT}
      path: /root/register-gitlab-runner.sh
      permissions: '0755'
  
runcmd:
- 'echo "${GITLAB-IP} ${GITLAB-HOSTNAME}" >> /etc/hosts'
- 'curl -L https://packages.gitlab.com/install/repositories/runner/gitlab-runner/script.deb.sh | bash'
- 'apt install -y gitlab-runner'

- 'add-apt-repository -y ppa:git-core/ppa'
- 'apt update -y'
- 'apt-get install -y git'
- 'apt-get install -y openjdk-11-jdk'
- 'echo "export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64" >> /etc/profile'

- 'mkdir /blackduck'
- 'wget https://sig-repo.synopsys.com/artifactory/bds-integrations-release/com/synopsys/integration/synopsys-detect/8.6.0/synopsys-detect-8.6.0.jar'
- 'mv synopsys-detect-8.6.0.jar /blackduck/synopsys-detect.jar'
- 'chown -R gitlab-runner:gitlab-runner /blackduck'

- 'touch /tmp/ready'
