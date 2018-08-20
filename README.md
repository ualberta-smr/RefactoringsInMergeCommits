# Refactoring Analysis
This project analyzes merge commits in git repositries and determines whether refactoring changes are to blame for the conflicts.


## System requirements
* Linux or macOS
* git
* Java 8
* Maven
* MySQL 5.7

## How to run

### 1. Compile the project
Clone the repository and run the following command to compile the project and create an executable JAR file:
```
TODO: MAVEN_COMMAND
```

### 2. Create a database configuration file
Edit the `database.properties` file and add your MySQL username and password:
```
development.driver=com.mysql.jdbc.Driver
development.username=USERNAME
development.password=PASSWORD
development.url=jdbc:mysql://localhost/refactoring_analysis
```
If you wish, you can choose a different name for the database (`refactoring_analysis` in the template). You don't need to create the database, since it will be automatically created by the program if it doesn't exist.

### 3. Create a list of repositories
The program requires a text file consisting of the git repositories you want to analyze. Each line in the text file should   include the complete URL of a git repository. (Example: [reposList.txt](reposList.txt))

### 4. Run the JAR file
You can run the JAR file with the following command:
 ```
 java -jar refactoring-analysis.jar [OPTIONS]
 ```
 Note that none of the options are required. Here is a list of available options:
 ```
 -c,--clonepath <file>        directory to temporarily download repositories (default=projects)
 -d,--dbproperties <file>     database properties file (default=database.properties)
 -h,--help                    print this message
 -p,--parallelism <threads>   number of threads for parallel computing (default=1)
 -r,--reposfile <file>        list of repositories to be analyzed (default=reposList.txt)
 ```
 Here is an example command with all the options:
 ```
  java -jar refactoring-analysis.jar -r list.txt -c downloadedRepos -d mydb.properties -p 8 
 ```
 
 #### JVM max heap size
 Depending on the size of the repositories you are analyzing, you might need to tune the maximum size of your JVM's heap memory. This becomes specifically important if you are using the `--parallelism` option to analyze multiple repositories in parallel.
 
 For setting the maximum heap size, you can use the `-Xmx` option. For example, if you want to set it to 50 Gigabytes, you can use this command:
```
java -jar -Xmx50g refactoring-analysis.jar [OPTIONS]
```
