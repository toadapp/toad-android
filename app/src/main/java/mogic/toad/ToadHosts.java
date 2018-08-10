package mogic.toad;

public class ToadHosts {
    private static ToadStringMap hosts = new ToadStringMap();

    public static String lookup(String host) {
        return hosts.lookup(host);
    }

    public static String get(String key) {
        return hosts.get(key);
    }

    public static void set(String key, String value) {
        hosts.set(key, value);
    }

    public static int size() {
        return hosts.size();
    }

    public static void remove(String key) {
        hosts.remove(key);
    }

    public static void removeMatched(String host) {
        hosts.removeMatched(host);
    }

    public static void clear() {
        hosts.clear();
    }
}