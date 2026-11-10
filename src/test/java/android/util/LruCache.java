package android.util;

import java.util.LinkedHashMap;
import java.util.Map;

// Test replacement for android.util.LruCache
public class LruCache<K, V> {
    private final int maxSize;
    private final LinkedHashMap<K, V> map;

    public LruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;
        this.map = new LinkedHashMap<K, V>(0, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > LruCache.this.maxSize;
            }
        };
    }

    public synchronized V get(K key) {
        if (key == null) return null;
        return map.get(key);
    }

    public synchronized V put(K key, V value) {
        if (key == null || value == null) return null;
        return map.put(key, value);
    }

    public synchronized V remove(K key) {
        if (key == null) return null;
        return map.remove(key);
    }

    public synchronized void evictAll() {
        map.clear();
    }

    public synchronized int size() {
        return map.size();
    }

    public synchronized int maxSize() {
        return maxSize;
    }
}

