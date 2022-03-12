import config.AbiConfig;
import config.LocaleConfig;
import config.ScreenDestinyConfig;
import manifest.AndroidManifestReader;
import manifest.ApkInputSource;
import modle.APK;
import modle.BaseAPK;
import modle.ConfigAPK;
import modle.Dependency;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xmlpull.v1.XmlPullParserException;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.ProcessManifest;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;
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

public class InstallXapk {
    private File appPath;
    private APK baseAPK;
    private Database database = new Database();
    private List<ConfigAPK> configAPK;

    private Dependency dependency;

    private String launchActivity;

    public InstallXapk(String appPath) {
        this.appPath = new File(appPath);
        this.configAPK = new ArrayList<>();
    }

    public Dependency getDependency() {
        return dependency;
    }


    public void preprocessXAPK() {
        for (File apk : Objects.requireNonNull(this.appPath.listFiles((dir, name) -> name.endsWith(".apk")))) {
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
                    ProcessManifest processManifest = new ProcessManifest(this.baseAPK.getLocation());
                    for (AXmlNode node : processManifest.getLaunchableActivityNodes()) {
                        this.launchActivity = node.getAttribute("name").getValue().toString();
                        break;
                    }
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
                    }
                }
            } catch (ParserConfigurationException | IOException | TransformerException | XmlPullParserException e) {
                e.printStackTrace();
            }
        }
    }

    public List<ConfigAPK> getConfigTask() {
        List<ConfigAPK> installTask = new ArrayList<>();

        boolean[] flags = new boolean[]{false, false, false};
        for (ConfigAPK configAPK : this.configAPK) {
            switch (configAPK.getType()) {
                case "Locale" -> {
                    if (!flags[0]) {
                        installTask.add(configAPK);
                        flags[0] = true;
                    }
                }
                case "Abi" -> {
                    if (!flags[1]) {
                        installTask.add(configAPK);
                        flags[1] = true;
                    }
                }
                case "ScreenDensity" -> {
                    if (!flags[2]) {
                        installTask.add(configAPK);
                        flags[2] = true;
                    }
                }
            }
        }

        return installTask;
    }

    public void getDependency(List<ConfigAPK> configTask) {
        if (configTask.size() == 1) {
            // 添加依赖
            dependency = new Dependency(baseAPK, new APK(configTask.get(0).getAppID(), configTask.get(0).getType()), 2);
        } else if (configTask.size() == 2) {
            // 安装一个
            for (ConfigAPK apk : configTask) {
                List<APK> installTask = new ArrayList<>();
                installTask.add(this.baseAPK);
                installTask.add(apk);
                if (install(installTask)) {
                    dependency = new Dependency(baseAPK, new APK(apk.getAppID(), apk.getType()), 2);
                    return;
                }
            }

            StringBuilder type = new StringBuilder();
            // 添加依赖
            for (ConfigAPK apk : configTask) {
                type.append(apk.getType()).append(",");
            }

            dependency = new Dependency(baseAPK, new APK(baseAPK.getAppID(), type.substring(0, type.length() - 1)), 2);
        } else if (configTask.size() == 3) {
            for (ConfigAPK apk : configTask) {
                List<APK> installTask = new ArrayList<>();
                installTask.add(this.baseAPK);
                installTask.add(apk);
                if (install(installTask)) {
                    // 添加依赖
                    dependency = new Dependency(baseAPK, new APK(apk.getAppID(), apk.getType()), 2);
                    return;
                }
            }

            for (int i = 0; i < configTask.size() - 1; i++) {
                for (int j = 1; j < configTask.size(); j++) {
                    List<APK> installTask = new ArrayList<>();
                    installTask.add(this.baseAPK);
                    installTask.add(configTask.get(i));
                    installTask.add(configTask.get(j));
                    if (install(installTask)) {
                        // 添加依赖
                        dependency = new Dependency(baseAPK, new APK(baseAPK.getAppID(), configTask.get(i).getType() + "," + configTask.get(j).getType()), 2);
                        return;
                    }
                }
            }

            // 添加依赖
            StringBuilder type = new StringBuilder();
            for (ConfigAPK apk : configTask) {
                type.append(apk.getType()).append(",");
            }

            dependency = new Dependency(baseAPK, new APK(baseAPK.getAppID(), type.substring(0, type.length() - 1)), 2);
        }
    }

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

    public boolean install(List<APK> installTask) {
        try {
            // 传输文件
            for (APK apk : installTask) {
                printProcess(Runtime.getRuntime().exec(String.format("adb push %s /data/local/tmp/", apk.getLocation().getAbsolutePath())));
            }


            Process p = Runtime.getRuntime().exec("adb shell pm install-create");
            p.waitFor();

            BufferedReader
                    stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
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
                printProcess(Runtime.getRuntime().exec(String.format("adb shell pm install-write %s base%d.apk /data/local/tmp/%s", session, i, installTask.get(i).getLocation().getName())));
            }


            waitInstallFinish(session);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        boolean flag = LaunchAndCheck();
        deleteAndUninstall(installTask);
        return flag;
    }

    private void waitInstallFinish(String session) {
        try {
            boolean flag = true;

            Process p;
            p = Runtime.getRuntime().exec(String.format("adb shell pm install-commit %s", session));
            p.waitFor();

            BufferedReader
                    stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = stdInput.readLine()) != null) {
                if (line.contains("Failure")) {
                    flag = false;
                    System.out.println(line);
                }
            }


            while (flag) {
                p = Runtime.getRuntime().exec("adb shell pm list packages");
                p.waitFor();

                stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

                while ((line = stdInput.readLine()) != null) {
                    if (line.contains(this.baseAPK.getAppID())) {
                        flag = false;
                        System.out.println(line);
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }


    }

    public boolean LaunchAndCheck() {
        try {
            boolean flag = false;
            printProcess(Runtime.getRuntime().exec("adb shell am start -n " + this.baseAPK.getAppID() + "/" + this.launchActivity));
            TimeUnit.SECONDS.sleep(5);

            Process process = Runtime.getRuntime().exec("adb shell dumpsys window | grep mCurrentFocus");
            process.waitFor();

            BufferedReader
                    stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            while ((line = stdInput.readLine()) != null) {
                if (line.contains(this.launchActivity)) {
                    flag = true;
                    System.out.println(line);
                }
            }

            return flag;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
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

    private void deleteAndUninstall(List<APK> installTask) {
        try {
            // 删除apk包
            for (APK apk : installTask) {
                printProcess(Runtime.getRuntime().exec("adb shell rm /data/local/tmp/" + apk.getLocation().getName()));
            }
            // 卸载程序
            Runtime.getRuntime().exec("adb uninstall " + this.baseAPK.getAppID());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        InstallXapk installXapk = new InstallXapk("/Volumes/Data/apk_pure/test/3_Tiles");
        installXapk.preprocessXAPK();
        System.out.println(installXapk.getConfigTask());
        installXapk.getDependency(installXapk.getConfigTask());
//        System.out.println(installXapk.dependencies);
    }
}
