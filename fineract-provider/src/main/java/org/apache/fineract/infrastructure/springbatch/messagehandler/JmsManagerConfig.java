/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.springbatch.messagehandler;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.apache.fineract.infrastructure.springbatch.OutputChannelInterceptor;
import org.apache.fineract.infrastructure.springbatch.messagehandler.conditions.JmsManagerCondition;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.jms.dsl.Jms;

@Configuration
@EnableBatchIntegration
@Conditional(JmsManagerCondition.class)
@Import(value = { JmsBrokerConfiguration.class })
public class JmsManagerConfig {

    @Autowired
    private DirectChannel outboundRequests;
    @Autowired
    private OutputChannelInterceptor outputInterceptor;
    @Autowired
    private FineractProperties fineractProperties;

    @Bean
    public IntegrationFlow outboundFlow(ActiveMQConnectionFactory connectionFactory) {
        return IntegrationFlows.from(outboundRequests) //
                .intercept(outputInterceptor) //
                .log(LoggingHandler.Level.DEBUG) //
                .handle(Jms.outboundAdapter(connectionFactory)
                        .destination(fineractProperties.getRemoteJobMessageHandler().getJms().getRequestQueueName()))
                .get();
    }
}
