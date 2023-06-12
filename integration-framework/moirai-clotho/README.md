# Moirai Ta1 System

The Moirai clotho system for Kairos TA1 that implements a REST server supporting CRUD access to one of many graph databases using the tinkerpop connecter.

Clotho (Greek "spinner") - spun the thread of life from her distaff onto her spindle.

OSX Requirements
- brew install coreutils

## To build

Run "./gradlew" which is the same as running "./gradlew clean swaggerClean generateSwaggerCode generateSwaggerUI build"
This will produce the jar "build/libs/moirai-clotho-<version>.jar"


## To run the jar.
The jar expects the following environment variables to be set to be able to run.
 * DB_TYPE - Which database the server will connect to. Options are "Neo4j", "OrientDB", "Blazegraph", and "Janusgraph"
 * DB_URI - The protocol and endpoint used to connect to the database.
 
The project is expected to be run inside a docker container that is automatically built and deployed whenever a commit is made to master.

For local development there are docker compose files for each of the databases listed above. Found under 'compose/'
They will spin up a docker container for the chosen database, spin up a docker container with the built, handle linking the containers networks and setting the correct environment variables.

From the main directory simple run "docker-compose -f ./compose/compose-neo4j.yml up" after the gradle build steps

## To deploy
./gradlew buildDockerImage will build the docker image
./gradlew pushDockerImage will push the docker image to the amazon ecr repo.

both of these accept the following flags
-Pimage-name=  the name of the image, to be able to push to ecr this name must be the ecr location. It's recommended to leave this blank
-Ptag= the tag for this image that is built
-Ptag-latest= in addition to the normal tag will also tag the build image as 'latest'

gitlab handles building and push a new image whenever there is a commit to master.

If you need to build locally and push to ecr run
./gradlew -Ptag=<image tag> buildDockerImage pushDockerImage --info
