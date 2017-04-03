/*
 *  Copyright 2017 EPAM Systems.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.epam.lathgertha.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CallableKeyTask<V, K, T> {
    private final Scheduler scheduler;
    private final TripleFunction<V, K, T, V> appender;

    private final Map<K, V> value = new ConcurrentHashMap<>();

    public CallableKeyTask(Scheduler scheduler, TripleFunction<V, K, T, V> appender) {
        this.scheduler = scheduler;
        this.appender = appender;
    }

    public void append(K key, T value) {
        this.value.put(key, appender.apply(this.value.remove(key), key, value));
    }

    public V call(K key, Runnable runnable) {
        scheduler.pushTask(runnable);
        return value.remove(key);
    }
}
