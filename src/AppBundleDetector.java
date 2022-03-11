/**/

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

public class AppBundleDetector {
    private String apksPath;
    private Queue<File> tasks;
    private final int MAX_THREAD = 10;

    public AppBundleDetector(String apksPath) {
        this.apksPath = apksPath;
        File apkFiles = new File(this.apksPath);

        this.tasks = new LinkedList<>(Arrays.asList(Objects.requireNonNull(apkFiles.listFiles())));
    }

    // 判断所有的app
    public void run() {
        while (!tasks.isEmpty()) {
            AppBundleDetectorXAPK appBundleDetector = new AppBundleDetectorXAPK(tasks.poll().getAbsolutePath());
            try{
                appBundleDetector.run();
                appBundleDetector.database.connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {
        AppBundleDetector detector = new AppBundleDetector("/Volumes/Data/apk_pure/download_x");
        detector.run();
    }
}
