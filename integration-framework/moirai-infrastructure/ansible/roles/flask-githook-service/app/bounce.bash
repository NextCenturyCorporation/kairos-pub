#! /bin/bash

ssh -i ~/.ssh/moirai-machine.pem ubuntu@test.kairos.nextcentury.com sudo systemctl stop flask

ssh -i ~/.ssh/moirai-machine.pem ubuntu@test.kairos.nextcentury.com rm /home/ubuntu/git-hook.py
scp -i ~/.ssh/moirai-machine.pem git-hook.py ubuntu@test.kairos.nextcentury.com:/home/ubuntu/git-hook.py
ssh -i ~/.ssh/moirai-machine.pem ubuntu@test.kairos.nextcentury.com sudo chmod +x /home/ubuntu/git-hook.py
sleep 1

ssh -i ~/.ssh/moirai-machine.pem ubuntu@test.kairos.nextcentury.com sudo systemctl start flask