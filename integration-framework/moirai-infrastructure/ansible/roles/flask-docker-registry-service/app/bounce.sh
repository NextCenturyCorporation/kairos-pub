#! /bin/bash

ssh -i ~/.ssh/moirai-machine.pem ubuntu@docker.kairos.nextcentury.com sudo systemctl stop flask

ssh -i ~/.ssh/moirai-machine.pem ubuntu@docker.kairos.nextcentury.com rm /home/ubuntu/service.py
scp -i ~/.ssh/moirai-machine.pem service.py ubuntu@docker.kairos.nextcentury.com:/home/ubuntu/service.py
ssh -i ~/.ssh/moirai-machine.pem ubuntu@docker.kairos.nextcentury.com sudo chmod +x /home/ubuntu/service.py
sleep 1

ssh -i ~/.ssh/moirai-machine.pem ubuntu@docker.kairos.nextcentury.com sudo systemctl start flask