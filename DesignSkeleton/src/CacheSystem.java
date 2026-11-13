//import java.time.Duration;
//import java.util.*;
//
///**
// * A small, self-contained, configurable in-memory cache framework.
// *
// * Features:
// * - Lazy loading: load a key from DataLoader on cache miss.
// * - Eager loading: preload all entries during build().
// * - LRU eviction: remove least-recently-used entries when capacity is reached.
// * - TTL expiration: expire entries after a configured duration.
// * - LRU and TTL can be combined.
// *
// * Java 17+
// *
// * This is an interview-friendly implementation, not a production cache:
// * operations are synchronized for correctness and the loader is called while
// * holding the cache lock. A production implementation should use per-key
// * loading coordination, metrics, bounded refresh, and more scalable concurrency.
// */
//public class CacheSystem {
//
//    public interface LoadingStrategy<K, V> {
//        void load(Cache<K, V> cache, DataLoader<K, V> loader);
//    }
//
//
//    public class EagerLoadingStrategy<K, V>
//            implements LoadingStrategy<K, V> {
//
//        @Override
//        public void load(Cache<K, V> cache,
//                         DataLoader<K, V> loader) {
//
//            Map<K, V> data = loader.loadAll();
//            data.forEach(cache::put);
//        }
//    }
//    public class LazyLoadingStrategy<K, V>
//            implements LoadingStrategy<K, V> {
//
//        @Override
//        public void load(Cache<K, V> cache,
//                         DataLoader<K, V> loader) {
//            // Data is loaded only when get(key) is called.
//            // Actual loading happens inside Cache.get().
//        }
//    }
//
//    public interface CachePolicy<K, V> {
//
//        void onInsert(K key, V value);
//
//        void onAccess(K key);
//
//        void onRemove(K key);
//
//        boolean shouldEvict();
//
//        K getEvictionCandidate();
//    }
//
//
//    public interface Cache<K, V> {
//
//        V get(K key);
//
//        void put(K key, V value);
//
//        void invalidate(K key);
//
//        void clear();
//
//        int size();
//    }
//
//}
