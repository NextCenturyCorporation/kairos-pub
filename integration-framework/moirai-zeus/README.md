# moirai-zeus
This project represents the springboot application backend for the Zeus management tool for Kairos project.
It serves to both manage external users allowing them to view their services, submissions, and experiments; and to aid the projects admins by automating deployment and management of key resources and infrastructure.

## Tools
### Local requirements
-   [java 11](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
-   [docker 19.03.5](https://docs.docker.com/install/#server) - The latest version should be fine to install
-   [docker-compose 1.24.1](https://github.com/docker/compose/releases/tag/1.24.1) - The latest version should be fine to install

### Included with the project, or automatically installed
-   [gradle](https://docs.gradle.org/current/userguide/getting_started.html) - A gradlew wrapper is included so you do not need to install gradle. use `./gradlew` to use the included version.
-   [openapi v3.0.0](http://spec.openapis.org/oas/v3.0.0) - We use openapi (formally [swagger](https://swagger.io/docs/specification/about/)) to generate the classes that receive the api requests and generate our interactive api documentation.
-   [springboot](https://www.tutorialspoint.com/spring_boot/spring_boot_introduction.htm) - This application is developed using the springboot microservice framework
-   [junit 5](https://junit.org/junit5/docs/current/user-guide/) - We are using junit 5 as our testing framework
-   [checkstyle](https://checkstyle.sourceforge.io/) - We use checkstyle for linting our java code.

## Configuration

-  Inside src\main\resources\application.properties set the attribute application.admin.secret to a secure password used as the default for the admin account.
-  src\main\java\com\ncc\kairos\moirai\zeus\ZeusApplication.java change generic.email@host.com to the admin account holders email

Properties for database connection visit: https://nextcentury.atlassian.net/wiki/spaces/KAIR/pages/890339780/Zeus+Configuration
These properties are to be set in a .env file. Add your AWS Key and Secret to the .env file.
If you have awscli setup these values can be found in  ~/.aws/credentials.
If you need credentials contact Ryan.Scott@caci.com

## Build

Builds the Docker image and then generates the necessary files from swagger and builds the jar.
```
./gradlew
```

## Local Development
Before docker-compose can run you need to create the local network for Zeus.
This is how the dockerized version of the ui communicates with Zeus
```
docker network create zeus-ui
```

Docker-compose will use the built zeus image and deploy it locally on port 8000
```
./gradlew && docker-compose down && docker-compose up
```

To run the application against the test configurations (Used for all development adding data models to the DB).
```
./gradlew && docker-compose down && ENVIRONMENT_TIER=test docker-compose up
```
To run the application against the production like configurations (for most development)
see the first step above.

### Debugging in intellij

Go to the ZuesApplication class, right click on the class name in the code, and click "debug Zeus....". Notice it should fail. We need to add environment variables.

Copy all the text in the .env file. 

Click Run on the top bar -> edit configurations. Now click on the icon on the far right of the environment variables field. There should be an icon for pasting on the right side bar on the popup that comes up. Click that.

Click ok, then apply. Debugging the Zeus application should now work. 

### Debugging in vscode

get the Debugger for Java extension by microsoft from the extension marketplace.
On the debug tab at the top there is a drop down, click that then add a new configuration
Add the following below, This tells it to run using the .env file as input variables
{
    // Use IntelliSense to learn about possible attributes.
    // Hover to view descriptions of existing attributes.
    // For more information, visit: https://go.microsoft.com/fwlink/?linkid=830387
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Launch ZeusApplication Dev",
            "envFile": "${workspaceFolder}/.env",
            "request": "launch",
            "mainClass": "com.ncc.kairos.moirai.zeus.ZeusApplication",
            "projectName": "moirai-zeus"
        }
    ]
}

## Deployment
The following builds the Docker image containing zeus moirai-zeus:1.0
The -Ptag and -Ptag-latest are both optional.
* -Ptag the default value is the project version.
* -Ptag-latest: The default value is false. When set to true it will tag the pushed image as latest.
```
./gradlew -Ptag=<your tag> -Ptag-latest=true buildDockerImage pushDockerImage
```

## Testing
### Testing java
Tests exist under src/test/resources and can be run with the following command.
This is automatically run during the gradle build step.
```
./gradlew test
```
### Testing Docker images
This step is automatically run during the gradle task `buildDockerImage`
```
./runDockerTests.sh moirai-zeus:1.0
./runDockerTests.sh terraformjava:latest
```
