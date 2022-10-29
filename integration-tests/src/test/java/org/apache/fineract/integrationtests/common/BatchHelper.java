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
package org.apache.fineract.integrationtests.common;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.HttpMethod;
import org.apache.fineract.batch.command.internal.CreateTransactionLoanCommandStrategy;
import org.apache.fineract.batch.domain.BatchRequest;
import org.apache.fineract.batch.domain.BatchResponse;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for {@link org.apache.fineract.integrationtests.BatchApiTest}. It takes care of creation of
 * {@code BatchRequest} list and posting this list to the server.
 *
 * @author Rishabh Shukla
 *
 * @see org.apache.fineract.integrationtests.BatchApiTest
 */
public final class BatchHelper {

    private static final Logger LOG = LoggerFactory.getLogger(BatchHelper.class);
    private static final String BATCH_API_URL = "/fineract-provider/api/v1/batches?" + Utils.TENANT_IDENTIFIER;
    private static final String BATCH_API_URL_EXT = BATCH_API_URL + "&enclosingTransaction=true";
    private static final SecureRandom secureRandom = new SecureRandom();

    private BatchHelper() {

    }

    /**
     * Returns a JSON String for a list of {@code BatchRequest}s
     *
     * @param batchRequests
     * @return JSON String of BatchRequest
     */
    public static String toJsonString(final List<BatchRequest> batchRequests) {
        return new Gson().toJson(batchRequests);
    }

    /**
     * Returns a Map from Json String
     *
     * @param
     * @return Map
     */
    public static Map generateMapFromJsonString(final String jsonString) {
        return new Gson().fromJson(jsonString, Map.class);
    }

    /**
     * Returns the converted string response into JSON.
     *
     * @param json
     * @return {@code List<BatchResponse>}
     */
    private static List<BatchResponse> fromJsonString(final String json) {
        return new Gson().fromJson(json, new TypeToken<List<BatchResponse>>() {}.getType());
    }

    /**
     * Returns a list of BatchResponse with query parameter enclosing transaction set to false by posting the jsonified
     * BatchRequest to the server.
     *
     * @param requestSpec
     * @param responseSpec
     * @param jsonifiedBatchRequests
     * @return a list of BatchResponse
     */
    public static List<BatchResponse> postBatchRequestsWithoutEnclosingTransaction(final RequestSpecification requestSpec,
            final ResponseSpecification responseSpec, final String jsonifiedBatchRequests) {
        final String response = Utils.performServerPost(requestSpec, responseSpec, BATCH_API_URL, jsonifiedBatchRequests, null);
        LOG.info("BatchHelper Response {}", response);
        return BatchHelper.fromJsonString(response);
    }

    /**
     * Returns a list of BatchResponse with query parameter enclosing transaction set to true by posting the jsonified
     * BatchRequest to the server.
     *
     * @param requestSpec
     * @param responseSpec
     * @param jsonifiedBatchRequests
     * @return a list of BatchResponse
     */
    public static List<BatchResponse> postBatchRequestsWithEnclosingTransaction(final RequestSpecification requestSpec,
            final ResponseSpecification responseSpec, final String jsonifiedBatchRequests) {
        final String response = Utils.performServerPost(requestSpec, responseSpec, BATCH_API_URL_EXT, jsonifiedBatchRequests, null);
        return BatchHelper.fromJsonString(response);
    }

    /**
     * Returns a BatchResponse based on the given BatchRequest, by posting the request to the server.
     *
     * @param
     * @return {@code List<BatchResponse>}
     */
    public static List<BatchResponse> postWithSingleRequest(final RequestSpecification requestSpec,
            final ResponseSpecification responseSpec, final BatchRequest br) {

        final List<BatchRequest> batchRequests = new ArrayList<>();
        batchRequests.add(br);

        final String jsonifiedRequest = BatchHelper.toJsonString(batchRequests);
        final List<BatchResponse> response = BatchHelper.postBatchRequestsWithoutEnclosingTransaction(requestSpec, responseSpec,
                jsonifiedRequest);

        // Verifies that the response result is there
        Assertions.assertNotNull(response);
        Assertions.assertTrue(response.size() > 0);

        return response;
    }

