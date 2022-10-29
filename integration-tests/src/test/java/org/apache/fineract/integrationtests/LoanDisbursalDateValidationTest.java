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

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.CollateralManagementHelper;
import org.apache.fineract.integrationtests.common.CommonConstants;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.loans.LoanApplicationTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanProductTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanStatusChecker;
import org.apache.fineract.integrationtests.common.loans.LoanTransactionHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("rawtypes")
public class LoanDisbursalDateValidationTest {

    private static final Logger LOG = LoggerFactory.getLogger(LoanDisbursalDateValidationTest.class);
    private ResponseSpecification responseSpec;
    private RequestSpecification requestSpec;
    private LoanTransactionHelper loanTransactionHelper;
    private ResponseSpecification responseForbiddenError;

    @BeforeEach
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        this.loanTransactionHelper = new LoanTransactionHelper(this.requestSpec, this.responseSpec);
        this.responseForbiddenError = new ResponseSpecBuilder().expectStatusCode(403).build();
    }

    @Test
    public void loanApplicationValidateDisbursalDate() {

        final String proposedAmount = "5000";
        final String approveDate = "01 March 2014";
        final String disbursalDate = "02 March 2014";

        // CREATE CLIENT
        final Integer clientID = ClientHelper.createClient(this.requestSpec, this.responseSpec, "01 January 2014");
        LOG.info("---------------------------------CLIENT CREATED WITH ID--------------------------------------------------- {}", clientID);

        // CREATE LOAN PRODUCT
        final Integer loanProductID = this.loanTransactionHelper
                .getLoanProductId(new LoanProductTestBuilder().withSyncExpectedWithDisbursementDate(true).build(null));
        LOG.info("----------------------------------LOAN PRODUCT CREATED WITH ID------------------------------------------- {}",
                loanProductID);

        // APPLY FOR LOAN
        final Integer loanID = applyForLoanApplication(clientID, loanProductID, proposedAmount);
        LOG.info("-----------------------------------LOAN CREATED WITH LOANID------------------------------------------------- {}", loanID);
        HashMap loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(this.requestSpec, this.responseSpec, loanID);

        // VALIDATE THE LOAN STATUS
        LoanStatusChecker.verifyLoanIsPending(loanStatusHashMap);

        LOG.info("-----------------------------------APPROVE LOAN-----------------------------------------------------------");
        loanStatusHashMap = this.loanTransactionHelper.approveLoan(approveDate, loanID);

        // VALIDATE THE LOAN IS APPROVED
        LoanStatusChecker.verifyLoanIsApproved(loanStatusHashMap);
        LoanStatusChecker.verifyLoanIsWaitingForDisbursal(loanStatusHashMap);

        // DISBURSE A LOAN
        String loanDetails = this.loanTransactionHelper.getLoanDetails(this.requestSpec, this.responseSpec, loanID);
        @SuppressWarnings("unchecked")
        List<HashMap> disbursalError = (List<HashMap>) this.loanTransactionHelper.disburseLoanWithNetDisbursalAmount(disbursalDate, loanID,
                this.responseForbiddenError, JsonPath.from(loanDetails).get("netDisbursalAmount").toString());

        Assertions.assertEquals(disbursalError.get(0).get(CommonConstants.RESPONSE_ERROR_MESSAGE_CODE),
                "error.msg.actual.disbursement.date.does.not.match.with.expected.disbursal.date");

    }

    private Integer applyForLoanApplication(final Integer clientID, final Integer loanProductID, final String proposedAmount) {
        List<HashMap> collaterals = new ArrayList<>();
        final Integer collateralId = CollateralManagementHelper.createCollateralProduct(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(collateralId);
        final Integer clientCollateralId = CollateralManagementHelper.createClientCollateral(this.requestSpec, this.responseSpec,
                clientID.toString(), collateralId);
        Assertions.assertNotNull(clientCollateralId);
        addCollaterals(collaterals, clientCollateralId, BigDecimal.valueOf(1));
        final String loanApplication = new LoanApplicationTestBuilder().withPrincipal(proposedAmount).withLoanTermFrequency("5")
                .withLoanTermFrequencyAsMonths().withNumberOfRepayments("5").withRepaymentEveryAfter("1")
                .withRepaymentFrequencyTypeAsMonths().withInterestRatePerPeriod("2").withExpectedDisbursementDate("01 March 2014")
                .withCollaterals(collaterals).withSubmittedOnDate("26 February 2014")
                .build(clientID.toString(), loanProductID.toString(), null);
        return this.loanTransactionHelper.getLoanId(loanApplication);
    }

    private void addCollaterals(List<HashMap> collaterals, Integer collateralId, BigDecimal quantity) {
        collaterals.add(collaterals(collateralId, quantity));
    }

    private HashMap<String, String> collaterals(Integer collateralId, BigDecimal quantity) {
        HashMap<String, String> collateral = new HashMap<String, String>(2);
        collateral.put("clientCollateralId", collateralId.toString());
        collateral.put("quantity", quantity.toString());
        return collateral;
    }

}
