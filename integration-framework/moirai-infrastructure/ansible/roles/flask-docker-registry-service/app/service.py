#!/usr/bin/env python3

from flask import Flask
from flask import Response
from flask import request
from flask import json
from pathlib import Path
import os
import docker
import logging
import logging.handlers
import subprocess

app = Flask(__name__)

syslogger=logging.getLogger('DFLogger')
syslogger.setLevel(logging.DEBUG)
syslogger.addHandler(logging.handlers.SysLogHandler(address='/dev/log'))

def getResponseSuccess(performerName, returnStatus):
    show('success')
    data = {
        'success'  : 'true',
        'performerName': performerName
    }
    js = json.dumps(data)
    return Response(js, status=returnStatus, mimetype='application/json')

def getResponseFail(performerName, returnStatus, message):
    show(message)
    data = {
        'success'  : 'false',
        'performerName': performerName,
        'error' : message
    }
    js = json.dumps(data)
    return Response(js, status=returnStatus, mimetype='application/json')

def show(message):
    syslogger.info('FlaskDockerService: '+str(message))

def callCommand(command):
    show(command)
    output = subprocess.run(command, shell=True, capture_output=True, text=True)
    show(output)
    return output

def getContainer(name):
    client = docker.from_env()
    containerList = client.containers.list()
    show(containerList)
    for container in containerList:
        if container.name == name:
            show('Found container '+name)
            return container
    show('Container '+name+' not found')
    return None

def setAuth(name, secret):
    callCommand('sudo mkdir /auth')
    callCommand('sudo mkdir /auth/'+name)
    callCommand('sudo chown ubuntu:ubuntu -R /auth')
    htpasswd = '/auth/'+name+'/'+ 'htpasswd'
    Path(htpasswd).touch()
    callCommand('sudo chmod 550 '+htpasswd)  

    callCommand('sudo htpasswd -cBb '+htpasswd+' admin Kair0s_pwd33122')
    callCommand('sudo htpasswd -Bb '+htpasswd+' '+ name + ' ' + secret)

def startContainer(name):
    cmdTemplate='docker run -P -d --restart=always --net nginx-network --name <NAME>'
    cmdTemplate+=' -v /home/ubuntu/config.yml:/etc/docker/registry/config.yml'
    cmdTemplate+=' -v /etc/letsencrypt:/etc/letsencrypt'
    cmdTemplate+=' -v /auth:/auth'
    cmdTemplate+=' -e "REGISTRY_STORAGE=s3"'
    cmdTemplate+=' -e "REGISTRY_STORAGE_S3_REGION=us-east-1"'
    cmdTemplate+=' -e "REGISTRY_STORAGE_S3_BUCKET=kairos-docker-registries"'
    cmdTemplate+=' -e "REGISTRY_STORAGE_S3_ROOTDIRECTORY=/repo/<NAME>"'
    cmdTemplate+=' -e "REGISTRY_STORAGE_CACHE_BLOBDESCRIPTOR=inmemory"'
    cmdTemplate+=' -e "REGISTRY_AUTH=htpasswd"'
    cmdTemplate+=' -e "REGISTRY_AUTH_HTPASSWD_REALM=Registry Realm"'
    cmdTemplate+=' -e "REGISTRY_AUTH_HTPASSWD_PATH=/auth/<NAME>/htpasswd"'
    cmdTemplate+=' registry:2'
    cmd=cmdTemplate.replace('<NAME>', name)
    
    callCommand(cmd)

@app.route('/registry/create')
def createRegistry():
    performerName = request.args.get('performer').lower()
    secret = request.args.get('secret')
    show('Creating registry for '+performerName+' with secret '+secret)
    
    container = getContainer(performerName)
    show(container)
    if (container != None):
        return getResponseFail(performerName, 409,'Registry is already up')
    setAuth(performerName,secret)
    startContainer(performerName)
    return getResponseSuccess(performerName, 200)

@app.route('/registry/password/reset')
def passwordReset():
    performerName = request.args.get('performer').lower()
    secret = request.args.get('secret')
    show('Resetting registry for '+performerName+' with secret '+secret)
    
    container = getContainer(performerName)
    show("?"+str(container))
    if (container == None):
        return getResponseFail(performerName, 404,'Registry not found')

    setAuth(performerName, secret)
    return getResponseSuccess(performerName, 200)

@app.route('/registry/delete')
def deleteRegistry():
    performerName = request.args.get('performer').lower()
    show('Deleting registry for '+performerName)

    container = getContainer(performerName)

    if (container == None):
        return getResponseFail(performerName, 404, 'Registry not found')
    else:
        containerId = container.id
        container.stop()
        callCommand('docker container rm ' + containerId)
        callCommand('sudo rm -rf /auth/' + performerName + '/')
        return getResponseSuccess(performerName, 200)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8008)
