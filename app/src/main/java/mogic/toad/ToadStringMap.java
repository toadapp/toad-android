package mogic.toad;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;

public class ToadStringMap {
    private ConcurrentHashMap<String, String> mHashMap;

    public ToadStringMap() {
        mHashMap = new ConcurrentHashMap<String, String>();
    }

    public String get(String key) {
        String value = mHashMap.get(key);
        if (value == null)
            return mHashMap.containsKey(key) ? "" : null;
        return value;
    }

    public void set(String key, String value) {
        if (key == null)
            return;
        mHashMap.put(key, value);
    }

    public void remove(String key) {
        mHashMap.remove(key);
    }

    public void clear() {
        mHashMap.clear();
    }

    public int size() {
        return mHashMap.size();
    }

    public String lookup(String fuzzyString) {
        if (fuzzyString == null)
            return null;
        String key;
        for (Iterator<String> it = mHashMap.keySet().iterator(); it.hasNext();) {
            key = it.next();
            if (fuzzyString.matches(key))
                return get(key);
        }
        return null;
    }

    public void removeMatched(String fuzzyString) {
        if (fuzzyString == null)
            return;
        String key;
        for (Iterator<String> it = mHashMap.keySet().iterator(); it.hasNext();) {
            key = it.next();
            if (fuzzyString.matches(key))
                mHashMap.remove(key);
        }
    }
}
