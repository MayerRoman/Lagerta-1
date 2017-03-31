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

package com.epam.lathgertha.subscriber;

import com.epam.lathgertha.capturer.TransactionScope;
import com.google.common.collect.Lists;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class DataProviderUtil {

    @SafeVarargs
    public static TransactionScope txScope(long txId, Map.Entry<String, List>... cacheScopes) {
        return new TransactionScope(txId, Lists.newArrayList(cacheScopes));
    }

    public static Map.Entry<String, List> cacheScope(String cacheName, Object... keys) {
        return new AbstractMap.SimpleImmutableEntry<>(cacheName, Lists.newArrayList(keys));
    }

    @SafeVarargs
    public static <T> List<T> list(T... ts) {
        return Lists.newArrayList(ts);
    }

    public static class NodeTransactionsBuilder {

        private Map<UUID, List<Long>> map;

        private NodeTransactionsBuilder(Map<UUID, List<Long>> map) {
            this.map = map;
        }

        public static NodeTransactionsBuilder builder() {
            return new NodeTransactionsBuilder(new HashMap<>());
        }

        public Map<UUID, List<Long>> build() {
            return map;
        }

        public NodeTransactionsBuilder nodeTransactions(UUID uuid, long... txIds) {
            List<Long> txs = Arrays.stream(txIds).boxed().collect(Collectors.toList());
            map.put(uuid, txs);
            return this;
        }
    }
}



