Run command: docker-compose -f clotho-neo4j.yml up

This is create two containers 
(1) A standard neo4j container which is used as the backend for storing the underlying schema data.
(2) A clotho container which created by the MOIRAI team and contains the validation endpoints.

Once up go to localhost:8008 to see the swagger-ui page to use the endpoints