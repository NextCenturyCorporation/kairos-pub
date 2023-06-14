data "local_file" "gitlab-version-script" {
  filename = "./scripts/gitlab-version.py"
}

data "local_file" "gitlab-init-script" {
  filename = "./scripts/gitlab-init.py"
}

data "local_file" "gitlab-rb-append" {
  filename = "./config-files/gitlab.rb.append.tpl"
}

data "local_file" "gitlab-backup-cronjob" {
  filename = "./config-files/gitlab-backup-cronjob"
}

data "local_file" "gitlab-token-script" {
  filename = "./scripts/serve-token.sh"
}

data "local_file" "gitlab-cleanup-script" {
  filename = "./scripts/ecr-cleanup.py"
}

data "local_file" "gitlab-cleanup-cronjob" {
  filename = "./config-files/ecr-cleanup-cronjob"
}

data "template_file" "register-gitlab-runner-script" {
  template = file("./scripts/register-gitlab-runner.sh.tpl")
  vars = {
    GITLAB-IP = aws_instance.gitlab.private_ip
  }
}