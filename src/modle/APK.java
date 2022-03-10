package modle;

import org.w3c.dom.Element;

import java.io.File;

public class APK {
    private String appID;
    private String subAppID;
    private Element manifest;
    private File location;

    public APK(String appID, String subAppID, String path, Element manifest) {
        this.appID = appID;
        this.subAppID = subAppID;
        this.manifest = manifest;
        this.location = new File(path);
    }

    public APK(String appID, String subAppID, File location, Element manifest) {
        this.appID = appID;
        this.location = location;
        this.manifest = manifest;
        this.subAppID = subAppID;
    }


    @Override
    public String toString() {
        return "APK{" +
                "appID='" + appID + '\'' +
                ", subAppID='" + subAppID + '\'' +
                '}';
    }

    public APK(String appID, String subAppID) {
        this.appID = appID;
        this.subAppID = subAppID;
    }

    public File getLocation() {
        return location;
    }


    public String getAppID() {
        return appID;
    }

    public Element getManifest() {
        return manifest;
    }

    public String getSubAppID() {
        return subAppID;
    }
}
