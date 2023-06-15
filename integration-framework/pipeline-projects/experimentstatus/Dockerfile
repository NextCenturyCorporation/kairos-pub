FROM adoptopenjdk/openjdk11:alpine-jre
RUN apk add --no-cache bash
RUN apk add --no-cache curl

RUN apk add --no-cache python3
RUN apk add --no-cache cmd:pip3
RUN pip3 install awscli --upgrade

RUN curl -LO https://storage.googleapis.com/kubernetes-release/release/`curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt`/bin/linux/amd64/kubectl 
RUN chmod +x ./kubectl
RUN mv ./kubectl /usr/local/bin/kubectl

# copy JAR into image
COPY build/libs/ExperimentStatus.jar /ExperimentStatus.jar 

#expose port
EXPOSE 10120

# java heap size settings xms (initial); xmx (max)
ENV JAVA_OPTS=" -Xms1024m -Xmx2048m "
ENTRYPOINT [ "bash", "-c", "java $JAVA_OPTS -jar /ExperimentStatus.jar \"$@\"", "--" ]
