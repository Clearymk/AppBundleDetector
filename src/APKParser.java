import manifest.AndroidManifestReader;
import manifest.ApkInputSource;
import modle.APK;
import modle.Dependency;
import org.w3c.dom.*;
import soot.*;
import soot.jimple.Stmt;
import soot.options.Options;


import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class APKParser {
    private APK apk;
    private String filter;
    private Boolean isFeature;
    private List<Dependency> dependencies;
    private List<String> featureInvokeLines = new ArrayList<>();
    private List<String> assetInvokeLines = new ArrayList<>();
    private final String androidJarPath = "/Users/clear/Library/Android/sdk/platforms";
    private List<Condition> conditions = new ArrayList<>();
    private final String START_INSTALL = "<com.google.android.play.core.splitinstall.SplitInstallManager: com.google.android.play.core.tasks.Task startInstall(com.google.android.play.core.splitinstall.SplitInstallRequest)>";
    private final String FETCH = "<com.google.android.play.core.assetpacks.AssetPackManager: com.google.android.play.core.tasks.Task fetch(java.util.List)>";
    private final String DEFERRED_INSTALL = "<com.google.android.play.core.splitinstall.SplitInstallManager: com.google.android.play.core.tasks.Task deferredInstall(java.util.List)>";
    public String[] features = {"0", "0", "0", "0", "0", "0", "0", "0"};

    public APKParser(APK apk) {
        this.apk = apk;
        this.dependencies = new ArrayList<>();
        this.isFeature = false;
        getBaseAppInfo();
    }

    public void getBaseAppInfo() {
        try {
            if (this.apk.getAppID().split("\\.").length < 2) {
                this.filter = this.apk.getAppID();
            } else {
                this.filter = this.apk.getAppID().split("\\.")[0] + "." + this.apk.getAppID().split("\\.")[1];
            }

            // 通过解压apk的方式检查 apk 中是否含有代码, 并且解压出manifest文件
            ZipFile zipFile = new ZipFile(this.apk.getLocation());

            Enumeration<? extends ZipEntry> files = zipFile.entries();
            boolean[] features = new boolean[]{false, false, false, false};
            while (files.hasMoreElements()) {
                ZipEntry entry = files.nextElement();
                if (entry.getName().startsWith("res/")) {
                    features[0] = true;
                } else if (entry.getName().startsWith("lib/")) {
                    features[1] = true;
                } else if (entry.getName().startsWith("apex/")) {
                    features[2] = true;
                } else if (entry.getName().endsWith(".dex")) {
                    features[3] = true;
                }
            }

            this.isFeature = features[0] || features[1] || features[2] || features[3];
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 检查manifest 得到delivery元素中的信息
    public void parseManifest() {
        NodeList dependenciesList = this.apk.getManifest().getElementsByTagName("uses-split");
        // 处理模块之间的依赖
        for (int i = 0; i < dependenciesList.getLength(); i++) {
            Element dependency = (Element) dependenciesList.item(i);
            if (!dependency.getAttribute("android:name").equals("")) {
                this.dependencies.add(new Dependency(this.apk, new APK(this.apk.getAppID(), dependency.getAttribute("android:name")), 1));
            }
        }
        // 处理config的依赖
        NodeList applicationList = this.apk.getManifest().getElementsByTagName("application");
        for (int i = 0; i < applicationList.getLength(); i++) {
            Element applicationElement = (Element) applicationList.item(i);
            boolean isSplitRequired = applicationElement.getAttribute("android:isSplitRequired").equals("true");
            String requiredSplitTypes = applicationElement.getAttribute("android:requiredSplitTypes");
            if (!requiredSplitTypes.equals("")) {
                String[] configDependencies = requiredSplitTypes.split(",");
                for (String configDependency : configDependencies) {
                    dependencies.add(new Dependency(apk, new APK(this.apk.getAppID(), configDependency), 2));
                }
            } else if (isSplitRequired) {
                dependencies.add(new Dependency(apk, new APK(this.apk.getAppID(), "unknown"), 3));
            }
        }
        //处理delivery方式
        if (this.apk.getManifest().getElementsByTagName("dist:module").item(0) != null) {
            Element moduleElement = (Element) this.apk.getManifest().getElementsByTagName("dist:module").item(0);
            String isOnDemand = moduleElement.getAttribute("dist:onDemand");
            boolean isAssetPack = moduleElement.getAttribute("dist:type").equals("asset-pack");

            // 旧写法
            if (!isOnDemand.equals("")) {
                if (isOnDemand.equals("false")) {
                    if (isFeature && !isAssetPack) {
                        this.features[0] = "1";
                    } else {
                        this.features[3] = "1";
                    }
                } else {
                    if (isFeature && !isAssetPack) {
                        this.features[6] = "1";
                    } else {
                        this.features[7] = "1";
                    }
                }
                //新写法
            } else if (this.apk.getManifest().getElementsByTagName("dist:delivery").item(0) != null) {
                Node deliveryNode = this.apk.getManifest().getElementsByTagName("dist:delivery").item(0);

                for (int i = 0; i < deliveryNode.getChildNodes().getLength(); i++) {
                    checkNode(deliveryNode.getChildNodes().item(i), isAssetPack);
                }
            }

        }
    }


    private void checkNode(Node node, boolean isAssetPack) {
        if ("dist:install-time".equals(node.getNodeName())) {
            if (isFeature && !isAssetPack) {
                this.features[0] = "1";
            } else {
                this.features[3] = "1";
            }
        } else if ("dist:conditions".equals(node.getNodeName())) {
            this.features[1] = "1";

            assert this.features[3].equals("0");

            if (features[0].equals("1")) {
                this.features[0] = "0";
            }

            walkCondition(node);
        } else if ("dist:on-demand".equals(node.getNodeName())) {
            if (isFeature && !isAssetPack) {
                this.features[6] = "1";
            } else {
                this.features[7] = "1";
            }
        } else if ("dist:fast-follow".equals(node.getNodeName())) {
            this.features[4] = "1";
        }

        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            checkNode(node.getChildNodes().item(i), isAssetPack);
        }
    }

    private void walkCondition(Node conditionsNode) {
        NodeList conditions = conditionsNode.getChildNodes();
        for (int i = 0; i < conditions.getLength(); i++) {
            Node condition = conditions.item(i);
            int type = 0;
            String key = "";
            String value = "";

            if (condition.hasAttributes()) {
                Element element = (Element) condition;
                switch (condition.getNodeName()) {
                    case "dist:device-feature" -> {
                        type = 1;
                        key = "device-feature";
                        value = element.getAttribute("dist:name");
                    }
                    case "dist:user-countries" -> {
                        type = 2;
                        key = "user-countries";
                        value = getCountries(element);
                    }
                    case "dist:min-sdk" -> {
                        type = 3;
                        key = "min-sdk";
                        value = element.getAttribute("dist:name");
                    }
                    case "dist:max-sdk" -> {
                        type = 3;
                        key = "max-sdk";
                        value = element.getAttribute("dist:name");
                    }
                    case "dist:device-groups" -> {
                        type = 4;
                        key = "device-groups";
                        value = element.getAttribute("dist:device-groups");
                    }
                    default -> {
                        type = 5;
                        key = element.getNodeName();
                        value = element.getAttribute("dist:name");
                    }
                }
            }

            assert !key.equals("") && !value.equals("");
            this.conditions.add(new Condition(type, key, value, this.apk.getAppID(), this.apk.getSubAppID()));
        }
    }

    private String getCountries(Node node) {
        StringBuilder countriesBuilder = new StringBuilder();
        NodeList countries = node.getChildNodes();
        for (int i = 0; i < countries.getLength(); i++) {
            Node county = countries.item(i);

            if (county.hasAttributes()) {
                countriesBuilder.append(county.getAttributes().item(0).getNodeValue().replace("dist:code=", ""));
                countriesBuilder.append(",");
            }
        }

        if (countriesBuilder.length() > 0) {
            countriesBuilder.delete(countriesBuilder.length() - 1, countriesBuilder.length());
        }

        return countriesBuilder.toString();
    }

    public void checkInvoke() {
        if (this.isFeature) {
            G.reset();
            Options.v().set_src_prec(Options.src_prec_apk);
            Options.v().set_output_format(Options.output_format_jimple);
            Options.v().set_process_dir(Collections.singletonList(this.apk.getLocation().getAbsolutePath()));
            Options.v().set_android_jars(this.androidJarPath);
            Options.v().set_keep_line_number(true);
            Options.v().set_allow_phantom_refs(true);
            Options.v().set_process_multiple_dex(true);
            Options.v().set_whole_program(true);

            soot.Scene.v().loadNecessaryClasses();

            for (SootClass sootClass : Scene.v().getApplicationClasses()) {
                if (sootClass.getName().toLowerCase().contains(this.filter)) {
                    for (SootMethod method : sootClass.getMethods()) {
                        if(!method.isAbstract() && method.isConcrete()) {
                            Body body = method.retrieveActiveBody();
                            if (method.hasActiveBody()) {
                                for (Unit unit : body.getUnits()) {
                                    Stmt stmt = (Stmt) unit;
                                    if (stmt.containsInvokeExpr()) {
                                        SootMethod invokeMethod = stmt.getInvokeExpr().getMethod();
                                        if (invokeMethod.getSignature().equals(this.START_INSTALL)) {
                                            this.features[2] = "1";
                                            this.featureInvokeLines.add(stmt.toString());
                                        } else if (invokeMethod.getSignature().equals(this.DEFERRED_INSTALL)) {
                                            this.features[2] = "1";
                                            this.featureInvokeLines.add(stmt.toString());
                                        } else if (invokeMethod.getSignature().equals(this.FETCH)) {
                                            this.features[5] = "1";
                                            this.assetInvokeLines.add(stmt.toString());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    public void writeToCSV() {
        File writeFile = new File("test\\app_bundle_features.csv");

        try {
            BufferedWriter writeText = new BufferedWriter(new FileWriter(writeFile, true));

            StringBuilder data = new StringBuilder(this.apk.getAppID() + ",");

            for (int i = 0; i < this.features.length - 1; i++) {
                data.append(features[i]);

                if (i != features.length - 1) {
                    data.append(",");
                }
            }

            data.append("\n");

            writeText.write(data.toString());

            writeText.flush();
            writeText.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void writeToDataBase() {
        Database database = new Database();

        if (features[2].equals("1")) {
            for (String s : this.featureInvokeLines) {
                database.insertApk(apk.getAppID(), apk.getSubAppID() + ":" + s, new String[]{"0", "0", "1", "0", "0", "0"}, 0);
            }

            features[2] = "0";
        }

        if (features[5].equals("1")) {
//            database.insertApk(this.appId, this.subAppId, new String[]{"0", "0", "0", "0", "0", "1"}, 0);
            for (String s : this.assetInvokeLines) {
                database.insertApk(apk.getAppID(), apk.getSubAppID() + ":" + s, new String[]{"0", "0", "1", "0", "0", "0"}, 0);
            }

            features[5] = "0";
        }

        if (features[6].equals("1")) {
            features[2] = "1";
        }

        if (features[7].equals("1")) {
            features[5] = "1";
        }

        this.features = new String[]{features[0], features[1], features[2], features[3], features[4], features[5]};
        System.out.println(Arrays.toString(this.features));
        database.insertApk(this.apk.getAppID(), this.apk.getSubAppID(), features, 1);
        // 处理condition
        for (Condition condition : conditions) {
            System.out.println(condition);
            int apkId = database.queryApkIdByAppIdSubId(this.apk.getAppID(), this.apk.getSubAppID());
            database.insertApkCondition(condition.getType(), condition.getKey(), condition.getValue(), apkId);
        }

        // 处理dependency
        for (Dependency dependency : dependencies) {
            System.out.println(dependency);
            database.insertDependency(dependency.getSrcAPK().getAppID(), dependency.getSrcAPK().getSubAppID(), dependency.getDestAPK().getAppID(), dependency.getDestAPK().getSubAppID(), dependency.getType());
        }
    }

    public void parse() {
        parseManifest();
        checkInvoke();
        writeToDataBase();
    }


    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public List<Condition> getConditions() {
        return conditions;
    }
}
