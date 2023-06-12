from __future__ import print_function
import sys
import json
sys.path.append("./python/")
import openapi_client
from openapi_client.rest import ApiException
from pprint import pprint
from openapi_client.api_client import ApiClient
from openapi_client.configuration import Configuration

# create an instance of the API class

config = Configuration()
config.host="http://validation.kairos.nextcentury.com"
api_client = ApiClient(configuration=config)

api_instance = openapi_client.KSFApi(api_client)



def validateJsonLD(json_ld_object):
    try:
        # evaluate ksf(Json-LD) requests for any syntactic errors and warnings
        api_response = api_instance.validate_ksf_request(json_ld_object)
        pprint(api_response)
    except ApiException as e:
        print("Exception when calling KSFApi->validate_ksf_request: %s\n" % e)

if __name__ == '__main__':
    f = open(sys.argv[1], "r")
    data = f.read()
    json_data = json.loads(data)
    json_ld_object = openapi_client.JsonLdObject(at_context=json_data['@context']) # JsonLdObject | KSF submission to validate
    json_ld_object.sdf_version=json_data['sdfVersion']
    json_ld_object.schemas=json_data['schemas']
    json_ld_object.ta2=json_data['ta2']
    validateJsonLD(json_ld_object)
