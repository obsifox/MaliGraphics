package dev.mali.graphics.core.texture;

import dev.mali.graphics.core.ResourceCache;

/**
 * Texture catalog (mission §16): LRU cache keyed by stable string ids
 * ("icon:<modid>", "skin:<uuid>", "pack:<name>/<path>", "ui:<asset>").
 * Actual decode + GPU upload happen in the Android/GLES layers; this class
 * owns identity, lifetime, and hit-rate metrics only. Pure Java — unit tested.
 */
public final class TextureManager<K> {

    private final ResourceCache<K, Object> cache;
    private final SizeEstimator<K> estimator;
    private long uploadCount;

    public interface SizeEstimator<K> { int bytesFor(K key); }

    public interface Factory<K> { Object createPending(K key); }

    public TextureManager(final Factory<K> userFactory, final SizeEstimator<K> estimator, long budgetBytes) {
        this.estimator = estimator;
        this.cache = new ResourceCache<>(
                new ResourceCache.Factory<K, Object>() {
                    @Override public Object create(K key) {
                        return userFactory.createPending(key);
                    }
                },
                new ResourceCache.Releaser<Object>() {
                    @Override public void release(Object resource) {
                        if (resource instanceof AutoCloseable) {
                            try { ((AutoCloseable) resource).close(); } catch (Exception ignored) {}
                        }
                    }
                },
                budgetBytes);
    }

    public Object get(K key) {
        Object v = cache.get(key);
        if (v instanceof PendingTexture) uploadCount++;
        return v;
    }

    public void putLoaded(K key, Object gpuHandle, int bytes) {
        cache.remove(key);              // drop pending placeholder
        int size = bytes > 0 ? bytes : (estimator != null ? estimator.bytesFor(key) : 1);
        cache.putDirect(key, gpuHandle, size);
    }

    public void evict(K key) { cache.remove(key); }
    public void clear() { cache.clear(); }

    public double hitRate() { return cache.hitRate(); }
    public int liveCount() { return cache.liveCount(); }
    public long uploadCount() { return uploadCount; }
    public long usedBytes() { return cache.usedBytes(); }

    /** Placeholder marker so renderer can skip drawing not-yet-loaded textures. */
    public static final class PendingTexture {
        public static final PendingTexture INSTANCE = new PendingTexture();
        private PendingTexture() {}
    }
}
