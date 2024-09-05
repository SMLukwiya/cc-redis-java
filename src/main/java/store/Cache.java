package store;

import RdbParser.KeyValuePair;

import java.util.ArrayList;

public class Cache {
    volatile ArrayList<KeyValuePair> cache = new ArrayList<>();

    public Cache() {}

    public Cache(ArrayList<KeyValuePair> cache) {
        this.cache = cache;
    }

    synchronized public void setCache(KeyValuePair item) {
        cache.add(item);
    }

    public ArrayList<KeyValuePair> getCache() { return cache; }
}
