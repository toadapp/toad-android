package mogic.toad;

public class ToadStringSet {
    private ToadStringMap mMap;

    public ToadStringSet() {
        mMap = new ToadStringMap();
    }

    public boolean contain(String key) {
        if (mMap.get(key) != null)
            return true;
        return false;
    }

    public void add(String key) {
        mMap.set(key, "");
    }

    public void remove(String key) {
        mMap.remove(key);
    }

    public void clear() {
        mMap.clear();
    }

    public int size() {
        return mMap.size();
    }

    public boolean match(String fuzzyString) {
        String result = mMap.lookup(fuzzyString);
        if (result != null)
            return true;
        return false;
    }

    public void removeMatched(String fuzzyString) {
        mMap.removeMatched(fuzzyString);
    }
}
