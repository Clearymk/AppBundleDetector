import manifest.AndroidManifestReader;
import manifest.ApkInputSource;
import modle.APK;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ReCheck {
    private File apksPath;
    private Database database;

    public ReCheck(String apksPath) {
        this.apksPath = new File(apksPath);
        this.database = new Database();
    }


    public void run() {
        for (File xapk : Objects.requireNonNull(this.apksPath.listFiles())) {
            if (xapk.isDirectory()) {
                for (File apk : Objects.requireNonNull(xapk.listFiles())) {
                    if (apk.getName().endsWith(".apk")) {
                        try {
                            Document manifest = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                            TransformerFactory.newInstance().newTransformer().transform(
                                    new SAXSource(new AndroidManifestReader(), new ApkInputSource(apk.getPath())), new DOMResult(manifest));
                            // 第一步 判断apk类型
                            Element manifestElement = (Element) manifest.getElementsByTagName("manifest").item(0);
                            String appID = manifestElement.getAttribute("package");
                            if (this.database.queryDependencyTypeByAppID(appID) == 2 && !this.database.queryDependencyIDByAppID(appID).contains(",")) {
                                InstallXapk installXapk = new InstallXapk(xapk.getPath(), "emulator-5558");
                                installXapk.preprocessXAPK();
                                List<APK> installTask = new ArrayList<>();
                                installTask.add(installXapk.baseAPK);
                                if (installXapk.install(installTask)) {
                                    database.updateDependencyUnknown("base", installXapk.baseAPK.getAppID());
                                    System.out.println("[+] update " + installXapk.baseAPK.getAppID() + " dependency to base");
                                    System.out.println("-----------");
                                }
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

    public static void main(String[] args) {
        ReCheck reCheck = new ReCheck("/Volumes/Data/apk_pure/download_x/");
        reCheck.run();
    }
}
