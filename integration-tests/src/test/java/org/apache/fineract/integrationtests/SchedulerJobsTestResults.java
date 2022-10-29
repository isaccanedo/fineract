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

import com.google.common.truth.Truth;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import org.apache.fineract.infrastructure.businessdate.domain.BusinessDateType;
import org.apache.fineract.integrationtests.common.BusinessDateHelper;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.CollateralManagementHelper;
import org.apache.fineract.integrationtests.common.GlobalConfigurationHelper;
import org.apache.fineract.integrationtests.common.HolidayHelper;
import org.apache.fineract.integrationtests.common.SchedulerJobHelper;
import org.apache.fineract.integrationtests.common.StandingInstructionsHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.accounting.Account;
import org.apache.fineract.integrationtests.common.accounting.AccountHelper;
import org.apache.fineract.integrationtests.common.accounting.JournalEntry;
import org.apache.fineract.integrationtests.common.accounting.JournalEntryHelper;
import org.apache.fineract.integrationtests.common.charges.ChargesHelper;
import org.apache.fineract.integrationtests.common.fixeddeposit.FixedDepositAccountHelper;
import org.apache.fineract.integrationtests.common.fixeddeposit.FixedDepositAccountStatusChecker;
import org.apache.fineract.integrationtests.common.fixeddeposit.FixedDepositProductHelper;
import org.apache.fineract.integrationtests.common.loans.LoanApplicationTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanProductTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanStatusChecker;
import org.apache.fineract.integrationtests.common.loans.LoanTransactionHelper;
import org.apache.fineract.integrationtests.common.savings.SavingsAccountHelper;
import org.apache.fineract.integrationtests.common.savings.SavingsProductHelper;
import org.apache.fineract.integrationtests.common.savings.SavingsStatusChecker;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.apache.fineract.portfolio.account.domain.AccountTransferType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@Order(1)
@TestMethodOrder(MethodName.class)
@SuppressWarnings({ "unchecked", "rawtypes" })
public class SchedulerJobsTestResults {

    private static final String FROM_ACCOUNT_TYPE_SAVINGS = "2";
    private static final String TO_ACCOUNT_TYPE_SAVINGS = "2";
    private static final String DATE_OF_JOINING = "01 January 2011";
    private static final String TRANSACTION_DATE = "01 March 2013";
    private static final String ACCOUNT_TYPE_INDIVIDUAL = "INDIVIDUAL";
    private static final String MINIMUM_OPENING_BALANCE = "1000";
    private static final Float SP_BALANCE = Float.valueOf(MINIMUM_OPENING_BALANCE);

    private ResponseSpecification responseSpec;
    private RequestSpecification requestSpec;
    private SchedulerJobHelper schedulerJobHelper;
    private SavingsAccountHelper savingsAccountHelper;
    private LoanTransactionHelper loanTransactionHelper;
    private AccountHelper accountHelper;
    private JournalEntryHelper journalEntryHelper;

    private TimeZone systemTimeZone;

    @BeforeEach
    public void setup() {
        Utils.initializeRESTAssured();
        requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        requestSpec.header("Fineract-Platform-TenantId", "default");
        responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        this.accountHelper = new AccountHelper(requestSpec, responseSpec);
        this.journalEntryHelper = new JournalEntryHelper(requestSpec, responseSpec);
        schedulerJobHelper = new SchedulerJobHelper(requestSpec);

        this.systemTimeZone = TimeZone.getTimeZone(Utils.TENANT_TIME_ZONE);
    }

    @AfterEach
    public void tearDown() {
        GlobalConfigurationHelper.resetAllDefaultGlobalConfigurations(requestSpec, responseSpec);
        GlobalConfigurationHelper.verifyAllDefaultGlobalConfigurations(requestSpec, responseSpec);
    }

