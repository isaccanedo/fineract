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
package org.apache.fineract.infrastructure.campaigns.jobs.updateemailoutboundwithcampaignmessage;

import org.apache.fineract.infrastructure.campaigns.email.domain.EmailCampaignRepository;
import org.apache.fineract.infrastructure.campaigns.email.domain.EmailMessageRepository;
import org.apache.fineract.infrastructure.campaigns.email.service.EmailCampaignReadPlatformService;
import org.apache.fineract.infrastructure.campaigns.email.service.EmailCampaignWritePlatformService;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UpdateEmailOutboundWithCampaignMessageConfig {

    @Autowired
    private JobBuilderFactory jobs;
    @Autowired
    private StepBuilderFactory steps;
    @Autowired
    private EmailCampaignReadPlatformService emailCampaignReadPlatformService;
    @Autowired
    private EmailCampaignRepository emailCampaignRepository;
    @Autowired
    private ClientRepositoryWrapper clientRepositoryWrapper;
    @Autowired
    private EmailMessageRepository emailMessageRepository;
    @Autowired
    private EmailCampaignWritePlatformService emailCampaignWritePlatformService;

    @Bean
    protected Step updateEmailOutboundWithCampaignMessageStep() {
        return steps.get(JobName.UPDATE_EMAIL_OUTBOUND_WITH_CAMPAIGN_MESSAGE.name())
                .tasklet(updateEmailOutboundWithCampaignMessageTasklet()).build();
    }

    @Bean
    public Job updateEmailOutboundWithCampaignMessageJob() {
        return jobs.get(JobName.UPDATE_EMAIL_OUTBOUND_WITH_CAMPAIGN_MESSAGE.name()).start(updateEmailOutboundWithCampaignMessageStep())
                .incrementer(new RunIdIncrementer()).build();
    }

    @Bean
    public UpdateEmailOutboundWithCampaignMessageTasklet updateEmailOutboundWithCampaignMessageTasklet() {
        return new UpdateEmailOutboundWithCampaignMessageTasklet(emailCampaignReadPlatformService, emailCampaignRepository,
                clientRepositoryWrapper, emailMessageRepository, emailCampaignWritePlatformService);
    }
}
