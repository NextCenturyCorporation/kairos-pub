import os
import requests

SUBMISSION_URL="http://localhost:10007"

ceid="00000"
task="testtype"
performername="ta2perf"
runid="testrun"
ta1name="ta1perf"

hrf_data = {
    "ceid": ceid,
    "experimenttype": task,
    "performername": performername,
    "runId": runid,
    "ta1name": ta1name
}

files=[('file', open('sample_hrf.txt','rb'))]

hrf_submit_dir = os.path.join(SUBMISSION_URL, 'kairos', 'hrf')
response = requests.post(hrf_submit_dir, data=hrf_data, files=files)
print("done")