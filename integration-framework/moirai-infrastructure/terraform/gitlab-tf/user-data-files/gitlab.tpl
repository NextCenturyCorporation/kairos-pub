#cloud-config
hostname: kairos-gitlab

package_update: true
package_upgrade: true
packages:
- python3-boto3

write_files:
- encoding: b64
  content: ${GITLAB-VERSION-SCRIPT}
  path: /root/gitlab-version.py
  permissions: '0700'
- encoding: b64
  content: ${GITLAB-INIT-SCRIPT}
  path: /root/gitlab-init.py
  permissions: '0700'
- encoding: b64
  content: ${GITLAB-RB-APPEND}
  path: /etc/gitlab/gitlab.rb.append
- encoding: b64
  content: ${GITLAB-BACKUP-CRONJOB}
  path: /etc/cron.d/gitlab-backup-cronjob
- encoding: b64
  content: ${GITLAB-TOKEN-SCRIPT}
  path: /root/serve-token.sh
  permissions: '0700'
- encoding: b64
  content: ${GITLAB-CLEANUP-SCRIPT}
  path: /usr/bin/ecr-cleanup.py
  permissions: '0700'
- encoding: b64
  content: ${GITLAB-CLEANUP-CRONJOB}
  path: /etc/cron.d/ecr-cleanup-cronjob
runcmd:
- 'apt -y update'
- 'export EXTERNAL_URL="http://${GITLAB-HOSTNAME}"'
- 'export GITLAB_VERSION=$(python3 /root/gitlab-version.py ${GITLAB-BACKUP-S3-BUCKET})'
- 'curl -sL https://packages.gitlab.com/install/repositories/gitlab/gitlab-ce/script.deb.sh | bash'
- 'rm /root/gitlab-version.py'

- 'apt -y install gitlab-ce$GITLAB_VERSION'
- 'touch /etc/gitlab/skip-auto-backup'
- 'python3 /root/gitlab-init.py ${GITLAB-BACKUP-S3-BUCKET} ${GITLAB-ETC-S3-BUCKET}'
- 'rm /root/gitlab-init.py'

- 'cat /etc/gitlab/gitlab.rb.append | sed "s/<GITLAB-BACKUP-S3-BUCKET>/${GITLAB-BACKUP-S3-BUCKET}/g" | sed "s/<AWS-REGION>/${AWS-REGION}/g" >> /etc/gitlab/gitlab.rb'
- 'rm /etc/gitlab/gitlab.rb.append'

- 'gitlab-ctl upgrade'
- 'gitlab-ctl reconfigure'
- 'gitlab-rake gitlab:check SANITIZE=true'
- 'apt -y autoremove'
- 'apt -y autoclean'

- 'gitlab-ctl restart'
- 'touch /tmp/ready'
# - 'reboot'
