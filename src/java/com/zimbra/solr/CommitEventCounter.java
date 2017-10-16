package com.zimbra.solr;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CommitEventCounter {
    private final AtomicInteger num = new AtomicInteger();
    private static final Map<String,CommitEventCounter> instanceMap = new HashMap<String,CommitEventCounter>();

    private CommitEventCounter() {}

    public static CommitEventCounter getInstance(String coreName) {
        synchronized (instanceMap) {
            CommitEventCounter instance = instanceMap.get(coreName);
            if (instance == null) {
                instance = new CommitEventCounter();
                instanceMap.put(coreName, instance);
            }
            return instance;
        }
    }

    public int increment() {
        return num.incrementAndGet();
    }

    public int decrement() {
        return num.decrementAndGet();
    }

    public int get() {
        return num.get();
    }

    public int getAndReset() {
        return num.getAndSet(0);
    }
}
