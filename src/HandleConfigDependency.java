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
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

public class HandleConfigDependency {
    private File apksPath;
    private Database database;
    private Queue<File> processTask;

    public HandleConfigDependency(String apksPath) {
        this.apksPath = new File(apksPath);
        this.database = new Database();
        this.processTask = new ArrayDeque<>();
    }

    private void addTask() {
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
                            if (this.database.queryDependencyTypeByAppID(appID) == 3) {
                                processTask.add(xapk);
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

    public void run() {
        addTask();

        for (File xapk : processTask) {
            InstallXapk installXapk = new InstallXapk(xapk.getPath());
            installXapk.preprocessXAPK();
            installXapk.getDependency(installXapk.getConfigTask());
            APK srcApk = installXapk.getDependency().getDestAPK();
            database.updateDependencyUnknown(srcApk.getSubAppID(), srcApk.getAppID());
        }
    }

    public static void main(String[] args) {
        HandleConfigDependency handleConfigDependency = new HandleConfigDependency("/Volumes/Data/apk_pure/download_x/");
        handleConfigDependency.run();
    }
}
