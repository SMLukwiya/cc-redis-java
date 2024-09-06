package store;

import RdbParser.KeyValuePair;

import java.util.ArrayList;

public class Cache {
    volatile ArrayList<KeyValuePair> cache = new ArrayList<>();
    volatile int offset = 0;
    volatile int currOffset = 0;

    public Cache() {}

    public Cache(ArrayList<KeyValuePair> cache) {
        this.cache = cache;
    }

    synchronized public void setCache(KeyValuePair item) {
        cache.add(item);
    }

    public ArrayList<KeyValuePair> getCache() { return cache; }

    synchronized public void setOffset(int numOfBytes) {
        offset += numOfBytes;
    }

    synchronized public void setCurrOffset() {
        currOffset = offset;
    }

    public int getOffset() { return currOffset; }
}
