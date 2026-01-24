package org.example;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class LruCache <K, V> extends LinkedHashMap<K, V> {

    private final int size;

    public LruCache(int size) {
        super(16, 0.75f, /* ordering mode: access-order */ true);
        this.size = size;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return size() > size;
    }

    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

}
