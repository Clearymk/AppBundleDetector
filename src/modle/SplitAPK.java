package modle;

import org.w3c.dom.Element;

import java.io.File;

public class SplitAPK extends APK{
    public SplitAPK(String appID, String subAppID, File location, Element manifest) {
        super(appID, subAppID, location, manifest);
    }
}
