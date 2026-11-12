import java.time.Duration;
import java.util.*;

/**
 * A small, self-contained, configurable in-memory cache framework.
 *
 * Features:
 * - Lazy loading: load a key from DataLoader on cache miss.
 * - Eager loading: preload all entries during build().
 * - LRU eviction: remove least-recently-used entries when capacity is reached.
 * - TTL expiration: expire entries after a configured duration.
 * - LRU and TTL can be combined.
 *
 * Java 17+
 *
 * This is an interview-friendly implementation, not a production cache:
 * operations are synchronized for correctness and the loader is called while
 * holding the cache lock. A production implementation should use per-key
 * loading coordination, metrics, bounded refresh, and more scalable concurrency.
 */
public class CacheSystemDemo {

    public static void main(String[] args) {
        // Example 1: Lazy loading + LRU + TTL
        Cache<String, String> userCache = CacheBuilder.<String, String>newBuilder()
                .capacity(3)
                .loadingMode(LoadingMode.LAZY)
                .dataLoader(new DataLoader<>() {
                    @Override
                    public String load(String key) {
                        System.out.println("Loading from source: " + key);
                        return "value-for-" + key;
                    }
                })
                .evictionPolicy(new LRUEvictionPolicy<>())
                .ttl(Duration.ofMinutes(5))
                .build();

        System.out.println(userCache.get("user-1")); // cache miss -> loads
        System.out.println(userCache.get("user-1")); // cache hit
        userCache.put("user-2", "Alice");
        userCache.put("user-3", "Bob");
        userCache.put("user-4", "Charlie"); // may evict LRU entry

        System.out.println("Cache size: " + userCache.size());

        // Example 2: Eager loading
        Cache<String, Integer> configCache = CacheBuilder.<String, Integer>newBuilder()
                .capacity(100)
                .loadingMode(LoadingMode.EAGER)
                .dataLoader(new DataLoader<>() {
                    @Override
                    public Integer load(String key) {
                        return null; // not used for eager bulk loading
                    }

                    @Override
                    public Map<String, Integer> loadAll() {
                        Map<String, Integer> configs = new HashMap<>();
                        configs.put("timeout", 30);
                        configs.put("max-retries", 3);
                        return configs;
                    }
                })
                .build();

        System.out.println("Eagerly loaded timeout: " + configCache.get("timeout"));
    }

    // ---------------------------------------------------------------------
    // Public cache contract
    // ---------------------------------------------------------------------

    public interface Cache<K, V> {
        V get(K key);
        void put(K key, V value);
        void invalidate(K key);
        void clear();
        int size();
    }

    // ---------------------------------------------------------------------
    // Data loading abstraction
    // ---------------------------------------------------------------------

    public interface DataLoader<K, V> {
        V load(K key);

        default Map<K, V> loadAll() {
            throw new UnsupportedOperationException(
                    "Eager loading requires DataLoader.loadAll()");
        }
    }

    public enum LoadingMode {
        LAZY,
        EAGER
    }

    // ---------------------------------------------------------------------
    // Eviction policy abstraction and LRU implementation
    // ---------------------------------------------------------------------

    public interface EvictionPolicy<K> {
        void onInsert(K key);
        void onAccess(K key);
        void onRemove(K key);
        K evictionCandidate();
    }

    /**
     * LinkedHashMap with accessOrder=true maintains least-recently-used
     * entries first. This implementation is intended to be called under
     * the CacheImpl lock.
     */
    public static class LRUEvictionPolicy<K> implements EvictionPolicy<K> {
        private final LinkedHashMap<K, Boolean> accessOrder =
                new LinkedHashMap<>(16, 0.75f, true);

        @Override
        public void onInsert(K key) {
            accessOrder.put(key, Boolean.TRUE);
        }

        @Override
        public void onAccess(K key) {
            if (accessOrder.containsKey(key)) {
                accessOrder.get(key); // updates access order
            }
        }

        @Override
        public void onRemove(K key) {
            accessOrder.remove(key);
        }

