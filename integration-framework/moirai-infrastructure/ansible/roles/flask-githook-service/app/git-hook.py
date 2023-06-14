#!/usr/bin/env python3

from collections import OrderedDict
from flask import Flask
from flask import Response
from flask import request
from flask import json
import boto3
import docker
import gitlab
import logging
import logging.handlers
import os
import subprocess

syslogger=logging.getLogger('DFLogger')
syslogger.setLevel(logging.DEBUG)
syslogger.addHandler(logging.handlers.SysLogHandler(address='/dev/log'))

app = Flask(__name__)
def show(message):
    syslogger.info('GitHookService: '+str(message))
    return(message)

def run(command):
    show(command)
    try:
        output = subprocess.run(command, shell=True, capture_output=True, text=True)
        show(output)
        return output
    except subprocess.CalledProcessError as error:
        error = {
            'command': command,
            'error': {
                'code': error.returncode,
                'output': error.output.decode("utf-8") 
            }
        }
        show(error)
        return error

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

def _getResponseSuccess():
    show('success')
    data = {
        'success'  : 'true'
    }
    js = json.dumps(data)
    return Response(js, status=200, mimetype='application/json')

def standardize(tag):
    show("standardize: "+str(tag))
    tag = tag.lower()
    if (tag == 'master'):
        tag = 'latest'
    return str(tag)

def standardDockerName(tag):
    return standardize(tag).replace("_","")

def _deploy_zeus(zeusTag):
    show('Deploying zeus '+zeusTag)
    return run('sudo ansible-playbook /home/ubuntu/moirai-infrastructure/ansible/redeploy-zeus-lower.yml --extra-vars="zeus_tag=' + standardize(zeusTag) + '"')

def _deploy_ui(uiTag, zeusTag):
    show('Deploying ui '+uiTag+' with zeus backend '+zeusTag)
    return run('sudo ansible-playbook /home/ubuntu/moirai-infrastructure/ansible/redeploy-ui-lower.yml --extra-vars="ui_tag=' + standardize(uiTag) + ' zeus_tag=' + standardize(zeusTag) + '"')

@app.route("/ui")
def deployUI():
    uiTag = request.args.get('uiTag')
    _deploy_ui(uiTag, 'latest')
    return _getResponseSuccess()

@app.route("/zeus")
def deployZeus():
    zeusTag = request.args.get('zeusTag')
    _deploy_zeus(zeusTag)
    return _getResponseSuccess()

@app.route("/custom")
def deployBoth():
    tag = str(request.args.get('tag'))
    _deploy_zeus(tag)
    _deploy_ui(tag, tag)
    return _getResponseSuccess()

def findGitlab():
    filterec2 = [{
        'Name': 'tag:Name',
        'Values': ['Gitlab']
    }]
    ec2 = boto3.client('ec2', region_name='us-east-1')
    res = ec2.describe_instances(Filters=filterec2)
    if len(res['Reservations']):
        dnsName = 'http://' + res['Reservations'][0]['Instances'][0]['PrivateIpAddress']
        show(dnsName)
        return dnsName
    else:
        raise Exception("Can't find instance of gitlab")

def getActiveBranchNames(dnsName, projectName, prefix):
    gl = gitlab.Gitlab(dnsName, private_token='CFwortnE1HQ7xcYowzob')
    gl.auth()
    projects = gl.projects.list(search=projectName)
    branchContainerList = []
    for project in projects:
        branches = project.branches.list()
        show("project: "+projectName)
        show(branches)
        for branch in branches:
            namedContainer = prefix+"-"+standardDockerName(branch.name)
            if namedContainer not in branchContainerList:
                branchContainerList.append(namedContainer)
    show(branchContainerList)
    return branchContainerList

@app.route("/cleanup")
def cleanUp():
    data = {
        "removedContainers" : []
    }
    
    dnsName = findGitlab()
    # Get list of container names for active branches
    keepActive = ["kairos-nginx"] # List of branches for Zeus and UI
    keepActive.extend(getActiveBranchNames(dnsName, 'moirai-zeus', 'zeus'))
    uiContainerNames=getActiveBranchNames(dnsName, 'moirai-ui', 'ui')
    keepActive.extend(uiContainerNames)
    
    backEndContainers=map(lambda ui: 'zeus'+ui[len('ui'):], uiContainerNames) # Every ui container potentially needs the zeus match, even if the zeus branch is gone.
    keepActive.extend(backEndContainers)
    keepActive = list(OrderedDict.fromkeys(keepActive)) #R emove duplicates
    show("Keep Active: "+str(keepActive))

    # Clean up unneeded containers
    client = docker.from_env()
    activeContainers = client.containers.list()
    containersToRemove = list(filter(lambda c: not c.name in keepActive, activeContainers))
    for container in containersToRemove:
        data["removedContainers"].append(container.name)
        show("Stopping container " + container.name)
        container.stop()
        container.remove(force=True)

    js = json.dumps(data)
    return Response(js, status=200, mimetype='application/json')


if __name__ == "__main__":
    app.run(host='0.0.0.0')


