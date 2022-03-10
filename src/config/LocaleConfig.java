package config;

import java.util.IllformedLocaleException;
import java.util.Locale;

public class LocaleConfig {

    public static Locale mLocale;


    public static boolean isLocaleSplit(String splitName) {
        return isLocaleValid(buildLocaleFromSplitName(splitName)) || splitName.endsWith("other_lang");
    }

    private static Locale buildLocaleFromSplitName(String splitName) {
        int configPartIndex = splitName.lastIndexOf("config.");
        if (configPartIndex == -1 || (configPartIndex != 0 && splitName.charAt(configPartIndex - 1) != '.'))
            return null;

        String localeTag = splitName.substring(configPartIndex + ("config.".length()));
        try {
            return new Locale.Builder().setLanguageTag(localeTag).build();
        } catch (IllformedLocaleException e) {
            return null;
        }
    }

    private static boolean isLocaleValid(Locale locale) {
        if (locale == null)
            return false;

        for (Locale validLocale : Locale.getAvailableLocales()) {
            if (validLocale.equals(locale)) {
                mLocale = validLocale;
                return true;
            }
        }
        return false;
    }
}
