/*
 * Copyright (c) 2017. EPAM Systems
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

package org.apache.ignite.activestore.mocked.mocks;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;

/**
 * @author Aleksandr_Meterko
 * @since 12/27/2016
 */
public class ProxyMockConsumer<K, V> extends MockConsumer<K, V> {

    private final AtomicBoolean hasRecords = new AtomicBoolean(false);

    public ProxyMockConsumer(OffsetResetStrategy offsetResetStrategy) {
        super(offsetResetStrategy);
    }

    @Override public void subscribe(Collection<String> topics, ConsumerRebalanceListener listener) {
        super.subscribe(topics, listener);
        Map<TopicPartition, Long> offsets = new HashMap<>();
        for (String topic : topics) {
            TopicPartition partition1 = new TopicPartition(topic, 0);
            offsets.put(partition1, 0L);

            TopicPartition partition2 = new TopicPartition(topic, 1);
            offsets.put(partition2, 0L);
        }
        rebalance(offsets.keySet());
        updateBeginningOffsets(offsets);
        updateEndOffsets(offsets);
    }

    @Override public ConsumerRecords<K, V> poll(long timeout) {
        if (!hasRecords.get()) {
            return ConsumerRecords.empty();
        }
        hasRecords.set(false);
        return super.poll(timeout);
    }

    @Override public void addRecord(ConsumerRecord<K, V> record) {
        super.addRecord(record);
        hasRecords.set(true);
    }
}
