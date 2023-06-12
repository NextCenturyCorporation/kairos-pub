# Python Clotho Api 

This package includes a python script to submit a given json file to interface with clotho's api, a generated python api, and a collection of example json files for different api calls.

## To Run

In clotho_api's main method, uncomment the block of code for your given api call. If you want to call the post vertex endpoint the section of code under the comment "Get vertex with label", 
uncomment the few lines of code directly underneath that comment. Then, run "python3 clotho_api.py" followed by the json file that will include the necessary information for that endpoint.

## Example

Running "python3 clotho_api.py json_examples/get_vertex_example" will return the api response if the json is valid.
If the json file is invalid, you wil instead see the error number and the http response header.
