package mogic.toad;

public class ToadHosts {
    private static ToadStringMap internalHosts = new ToadStringMap();
    private static ToadStringMap externalHosts = new ToadStringMap();

    public static String lookup(String host) {
        String result = internalHosts.lookup(host);
        if (result != null)
            return result;
        return externalHosts.lookup(host);
    }

    public static String get(String key) {
        return externalHosts.get(key);
    }

    public static void set(String key, String value) {
        externalHosts.set(key, value);
    }

    public static int size() {
        return externalHosts.size();
    }

    public static void remove(String key) {
        externalHosts.remove(key);
    }

    public static void removeMatched(String host) {
        externalHosts.removeMatched(host);
    }

    public static void clear() {
        externalHosts.clear();
    }

    public static String getInternal(String key) {
        return internalHosts.get(key);
    }

    public static void setInternal(String key, String value) {
        internalHosts.set(key, value);
    }

    public static int internalSize() {
        return internalHosts.size();
    }

    public static void removeInternal(String key) {
        internalHosts.remove(key);
    }

    public static void removeMatchedInternal(String host) {
        internalHosts.removeMatched(host);
    }

    public static void clearInternal() {
        internalHosts.clear();
    }
}