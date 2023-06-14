#!/usr/bin/env python3

import boto3
import tarfile
import os
import subprocess
import shutil
import sys

s3 = boto3.client('s3')
backup_bucket_name = sys.argv[1]
etc_bucket_name = sys.argv[2]

# Get latest gitlab data from s3 and restore
get_last_modified = lambda obj: int(obj['LastModified'].strftime('%s'))
objs = s3.list_objects_v2(Bucket=backup_bucket_name)['Contents']
last_added_backup = [obj['Key'] for obj in sorted(objs, key=get_last_modified)][-1]
backup_output_file = os.path.join("/var/opt/gitlab/backups", last_added_backup)
backup_name = last_added_backup.strip("_gitlab_backup.tar")

s3.download_file(backup_bucket_name, last_added_backup, backup_output_file)
shutil.chown(backup_output_file, user="git", group="git")
os.chmod(backup_output_file, 0o600)
subprocess.run(["gitlab-backup", "restore", "BACKUP=" + backup_name, "force=yes"])

# Download and extract gitlab config from s3
s3.download_file(etc_bucket_name, 'gitlab_secrets.tar', '/gitlab_secrets.tar')

with tarfile.open("/gitlab_secrets.tar") as backup_tar:
    backup_tar.extractall(path="/")
os.remove("/gitlab_secrets.tar")
