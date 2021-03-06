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
package com.epam.lagerta.cluster;

import org.apache.ignite.Ignite;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.ArrayList;
import java.util.List;

public class AppContextOneProcessClusterManager extends DefaultOneProcessClusterManager {
    private final String configPath;
    private final List<ConfigurableApplicationContext> contexts = new ArrayList<>();

    public AppContextOneProcessClusterManager(String configPath) {
        this.configPath = configPath;
    }

    /** {@inheritDoc} */
    @Override
    protected Ignite startGrid(int gridNumber, int clusterSize) {
        ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext(configPath);
        contexts.add(applicationContext);
        return applicationContext.getBean(Ignite.class);
    }

    public <T> T getBean(Class<T> beanClass) {
        return contexts.get(0).getBean(beanClass);
    }
}
