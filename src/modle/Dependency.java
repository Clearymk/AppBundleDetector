package modle;

public class Dependency {
    private APK srcAPK;
    private APK destAPK;
    // type 1 module之间的依赖;
    // type 2 base 与 config 之间的依赖(requiredSplitTypes);
    // type 3 base 与 config 之间的依赖(isSplitRequired), 需后续处理;
    private int type;

    public Dependency(APK srcAPK, APK destAPK, int type) {
        this.srcAPK = srcAPK;
        this.destAPK = destAPK;
        this.type = type;
    }

    public APK getSrcAPK() {
        return srcAPK;
    }

    public APK getDestAPK() {
        return destAPK;
    }

    public int getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Dependency{" +
                "srcAPK=" + srcAPK +
                ", destAPK=" + destAPK +
                ", type=" + type +
                '}';
    }
}
