package config;

import java.util.HashSet;
import java.util.Set;

public class AbiConfig {
    public static final String ARMEABI = "armeabi";
    public static final String ARMEABI_V7A = "armeabi_v7a";
    public static final String ARM64_V8A = "arm64_v8a";
    public static final String X86 = "x86";
    public static final String X86_64 = "x86_64";
    public static final String MIPS = "mips";
    public static final String MIPS64 = "mips64";

    private static Set<String> ALL_ABIS = new HashSet<>();

    static {
        ALL_ABIS.add(ARMEABI);
        ALL_ABIS.add(ARMEABI_V7A);
        ALL_ABIS.add(ARM64_V8A);
        ALL_ABIS.add(X86);
        ALL_ABIS.add(X86_64);
        ALL_ABIS.add(MIPS);
        ALL_ABIS.add(MIPS64);
    }

    public static String mAbi;


    public String abi() {
        return mAbi;
    }

    public static boolean isAbiSplit(String splitName) {
        return getAbiFromSplitName(splitName) != null;
    }

    private static String getAbiFromSplitName(String splitName) {
        int configPartIndex = splitName.lastIndexOf("config.");
        if (configPartIndex == -1 || (configPartIndex != 0 && splitName.charAt(configPartIndex - 1) != '.'))
            return null;

        String abi = splitName.substring(configPartIndex + ("config.".length()));
        if (ALL_ABIS.contains(abi)) {
            mAbi = abi;
            return abi;
        }

        return null;
    }
}
