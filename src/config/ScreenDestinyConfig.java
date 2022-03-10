package config;

import java.util.HashMap;
import java.util.Map;

public class ScreenDestinyConfig {
    private static final String LDPI = "ldpi";
    private static final String MDPI = "mdpi";
    private static final String TVDPI = "tvdpi";
    private static final String HDPI = "hdpi";
    private static final String XHDPI = "xhdpi";
    private static final String XXHDPI = "xxhdpi";
    private static final String XXXHDPI = "xxxhdpi";
    private static final int DENSITY_LOW = 120;
    private static final int DENSITY_MEDIUM = 160;
    private static final int DENSITY_TV = 213;
    private static final int DENSITY_HIGH = 240;
    private static final int DENSITY_XHIGH = 320;
    private static final int DENSITY_XXHIGH = 480;
    private static final int DENSITY_XXXHIGH = 640;

    private static final Map<String, Integer> DENSITY_NAME_TO_DENSITY = new HashMap<>();

    static {
        DENSITY_NAME_TO_DENSITY.put(LDPI, DENSITY_LOW);
        DENSITY_NAME_TO_DENSITY.put(MDPI, DENSITY_MEDIUM);
        DENSITY_NAME_TO_DENSITY.put(TVDPI, DENSITY_TV);
        DENSITY_NAME_TO_DENSITY.put(HDPI, DENSITY_HIGH);
        DENSITY_NAME_TO_DENSITY.put(XHDPI, DENSITY_XHIGH);
        DENSITY_NAME_TO_DENSITY.put(XXHDPI, DENSITY_XXHIGH);
        DENSITY_NAME_TO_DENSITY.put(XXXHDPI, DENSITY_XXXHIGH);
    }

    public static String mDensityName;
    private int mDensity;


    public int density() {
        return mDensity;
    }

    public String densityName() {
        return mDensityName;
    }

    public static boolean isScreenDensitySplit(String splitName) {
        return getDensityFromSplitName(splitName) != null;
    }

    public static String getDensityFromSplitName(String splitName) {
        int configPartIndex = splitName.lastIndexOf("config.");
        if (configPartIndex == -1 || (configPartIndex != 0 && splitName.charAt(configPartIndex - 1) != '.'))
            return null;

        String densityName = splitName.substring(configPartIndex + ("config.".length()));
        if (DENSITY_NAME_TO_DENSITY.containsKey(densityName)) {
            mDensityName = densityName;
            return densityName;
        }
        return null;
    }
}
