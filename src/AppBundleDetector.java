/**/

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

public class AppBundleDetector {
    private String apksPath;
    private Queue<File> tasks;
    private Database database;

    public AppBundleDetector(String apksPath) {
        this.apksPath = apksPath;
        File apkFiles = new File(this.apksPath);
        this.database = new Database();
        this.tasks = new LinkedList<>(Arrays.asList(Objects.requireNonNull(apkFiles.listFiles())));
    }

    // 判断所有的app
    public void run() {
        while (!tasks.isEmpty()) {
            File dir = new File(tasks.peek().getParent() + File.separator + tasks.peek().getName().replaceAll(".xapk", "").replaceAll(" ", "_"));
            if (dir.exists()) {
                tasks.poll();
                continue;
            }
            try {
                AppBundleDetectorXAPK detector = new AppBundleDetectorXAPK(tasks.poll().getAbsolutePath(), this.database);
                detector.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {
        AppBundleDetector detector = new AppBundleDetector("/Volumes/Data/apk_pure/redownload");
        detector.run();
    }
}
