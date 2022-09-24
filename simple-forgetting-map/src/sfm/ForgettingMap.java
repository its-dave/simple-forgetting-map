package sfm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ForgettingMap is a key-value map with a fixed maximum size
 * When the map is added to beyond its maximum size, the least-used entry is removed
 */
public class ForgettingMap<K,V> {
    final int max;
    final Map<K,V> map;
    final Map<K,Integer> usageCountMap;

    /**
     * Creates a new ForgettingMap
     * @param maxSize the maximum number of entries which can be added before the least-used entry is removed
     */
    public ForgettingMap(int maxSize) {
        max = maxSize;
        map = new ConcurrentHashMap<>(maxSize);
        usageCountMap = new ConcurrentHashMap<>(maxSize);
    }

    /**
     * Adds a new key-value pair to the map, removing the least-used entry if the maximum size is exceeded
     * @param key
     * @param value
     */
    public void add(K key, V value) {
        K leastUsed = getLeastUsedKey();
        map.put(key, value);
        usageCountMap.put(key, 0);
        if (map.size() > max) {
            map.remove(leastUsed);
            usageCountMap.remove(leastUsed);
        }
    }

    /**
     * Gets the value for the specified key, counts to the entry being used for the purposes of calculating the least-used entry
     * @param key
     * @return the value for the specified key
     */
    public V find(K key) {
        V value = map.get(key);
        if (value != null) {
            usageCountMap.put(key, usageCountMap.get(key) + 1);
        }
        return value;
    }

    /**
     * @return the key which has accessed the least number of times
     */
    K getLeastUsedKey() {
        K leastUsedKey = null;
        int lowestUsageCount = Integer.MAX_VALUE;
        for (K key : usageCountMap.keySet()) {
            Integer usageCount = usageCountMap.get(key);
            if (usageCount < lowestUsageCount) {
                leastUsedKey = key;
                lowestUsageCount = usageCount;
            }
        }
        return leastUsedKey;
    }
}
