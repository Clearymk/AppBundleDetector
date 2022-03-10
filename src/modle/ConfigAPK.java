package modle;

import org.w3c.dom.Element;

import java.io.File;

public class ConfigAPK extends APK{
    private String type;

    public ConfigAPK(String type, String appID, String subAppID, File location, Element manifest) {
        super(appID, subAppID, location, manifest);
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
