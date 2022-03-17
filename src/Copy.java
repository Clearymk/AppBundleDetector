import manifest.AndroidManifestReader;
import manifest.ApkInputSource;
import org.apache.commons.io.FileUtils;
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
import java.util.List;
import java.util.Objects;

public class Copy {
    private static final String inputPath = "/Volumes/Data/apk_pure/download_x";
    private static final String outputPath = "/Volumes/Data/result";
    private static final String featureInstallTime = "feature_install_time";
    private static final String featureOnDemand = "feature_ondemand";
    private static final String featureCondition = "feature_condition";
    private static final String assetInstallTime = "asset_install_time";
    private static final String assetOndemand = "asset_ondemand";
    private static final String assetFastFollow = "asset_fastfollow";
    private Database database;

    public Copy() {
        this.database = new Database();
    }

    public void copy(int status, String folder) {
        List<String> appIds = database.queryApkIdByStatue(status);
        for (File file : Objects.requireNonNull(new File(inputPath).listFiles())) {
            if (file.isDirectory()) {
                for (File apk : Objects.requireNonNull(file.listFiles())) {
                    if (apk.getName().endsWith(".apk")) {
                        try {
                            Document manifest = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                            TransformerFactory.newInstance().newTransformer().transform(
                                    new SAXSource(new AndroidManifestReader(), new ApkInputSource(apk.getPath())), new DOMResult(manifest));
                            Element manifestElement = (Element) manifest.getElementsByTagName("manifest").item(0);
                            String appID = manifestElement.getAttribute("package");
//                            System.out.println(appID);
                            if (appIds.contains(appID)) {
                                FileUtils.copyDirectory(file, new File(outputPath + File.separator + folder + File.separator + file.getName()));
                                System.out.println("copy " + apk.getPath() + " to " + folder);
                            }
                            break;
                        } catch (TransformerException | ParserConfigurationException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        Copy copy = new Copy();
        copy.copy(2, Copy.featureOnDemand);
    }

}
