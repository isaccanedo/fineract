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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.stream.Stream;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.fineract.batch.domain.BatchRequest;
import org.apache.fineract.batch.domain.BatchResponse;
import org.apache.fineract.infrastructure.core.api.MutableUriInfo;
import org.apache.fineract.portfolio.loanaccount.api.LoanChargesApiResource;
import org.apache.http.HttpStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * The {@link GetChargeByIdCommandStrategy} test class.
 */
public class GetChargeByIdCommandStrategyTest {

    /**
     * Query parameter provider.
     *
     * @return the test data stream
     */
    private static Stream<Arguments> provideQueryParameters() {
        return Stream.of(Arguments.of(null, 0), Arguments.of("fields=name,amountOrPercentage", 1));
    }

    /**
     * Test {@link GetChargeByIdCommandStrategy#execute} happy path scenario.
     */
    @ParameterizedTest
    @MethodSource("provideQueryParameters")
    public void testExecuteSuccessScenario(final String queryParameter, final int numberOfQueryParams) {
        final TestContext testContext = new TestContext();

        final Long loanId = Long.valueOf(RandomStringUtils.randomNumeric(4));
        final Long chargeId = Long.valueOf(RandomStringUtils.randomNumeric(4));
        final BatchRequest request = getBatchRequest(loanId, chargeId, queryParameter);
        final String responseBody = "someResponseBody";

        given(testContext.loanChargesApiResource.retrieveLoanCharge(eq(loanId), eq(chargeId), any(UriInfo.class))).willReturn(responseBody);

        final BatchResponse response = testContext.subjectToTest.execute(request, testContext.uriInfo);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        assertThat(response.getRequestId()).isEqualTo(request.getRequestId());
        assertThat(response.getHeaders()).isEqualTo(request.getHeaders());
        assertThat(response.getBody()).isEqualTo(responseBody);

        verify(testContext.loanChargesApiResource).retrieveLoanCharge(eq(loanId), eq(chargeId), testContext.uriInfoCaptor.capture());
        MutableUriInfo mutableUriInfo = testContext.uriInfoCaptor.getValue();
        assertThat(mutableUriInfo.getAdditionalQueryParameters()).hasSize(numberOfQueryParams);
    }

    /**
     * Creates and returns a request with the given loan id and charge id.
     *
     * @param loanId
     *            the loan id
     * @param chargeId
     *            the charge id
     * @param queryParameter
     *            the query parameter
     * @return BatchRequest
     */
    private BatchRequest getBatchRequest(final Long loanId, final Long chargeId, final String queryParameter) {
        final BatchRequest br = new BatchRequest();
        String relativeUrl = "loans/" + loanId + "/charges/" + chargeId;

        if (queryParameter != null) {
            relativeUrl = relativeUrl + "?" + queryParameter;
        }

        br.setRequestId(Long.valueOf(RandomStringUtils.randomNumeric(5)));
        br.setRelativeUrl(relativeUrl);
        br.setMethod(HttpMethod.GET);
        br.setReference(Long.valueOf(RandomStringUtils.randomNumeric(5)));
        br.setBody("{}");

        return br;
    }

    /**
     * Private test context class used since testng runs in parallel to avoid state between tests
     */
    private static class TestContext {

        /**
         * The subject to test
         */
        private final GetChargeByIdCommandStrategy subjectToTest;

        /**
         * The mock uri info
         */
        @Mock
        private UriInfo uriInfo;

        /**
         * The mock {@link LoanChargesApiResource} object.
         */
        @Mock
        private LoanChargesApiResource loanChargesApiResource;

        /**
         * The uri info captor
         */
        @Captor
        private ArgumentCaptor<MutableUriInfo> uriInfoCaptor;

        /**
         * Test Context constructor
         */
        TestContext() {
            MockitoAnnotations.openMocks(this);
            subjectToTest = new GetChargeByIdCommandStrategy(loanChargesApiResource);
        }
    }
}
