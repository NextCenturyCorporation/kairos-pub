#!/bin/sh
# Install dependencies for Certbot
sudo apt-get update
sudo apt-get install software-properties-common -y
sudo add-apt-repository universe -y
sudo add-apt-repository ppa:certbot/certbot -y
sudo apt-get update
sudo apt-get install certbot -y
# Kairos Gmail: karios.moriai@gmail.com