    @Test
    public void testApplyAnnualFeeForSavingsJobOutcome() throws InterruptedException {
        this.savingsAccountHelper = new SavingsAccountHelper(requestSpec, responseSpec);

        final Integer clientID = ClientHelper.createClient(requestSpec, responseSpec);
        Assertions.assertNotNull(clientID);

        final Integer savingsProductID = createSavingsProduct(requestSpec, responseSpec,
                ClientSavingsIntegrationTest.MINIMUM_OPENING_BALANCE);
        Assertions.assertNotNull(savingsProductID);

        final Integer savingsId = this.savingsAccountHelper.applyForSavingsApplication(clientID, savingsProductID,
                ClientSavingsIntegrationTest.ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertNotNull(savingsProductID);

        HashMap savingsStatusHashMap = SavingsStatusChecker.getStatusOfSavings(requestSpec, responseSpec, savingsId);
        SavingsStatusChecker.verifySavingsIsPending(savingsStatusHashMap);

        final Integer annualFeeChargeId = ChargesHelper.createCharges(requestSpec, responseSpec, ChargesHelper.getSavingsAnnualFeeJSON());
        Assertions.assertNotNull(annualFeeChargeId);

        this.savingsAccountHelper.addChargesForSavings(savingsId, annualFeeChargeId, true);
        ArrayList<HashMap> chargesPendingState = this.savingsAccountHelper.getSavingsCharges(savingsId);
        Assertions.assertEquals(1, chargesPendingState.size());

        savingsStatusHashMap = this.savingsAccountHelper.approveSavings(savingsId);
        SavingsStatusChecker.verifySavingsIsApproved(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.activateSavings(savingsId);
        SavingsStatusChecker.verifySavingsIsActive(savingsStatusHashMap);

        String JobName = "Apply Annual Fee For Savings";

        this.schedulerJobHelper.executeAndAwaitJob(JobName);

        final HashMap savingsDetails = this.savingsAccountHelper.getSavingsDetails(savingsId);
        final HashMap annualFeeDetails = (HashMap) savingsDetails.get("annualFee");
        ArrayList<Integer> annualFeeDueDateAsArrayList = (ArrayList<Integer>) annualFeeDetails.get("dueDate");
        LocalDate nextDueDateForAnnualFee = LocalDate.of(annualFeeDueDateAsArrayList.get(0), annualFeeDueDateAsArrayList.get(1),
                annualFeeDueDateAsArrayList.get(2));
        LocalDate todaysDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        Truth.assertWithMessage("Verifying that all due Annual Fees have been paid").that(nextDueDateForAnnualFee)
                .isGreaterThan(todaysDate);
    }

    @Test
    public void testInterestPostingForSavingsJobOutcome() throws InterruptedException {
        this.savingsAccountHelper = new SavingsAccountHelper(requestSpec, responseSpec);

        final Integer clientID = ClientHelper.createClient(requestSpec, responseSpec);
        Assertions.assertNotNull(clientID);

        final Integer savingsProductID = createSavingsProduct(requestSpec, responseSpec,
                ClientSavingsIntegrationTest.MINIMUM_OPENING_BALANCE);
        Assertions.assertNotNull(savingsProductID);

        final Integer savingsId = this.savingsAccountHelper.applyForSavingsApplication(clientID, savingsProductID,
                ClientSavingsIntegrationTest.ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertNotNull(savingsProductID);

        HashMap savingsStatusHashMap = SavingsStatusChecker.getStatusOfSavings(requestSpec, responseSpec, savingsId);
        SavingsStatusChecker.verifySavingsIsPending(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.approveSavings(savingsId);
        SavingsStatusChecker.verifySavingsIsApproved(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.activateSavings(savingsId);
        SavingsStatusChecker.verifySavingsIsActive(savingsStatusHashMap);

        final HashMap summaryBefore = this.savingsAccountHelper.getSavingsSummary(savingsId);

        String JobName = "Post Interest For Savings";

        this.schedulerJobHelper.executeAndAwaitJob(JobName);
        final HashMap summaryAfter = this.savingsAccountHelper.getSavingsSummary(savingsId);

        Assertions.assertNotSame(summaryBefore.get("accountBalance"), summaryAfter.get("accountBalance"),
                "Verifying the Balance after running Post Interest for Savings Job");
    }

    @Test
    public void testTransferFeeForLoansFromSavingsJobOutcome() throws InterruptedException {
        this.savingsAccountHelper = new SavingsAccountHelper(requestSpec, responseSpec);
        this.loanTransactionHelper = new LoanTransactionHelper(requestSpec, responseSpec);

        final Integer clientID = ClientHelper.createClient(requestSpec, responseSpec);
        Assertions.assertNotNull(clientID);

        final Integer savingsProductID = createSavingsProduct(requestSpec, responseSpec,
                ClientSavingsIntegrationTest.MINIMUM_OPENING_BALANCE);
        Assertions.assertNotNull(savingsProductID);

        final Integer savingsId = this.savingsAccountHelper.applyForSavingsApplication(clientID, savingsProductID,
                ClientSavingsIntegrationTest.ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertNotNull(savingsProductID);

        HashMap savingsStatusHashMap = SavingsStatusChecker.getStatusOfSavings(requestSpec, responseSpec, savingsId);
        SavingsStatusChecker.verifySavingsIsPending(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.approveSavings(savingsId);
        SavingsStatusChecker.verifySavingsIsApproved(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.activateSavings(savingsId);
        SavingsStatusChecker.verifySavingsIsActive(savingsStatusHashMap);

        final Integer loanProductID = createLoanProduct(null);
        Assertions.assertNotNull(loanProductID);

        final Integer loanID = applyForLoanApplication(clientID.toString(), loanProductID.toString(), savingsId.toString(),
                "10 January 2013");
        Assertions.assertNotNull(loanID);

        HashMap loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(requestSpec, responseSpec, loanID);
        LoanStatusChecker.verifyLoanIsPending(loanStatusHashMap);

        loanStatusHashMap = this.loanTransactionHelper.approveLoan(AccountTransferTest.LOAN_APPROVAL_DATE, loanID);
        LoanStatusChecker.verifyLoanIsApproved(loanStatusHashMap);

        Integer specifiedDueDateChargeId = ChargesHelper.createCharges(requestSpec, responseSpec,
                ChargesHelper.getLoanSpecifiedDueDateWithAccountTransferJSON());
        Assertions.assertNotNull(specifiedDueDateChargeId);

        this.loanTransactionHelper.addChargesForLoan(loanID,
                LoanTransactionHelper.getSpecifiedDueDateChargesForLoanAsJSON(specifiedDueDateChargeId.toString(), "12 March 2013", "100"));
        ArrayList<HashMap> chargesPendingState = this.loanTransactionHelper.getLoanCharges(loanID);
        Assertions.assertEquals(1, chargesPendingState.size());

        String loanDetails = this.loanTransactionHelper.getLoanDetails(requestSpec, responseSpec, loanID);
        loanStatusHashMap = this.loanTransactionHelper.disburseLoanWithNetDisbursalAmount(AccountTransferTest.LOAN_DISBURSAL_DATE, loanID,
                JsonPath.from(loanDetails).get("netDisbursalAmount").toString());
        LoanStatusChecker.verifyLoanIsActive(loanStatusHashMap);
        final HashMap summaryBefore = this.savingsAccountHelper.getSavingsSummary(savingsId);

        String JobName = "Transfer Fee For Loans From Savings";
        this.schedulerJobHelper.executeAndAwaitJob(JobName);
        final HashMap summaryAfter = this.savingsAccountHelper.getSavingsSummary(savingsId);

        final HashMap chargeData = ChargesHelper.getChargeById(requestSpec, responseSpec, specifiedDueDateChargeId);

        Float chargeAmount = (Float) chargeData.get("amount");

        final Float balance = (Float) summaryBefore.get("accountBalance") - chargeAmount;

        Assertions.assertEquals(balance, (Float) summaryAfter.get("accountBalance"),
                "Verifying the Balance after running Transfer Fee for Loans from Savings");
    }

    @Test
    public void testApplyHolidaysToLoansJobOutcome() throws InterruptedException {
        this.loanTransactionHelper = new LoanTransactionHelper(requestSpec, responseSpec);

        final Integer clientID = ClientHelper.createClient(requestSpec, responseSpec);
        Assertions.assertNotNull(clientID);

        Integer holidayId = HolidayHelper.createHolidays(requestSpec, responseSpec);
        Assertions.assertNotNull(holidayId);

        final Integer loanProductID = createLoanProduct(null);
        Assertions.assertNotNull(loanProductID);

        final Integer loanID = applyForLoanApplication(clientID.toString(), loanProductID.toString(), null, "10 January 2013");
        Assertions.assertNotNull(loanID);

        HashMap loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(requestSpec, responseSpec, loanID);
        LoanStatusChecker.verifyLoanIsPending(loanStatusHashMap);

        loanStatusHashMap = this.loanTransactionHelper.approveLoan(AccountTransferTest.LOAN_APPROVAL_DATE, loanID);
        LoanStatusChecker.verifyLoanIsApproved(loanStatusHashMap);

        String loanDetails = this.loanTransactionHelper.getLoanDetails(requestSpec, responseSpec, loanID);
        loanStatusHashMap = this.loanTransactionHelper.disburseLoanWithNetDisbursalAmount(AccountTransferTest.LOAN_DISBURSAL_DATE, loanID,
                JsonPath.from(loanDetails).get("netDisbursalAmount").toString());
        LoanStatusChecker.verifyLoanIsActive(loanStatusHashMap);

        // Retrieving All Global Configuration details
        final ArrayList<HashMap> globalConfig = GlobalConfigurationHelper.getAllGlobalConfigurations(requestSpec, responseSpec);
        Assertions.assertNotNull(globalConfig);

        // Updating Value for reschedule-repayments-on-holidays Global
        // Configuration
        Integer configId = (Integer) globalConfig.get(3).get("id");
        Assertions.assertNotNull(configId);

        HashMap configData = GlobalConfigurationHelper.getGlobalConfigurationById(requestSpec, responseSpec, configId.toString());
        Assertions.assertNotNull(configData);

        Boolean enabled = (Boolean) globalConfig.get(3).get("enabled");

        if (!enabled) {
            enabled = true;
            configId = GlobalConfigurationHelper.updateEnabledFlagForGlobalConfiguration(requestSpec, responseSpec, configId, enabled);
        }

        holidayId = HolidayHelper.activateHolidays(requestSpec, responseSpec, holidayId.toString());
        Assertions.assertNotNull(holidayId);

        String JobName = "Apply Holidays To Loans";

        this.schedulerJobHelper.executeAndAwaitJob(JobName);

        HashMap holidayData = HolidayHelper.getHolidayById(requestSpec, responseSpec, holidayId.toString());
        ArrayList<Integer> repaymentsRescheduledDate = (ArrayList<Integer>) holidayData.get("repaymentsRescheduledTo");

        Assertions.assertEquals(repaymentsRescheduledDate, repaymentsRescheduledDate,
                "Verifying Repayment Rescheduled Date after Running Apply Holidays to Loans Scheduler Job");
    }

    @Test
    public void testApplyDueFeeChargesForSavingsJobOutcome() throws InterruptedException {
        this.savingsAccountHelper = new SavingsAccountHelper(requestSpec, responseSpec);

        final Integer clientID = ClientHelper.createClient(requestSpec, responseSpec);
        Assertions.assertNotNull(clientID);

        final Integer savingsProductID = createSavingsProduct(requestSpec, responseSpec,
                ClientSavingsIntegrationTest.MINIMUM_OPENING_BALANCE);
        Assertions.assertNotNull(savingsProductID);

        final Integer savingsId = this.savingsAccountHelper.applyForSavingsApplication(clientID, savingsProductID,
                ClientSavingsIntegrationTest.ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertNotNull(savingsProductID);

        HashMap savingsStatusHashMap = SavingsStatusChecker.getStatusOfSavings(requestSpec, responseSpec, savingsId);
        SavingsStatusChecker.verifySavingsIsPending(savingsStatusHashMap);

        final Integer specifiedDueDateChargeId = ChargesHelper.createCharges(requestSpec, responseSpec,
                ChargesHelper.getSavingsSpecifiedDueDateJSON());
        Assertions.assertNotNull(specifiedDueDateChargeId);

        this.savingsAccountHelper.addChargesForSavings(savingsId, specifiedDueDateChargeId, true);
        ArrayList<HashMap> chargesPendingState = this.savingsAccountHelper.getSavingsCharges(savingsId);
        Assertions.assertEquals(1, chargesPendingState.size());

        savingsStatusHashMap = this.savingsAccountHelper.approveSavings(savingsId);
        SavingsStatusChecker.verifySavingsIsApproved(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.activateSavings(savingsId);
        SavingsStatusChecker.verifySavingsIsActive(savingsStatusHashMap);

        HashMap summaryBefore = this.savingsAccountHelper.getSavingsSummary(savingsId);

        String JobName = "Pay Due Savings Charges";

        this.schedulerJobHelper.executeAndAwaitJob(JobName);
        HashMap summaryAfter = this.savingsAccountHelper.getSavingsSummary(savingsId);

        final HashMap chargeData = ChargesHelper.getChargeById(requestSpec, responseSpec, specifiedDueDateChargeId);

        Float chargeAmount = (Float) chargeData.get("amount");

        final Float balance = (Float) summaryBefore.get("accountBalance") - chargeAmount;

        Assertions.assertEquals(balance, (Float) summaryAfter.get("accountBalance"),
                "Verifying the Balance after running Pay due Savings Charges");
    }

    @Test
    public void testUpdateAccountingRunningBalancesJobOutcome() {
        this.savingsAccountHelper = new SavingsAccountHelper(requestSpec, responseSpec);

        final Account assetAccount = this.accountHelper.createAssetAccount();
        final Account incomeAccount = this.accountHelper.createIncomeAccount();
        final Account expenseAccount = this.accountHelper.createExpenseAccount();
        final Account liabilityAccount = this.accountHelper.createLiabilityAccount();

        final Integer accountID = assetAccount.getAccountID();

        final Integer savingsProductID = createSavingsProduct(MINIMUM_OPENING_BALANCE, assetAccount, incomeAccount, expenseAccount,
                liabilityAccount);

        final Integer clientID = ClientHelper.createClient(requestSpec, responseSpec, DATE_OF_JOINING);
        final Integer savingsID = this.savingsAccountHelper.applyForSavingsApplication(clientID, savingsProductID, ACCOUNT_TYPE_INDIVIDUAL);

        HashMap savingsStatusHashMap = SavingsStatusChecker.getStatusOfSavings(requestSpec, responseSpec, savingsID);
        SavingsStatusChecker.verifySavingsIsPending(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.approveSavings(savingsID);
        SavingsStatusChecker.verifySavingsIsApproved(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.activateSavings(savingsID);
        SavingsStatusChecker.verifySavingsIsActive(savingsStatusHashMap);

        // Checking initial Account entries.
        final JournalEntry[] assetAccountInitialEntry = { new JournalEntry(SP_BALANCE, JournalEntry.TransactionType.DEBIT) };
        final JournalEntry[] liabilityAccountInitialEntry = { new JournalEntry(SP_BALANCE, JournalEntry.TransactionType.CREDIT) };
        this.journalEntryHelper.checkJournalEntryForAssetAccount(assetAccount, TRANSACTION_DATE, assetAccountInitialEntry);
        this.journalEntryHelper.checkJournalEntryForLiabilityAccount(liabilityAccount, TRANSACTION_DATE, liabilityAccountInitialEntry);

        String JobName = "Update Accounting Running Balances";

        this.schedulerJobHelper.executeAndAwaitJob(JobName);
        final HashMap runningBalanceAfter = this.accountHelper.getAccountingWithRunningBalanceById(accountID.toString());

        final Integer INT_BALANCE = Integer.valueOf(MINIMUM_OPENING_BALANCE);

        Assertions.assertEquals(INT_BALANCE, runningBalanceAfter.get("organizationRunningBalance"),
                "Verifying Account Running Balance after running Update Accounting Running Balances Scheduler Job");
    }

    @Test
    public void testUpdateLoanArrearsAgingJobOutcome() {
        loanTransactionHelper = new LoanTransactionHelper(requestSpec, responseSpec);

        final Integer clientID = ClientHelper.createClient(requestSpec, responseSpec);
        Assertions.assertNotNull(clientID);

        final Integer loanProductID = createLoanProduct(null);
        Assertions.assertNotNull(loanProductID);

        final Integer loanID = applyForLoanApplication(clientID.toString(), loanProductID.toString(), null, "10 January 2013");
        Assertions.assertNotNull(loanID);

        HashMap loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(requestSpec, responseSpec, loanID);
        LoanStatusChecker.verifyLoanIsPending(loanStatusHashMap);

        loanStatusHashMap = loanTransactionHelper.approveLoan(AccountTransferTest.LOAN_APPROVAL_DATE, loanID);
        LoanStatusChecker.verifyLoanIsApproved(loanStatusHashMap);

        String loanDetails = loanTransactionHelper.getLoanDetails(requestSpec, responseSpec, loanID);
        loanStatusHashMap = loanTransactionHelper.disburseLoanWithNetDisbursalAmount(AccountTransferTest.LOAN_DISBURSAL_DATE, loanID,
                JsonPath.from(loanDetails).get("netDisbursalAmount").toString());
        LoanStatusChecker.verifyLoanIsActive(loanStatusHashMap);

        String JobName = "Update Loan Arrears Ageing";

        schedulerJobHelper.executeAndAwaitJob(JobName);
        HashMap loanSummaryData = loanTransactionHelper.getLoanSummary(requestSpec, responseSpec, loanID);

        Float totalLoanArrearsAging = (Float) loanSummaryData.get("principalOverdue") + (Float) loanSummaryData.get("interestOverdue");

        Assertions.assertEquals(totalLoanArrearsAging, loanSummaryData.get("totalOverdue"),
                "Verifying Arrears Aging after Running Update Loan Arrears Aging Scheduler Job");
    }

    @Test
    public void testExecuteStandingInstructionsJobOutcome() throws InterruptedException {
        savingsAccountHelper = new SavingsAccountHelper(requestSpec, responseSpec);
        StandingInstructionsHelper standingInstructionsHelper = new StandingInstructionsHelper(requestSpec, responseSpec);

        final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.US);
        final DateTimeFormatter monthDayFormat = DateTimeFormatter.ofPattern("dd MMMM", Locale.US);

        // Create the LocalDate with the Zone used by default
        final LocalDate localDate = LocalDate.now(this.systemTimeZone.toZoneId());
        ZonedDateTime currentDate = ZonedDateTime.of(localDate, LocalTime.MIDNIGHT, this.systemTimeZone.toZoneId());
        // When the Stanging Instruction will be applied
        final String MONTH_DAY = monthDayFormat.format(currentDate.toLocalDate());
        // Standing Instruction valid from (One week before today)
        currentDate = currentDate.minus(Duration.ofDays(7));
        final String VALID_FROM = dateFormat.format(currentDate);
        // Standing Instruction valid to (One year after)
        currentDate = currentDate.plus(1, ChronoUnit.YEARS);
        final String VALID_TO = dateFormat.format(currentDate);

        final Integer clientID = ClientHelper.createClient(requestSpec, responseSpec);
        Assertions.assertNotNull(clientID);

        final Integer savingsProductID = createSavingsProduct(requestSpec, responseSpec,
                ClientSavingsIntegrationTest.MINIMUM_OPENING_BALANCE);
        Assertions.assertNotNull(savingsProductID);

        final Integer fromSavingsId = this.savingsAccountHelper.applyForSavingsApplication(clientID, savingsProductID,
                ClientSavingsIntegrationTest.ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertNotNull(savingsProductID);

        HashMap fromSavingsStatusHashMap = SavingsStatusChecker.getStatusOfSavings(requestSpec, responseSpec, fromSavingsId);
        SavingsStatusChecker.verifySavingsIsPending(fromSavingsStatusHashMap);

        fromSavingsStatusHashMap = this.savingsAccountHelper.approveSavings(fromSavingsId);
        SavingsStatusChecker.verifySavingsIsApproved(fromSavingsStatusHashMap);

        fromSavingsStatusHashMap = this.savingsAccountHelper.activateSavings(fromSavingsId);
        SavingsStatusChecker.verifySavingsIsActive(fromSavingsStatusHashMap);

        final Integer toSavingsId = this.savingsAccountHelper.applyForSavingsApplication(clientID, savingsProductID,
                ClientSavingsIntegrationTest.ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertNotNull(savingsProductID);

        HashMap toSavingsStatusHashMap = SavingsStatusChecker.getStatusOfSavings(requestSpec, responseSpec, toSavingsId);
        SavingsStatusChecker.verifySavingsIsPending(toSavingsStatusHashMap);

        toSavingsStatusHashMap = this.savingsAccountHelper.approveSavings(toSavingsId);
        SavingsStatusChecker.verifySavingsIsApproved(toSavingsStatusHashMap);

        toSavingsStatusHashMap = this.savingsAccountHelper.activateSavings(toSavingsId);
        SavingsStatusChecker.verifySavingsIsActive(toSavingsStatusHashMap);

        HashMap fromSavingsSummaryBefore = this.savingsAccountHelper.getSavingsSummary(fromSavingsId);
        Float fromSavingsBalanceBefore = (Float) fromSavingsSummaryBefore.get("accountBalance");

        HashMap toSavingsSummaryBefore = this.savingsAccountHelper.getSavingsSummary(toSavingsId);
        Float toSavingsBalanceBefore = (Float) toSavingsSummaryBefore.get("accountBalance");

        Integer standingInstructionId = standingInstructionsHelper.createStandingInstruction(clientID.toString(), fromSavingsId.toString(),
                toSavingsId.toString(), FROM_ACCOUNT_TYPE_SAVINGS, TO_ACCOUNT_TYPE_SAVINGS, VALID_FROM, VALID_TO, MONTH_DAY);
        Assertions.assertNotNull(standingInstructionId);

        String JobName = "Execute Standing Instruction";
        this.schedulerJobHelper.executeAndAwaitJob(JobName);
        HashMap fromSavingsSummaryAfter = this.savingsAccountHelper.getSavingsSummary(fromSavingsId);
        Float fromSavingsBalanceAfter = (Float) fromSavingsSummaryAfter.get("accountBalance");

        HashMap toSavingsSummaryAfter = this.savingsAccountHelper.getSavingsSummary(toSavingsId);
        Float toSavingsBalanceAfter = (Float) toSavingsSummaryAfter.get("accountBalance");

        final HashMap standingInstructionData = standingInstructionsHelper.getStandingInstructionById(standingInstructionId.toString());
        Float expectedFromSavingsBalance = fromSavingsBalanceBefore - (Float) standingInstructionData.get("amount");
        Float expectedToSavingsBalance = toSavingsBalanceBefore + (Float) standingInstructionData.get("amount");

        Assertions.assertEquals(expectedFromSavingsBalance, fromSavingsBalanceAfter,
                "Verifying From Savings Balance after Successful completion of Scheduler Job");
        Assertions.assertEquals(expectedToSavingsBalance, toSavingsBalanceAfter,
                "Verifying To Savings Balance after Successful completion of Scheduler Job");
        Integer fromAccountType = PortfolioAccountType.SAVINGS.getValue();
        Integer transferType = AccountTransferType.ACCOUNT_TRANSFER.getValue();
        List<HashMap> standingInstructionHistoryData = standingInstructionsHelper.getStandingInstructionHistory(fromSavingsId,
                fromAccountType, clientID, transferType);
        Assertions.assertEquals(1, standingInstructionHistoryData.size(),
                "Verifying the no of standing instruction transactions logged for the client");
        HashMap loggedTransaction = standingInstructionHistoryData.get(0);

        Assertions.assertEquals((Float) standingInstructionData.get("amount"), (Float) loggedTransaction.get("amount"),
                "Verifying transferred amount and logged transaction amounts");
    }

    @Test
    public void testApplyPenaltyForOverdueLoansJobOutcome() throws InterruptedException {
        this.savingsAccountHelper = new SavingsAccountHelper(requestSpec, responseSpec);
        this.loanTransactionHelper = new LoanTransactionHelper(requestSpec, responseSpec);

        final Integer clientID = ClientHelper.createClient(requestSpec, responseSpec);
        Assertions.assertNotNull(clientID);

        Integer overdueFeeChargeId = ChargesHelper.createCharges(requestSpec, responseSpec, ChargesHelper.getLoanOverdueFeeJSON());
        Assertions.assertNotNull(overdueFeeChargeId);

        final Integer loanProductID = createLoanProduct(overdueFeeChargeId.toString());
        Assertions.assertNotNull(loanProductID);

        final Integer loanID = applyForLoanApplication(clientID.toString(), loanProductID.toString(), null, "10 January 2020");
        Assertions.assertNotNull(loanID);

        HashMap loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(requestSpec, responseSpec, loanID);
        LoanStatusChecker.verifyLoanIsPending(loanStatusHashMap);

        loanStatusHashMap = this.loanTransactionHelper.approveLoan("01 March 2020", loanID);
        LoanStatusChecker.verifyLoanIsApproved(loanStatusHashMap);

        String loanDetails = this.loanTransactionHelper.getLoanDetails(requestSpec, responseSpec, loanID);
        loanStatusHashMap = this.loanTransactionHelper.disburseLoanWithNetDisbursalAmount("02 March 2020", loanID,
                JsonPath.from(loanDetails).get("netDisbursalAmount").toString());
        LoanStatusChecker.verifyLoanIsActive(loanStatusHashMap);

        String JobName = "Apply penalty to overdue loans";
        this.schedulerJobHelper.executeAndAwaitJob(JobName);

        final HashMap chargeData = ChargesHelper.getChargeById(requestSpec, responseSpec, overdueFeeChargeId);

        Float chargeAmount = (Float) chargeData.get("amount");

        ArrayList<HashMap> repaymentScheduleDataAfter = this.loanTransactionHelper.getLoanRepaymentSchedule(requestSpec, responseSpec,
                loanID);

        Assertions.assertEquals(chargeAmount, (Float) repaymentScheduleDataAfter.get(1).get("penaltyChargesDue"),
                "Verifying From Penalty Charges due fot first Repayment after Successful completion of Scheduler Job");

        loanStatusHashMap = this.loanTransactionHelper.undoDisbursal(loanID);
        LoanStatusChecker.verifyLoanIsApproved(loanStatusHashMap);
        LoanStatusChecker.verifyLoanIsWaitingForDisbursal(loanStatusHashMap);
    }

    @Test
    public void testLoanCOBJobOutcome() {
        this.savingsAccountHelper = new SavingsAccountHelper(requestSpec, responseSpec);
        this.loanTransactionHelper = new LoanTransactionHelper(requestSpec, responseSpec);

        final Integer clientID = ClientHelper.createClient(requestSpec, responseSpec);
        Assertions.assertNotNull(clientID);

        Integer overdueFeeChargeId = ChargesHelper.createCharges(requestSpec, responseSpec,
                ChargesHelper.getLoanOverdueFeeJSONWithCalculationTypePercentage("1"));
        Assertions.assertNotNull(overdueFeeChargeId);

        final Integer loanProductID = createLoanProduct(overdueFeeChargeId.toString());
        Assertions.assertNotNull(loanProductID);
        List<Integer> loanIDs = new ArrayList<>();
        HashMap loanStatusHashMap;
        for (int i = 0; i < 10; i++) {
            final Integer loanID = applyForLoanApplication(clientID.toString(), loanProductID.toString(), null, "10 January 2020");

            Assertions.assertNotNull(loanID);

            loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(requestSpec, responseSpec, loanID);
            LoanStatusChecker.verifyLoanIsPending(loanStatusHashMap);

            loanStatusHashMap = this.loanTransactionHelper.approveLoan("01 March 2020", loanID);
            LoanStatusChecker.verifyLoanIsApproved(loanStatusHashMap);

            String loanDetails = this.loanTransactionHelper.getLoanDetails(requestSpec, responseSpec, loanID);
            loanStatusHashMap = this.loanTransactionHelper.disburseLoanWithNetDisbursalAmount("02 March 2020", loanID,
                    JsonPath.from(loanDetails).get("netDisbursalAmount").toString());
            LoanStatusChecker.verifyLoanIsActive(loanStatusHashMap);
            loanIDs.add(loanID);

        }

        String jobName = "Loan COB";
        this.schedulerJobHelper.executeAndAwaitJob(jobName);
        for (Integer loanId : loanIDs) {
            List<HashMap> repaymentScheduleDataAfter = this.loanTransactionHelper.getLoanRepaymentSchedule(requestSpec, responseSpec,
                    loanId);

            Assertions.assertEquals(39.39f, (Float) repaymentScheduleDataAfter.get(1).get("penaltyChargesDue"),
                    "Verifying From Penalty Charges due fot first Repayment after Successful completion of Scheduler Job");
            Assertions.assertEquals(39.39f, (Float) repaymentScheduleDataAfter.get(2).get("penaltyChargesDue"),
                    "Verifying From Penalty Charges due fot first Repayment after Successful completion of Scheduler Job");
            Assertions.assertEquals(39.39f, (Float) repaymentScheduleDataAfter.get(3).get("penaltyChargesDue"),
                    "Verifying From Penalty Charges due fot first Repayment after Successful completion of Scheduler Job");
            Assertions.assertEquals(39.39f, (Float) repaymentScheduleDataAfter.get(4).get("penaltyChargesDue"),
                    "Verifying From Penalty Charges due fot first Repayment after Successful completion of Scheduler Job");

        }
    }

    @Test
    public void testLoanCOBApplyPenaltyOnDue() {
        GlobalConfigurationHelper.updateIsBusinessDateEnabled(requestSpec, responseSpec, Boolean.TRUE);
        // set penalty wait period to 0
        GlobalConfigurationHelper.updateValueForGlobalConfiguration(this.requestSpec, this.responseSpec, "10", "0");
        this.loanTransactionHelper = new LoanTransactionHelper(requestSpec, responseSpec);

        final Integer clientID = ClientHelper.createClient(requestSpec, responseSpec);
        Assertions.assertNotNull(clientID);

        Integer overdueFeeChargeId = ChargesHelper.createCharges(requestSpec, responseSpec,
                ChargesHelper.getLoanOverdueFeeJSONWithCalculationTypePercentage("1"));
        Assertions.assertNotNull(overdueFeeChargeId);

        final Integer loanProductID = createLoanProduct(overdueFeeChargeId.toString());
        Assertions.assertNotNull(loanProductID);
        HashMap loanStatusHashMap;

        final Integer loanID = applyForLoanApplication(clientID.toString(), loanProductID.toString(), null, "10 January 2020");

        Assertions.assertNotNull(loanID);

        loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(requestSpec, responseSpec, loanID);
        LoanStatusChecker.verifyLoanIsPending(loanStatusHashMap);

        loanStatusHashMap = this.loanTransactionHelper.approveLoan("01 March 2020", loanID);
        LoanStatusChecker.verifyLoanIsApproved(loanStatusHashMap);

        String loanDetails = this.loanTransactionHelper.getLoanDetails(requestSpec, responseSpec, loanID);
        loanStatusHashMap = this.loanTransactionHelper.disburseLoanWithNetDisbursalAmount("02 March 2020", loanID,
                JsonPath.from(loanDetails).get("netDisbursalAmount").toString());
        LoanStatusChecker.verifyLoanIsActive(loanStatusHashMap);
        BusinessDateHelper.updateBusinessDate(requestSpec, responseSpec, BusinessDateType.COB_DATE, LocalDate.of(2020, 4, 1));
        String jobName = "Loan COB";

        this.schedulerJobHelper.executeAndAwaitJob(jobName);
        List<HashMap> repaymentScheduleDataAfter = this.loanTransactionHelper.getLoanRepaymentSchedule(requestSpec, responseSpec, loanID);
        Assertions.assertEquals(0, (Integer) repaymentScheduleDataAfter.get(1).get("penaltyChargesDue"),
                "Verifying From Penalty Charges due fot first Repayment after Successful completion of Scheduler Job");

        BusinessDateHelper.updateBusinessDate(requestSpec, responseSpec, BusinessDateType.COB_DATE, LocalDate.of(2020, 4, 2));
        this.schedulerJobHelper.executeAndAwaitJob(jobName);
        repaymentScheduleDataAfter = this.loanTransactionHelper.getLoanRepaymentSchedule(requestSpec, responseSpec, loanID);
        Assertions.assertEquals(39.39f, (Float) repaymentScheduleDataAfter.get(1).get("penaltyChargesDue"),
                "Verifying From Penalty Charges due fot first Repayment after Successful completion of Scheduler Job");

        GlobalConfigurationHelper.updateValueForGlobalConfiguration(this.requestSpec, this.responseSpec, "10", "1");

        BusinessDateHelper.updateBusinessDate(requestSpec, responseSpec, BusinessDateType.COB_DATE, LocalDate.of(2020, 5, 2));
        this.schedulerJobHelper.executeAndAwaitJob(jobName);
        repaymentScheduleDataAfter = this.loanTransactionHelper.getLoanRepaymentSchedule(requestSpec, responseSpec, loanID);
        Assertions.assertEquals(0, (Integer) repaymentScheduleDataAfter.get(2).get("penaltyChargesDue"),
                "Verifying From Penalty Charges due fot first Repayment after Successful completion of Scheduler Job");

        BusinessDateHelper.updateBusinessDate(requestSpec, responseSpec, BusinessDateType.COB_DATE, LocalDate.of(2020, 5, 3));
        this.schedulerJobHelper.executeAndAwaitJob(jobName);
        repaymentScheduleDataAfter = this.loanTransactionHelper.getLoanRepaymentSchedule(requestSpec, responseSpec, loanID);
        Assertions.assertEquals(39.39f, (Float) repaymentScheduleDataAfter.get(2).get("penaltyChargesDue"),
                "Verifying From Penalty Charges due fot first Repayment after Successful completion of Scheduler Job");

        List<Map> transactions = this.loanTransactionHelper.getLoanTransactions(this.requestSpec, this.responseSpec, loanID);
        Assertions.assertEquals(39.39f, (Float) transactions.get(2).get("amount"));
        Assertions.assertEquals(2020, ((List) transactions.get(2).get("date")).get(0));
        Assertions.assertEquals(4, ((List) transactions.get(2).get("date")).get(1));
        Assertions.assertEquals(2, ((List) transactions.get(2).get("date")).get(2));

        transactions = this.loanTransactionHelper.getLoanTransactions(this.requestSpec, this.responseSpec, loanID);
        Assertions.assertEquals(39.39f, (Float) transactions.get(3).get("amount"));
        Assertions.assertEquals(2020, ((List) transactions.get(3).get("date")).get(0));
        Assertions.assertEquals(5, ((List) transactions.get(3).get("date")).get(1));
        Assertions.assertEquals(2, ((List) transactions.get(3).get("date")).get(2));

        GlobalConfigurationHelper.updateIsBusinessDateEnabled(requestSpec, responseSpec, Boolean.FALSE);
        GlobalConfigurationHelper.updateValueForGlobalConfiguration(this.requestSpec, this.responseSpec, "10", "2");
    }

    @Test
    public void testAvoidUnncessaryPenaltyWhenAmountZeroForOverdueLoansJobOutcome() throws InterruptedException {
        this.savingsAccountHelper = new SavingsAccountHelper(requestSpec, responseSpec);
        this.loanTransactionHelper = new LoanTransactionHelper(requestSpec, responseSpec);

        final Integer clientID = ClientHelper.createClient(requestSpec, responseSpec);
        Assertions.assertNotNull(clientID);

        Integer overdueFeeChargeId = ChargesHelper.createCharges(requestSpec, responseSpec,
                ChargesHelper.getLoanOverdueFeeJSONWithCalculationTypePercentage("0.000001"));
        Assertions.assertNotNull(overdueFeeChargeId);

        final Integer loanProductID = createLoanProduct(overdueFeeChargeId.toString());
        Assertions.assertNotNull(loanProductID);

        final Integer loanID = applyForLoanApplication(clientID.toString(), loanProductID.toString(), null, "10 January 2013");
        Assertions.assertNotNull(loanID);

        HashMap loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(requestSpec, responseSpec, loanID);
        LoanStatusChecker.verifyLoanIsPending(loanStatusHashMap);

        loanStatusHashMap = this.loanTransactionHelper.approveLoan(AccountTransferTest.LOAN_APPROVAL_DATE, loanID);
        LoanStatusChecker.verifyLoanIsApproved(loanStatusHashMap);

        String loanDetails = this.loanTransactionHelper.getLoanDetails(requestSpec, responseSpec, loanID);
        loanStatusHashMap = this.loanTransactionHelper.disburseLoanWithNetDisbursalAmount(AccountTransferTest.LOAN_APPROVAL_DATE_PLUS_ONE,
                loanID, JsonPath.from(loanDetails).get("netDisbursalAmount").toString());
        LoanStatusChecker.verifyLoanIsActive(loanStatusHashMap);

        String JobName = "Apply penalty to overdue loans";
        int jobId = 12;

        this.schedulerJobHelper.executeAndAwaitJob(JobName);

        Map<String, Object> schedulerJob = this.schedulerJobHelper.getSchedulerJobById(jobId);

        Assertions.assertNotNull(schedulerJob);
        while ((Boolean) schedulerJob.get("currentlyRunning")) {
            Thread.sleep(15000);
            schedulerJob = this.schedulerJobHelper.getSchedulerJobById(jobId);
            Assertions.assertNotNull(schedulerJob);
        }

        ArrayList<HashMap> repaymentScheduleDataAfter = this.loanTransactionHelper.getLoanRepaymentSchedule(requestSpec, responseSpec,
                loanID);

        Assertions.assertEquals(0, repaymentScheduleDataAfter.get(1).get("penaltyChargesDue"),
                "Verifying From Penalty Charges due fot first Repayment after Successful completion of Scheduler Job");

        final List<?> loanCharges = this.loanTransactionHelper.getLoanCharges(requestSpec, responseSpec, loanID);
        Assertions.assertNull(loanCharges, "Verifying that charge isn't created when the amount is 0");

        loanStatusHashMap = this.loanTransactionHelper.undoDisbursal(loanID);
        LoanStatusChecker.verifyLoanIsApproved(loanStatusHashMap);
        LoanStatusChecker.verifyLoanIsWaitingForDisbursal(loanStatusHashMap);
    }

    @Test
    public void testUpdateOverdueDaysForNPA() throws InterruptedException {
        this.loanTransactionHelper = new LoanTransactionHelper(requestSpec, responseSpec);

        final Integer clientID = ClientHelper.createClient(requestSpec, responseSpec);
        Assertions.assertNotNull(clientID);

        final Integer loanProductID = createLoanProduct(null);
        Assertions.assertNotNull(loanProductID);

        final Integer loanID = applyForLoanApplication(clientID.toString(), loanProductID.toString(), null, "10 January 2013");
        Assertions.assertNotNull(loanID);

        HashMap loanStatusHashMap = LoanStatusChecker.getStatusOfLoan(requestSpec, responseSpec, loanID);
        LoanStatusChecker.verifyLoanIsPending(loanStatusHashMap);

        loanStatusHashMap = this.loanTransactionHelper.approveLoan(AccountTransferTest.LOAN_APPROVAL_DATE, loanID);
        LoanStatusChecker.verifyLoanIsApproved(loanStatusHashMap);

        String loanDetails = this.loanTransactionHelper.getLoanDetails(requestSpec, responseSpec, loanID);
        loanStatusHashMap = this.loanTransactionHelper.disburseLoanWithNetDisbursalAmount(AccountTransferTest.LOAN_APPROVAL_DATE_PLUS_ONE,
                loanID, JsonPath.from(loanDetails).get("netDisbursalAmount").toString());
        LoanStatusChecker.verifyLoanIsActive(loanStatusHashMap);

        final Boolean isNPABefore = (Boolean) this.loanTransactionHelper.getLoanDetail(requestSpec, responseSpec, loanID, "isNPA");
        Assertions.assertFalse(isNPABefore);
        String JobName = "Update Non Performing Assets";
        this.schedulerJobHelper.executeAndAwaitJob(JobName);
        final Boolean isNPAAfter = (Boolean) this.loanTransactionHelper.getLoanDetail(requestSpec, responseSpec, loanID, "isNPA");
        Assertions.assertTrue(isNPAAfter);
    }

    @Test
    public void testInterestTransferForSavings() throws InterruptedException {
        this.savingsAccountHelper = new SavingsAccountHelper(requestSpec, responseSpec);
        FixedDepositAccountHelper fixedDepositAccountHelper = new FixedDepositAccountHelper(requestSpec, responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -3);
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.YEAR, 10);
        final String VALID_TO = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -2);
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.MONTH, 1);
        final String WHOLE_TERM = "1";

        Integer clientId = ClientHelper.createClient(requestSpec, responseSpec);
        Assertions.assertNotNull(clientId);
        Float balance = Float.parseFloat(MINIMUM_OPENING_BALANCE) + Float.parseFloat(FixedDepositAccountHelper.DEPOSIT_AMOUNT);
        final Integer savingsProductID = createSavingsProduct(requestSpec, responseSpec, String.valueOf(balance));
        Assertions.assertNotNull(savingsProductID);

        final Integer savingsId = this.savingsAccountHelper.applyForSavingsApplication(clientId, savingsProductID,
                ClientSavingsIntegrationTest.ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertNotNull(savingsId);

        HashMap savingsStatusHashMap = SavingsStatusChecker.getStatusOfSavings(requestSpec, responseSpec, savingsId);
        SavingsStatusChecker.verifySavingsIsPending(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.approveSavings(savingsId);
        SavingsStatusChecker.verifySavingsIsApproved(savingsStatusHashMap);

        savingsStatusHashMap = this.savingsAccountHelper.activateSavings(savingsId);
        SavingsStatusChecker.verifySavingsIsActive(savingsStatusHashMap);
        HashMap summary = savingsAccountHelper.getSavingsSummary(savingsId);
        assertEquals(balance, summary.get("accountBalance"), "Verifying opening Balance");

        Integer fixedDepositProductId = createFixedDepositProduct(VALID_FROM, VALID_TO);
        Assertions.assertNotNull(fixedDepositProductId);

        Integer fixedDepositAccountId = applyForFixedDepositApplication(clientId.toString(), fixedDepositProductId.toString(),
                SUBMITTED_ON_DATE, WHOLE_TERM, savingsId.toString());
        Assertions.assertNotNull(fixedDepositAccountId);

        HashMap fixedDepositAccountStatusHashMap = FixedDepositAccountStatusChecker.getStatusOfFixedDepositAccount(requestSpec,
                responseSpec, fixedDepositAccountId.toString());
        FixedDepositAccountStatusChecker.verifyFixedDepositIsPending(fixedDepositAccountStatusHashMap);

        fixedDepositAccountStatusHashMap = fixedDepositAccountHelper.approveFixedDeposit(fixedDepositAccountId, APPROVED_ON_DATE);
        FixedDepositAccountStatusChecker.verifyFixedDepositIsApproved(fixedDepositAccountStatusHashMap);

        fixedDepositAccountStatusHashMap = fixedDepositAccountHelper.activateFixedDeposit(fixedDepositAccountId, ACTIVATION_DATE);
        FixedDepositAccountStatusChecker.verifyFixedDepositIsActive(fixedDepositAccountStatusHashMap);
        summary = savingsAccountHelper.getSavingsSummary(savingsId);
        balance = Float.parseFloat(MINIMUM_OPENING_BALANCE);
        assertEquals(balance, summary.get("accountBalance"), "Verifying Balance");

        fixedDepositAccountHelper.postInterestForFixedDeposit(fixedDepositAccountId);

        HashMap fixedDepositSummary = savingsAccountHelper.getSavingsSummary(fixedDepositAccountId);
        float interestPosted = (Float) fixedDepositSummary.get("accountBalance")
                - Float.parseFloat(FixedDepositAccountHelper.DEPOSIT_AMOUNT);

        String JobName = "Transfer Interest To Savings";
        this.schedulerJobHelper.executeAndAwaitJob(JobName);
        fixedDepositSummary = savingsAccountHelper.getSavingsSummary(fixedDepositAccountId);
        assertEquals(Float.parseFloat(FixedDepositAccountHelper.DEPOSIT_AMOUNT), fixedDepositSummary.get("accountBalance"),
                "Verifying opening Balance");

        summary = savingsAccountHelper.getSavingsSummary(savingsId);
        balance = Float.parseFloat(MINIMUM_OPENING_BALANCE) + interestPosted;
        validateNumberForEqualExcludePrecision(String.valueOf(balance), String.valueOf(summary.get("accountBalance")));
    }

    private Integer createSavingsProduct(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final String minOpeningBalance) {
        SavingsProductHelper savingsProductHelper = new SavingsProductHelper();
        final String savingsProductJSON = savingsProductHelper.withInterestCompoundingPeriodTypeAsDaily()
                .withInterestPostingPeriodTypeAsMonthly().withInterestCalculationPeriodTypeAsDailyBalance()
                .withMinimumOpenningBalance(minOpeningBalance).build();
        return SavingsProductHelper.createSavingsProduct(savingsProductJSON, requestSpec, responseSpec);
    }

    private Integer createSavingsProduct(final String minOpeningBalance, final Account... accounts) {
        final String savingsProductJSON = new SavingsProductHelper().withInterestCompoundingPeriodTypeAsDaily()
                .withInterestPostingPeriodTypeAsQuarterly().withInterestCalculationPeriodTypeAsDailyBalance()
                .withMinimumOpenningBalance(minOpeningBalance).withAccountingRuleAsCashBased(accounts).build();
        return SavingsProductHelper.createSavingsProduct(savingsProductJSON, requestSpec, responseSpec);
    }

    private Integer createLoanProduct(final String chargeId) {
        final String loanProductJSON = new LoanProductTestBuilder().withPrincipal("15,000.00").withNumberOfRepayments("4")
                .withRepaymentAfterEvery("1").withRepaymentTypeAsMonth().withinterestRatePerPeriod("1")
                .withInterestRateFrequencyTypeAsMonths().withAmortizationTypeAsEqualInstallments().withInterestTypeAsDecliningBalance()
                .build(chargeId);
        return this.loanTransactionHelper.getLoanProductId(loanProductJSON);
    }

    private void addCollaterals(List<HashMap> collaterals, Integer collateralId, BigDecimal quantity) {
        collaterals.add(collaterals(collateralId, quantity));
    }

    private HashMap<String, String> collaterals(Integer collateralId, BigDecimal quantity) {
        HashMap<String, String> collateral = new HashMap<>(2);
        collateral.put("clientCollateralId", collateralId.toString());
        collateral.put("quantity", quantity.toString());
        return collateral;
    }

    private Integer applyForLoanApplication(final String clientID, final String loanProductID, final String savingsID, final String date) {

        List<HashMap> collaterals = new ArrayList<>();
        final Integer collateralId = CollateralManagementHelper.createCollateralProduct(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(collateralId);
        final Integer clientCollateralId = CollateralManagementHelper.createClientCollateral(this.requestSpec, this.responseSpec, clientID,
                collateralId);
        Assertions.assertNotNull(clientCollateralId);
        addCollaterals(collaterals, clientCollateralId, BigDecimal.valueOf(1));

        final String loanApplicationJSON = new LoanApplicationTestBuilder().withPrincipal("15,000.00").withLoanTermFrequency("4")
                .withLoanTermFrequencyAsMonths().withNumberOfRepayments("4").withRepaymentEveryAfter("1")
                .withRepaymentFrequencyTypeAsMonths().withInterestRatePerPeriod("2").withAmortizationTypeAsEqualInstallments()
                .withInterestTypeAsDecliningBalance().withInterestCalculationPeriodTypeSameAsRepaymentPeriod()
                .withExpectedDisbursementDate(date).withSubmittedOnDate(date).withCollaterals(collaterals)
                .build(clientID, loanProductID, savingsID);
        return this.loanTransactionHelper.getLoanId(loanApplicationJSON);
    }

    private Integer createFixedDepositProduct(final String validFrom, final String validTo) {
        FixedDepositProductHelper fixedDepositProductHelper = new FixedDepositProductHelper(requestSpec, responseSpec);
        final String fixedDepositProductJSON = fixedDepositProductHelper.withPeriodRangeChart().build(validFrom, validTo);
        return FixedDepositProductHelper.createFixedDepositProduct(fixedDepositProductJSON, requestSpec, responseSpec);
    }

    private Integer applyForFixedDepositApplication(final String clientID, final String productID, final String submittedOnDate,
            final String penalInterestType, String savingsId) {
        final String fixedDepositApplicationJSON = new FixedDepositAccountHelper(requestSpec, responseSpec)
                .withSubmittedOnDate(submittedOnDate).withSavings(savingsId).transferInterest(true)
                .withLockinPeriodFrequency("1", FixedDepositAccountHelper.DAYS).build(clientID, productID, penalInterestType);
        return FixedDepositAccountHelper.applyFixedDepositApplication(fixedDepositApplicationJSON, requestSpec, responseSpec);
    }

    private void validateNumberForEqualExcludePrecision(String val, String val2) {
        DecimalFormat twoDForm = new DecimalFormat("#", new DecimalFormatSymbols(Locale.US));
        assertEquals(0,
                Float.valueOf(twoDForm.format(Float.parseFloat(val))).compareTo(Float.valueOf(twoDForm.format(Float.parseFloat(val2)))));
    }
}
