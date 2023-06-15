#FROM alpine:3.7
FROM bentolor/docker-dind-awscli
RUN apk add --no-cache bash
RUN apk add --no-cache curl

#RUN apk add --no-cache python3
#RUN pip3 install awscli --upgrade

RUN curl -LO https://storage.googleapis.com/kubernetes-release/release/`curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt`/bin/linux/amd64/kubectl 
RUN chmod +x ./kubectl
RUN mv ./kubectl /usr/local/bin/kubectl
