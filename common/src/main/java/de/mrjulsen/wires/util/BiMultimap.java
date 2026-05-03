package de.mrjulsen.wires.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BiMultimap<K, V> {

    private final HashMap<K, HashSet<V>> forward = new HashMap<>();
    private final HashMap<V, HashSet<K>> inverse = new HashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    public void put(K key, V value) {
        writeLock.lock();
        try {
            forward.computeIfAbsent(key, k -> new HashSet<>()).add(value);
            inverse.computeIfAbsent(value, v -> new HashSet<>()).add(key);
        } finally {
            writeLock.unlock();
        }
    }

    public boolean remove(K key, V value) {
        writeLock.lock();
        try {
            HashSet<V> values = forward.get(key);
            if (values == null || !values.remove(value)) return false;
            if (values.isEmpty()) forward.remove(key);

            HashSet<K> keys = inverse.get(value);
            if (keys != null) {
                keys.remove(key);
                if (keys.isEmpty()) inverse.remove(value);
            }
            return true;
        } finally {
            writeLock.unlock();
        }
    }

    public Set<K> removeByValue(V value) {
        writeLock.lock();
        try {
            HashSet<K> keys = inverse.remove(value);
            if (keys == null) return Collections.emptySet();
            for (K key : keys) {
                HashSet<V> values = forward.get(key);
                if (values != null) {
                    values.remove(value);
                    if (values.isEmpty()) forward.remove(key);
                }
            }
            return keys;
        } finally {
            writeLock.unlock();
        }
    }

    public Set<V> removeAll(K key) {
        writeLock.lock();
        try {
            HashSet<V> values = forward.remove(key);
            if (values == null) return Collections.emptySet();
            for (V value : values) {
                HashSet<K> keys = inverse.get(value);
                if (keys != null) {
                    keys.remove(key);
                    if (keys.isEmpty()) inverse.remove(value);
                }
            }
            return values;
        } finally {
            writeLock.unlock();
        }
    }

    public Set<V> get(K key) {
        readLock.lock();
        try {
            HashSet<V> values = forward.get(key);
            return values != null ? new HashSet<>(values) : Collections.emptySet();
        } finally {
            readLock.unlock();
        }
    }

    public Set<K> getByValue(V value) {
        readLock.lock();
        try {
            HashSet<K> keys = inverse.get(value);
            return keys != null ? new HashSet<>(keys) : Collections.emptySet();
        } finally {
            readLock.unlock();
        }
    }

    public boolean contains(K key, V value) {
        readLock.lock();
        try {
            HashSet<V> values = forward.get(key);
            return values != null && values.contains(value);
        } finally {
            readLock.unlock();
        }
    }

    public boolean containsKey(K key) {
        readLock.lock();
        try {
            return forward.containsKey(key);
        } finally {
            readLock.unlock();
        }
    }

    public boolean containsValue(V value) {
        readLock.lock();
        try {
            return inverse.containsKey(value);
        } finally {
            readLock.unlock();
        }
    }

    public int size() {
        readLock.lock();
        try {
            return forward.size();
        } finally {
            readLock.unlock();
        }
    }

    public boolean isEmpty() {
        readLock.lock();
        try {
            return forward.isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    public void clear() {
        writeLock.lock();
        try {
            forward.clear();
            inverse.clear();
        } finally {
            writeLock.unlock();
        }
    }
}