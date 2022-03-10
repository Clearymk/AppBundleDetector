package modle;

import org.w3c.dom.Element;

import java.io.File;

public class BaseAPK extends APK {
    public BaseAPK(String appID, File location, Element manifest) {
        super(appID, appID, location, manifest);
    }
}
