#!/usr/bin/env python3

import boto3
import tarfile
import os
import subprocess
import shutil
import sys

s3 = boto3.client('s3')
bucket_name = sys.argv[1]

# Get the latest gitlab backup and return the version associated with it
get_last_modified = lambda obj: int(obj['LastModified'].strftime('%s'))
try:
    objs = s3.list_objects_v2(Bucket=bucket_name)['Contents']
    last_added_backup = [obj['Key'] for obj in sorted(objs, key=get_last_modified)][-1]
    backup_name = last_added_backup.strip("_gitlab_backup.tar")
    backup_version = backup_name.split("_")[-1]
    print("="+backup_version+"-ce.0")
except:
    print("")
