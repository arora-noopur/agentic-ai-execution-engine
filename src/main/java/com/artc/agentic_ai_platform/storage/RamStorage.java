package com.artc.agentic_ai_platform.storage;


import com.artc.agentic_ai_platform.config.AppConfig;
import com.artc.agentic_ai_platform.core.IStorageBackend;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class RamStorage implements IStorageBackend {

    // Wrapped in synchronizedMap for thread safety
    private final Map<String, Object> store;

    public RamStorage(AppConfig appConfig) {
        this(appConfig.getStorage().getRam().getMaxEntries());
    }

    public RamStorage(int maxEntries) {
        // LinkedHashMap with accessOrder=true creates an LRU cache
        this.store = Collections.synchronizedMap(
                new LinkedHashMap<String, Object>(maxEntries, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
                        // Automatically remove oldest entry when size exceeds limit
                        return size() > maxEntries;
                    }
                }
        );
    }

    @Override
    public void save(String k, Object v) {
        store.put(k, v);
    }

    public void save(String k, Object v, long ttl) {
        store.put(k, v);
    }

    public <T> Optional<T> get(String k, Class<T> c) {
        return Optional.ofNullable(c.cast(store.get(k)));
    }

    public void delete(String k) {
        store.remove(k);
    }
}
