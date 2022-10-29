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
package org.apache.fineract.batch.command.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.fineract.batch.domain.BatchRequest;
import org.apache.fineract.batch.domain.BatchResponse;
import org.apache.fineract.portfolio.loanaccount.api.LoanTransactionsApiResource;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Test class fpr {@link CreateTransactionLoanCommandStrategy}.
 */
public class CreateTransactionLoanCommandStrategyTest {

    /**
     * Test {@link CreateTransactionLoanCommandStrategy#execute} happy path scenario.
     */
    @Test
    public void testExecuteSuccessScenario() {
        final TestContext testContext = new TestContext();
        final Long loanId = Long.valueOf(RandomStringUtils.randomNumeric(4));
        final String command = "myCommand";
        final BatchRequest batchRequest = getBatchRequest(loanId, command);
        final String responseBody = "myResponseBody";

        when(testContext.loanTransactionsApiResource.executeLoanTransaction(loanId, command, batchRequest.getBody()))
                .thenReturn(responseBody);

        BatchResponse batchResponse = testContext.subjectToTest.execute(batchRequest, testContext.uriInfo);

        assertEquals(HttpStatus.SC_OK, batchResponse.getStatusCode());
        assertSame(responseBody, batchResponse.getBody());
        assertEquals(batchRequest.getRequestId(), batchResponse.getRequestId());
        assertEquals(batchRequest.getHeaders(), batchResponse.getHeaders());

        verify(testContext.loanTransactionsApiResource).executeLoanTransaction(loanId, command, batchRequest.getBody());
    }

    /**
     * Creates and returns a request with the given loan id and command value.
     *
     * @param loanId
     *            the loan id
     * @param command
     *            the transaction id
     * @return BatchRequest
     */
    private BatchRequest getBatchRequest(final Long loanId, final String command) {

        final BatchRequest br = new BatchRequest();
        String relativeUrl = "loans/" + loanId + "/transactions?command=" + command;

        br.setRequestId(Long.valueOf(RandomStringUtils.randomNumeric(5)));
        br.setRelativeUrl(relativeUrl);
        br.setMethod(HttpMethod.POST);
        br.setReference(Long.valueOf(RandomStringUtils.randomNumeric(5)));
        br.setBody("{}");

        return br;
    }

    /**
     * Private test context class used since testng runs in parallel to avoid state between tests
     */
    private static class TestContext {

        /**
         * Mock URI info.
         */
        @Mock
        private UriInfo uriInfo;

        /**
         * Mock loan transactions API resource.
         */
        @Mock
        private LoanTransactionsApiResource loanTransactionsApiResource;

        /**
         * The {@link CreateTransactionLoanCommandStrategy} under test.
         */
        private final CreateTransactionLoanCommandStrategy subjectToTest;

        /**
         * Constructor.
         */
        TestContext() {
            MockitoAnnotations.openMocks(this);
            subjectToTest = new CreateTransactionLoanCommandStrategy(loanTransactionsApiResource);
        }
    }
}
