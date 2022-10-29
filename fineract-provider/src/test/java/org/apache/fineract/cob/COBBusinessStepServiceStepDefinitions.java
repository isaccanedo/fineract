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
package org.apache.fineract.cob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.google.common.base.Splitter;
import io.cucumber.java8.En;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import org.apache.fineract.cob.domain.BatchBusinessStep;
import org.apache.fineract.cob.domain.BatchBusinessStepRepository;
import org.apache.fineract.cob.exceptions.BusinessStepException;
import org.apache.fineract.cob.loan.LoanCOBBusinessStep;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.infrastructure.core.domain.ActionContext;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.event.business.service.BusinessEventNotifierService;
import org.apache.fineract.mix.data.MixTaxonomyData;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationContext;

public class COBBusinessStepServiceStepDefinitions implements En {

    private ApplicationContext applicationContext = mock(ApplicationContext.class);
    private ListableBeanFactory beanFactory = mock(ListableBeanFactory.class);
    private BatchBusinessStepRepository batchBusinessStepRepository = mock(BatchBusinessStepRepository.class);
    private BusinessEventNotifierService businessEventNotifierService = mock(BusinessEventNotifierService.class);
    private final COBBusinessStepService businessStepService = new COBBusinessStepServiceImpl(batchBusinessStepRepository,
            applicationContext, beanFactory, businessEventNotifierService);
    private COBBusinessStep cobBusinessStep = mock(COBBusinessStep.class);
    private COBBusinessStep notRegistereCobBusinessStep = mock(COBBusinessStep.class);
    private TreeMap<Long, String> executionMap;
    private AbstractAuditableCustom item;
    private AbstractAuditableCustom outputItem = mock(AbstractAuditableCustom.class);

    private AbstractAuditableCustom resultItem;

    private HashMap<MixTaxonomyData, BigDecimal> data = new HashMap<>();

    private String result;
    private Class clazz;
    private String jobName;
    private BatchBusinessStep batchBusinessStep = mock(BatchBusinessStep.class);
    private TreeMap<Long, String> resultMap;

    public COBBusinessStepServiceStepDefinitions() {
        Given("/^The COBBusinessStepService.run method with executeMap (.*)$/", (String executionMap) -> {
            if ("null".equals(executionMap)) {
                this.executionMap = null;
            } else if ("".equals(executionMap)) {
                this.executionMap = new TreeMap<>();
            } else {
                List<String> splitStr = Splitter.on(',').splitToList(executionMap);
                Long key = Long.parseLong(splitStr.get(0));
                String value = splitStr.get(1);
                this.executionMap = new TreeMap<>();
                this.executionMap.put(key, value);
            }

            this.item = mock(AbstractAuditableCustom.class);

            lenient().when(this.applicationContext.getBean("test")).thenReturn(cobBusinessStep);
            lenient().when(this.applicationContext.getBean("notExist")).thenThrow(BeanCreationException.class);
            lenient().when(this.cobBusinessStep.execute(this.item)).thenReturn(outputItem);

            ThreadLocalContextUtil.setActionContext(ActionContext.DEFAULT);
        });

        Given("/^The COBBusinessStepService.getCOBBusinessStepMap method with businessStepClass (.*), and jobName (.*)$/",
                (String className, String jobName) -> {
                    if ("null".equals(className)) {
                        this.clazz = null;
                    } else if ("empty".equals(className)) {
                        this.clazz = String.class;
                    } else if ("LoanCOBBusinessStep".equals(className)) {
                        this.clazz = LoanCOBBusinessStep.class;
                    } else {
                        this.clazz = Object.class;
                    }

                    this.jobName = jobName;

                    lenient().when(this.batchBusinessStepRepository.findAllByJobName("exist"))
                            .thenReturn(Collections.singletonList(this.batchBusinessStep));
                    lenient().when(this.batchBusinessStepRepository.findAllByJobName("notExist")).thenReturn(Collections.emptyList());

                    lenient().when(this.beanFactory.getBeanNamesForType((Class<?>) null)).thenReturn(new String[] { "notExist" });
                    lenient().when(this.beanFactory.getBeanNamesForType(Object.class)).thenReturn(new String[] { "testNotRegistered" });
                    lenient().when(this.beanFactory.getBeanNamesForType(String.class)).thenReturn(new String[] {});
                    lenient().when(this.beanFactory.getBeanNamesForType(LoanCOBBusinessStep.class)).thenReturn(new String[] { "test" });

                    lenient().when(this.applicationContext.getBean("test")).thenReturn(this.cobBusinessStep);
                    lenient().when(this.applicationContext.getBean("testNotRegistered")).thenReturn(this.notRegistereCobBusinessStep);
                    lenient().when(this.applicationContext.getBean("notExist")).thenThrow(BeanCreationException.class);

                    lenient().when(this.cobBusinessStep.getEnumStyledName()).thenReturn("registered");
                    lenient().when(this.notRegistereCobBusinessStep.getEnumStyledName()).thenReturn("notRegistered");
                    lenient().when(this.batchBusinessStep.getStepName()).thenReturn("registered");
                    lenient().when(this.batchBusinessStep.getStepOrder()).thenReturn(1L);

                    lenient().when(this.cobBusinessStep.execute(this.item)).thenReturn(outputItem);

                });

        When("COBBusinessStepService.run method executed", () -> {
            resultItem = this.businessStepService.run(this.executionMap, this.item);
        });

        When("COBBusinessStepService.getCOBBusinessStepMap method executed", () -> {
            resultMap = this.businessStepService.getCOBBusinessStepMap(this.clazz, this.jobName);
        });

        Then("The COBBusinessStepService.run result should match", () -> {
            assertEquals(outputItem, resultItem);
            assertEquals(ActionContext.COB, ThreadLocalContextUtil.getActionContext());
            ThreadLocalContextUtil.setActionContext(ActionContext.DEFAULT);
        });

        Then("throw exception COBBusinessStepService.run method", () -> {
            assertThrows(BusinessStepException.class, () -> {
                resultItem = this.businessStepService.run(this.executionMap, this.item);
            });
            ThreadLocalContextUtil.setActionContext(ActionContext.DEFAULT);
        });

        Then("The COBBusinessStepService.getCOBBusinessStepMap result exception", () -> {
            assertThrows(BeanCreationException.class, () -> {
                this.businessStepService.getCOBBusinessStepMap(this.clazz, this.jobName);
            });
            ThreadLocalContextUtil.setActionContext(ActionContext.DEFAULT);
        });

        Then("The COBBusinessStepService.getCOBBusinessStepMap result should match", () -> {
            assertEquals(1, resultMap.size());
            assertEquals("test", resultMap.get(1L));
            ThreadLocalContextUtil.setActionContext(ActionContext.DEFAULT);
        });

        Then("The COBBusinessStepService.getCOBBusinessStepMap result empty", () -> {
            assertEquals(0, resultMap.size());
            ThreadLocalContextUtil.setActionContext(ActionContext.DEFAULT);
        });

    }
}
