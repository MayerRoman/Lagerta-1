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
package com.epam.lagerta.subscriber.lead;

import com.epam.lagerta.capturer.TransactionScope;
import com.epam.lagerta.common.CallableKeyListTask;
import com.epam.lagerta.common.CallableKeyTask;
import com.epam.lagerta.common.Scheduler;
import com.epam.lagerta.subscriber.ReaderTxScope;
import com.epam.lagerta.subscriber.util.PlannerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class LeadImpl extends Scheduler implements Lead {
    private static final Logger LOGGER = LoggerFactory.getLogger(LeadImpl.class);

    private final Set<Long> inProgress = new HashSet<>();
    private final Set<UUID> lostReaders = new HashSet<>();
    private final CallableKeyTask<List<Long>, UUID, List<Long>> toCommit = new CallableKeyListTask<>(this);

    private final CommittedTransactions committed;
    private final ReadTransactions readTransactions;
    private final Heartbeats heartbeats;
    private final Reconciler reconciler;

    LeadImpl(
            LeadStateAssistant stateAssistant,
            ReadTransactions readTransactions,
            CommittedTransactions committed,
            Heartbeats heartbeats,
            GapDetectionStrategy gapDetectionStrategy,
            Reconciler reconciler,
            RuleTimeouts timeouts
    ) {
        this.readTransactions = readTransactions;
        this.committed = committed;
        this.heartbeats = heartbeats;
        this.reconciler = reconciler;
        pushTask(() -> stateAssistant.load(this));
        registerRule(this.committed::compress);
        per(timeouts.getHearbeatExpirationThreshold()).execute(this::markLostAndFound);
        registerRule(() -> this.readTransactions.pruneCommitted(this.committed, heartbeats, lostReaders, inProgress));
        registerRule(this::plan);
        per(timeouts.getSaveStatePeriod()).execute(() -> stateAssistant.saveState(this));
        per(timeouts.getGapCheckPeriod()).execute(() ->
                reconcileOnGaps(readTransactions, committed, gapDetectionStrategy));
    }

    @SuppressWarnings("unused") // used in Spring config
    public LeadImpl(LeadStateAssistant stateAssistant, GapDetectionStrategy gapDetectionStrategy,
                    Reconciler reconciler, RuleTimeouts ruleTimeouts) {
        this(stateAssistant, new ReadTransactions(), CommittedTransactions.createNotReady(),
                new Heartbeats(ruleTimeouts.getHearbeatExpirationThreshold()), gapDetectionStrategy, reconciler,
                ruleTimeouts);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Long> notifyRead(UUID readerId, List<TransactionScope> txScopes) {
        if (!txScopes.isEmpty()) {
            LOGGER.trace("[L] notify read from {} ->  {}", readerId, txScopes);
        }
        pushTask(() -> heartbeats.update(readerId));
        List<Long> result = !txScopes.isEmpty()
                ? toCommit.call(readerId, () -> readTransactions.addAllOnNode(readerId, txScopes))
                : toCommit.call(readerId);
        if (result != null) {
            LOGGER.trace("[L] ready to commit for {} ->  {}", readerId, result);
        }
        return result == null ? Collections.emptyList() : result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyCommitted(UUID readerId, List<Long> ids) {
        LOGGER.trace("[L] notify committed from {} -> {} ", readerId, ids);
        pushTask(() -> {
            committed.addAll(ids);
            inProgress.removeAll(ids);
            heartbeats.update(readerId);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyFailed(UUID readerId, Long id) {
        LOGGER.error("[L] notify failed transaction from {} ->  {}", readerId, id);
        //todo
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateState(CommittedTransactions newCommitted) {
        LOGGER.info("[L] loaded {}", newCommitted);
        pushTask(() -> {
            committed.addAll(newCommitted);
            readTransactions.makeReady();
            readTransactions.pruneCommitted(committed, heartbeats, lostReaders, inProgress);
        });
    }

    @Override
    public boolean isReconciliationGoing() {
        return reconciler.isReconciliationGoing();
    }

    private void markLostAndFound() {
        for (UUID readerId : heartbeats.knownReaders()) {
            boolean knownAsLost = lostReaders.contains(readerId);
            if (heartbeats.isAvailable(readerId) == knownAsLost) {
                if (knownAsLost ? lostReaders.remove(readerId) : lostReaders.add(readerId)) {
                    readTransactions.scheduleDuplicatesPruning();
                }
            }
        }
    }

    private void plan() {
        List<ReaderTxScope> ready = PlannerUtil.plan(readTransactions, committed, inProgress, lostReaders);
        if (!ready.isEmpty()) {
            LOGGER.trace("[L] Planned {}", ready);
        }
        ready.stream()
                .peek(ReaderTxScope::markInProgress)
                .peek(scope -> inProgress.add(scope.getTransactionId()))
                .collect(groupingBy(ReaderTxScope::getReaderId, toList()))
                .forEach((key, value) -> toCommit.append(key,
                        value.stream().map(TransactionScope::getTransactionId).collect(toList())));
    }

    private void reconcileOnGaps(ReadTransactions readTransactions, CommittedTransactions committed,
                                 GapDetectionStrategy gapDetectionStrategy) {
        List<Long> gaps = gapDetectionStrategy.gapDetected(committed, readTransactions);
        if (!gaps.isEmpty() && !isReconciliationGoing()) {
            LOGGER.debug("[L] Gaps were found. Starting gap fixing");
            reconciler.startReconciliation(gaps);
        }
    }

    @Override
    public long getLastDenseCommitted() {
        return committed.getLastDenseCommit();
    }
}
