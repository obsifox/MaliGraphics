package dev.mali.graphics.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Generic reference-tracked LRU resource cache (mission §15, §16).
 * Pure Java — unit tested. hit/miss stats feed GraphicsProfiler.Snapshot.cacheHitRate.
 */
public final class ResourceCache<K, R> {
    public interface Factory<K, R> { R create(K key); }
    public interface Releaser<R> { void release(R resource); }

    private final Map<K, Node> map = new HashMap<>();
    private final Deque<Node> lru = new ArrayDeque<>();     // head = most recent
    private final Factory<K, R> factory;
    private final Releaser<R> releaser;
    private final long maxBytes;
    private long usedBytes;
    private long hits, misses, evictions;

    private final class Node {
        K key; R value; int bytes;
    }

    public ResourceCache(Factory<K, R> factory, Releaser<R> releaser, long maxBytes) {
        this.factory = factory;
        this.releaser = releaser;
        this.maxBytes = maxBytes;
    }

    public synchronized R get(K key) {
        Node n = map.get(key);
        if (n != null) {
            hits++;
            touch(n);
            return n.value;
        }
        misses++;
        R created = factory.create(key);
        int bytes = estimateBytes(created);
        while (usedBytes + (long) bytes > maxBytes && !lru.isEmpty()) {
            evictOldest();
        }
        Node fresh = new Node();
        fresh.key = key; fresh.value = created; fresh.bytes = bytes;
        map.put(key, fresh);
        lru.addFirst(fresh);
        usedBytes += bytes;
        return created;
    }

    private void touch(Node n) {
        lru.remove(n);
        lru.addFirst(n);
    }

    private void evictOldest() {
        Iterator<Node> it = lru.descendingIterator();
        if (!it.hasNext()) return;
        Node old = it.next();
        it.remove();
        map.remove(old.key);
        usedBytes -= old.bytes;
        evictions++;
        releaser.release(old.value);
    }

    /** Inserts an already-created resource with a known byte size (bypasses factory). */
    public synchronized void putDirect(K key, R resource, int bytes) {
        Node existing = map.get(key);
        if (existing != null) {
            touch(existing);
            return;
        }
        while (usedBytes + (long) bytes > maxBytes && !lru.isEmpty()) {
            evictOldest();
        }
        Node n = new Node();
        n.key = key; n.value = resource; n.bytes = bytes;
        map.put(key, n);
        lru.addFirst(n);
        usedBytes += bytes;
    }

    public synchronized void remove(K key) {
        Node n = map.remove(key);
        if (n == null) return;
        lru.remove(n);
        usedBytes -= n.bytes;
        releaser.release(n.value);
    }

    public synchronized void clear() {
        for (Node n : map.values()) releaser.release(n.value);
        map.clear();
        lru.clear();
        usedBytes = 0;
    }

    /** Override for typed size estimation; default 1 byte per entry. */
    protected int estimateBytes(R resource) { return 1; }

    public synchronized double hitRate() {
        long total = hits + misses;
        return total == 0 ? 1.0 : (double) hits / total;
    }
    public synchronized long hits() { return hits; }
    public synchronized long misses() { return misses; }
    public synchronized long evictions() { return evictions; }
    public synchronized long usedBytes() { return usedBytes; }
    public synchronized int liveCount() { return map.size(); }
}
