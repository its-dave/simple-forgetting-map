package sfm;

/**
 * ForgettingMap is a key-value map with a fixed maximum size
 * When the map is added to beyond its maximum size, the least-used entry is removed
 */
public class ForgettingMap<K,V> {

    /**
     * Creates a new ForgettingMap
     * @param maxSize the maximum number of entries which can be added before the least-used entry is removed
     */
    public ForgettingMap(int maxSize) {
        
    }

    /**
     * Adds a new key-value pair to the map, removing the least-used entry if the maximum size is exceeded
     * @param key
     * @param value
     */
    public void add(K key, V value) {

    }

    /**
     * Gets the value for the specified key, counts to the entry being used for the purposes of calculating the least-used entry
     * @param key
     * @return the value for the specified key
     */
    public V find(K key) {
        return null;
    }
}
