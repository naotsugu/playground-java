package org.example;

import java.util.LinkedHashMap;
import java.util.Map;

public class FifoCache<K, V> extends LinkedHashMap<K, V> {

    private final int size;

    public FifoCache(int size) {
        super(16, 0.75f, /* ordering mode: insertion-order */ false);
        this.size = size;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return size() > size;
    }

}
