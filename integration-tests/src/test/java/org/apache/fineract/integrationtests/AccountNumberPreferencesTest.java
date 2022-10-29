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
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.fineract.integrationtests.common.CenterDomain;
import org.apache.fineract.integrationtests.common.CenterHelper;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.CollateralManagementHelper;
import org.apache.fineract.integrationtests.common.CommonConstants;
import org.apache.fineract.integrationtests.common.GroupHelper;
import org.apache.fineract.integrationtests.common.OfficeHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.loans.LoanApplicationTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanProductTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanTransactionHelper;
import org.apache.fineract.integrationtests.common.savings.SavingsAccountHelper;
import org.apache.fineract.integrationtests.common.savings.SavingsProductHelper;
import org.apache.fineract.integrationtests.common.system.AccountNumberPreferencesHelper;
import org.apache.fineract.integrationtests.common.system.CodeHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountNumberPreferencesTest {

    private static final Logger LOG = LoggerFactory.getLogger(AccountNumberPreferencesTest.class);
    private RequestSpecification requestSpec;
    private ResponseSpecification responseSpec;
    private ResponseSpecification responseValidationError;
    private ResponseSpecification responseNotFoundError;
    private ResponseSpecification responseForbiddenError;
    private Integer clientId;
    private Integer loanProductId;
    private Integer loanId;
    private Integer savingsProductId;
    private Integer savingsId;
    private final String loanPrincipalAmount = "100000.00";
    private final String numberOfRepayments = "12";
    private final String interestRatePerPeriod = "18";
    private final String dateString = "04 September 2014";
    private final String minBalanceForInterestCalculation = null;
    private final String minRequiredBalance = null;
    private final String enforceMinRequiredBalance = "false";
    private LoanTransactionHelper loanTransactionHelper;
    private SavingsAccountHelper savingsAccountHelper;
    private AccountNumberPreferencesHelper accountNumberPreferencesHelper;
    private Integer clientAccountNumberPreferenceId;
    private Integer loanAccountNumberPreferenceId;
    private Integer savingsAccountNumberPreferenceId;
    private Integer groupsAccountNumberPreferenceId;
    private Integer centerAccountNumberPreferenceId;
    private static final String MINIMUM_OPENING_BALANCE = "1000.0";
    private static final String ACCOUNT_TYPE_INDIVIDUAL = "INDIVIDUAL";
    private Boolean isAccountPreferenceSetUp = false;
    private Integer clientTypeCodeId;
    private String clientCodeValueName;
    private Integer clientCodeValueId;
    private final String clientTypeName = "CLIENT_TYPE";
    private final String officeName = "OFFICE_NAME";
    private final String loanShortName = "LOAN_PRODUCT_SHORT_NAME";
    private final String savingsShortName = "SAVINGS_PRODUCT_SHORT_NAME";
    private Integer groupID;
    private Integer centerId;
    private String groupAccountNo;

    @BeforeEach
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        this.responseValidationError = new ResponseSpecBuilder().expectStatusCode(400).build();
        this.responseNotFoundError = new ResponseSpecBuilder().expectStatusCode(404).build();
        this.responseForbiddenError = new ResponseSpecBuilder().expectStatusCode(403).build();
        this.loanTransactionHelper = new LoanTransactionHelper(this.requestSpec, this.responseSpec);
        this.accountNumberPreferencesHelper = new AccountNumberPreferencesHelper(this.requestSpec, this.responseSpec);

    }

    @Test
    public void testAccountNumberPreferences() {

        /* Create Loan and Savings Product */
        this.createLoanAndSavingsProduct();

        /* Ensure no account number preferences are present in the system */
        this.deleteAllAccountNumberPreferences();

        /*
         * Validate the default account number generation rules for clients, loans and savings accounts.
         */
        this.validateDefaultAccountNumberGeneration();

        /* Create and Validate account number preferences */
        this.createAccountNumberPreference();

        /*
         * Validate account number preference rules apply to Clients,Loans and Saving Accounts
         */
        this.validateAccountNumberGenerationWithPreferences();

        /* Validate account number preferences Updation */
        this.updateAccountNumberPreference();

        /*
         * Validate account number preference rules apply to Clients,Loans and Saving Accounts after Updation
         */
        this.validateAccountNumberGenerationWithPreferences();

        /* Delete all account number preferences */
        this.deleteAllAccountNumberPreferences();

    }

    private void createLoanAndSavingsProduct() {
        this.createLoanProduct();
        this.createSavingsProduct();
    }

    private void deleteAllAccountNumberPreferences() {
        ArrayList<HashMap<String, Object>> preferenceIds = this.accountNumberPreferencesHelper.getAllAccountNumberPreferences();
        /* Deletion of valid account preference ID */
        for (HashMap<String, Object> preferenceId : preferenceIds) {
            Integer id = (Integer) preferenceId.get("id");
            HashMap<String, Object> delResponse = this.accountNumberPreferencesHelper.deleteAccountNumberPreference(id, this.responseSpec,
                    "");
            LOG.info("Successfully deleted account number preference (ID: {} )", delResponse.get("resourceId"));
        }
        /* Deletion of invalid account preference ID should fail */
        LOG.info(
                "---------------------------------DELETING ACCOUNT NUMBER PREFERENCE WITH INVALID ID------------------------------------------");

        HashMap<String, Object> deletionError = this.accountNumberPreferencesHelper.deleteAccountNumberPreference(10,
                this.responseNotFoundError, "");
        Assertions.assertEquals("error.msg.resource.not.found", deletionError.get(CommonConstants.RESPONSE_ERROR_MESSAGE_CODE));
    }

    private void validateDefaultAccountNumberGeneration() {
        this.createAndValidateClientEntity(this.isAccountPreferenceSetUp);
        this.createAndValidateLoanEntity(this.isAccountPreferenceSetUp);
        this.createAndValidateSavingsEntity(this.isAccountPreferenceSetUp);
        this.createAndValidateGroup(this.isAccountPreferenceSetUp);
        this.createAndValidateCenter(this.isAccountPreferenceSetUp);
    }

    private void validateAccountNumberGenerationWithPreferences() {
        this.isAccountPreferenceSetUp = true;
        this.createAndValidateClientEntity(this.isAccountPreferenceSetUp);
        this.createAndValidateLoanEntity(this.isAccountPreferenceSetUp);
        this.createAndValidateSavingsEntity(this.isAccountPreferenceSetUp);
        this.createAndValidateGroup(this.isAccountPreferenceSetUp);
        this.createAndValidateCenter(this.isAccountPreferenceSetUp);
    }

    private void createAccountNumberPreference() {
        this.clientAccountNumberPreferenceId = (Integer) this.accountNumberPreferencesHelper
                .createClientAccountNumberPreference(this.responseSpec, "resourceId");
        LOG.info("Successfully created account number preferences for Client (ID: {})", this.clientAccountNumberPreferenceId);

        this.loanAccountNumberPreferenceId = (Integer) this.accountNumberPreferencesHelper
                .createLoanAccountNumberPreference(this.responseSpec, "resourceId");
        LOG.info("Successfully created account number preferences for Loan (ID: {} )", this.loanAccountNumberPreferenceId);

        this.savingsAccountNumberPreferenceId = (Integer) this.accountNumberPreferencesHelper
                .createSavingsAccountNumberPreference(this.responseSpec, "resourceId");
        LOG.info("Successfully created account number preferences for Savings (ID: {})", this.savingsAccountNumberPreferenceId);

        this.groupsAccountNumberPreferenceId = (Integer) this.accountNumberPreferencesHelper
                .createGroupsAccountNumberPreference(this.responseSpec, "resourceId");
        LOG.info("Successfully created account number preferences for Groups (ID: {})", this.groupsAccountNumberPreferenceId);

        this.centerAccountNumberPreferenceId = (Integer) this.accountNumberPreferencesHelper
                .createCenterAccountNumberPreference(this.responseSpec, "resourceId");
        LOG.info("Successfully created account number preferences for Center (ID: {})", this.centerAccountNumberPreferenceId);

        this.accountNumberPreferencesHelper.verifyCreationOfAccountNumberPreferences(this.clientAccountNumberPreferenceId,
                this.loanAccountNumberPreferenceId, this.savingsAccountNumberPreferenceId, this.groupsAccountNumberPreferenceId,
                this.centerAccountNumberPreferenceId, this.responseSpec, this.requestSpec);

        this.createAccountNumberPreferenceInvalidData("1000", "1001");
        this.createAccountNumberPreferenceDuplicateData("1", "101");

    }

    private void createAccountNumberPreferenceDuplicateData(final String accountType, final String prefixType) {
        /* Creating account Preference with duplicate data should fail */
        LOG.info(
                "---------------------------------CREATING ACCOUNT NUMBER PREFERENCE WITH DUPLICATE DATA------------------------------------------");

        HashMap<String, Object> creationError = this.accountNumberPreferencesHelper
                .createAccountNumberPreferenceWithInvalidData(this.responseForbiddenError, accountType, prefixType, "");

        Assertions.assertEquals("error.msg.account.number.format.duplicate.account.type",
                creationError.get(CommonConstants.RESPONSE_ERROR_MESSAGE_CODE));

    }

    private void createAccountNumberPreferenceInvalidData(final String accountType, final String prefixType) {

        /* Creating account Preference with invalid data should fail */
        LOG.info(
                "---------------------------------CREATING ACCOUNT NUMBER PREFERENCE WITH INVALID DATA------------------------------------------");

        HashMap<String, Object> creationError = this.accountNumberPreferencesHelper
                .createAccountNumberPreferenceWithInvalidData(this.responseValidationError, accountType, prefixType, "");

        if (creationError.get(CommonConstants.RESPONSE_ERROR_MESSAGE_CODE)
                .equals("validation.msg.accountNumberFormat.accountType.is.not.within.expected.range")) {
            Assertions.assertEquals("validation.msg.accountNumberFormat.accountType.is.not.within.expected.range",
                    creationError.get(CommonConstants.RESPONSE_ERROR_MESSAGE_CODE));
        } else if (creationError.get(CommonConstants.RESPONSE_ERROR_MESSAGE_CODE)
                .equals("validation.msg.accountNumberFormat.prefixType.is.not.one.of.expected.enumerations")) {
            Assertions.assertEquals("validation.msg.accountNumberFormat.prefixType.is.not.one.of.expected.enumerations",
                    creationError.get(CommonConstants.RESPONSE_ERROR_MESSAGE_CODE));
        }
    }

    private void updateAccountNumberPreference() {
        HashMap<String, Object> accountNumberPreferences = this.accountNumberPreferencesHelper
                .updateAccountNumberPreference(this.clientAccountNumberPreferenceId, "101", this.responseSpec, "");

        LOG.info("--------------------------UPDATION SUCCESSFUL FOR ACCOUNT NUMBER PREFERENCE ID {}",
                accountNumberPreferences.get("resourceId"));

        this.accountNumberPreferencesHelper.verifyUpdationOfAccountNumberPreferences((Integer) accountNumberPreferences.get("resourceId"),
                this.responseSpec, this.requestSpec);

        /* Update invalid account preference id should fail */
        LOG.info(
                "---------------------------------UPDATING ACCOUNT NUMBER PREFERENCE WITH INVALID DATA------------------------------------------");

        /* Invalid Account Type */
        HashMap<String, Object> updationError = this.accountNumberPreferencesHelper.updateAccountNumberPreference(9999, "101",
                this.responseNotFoundError, "");
        if (updationError.get(CommonConstants.RESPONSE_ERROR_MESSAGE_CODE).equals("error.msg.resource.not.found")) {
            Assertions.assertEquals("error.msg.resource.not.found", updationError.get(CommonConstants.RESPONSE_ERROR_MESSAGE_CODE));
        }
        /* Invalid Prefix Type */
        HashMap<String, Object> updationError1 = this.accountNumberPreferencesHelper
                .updateAccountNumberPreference(this.clientAccountNumberPreferenceId, "103", this.responseValidationError, "");

        Assertions.assertEquals("validation.msg.validation.errors.exist", updationError1.get(CommonConstants.RESPONSE_ERROR_MESSAGE_CODE));

    }

    private void createAndValidateClientEntity(Boolean isAccountPreferenceSetUp) {
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        if (isAccountPreferenceSetUp) {
            this.createAndValidateClientBasedOnAccountPreference();
        } else {
            this.createAndValidateClientWithoutAccountPreference();
        }
    }

    private void createAndValidateGroup(Boolean isAccountPreferenceSetUp) {
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        this.groupID = GroupHelper.createGroup(this.requestSpec, this.responseSpec);
        GroupHelper.verifyGroupCreatedOnServer(this.requestSpec, this.responseSpec, groupID);

        this.groupID = GroupHelper.activateGroup(this.requestSpec, this.responseSpec, groupID.toString());
        GroupHelper.verifyGroupActivatedOnServer(this.requestSpec, this.responseSpec, groupID, true);

        final String GROUP_URL = "/fineract-provider/api/v1/groups/" + this.groupID + "?" + Utils.TENANT_IDENTIFIER;
        this.groupAccountNo = Utils.performServerGet(requestSpec, responseSpec, GROUP_URL, "accountNo");

        if (isAccountPreferenceSetUp) {
            String groupsPrefixName = (String) this.accountNumberPreferencesHelper
                    .getAccountNumberPreference(this.groupsAccountNumberPreferenceId, "prefixType.value");

            if (groupsPrefixName.equals(this.officeName)) {

                final String groupOfficeName = Utils.performServerGet(requestSpec, responseSpec, GROUP_URL, "officeName");

                this.validateAccountNumberLengthAndStartsWithPrefix(this.groupAccountNo, groupOfficeName);
            }
        } else {
            validateAccountNumberLengthAndStartsWithPrefix(this.groupAccountNo, null);
        }
    }

    private void createAndValidateCenter(Boolean isAccountPreferenceSetUp) {
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        Integer officeId = new OfficeHelper(requestSpec, responseSpec).createOffice("01 July 2007");

        String name = "CenterCreation" + new Timestamp(new java.util.Date().getTime());
        this.centerId = CenterHelper.createCenter(name, officeId, requestSpec, responseSpec);
        CenterDomain center = CenterHelper.retrieveByID(centerId, requestSpec, responseSpec);
        Assertions.assertNotNull(center);
        Assertions.assertTrue(center.getName().equals(name));

        if (isAccountPreferenceSetUp) {
            String centerPrefixName = (String) this.accountNumberPreferencesHelper
                    .getAccountNumberPreference(this.centerAccountNumberPreferenceId, "prefixType.value");
            final String CENTER_URL = "/fineract-provider/api/v1/centers/" + this.centerId + "?" + Utils.TENANT_IDENTIFIER;

            if (centerPrefixName.equals(this.officeName)) {
                final String centerOfficeName = Utils.performServerGet(requestSpec, responseSpec, CENTER_URL, "officeName");
                this.validateAccountNumberLengthAndStartsWithPrefix(center.getAccountNo(), centerOfficeName);
            }
        } else {
            validateAccountNumberLengthAndStartsWithPrefix(center.getAccountNo(), null);
        }
    }

    private void createAndValidateClientWithoutAccountPreference() {
        this.clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(this.clientId);
        String clientAccountNo = (String) ClientHelper.getClient(requestSpec, responseSpec, this.clientId.toString(), "accountNo");
        validateAccountNumberLengthAndStartsWithPrefix(clientAccountNo, null);
    }

    private void createAndValidateClientBasedOnAccountPreference() {
        final String codeName = "ClientType";
        String clientAccountNo = null;
        String clientPrefixName = (String) this.accountNumberPreferencesHelper
                .getAccountNumberPreference(this.clientAccountNumberPreferenceId, "prefixType.value");
        if (clientPrefixName.equals(this.clientTypeName)) {

            /* Retrieve Code id for the Code "ClientType" */
            HashMap<String, Object> code = CodeHelper.getCodeByName(this.requestSpec, this.responseSpec, codeName);
            this.clientTypeCodeId = (Integer) code.get("id");

            /* Retrieve/Create Code Values for the Code "ClientType" */
            HashMap<String, Object> codeValue = CodeHelper.retrieveOrCreateCodeValue(this.clientTypeCodeId, this.requestSpec,
                    this.responseSpec);

            this.clientCodeValueName = (String) codeValue.get("name");
            this.clientCodeValueId = (Integer) codeValue.get("id");

            /* Create Client with Client Type */
            this.clientId = ClientHelper.createClientForAccountPreference(this.requestSpec, this.responseSpec, this.clientCodeValueId,
                    "clientId");
            ClientHelper.verifyClientCreatedOnServer(this.requestSpec, this.responseSpec, this.clientId);

            // Assertions.assertNotNull(clientId);

            clientAccountNo = (String) ClientHelper.getClient(this.requestSpec, this.responseSpec, this.clientId.toString(), "accountNo");
            this.validateAccountNumberLengthAndStartsWithPrefix(clientAccountNo, this.clientCodeValueName);

        } else if (clientPrefixName.equals(this.officeName)) {
            this.clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
            ClientHelper.verifyClientCreatedOnServer(this.requestSpec, this.responseSpec, this.clientId);
            // Assertions.assertNotNull(clientId);
            clientAccountNo = (String) ClientHelper.getClient(requestSpec, responseSpec, this.clientId.toString(), "accountNo");
            String officeName = (String) ClientHelper.getClient(requestSpec, responseSpec, this.clientId.toString(), "officeName");
            this.validateAccountNumberLengthAndStartsWithPrefix(clientAccountNo, officeName);
        }
    }

    private void validateAccountNumberLengthAndStartsWithPrefix(final String accountNumber, String prefix) {
        if (prefix != null) {
            prefix = prefix.substring(0, Math.min(prefix.length(), 10));
            Assertions.assertEquals(accountNumber.length(), prefix.length() + 9);
            Assertions.assertTrue(accountNumber.startsWith(prefix));
        } else {
            Assertions.assertEquals(9, accountNumber.length());
        }
    }

    private void createLoanProduct() {
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();

        LOG.info("---------------------------------CREATING LOAN PRODUCT------------------------------------------");

        final String loanProductJSON = new LoanProductTestBuilder().withPrincipal(loanPrincipalAmount)
                .withNumberOfRepayments(numberOfRepayments).withinterestRatePerPeriod(interestRatePerPeriod)
                .withInterestRateFrequencyTypeAsYear().build(null);

        this.loanProductId = this.loanTransactionHelper.getLoanProductId(loanProductJSON);
        LOG.info("Successfully created loan product  (ID: {} )", this.loanProductId);
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

    private void createAndValidateLoanEntity(Boolean isAccountPreferenceSetUp) {
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();

        LOG.info("---------------------------------NEW LOAN APPLICATION------------------------------------------");
        List<HashMap> collaterals = new ArrayList<>();
        final Integer collateralId = CollateralManagementHelper.createCollateralProduct(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(collateralId);
        final Integer clientCollateralId = CollateralManagementHelper.createClientCollateral(this.requestSpec, this.responseSpec,
                this.clientId.toString(), collateralId);
        Assertions.assertNotNull(clientCollateralId);
        addCollaterals(collaterals, clientCollateralId, BigDecimal.valueOf(1));
        final String loanApplicationJSON = new LoanApplicationTestBuilder().withPrincipal(loanPrincipalAmount)
                .withLoanTermFrequency(numberOfRepayments).withLoanTermFrequencyAsMonths().withNumberOfRepayments(numberOfRepayments)
                .withRepaymentEveryAfter("1").withRepaymentFrequencyTypeAsMonths().withAmortizationTypeAsEqualInstallments()
                .withInterestCalculationPeriodTypeAsDays().withInterestRatePerPeriod(interestRatePerPeriod).withLoanTermFrequencyAsMonths()
                .withSubmittedOnDate(dateString).withExpectedDisbursementDate(dateString).withPrincipalGrace("2").withInterestGrace("2")
                .withCollaterals(collaterals).build(this.clientId.toString(), this.loanProductId.toString(), null);

        LOG.info("Loan Application :{}", loanApplicationJSON);

        this.loanId = this.loanTransactionHelper.getLoanId(loanApplicationJSON);
        String loanAccountNo = (String) this.loanTransactionHelper.getLoanDetail(this.requestSpec, this.responseSpec, this.loanId,
                "accountNo");

        if (isAccountPreferenceSetUp) {
            String loanPrefixName = (String) this.accountNumberPreferencesHelper
                    .getAccountNumberPreference(this.loanAccountNumberPreferenceId, "prefixType.value");
            if (loanPrefixName.equals(this.officeName)) {
                String loanOfficeName = (String) ClientHelper.getClient(requestSpec, responseSpec, this.clientId.toString(), "officeName");
                this.validateAccountNumberLengthAndStartsWithPrefix(loanAccountNo, loanOfficeName);
            } else if (loanPrefixName.equals(this.loanShortName)) {
                String loanShortName = (String) this.loanTransactionHelper.getLoanProductDetail(this.requestSpec, this.responseSpec,
                        this.loanProductId, "shortName");
                this.validateAccountNumberLengthAndStartsWithPrefix(loanAccountNo, loanShortName);
            }
            LOG.info("SUCCESSFULLY CREATED LOAN APPLICATION BASED ON ACCOUNT PREFERENCES (ID: {} )", this.loanId);
        } else {
            this.validateAccountNumberLengthAndStartsWithPrefix(loanAccountNo, null);
            LOG.info("SUCCESSFULLY CREATED LOAN APPLICATION (ID: {} )", loanId);
        }
    }

    private void createSavingsProduct() {
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();

        LOG.info("------------------------------CREATING NEW SAVINGS PRODUCT ---------------------------------------");

        SavingsProductHelper savingsProductHelper = new SavingsProductHelper();

        final String savingsProductJSON = savingsProductHelper
                //
                .withInterestCompoundingPeriodTypeAsDaily()
                //
                .withInterestPostingPeriodTypeAsMonthly()
                //
                .withInterestCalculationPeriodTypeAsDailyBalance()
                //
                .withMinBalanceForInterestCalculation(minBalanceForInterestCalculation)
                //
                .withMinRequiredBalance(minRequiredBalance).withEnforceMinRequiredBalance(enforceMinRequiredBalance)
                .withMinimumOpenningBalance(MINIMUM_OPENING_BALANCE).build();
        this.savingsProductId = SavingsProductHelper.createSavingsProduct(savingsProductJSON, this.requestSpec, this.responseSpec);
        LOG.info("Sucessfully created savings product (ID: {} )", this.savingsProductId);

    }

    private void createAndValidateSavingsEntity(Boolean isAccountPreferenceSetUp) {
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();

        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);

        this.savingsId = this.savingsAccountHelper.applyForSavingsApplication(this.clientId, this.savingsProductId,
                ACCOUNT_TYPE_INDIVIDUAL);

        String savingsAccountNo = (String) this.savingsAccountHelper.getSavingsAccountDetail(this.savingsId, "accountNo");

        if (isAccountPreferenceSetUp) {
            String savingsPrefixName = (String) this.accountNumberPreferencesHelper
                    .getAccountNumberPreference(this.savingsAccountNumberPreferenceId, "prefixType.value");

            if (savingsPrefixName.equals(this.officeName)) {
                String savingsOfficeName = (String) ClientHelper.getClient(requestSpec, responseSpec, this.clientId.toString(),
                        "officeName");
                this.validateAccountNumberLengthAndStartsWithPrefix(savingsAccountNo, savingsOfficeName);
            } else if (savingsPrefixName.equals(this.savingsShortName)) {
                String loanShortName = (String) this.savingsAccountHelper.getSavingsAccountDetail(this.savingsId, "shortName");
                this.validateAccountNumberLengthAndStartsWithPrefix(savingsAccountNo, loanShortName);
            }
            LOG.info("SUCCESSFULLY CREATED SAVINGS APPLICATION BASED ON ACCOUNT PREFERENCES (ID:  {} )", this.loanId);
        } else {
            this.validateAccountNumberLengthAndStartsWithPrefix(savingsAccountNo, null);
            LOG.info("SUCCESSFULLY CREATED SAVINGS APPLICATION (ID:{} )", this.savingsId);
        }
    }
}
