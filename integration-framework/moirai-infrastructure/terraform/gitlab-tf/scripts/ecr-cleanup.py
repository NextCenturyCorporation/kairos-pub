#!//bin/env python3

import boto3
import datetime
from datetime import timedelta

# Specific to the AWS Account
ourRegistryId='130602597458'
client = boto3.client('ecr', region_name='us-east-1')
def compareAndDelete(repository):
    deleteMe = []
    response = client.list_images(
                registryId=ourRegistryId,
                repositoryName=repository,
                maxResults=50,
                filter={'tagStatus': 'TAGGED'})
    for image in response['imageIds']:
        fullImage = client.describe_images(
            registryId=ourRegistryId,
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
                registryId=ourRegistryId,
                repositoryName=repository,
                imageIds=deleteMe
        )
    print(deleteMe)

# Run for each known repository
compareAndDelete('zeus')
compareAndDelete('ui')
compareAndDelete('clotho')
