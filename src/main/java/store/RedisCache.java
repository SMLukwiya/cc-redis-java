package store;

import Parser.RedisTypes.RESPArray;
import RdbParser.KeyValuePair;

import java.util.ArrayList;
import java.util.List;

public class RedisCache {
    private volatile static ArrayList<KeyValuePair> cache = new ArrayList<>();

    private ArrayList<RESPArray> queuedCommands = new ArrayList<>();
    private volatile static int offset = 0;
    private volatile static int currOffset = 0;
    private boolean queueMultiCommands = false;

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

    public void setQueueMultiCommands(boolean value) {
        this.queueMultiCommands = value;
    }

    public boolean getQueueMultiCommands() {
        return this.queueMultiCommands;
    }

    public void setQueuedCommands(RESPArray command) {
        this.queuedCommands.add(command);
    }

    public List<RESPArray> getQueuedCommands() {
        return this.queuedCommands;
    }

    public void clearQueuedCommands() { this.queuedCommands.clear(); }
}
