import config.AbiConfig;
import config.LocaleConfig;
import config.ScreenDestinyConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParserException;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.ProcessManifest;

public class    InstallXapk {
    private String appPath;
    private List<File> installTask = new ArrayList<>();
    private String appId;
    private String launchActivity;

    public InstallXapk(String appPath) {
        this.appPath = appPath;
    }

    // 检查manifest得到base apk 和 split apk
    public void checkPath() {
        File appFile = new File(appPath);

        if (appFile.isDirectory()) {
            int[] flag = {0, 0, 0};
            for (File splitApk : Objects.requireNonNull(appFile.listFiles())) {
                if (splitApk.getName().endsWith(".apk")) {
                    if (splitApk.getName().contains("config.")) {
                        if (LocaleConfig.isLocaleSplit(splitApk.getName().substring(0, splitApk.getName().length() - 4))) {
                            if (flag[0] == 0) {
                                this.installTask.add(splitApk);
                                flag[0] = 1;
                            }
                        } else if (AbiConfig.isAbiSplit(splitApk.getName().substring(0, splitApk.getName().length() - 4))) {
                            if (flag[1] == 0) {
                                this.installTask.add(splitApk);
                                flag[1] = 1;
                            }
                        } else if (ScreenDestinyConfig.isScreenDensitySplit(splitApk.getName().substring(0, splitApk.getName().length() - 4))) {
                            if (flag[2] == 0) {
                                this.installTask.add(splitApk);
                                flag[2] = 1;
                            }
                        }
                    } else {
                        if (checkBaseApk(splitApk.getAbsolutePath())) {
                            this.installTask.add(splitApk);
                        }
                    }
                }
            }
        }
    }

    private boolean checkBaseApk(String apkPath) {
        try {
            ProcessManifest manifest = new ProcessManifest(apkPath);
            this.appId = manifest.getAXml().getNodesWithTag("manifest").get(0).getAttribute("package").getValue().toString();
            for (AXmlNode node : manifest.getLaunchableActivityNodes()) {
                this.launchActivity = node.getAttribute("name").getValue().toString();
                break;
            }

            if (manifest.getAXml().getNodesWithTag("manifest").get(0).getAttribute("split") == null) {
                return true;
            }
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void install() {
        try {
            // 传输文件
            for (File apkFile : this.installTask) {
                Runtime.getRuntime().exec(String.format("adb push \"%s\" /data/local/tmp/", apkFile.getAbsolutePath())).waitFor();
            }


            Process p = Runtime.getRuntime().exec("adb shell pm install-create");
            p.waitFor();

            BufferedReader
                    stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            String session = null;
            while ((line = stdInput.readLine()) != null) {
                String regex = "\\[(.*?)]";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    session = matcher.group();
                    session = session.replace("[", "");
                    session = session.replace("]", "");
                }
                System.out.println(line);
            }

            for (int i = 0; i < installTask.size(); i++) {
                printProcess(Runtime.getRuntime().exec(String.format("adb shell pm install-write %s base%d.apk /data/local/tmp/%s", session, i, installTask.get(i).getName())));
            }

            printProcess(Runtime.getRuntime().exec(String.format("adb shell pm install-commit %s", session)));

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void LaunchAndCheck() {
        try {
            printProcess(Runtime.getRuntime().exec("adb shell am start -n " + this.appId + "/" + this.launchActivity));
            TimeUnit.MINUTES.sleep(1);

            Process p = Runtime.getRuntime().exec("adb shell cd /data/data/" + this.appId + "/files/assetpack");
            p.waitFor();

            BufferedReader
                    stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            boolean flag = true;
            while ((line = stdInput.readLine()) != null) {
                if (line.contains("No such file or directory")) {
                    flag = false;
                }
            }

            if (flag) {
                createFastFollowRecord();
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createFastFollowRecord() {
        System.out.println("fast follow find");
//        Database database = new Database();
//        database.insertApk(this.appId, "fast_follow", new String[]{"0", "0", "0", "0", "1", "0"}, 0);
    }

    private void printProcess(Process p) {
        try {
            p.waitFor();
            BufferedReader
                    stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(p.getErrorStream()));
            String line = null;

            while ((line = stdInput.readLine()) != null) {
                System.out.println(line);
            }

            while ((line = stdError.readLine()) != null) {
                System.out.println(line);
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteAndUninstall() {
        try {
            // 删除apk包
            for (File file : installTask) {
                printProcess(Runtime.getRuntime().exec("adb shell rm /data/local/tmp/" + file.getName()));
            }
            // 卸载程序
            Runtime.getRuntime().exec("adb uninstall " + this.appId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        InstallXapk installXapk = new InstallXapk("D:\\IDEA\\code\\AppBundleDetector\\test\\Asphalt 8");
        installXapk.checkPath();
        installXapk.install();
        installXapk.LaunchAndCheck();
        installXapk.deleteAndUninstall();
        System.out.println(installXapk.installTask);
    }
}
