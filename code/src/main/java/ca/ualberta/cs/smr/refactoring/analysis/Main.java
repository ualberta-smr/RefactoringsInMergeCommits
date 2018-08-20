package ca.ualberta.cs.smr.refactoring.analysis;

import org.apache.commons.cli.*;

import java.io.File;

public class Main {

    private static final int DEFAULT_PARALLELISM = 1;
    private static final String DEFAULT_REPOS_FILE = "reposList.txt";
    private static final String DEFAULT_CLONE_PATH = "projects";
    private static final String DEFAULT_DB_PROPERTIES_FILE = "database.properties";


    public static void main(String[] args) {
        Parser parser = new GnuParser();
        try {
            CommandLine commandLine = parser.parse(createOptions(), args);


            if (commandLine.hasOption("h")){
                printHelp();
                return;
            }

            int parallelism = DEFAULT_PARALLELISM;
            String reposFile = DEFAULT_REPOS_FILE;
            String clonePath = DEFAULT_CLONE_PATH;
            String dbPropertiesFile = DEFAULT_DB_PROPERTIES_FILE;

            if (commandLine.hasOption("r")) {
                reposFile = commandLine.getOptionValue("r");
            }
            if (commandLine.hasOption("c")) {
                clonePath = commandLine.getOptionValue("c");
            }
            if (commandLine.hasOption("d")) {
                dbPropertiesFile = commandLine.getOptionValue("d");
            }
            if (commandLine.hasOption("p")) {
                parallelism = Integer.valueOf(commandLine.getOptionValue("p"));
            }

            dbPropertiesFile = (new File(dbPropertiesFile)).getAbsolutePath();
            System.setProperty("env.connections.file", dbPropertiesFile);

            RefactoringAnalysis refactoringAnalysis = new RefactoringAnalysis(reposFile, clonePath);
            refactoringAnalysis.start(parallelism);
        } catch (Exception e) {
            e.printStackTrace();
            printHelp();
        }
    }

    private static Options createOptions() {
        Options options = new Options();
        options.addOption("h", "help", false, "print this message");
        options.addOption(OptionBuilder.withLongOpt("reposfile")
                .withDescription(String.format("list of repositories to be analyzed (default=%s)", DEFAULT_REPOS_FILE))
                .hasArgs()
                .withArgName("file")
                .isRequired(false)
                .create("r"));

        options.addOption(OptionBuilder.withLongOpt("clonepath")
                .withDescription(String.format("directory to temporarily download repositories (default=%s)", DEFAULT_CLONE_PATH))
                .hasArgs()
                .withArgName("file")
                .isRequired(false)
                .create("c"));

        options.addOption(OptionBuilder.withLongOpt("dbproperties")
                .withDescription(String.format("database properties file (default=%s)", DEFAULT_DB_PROPERTIES_FILE))
                .hasArgs()
                .withArgName("file")
                .isRequired(false)
                .create("d"));

        options.addOption(OptionBuilder.withLongOpt("parallelism")
                .withDescription(String.format("number of threads for parallel computing (default=%d)", DEFAULT_PARALLELISM))
                .hasArgs()
                .withArgName("threads")
                .isRequired(false)
                .create("p"));
        return options;
    }

    private static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "java -jar refactoring-analysis.jar [OPTIONS]", createOptions());
    }
}
