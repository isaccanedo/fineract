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
package org.apache.fineract.integrationtests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.CommonConstants;
import org.apache.fineract.integrationtests.common.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({ "unused" })
public class ClientUndoRejectAndWithdrawalIntegrationTest {

    private static final String CREATE_CLIENT_URL = "/fineract-provider/api/v1/clients?" + Utils.TENANT_IDENTIFIER;
    public static final String DATE_FORMAT = "dd MMMM yyyy";
    private final String submittedOnDate = "submittedOnDate";
    private final String officeId = "officeId";
    private ResponseSpecification responseSpec;
    private RequestSpecification requestSpec;
    private ClientHelper clientHelper;

    @BeforeEach
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.requestSpec.header("Fineract-Platform-TenantId", "default");
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();

        this.clientHelper = new ClientHelper(this.requestSpec, this.responseSpec);
    }

    @Test
    public void clientUndoRejectIntegrationTest() {

        // CREATE CLIENT
        this.clientHelper = new ClientHelper(this.requestSpec, this.responseSpec);
        final Integer clientId = ClientHelper.createClientPending(this.requestSpec, this.responseSpec);
        ClientHelper.verifyClientCreatedOnServer(this.requestSpec, this.responseSpec, clientId);
        // Assertions.assertNotNull(clientId);

        // GET CLIENT STATUS
        HashMap<String, Object> status = ClientHelper.getClientStatus(requestSpec, responseSpec, String.valueOf(clientId));

        ClientStatusChecker.verifyClientPending(status);

        status = this.clientHelper.rejectClient(clientId);
        ClientStatusChecker.verifyClientRejected(status);

        status = this.clientHelper.undoReject(clientId);
        ClientStatusChecker.verifyClientPending(status);

    }

    @Test
    public void testClientUndoRejectWithDateBeforeRejectDate() {
        final ResponseSpecification errorResponse = new ResponseSpecBuilder().expectStatusCode(403).build();
        final ClientHelper validationErrorHelper = new ClientHelper(this.requestSpec, errorResponse);

        // CREATE CLIENT
        this.clientHelper = new ClientHelper(this.requestSpec, this.responseSpec);
        final Integer clientId = ClientHelper.createClientPending(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        // GET CLIENT STATUS
        HashMap<String, Object> status = ClientHelper.getClientStatus(requestSpec, responseSpec, String.valueOf(clientId));

        ClientStatusChecker.verifyClientPending(status);

        status = this.clientHelper.rejectClient(clientId);
        ClientStatusChecker.verifyClientRejected(status);

        ArrayList<HashMap<String, Object>> clientErrorData = validationErrorHelper.undoRejectedclient(clientId,
                CommonConstants.RESPONSE_ERROR, ClientHelper.CREATED_DATE);
        assertEquals("error.msg.client.reopened.date.cannot.before.client.rejected.date",
                clientErrorData.get(0).get(CommonConstants.RESPONSE_ERROR_MESSAGE_CODE));

        status = this.clientHelper.undoReject(clientId);
        ClientStatusChecker.verifyClientPending(status);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testClientUndoRejectWithoutReject() {
        final ResponseSpecification errorResponse = new ResponseSpecBuilder().expectStatusCode(403).build();
        final ClientHelper validationErrorHelper = new ClientHelper(this.requestSpec, errorResponse);

        // CREATE CLIENT
        this.clientHelper = new ClientHelper(this.requestSpec, this.responseSpec);
        final Integer clientId = ClientHelper.createClientPending(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        // GET CLIENT STATUS
        HashMap<String, Object> status = ClientHelper.getClientStatus(requestSpec, responseSpec, String.valueOf(clientId));
        ClientStatusChecker.verifyClientPending(status);

        LocalDate todaysDate = Utils.getLocalDateOfTenant();
        final String undoRejectDate = todaysDate.format(Utils.dateFormatter);

        ArrayList<HashMap<String, Object>> clientErrorData = validationErrorHelper.undoRejectedclient(clientId,
                CommonConstants.RESPONSE_ERROR, undoRejectDate);
        assertEquals("error.msg.client.undorejection.on.nonrejected.account",
                clientErrorData.get(0).get(CommonConstants.RESPONSE_ERROR_MESSAGE_CODE));

        status = ClientHelper.getClientStatus(requestSpec, responseSpec, String.valueOf(clientId));
        ClientStatusChecker.verifyClientPending(status);

    }

    @Test
    public void testClientUndoRejectWithFutureDate() {

        final ResponseSpecification errorResponse = new ResponseSpecBuilder().expectStatusCode(400).build();
        final ClientHelper validationErrorHelper = new ClientHelper(this.requestSpec, errorResponse);

        // CREATE CLIENT
        this.clientHelper = new ClientHelper(this.requestSpec, this.responseSpec);
        final Integer clientId = ClientHelper.createClientPending(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        // GET CLIENT STATUS
        HashMap<String, Object> status = ClientHelper.getClientStatus(requestSpec, responseSpec, String.valueOf(clientId));

        ClientStatusChecker.verifyClientPending(status);

        status = this.clientHelper.rejectClient(clientId);
        ClientStatusChecker.verifyClientRejected(status);
        LocalDate tomorrowsDate = Utils.getLocalDateOfTenant().plusDays(1);
        final String undoRejectDate = tomorrowsDate.format(Utils.dateFormatter);
        ArrayList<HashMap<String, Object>> clientErrorData = validationErrorHelper.undoRejectedclient(clientId,
                CommonConstants.RESPONSE_ERROR, undoRejectDate);
        assertEquals("validation.msg.client.reopenedDate.is.greater.than.date",
                clientErrorData.get(0).get(CommonConstants.RESPONSE_ERROR_MESSAGE_CODE));

        status = this.clientHelper.undoReject(clientId);
        ClientStatusChecker.verifyClientPending(status);

    }

    @Test
    public void clientUndoWithDrawnIntegrationTest() {

        // CREATE CLIENT
        this.clientHelper = new ClientHelper(this.requestSpec, this.responseSpec);
        final Integer clientId = ClientHelper.createClientPending(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        // GET CLIENT STATUS
        HashMap<String, Object> status = ClientHelper.getClientStatus(requestSpec, responseSpec, String.valueOf(clientId));

        ClientStatusChecker.verifyClientPending(status);

        status = this.clientHelper.withdrawClient(clientId);
        ClientStatusChecker.verifyClientWithdrawn(status);

        status = this.clientHelper.undoWithdrawn(clientId);
        ClientStatusChecker.verifyClientPending(status);

    }

    @Test
    public void testClientUndoWithDrawnWithDateBeforeWithdrawal() {

        final ResponseSpecification errorResponse = new ResponseSpecBuilder().expectStatusCode(403).build();
        final ClientHelper validationErrorHelper = new ClientHelper(this.requestSpec, errorResponse);

        // CREATE CLIENT
        this.clientHelper = new ClientHelper(this.requestSpec, this.responseSpec);
        final Integer clientId = ClientHelper.createClientPending(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        // GET CLIENT STATUS
        HashMap<String, Object> status = ClientHelper.getClientStatus(requestSpec, responseSpec, String.valueOf(clientId));

        ClientStatusChecker.verifyClientPending(status);

        status = this.clientHelper.withdrawClient(clientId);
        ClientStatusChecker.verifyClientWithdrawn(status);

        ArrayList<HashMap<String, Object>> clientErrorData = validationErrorHelper.undoWithdrawclient(clientId,
                CommonConstants.RESPONSE_ERROR, ClientHelper.CREATED_DATE);
        assertEquals("error.msg.client.reopened.date.cannot.before.client.withdrawal.date",
                clientErrorData.get(0).get(CommonConstants.RESPONSE_ERROR_MESSAGE_CODE));

        status = this.clientHelper.undoWithdrawn(clientId);
        ClientStatusChecker.verifyClientPending(status);

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testClientUndoWithDrawnWithoutWithdrawal() {
        final ResponseSpecification errorResponse = new ResponseSpecBuilder().expectStatusCode(403).build();
        final ClientHelper validationErrorHelper = new ClientHelper(this.requestSpec, errorResponse);
        // CREATE CLIENT
        this.clientHelper = new ClientHelper(this.requestSpec, this.responseSpec);
        final Integer clientId = ClientHelper.createClientPending(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        // GET CLIENT STATUS
        HashMap<String, Object> status = ClientHelper.getClientStatus(requestSpec, responseSpec, String.valueOf(clientId));

        LocalDate todaysDate = Utils.getLocalDateOfTenant();
        final String undoWithdrawDate = todaysDate.format(Utils.dateFormatter);

        ArrayList<HashMap<String, Object>> clientErrorData = validationErrorHelper.undoWithdrawclient(clientId,
                CommonConstants.RESPONSE_ERROR, undoWithdrawDate);
        assertEquals("error.msg.client.undoWithdrawal.on.nonwithdrawal.account",
                clientErrorData.get(0).get(CommonConstants.RESPONSE_ERROR_MESSAGE_CODE));

        status = ClientHelper.getClientStatus(requestSpec, responseSpec, String.valueOf(clientId));
        ClientStatusChecker.verifyClientPending(status);

    }

    @Test
    public void testClientUndoWithDrawnWithFutureDate() {

        final ResponseSpecification errorResponse = new ResponseSpecBuilder().expectStatusCode(400).build();
        final ClientHelper validationErrorHelper = new ClientHelper(this.requestSpec, errorResponse);

        // CREATE CLIENT
        this.clientHelper = new ClientHelper(this.requestSpec, this.responseSpec);
        final Integer clientId = ClientHelper.createClientPending(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        // GET CLIENT STATUS
        HashMap<String, Object> status = ClientHelper.getClientStatus(requestSpec, responseSpec, String.valueOf(clientId));

        ClientStatusChecker.verifyClientPending(status);

        status = this.clientHelper.withdrawClient(clientId);
        ClientStatusChecker.verifyClientWithdrawn(status);
        LocalDate tomorrowsDate = Utils.getLocalDateOfTenant().plusDays(1);
        final String undoWithdrawDate = tomorrowsDate.format(Utils.dateFormatter);
        ArrayList<HashMap<String, Object>> clientErrorData = validationErrorHelper.undoWithdrawclient(clientId,
                CommonConstants.RESPONSE_ERROR, undoWithdrawDate);
        assertEquals("validation.msg.client.reopenedDate.is.greater.than.date",
                clientErrorData.get(0).get(CommonConstants.RESPONSE_ERROR_MESSAGE_CODE));

        status = this.clientHelper.undoWithdrawn(clientId);
        ClientStatusChecker.verifyClientPending(status);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testValidateReopenedDate() {
        final ResponseSpecification errorResponse = new ResponseSpecBuilder().expectStatusCode(400).build();
        final ClientHelper validationErrorHelper = new ClientHelper(this.requestSpec, errorResponse);

        // CREATE CLIENT
        this.clientHelper = new ClientHelper(this.requestSpec, this.responseSpec);
        final Integer clientId = ClientHelper.createClientPending(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);
        // GET CLIENT STATUS
        HashMap<String, Object> status = ClientHelper.getClientStatus(requestSpec, responseSpec, String.valueOf(clientId));
        ClientStatusChecker.verifyClientPending(status);

        status = this.clientHelper.withdrawClient(clientId);
        ClientStatusChecker.verifyClientWithdrawn(status);
        status = this.clientHelper.undoWithdrawn(clientId);
        ClientStatusChecker.verifyClientPending(status);
        ArrayList<HashMap<String, Object>> clientErrorData = validationErrorHelper.activateClient(clientId, CommonConstants.RESPONSE_ERROR);
        assertEquals("error.msg.clients.submittedOnDate.after.reopened.date",
                clientErrorData.get(0).get(CommonConstants.RESPONSE_ERROR_MESSAGE_CODE));

    }

    @Test
    public void testReopenedDate() {
        final ResponseSpecification errorResponse = new ResponseSpecBuilder().expectStatusCode(400).build();
        // final ClientHelper validationErrorHelper = new
        // ClientHelper(this.requestSpec, errorResponse);

        // CREATE CLIENT
        this.clientHelper = new ClientHelper(this.requestSpec, this.responseSpec);
        final Integer clientId = ClientHelper.createClientPending(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);
        // GET CLIENT STATUS
        HashMap<String, Object> status = ClientHelper.getClientStatus(requestSpec, responseSpec, String.valueOf(clientId));
        ClientStatusChecker.verifyClientPending(status);

        status = this.clientHelper.withdrawClient(clientId);
        ClientStatusChecker.verifyClientWithdrawn(status);
        status = this.clientHelper.undoWithdrawn(clientId);
        ClientStatusChecker.verifyClientPending(status);
        status = this.clientHelper.activateClientWithDiffDateOption(clientId, ClientHelper.CREATED_DATE_PLUS_TWO);

    }

    private static String randomIDGenerator(final String prefix, final int lenOfRandomSuffix) {
        return Utils.randomStringGenerator(prefix, lenOfRandomSuffix, "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

}
