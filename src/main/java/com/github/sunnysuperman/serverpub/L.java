package com.github.sunnysuperman.serverpub;

public class L {

    public static interface SimpleLogger {

        void info(String msg);

        void error(String msg, Throwable ex);
    }

    private static SimpleLogger sLogger;

    public static void register(SimpleLogger logger) {
        sLogger = logger;
    }

    public static void info(String msg) {
        if (sLogger != null) {
            sLogger.info(msg);
        } else {
            System.out.println(msg);
        }
    }

    public static void error(String msg, Throwable ex) {
        if (sLogger != null) {
            sLogger.error(msg, ex);
        } else {
            if (msg != null) {
                System.err.println(msg);
            }
            if (ex != null) {
                ex.printStackTrace();
            }
        }
    }

    public static void error(String msg) {
        error(msg, null);
    }

    public static void error(Throwable ex) {
        error(null, ex);
    }
}
