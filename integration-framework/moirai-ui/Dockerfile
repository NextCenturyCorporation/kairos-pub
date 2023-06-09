FROM nginx

RUN mkdir /app
COPY /dist /app
COPY nginx.conf.template /etc/nginx/nginx.conf.template

# Note we are setting ZEUS_LOCATION here as the default environment variable
# The run command isn't using it yet, when init.sh is called on startup it will take the env variable and do the replacement
ENV ZEUS_LOCATION http://zeus.kairos.nextcentury.com:8000/

ENV CLOTHO_LOCATION https://validation.kairos.nextcentury.com/

RUN echo "#!/bin/bash +x" >>/tmp/init.sh
RUN echo "" >>/tmp/init.sh
RUN echo "cat /etc/nginx/nginx.conf.template | envsubst '\$ZEUS_LOCATION' | envsubst '\$CLOTHO_LOCATION' > /etc/nginx/nginx.conf" >>/tmp/init.sh

RUN echo "exec nginx -g 'daemon off;'" >>/tmp/init.sh
RUN chmod +x /tmp/init.sh

CMD /tmp/init.sh