        @Override
        public K evictionCandidate() {
            Iterator<K> iterator = accessOrder.keySet().iterator();
            return iterator.hasNext() ? iterator.next() : null;
        }

        public void clear() {
            accessOrder.clear();
        }
    }

    // ---------------------------------------------------------------------
    // Expiration policy abstraction and TTL implementation
    // ---------------------------------------------------------------------

    public interface ExpirationPolicy<K> {
        void onInsert(K key);
        boolean isExpired(K key);
        void onRemove(K key);
        void clear();
    }

    public static class TTLExpirationPolicy<K> implements ExpirationPolicy<K> {
        private final long ttlMillis;
        private final Map<K, Long> expiryTimes = new HashMap<>();

        public TTLExpirationPolicy(Duration ttl) {
            if (ttl == null || ttl.isZero() || ttl.isNegative()) {
                throw new IllegalArgumentException("TTL must be positive");
            }
            this.ttlMillis = ttl.toMillis();
        }

        @Override
        public void onInsert(K key) {
            expiryTimes.put(key, System.currentTimeMillis() + ttlMillis);
        }

        @Override
        public boolean isExpired(K key) {
            Long expiryTime = expiryTimes.get(key);
            return expiryTime == null || System.currentTimeMillis() >= expiryTime;
        }

        @Override
        public void onRemove(K key) {
            expiryTimes.remove(key);
        }

        @Override
        public void clear() {
            expiryTimes.clear();
        }
    }

    // ---------------------------------------------------------------------
    // Cache implementation
    // ---------------------------------------------------------------------

    public static class CacheImpl<K, V> implements Cache<K, V> {

        private final Map<K, V> storage = new HashMap<>();
        private final int capacity;
        private final EvictionPolicy<K> evictionPolicy;
        private final ExpirationPolicy<K> expirationPolicy;
        private final DataLoader<K, V> dataLoader;
        private final LoadingMode loadingMode;

        private CacheImpl(CacheBuilder<K, V> builder) {
            this.capacity = builder.capacity;
            this.evictionPolicy = builder.evictionPolicy;
            this.expirationPolicy = builder.expirationPolicy;
            this.dataLoader = builder.dataLoader;
            this.loadingMode = builder.loadingMode;
        }

