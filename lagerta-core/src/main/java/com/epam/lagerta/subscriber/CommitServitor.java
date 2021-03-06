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

package com.epam.lagerta.subscriber;

import com.epam.lagerta.kafka.KafkaLogCommitter;
import com.epam.lagerta.services.LeadService;
import com.epam.lagerta.util.Serializer;
import org.apache.ignite.Ignite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Utilize logic of single transaction commit and write to local kafka log
 */
public class CommitServitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommitServitor.class);

    private final Committer committer;
    private final Serializer serializer;
    private final KafkaLogCommitter kafkaLogCommitter;
    private final LeadService lead;
    private final UUID readerId;

    public CommitServitor(Serializer serializer, Committer committer, KafkaLogCommitter kafkaLogCommitter,
                          UUID readerId, Ignite ignite) {
        this.serializer = serializer;
        this.committer = committer;
        this.kafkaLogCommitter = kafkaLogCommitter;
        this.readerId = readerId;
        lead = ignite.services().serviceProxy(LeadService.NAME, LeadService.class, false);
    }

    @SuppressWarnings("unchecked")
    public boolean commit(Long txId, Map<Long, TransactionData> buffer) {
        try {
            TransactionData transactionScopeAndSerializedValues = buffer.get(txId);
            List<Map.Entry<String, List>> scope = transactionScopeAndSerializedValues.getTransactionScope().getScope();

            Iterator<String> cacheNames = scope.stream().map(Map.Entry::getKey).iterator();
            Iterator<List> keys = scope.stream().map(Map.Entry::getValue).iterator();
            Iterator values = serializer.<List>deserialize(transactionScopeAndSerializedValues.getValue()).iterator();

            LOGGER.trace("[R] Beginning commit sequence for transaction {}", txId);
            committer.commit(cacheNames, keys, values);
            kafkaLogCommitter.commitTransaction(txId);
            return true;
        } catch (Exception e) {
            LOGGER.error("[R] error while commit transaction in " + readerId, e);
            lead.notifyFailed(readerId, txId);
            return false;
        }
    }
}
