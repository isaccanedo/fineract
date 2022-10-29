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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.fineract.batch.domain.BatchRequest;
import org.apache.fineract.batch.domain.BatchResponse;
import org.apache.fineract.portfolio.loanaccount.api.LoanTransactionsApiResource;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AdjustTransactionCommandStrategyTest {

    /**
     * Test {@link AdjustTransactionCommandStrategy#execute} happy path scenario.
     */
    @Test
    public void testExecuteSuccessScenario() {
        // given
        final TestContext testContext = new TestContext();

        final Long loanId = Long.valueOf(RandomStringUtils.randomNumeric(4));
        final Long transactionId = Long.valueOf(RandomStringUtils.randomNumeric(4));
        final BatchRequest request = getBatchRequest(loanId, transactionId);
        final String responseBody = "{\"officeId\":1,\"clientId\":107,\"loanId\":71,\"resourceId\":193,\"changes\""
                + ":{\"transactionDate\":\"03 October 2022\",\"transactionAmount\":\"500\",\"locale\":\"en\",\"dateFormat\":"
                + "\"dd MMMM yyyy\",\"paymentTypeId\":\"\"}}";

        given(testContext.loanTransactionsApiResource.adjustLoanTransaction(eq(loanId), eq(transactionId), eq(request.getBody()), eq(null)))
                .willReturn(responseBody);

        // when
        final BatchResponse response = testContext.subjectToTest.execute(request, testContext.uriInfo);

        // then
        assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
        assertEquals(response.getRequestId(), request.getRequestId());
        assertEquals(response.getHeaders(), request.getHeaders());
        assertEquals(response.getBody(), responseBody);
    }

    /**
     * Creates and returns a request with the given loan id and transaction id.
     *
     * @param loanId
     *            the loan id
     * @param transactionId
     *            the transaction id
     * @return BatchRequest
     */
    private BatchRequest getBatchRequest(final Long loanId, final Long transactionId) {

        final BatchRequest br = new BatchRequest();
        String relativeUrl = String.format("loans/%s/transactions/%s", loanId, transactionId);

        br.setRequestId(Long.valueOf(RandomStringUtils.randomNumeric(5)));
        br.setRelativeUrl(relativeUrl);
        br.setMethod(HttpMethod.POST);
        br.setReference(Long.valueOf(RandomStringUtils.randomNumeric(5)));
        br.setBody("{\"locale\":\"en\",\"dateFormat\":\"dd MMMM yyyy\",\"transactionDate\":\"03 October 2022\",\"transactionAmount\":500}");

        return br;
    }

    /**
     * Private test context class used since testng runs in parallel to avoid state between tests
     */
    private static class TestContext {

        @Mock
        private UriInfo uriInfo;

        @Mock
        private LoanTransactionsApiResource loanTransactionsApiResource;

        private final AdjustTransactionCommandStrategy subjectToTest;

        TestContext() {
            MockitoAnnotations.openMocks(this);
            subjectToTest = new AdjustTransactionCommandStrategy(loanTransactionsApiResource);
        }
    }
}
