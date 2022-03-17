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

    public void copy() {
        List<String> appIds_1 = database.queryApkIdByStatue(1);
        List<String> appIds_2 = database.queryApkIdByStatue(2);
        List<String> appIds_3 = database.queryApkIdByStatue(3);
        List<String> appIds_4 = database.queryApkIdByStatue(4);
        List<String> appIds_5 = database.queryApkIdByStatue(5);
        List<String> appIds_6 = database.queryApkIdByStatue(6);
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

                            if (appIds_1.contains(appID)) {
                                FileUtils.copyDirectory(file, new File(outputPath + File.separator + featureInstallTime + File.separator + file.getName()));
                                System.out.println("copy " + file.getPath() + " to " + assetInstallTime);
                            }
                            if (appIds_2.contains(appID)) {
                                FileUtils.copyDirectory(file, new File(outputPath + File.separator + featureCondition + File.separator + file.getName()));
                                System.out.println("copy " + file.getPath() + " to " + assetFastFollow);
                            }
                            if (appIds_3.contains(appID)) {
                                FileUtils.copyDirectory(file, new File(outputPath + File.separator + featureOnDemand + File.separator + file.getName()));
                                System.out.println("copy " + file.getPath() + " to " + assetOndemand);
                            }

                            if (appIds_4.contains(appID)) {
                                FileUtils.copyDirectory(file, new File(outputPath + File.separator + assetInstallTime + File.separator + file.getName()));
                                System.out.println("copy " + file.getPath() + " to " + assetInstallTime);
                            }
                            if (appIds_5.contains(appID)) {
                                FileUtils.copyDirectory(file, new File(outputPath + File.separator + assetFastFollow + File.separator + file.getName()));
                                System.out.println("copy " + file.getPath() + " to " + assetFastFollow);
                            }
                            if (appIds_6.contains(appID)) {
                                FileUtils.copyDirectory(file, new File(outputPath + File.separator + assetOndemand + File.separator + file.getName()));
                                System.out.println("copy " + file.getPath() + " to " + assetOndemand);
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
        copy.copy();
    }

}
