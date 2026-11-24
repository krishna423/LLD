package Imp;
import java.time.LocalDate;
import java.util.*;

/**
 * In-memory cache framework.
 *
 * Features:
 * - Loading Type : Lazy loading, Eager loading
 * - CacheType : LRU, TTL expiration
 */
public class CacheFramework {

    //Think it as an in-memory solution only without using any external component like DB, Kafka
    //DB , Kafka are related to HLD not LLD

    // Client will decide what is cache hit/miss
    // and make a call to db and again call to cache to fill data

    // client-> cacheRead(mine)-->miss-->call DB--> update cache(mine)-->return response

     interface ICache<K, V> {
        V get(K key);
        void put(K key, V value);
        void remove(K key);
        void clear();
        int size();
        void putAll(K key, V value);
        //Map imMemoryCache to store value (key,value) - im-memory
    }

    // Factory Pattern - LRUCache, TLSBased
    // Composition - LRUEviction, FIFO Eviction

    interface ICacheLoadingStrategy<K, V> {
         void load(ICache<K, V> cache);
         // void load(ICache<K, V> cache, DataLoader loader);
    }


    class EagerLoadingStrategy<K, V> implements ICacheLoadingStrategy<K, V> {

         //Database Config loader
        @Override
        public void load(ICache<K, V> cache) {
            //Map<K, V>  data = loader.loadAll();
            //imMemoryCacheHashMap.put()
        }
    }


     class LRUCacheImpl<K, V> implements ICache<K, V> {
        @Override
        public V get(K key) {
            return null;
        }

        @Override
        public void put(K key, V value) {

        }

        @Override
        public void remove(K key) {

        }

        @Override
        public void clear() {

        }

        @Override
        public int size() {
            return 0;
        }

         @Override
         public void putAll(K key, V value) {

         }
     }


//composition
     interface CacheEvictionPolicy<K, V> {
        void onInsert(K key, V value);
        void onAccess(K key);
        void onRemove(K key);
        boolean shouldEvict();
        K getEvictionCandidate();
    }

     class LRUCacheEvictionPolicyImpl<K, V> implements CacheEvictionPolicy<K, V> {

        ICache<K,V> cache;

        @Override
        public void onInsert(K key, V value) {
            cache.put(key, value);
        }

        @Override
        public void onAccess(K key) {
            // cache.get(key);
        }

        @Override
        public void onRemove(K key) {

        }

        @Override
        public boolean shouldEvict() {
            return false;
        }

        @Override
        public K getEvictionCandidate() {
            return null;
        }
    }


    class TTLCacheEvictionPolicyImpl<K, V> implements CacheEvictionPolicy<K, V> {

        ICache<K,Pair<V, LocalDate>> cache;

        @Override
        public void onInsert(K key, V value) {
            cache.put(key, new Pair<>());
        }

        @Override
        public void onAccess(K key) {
            // cache.get(key);
            //match time
        }

        @Override
        public void onRemove(K key) {

        }

        @Override
        public boolean shouldEvict() {
            return false;
        }

        @Override
        public K getEvictionCandidate() {
            return null;
        }
    }

    class Pair<V, T> {
        V value ;
        T time;

    }






}
