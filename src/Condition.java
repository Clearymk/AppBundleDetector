public class Condition {
    private int type;
    private String key;
    private String value;
    private String appId;
    private String subAppId;

    public Condition(int type, String key, String value, String appId, String subAppId) {
        this.type = type;
        this.key = key;
        this.value = value;
        this.appId = appId;
        this.subAppId = subAppId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getSubAppId() {
        return subAppId;
    }

    public void setSubAppId(String subAppId) {
        this.subAppId = subAppId;
    }

    @Override
    public String toString() {
        return "Condition{" +
                "type=" + type +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", appId='" + appId + '\'' +
                ", subAppId='" + subAppId + '\'' +
                '}';
    }
}
