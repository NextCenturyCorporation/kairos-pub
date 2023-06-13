#!/bin/bash

tier=$(echo $ENVIRONMENT_TIER | awk '{print tolower($0)}')
if [ $tier != development ]
then
  ./prereqs.sh
fi

#java -jar /usr/src/moirai-zeus.jar
java -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=7272 -Dcom.sun.management.jmxremote.rmi.port=7272  -Djava.rmi.server.hostname=localhost  -Dcom.sun.management.jmxremote.authenticate=false  -Dcom.sun.management.jmxremote.ssl=false -jar /usr/src/moirai-zeus.jar