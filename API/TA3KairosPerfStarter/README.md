# TA3KairosPerfStarter

This project contains an Eclipse project containing java source code (Java 11) - 

*  provided for performers to aid in jump-starting their development and integration with the Kairos Runtime architecture.
*  contains starter code with skeleton implementations of these folder monitors. Performers need only provide their own implementations as they see fit. 


    
The class **AlgorithmExecutor** in this project is where the performer algorithm implementation can be “plugged in”. 


The project also contains a shell script (build.sh) will - 

*  build the java source code (uses Gradle)
*  create, tag and upload the docker image to the KAIROS docker repository.  

Before running the script, make sure to supply user credentials (userid and password) in the script, to be able to authenticate to Kairos Docker Repository. 
Contact the NextCentury/CACI Kairos Development team to obtain those credentials. 

Steps to start development - 

1.  Pull the API project folder 
2.  Rename the project locally. Make sure the name matches the build and the resulting artifact in the docker/Dockerfile
3.  Run **./gradlew clean build**. 
4.  (Recommended) Run local unit tests on the generated artifact jar file
5.  Plug the Kairos Docker repo user credentials you obtained from the Kairos Dev team into the build script and run the script to build, tag and push Docker image to Kairos Docker Repo
6.  Plug in your implementation (refer to the **AlgorithmExecutor** class discussed above) and re-run build script.
