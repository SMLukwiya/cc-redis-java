package store;

import RdbParser.KeyValuePair;

import java.util.ArrayList;

public class RedisCache {
    private volatile static ArrayList<KeyValuePair> cache = new ArrayList<>();
    private volatile static int offset = 0;
    private volatile static int currOffset = 0;

    public RedisCache() {}

    public RedisCache(ArrayList<KeyValuePair> cache) {
        this.cache = cache;
    }

    synchronized public static void setCache(KeyValuePair item) {
        cache.add(item);
    }

    public static ArrayList<KeyValuePair> getCache() { return cache; }

    synchronized public static void setOffset(int numOfBytes) {
        offset += numOfBytes;
    }

    synchronized public static void setCurrOffset() {
        currOffset = offset;
    }

    public static int getOffset() { return currOffset; }
}
