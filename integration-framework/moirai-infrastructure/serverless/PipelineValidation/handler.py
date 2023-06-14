import json
import boto3
import datetime
from urllib.parse import unquote
from datetime import timedelta
import urllib3

s3 = boto3.resource('s3',region_name='us-east-1')

def save(data, bucket, key):
    print(f"saving validation to bucket:{bucket}, key:{key}")
    print(data)
    s3.Object(bucket,key).put(Body=data)

def getValidation(bucket,key):
    print(f"validating bucket:{bucket}, key:{key}")
    data = s3.Object(bucket,key).get()['Body'].read()

    headers={"accept": "application/json", "Content-Type": "application/ld+json"}
    response = urllib3.PoolManager().request("POST",'https://validation.kairos.nextcentury.com/json-ld/ksf/validate?failOnUnknown=true', headers=headers, body=data)
    print (response.status)
    return response

def getHandleResponse(event):
    body = {
        "message": "Go Serverless v1.0! Your function executed successfully!",
        "input": event
    }

    return {
        "statusCode": 200,
        "body": json.dumps(body)
    }

def handle(event, context):
    records = event['Records']
    for rec in records:
        bucket = rec['s3']['bucket']['name']
        key = unquote(rec['s3']['object']['key'])
        if ("submission-results" in key):            
            response = getValidation(bucket, key)

            if (response.status == 504):
                outkey = key.replace("submission-results","validation-results").replace(".json",".504")
                save(response.data, bucket, outkey)
            else:
                outkey = key.replace("submission-results","validation-results").replace(".json",".validation")
                save(json.dumps(json.loads(response.data.decode('utf-8'))), bucket, outkey)
    return getHandleResponse(event)