#! /bin/bash

docker kill ui-local
docker kill nginx

docker rm ui-local
docker rm nginx

docker run -d -P --network=zeus-ui --name ui-local --env ZEUS_LOCATION=http://zeus-local:8000/ ui:local

docker run -d -p 80:80 --network=zeus-ui --name nginx nginx
docker cp /tmp/nginx.conf nginx:/etc/nginx/nginx.conf
docker exec nginx nginx -s reload


# docker cp /tmp/test.nginx.conf ui-test:/etc/nginx/nginx.conf
# docker exec ui-test nginx -s reload