    /**
     * Creates and returns a {@link org.apache.fineract.batch.command.internal.CreateClientCommandStrategy} Request as
     * one of the request in Batch.
     *
     * @param
     * @param externalId
     * @return BatchRequest
     */
    public static BatchRequest createClientRequest(final Long requestId, final String externalId) {

        final BatchRequest br = new BatchRequest();
        br.setRequestId(requestId);
        br.setRelativeUrl("clients");
        br.setMethod("POST");

        final String extId;
        if (externalId.equals("")) {
            extId = "ext" + String.valueOf((10000 * secureRandom.nextDouble())) + String.valueOf((10000 * secureRandom.nextDouble()));
        } else {
            extId = externalId;
        }

        final String body = "{ \"officeId\": 1, \"legalFormId\":1, \"firstname\": \"Petra\", \"lastname\": \"Yton\"," + "\"externalId\": "
                + extId + ",  \"dateFormat\": \"dd MMMM yyyy\", \"locale\": \"en\","
                + "\"active\": false, \"submittedOnDate\": \"04 March 2009\"}";

        br.setBody(body);

        return br;
    }

    /**
     * Creates and returns a {@link org.apache.fineract.batch.command.internal.CreateClientCommandStrategy} Request as
     * one of the request in Batch.
     *
     * @param
     * @param externalId
     * @return BatchRequest
     */
    public static BatchRequest createActiveClientRequest(final Long requestId, final String externalId) {

        final BatchRequest br = new BatchRequest();
        br.setRequestId(requestId);
        br.setRelativeUrl("clients");
        br.setMethod("POST");

        final String extId;
        if (externalId.equals("")) {
            extId = "ext" + String.valueOf((10000 * secureRandom.nextDouble())) + String.valueOf((10000 * secureRandom.nextDouble()));
        } else {
            extId = externalId;
        }

        final String body = "{ \"officeId\": 1, \"legalFormId\":1, \"firstname\": \"Petra\", \"lastname\": \"Yton\"," + "\"externalId\": \""
                + externalId + "\",  \"dateFormat\": \"dd MMMM yyyy\", \"locale\": \"en\","
                + "\"active\": true, \"activationDate\": \"04 March 2010\", \"submittedOnDate\": \"04 March 2010\"}";

        br.setBody(body);

        return br;
    }

    /**
     * Creates and returns a {@link org.apache.fineract.batch.command.internal.UpdateClientCommandStrategy} Request with
     * given requestId and reference.
     *
     * @param
     * @param
     * @return BatchRequest
     */
    public static BatchRequest updateClientRequest(final Long requestId, final Long reference) {

        final BatchRequest br = new BatchRequest();

        br.setRequestId(requestId);
        br.setRelativeUrl("clients/$.clientId");
        br.setMethod("PUT");
        br.setReference(reference);
        br.setBody("{\"firstname\": \"TestFirstName\", \"lastname\": \"TestLastName\"}");

        return br;
    }

    /**
     * Creates and returns a {@link org.apache.fineract.batch.command.internal.ApplyLoanCommandStrategy} Request with
     * given requestId and reference.
     *
     * @param requestId
     *            the request ID
     * @param reference
     *            the reference ID
     * @param productId
     *            the product ID
     * @return BatchRequest the batch request
     */
    public static BatchRequest applyLoanRequest(final Long requestId, final Long reference, final Integer productId,
            final Integer clientCollateralId) {
        return applyLoanRequest(requestId, reference, productId, clientCollateralId, LocalDate.now(ZoneId.systemDefault()).minusDays(10),
                "10,000.00");
    }

