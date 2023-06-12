# Python Api Validation

This package includes a python script to submit a given ksf file for validation, a generated python api, and a sample example ksf file.

## To Run

Run "python3 validation.py" followed by the file you wish to submit for validation.

## Example

Running "python3 validation.py example_ksf/example_file" will return

`Exception when calling KSFApi->validate_ksf_request: (400)`

`Reason:`

`HTTP response headers: HTTPHeaderDict({'Content-Type': 'application/json', 'Connection': 'close', 'Date': 'Fri, 17 Jul 2020 13:23:59 GMT', 'Transfer-Encoding': 'chunked', 'Vary': 'Origin, Access-Control-Request-Method, Access-Control-Request-Headers'})`

`HTTP response body: {"errorsList":["ERROR: Only one of: 'entityTypes', 'entityTypes_OR', or 'entityTypes_AND' may be specified for slot 'https://caci.com/kairos/Schemas/CoordinatedBombingAttack/Slots/training_1/trainer'."],"warningsList":[]}`


This response contains 3 parts.

 * The HTTP response number
 * The HTTP response headers
 * The HTTP response body

The response number will either be 200 for valid submissions, 400 for ksf submissions that contain errors or warnings,
or 500 for a server side failure.

The response header just contains basic http header information

The response body will return a list of errors and warnings for your submission if they exist.
