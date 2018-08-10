package mogic.toad;

public class ToadUrlInterceptor {
    private static ToadStringSet whitelist = new ToadStringSet();
    private static ToadStringSet blacklist = new ToadStringSet();

    public static class Whitelist {
        public static boolean match(String url) {
            return whitelist.match(url);
        }

        public static boolean contain(String key) {
            return whitelist.contain(key);
        }

        public static void add(String key) {
            whitelist.add(key);
        }

        public static int size() {
            return whitelist.size();
        }

        public static void remove(String key) {
            whitelist.remove(key);
        }

        public static void removeMatched(String url) {
            whitelist.removeMatched(url);
        }

        public static void clear() {
            whitelist.clear();
        }
    }

    public static class Blacklist {
        public static boolean match(String url) {
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

    public static boolean intercept(String url) {
        if (whitelist.match(url))
            return false;
        return blacklist.match(url);
    }
}