package uppa.group2;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static boolean debug = false;

    private Logger() {}

    public static void setDebug(boolean enabled) {
        debug = enabled;
    }

    public static void info(String msg) {
        System.out.println("[" + time() + "] [INFO]  " + msg);
    }

    public static void warn(String msg) {
        System.out.println("[" + time() + "] [WARN]  " + msg);
    }

    public static void error(String msg) {
        System.err.println("[" + time() + "] [ERROR] " + msg);
    }

    public static void debug(String msg) {
        if (debug) System.out.println("[" + time() + "] [DEBUG] " + msg);
    }

    private static String time() {
        return LocalTime.now().format(FMT);
    }
}
