/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.reactivex.internal.schedulers;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages the creating of ScheduledExecutorServices and sets up purging.
 */
public final class SchedulerPoolFactory {
    /** Utility class. */
    private SchedulerPoolFactory() {
        throw new IllegalStateException("No instances!");
    }

    static final String PURGE_ENABLED_KEY = "rx2.purge-enabled";

    /**
     * Indicates the periodic purging of the ScheduledExecutorService is enabled.
     */
    public static final boolean PURGE_ENABLED;

    static final String PURGE_PERIOD_SECONDS_KEY = "rx2.purge-period-seconds";

    /**
     * Indicates the purge period of the ScheduledExecutorServices created by create().
     */
    public static final int PURGE_PERIOD_SECONDS;

    static final AtomicReference<ScheduledExecutorService> PURGE_THREAD =
            new AtomicReference<ScheduledExecutorService>();

    // Upcast to the Map interface here to avoid 8.x compatibility issues.
    // See http://stackoverflow.com/a/32955708/61158
    static final Map<ScheduledThreadPoolExecutor, Object> POOLS =
            new ConcurrentHashMap<ScheduledThreadPoolExecutor, Object>();

    /**
     * Starts the purge thread if not already started.
     */
    public static void start() {
        tryStart(PURGE_ENABLED);
    }

    static void tryStart(boolean purgeEnabled) {
        if (purgeEnabled) {
            for (;;) {
                ScheduledExecutorService curr = PURGE_THREAD.get();
                if (curr != null) {
                    return;
                }
                ScheduledExecutorService next = Executors.newScheduledThreadPool(1, new RxThreadFactory("RxSchedulerPurge"));
                if (PURGE_THREAD.compareAndSet(curr, next)) {

                    next.scheduleAtFixedRate(new ScheduledTask(), PURGE_PERIOD_SECONDS, PURGE_PERIOD_SECONDS, TimeUnit.SECONDS);

                    return;
                } else {
                    next.shutdownNow();
                }
            }
        }
    }

    /**
     * Stops the purge thread.
     */
    public static void shutdown() {
        ScheduledExecutorService exec = PURGE_THREAD.getAndSet(null);
        if (exec != null) {
            exec.shutdownNow();
        }
        POOLS.clear();
    }

    static {
        Properties properties = System.getProperties();

        PurgeProperties pp = new PurgeProperties();
        pp.load(properties);

        PURGE_ENABLED = pp.purgeEnable;
        PURGE_PERIOD_SECONDS = pp.purgePeriod;

        start();
    }

    static final class PurgeProperties {

        boolean purgeEnable;

        int purgePeriod;

        void load(Properties properties) {
            if (properties.containsKey(PURGE_ENABLED_KEY)) {
                purgeEnable = Boolean.parseBoolean(properties.getProperty(PURGE_ENABLED_KEY));
            } else {
                purgeEnable = true;
            }

            if (purgeEnable && properties.containsKey(PURGE_PERIOD_SECONDS_KEY)) {
                try {
                    purgePeriod = Integer.parseInt(properties.getProperty(PURGE_PERIOD_SECONDS_KEY));
                } catch (NumberFormatException ex) {
                    purgePeriod = 1;
                }
            } else {
                purgePeriod = 1;
            }
        }
    }

    /**
     * Creates a ScheduledExecutorService with the given factory.
     * @param factory the thread factory
     * @return the ScheduledExecutorService
     */
    public static ScheduledExecutorService create(ThreadFactory factory) {
        final ScheduledExecutorService exec = Executors.newScheduledThreadPool(1, factory);
        tryPutIntoPool(PURGE_ENABLED, exec);
        return exec;
    }

    static void tryPutIntoPool(boolean purgeEnabled, ScheduledExecutorService exec) {
        if (purgeEnabled && exec instanceof ScheduledThreadPoolExecutor) {
            ScheduledThreadPoolExecutor e = (ScheduledThreadPoolExecutor) exec;
            POOLS.put(e, exec);
        }
    }

    static final class ScheduledTask implements Runnable {
        @Override
        public void run() {
            for (ScheduledThreadPoolExecutor e : new ArrayList<ScheduledThreadPoolExecutor>(POOLS.keySet())) {
                if (e.isShutdown()) {
                    POOLS.remove(e);
                } else {
                    e.purge();
                }
            }
        }
    }
}
