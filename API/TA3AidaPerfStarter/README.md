# TA3AidaPerfStarter

This folder contains an Eclipse project containing Java source code (Java 11) -

*  aids in jump-starting development and integration with the KAIROS Runtime architecture.
*  contains starter code with skeleton implementations of folder monitors. Performers need only provide their own implementations as they see fit.



The class **AlgorithmExecutor** in this project is where the performer algorithm implementation can be “plugged in”.


The project also contains a shell script (build.sh) which will -

*  build the Java source code (uses Gradle)
*  create, tag and upload the docker image to the KAIROS Docker Repository.

Before running the script, make sure to supply user credentials (userid and password) in the script, to be able to authenticate to the KAIROS Docker Repository.
Contact the NextCentury/CACI KAIROS Development team to obtain those credentials.

Steps to start development -

1.  Pull the project folder from https://github.com/NextCenturyCorporation/kairos-pub/tree/master/API/TA3AidaPerfStarter
2.  Rename the project locally. Make sure the name matches the build and the resulting artifact in the docker/Dockerfile
3.  Run **./gradlew clean build**.
4.  (Recommended) Run local unit tests on the generated artifact jar file
5.  Plug the KAIROS Docker Repository user credentials you obtained from the KAIROS Dev team into the build script and run the script to build, tag, and push the Docker image to the KAIROS Docker Repository
6.  Plug in your implementation (refer to the **AlgorithmExecutor** class discussed above), along with any 3rd party library dependencies into the project and re-run the build script.
