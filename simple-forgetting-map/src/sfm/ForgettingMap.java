package sfm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ForgettingMap is a key-value map with a fixed maximum size
 * When the map is added to beyond its maximum size, the least-used entry is removed
 */
public class ForgettingMap<K,V> {
    final int maxSize;
    final Map<K,Value<V>> map;

    /**
     * Creates a new ForgettingMap
     * @param maxSize the maximum number of entries which can be added before the least-used entry is removed
     */
    public ForgettingMap(int maxSize) {
        this.maxSize = maxSize;
        this.map = new ConcurrentHashMap<>(maxSize);
    }

    /**
     * Adds a new key-value pair to the map, removing the least-used entry if the maximum size is exceeded
     * @param key
     * @param value
     */
    public synchronized void add(K key, V value) {
        K leastUsed = getLeastUsedKey();
        map.put(key, new Value<>(value));
        if (map.size() > maxSize) {
            map.remove(leastUsed);
        }
    }

    /**
     * Gets the value for the specified key, counts to the entry being used for the purposes of calculating the least-used entry
     * @param key
     * @return the value for the specified key
     */
    public synchronized V find(K key) {
        Value<V> value = map.get(key);
        if (value != null) {
            value.incrementUseCount();
            return value.getValue();
        }
        return null;
    }

    /**
     * @return the key which has accessed the least number of times
     */
    K getLeastUsedKey() {
        K leastUsedKey = null;
        int lowestUsageCount = Integer.MAX_VALUE;
        for (K key : map.keySet()) {
            Integer usageCount = map.get(key).getUseCount();
            if (usageCount < lowestUsageCount) {
                leastUsedKey = key;
                lowestUsageCount = usageCount;
            }
        }
        return leastUsedKey;
    }
}

class Value<V> {
    private final V value;
    private int useCount;

    Value(V value) {
        this.value = value;
        this.useCount = 0;
    }

    V getValue() {
        return value;
    }

    void incrementUseCount() {
        useCount++;
    }

    int getUseCount() {
        return useCount;
    }
}
