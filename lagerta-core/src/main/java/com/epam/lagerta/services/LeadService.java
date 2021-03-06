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
package com.epam.lagerta.services;

import com.epam.lagerta.capturer.TransactionScope;
import org.apache.ignite.services.Service;

import java.util.List;
import java.util.UUID;

public interface LeadService extends Service {
    String NAME = "Lead";

    List<Long> notifyRead(UUID readerId, List<TransactionScope> txScopes);

    void notifyCommitted(UUID readerId, List<Long> ids);

    void notifyFailed(UUID readerId, Long id);

    long getLastDenseCommitted();
}