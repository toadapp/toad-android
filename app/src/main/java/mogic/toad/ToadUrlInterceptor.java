package mogic.toad;

public class ToadUrlInterceptor {
    private static ToadStringSet blacklist = new ToadStringSet();

    public static boolean intercept(String url) {
        return blacklist.match(url);
    }

    public static boolean contain(String key) {
        return blacklist.contain(key);
    }

    public static void add(String key) {
        blacklist.add(key);
    }

    public static int size() {
        return blacklist.size();
    }

    public static void remove(String key) {
        blacklist.remove(key);
    }

    public static void removeMatched(String url) {
        blacklist.removeMatched(url);
    }

    public static void clear() {
        blacklist.clear();
    }
}