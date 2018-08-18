package ca.ualberta.cs.smr.refactoring.analysis.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class Utils {

    private static final boolean LOG_TO_FILE  = true;
    private static final String LOG_FILE = "analysis_log.txt";

    public static void log(String projectName, Object message) {
        String timeStamp = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss z").format(new Date());
        String logMessage = timeStamp + " ";
        if (message instanceof String){
            logMessage += (String) message;
        } else if (message instanceof Exception) {
            logMessage += ((Exception) message).getMessage() + "\n";
            StringBuilder stackBuilder = new StringBuilder();
            StackTraceElement[] stackTraceElements = ((Exception) message).getStackTrace();
            for (int i = 0; i < stackTraceElements.length; i++) {
                StackTraceElement stackTraceElement = stackTraceElements[i];
                stackBuilder.append(stackTraceElement.toString());
                if (i < stackTraceElements.length - 1) stackBuilder.append("\n");
            }
            logMessage += stackBuilder.toString();
        } else {
            logMessage = message.toString();
        }
        System.out.println(logMessage);

        if (LOG_TO_FILE) {
            String logPath = LOG_FILE;
            if (projectName != null && !projectName.trim().equals("")) logPath = projectName;
            try {
                new File("logs").mkdirs();
                Files.write(Paths.get("logs/" + logPath + ".log"), Arrays.asList(logMessage),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static String runSystemCommand(String dir, String... commands) {
        StringBuilder builder = new StringBuilder();
        try {
//            if (verbose) {
//                for (String command : commands) {
//                    System.out.print(command + " ");
//                }
//                System.out.println();
//            }
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(commands, null, new File(dir));

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));

            String s = null;
            while ((s = stdInput.readLine()) != null) {
                builder.append(s);
                builder.append("\n");
//                if (verbose) log(s);
            }

            while ((s = stdError.readLine()) != null) {
                builder.append(s);
                builder.append("\n");
//                if (verbose) log(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }
}
