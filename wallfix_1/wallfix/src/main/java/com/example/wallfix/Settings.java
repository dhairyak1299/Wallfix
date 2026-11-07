package com.example.wallfix;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/** Module: remembers the blocks-per-second setting in config/wallfix.properties. Never throws; a broken file just means defaults. */
public final class Settings {
    private static final File FILE = new File("config", "wallfix.properties");

    private Settings() {}

    public static double loadRate() {
        FileInputStream in = null;
        try {
            if (!FILE.exists()) return Config.DEFAULT_PLACE_PER_SEC;
            Properties p = new Properties();
            in = new FileInputStream(FILE);
            p.load(in);
            double r = Double.parseDouble(p.getProperty("blocksPerSecond", String.valueOf(Config.DEFAULT_PLACE_PER_SEC)));
            return Math.max(Config.MIN_RATE, Math.min(Config.MAX_RATE, r));
        } catch (Exception e) {
            return Config.DEFAULT_PLACE_PER_SEC;
        } finally {
            if (in != null) try { in.close(); } catch (IOException ignored) {}
        }
    }

    public static void saveRate(double rate) {
        FileOutputStream out = null;
        try {
            File dir = FILE.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            Properties p = new Properties();
            p.setProperty("blocksPerSecond", String.valueOf(rate));
            out = new FileOutputStream(FILE);
            p.store(out, "WallFix settings");
        } catch (Exception ignored) {
        } finally {
            if (out != null) try { out.close(); } catch (IOException ignored) {}
        }
    }
}
