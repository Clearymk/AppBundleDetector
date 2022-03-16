

import java.sql.*;


public class Database {
    public Connection connection;

    public Database() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection("jdbc:mysql://10.19.124.236:3307/apks", "root", "catlab1a509");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertDependency(String srcAppId, String srcSubAppId, String destAppId, String destSubAppId, int type) {
        String insertSql = "insert into apk_dependency (src_apk_id, src_sub_apk_id, dest_apk_id, dest_sub_apk_id, type) values (?, ?, ?, ?, ?)";
        PreparedStatement insertStmt = null;
        try {
            insertStmt = connection.prepareStatement(insertSql);
            insertStmt.setString(1, srcAppId);
            insertStmt.setString(2, srcSubAppId);
            insertStmt.setString(3, destAppId);
            insertStmt.setString(4, destSubAppId);
            insertStmt.setInt(5, type);
            insertStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertApk(String app_id, String sub_app_id, String[] features, int exist) {
        try {
            String insertSql = "insert into apk (app_id, sub_app_id, " +
                    "status_1, status_2, status_3,status_4, status_5, status_6, exist)" +
                    "values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement insertStmt = connection.prepareStatement(insertSql);

            insertStmt.setString(1, app_id);
            insertStmt.setString(2, sub_app_id);

            for (int i = 0; i < features.length; i++) {
                insertStmt.setString(i + 3, features[i]);
            }

            insertStmt.setInt(9, exist);
            insertStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertApkCondition(int type, String key, String value, int apkId) {
        try {
            String insertSql = "insert into apk_condition (type, `key`, value, apk_id)" +
                    "values (?, ?, ?, ?)";

            PreparedStatement insertStmt = connection.prepareStatement(insertSql);

            insertStmt.setInt(1, type);
            insertStmt.setString(2, key);
            insertStmt.setString(3, value);
            insertStmt.setInt(4, apkId);
            insertStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean queryStatue5ByAppId(String appId) {
        try {
            String querySql = "select * from apk where app_id=? and status_5=0";

            PreparedStatement queryStmt = connection.prepareStatement(querySql);

            queryStmt.setString(1, appId);

            ResultSet resultSet = queryStmt.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int queryApkIdByAppIdSubId(String appId, String subAppId) {
        try {
            String querySql = "select id from apk where app_id=? and sub_app_id=?";

            PreparedStatement queryStmt = connection.prepareStatement(querySql);

            queryStmt.setString(1, appId);
            queryStmt.setString(2, subAppId);


            ResultSet resultSet = queryStmt.executeQuery();
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public boolean queryApkIdByAppId(String appId) {
        try {
            String querySql = "select id from apk where app_id=?";

            PreparedStatement queryStmt = connection.prepareStatement(querySql);

            queryStmt.setString(1, appId);

            ResultSet resultSet = queryStmt.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int queryDependencyTypeByAppID(String appId) {
        try {
            String querySql = "select type from apk_dependency where src_apk_id=?";

            PreparedStatement queryStmt = connection.prepareStatement(querySql);

            queryStmt.setString(1, appId);

            ResultSet resultSet = queryStmt.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public void updateAppStatuesByFastFollow(String appId) {
        try {
            String updateSql = "update apk_dependency set status_5 = 1where app_id=?";

            PreparedStatement updateStmt = connection.prepareStatement(updateSql);

            updateStmt.setString(1, appId);

            updateStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateDependencyUnknown(String appId, String srcApkId) {
        try {
            String updateSql = "update apk_dependency set dest_sub_apk_id = ?, type = 2 where src_apk_id=? and type=3";

            PreparedStatement updateStmt = connection.prepareStatement(updateSql);

            updateStmt.setString(1, appId);
            updateStmt.setString(2, srcApkId);

            updateStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String queryAppIdByLocation(String location) {
        try {
            String querySql = "select apkid from apkpure where filename=?";

            PreparedStatement queryStmt = connection.prepareStatement(querySql);

            queryStmt.setString(1, location);

            ResultSet resultSet = queryStmt.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString(1);
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "";
    }

    public static void main(String[] args) {
        Database database = new Database();
//        String[] features = {"0", "1", "0", "0", "0", "0"};
//        database.insertApk("a", "a", features, 0);
//        int apkId = database.queryApkIdByAppIdSubId("a", "a");
//        database.insertApkCondition(1, null, null, apkId);
    }
}
