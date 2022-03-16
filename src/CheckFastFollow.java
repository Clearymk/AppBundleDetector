import manifest.AndroidManifestReader;
import manifest.ApkInputSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class CheckFastFollow {
    private File apksPath;
    private Database database;

    public CheckFastFollow(String apksPath) {
        this.apksPath = new File(apksPath);
        this.database = new Database();
    }


    public void run() {
        boolean flag = false;
        for (File xapk : Objects.requireNonNull(this.apksPath.listFiles())) {
            if (xapk.isDirectory()) {
                if (!xapk.getName().equals("")) {
                    flag = true;
                }

                if (flag) {
                    for (File apk : Objects.requireNonNull(xapk.listFiles())) {
                        if (apk.getName().endsWith(".apk")) {
                            try {
                                Document manifest = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                                TransformerFactory.newInstance().newTransformer().transform(
                                        new SAXSource(new AndroidManifestReader(), new ApkInputSource(apk.getPath())), new DOMResult(manifest));
                                // 第一步 判断apk类型
                                Element manifestElement = (Element) manifest.getElementsByTagName("manifest").item(0);
                                String appID = manifestElement.getAttribute("package");
                                if (this.database.queryStatue5ByAppId(appID)) {
                                    InstallSingleXAPK installSingleXAPK = new InstallSingleXAPK(xapk.getPath(), "emulator-5556");
                                    installSingleXAPK.checkPath();
                                    installSingleXAPK.install();
                                    installSingleXAPK.LaunchAndCheck();
                                    installSingleXAPK.deleteAndUninstall();
                                    System.out.println("-----------");
                                }
                                break;
                            } catch (ParserConfigurationException | IOException | TransformerException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        CheckFastFollow checkFastFollow = new CheckFastFollow("/Volumes/Data/apk_pure/download_x/");
        checkFastFollow.run();
    }
}