    /**
     * Creates and returns a {@link org.apache.fineract.batch.command.internal.ApplyLoanCommandStrategy} Request with
     * given requestId and reference.
     *
     * @param requestId
     *            the request ID
     * @param reference
     *            the reference ID
     * @param productId
     *            the product ID
     * @param date
     *            the loan submitted on date
     * @param loanAmount
     *            the loan amount
     * @return BatchRequest the batch request
     */
    public static BatchRequest applyLoanRequest(final Long requestId, final Long reference, final Integer productId,
            final Integer clientCollateralId, final LocalDate date, final String loanAmount) {

        final BatchRequest br = new BatchRequest();

        br.setRequestId(requestId);
        br.setRelativeUrl("loans");
        br.setMethod("POST");
        br.setReference(reference);
        String dateString = date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));

        String body = "{\"dateFormat\": \"dd MMMM yyyy\", \"locale\": \"en_GB\", \"clientId\": \"$.clientId\"," + "\"productId\": "
                + productId + ", \"principal\": \"" + loanAmount + "\", \"loanTermFrequency\": 10,"
                + "\"loanTermFrequencyType\": 2, \"loanType\": \"individual\", \"numberOfRepayments\": 10,"
                + "\"repaymentEvery\": 1, \"repaymentFrequencyType\": 2, \"interestRatePerPeriod\": 10,"
                + "\"amortizationType\": 1, \"interestType\": 0, \"interestCalculationPeriodType\": 1,"
                + "\"transactionProcessingStrategyId\": 1, \"expectedDisbursementDate\": \"" + dateString + "\",";

        if (clientCollateralId != null) {
            body = body + "\"collateral\": [{\"clientCollateralId\": \"" + clientCollateralId + "\", \"quantity\": \"1\"}],";
        }

        body = body + "\"submittedOnDate\": \"" + dateString + "\"}";

        br.setBody(body);

        return br;
    }

    /**
     * Creates and returns a {@link org.apache.fineract.batch.command.internal.ApplyLoanCommandStrategy} request with
     * given clientId and product id.
     *
     * @param requestId
     *            the request id
     * @param clientId
     *            the client id
     * @param productId
     *            the product id
     * @return {@link BatchRequest}
     */
    public static BatchRequest applyLoanRequestWithClientId(final Long requestId, final Integer clientId, final Integer productId) {

        final BatchRequest br = new BatchRequest();

        br.setRequestId(requestId);
        br.setRelativeUrl("loans");
        br.setMethod("POST");

        String body = String.format("{\"dateFormat\": \"dd MMMM yyyy\", \"locale\": \"en_GB\", \"clientId\": %s, "
                + "\"productId\": %s, \"principal\": \"10,000.00\", \"loanTermFrequency\": 10,"
                + "\"loanTermFrequencyType\": 2, \"loanType\": \"individual\", \"numberOfRepayments\": 10,"
                + "\"repaymentEvery\": 1, \"repaymentFrequencyType\": 2, \"interestRatePerPeriod\": 10,"
                + "\"amortizationType\": 1, \"interestType\": 0, \"interestCalculationPeriodType\": 1,"
                + "\"transactionProcessingStrategyId\": 1, \"expectedDisbursementDate\": \"10 Jun 2013\","
                + "\"submittedOnDate\": \"10 Jun 2013\"}", clientId, productId);

        br.setBody(body);

        return br;
    }

    /**
     * Creates and returns a {@link org.apache.fineract.batch.command.internal.ApplySavingsCommandStrategy} Request with
     * given requestId and reference.
     *
     * @param requestId
     * @param reference
     * @param productId
     * @return BatchRequest
     */
    public static BatchRequest applySavingsRequest(final Long requestId, final Long reference, final Integer productId) {

        final BatchRequest br = new BatchRequest();

        br.setRequestId(requestId);
        br.setRelativeUrl("savingsaccounts");
        br.setMethod("POST");
        br.setReference(reference);

        final String body = "{\"clientId\": \"$.clientId\", \"productId\": " + productId + ","
                + "\"locale\": \"en\", \"dateFormat\": \"dd MMMM yyyy\", \"submittedOnDate\": \"01 March 2011\"}";
        br.setBody(body);

        return br;
    }

    /**
     * Creates and returns a {@link org.apache.fineract.batch.command.internal.CreateChargeCommandStrategy} Request with
     * given requestId and reference
     *
     * @param requestId
     *            the batch request id.
     * @param reference
     *            the reference id.
     * @param chargeId
     *            the charge id used for getting charge type.
     * @return BatchRequest
     */
    public static BatchRequest createChargeRequest(final Long requestId, final Long reference, final Integer chargeId) {

        final BatchRequest br = new BatchRequest();
        br.setRequestId(requestId);
        br.setRelativeUrl("loans/$.loanId/charges");
        br.setMethod("POST");
        br.setReference(reference);

        final String dateFormat = "dd MMMM yyyy";
        final String dateString = LocalDate.now(Utils.getZoneIdOfTenant()).format(DateTimeFormatter.ofPattern(dateFormat));

        final String body = String.format(
                "{\"chargeId\": \"%d\", \"locale\": \"en\", \"amount\": \"11.15\", " + "\"dateFormat\": \"%s\", \"dueDate\": \"%s\"}",
                chargeId, dateFormat, dateString);
        br.setBody(body);

        return br;
    }

    /**
     * Creates and returns a {@link org.apache.fineract.batch.command.internal.CollectChargesCommandStrategy} Request
     * with given requestId and reference.
     *
     * @param requestId
     * @param reference
     * @return BatchRequest
     */
    public static BatchRequest collectChargesRequest(final Long requestId, final Long reference) {

        final BatchRequest br = new BatchRequest();

        br.setRequestId(requestId);
        br.setRelativeUrl("loans/$.loanId/charges");
        br.setReference(reference);
        br.setMethod("GET");
        br.setBody("{ }");

        return br;
    }

    /**
     * Creates and returns a {@link org.apache.fineract.batch.command.internal.GetChargeByIdCommandStrategy} request
     * with given requestId and reference.
     *
     * @param requestId
     *            the request id
     * @param reference
     *            the reference
     * @return the {@link BatchRequest}
     */
    public static BatchRequest getChargeByIdCommandStrategy(final Long requestId, final Long reference) {

        final BatchRequest br = new BatchRequest();
        String relativeUrl = "loans/$.loanId/charges/$.resourceId";

        br.setRequestId(requestId);
        br.setRelativeUrl(relativeUrl);
        br.setMethod(HttpMethod.GET);
        br.setReference(reference);
        br.setBody("{}");

        return br;
    }

    /**
     * Creates and returns a {@link org.apache.fineract.batch.command.internal.ActivateClientCommandStrategy} Request
     * with given requestId and reference.
     *
     *
     * @param requestId
     * @param reference
     * @return BatchRequest
     */
    public static BatchRequest activateClientRequest(final Long requestId, final Long reference) {

        final BatchRequest br = new BatchRequest();

        br.setRequestId(requestId);
        br.setRelativeUrl("clients/$.clientId?command=activate");
        br.setReference(reference);
        br.setMethod("POST");
        br.setBody("{\"locale\": \"en\", \"dateFormat\": \"dd MMMM yyyy\", \"activationDate\": \"01 March 2011\"}");

        return br;
    }

    /**
     * Creates and returns a {@link org.apache.fineract.batch.command.internal.ApproveLoanCommandStrategy} Request with
     * given requestId and reference.
     *
     *
     * @param requestId
     *            the request ID
     * @param reference
     *            the reference ID
     * @return BatchRequest the batch request
     */
    public static BatchRequest approveLoanRequest(final Long requestId, final Long reference) {
        return approveLoanRequest(requestId, reference, LocalDate.now(ZoneId.systemDefault()).minusDays(10));
    }

    /**
     * Creates and returns a {@link org.apache.fineract.batch.command.internal.ApproveLoanCommandStrategy} Request with
     * given requestId and reference.
     *
     *
     * @param requestId
     *            the request ID
     * @param reference
     *            the reference ID
     * @param date
     *            the approved on date
     * @return BatchRequest the batch request
     */
    public static BatchRequest approveLoanRequest(final Long requestId, final Long reference, LocalDate date) {
        final BatchRequest br = new BatchRequest();

        br.setRequestId(requestId);
        br.setRelativeUrl("loans/$.loanId?command=approve");
        br.setReference(reference);
        br.setMethod("POST");
        String dateString = date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        br.setBody("{\"locale\": \"en\", \"dateFormat\": \"dd MMMM yyyy\", \"approvedOnDate\": \"" + dateString + "\","
                + "\"note\": \"Loan approval note\"}");

        return br;
    }

    /**
     * Creates and returns a {@link org.apache.fineract.batch.command.internal.DisburseLoanCommandStrategy} Request with
     * given requestId and reference.
     *
     *
     * @param requestId
     *            the request ID
     * @param reference
     *            the reference ID
     * @return BatchRequest the batch request
     */
    public static BatchRequest disburseLoanRequest(final Long requestId, final Long reference) {
        return disburseLoanRequest(requestId, reference, LocalDate.now(ZoneId.systemDefault()).minusDays(8));
    }

    /**
     * Creates and returns a {@link org.apache.fineract.batch.command.internal.DisburseLoanCommandStrategy} Request with
     * given requestId and reference.
     *
     *
     * @param requestId
     *            the request ID
     * @param reference
     *            the reference ID
     * @param date
     *            the actual disbursement date
     * @return BatchRequest the batch request
     */
    public static BatchRequest disburseLoanRequest(final Long requestId, final Long reference, final LocalDate date) {
        final BatchRequest br = new BatchRequest();

        br.setRequestId(requestId);
        br.setRelativeUrl("loans/$.loanId?command=disburse");
        br.setReference(reference);
        br.setMethod("POST");
        String dateString = date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        br.setBody("{\"locale\": \"en\", \"dateFormat\": \"dd MMMM yyyy\", \"actualDisbursementDate\": \"" + dateString + "\"}");

        return br;
    }

    /**
     * Creates and returns a {@link CreateTransactionLoanCommandStrategy} Request with given requestId.
     *
     * @param requestId
     *            the request ID
     * @param reference
     *            the reference ID
     * @param amount
     *            the amount
     * @return BatchRequest the batch request
     */
    public static BatchRequest repayLoanRequest(final Long requestId, final Long reference, final String amount) {
        return createTransactionRequest(requestId, reference, "repayment", amount, LocalDate.now(ZoneId.systemDefault()));
    }

    /**
     * Creates and returns a {@link CreateTransactionLoanCommandStrategy} Request with given requestId.
     *
     * @param requestId
     *            the request ID
     * @param reference
     *            the reference ID
     * @param amount
     *            the amount
     * @param date
     *            the transaction date
     * @return BatchRequest the batch request
     */
    public static BatchRequest createTransactionRequest(final Long requestId, final Long reference, final String transactionCommand,
            final String amount, final LocalDate date) {
        final BatchRequest br = new BatchRequest();

        br.setRequestId(requestId);
        br.setReference(reference);
        br.setRelativeUrl(String.format("loans/$.loanId/transactions?command=%s", transactionCommand));
        br.setMethod("POST");
        String dateString = date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        br.setBody(String.format(
                "{\"locale\": \"en\", \"dateFormat\": \"dd MMMM yyyy\", " + "\"transactionDate\": \"%s\",  \"transactionAmount\": %s}",
                dateString, amount));

        return br;
    }

    /**
     * Creates and returns a {@link CreateTransactionLoanCommandStrategy} request with given request ID.
     *
     *
     * @param requestId
     *            the request ID
     * @param reference
     *            the reference
     * @param amount
     *            the amount
     * @return BatchRequest the created {@link BatchRequest}
     */
    public static BatchRequest creditBalanceRefundRequest(final Long requestId, final Long reference, final String amount) {
        return createTransactionRequest(requestId, reference, "creditBalanceRefund", amount, LocalDate.now(ZoneId.systemDefault()));
    }

    /**
     * Creates and returns a {@link CreateTransactionLoanCommandStrategy} request with given request ID for goodwill
     * credit transaction.
     *
     *
     * @param requestId
     *            the request ID
     * @param reference
     *            the reference
     * @param amount
     *            the amount
     * @return BatchRequest the created {@link BatchRequest}
     */
    public static BatchRequest goodwillCreditRequest(final Long requestId, final Long reference, final String amount) {
        return createTransactionRequest(requestId, reference, "goodwillCredit", amount, LocalDate.now(ZoneId.systemDefault()));
    }

    /**
     * Creates and returns a {@link CreateTransactionLoanCommandStrategy} request with given request ID for merchant
     * issued refund transaction.
     *
     *
     * @param requestId
     *            the request ID
     * @param reference
     *            the reference
     * @param amount
     *            the amount
     * @return BatchRequest the created {@link BatchRequest}
     */
    public static BatchRequest merchantIssuedRefundRequest(final Long requestId, final Long reference, final String amount) {
        return createTransactionRequest(requestId, reference, "merchantIssuedRefund", amount, LocalDate.now(ZoneId.systemDefault()));
    }

    /**
     * Creates and returns a {@link CreateTransactionLoanCommandStrategy} request with given request ID for payout
     * refund transaction.
     *
     *
     * @param requestId
     *            the request ID
     * @param reference
     *            the reference
     * @param amount
     *            the amount
     * @return BatchRequest the created {@link BatchRequest}
     */
    public static BatchRequest payoutRefundRequest(final Long requestId, final Long reference, final String amount) {
        return createTransactionRequest(requestId, reference, "payoutRefund", amount, LocalDate.now(ZoneId.systemDefault()));
    }

    /**
     * Creates and returns a
     * {@link org.apache.fineract.batch.command.internal.CreateLoanRescheduleRequestCommandStrategy} request with given
     * request ID.
     *
     *
     * @param requestId
     *            the request ID
     * @param reference
     *            teh reference
     * @param rescheduleFromDate
     *            the reschedule from date
     * @param rescheduleReasonId
     *            the reschedule reason code value id
     *
     * @return BatchRequest the created {@link BatchRequest}
     */
    public static BatchRequest createRescheduleLoanRequest(final Long requestId, final Long reference, final LocalDate rescheduleFromDate,
            final Integer rescheduleReasonId) {
        final BatchRequest br = new BatchRequest();

        br.setRequestId(requestId);
        br.setReference(reference);
        br.setRelativeUrl("rescheduleloans");
        br.setMethod("POST");
        final LocalDate today = LocalDate.now(ZoneId.systemDefault());
        final LocalDate adjustedDueDate = LocalDate.now(ZoneId.systemDefault()).plusDays(40);
        final String submittedOnDate = today.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        final String rescheduleFromDateString = rescheduleFromDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        final String adjustedDueDateString = adjustedDueDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        br.setBody(String.format("{\"locale\": \"en\", \"dateFormat\": \"dd MMMM yyyy\", "
                + "\"submittedOnDate\": \"%s\",  \"rescheduleFromDate\": \"%s\", \"rescheduleReasonId\": %d, \"adjustedDueDate\": \"%s\", \"loanId\": \"$.loanId\"}",
                submittedOnDate, rescheduleFromDateString, rescheduleReasonId, adjustedDueDateString));

        return br;
    }

    /**
     * Creates and returns a {@link org.apache.fineract.batch.command.internal.ApproveLoanRescheduleCommandStrategy}
     * request with given request ID.
     *
     *
     * @param requestId
     *            the request ID
     * @param reference
     *            teh reference
     * @return BatchRequest the created {@link BatchRequest}
     */
    public static BatchRequest approveRescheduleLoanRequest(final Long requestId, final Long reference) {
        final BatchRequest br = new BatchRequest();

        br.setRequestId(requestId);
        br.setReference(reference);
        br.setRelativeUrl("rescheduleloans/$.resourceId?command=approve");
        br.setMethod("POST");
        final LocalDate approvedOnDate = LocalDate.now(ZoneId.systemDefault());
        final String approvedOnDateString = approvedOnDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        br.setBody(String.format("{\"locale\": \"en\", \"dateFormat\": \"dd MMMM yyyy\", " + "\"approvedOnDate\": \"%s\"}",
                approvedOnDateString));

        return br;
    }

    /**
     * Checks that the client with given externalId is not created on the server.
     *
     * @param requestSpec
     * @param responseSpec
     * @param externalId
     */
    public static void verifyClientCreatedOnServer(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final String externalId) {
        LOG.info("------------------------------CHECK CLIENT DETAILS------------------------------------\n");
        final String CLIENT_URL = "/fineract-provider/api/v1/clients?externalId=" + externalId + "&" + Utils.TENANT_IDENTIFIER;
        final Integer responseRecords = Utils.performServerGet(requestSpec, responseSpec, CLIENT_URL, "totalFilteredRecords");
        Assertions.assertEquals((long) 0, (long) responseRecords, "No records found with given externalId");
    }

    /**
     * Creates and returns a {@link org.apache.fineract.batch.command.internal.GetTransactionByIdCommandStrategy}
     * request with given requestId and reference.
     *
     * @param requestId
     *            the request id
     * @param reference
     *            the reference
     * @return the {@link BatchRequest}
     */
    public static BatchRequest getTransactionByIdRequest(final Long requestId, final Long reference) {

        final BatchRequest br = new BatchRequest();
        String relativeUrl = "loans/$.loanId/transactions/$.resourceId";

        br.setRequestId(requestId);
        br.setRelativeUrl(relativeUrl);
        br.setMethod(HttpMethod.GET);
        br.setReference(reference);
        br.setBody("{}");

        return br;
    }

    /**
     * Creates and returns a {@link org.apache.fineract.batch.command.internal.GetLoanByIdCommandStrategy} request with
     * given requestId and reference.
     *
     * @param requestId
     *            the request id
     * @param reference
     *            the reference
     * @param queryParameter
     *            the query parameters
     * @return the {@link BatchRequest}
     */
    public static BatchRequest getLoanByIdRequest(final Long requestId, final Long reference, final String queryParameter) {

        final BatchRequest br = new BatchRequest();
        String relativeUrl = "loans/$.loanId";
        if (queryParameter != null) {
            relativeUrl = relativeUrl + "?" + queryParameter;
        }

        br.setRequestId(requestId);
        br.setRelativeUrl(relativeUrl);
        br.setMethod(HttpMethod.GET);
        br.setReference(reference);
        br.setBody("{}");

        return br;
    }

    /**
     * Creates and returns a {@link org.apache.fineract.batch.command.internal.GetLoanByIdCommandStrategy} request with
     * given loan id and query param.
     *
     * @param loanId
     *            the loan id
     * @param queryParameter
     *            the query parameters
     * @return the {@link BatchRequest}
     */
    public static BatchRequest getLoanByIdRequest(final Long loanId, final String queryParameter) {
        final BatchRequest br = new BatchRequest();
        String relativeUrl = String.format("loans/%s", loanId);
        if (queryParameter != null) {
            relativeUrl = relativeUrl + "?" + queryParameter;
        }

        br.setRequestId(4567L);
        br.setRelativeUrl(relativeUrl);
        br.setMethod(HttpMethod.GET);
        br.setBody("{}");

        return br;
    }

    /**
     * Creates and returns a batch request to get datatable entry.
     *
     * @param loanId
     *            the loan id
     * @param datatableName
     *            the name of datatable
     * @param queryParameter
     *            the query parameters
     * @param referenceId
     *            the reference id
     * @return the {@link BatchRequest}
     */
    public static BatchRequest getDatatableByIdRequest(final Long loanId, final String datatableName, final String queryParameter,
            final Long referenceId) {
        final BatchRequest br = new BatchRequest();
        String relativeUrl = String.format("datatables/%s/%s", datatableName, loanId);
        if (queryParameter != null) {
            relativeUrl = relativeUrl + "?" + queryParameter;
        }

        br.setRequestId(4571L);
        br.setReference(referenceId);
        br.setRelativeUrl(relativeUrl);
        br.setMethod(HttpMethod.GET);
        br.setBody("{}");

        return br;
    }

    /**
     * Creates and returns a batch request to create datatable entry.
     *
     * @param loanId
     *            the loan id
     * @param datatableName
     *            the name of datatable
     * @param columnNames
     *            the column names
     * @return the {@link BatchRequest}
     */
    public static BatchRequest createDatatableEntryRequest(final Long loanId, final String datatableName, final List<String> columnNames) {
        final BatchRequest br = new BatchRequest();
        final String relativeUrl = String.format("datatables/%s/%s", datatableName, loanId);
        final Map<String, Object> datatableEntryMap = new HashMap<>();
        datatableEntryMap.putAll(columnNames.stream().collect(Collectors.toMap(v -> v, v -> Utils.randomNameGenerator("VAL_", 3))));
        final String datatableEntryRequestJsonString = new Gson().toJson(datatableEntryMap);
        LOG.info("CreateDataTableEntry map : {}", datatableEntryRequestJsonString);

        br.setRequestId(4569L);
        br.setRelativeUrl(relativeUrl);
        br.setMethod(HttpMethod.POST);
        br.setBody(datatableEntryRequestJsonString);

        return br;
    }

    /**
     * Creates and returns a batch request to create datatable entry.
     *
     * @param loanId
     *            the loan id
     * @param datatableName
     *            the name of datatable
     * @param datatableEntryId
     *            the resource id of the datatable entry
     * @param columnNames
     *            the column names
     * @return the {@link BatchRequest}
     */
    public static BatchRequest updateDatatableEntryByEntryIdRequest(final Long loanId, final String datatableName,
            final Long datatableEntryId, final List<String> columnNames) {
        final BatchRequest br = new BatchRequest();
        final String relativeUrl = String.format("datatables/%s/%s/%s", datatableName, loanId, datatableEntryId);
        final Map<String, Object> datatableEntryMap = new HashMap<>();
        datatableEntryMap.putAll(columnNames.stream().collect(Collectors.toMap(v -> v, v -> Utils.randomNameGenerator("VAL_", 3))));
        final String datatableEntryRequestJsonString = new Gson().toJson(datatableEntryMap);
        LOG.info("UpdateDataTableEntry map : {}", datatableEntryRequestJsonString);

        br.setRequestId(4570L);
        br.setReference(4569L);
        br.setRelativeUrl(relativeUrl);
        br.setMethod(HttpMethod.PUT);
        br.setBody(datatableEntryRequestJsonString);

        return br;
    }

    public static BatchRequest createAdjustTransactionRequest(final Long requestId, final Long reference, final String amount,
            final LocalDate date) {
        final BatchRequest br = new BatchRequest();

        br.setRequestId(requestId);
        br.setReference(reference);
        br.setRelativeUrl("loans/$.loanId/transactions/$.resourceId");
        br.setMethod("POST");
        String dateString = date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        br.setBody(String.format(
                "{\"locale\": \"en\", \"dateFormat\": \"dd MMMM yyyy\", " + "\"transactionDate\": \"%s\",  \"transactionAmount\": %s}",
                dateString, amount));

        return br;

    }
}
