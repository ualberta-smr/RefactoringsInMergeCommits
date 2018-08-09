package ca.ualberta.smr.refactoring.analysis.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    public static void log(String message) {
        String timeStamp = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss z").format(new Date());
        System.out.println(timeStamp + " " + message);
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
