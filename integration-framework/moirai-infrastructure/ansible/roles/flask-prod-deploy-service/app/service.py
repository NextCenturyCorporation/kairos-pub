#!/usr/bin/env python3

from flask import Flask
from flask import Response
from flask import json
import boto3
import os

app = Flask(__name__)

@app.route("/redeploy")
def redeploy():
    data = {
        'success'  : 'true'
    }
    js = json.dumps(data)
    os.system('sudo ansible-playbook /home/ubuntu/moirai-infrastructure/ansible/deploy-zeus-dockers.yml')
    return Response(js, status=200, mimetype='application/json')

if __name__ == "__main__":
    app.run(host='0.0.0.0')


