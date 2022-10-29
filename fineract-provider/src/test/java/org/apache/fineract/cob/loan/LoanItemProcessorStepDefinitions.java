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
package org.apache.fineract.cob.loan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import io.cucumber.java8.En;
import java.util.TreeMap;
import org.apache.fineract.cob.COBBusinessStepService;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

public class LoanItemProcessorStepDefinitions implements En {

    private COBBusinessStepService cobBusinessStepService = mock(COBBusinessStepService.class);

    private LoanItemProcessor loanItemProcessor = new LoanItemProcessor(cobBusinessStepService);

    private Loan loan = mock(Loan.class);

    private Loan loanItem;
    private Loan processedLoan = mock(Loan.class);

    private Loan resultItem;

    private TreeMap<Long, String> treeMap = mock(TreeMap.class);

    public LoanItemProcessorStepDefinitions() {
        Given("/^The LoanItemProcessor.process method with item (.*)$/", (String loanItem) -> {

            StepExecution stepExecution = new StepExecution("test", null);
            ExecutionContext stepExecutionContext = new ExecutionContext();
            stepExecutionContext.put(LoanCOBConstant.BUSINESS_STEP_MAP, treeMap);
            stepExecution.setExecutionContext(stepExecutionContext);
            loanItemProcessor.beforeStep(stepExecution);

            if (loanItem.isEmpty()) {
                this.loanItem = null;
            } else {
                this.loanItem = loan;
            }

            lenient().when(this.cobBusinessStepService.run(treeMap, null)).thenThrow(new RuntimeException("fail"));
            lenient().when(this.cobBusinessStepService.run(treeMap, loan)).thenReturn(processedLoan);

        });

        When("LoanItemProcessor.process method executed", () -> {
            resultItem = this.loanItemProcessor.process(loanItem);
        });

        Then("LoanItemProcessor.process result should match", () -> {
            assertEquals(processedLoan, resultItem);
        });

        Then("throw exception LoanItemProcessor.process method", () -> {
            assertThrows(RuntimeException.class, () -> {
                resultItem = this.loanItemProcessor.process(loanItem);
            });
        });
    }
}
