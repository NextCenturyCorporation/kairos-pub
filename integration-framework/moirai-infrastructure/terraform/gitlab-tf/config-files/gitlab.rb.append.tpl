
nginx['listen_port'] = 80
nginx['listen_https'] = false
nginx['proxy_set_headers'] = {
    'X-Forwarded-Proto' => 'https',
    'X-Forwarded-Ssl' => 'on'
}

gitlab_rails['backup_upload_connection'] = {
    'provider' => 'AWS',
    'region' => '<AWS-REGION>',
    'use_iam_profile' => true
}

gitlab_rails['backup_upload_remote_directory'] = '<GITLAB-BACKUP-S3-BUCKET>'
gitlab_rails['backup_keep_time'] = 604800
