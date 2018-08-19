# Refactoring Analysis
This project analyzes merge commits in git repositries and determines whether refactoring changes are to blame for the conflicts.


## System requirements
* Linux or macOS
* git
* Java 8
* Maven
* MySQL 5.7

## How to run

### Make an executable JAR
Clone the repository and navigate to `code`. Run this command to create an executable JAR file:
```
TODO: MAVEN_COMMAND
```

### Database configuration
Create a `database.properties` file similar to the template below, with your MySQL username and password:
```
development.driver=com.mysql.jdbc.Driver
development.username=%username%
development.password=%password%
development.url=jdbc:mysql://localhost/refactoring_analysis
```
If you wish, you can choose a different name for the database (`refactoring_analysis` in the template). You don't need to create the database, since it will be automatically created by the program if it doesn't exist.

### List of repositories
The program requires a text file consisting of the git repositories you want to analyze. Each line in the textfile should  include the URL of a git repository.

### Run the JAR file
You can run the file with the following command:
 ```
 java -jar refactoring-analysis.jar "/path/to/reposList.txt" -Denv.connections.file=/path/to/file/database.properties
 ```