        private void initializeEagerly() {
            if (loadingMode != LoadingMode.EAGER) {
                return;
            }
            if (dataLoader == null) {
                throw new IllegalStateException("Eager loading requires a DataLoader");
            }

            Map<K, V> initialData = dataLoader.loadAll();
            if (initialData == null) {
                return;
            }

            for (Map.Entry<K, V> entry : initialData.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public synchronized V get(K key) {
            Objects.requireNonNull(key, "Cache key cannot be null");

            V value = storage.get(key);

            if (value != null) {
                if (isExpired(key)) {
                    removeInternal(key);
                } else {
                    if (evictionPolicy != null) {
                        evictionPolicy.onAccess(key);
                    }
                    return value;
                }
            }

            // Lazy load only when the requested key is missing.
            if (loadingMode == LoadingMode.LAZY && dataLoader != null) {
                value = dataLoader.load(key);
                if (value != null) {
                    put(key, value);
                }
            }

            return value;
        }

        @Override
        public synchronized void put(K key, V value) {
            Objects.requireNonNull(key, "Cache key cannot be null");
            Objects.requireNonNull(value, "Cache value cannot be null");

            // Replace existing entry without treating it as a new capacity slot.
            if (storage.containsKey(key)) {
                storage.put(key, value);
                if (evictionPolicy != null) {
                    evictionPolicy.onAccess(key);
                }
                if (expirationPolicy != null) {
                    expirationPolicy.onInsert(key);
                }
                return;
            }

            // Make room before inserting a new entry.
            while (storage.size() >= capacity) {
                if (evictionPolicy == null) {
                    throw new IllegalStateException(
                            "Cache is full and no eviction policy is configured");
                }
                K candidate = evictionPolicy.evictionCandidate();
                if (candidate == null) {
                    throw new IllegalStateException("Eviction policy returned no candidate");
                }
                removeInternal(candidate);
            }

            storage.put(key, value);

            if (evictionPolicy != null) {
                evictionPolicy.onInsert(key);
            }
            if (expirationPolicy != null) {
                expirationPolicy.onInsert(key);
            }
        }

        @Override
        public synchronized void invalidate(K key) {
            removeInternal(key);
        }

        private boolean isExpired(K key) {
            return expirationPolicy != null && expirationPolicy.isExpired(key);
        }

        private void removeInternal(K key) {
            storage.remove(key);
            if (evictionPolicy != null) {
                evictionPolicy.onRemove(key);
            }
            if (expirationPolicy != null) {
                expirationPolicy.onRemove(key);
            }
        }

        @Override
        public synchronized void clear() {
            storage.clear();
            if (evictionPolicy != null) {
                // Remove policy metadata for every known entry.
                // A fresh policy instance is generally preferable after clear;
                // this implementation removes keys using the tracked cache keys.
                // See clearPolicyState() below.
                clearPolicyState();
            }
            if (expirationPolicy != null) {
                expirationPolicy.clear();
            }
        }

        private void clearPolicyState() {
            // LRU policy has no public clear operation, so remove each key
            // before storage is cleared in a production design. Here, rebuild
            // is not possible through the interface; see note below.
            if (evictionPolicy instanceof LRUEvictionPolicy<?> lru) {
                @SuppressWarnings("unchecked")
                LRUEvictionPolicy<K> typed = (LRUEvictionPolicy<K>) lru;
                typed.clear();
            }
        }

        @Override
        public synchronized int size() {
            // Remove expired entries before reporting size.
            if (expirationPolicy != null) {
                List<K> expiredKeys = new ArrayList<>();
                for (K key : storage.keySet()) {
                    if (expirationPolicy.isExpired(key)) {
                        expiredKeys.add(key);
                    }
                }
                for (K key : expiredKeys) {
                    removeInternal(key);
                }
            }
            return storage.size();
        }
    }

    // ---------------------------------------------------------------------
    // Builder
    // ---------------------------------------------------------------------

    public static class CacheBuilder<K, V> {
        private int capacity = 100;
        private LoadingMode loadingMode = LoadingMode.LAZY;
        private DataLoader<K, V> dataLoader;
        private EvictionPolicy<K> evictionPolicy;
        private ExpirationPolicy<K> expirationPolicy;

        private CacheBuilder() {
        }

        public static <K, V> CacheBuilder<K, V> newBuilder() {
            return new CacheBuilder<>();
        }

        public CacheBuilder<K, V> capacity(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("Capacity must be greater than zero");
            }
            this.capacity = capacity;
            return this;
        }

        public CacheBuilder<K, V> loadingMode(LoadingMode loadingMode) {
            this.loadingMode = Objects.requireNonNull(loadingMode);
            return this;
        }

        public CacheBuilder<K, V> dataLoader(DataLoader<K, V> dataLoader) {
            this.dataLoader = dataLoader;
            return this;
        }

        public CacheBuilder<K, V> evictionPolicy(EvictionPolicy<K> evictionPolicy) {
            this.evictionPolicy = evictionPolicy;
            return this;
        }

        public CacheBuilder<K, V> ttl(Duration ttl) {
            this.expirationPolicy = new TTLExpirationPolicy<>(ttl);
            return this;
        }

        public CacheBuilder<K, V> expirationPolicy(ExpirationPolicy<K> policy) {
            this.expirationPolicy = policy;
            return this;
        }

        public Cache<K, V> build() {
            if (loadingMode == LoadingMode.LAZY && dataLoader == null) {
                // A cache without a loader is still useful for explicit put/get.
            }
            CacheImpl<K, V> cache = new CacheImpl<>(this);
            cache.initializeEagerly();
            return cache;
        }
    }

    // Add clear() to LRU as a convenience for CacheImpl.clear().
    // Kept as a method on the concrete policy to avoid expanding the interview
    // interface solely for this implementation detail.
    static {
        // No static initialization required.
    }
}
