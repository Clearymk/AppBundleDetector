import config.AbiConfig;
import config.LocaleConfig;
import config.ScreenDestinyConfig;
import manifest.AndroidManifestReader;
import manifest.ApkInputSource;
import modle.APK;
import modle.BaseAPK;
import modle.ConfigAPK;
import modle.SplitAPK;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
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

public class AppBundleDetectorXAPK {
    private File apkFile;
    private List<String[]> dependencies;
    private File outputFile;
    public Database database;

    private BaseAPK baseAPK;
    private List<ConfigAPK> configAPK;
    private List<SplitAPK> splitAPK;


    public AppBundleDetectorXAPK(String apkPath, Database database) {
        this.apkFile = new File(apkPath);
        this.dependencies = new ArrayList<>();
        this.splitAPK = new ArrayList<>();
        this.configAPK = new ArrayList<>();
        this.database = database;
    }

    private void decompressXAPK(String xapkPath) {
        ZipFile xapk = new ZipFile(xapkPath);
        String outputPath = xapkPath.replaceAll(".xapk", "").replaceAll(" ", "_");

        try {
            xapk.extractAll(outputPath);
        } catch (ZipException e) {
            e.printStackTrace();
        }

        this.outputFile = new File(outputPath);
    }

    public void preprocessXAPK() {
        for (File apk : Objects.requireNonNull(this.outputFile.listFiles((dir, name) -> name.endsWith(".apk")))) {
            try {
                // 得到每个apk的manifest
                Document manifest = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                TransformerFactory.newInstance().newTransformer().transform(
                        new SAXSource(new AndroidManifestReader(), new ApkInputSource(apk.getPath())), new DOMResult(manifest));
                // 第一步 判断apk类型
                Element manifestElement = (Element) manifest.getElementsByTagName("manifest").item(0);
                String appID = manifestElement.getAttribute("package");

                if (manifestElement.getAttribute("split").equals("")) {
                    // 为base apk
                    this.baseAPK = new BaseAPK(appID, apk, manifestElement);
                } else {
                    // 为split apk 或 config apk
                    boolean[] isConfig = checkConfigApk(apk.getName());
                    String subAppID = manifestElement.getAttribute("split");
                    // 如果是config apk
                    if (isConfig[0]) {
                        this.configAPK.add(new ConfigAPK("Locale", appID, subAppID, apk, manifestElement));
                    } else if (isConfig[1]) {
                        this.configAPK.add(new ConfigAPK("Abi", appID, subAppID, apk, manifestElement));
                    } else if (isConfig[2]) {
                        this.configAPK.add(new ConfigAPK("ScreenDensity", appID, subAppID, apk, manifestElement));
                    } else {
                        //如果是split apk
                        this.splitAPK.add(new SplitAPK(appID, subAppID, apk, manifestElement));
                    }
                }
            } catch (ParserConfigurationException | IOException | TransformerException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isProcessedBefore(APK apk) {
        if (this.database.queryApkIdByAppId(apk.getAppID())) {
            System.out.println(apk.getLocation().getName() + " processed before skip");
            return true;
        }
        return false;
    }

    public void run() {
        if (!this.apkFile.isFile() || !this.apkFile.getName().endsWith(".xapk")) {
            return;
        }

        System.out.println("decompress xapk " + this.apkFile.getName());
        decompressXAPK(this.apkFile.getPath());
        System.out.println("preprocess apks " + this.apkFile.getName());
        preprocessXAPK();
        if(!isProcessedBefore(this.baseAPK)) {
            System.out.println("process base apk " + this.baseAPK.getLocation().getName());
            new APKParser(this.baseAPK, this.database).parse();
            System.out.println("process split apk");
            for (SplitAPK splitAPK : this.splitAPK) {
                System.out.println("process " + splitAPK.getLocation().getName());
                new APKParser(splitAPK, this.database).parse();
                System.out.println("++++++++++");
            }
        }

        System.out.println("------------");
    }


    // 0:locale 1:abi 2:destiny
    private boolean[] checkConfigApk(String apkName) {
        boolean[] isConfigApk = new boolean[]{false, false, false};
        apkName = apkName.substring(0, apkName.length() - 4);
        if (apkName.contains("config.")) {
            if (LocaleConfig.isLocaleSplit(apkName)) {
                isConfigApk[0] = true;
            } else if (AbiConfig.isAbiSplit(apkName)) {
                isConfigApk[1] = true;
            } else if (ScreenDestinyConfig.isScreenDensitySplit(apkName)) {
                isConfigApk[2] = true;
            }
        }

        return isConfigApk;
    }

    public static void main(String[] args) throws IOException {
        AppBundleDetectorXAPK detectorXAPK = new AppBundleDetectorXAPK("test/test/Brave Private Web Browser_v1.35.103_apkpure.com.xapk", new Database());
        detectorXAPK.run();
    }
}
