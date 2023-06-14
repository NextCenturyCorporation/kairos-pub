resource "null_resource" "beachhead_upload" {
  connection {
    host        = local.perfeval.beachhead.public_ip
    type        = "ssh"
    user        = local.perfeval.beachhead.user
    private_key = file(local.machine_key_pair_file)
  }

  triggers = {
    always_run = timestamp()
  }

  provisioner "remote-exec" {
    inline = [
      "mkdir /home/ec2-user/deployments/ || true"
    ]
  }

  provisioner "file" {
    source      = "${path.module}/deployments/"
    destination = "/home/ec2-user/deployments"
  }
}
