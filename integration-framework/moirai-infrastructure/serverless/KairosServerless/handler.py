import json
import boto3
import datetime
from datetime import timedelta

def clean(event, context):
    body = {
        "message": "Go Serverless v1.0! Your function executed successfully!",
        "input": event
    }

    response = {
        "statusCode": 200,
        "body": json.dumps(body)
    }

    # Use this code if you don't use the http event with the LAMBDA-PROXY
    # integration
    """
    return {
        "message": "Go Serverless v1.0! Your function executed successfully!",
        "event": event
    }
    """
        # Run for each known repository
    compareAndDelete('zeus')
    compareAndDelete('ui')
    compareAndDelete('clotho')
    return response


# Specific to the AWS Account
client = boto3.client('ecr', region_name='us-east-1')
def compareAndDelete(repository):
    deleteMe = []
    response = client.list_images(
                repositoryName=repository,
                maxResults=50,
                filter={'tagStatus': 'TAGGED'})
    for image in response['imageIds']:
        fullImage = client.describe_images(
            repositoryName=repository,
            imageIds=[{'imageDigest': image['imageDigest'], 'imageTag': image['imageTag']}])
        if "latest" in fullImage['imageDetails'][0]['imageTags']:
            print("THIS IS THE LATEST ONE, Never delete")
        else:
            savedDate =  fullImage['imageDetails'][0]['imagePushedAt']
            expirationDate = savedDate + timedelta(days=60)
            if expirationDate < datetime.datetime.now(expirationDate.tzinfo):
                deleteMe.append({'imageDigest': image['imageDigest'], 'imageTag': image['imageTag']})
    if deleteMe:
        client.batch_delete_image(
                repositoryName=repository,
                imageIds=deleteMe
        )
    print(deleteMe)