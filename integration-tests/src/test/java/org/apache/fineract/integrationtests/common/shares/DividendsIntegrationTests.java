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
package org.apache.fineract.integrationtests.common.shares;

import com.google.gson.Gson;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.savings.SavingsAccountHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DividendsIntegrationTests {

    private final String[] dates = { "01 Jan 2015", "01 Apr 2015", "01 Oct 2015", "01 Dec 2015", "01 Mar 2016" };
    private final String[] shares = { "100", "200", "300", "100", "500" };

    private RequestSpecification requestSpec;
    private ResponseSpecification responseSpec;

    @BeforeEach
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateDividends() {
        DateFormat simple = new SimpleDateFormat("dd MMM yyyy");
        final Integer productId = createShareProduct();
        ArrayList<Integer> shareAccounts = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
            Assertions.assertNotNull(clientId);
            Integer savingsAccountId = SavingsAccountHelper.openSavingsAccount(requestSpec, responseSpec, clientId, "1000");
            Assertions.assertNotNull(savingsAccountId);
            final Integer shareAccountId = createShareAccount(clientId, productId, savingsAccountId, dates[i], shares[i]);
            shareAccounts.add(shareAccountId);
            Assertions.assertNotNull(shareAccountId);
            Map<String, Object> shareAccountData = ShareAccountTransactionHelper.retrieveShareAccount(shareAccountId, requestSpec,
                    responseSpec);
            Assertions.assertNotNull(shareAccountData);
            // Approve share Account
            Map<String, Object> approveMap = new HashMap<>();
            approveMap.put("note", "Share Account Approval Note");
            approveMap.put("dateFormat", "dd MMMM yyyy");
            approveMap.put("approvedDate", "01 Jan 2016");
            approveMap.put("locale", "en");
            String approve = new Gson().toJson(approveMap);
            ShareAccountTransactionHelper.postCommand("approve", shareAccountId, approve, requestSpec, responseSpec);
            // Activate Share Account
            Map<String, Object> activateMap = new HashMap<>();
            activateMap.put("dateFormat", "dd MMMM yyyy");
            activateMap.put("activatedDate", "01 Jan 2016");
            activateMap.put("locale", "en");
            String activateJson = new Gson().toJson(activateMap);
            ShareAccountTransactionHelper.postCommand("activate", shareAccountId, activateJson, requestSpec, responseSpec);
        }

        Map<String, Object> dividendsMap = new HashMap<>();
        dividendsMap.put("dividendPeriodStartDate", "01 Jan 2015");
        dividendsMap.put("dividendPeriodEndDate", "01 Apr 2016");
        dividendsMap.put("dividendAmount", "50000");
        dividendsMap.put("dateFormat", "dd MMMM yyyy");
        dividendsMap.put("locale", "en");
        String createDividendsJson = new Gson().toJson(dividendsMap);
        final Integer dividendId = ShareDividendsTransactionHelper.createShareProductDividends(productId, createDividendsJson, requestSpec,
                responseSpec);

        Map<String, Object> productdividends = ShareDividendsTransactionHelper.retrieveAllDividends(productId, requestSpec, responseSpec);
        Assertions.assertEquals("1", String.valueOf(productdividends.get("totalFilteredRecords")));
        Map<String, Object> dividend = ((List<Map<String, Object>>) productdividends.get("pageItems")).get(0);
        Assertions.assertEquals("50000.0", String.valueOf(dividend.get("amount")));
        Map<String, Object> status = (Map<String, Object>) dividend.get("status");
        Assertions.assertEquals("shareAccountDividendStatusType.initiated", String.valueOf(status.get("code")));
        List<Integer> startdateList = (List<Integer>) dividend.get("dividendPeriodStartDate");
        Calendar cal = Calendar.getInstance();
        cal.set(startdateList.get(0), startdateList.get(1) - 1, startdateList.get(2));
        Date startDate = cal.getTime();
        Assertions.assertEquals("01 Jan 2015", simple.format(startDate));
        List<Integer> enddateList = (List<Integer>) dividend.get("dividendPeriodEndDate");
        cal = Calendar.getInstance();
        cal.set(enddateList.get(0), enddateList.get(1) - 1, enddateList.get(2));
        Date endDate = cal.getTime();
        Assertions.assertEquals("01 Apr 2016", simple.format(endDate));

        Map<String, Object> dividenddetails = ShareDividendsTransactionHelper.retrieveDividendDetails(productId, dividendId, requestSpec,
                responseSpec);
        Assertions.assertEquals("5", String.valueOf(dividenddetails.get("totalFilteredRecords")));
        List<Map<String, Object>> pageItems = (List<Map<String, Object>>) dividenddetails.get("pageItems");
        for (Map<String, Object> dividendData : pageItems) {
            Map<String, Object> accountData = (Map<String, Object>) dividendData.get("accountData");
            String accountId = String.valueOf(accountData.get("id"));
            if (String.valueOf(shareAccounts.get(0)).equals(accountId)) {
                Assertions.assertEquals("11320.755", String.valueOf(dividendData.get("amount")));
            } else if (String.valueOf(shareAccounts.get(1)).equals(accountId)) {
                Assertions.assertEquals("18172.791", String.valueOf(dividendData.get("amount")));
            } else if (String.valueOf(shareAccounts.get(2)).equals(accountId)) {
                Assertions.assertEquals("13629.593", String.valueOf(dividendData.get("amount")));
            } else if (String.valueOf(shareAccounts.get(3)).equals(accountId)) {
                Assertions.assertEquals("3028.7983", String.valueOf(dividendData.get("amount")));
            } else if (String.valueOf(shareAccounts.get(4)).equals(accountId)) {
                Assertions.assertEquals("3848.0637", String.valueOf(dividendData.get("amount")));
            }
            Map<String, Object> statusMap = (Map<String, Object>) dividendData.get("status");
            Assertions.assertEquals("shareAccountDividendStatusType.initiated", String.valueOf(statusMap.get("code")));
        }

        String jsonString = "";
        ShareDividendsTransactionHelper.postCommand("approve", productId, dividendId, jsonString, requestSpec, responseSpec);

        productdividends = ShareDividendsTransactionHelper.retrieveAllDividends(productId, requestSpec, responseSpec);
        Assertions.assertEquals("1", String.valueOf(productdividends.get("totalFilteredRecords")));
        dividend = ((List<Map<String, Object>>) productdividends.get("pageItems")).get(0);
        Assertions.assertEquals("50000.0", String.valueOf(dividend.get("amount")));
        status = (Map<String, Object>) dividend.get("status");
        Assertions.assertEquals("shareAccountDividendStatusType.approved", String.valueOf(status.get("code")));
        startdateList = (List<Integer>) dividend.get("dividendPeriodStartDate");
        cal = Calendar.getInstance();
        cal.set(startdateList.get(0), startdateList.get(1) - 1, startdateList.get(2));
        startDate = cal.getTime();
        Assertions.assertEquals("01 Jan 2015", simple.format(startDate));
        enddateList = (List<Integer>) dividend.get("dividendPeriodEndDate");
        cal = Calendar.getInstance();
        cal.set(enddateList.get(0), enddateList.get(1) - 1, enddateList.get(2));
        endDate = cal.getTime();
        Assertions.assertEquals("01 Apr 2016", simple.format(endDate));

        dividenddetails = ShareDividendsTransactionHelper.retrieveDividendDetails(productId, dividendId, requestSpec, responseSpec);
        Assertions.assertEquals("5", String.valueOf(dividenddetails.get("totalFilteredRecords")));
        pageItems = (List<Map<String, Object>>) dividenddetails.get("pageItems");
        for (Map<String, Object> dividendData : pageItems) {
            Map<String, Object> accountData = (Map<String, Object>) dividendData.get("accountData");
            String accountId = String.valueOf(accountData.get("id"));
            if (String.valueOf(shareAccounts.get(0)).equals(accountId)) {
                Assertions.assertEquals("11320.755", String.valueOf(dividendData.get("amount")));
            } else if (String.valueOf(shareAccounts.get(1)).equals(accountId)) {
                Assertions.assertEquals("18172.791", String.valueOf(dividendData.get("amount")));
            } else if (String.valueOf(shareAccounts.get(2)).equals(accountId)) {
                Assertions.assertEquals("13629.593", String.valueOf(dividendData.get("amount")));
            } else if (String.valueOf(shareAccounts.get(3)).equals(accountId)) {
                Assertions.assertEquals("3028.7983", String.valueOf(dividendData.get("amount")));
            } else if (String.valueOf(shareAccounts.get(4)).equals(accountId)) {
                Assertions.assertEquals("3848.0637", String.valueOf(dividendData.get("amount")));
            }
            Map<String, Object> statusMap = (Map<String, Object>) dividendData.get("status");
            Assertions.assertEquals("shareAccountDividendStatusType.initiated", String.valueOf(statusMap.get("code")));
        }

    }

    private Integer createShareProduct() {
        String shareProductJson = new ShareProductHelper().build();
        return ShareProductTransactionHelper.createShareProduct(shareProductJson, requestSpec, responseSpec);
    }

    private Integer createShareAccount(final Integer clientId, final Integer productId, final Integer savingsAccountId,
            String applicationDate, String requestedShares) {
        String josn = new ShareAccountHelper().withClientId(String.valueOf(clientId)).withProductId(String.valueOf(productId))
                .withExternalId("External1").withSavingsAccountId(String.valueOf(savingsAccountId)).withSubmittedDate("01 Jan 2016")
                .withApplicationDate(applicationDate).withRequestedShares(requestedShares).build();
        return ShareAccountTransactionHelper.createShareAccount(josn, requestSpec, responseSpec);
    }
}
