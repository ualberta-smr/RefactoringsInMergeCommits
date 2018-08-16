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
    private static final String LOG_PATH = "analysis_log.txt";

    public static void log(String message) {
        String timeStamp = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss z").format(new Date());
        String logLine = timeStamp + " " + message;
        System.out.println(logLine);

        if (LOG_TO_FILE) {
            try {
                Files.write(Paths.get(LOG_PATH), Arrays.asList(logLine), StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static String runSystemCommand(String dir, boolean verbose, String... commands) {
        StringBuilder builder = new StringBuilder();
        try {
            if (verbose) {
                for (String command : commands) {
                    System.out.print(command + " ");
                }
                System.out.println();
            }
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
                if (verbose) log(s);
            }

            while ((s = stdError.readLine()) != null) {
                builder.append(s);
                builder.append("\n");
                if (verbose) log(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }
}
