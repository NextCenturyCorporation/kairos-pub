#!/usr/bin/env python3

from flask import Flask
from flask import Response
from flask import request
from flask import json
import boto3
from boto3 import client
import os
import time
import shutil

app = Flask(__name__)

@app.route("/zip", methods = ['POST'])
def s3zip():
    data = request.get_json()
    prefixes = data['prefixes'] if 'prefixes' in data else []
    keys = data['keys'] if 'keys' in data else []
    path = "/tmp/s3/"+str(time.time()).replace(".","")

    for prefix in prefixes:
        os.system('aws s3 sync s3://'+data['bucket']+'/'+prefix+' '+path+'/'+prefix)
    for key in keys:
        os.system('aws s3 cp s3://'+data['bucket']+'/'+key+' '+path+'/'+key)
    shutil.make_archive(path,'zip',path)

    os.system('aws s3 cp '+path+'.zip s3://'+data['output'])
    os.system('rm -rf '+path)
    os.system('rm '+path+'.zip')

    data = {
        'success'  : 'true'
    }
    js = json.dumps(data)
    return Response(js, status=200, mimetype='application/json')

if __name__ == "__main__":
    app.run(host='0.0.0.0')


