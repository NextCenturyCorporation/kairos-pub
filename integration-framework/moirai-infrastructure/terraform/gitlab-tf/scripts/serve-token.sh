#!/usr/bin/env bash

echo "Saving runner token to /root/token"
gitlab-rails runner -e production "puts Gitlab::CurrentSettings.current_application_settings.runners_registration_token" > /root/token

echo "Waiting for runner to download token"
{ echo -ne "HTTP/1.0 200 OK\r\nContent-Length: $(wc -c </root/token)\r\n\r\n"; cat /root/token; } | nc -l 8000

echo "Removing /root/token"
rm -f /root/token
