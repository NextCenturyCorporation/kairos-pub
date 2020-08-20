FROM adoptopenjdk/openjdk11:alpine-jre
RUN apk add --no-cache bash

# copy JAR into image
COPY TA3KairosPerfStarter.jar /TA3KairosPerfStarter.jar 

#expose port
EXPOSE 10100

# java heap size settings xms (initial); xmx (max)
ENV JAVA_OPTS=" -Xms1024m -Xmx2048m "
ENTRYPOINT [ "bash", "-c", "java $JAVA_OPTS -jar /TA3KairosPerfStarter.jar \"$@\"", "--" ]
