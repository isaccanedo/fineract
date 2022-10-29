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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import org.apache.fineract.accounting.common.AccountingConstants.FinancialActivity;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.CommonConstants;
import org.apache.fineract.integrationtests.common.SchedulerJobHelper;
import org.apache.fineract.integrationtests.common.TaxComponentHelper;
import org.apache.fineract.integrationtests.common.TaxGroupHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.accounting.Account;
import org.apache.fineract.integrationtests.common.accounting.Account.AccountType;
import org.apache.fineract.integrationtests.common.accounting.AccountHelper;
import org.apache.fineract.integrationtests.common.accounting.FinancialActivityAccountHelper;
import org.apache.fineract.integrationtests.common.accounting.JournalEntry;
import org.apache.fineract.integrationtests.common.accounting.JournalEntryHelper;
import org.apache.fineract.integrationtests.common.fixeddeposit.FixedDepositAccountStatusChecker;
import org.apache.fineract.integrationtests.common.recurringdeposit.RecurringDepositAccountHelper;
import org.apache.fineract.integrationtests.common.recurringdeposit.RecurringDepositAccountStatusChecker;
import org.apache.fineract.integrationtests.common.recurringdeposit.RecurringDepositProductHelper;
import org.apache.fineract.integrationtests.common.savings.SavingsAccountHelper;
import org.apache.fineract.integrationtests.common.savings.SavingsProductHelper;
import org.apache.fineract.integrationtests.common.savings.SavingsStatusChecker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({ "unused", "rawtypes", "unchecked", "static-access" })
public class RecurringDepositTest {

    private static final Logger LOG = LoggerFactory.getLogger(RecurringDepositTest.class);
    private ResponseSpecification responseSpec;
    private RequestSpecification requestSpec;
    private RecurringDepositProductHelper recurringDepositProductHelper;
    private SavingsAccountHelper savingsAccountHelper;
    private AccountHelper accountHelper;
    private RecurringDepositAccountHelper recurringDepositAccountHelper;
    private JournalEntryHelper journalEntryHelper;
    private FinancialActivityAccountHelper financialActivityAccountHelper;

    public static final String WHOLE_TERM = "1";
    private static final String TILL_PREMATURE_WITHDRAWAL = "2";
    private static final String DAILY = "1";
    private static final String MONTHLY = "4";
    private static final String QUARTERLY = "5";
    private static final String BI_ANNUALLY = "6";
    private static final String ANNUALLY = "7";
    private static final String INTEREST_CALCULATION_USING_DAILY_BALANCE = "1";
    private static final String INTEREST_CALCULATION_USING_AVERAGE_DAILY_BALANCE = "2";
    private static final String DAYS_360 = "360";
    private static final String DAYS_365 = "365";
    private static final String NONE = "1";
    private static final String CASH_BASED = "2";

    public static final String MINIMUM_OPENING_BALANCE = "1000.0";
    public static final String ACCOUNT_TYPE_INDIVIDUAL = "INDIVIDUAL";
    public static final String CLOSURE_TYPE_WITHDRAW_DEPOSIT = "100";
    public static final String CLOSURE_TYPE_TRANSFER_TO_SAVINGS = "200";
    public static final String CLOSURE_TYPE_REINVEST = "300";
    public static final Integer DAILY_COMPOUNDING_INTERVAL = 0;
    public static final Integer MONTHLY_INTERVAL = 1;
    public static final Integer QUARTERLY_INTERVAL = 3;
    public static final Integer BIANNULLY_INTERVAL = 6;
    public static final Integer ANNUL_INTERVAL = 12;

    public static final Float DEPOSIT_AMOUNT = 2000.0f;

    // TODO Given the difference in calculation methods in test vs application,
    // the exact values
    // returned may differ enough to cause differences in rounding. Given this,
    // we only compare that the result is within THRESHOLD of the expected amount.
    // A proper solution would be to implement the exact interest
    // calculation in this test,
    // and then to compare the exact results
    public static final Float THRESHOLD = 1.0f;

    @BeforeEach
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.requestSpec.header("Fineract-Platform-TenantId", "default");
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        this.journalEntryHelper = new JournalEntryHelper(this.requestSpec, this.responseSpec);
        this.financialActivityAccountHelper = new FinancialActivityAccountHelper(this.requestSpec);
    }

    /***
     * Test case for Recurring Deposit Account premature closure with transaction type withdrawal and Cash Based
     * accounting enabled
     */
    @Test
    public void testRecurringDepositAccountWithPrematureClosureTypeWithdrawal() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.accountHelper = new AccountHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        /***
         * Create GL Accounts for product account mapping
         */
        final Account assetAccount = this.accountHelper.createAssetAccount();
        final Account incomeAccount = this.accountHelper.createIncomeAccount();
        final Account expenseAccount = this.accountHelper.createExpenseAccount();
        final Account liabilityAccount = this.accountHelper.createLiabilityAccount();

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);
        DateFormat currentDateFormat = new SimpleDateFormat("dd");

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -3);
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.YEAR, 10);
        final String VALID_TO = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -1);
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());

        Integer currentDate = Integer.valueOf(currentDateFormat.format(todaysDate.getTime()));
        Integer daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);
        Integer numberOfDaysLeft = daysInMonth - currentDate + 1;
        todaysDate.add(Calendar.DATE, numberOfDaysLeft);
        final String INTEREST_POSTED_DATE = dateFormat.format(todaysDate.getTime());
        final String CLOSED_ON_DATE = dateFormat.format(Calendar.getInstance().getTime());

        /***
         * Create client for applying Deposit account
         */
        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        /***
         * Create RD product with CashBased accounting enabled
         */
        final String accountingRule = CASH_BASED;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule, assetAccount,
                liabilityAccount, incomeAccount, expenseAccount);
        Assertions.assertNotNull(recurringDepositProductId);

        /***
         * Apply for RD account with created product and verify status
         */
        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        /***
         * Approve the RD account and verify whether account is approved
         */
        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsApproved(recurringDepositAccountStatusHashMap);

        /***
         * Activate the RD Account and verify whether account is activated
         */
        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.activateRecurringDeposit(recurringDepositAccountId,
                ACTIVATION_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsActive(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec,
                this.responseSpec, recurringDepositAccountId);
        Float depositAmount = (Float) recurringDepositAccountData.get("mandatoryRecommendedDepositAmount");

        /***
         * Perform Deposit transaction and verify journal entries are posted for the transaction
         */
        Integer depositTransactionId = this.recurringDepositAccountHelper.depositToRecurringDepositAccount(recurringDepositAccountId,
                depositAmount, expectedFirstDepositOnDate);
        Assertions.assertNotNull(depositTransactionId);

        this.journalEntryHelper.checkJournalEntryForAssetAccount(assetAccount, expectedFirstDepositOnDate,
                new JournalEntry(depositAmount, JournalEntry.TransactionType.DEBIT));
        this.journalEntryHelper.checkJournalEntryForLiabilityAccount(liabilityAccount, expectedFirstDepositOnDate,
                new JournalEntry(depositAmount, JournalEntry.TransactionType.CREDIT));

        /***
         * Update interest earned field for RD account
         */
        recurringDepositAccountId = this.recurringDepositAccountHelper.calculateInterestForRecurringDeposit(recurringDepositAccountId);
        Assertions.assertNotNull(recurringDepositAccountId);

        /***
         * Post interest and verify journal entries
         */
        Integer transactionIdForPostInterest = this.recurringDepositAccountHelper
                .postInterestForRecurringDeposit(recurringDepositAccountId);
        Assertions.assertNotNull(transactionIdForPostInterest);

        HashMap accountSummary = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        Float totalInterestPosted = (Float) accountSummary.get("totalInterestPosted");

        final JournalEntry[] expenseAccountEntry = { new JournalEntry(totalInterestPosted, JournalEntry.TransactionType.DEBIT) };
        final JournalEntry[] liablilityAccountEntry = { new JournalEntry(totalInterestPosted, JournalEntry.TransactionType.CREDIT) };
        this.journalEntryHelper.checkJournalEntryForAssetAccount(expenseAccount, INTEREST_POSTED_DATE, expenseAccountEntry);
        this.journalEntryHelper.checkJournalEntryForLiabilityAccount(liabilityAccount, INTEREST_POSTED_DATE, liablilityAccountEntry);

        /***
         * Calculate expected premature closure amount
         */
        HashMap recurringDepositPrematureData = this.recurringDepositAccountHelper
                .calculatePrematureAmountForRecurringDeposit(recurringDepositAccountId, CLOSED_ON_DATE);

        /***
         * Preclose the RD account verify whether account is preClosed
         */
        Integer prematureClosureTransactionId = (Integer) this.recurringDepositAccountHelper.prematureCloseForRecurringDeposit(
                recurringDepositAccountId, CLOSED_ON_DATE, CLOSURE_TYPE_WITHDRAW_DEPOSIT, null, CommonConstants.RESPONSE_RESOURCE_ID);
        Assertions.assertNotNull(prematureClosureTransactionId);

        recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker.getStatusOfRecurringDepositAccount(this.requestSpec,
                this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositAccountIsPrematureClosed(recurringDepositAccountStatusHashMap);

        /***
         * Verify journal entry transactions for preclosure transaction
         */
        recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec, this.responseSpec,
                recurringDepositAccountId);
        Float maturityAmount = Float.valueOf(recurringDepositAccountData.get("maturityAmount").toString());
        this.journalEntryHelper.checkJournalEntryForAssetAccount(assetAccount, CLOSED_ON_DATE,
                new JournalEntry(maturityAmount, JournalEntry.TransactionType.CREDIT));
        this.journalEntryHelper.checkJournalEntryForLiabilityAccount(liabilityAccount, CLOSED_ON_DATE,
                new JournalEntry(maturityAmount, JournalEntry.TransactionType.DEBIT));

    }

    /***
     * Test case for Recurring Deposit Account premature closure with transaction transfers to savings account and Cash
     * Based accounting enabled
     */
    @Test
    public void testRecurringDepositAccountWithPrematureClosureTypeTransferToSavings() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.accountHelper = new AccountHelper(this.requestSpec, this.responseSpec);
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        /***
         * Create GL Accounts for product account mapping
         */
        final Account assetAccount = this.accountHelper.createAssetAccount();
        final Account incomeAccount = this.accountHelper.createIncomeAccount();
        final Account expenseAccount = this.accountHelper.createExpenseAccount();
        final Account liabilityAccount = this.accountHelper.createLiabilityAccount();

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);
        DateFormat currentDateFormat = new SimpleDateFormat("dd");

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -3);
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.YEAR, 10);
        final String VALID_TO = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -1);
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());

        Integer currentDate = Integer.valueOf(currentDateFormat.format(todaysDate.getTime()));
        Integer daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);
        Integer numberOfDaysLeft = daysInMonth - currentDate + 1;
        todaysDate.add(Calendar.DATE, numberOfDaysLeft);
        final String INTEREST_POSTED_DATE = dateFormat.format(todaysDate.getTime());
        final String CLOSED_ON_DATE = dateFormat.format(Calendar.getInstance().getTime());

        /***
         * Create client for applying Deposit and Savings accounts
         */
        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        /***
         * Create Savings product with CashBased accounting enabled
         */
        final String accountingRule = CASH_BASED;
        final Integer savingsProductID = createSavingsProduct(this.requestSpec, this.responseSpec, MINIMUM_OPENING_BALANCE, accountingRule,
                assetAccount, liabilityAccount, incomeAccount, expenseAccount);
        Assertions.assertNotNull(savingsProductID);

        /***
         * Create Savings account and verify status is pending
         */
        final Integer savingsId = this.savingsAccountHelper.applyForSavingsApplication(clientId, savingsProductID, ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertNotNull(savingsProductID);

        HashMap savingsStatusHashMap = SavingsStatusChecker.getStatusOfSavings(this.requestSpec, this.responseSpec, savingsId);
        SavingsStatusChecker.verifySavingsIsPending(savingsStatusHashMap);

        /***
         * Approve the savings account and verify account is approved
         */
        savingsStatusHashMap = this.savingsAccountHelper.approveSavings(savingsId);
        SavingsStatusChecker.verifySavingsIsApproved(savingsStatusHashMap);

        /***
         * Activate the savings account and verify account is activated
         */
        savingsStatusHashMap = this.savingsAccountHelper.activateSavings(savingsId);
        SavingsStatusChecker.verifySavingsIsActive(savingsStatusHashMap);

        /***
         * Create RD product with CashBased accounting enabled
         */
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule, assetAccount,
                liabilityAccount, incomeAccount, expenseAccount);
        Assertions.assertNotNull(recurringDepositProductId);

        /***
         * Apply for RD account with created product and verify status
         */
        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        /***
         * Approve the RD account and verify whether account is approved
         */
        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsApproved(recurringDepositAccountStatusHashMap);

        /***
         * Activate the RD Account and verify whether account is activated
         */
        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.activateRecurringDeposit(recurringDepositAccountId,
                ACTIVATION_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsActive(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec,
                this.responseSpec, recurringDepositAccountId);
        Float depositAmount = (Float) recurringDepositAccountData.get("mandatoryRecommendedDepositAmount");

        /***
         * Perform Deposit transaction and verify journal entries are posted for the transaction
         */
        Integer depositTransactionId = this.recurringDepositAccountHelper.depositToRecurringDepositAccount(recurringDepositAccountId,
                depositAmount, expectedFirstDepositOnDate);
        Assertions.assertNotNull(depositTransactionId);

        this.journalEntryHelper.checkJournalEntryForAssetAccount(assetAccount, expectedFirstDepositOnDate,
                new JournalEntry(depositAmount, JournalEntry.TransactionType.DEBIT));
        this.journalEntryHelper.checkJournalEntryForLiabilityAccount(liabilityAccount, expectedFirstDepositOnDate,
                new JournalEntry(depositAmount, JournalEntry.TransactionType.CREDIT));

        /***
         * Update interest earned field for RD account
         */
        recurringDepositAccountId = this.recurringDepositAccountHelper.calculateInterestForRecurringDeposit(recurringDepositAccountId);
        Assertions.assertNotNull(recurringDepositAccountId);

        /***
         * Post interest and verify journal entries
         */
        Integer transactionIdForPostInterest = this.recurringDepositAccountHelper
                .postInterestForRecurringDeposit(recurringDepositAccountId);
        Assertions.assertNotNull(transactionIdForPostInterest);

        HashMap accountSummary = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        Float totalInterestPosted = (Float) accountSummary.get("totalInterestPosted");

        final JournalEntry[] expenseAccountEntry = { new JournalEntry(totalInterestPosted, JournalEntry.TransactionType.DEBIT) };
        final JournalEntry[] liablilityAccountEntry = { new JournalEntry(totalInterestPosted, JournalEntry.TransactionType.CREDIT) };
        this.journalEntryHelper.checkJournalEntryForAssetAccount(expenseAccount, INTEREST_POSTED_DATE, expenseAccountEntry);
        this.journalEntryHelper.checkJournalEntryForLiabilityAccount(liabilityAccount, INTEREST_POSTED_DATE, liablilityAccountEntry);

        /***
         * Get saving account balance before preClosing RD account
         */
        HashMap savingsSummaryBefore = this.savingsAccountHelper.getSavingsSummary(savingsId);
        Float balanceBefore = (Float) savingsSummaryBefore.get("accountBalance");

        HashMap recurringDepositPrematureData = this.recurringDepositAccountHelper
                .calculatePrematureAmountForRecurringDeposit(recurringDepositAccountId, CLOSED_ON_DATE);

        /***
         * Retrieve mapped financial account for liability transfer
         */
        Account financialAccount = getMappedLiabilityFinancialAccount();

        /***
         * Preclose the RD account verify whether account is preClosed
         */
        Integer prematureClosureTransactionId = (Integer) this.recurringDepositAccountHelper.prematureCloseForRecurringDeposit(
                recurringDepositAccountId, CLOSED_ON_DATE, CLOSURE_TYPE_TRANSFER_TO_SAVINGS, savingsId,
                CommonConstants.RESPONSE_RESOURCE_ID);
        Assertions.assertNotNull(prematureClosureTransactionId);

        recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker.getStatusOfRecurringDepositAccount(this.requestSpec,
                this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositAccountIsPrematureClosed(recurringDepositAccountStatusHashMap);

        recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec, this.responseSpec,
                recurringDepositAccountId);
        Float maturityAmount = Float.valueOf(recurringDepositAccountData.get("maturityAmount").toString());
        /***
         * Verify journal entry transactions for preclosure transaction As this transaction is an account transfer you
         * should get financial account mapping details and verify amounts
         */
        this.journalEntryHelper.checkJournalEntryForLiabilityAccount(liabilityAccount, CLOSED_ON_DATE,
                new JournalEntry(maturityAmount, JournalEntry.TransactionType.CREDIT),
                new JournalEntry(maturityAmount, JournalEntry.TransactionType.DEBIT));

        this.journalEntryHelper.checkJournalEntryForLiabilityAccount(financialAccount, CLOSED_ON_DATE,
                new JournalEntry(maturityAmount, JournalEntry.TransactionType.DEBIT),
                new JournalEntry(maturityAmount, JournalEntry.TransactionType.CREDIT));
        /***
         * Verify rd account maturity amount and savings account balance
         */
        HashMap recurringDepositData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec, this.responseSpec,
                recurringDepositAccountId);
        Float prematurityAmount = (Float) recurringDepositData.get("maturityAmount");

        HashMap savingsSummaryAfter = this.savingsAccountHelper.getSavingsSummary(savingsId);

        Float balanceAfter = (Float) savingsSummaryAfter.get("accountBalance");
        Float expectedSavingsBalance = balanceBefore + prematurityAmount;

        Assertions.assertTrue(Math.abs(expectedSavingsBalance - balanceAfter) < THRESHOLD,
                "Verifying Savings Account Balance after Premature Closure");

    }

    @Test
    public void testRecurringDepositAccountWithPrematureClosureTypeTransferToSavings_WITH_HOLD_TAX() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.accountHelper = new AccountHelper(this.requestSpec, this.responseSpec);
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        /***
         * Create GL Accounts for product account mapping
         */
        final Account assetAccount = this.accountHelper.createAssetAccount();
        final Account incomeAccount = this.accountHelper.createIncomeAccount();
        final Account expenseAccount = this.accountHelper.createExpenseAccount();
        final Account liabilityAccount = this.accountHelper.createLiabilityAccount();
        final Account liabilityAccountForTax = this.accountHelper.createLiabilityAccount();

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);
        DateFormat currentDateFormat = new SimpleDateFormat("dd");

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -3);
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.YEAR, 10);
        final String VALID_TO = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -1);
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());

        Integer currentDate = Integer.valueOf(currentDateFormat.format(todaysDate.getTime()));
        Integer daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);
        Integer numberOfDaysLeft = daysInMonth - currentDate + 1;
        todaysDate.add(Calendar.DATE, numberOfDaysLeft);
        final String INTEREST_POSTED_DATE = dateFormat.format(todaysDate.getTime());
        final String CLOSED_ON_DATE = dateFormat.format(Calendar.getInstance().getTime());

        /***
         * Create client for applying Deposit and Savings accounts
         */
        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        /***
         * Create Savings product with CashBased accounting enabled
         */
        final String accountingRule = CASH_BASED;
        final Integer savingsProductID = createSavingsProduct(this.requestSpec, this.responseSpec, MINIMUM_OPENING_BALANCE, accountingRule,
                assetAccount, liabilityAccount, incomeAccount, expenseAccount);
        Assertions.assertNotNull(savingsProductID);

        /***
         * Create Savings account and verify status is pending
         */
        final Integer savingsId = this.savingsAccountHelper.applyForSavingsApplication(clientId, savingsProductID, ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertNotNull(savingsProductID);

        HashMap savingsStatusHashMap = SavingsStatusChecker.getStatusOfSavings(this.requestSpec, this.responseSpec, savingsId);
        SavingsStatusChecker.verifySavingsIsPending(savingsStatusHashMap);

        /***
         * Approve the savings account and verify account is approved
         */
        savingsStatusHashMap = this.savingsAccountHelper.approveSavings(savingsId);
        SavingsStatusChecker.verifySavingsIsApproved(savingsStatusHashMap);

        /***
         * Activate the savings account and verify account is activated
         */
        savingsStatusHashMap = this.savingsAccountHelper.activateSavings(savingsId);
        SavingsStatusChecker.verifySavingsIsActive(savingsStatusHashMap);

        /***
         * Create RD product with CashBased accounting enabled
         */
        final Integer taxGroupId = createTaxGroup("10", liabilityAccountForTax);
        Integer recurringDepositProductId = createRecurringDepositProductWithWithHoldTax(VALID_FROM, VALID_TO, String.valueOf(taxGroupId),
                accountingRule, assetAccount, liabilityAccount, incomeAccount, expenseAccount);
        Assertions.assertNotNull(recurringDepositProductId);

        /***
         * Apply for RD account with created product and verify status
         */
        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        /***
         * Approve the RD account and verify whether account is approved
         */
        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsApproved(recurringDepositAccountStatusHashMap);

        /***
         * Activate the RD Account and verify whether account is activated
         */
        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.activateRecurringDeposit(recurringDepositAccountId,
                ACTIVATION_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsActive(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec,
                this.responseSpec, recurringDepositAccountId);
        Float depositAmount = (Float) recurringDepositAccountData.get("mandatoryRecommendedDepositAmount");

        /***
         * Perform Deposit transaction and verify journal entries are posted for the transaction
         */
        Integer depositTransactionId = this.recurringDepositAccountHelper.depositToRecurringDepositAccount(recurringDepositAccountId,
                depositAmount, expectedFirstDepositOnDate);
        Assertions.assertNotNull(depositTransactionId);

        this.journalEntryHelper.checkJournalEntryForAssetAccount(assetAccount, expectedFirstDepositOnDate,
                new JournalEntry(depositAmount, JournalEntry.TransactionType.DEBIT));
        this.journalEntryHelper.checkJournalEntryForLiabilityAccount(liabilityAccount, expectedFirstDepositOnDate,
                new JournalEntry(depositAmount, JournalEntry.TransactionType.CREDIT));

        /***
         * Update interest earned field for RD account
         */
        recurringDepositAccountId = this.recurringDepositAccountHelper.calculateInterestForRecurringDeposit(recurringDepositAccountId);
        Assertions.assertNotNull(recurringDepositAccountId);

        /***
         * Post interest and verify journal entries
         */
        Integer transactionIdForPostInterest = this.recurringDepositAccountHelper
                .postInterestForRecurringDeposit(recurringDepositAccountId);
        Assertions.assertNotNull(transactionIdForPostInterest);

        HashMap accountSummary = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        Float totalInterestPosted = (Float) accountSummary.get("totalInterestPosted");
        Assertions.assertNull(accountSummary.get("totalWithholdTax"));

        final JournalEntry[] expenseAccountEntry = { new JournalEntry(totalInterestPosted, JournalEntry.TransactionType.DEBIT) };
        final JournalEntry[] liablilityAccountEntry = { new JournalEntry(totalInterestPosted, JournalEntry.TransactionType.CREDIT) };
        this.journalEntryHelper.checkJournalEntryForAssetAccount(expenseAccount, INTEREST_POSTED_DATE, expenseAccountEntry);
        this.journalEntryHelper.checkJournalEntryForLiabilityAccount(liabilityAccount, INTEREST_POSTED_DATE, liablilityAccountEntry);

        /***
         * Get saving account balance before preClosing RD account
         */
        HashMap savingsSummaryBefore = this.savingsAccountHelper.getSavingsSummary(savingsId);
        Float balanceBefore = (Float) savingsSummaryBefore.get("accountBalance");

        HashMap recurringDepositPrematureData = this.recurringDepositAccountHelper
                .calculatePrematureAmountForRecurringDeposit(recurringDepositAccountId, CLOSED_ON_DATE);

        /***
         * Retrieve mapped financial account for liability transfer
         */
        Account financialAccount = getMappedLiabilityFinancialAccount();

        /***
         * Preclose the RD account verify whether account is preClosed
         */
        Integer prematureClosureTransactionId = (Integer) this.recurringDepositAccountHelper.prematureCloseForRecurringDeposit(
                recurringDepositAccountId, CLOSED_ON_DATE, CLOSURE_TYPE_TRANSFER_TO_SAVINGS, savingsId,
                CommonConstants.RESPONSE_RESOURCE_ID);
        Assertions.assertNotNull(prematureClosureTransactionId);

        recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker.getStatusOfRecurringDepositAccount(this.requestSpec,
                this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositAccountIsPrematureClosed(recurringDepositAccountStatusHashMap);

        recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec, this.responseSpec,
                recurringDepositAccountId);
        Float maturityAmount = Float.valueOf(recurringDepositAccountData.get("maturityAmount").toString());
        /***
         * Verify journal entry transactions for preclosure transaction As this transaction is an account transfer you
         * should get financial account mapping details and verify amounts
         */
        this.journalEntryHelper.checkJournalEntryForLiabilityAccount(liabilityAccount, CLOSED_ON_DATE,
                new JournalEntry(maturityAmount, JournalEntry.TransactionType.CREDIT),
                new JournalEntry(maturityAmount, JournalEntry.TransactionType.DEBIT));

        this.journalEntryHelper.checkJournalEntryForLiabilityAccount(financialAccount, CLOSED_ON_DATE,
                new JournalEntry(maturityAmount, JournalEntry.TransactionType.DEBIT),
                new JournalEntry(maturityAmount, JournalEntry.TransactionType.CREDIT));
        /***
         * Verify rd account maturity amount and savings account balance
         */
        HashMap recurringDepositData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec, this.responseSpec,
                recurringDepositAccountId);
        Float prematurityAmount = (Float) recurringDepositData.get("maturityAmount");
        HashMap summary = (HashMap) recurringDepositData.get("summary");
        Assertions.assertNotNull(summary.get("totalWithholdTax"));
        Float withHoldTax = (Float) summary.get("totalWithholdTax");
        this.journalEntryHelper.checkJournalEntryForLiabilityAccount(liabilityAccountForTax, CLOSED_ON_DATE,
                new JournalEntry(withHoldTax, JournalEntry.TransactionType.CREDIT));

        HashMap savingsSummaryAfter = this.savingsAccountHelper.getSavingsSummary(savingsId);

        Float balanceAfter = (Float) savingsSummaryAfter.get("accountBalance");
        Float expectedSavingsBalance = balanceBefore + prematurityAmount;

        Assertions.assertTrue(Math.abs(expectedSavingsBalance - balanceAfter) < THRESHOLD,
                "Verifying Savings Account Balance after Premature Closure");

    }

    @Test
    @Disabled // TODO FINERACT-1248
    public void testRecurringDepositAccountWithClosureTypeTransferToSavings_WITH_HOLD_TAX() throws InterruptedException {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.accountHelper = new AccountHelper(this.requestSpec, this.responseSpec);
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        /***
         * Create GL Accounts for product account mapping
         */
        final Account assetAccount = this.accountHelper.createAssetAccount();
        final Account incomeAccount = this.accountHelper.createIncomeAccount();
        final Account expenseAccount = this.accountHelper.createExpenseAccount();
        final Account liabilityAccount = this.accountHelper.createLiabilityAccount();
        final Account liabilityAccountForTax = this.accountHelper.createLiabilityAccount();

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);
        DateFormat currentDateFormat = new SimpleDateFormat("dd");

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -20);
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.YEAR, 10);
        final String VALID_TO = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -20);
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());

        Integer currentDate = Integer.valueOf(currentDateFormat.format(todaysDate.getTime()));
        Integer daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);
        Integer numberOfDaysLeft = daysInMonth - currentDate + 1;
        todaysDate.add(Calendar.DATE, numberOfDaysLeft);
        final String INTEREST_POSTED_DATE = dateFormat.format(todaysDate.getTime());
        Calendar closedOn = Calendar.getInstance();
        closedOn.add(Calendar.MONTH, -6);
        final String CLOSED_ON_DATE = dateFormat.format(closedOn.getTime());

        /***
         * Create client for applying Deposit and Savings accounts
         */
        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        /***
         * Create Savings product with CashBased accounting enabled
         */
        final String accountingRule = CASH_BASED;
        final Integer savingsProductID = createSavingsProduct(this.requestSpec, this.responseSpec, MINIMUM_OPENING_BALANCE, accountingRule,
                assetAccount, liabilityAccount, incomeAccount, expenseAccount);
        Assertions.assertNotNull(savingsProductID);

        /***
         * Create Savings account and verify status is pending
         */
        final Integer savingsId = this.savingsAccountHelper.applyForSavingsApplication(clientId, savingsProductID, ACCOUNT_TYPE_INDIVIDUAL);
        Assertions.assertNotNull(savingsProductID);

        HashMap savingsStatusHashMap = SavingsStatusChecker.getStatusOfSavings(this.requestSpec, this.responseSpec, savingsId);
        SavingsStatusChecker.verifySavingsIsPending(savingsStatusHashMap);

        /***
         * Approve the savings account and verify account is approved
         */
        savingsStatusHashMap = this.savingsAccountHelper.approveSavings(savingsId);
        SavingsStatusChecker.verifySavingsIsApproved(savingsStatusHashMap);

        /***
         * Activate the savings account and verify account is activated
         */
        savingsStatusHashMap = this.savingsAccountHelper.activateSavings(savingsId);
        SavingsStatusChecker.verifySavingsIsActive(savingsStatusHashMap);

        /***
         * Create RD product with CashBased accounting enabled
         */
        final Integer taxGroupId = createTaxGroup("10", liabilityAccountForTax);
        Integer recurringDepositProductId = createRecurringDepositProductWithWithHoldTax(VALID_FROM, VALID_TO, String.valueOf(taxGroupId),
                accountingRule, assetAccount, liabilityAccount, incomeAccount, expenseAccount);
        Assertions.assertNotNull(recurringDepositProductId);

        /***
         * Apply for RD account with created product and verify status
         */
        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        /***
         * Approve the RD account and verify whether account is approved
         */
        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsApproved(recurringDepositAccountStatusHashMap);

        /***
         * Activate the RD Account and verify whether account is activated
         */
        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.activateRecurringDeposit(recurringDepositAccountId,
                ACTIVATION_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsActive(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec,
                this.responseSpec, recurringDepositAccountId);
        Float depositAmount = (Float) recurringDepositAccountData.get("mandatoryRecommendedDepositAmount");

        /***
         * Perform Deposit transaction and verify journal entries are posted for the transaction
         */
        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -20);

        for (int i = 0; i < 14; i++) {
            Integer depositTransactionId = this.recurringDepositAccountHelper.depositToRecurringDepositAccount(recurringDepositAccountId,
                    depositAmount, dateFormat.format(todaysDate.getTime()));
            Assertions.assertNotNull(depositTransactionId);

            this.journalEntryHelper.checkJournalEntryForAssetAccount(assetAccount, dateFormat.format(todaysDate.getTime()),
                    new JournalEntry(depositAmount, JournalEntry.TransactionType.DEBIT));
            this.journalEntryHelper.checkJournalEntryForLiabilityAccount(liabilityAccount, dateFormat.format(todaysDate.getTime()),
                    new JournalEntry(depositAmount, JournalEntry.TransactionType.CREDIT));
            todaysDate.add(Calendar.MONTH, 1);
        }

        /***
         * FD account verify whether account is matured
         */

        SchedulerJobHelper schedulerJobHelper = new SchedulerJobHelper(requestSpec);
        String JobName = "Update Deposit Accounts Maturity details";
        schedulerJobHelper.executeAndAwaitJob(JobName);

        HashMap accountDetails = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec, this.responseSpec,
                recurringDepositAccountId);

        HashMap summary = (HashMap) accountDetails.get("summary");
        Assertions.assertNotNull(summary.get("totalWithholdTax"));
        Float withHoldTax = (Float) summary.get("totalWithholdTax");
        this.journalEntryHelper.checkJournalEntryForLiabilityAccount(liabilityAccountForTax, CLOSED_ON_DATE,
                new JournalEntry(withHoldTax, JournalEntry.TransactionType.CREDIT));

        recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker.getStatusOfRecurringDepositAccount(this.requestSpec,
                this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositAccountIsMatured(recurringDepositAccountStatusHashMap);

    }

    /***
     * Test case for Recurring Deposit Account premature closure with transaction type ReInvest and Cash Based
     * accounting enabled
     */
    @Test
    public void testRecurringDepositAccountWithPrematureClosureTypeReinvest() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.accountHelper = new AccountHelper(this.requestSpec, this.responseSpec);
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        RecurringDepositAccountHelper recurringDepositAccountHelperValidationError = new RecurringDepositAccountHelper(this.requestSpec,
                new ResponseSpecBuilder().build());

        /***
         * Create GL Accounts for product account mapping
         */
        final Account assetAccount = this.accountHelper.createAssetAccount();
        final Account incomeAccount = this.accountHelper.createIncomeAccount();
        final Account expenseAccount = this.accountHelper.createExpenseAccount();
        final Account liabilityAccount = this.accountHelper.createLiabilityAccount();

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);
        DateFormat currentDateFormat = new SimpleDateFormat("dd");

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -3);
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.YEAR, 10);
        final String VALID_TO = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -1);
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());

        Integer currentDate = Integer.valueOf(currentDateFormat.format(todaysDate.getTime()));
        Integer daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);
        Integer numberOfDaysLeft = daysInMonth - currentDate + 1;
        todaysDate.add(Calendar.DATE, numberOfDaysLeft);
        final String INTEREST_POSTED_DATE = dateFormat.format(todaysDate.getTime());
        final String CLOSED_ON_DATE = dateFormat.format(Calendar.getInstance().getTime());

        /***
         * Create client for applying Deposit account
         */
        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        /***
         * Create RD product with CashBased accounting enabled
         */
        final String accountingRule = CASH_BASED;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule, assetAccount,
                liabilityAccount, incomeAccount, expenseAccount);
        Assertions.assertNotNull(recurringDepositProductId);

        ArrayList<HashMap> allRecurringDepositProductsData = RecurringDepositProductHelper
                .retrieveAllRecurringDepositProducts(this.requestSpec, this.responseSpec);
        HashMap recurringDepositProductData = RecurringDepositProductHelper.retrieveRecurringDepositProductById(this.requestSpec,
                this.responseSpec, recurringDepositProductId.toString());

        /***
         * Apply for RD account with created product and verify status
         */
        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        /***
         * Approve the RD account and verify whether account is approved
         */
        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsApproved(recurringDepositAccountStatusHashMap);

        /***
         * Activate the RD Account and verify whether account is activated
         */
        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.activateRecurringDeposit(recurringDepositAccountId,
                ACTIVATION_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsActive(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec,
                this.responseSpec, recurringDepositAccountId);
        Float depositAmount = (Float) recurringDepositAccountData.get("mandatoryRecommendedDepositAmount");

        /***
         * Perform Deposit transaction and verify journal entries are posted for the transaction
         */
        Integer depositTransactionId = this.recurringDepositAccountHelper.depositToRecurringDepositAccount(recurringDepositAccountId,
                DEPOSIT_AMOUNT, expectedFirstDepositOnDate);
        Assertions.assertNotNull(depositTransactionId);

        this.journalEntryHelper.checkJournalEntryForAssetAccount(assetAccount, expectedFirstDepositOnDate,
                new JournalEntry(depositAmount, JournalEntry.TransactionType.DEBIT));
        this.journalEntryHelper.checkJournalEntryForLiabilityAccount(liabilityAccount, expectedFirstDepositOnDate,
                new JournalEntry(depositAmount, JournalEntry.TransactionType.CREDIT));

        /***
         * Update interest earned field for RD account
         */
        recurringDepositAccountId = this.recurringDepositAccountHelper.calculateInterestForRecurringDeposit(recurringDepositAccountId);
        Assertions.assertNotNull(recurringDepositAccountId);

        /***
         * Post interest and verify journal entries
         */
        Integer transactionIdForPostInterest = this.recurringDepositAccountHelper
                .postInterestForRecurringDeposit(recurringDepositAccountId);
        Assertions.assertNotNull(transactionIdForPostInterest);

        HashMap accountSummary = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        Float totalInterestPosted = (Float) accountSummary.get("totalInterestPosted");

        final JournalEntry[] expenseAccountEntry = { new JournalEntry(totalInterestPosted, JournalEntry.TransactionType.DEBIT) };
        final JournalEntry[] liablilityAccountEntry = { new JournalEntry(totalInterestPosted, JournalEntry.TransactionType.CREDIT) };
        this.journalEntryHelper.checkJournalEntryForAssetAccount(expenseAccount, INTEREST_POSTED_DATE, expenseAccountEntry);
        this.journalEntryHelper.checkJournalEntryForLiabilityAccount(liabilityAccount, INTEREST_POSTED_DATE, liablilityAccountEntry);

        /***
         * Calculate expected premature closure amount
         */
        HashMap recurringDepositPrematureData = this.recurringDepositAccountHelper
                .calculatePrematureAmountForRecurringDeposit(recurringDepositAccountId, CLOSED_ON_DATE);

        /***
         * Expected to get an error response from api because re-invest option is not supported for account preClosure
         */
        ArrayList<HashMap> errorResponse = (ArrayList<HashMap>) recurringDepositAccountHelperValidationError
                .prematureCloseForRecurringDeposit(recurringDepositAccountId, CLOSED_ON_DATE, CLOSURE_TYPE_REINVEST, null,
                        CommonConstants.RESPONSE_ERROR);

        assertEquals("validation.msg.recurringdepositaccount.onAccountClosureId.reinvest.not.allowed",
                errorResponse.get(0).get(CommonConstants.RESPONSE_ERROR_MESSAGE_CODE));

    }

    /***
     * Test case for Update Recurring Deposit Account details
     */
    @Test
    public void testRecurringDepositAccountUpdation() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -3);
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.YEAR, 10);
        final String VALID_TO = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -1);
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());

        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        final String accountingRule = NONE;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule);
        Assertions.assertNotNull(recurringDepositProductId);

        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        todaysDate.add(Calendar.DATE, -1);

        HashMap modificationsHashMap = this.recurringDepositAccountHelper.updateRecurringDepositAccount(clientId.toString(),
                recurringDepositProductId.toString(), recurringDepositAccountId.toString(), VALID_FROM, VALID_TO, WHOLE_TERM,
                SUBMITTED_ON_DATE);
        Assertions.assertTrue(modificationsHashMap.containsKey("submittedOnDate"));

    }

    /***
     * Test case for Approve and Undo Approval of Recurring Deposit Account
     */
    @Test
    public void testRecurringDepositAccountUndoApproval() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -3);
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.YEAR, 10);
        final String VALID_TO = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -1);
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());

        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        final String accountingRule = NONE;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule);
        Assertions.assertNotNull(recurringDepositProductId);

        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsApproved(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.undoApproval(recurringDepositAccountId);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);
    }

    /***
     * Test case for Closure of Recurring Deposit Account(Reject Application)
     */
    @Test
    public void testRecurringDepositAccountRejectedAndClosed() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.accountHelper = new AccountHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -3);
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.YEAR, 10);
        final String VALID_TO = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -1);
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String REJECTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());

        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        final String accountingRule = NONE;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule);
        Assertions.assertNotNull(recurringDepositProductId);

        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.rejectApplication(recurringDepositAccountId,
                REJECTED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsRejected(recurringDepositAccountStatusHashMap);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositAccountIsClosed(recurringDepositAccountStatusHashMap);
    }

    /***
     * Test case for Closure of Recurring Deposit Account(Withdrawn by applicant)
     */
    @Test
    public void testRecurringDepositAccountWithdrawnByClientAndClosed() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.accountHelper = new AccountHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -3);
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.YEAR, 10);
        final String VALID_TO = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -1);
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String WITHDRAWN_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());

        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        final String accountingRule = NONE;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule);
        Assertions.assertNotNull(recurringDepositProductId);

        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.withdrawApplication(recurringDepositAccountId,
                WITHDRAWN_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsWithdrawn(recurringDepositAccountStatusHashMap);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositAccountIsClosed(recurringDepositAccountStatusHashMap);
    }

    /***
     * Test case for Delete of Recurring Deposit Account.
     */
    @Test
    public void testRecurringDepositAccountIsDeleted() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -3);
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.YEAR, 10);
        final String VALID_TO = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -1);
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());

        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        final String accountingRule = NONE;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule);
        Assertions.assertNotNull(recurringDepositProductId);

        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        recurringDepositAccountId = (Integer) this.recurringDepositAccountHelper
                .deleteRecurringDepositApplication(recurringDepositAccountId, "resourceId");
        Assertions.assertNotNull(recurringDepositAccountId);
    }

    /***
     * Test case for update Recurring deposit account transactions
     */
    @Test
    public void testUpdateAndUndoTransactionForRecurringDepositAccount() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.accountHelper = new AccountHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        /***
         * Create GL Accounts for product account mapping
         */
        final Account assetAccount = this.accountHelper.createAssetAccount();
        final Account incomeAccount = this.accountHelper.createIncomeAccount();
        final Account expenseAccount = this.accountHelper.createExpenseAccount();
        final Account liabilityAccount = this.accountHelper.createLiabilityAccount();

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -3);
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.YEAR, 10);
        final String VALID_TO = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -1);
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.MONTH, 1);
        final String DEPOSIT_DATE = dateFormat.format(todaysDate.getTime());

        /***
         * Create client for applying Deposit account
         */
        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        /***
         * Create RD product with CashBased accounting enabled
         */
        final String accountingRule = CASH_BASED;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule, assetAccount,
                liabilityAccount, incomeAccount, expenseAccount);
        Assertions.assertNotNull(recurringDepositProductId);

        /***
         * Apply for RD account with created product and verify status
         */
        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        /***
         * Approve the RD account and verify whether account is approved
         */
        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsApproved(recurringDepositAccountStatusHashMap);

        /***
         * Activate the RD Account and verify whether account is activated
         */
        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.activateRecurringDeposit(recurringDepositAccountId,
                ACTIVATION_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsActive(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositSummaryBefore = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        Float balanceBefore = (Float) recurringDepositSummaryBefore.get("accountBalance");

        /***
         * Perform Deposit transaction and verify journal entries are posted for the transaction
         */
        Integer transactionIdForDeposit = this.recurringDepositAccountHelper.depositToRecurringDepositAccount(recurringDepositAccountId,
                DEPOSIT_AMOUNT, DEPOSIT_DATE);
        Assertions.assertNotNull(transactionIdForDeposit);

        this.journalEntryHelper.checkJournalEntryForAssetAccount(assetAccount, DEPOSIT_DATE,
                new JournalEntry(DEPOSIT_AMOUNT, JournalEntry.TransactionType.DEBIT));
        this.journalEntryHelper.checkJournalEntryForLiabilityAccount(liabilityAccount, DEPOSIT_DATE,
                new JournalEntry(DEPOSIT_AMOUNT, JournalEntry.TransactionType.CREDIT));

        /***
         * verify account balances after transactions
         */
        Float expectedBalanceAfter = balanceBefore + DEPOSIT_AMOUNT;
        HashMap recurringDepositSummaryAfter = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        Float balanceAfter = (Float) recurringDepositSummaryAfter.get("accountBalance");

        Assertions.assertEquals(expectedBalanceAfter, balanceAfter, "Verifying account balance after deposit");

        /***
         * Update transaction and verify account balance after transaction
         */
        Float updatedTransactionAmount = DEPOSIT_AMOUNT - 1000.0f;
        Integer updateTransactionId = this.recurringDepositAccountHelper.updateTransactionForRecurringDeposit(recurringDepositAccountId,
                transactionIdForDeposit, DEPOSIT_DATE, updatedTransactionAmount);
        Assertions.assertNotNull(updateTransactionId);

        this.journalEntryHelper.checkJournalEntryForAssetAccount(assetAccount, DEPOSIT_DATE,
                new JournalEntry(updatedTransactionAmount, JournalEntry.TransactionType.DEBIT));
        this.journalEntryHelper.checkJournalEntryForLiabilityAccount(liabilityAccount, DEPOSIT_DATE,
                new JournalEntry(updatedTransactionAmount, JournalEntry.TransactionType.CREDIT));

        expectedBalanceAfter = DEPOSIT_AMOUNT - updatedTransactionAmount;
        recurringDepositSummaryAfter = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        balanceAfter = (Float) recurringDepositSummaryAfter.get("accountBalance");

        Assertions.assertEquals(expectedBalanceAfter, balanceAfter, "Verifying account balance after updating Transaction");

        Integer undoTransactionId = this.recurringDepositAccountHelper.undoTransactionForRecurringDeposit(recurringDepositAccountId,
                updateTransactionId, DEPOSIT_DATE, 0.0f);
        Assertions.assertNotNull(undoTransactionId);

        expectedBalanceAfter = expectedBalanceAfter - updatedTransactionAmount;
        recurringDepositSummaryAfter = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        balanceAfter = (Float) recurringDepositSummaryAfter.get("accountBalance");

        Assertions.assertEquals(expectedBalanceAfter, balanceAfter, "Verifying account balance after Undo Transaction");

    }

    /***
     * Test case for verify maturity amount with monthly compounding and monthly posting with 365 days in year
     */
    @Test
    public void testMaturityAmountForMonthlyCompoundingAndMonthlyPosting_With_365_Days() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);
        DateFormat currentDateFormat = new SimpleDateFormat("dd");

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -3);
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.YEAR, 10);
        final String VALID_TO = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -1);
        Integer currentDate = Integer.valueOf(currentDateFormat.format(todaysDate.getTime()));
        todaysDate.add(Calendar.DATE, -(currentDate - 1));
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());

        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        final String accountingRule = NONE;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule);
        Assertions.assertNotNull(recurringDepositProductId);

        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsApproved(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec,
                this.responseSpec, recurringDepositAccountId);
        Float depositAmount = (Float) recurringDepositAccountData.get("mandatoryRecommendedDepositAmount");
        Float maturityAmount = (Float) recurringDepositAccountData.get("maturityAmount");
        Integer depositPeriod = (Integer) recurringDepositAccountData.get("depositPeriod");
        HashMap daysInYearMap = (HashMap) recurringDepositAccountData.get("interestCalculationDaysInYearType");
        Integer daysInYear = (Integer) daysInYearMap.get("id");
        ArrayList<ArrayList<HashMap>> interestRateChartData = RecurringDepositProductHelper
                .getInterestRateChartSlabsByProductId(this.requestSpec, this.responseSpec, recurringDepositProductId);

        HashMap recurringDepositSummary = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        Float principal = (Float) recurringDepositSummary.get("accountBalance");

        Float interestRate = RecurringDepositAccountHelper.getInterestRate(interestRateChartData, depositPeriod);
        double interestRateInFraction = interestRate / 100;
        double perDay = (double) 1 / daysInYear;
        LOG.info("per day = {}", perDay);
        double interestPerDay = interestRateInFraction * perDay;

        Integer daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);

        principal = RecurringDepositAccountHelper.getPrincipalAfterCompoundingInterest(todaysDate, principal, depositAmount, depositPeriod,
                interestPerDay, MONTHLY_INTERVAL, MONTHLY_INTERVAL);

        LOG.info("{}", principal.toString());
        Assertions.assertTrue(Math.abs(principal - maturityAmount) < THRESHOLD, "Verifying Maturity amount for Recurring Deposit Account");
    }

    /***
     * Test case for verify maturity amount with monthly compounding and monthly posting with 360 days in year
     */
    @Test
    public void testMaturityAmountForMonthlyCompoundingAndMonthlyPosting_With_360_Days() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);
        DateFormat currentDateFormat = new SimpleDateFormat("dd");

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -3);
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.YEAR, 10);
        final String VALID_TO = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -1);
        Integer currentDate = Integer.valueOf(currentDateFormat.format(todaysDate.getTime()));
        todaysDate.add(Calendar.DATE, -(currentDate - 1));
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());

        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        final String accountingRule = NONE;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule);
        Assertions.assertNotNull(recurringDepositProductId);

        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap modificationsHashMap = this.recurringDepositAccountHelper.updateInterestCalculationConfigForRecurringDeposit(
                clientId.toString(), recurringDepositProductId.toString(), recurringDepositAccountId.toString(), SUBMITTED_ON_DATE,
                VALID_FROM, VALID_TO, DAYS_360, WHOLE_TERM, INTEREST_CALCULATION_USING_DAILY_BALANCE, MONTHLY, MONTHLY,
                expectedFirstDepositOnDate);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsApproved(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec,
                this.responseSpec, recurringDepositAccountId);
        Float depositAmount = (Float) recurringDepositAccountData.get("mandatoryRecommendedDepositAmount");
        Float maturityAmount = (Float) recurringDepositAccountData.get("maturityAmount");
        Integer depositPeriod = (Integer) recurringDepositAccountData.get("depositPeriod");
        HashMap daysInYearMap = (HashMap) recurringDepositAccountData.get("interestCalculationDaysInYearType");
        Integer daysInYear = (Integer) daysInYearMap.get("id");
        ArrayList<ArrayList<HashMap>> interestRateChartData = RecurringDepositProductHelper
                .getInterestRateChartSlabsByProductId(this.requestSpec, this.responseSpec, recurringDepositProductId);

        HashMap recurringDepositSummary = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        Float principal = (Float) recurringDepositSummary.get("accountBalance");

        Float interestRate = RecurringDepositAccountHelper.getInterestRate(interestRateChartData, depositPeriod);
        double interestRateInFraction = interestRate / 100;
        double perDay = (double) 1 / daysInYear;
        LOG.info("per day = {}", perDay);
        double interestPerDay = interestRateInFraction * perDay;

        Integer daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);

        principal = RecurringDepositAccountHelper.getPrincipalAfterCompoundingInterest(todaysDate, principal, depositAmount, depositPeriod,
                interestPerDay, MONTHLY_INTERVAL, MONTHLY_INTERVAL);

        LOG.info("{}", principal.toString());
        Assertions.assertTrue(Math.abs(principal - maturityAmount) < THRESHOLD, "Verifying Maturity amount for Recurring Deposit Account");
    }

    /***
     * Test case for verify interest posting of RD account
     */
    @Test
    public void testPostInterestForRecurringDeposit() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);
        DateFormat currentDateFormat = new SimpleDateFormat("dd");

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -3);
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.YEAR, 10);
        final String VALID_TO = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -1);
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());

        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        final String accountingRule = NONE;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule);
        Assertions.assertNotNull(recurringDepositProductId);

        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsApproved(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.activateRecurringDeposit(recurringDepositAccountId,
                ACTIVATION_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsActive(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec,
                this.responseSpec, recurringDepositAccountId);
        Integer depositPeriod = (Integer) recurringDepositAccountData.get("depositPeriod");
        HashMap daysInYearMap = (HashMap) recurringDepositAccountData.get("interestCalculationDaysInYearType");
        Integer daysInYear = (Integer) daysInYearMap.get("id");
        ArrayList<ArrayList<HashMap>> interestRateChartData = RecurringDepositProductHelper
                .getInterestRateChartSlabsByProductId(this.requestSpec, this.responseSpec, recurringDepositProductId);

        Integer depositTransactionId = this.recurringDepositAccountHelper.depositToRecurringDepositAccount(recurringDepositAccountId,
                DEPOSIT_AMOUNT, expectedFirstDepositOnDate);
        Assertions.assertNotNull(depositTransactionId);

        HashMap recurringDepositSummary = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        Float principal = (Float) recurringDepositSummary.get("totalDeposits");

        Float interestRate = RecurringDepositAccountHelper.getInterestRate(interestRateChartData, depositPeriod);
        double interestRateInFraction = interestRate / 100;
        double perDay = (double) 1 / daysInYear;
        LOG.info("per day = {}", perDay);
        double interestPerDay = interestRateInFraction * perDay;

        Integer currentDate = Integer.valueOf(currentDateFormat.format(todaysDate.getTime()));
        Integer daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);
        daysInMonth = daysInMonth - currentDate + 1;
        Float interestToBePosted = (float) (interestPerDay * principal * daysInMonth);
        principal += interestToBePosted;

        Float expectedBalanceAfter = principal;
        LOG.info("{}", expectedBalanceAfter.toString());

        Integer transactionIdForPostInterest = this.recurringDepositAccountHelper
                .postInterestForRecurringDeposit(recurringDepositAccountId);
        Assertions.assertNotNull(transactionIdForPostInterest);

        HashMap recurringDepositAccountSummary = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        Float interestAmountPosted = (Float) recurringDepositAccountSummary.get("totalInterestPosted");
        Float principalAfter = (Float) recurringDepositAccountSummary.get("accountBalance");

        Assertions.assertTrue(Math.abs(interestToBePosted - interestAmountPosted) < THRESHOLD,
                "Verifying Amount of Interest Posted to Recurring Deposit Account");
        Assertions.assertTrue(Math.abs(expectedBalanceAfter - principalAfter) < THRESHOLD,
                "Verifying Principal Amount after Interest Posting");

    }

    /***
     * Test case for verify premature closure amount with penal interest for whole term with closure transaction type
     * withdrawal and 365 days in year
     */
    @Test
    public void testPrematureClosureAmountWithPenalInterestForWholeTerm_With_365_Days() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.accountHelper = new AccountHelper(this.requestSpec, this.responseSpec);
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);
        DateFormat currentDateFormat = new SimpleDateFormat("dd");

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -3);
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.YEAR, 10);
        final String VALID_TO = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -1);
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.MONTH, 1);
        final String CLOSED_ON_DATE = dateFormat.format(todaysDate.getTime());

        final Account assetAccount = this.accountHelper.createAssetAccount();
        final Account incomeAccount = this.accountHelper.createIncomeAccount();
        final Account expenseAccount = this.accountHelper.createExpenseAccount();
        final Account liabilityAccount = this.accountHelper.createLiabilityAccount();

        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        final String accountingRule = NONE;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule);
        Assertions.assertNotNull(recurringDepositProductId);

        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsApproved(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.activateRecurringDeposit(recurringDepositAccountId,
                ACTIVATION_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsActive(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec,
                this.responseSpec, recurringDepositAccountId);
        Float depositAmount = (Float) recurringDepositAccountData.get("mandatoryRecommendedDepositAmount");
        Integer depositPeriod = (Integer) recurringDepositAccountData.get("depositPeriod");
        HashMap daysInYearMap = (HashMap) recurringDepositAccountData.get("interestCalculationDaysInYearType");
        Float preClosurePenalInterestRate = (Float) recurringDepositAccountData.get("preClosurePenalInterest");
        Integer daysInYear = (Integer) daysInYearMap.get("id");
        ArrayList<ArrayList<HashMap>> interestRateChartData = RecurringDepositProductHelper
                .getInterestRateChartSlabsByProductId(this.requestSpec, this.responseSpec, recurringDepositProductId);

        Integer depositTransactionId = this.recurringDepositAccountHelper.depositToRecurringDepositAccount(recurringDepositAccountId,
                DEPOSIT_AMOUNT, expectedFirstDepositOnDate);
        Assertions.assertNotNull(depositTransactionId);

        HashMap recurringDepositSummary = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        Float principal = (Float) recurringDepositSummary.get("totalDeposits");

        Float interestRate = RecurringDepositAccountHelper.getInterestRate(interestRateChartData, depositPeriod);
        interestRate -= preClosurePenalInterestRate;
        double interestRateInFraction = interestRate / 100;
        double perDay = (double) 1 / daysInYear;
        LOG.info("per day = {}", perDay);
        double interestPerDay = interestRateInFraction * perDay;

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        Integer currentDate = Integer.valueOf(currentDateFormat.format(calendar.getTime()));
        Integer daysInMonth = calendar.getActualMaximum(Calendar.DATE);
        daysInMonth = daysInMonth - currentDate + 1;
        Float interestPerMonth = (float) (interestPerDay * principal * daysInMonth);
        principal += interestPerMonth + depositAmount;
        calendar.add(Calendar.DATE, daysInMonth);
        LOG.info("{}", monthDayFormat.format(calendar.getTime()));

        expectedFirstDepositOnDate = dateFormat.format(calendar.getTime());
        Integer transactionIdForDeposit = this.recurringDepositAccountHelper.depositToRecurringDepositAccount(recurringDepositAccountId,
                DEPOSIT_AMOUNT, expectedFirstDepositOnDate);
        Assertions.assertNotNull(transactionIdForDeposit);

        currentDate = currentDate - 1;
        interestPerMonth = (float) (interestPerDay * principal * currentDate);
        LOG.info("IPM = {}", interestPerMonth);
        principal += interestPerMonth;
        LOG.info("principal = {}", principal);

        HashMap recurringDepositPrematureData = this.recurringDepositAccountHelper
                .calculatePrematureAmountForRecurringDeposit(recurringDepositAccountId, CLOSED_ON_DATE);

        Integer prematureClosureTransactionId = (Integer) this.recurringDepositAccountHelper.prematureCloseForRecurringDeposit(
                recurringDepositAccountId, CLOSED_ON_DATE, CLOSURE_TYPE_WITHDRAW_DEPOSIT, null, CommonConstants.RESPONSE_RESOURCE_ID);
        Assertions.assertNotNull(prematureClosureTransactionId);

        recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker.getStatusOfRecurringDepositAccount(this.requestSpec,
                this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositAccountIsPrematureClosed(recurringDepositAccountStatusHashMap);

        recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec, this.responseSpec,
                recurringDepositAccountId);

        Float maturityAmount = (Float) recurringDepositAccountData.get("maturityAmount");

        Assertions.assertTrue(Math.abs(principal - maturityAmount) < THRESHOLD, "Verifying Pre-Closure maturity amount");

    }

    /***
     * Test case for verify premature closure amount with penal interest for whole term with closure transaction type
     * withdrawal and 360 days in year
     */
    @Test
    public void testPrematureClosureAmountWithPenalInterestForWholeTerm_With_360_Days() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.accountHelper = new AccountHelper(this.requestSpec, this.responseSpec);
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);
        DateFormat currentDateFormat = new SimpleDateFormat("dd");

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -3);
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.YEAR, 10);
        final String VALID_TO = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -1);
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.MONTH, 1);
        final String CLOSED_ON_DATE = dateFormat.format(todaysDate.getTime());

        final Account assetAccount = this.accountHelper.createAssetAccount();
        final Account incomeAccount = this.accountHelper.createIncomeAccount();
        final Account expenseAccount = this.accountHelper.createExpenseAccount();
        final Account liabilityAccount = this.accountHelper.createLiabilityAccount();

        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        final String accountingRule = NONE;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule);
        Assertions.assertNotNull(recurringDepositProductId);

        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap modificationsHashMap = this.recurringDepositAccountHelper.updateInterestCalculationConfigForRecurringDeposit(
                clientId.toString(), recurringDepositProductId.toString(), recurringDepositAccountId.toString(), SUBMITTED_ON_DATE,
                VALID_FROM, VALID_TO, DAYS_360, WHOLE_TERM, INTEREST_CALCULATION_USING_DAILY_BALANCE, MONTHLY, MONTHLY,
                expectedFirstDepositOnDate);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsApproved(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.activateRecurringDeposit(recurringDepositAccountId,
                ACTIVATION_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsActive(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec,
                this.responseSpec, recurringDepositAccountId);
        Float depositAmount = (Float) recurringDepositAccountData.get("mandatoryRecommendedDepositAmount");
        Integer depositPeriod = (Integer) recurringDepositAccountData.get("depositPeriod");
        HashMap daysInYearMap = (HashMap) recurringDepositAccountData.get("interestCalculationDaysInYearType");
        Float preClosurePenalInterestRate = (Float) recurringDepositAccountData.get("preClosurePenalInterest");
        Integer daysInYear = (Integer) daysInYearMap.get("id");
        ArrayList<ArrayList<HashMap>> interestRateChartData = RecurringDepositProductHelper
                .getInterestRateChartSlabsByProductId(this.requestSpec, this.responseSpec, recurringDepositProductId);

        Integer depositTransactionId = this.recurringDepositAccountHelper.depositToRecurringDepositAccount(recurringDepositAccountId,
                DEPOSIT_AMOUNT, expectedFirstDepositOnDate);
        Assertions.assertNotNull(depositTransactionId);

        HashMap recurringDepositSummary = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        Float principal = (Float) recurringDepositSummary.get("totalDeposits");

        Float interestRate = RecurringDepositAccountHelper.getInterestRate(interestRateChartData, depositPeriod);
        interestRate -= preClosurePenalInterestRate;
        double interestRateInFraction = interestRate / 100;
        double perDay = (double) 1 / daysInYear;
        LOG.info("per day = {}", perDay);
        double interestPerDay = interestRateInFraction * perDay;

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        Integer currentDate = Integer.valueOf(currentDateFormat.format(calendar.getTime()));
        Integer daysInMonth = calendar.getActualMaximum(Calendar.DATE);
        daysInMonth = daysInMonth - currentDate + 1;
        Float interestPerMonth = (float) (interestPerDay * principal * daysInMonth);
        principal += interestPerMonth + depositAmount;
        calendar.add(Calendar.DATE, daysInMonth);
        LOG.info("{}", monthDayFormat.format(calendar.getTime()));

        expectedFirstDepositOnDate = dateFormat.format(calendar.getTime());
        Integer transactionIdForDeposit = this.recurringDepositAccountHelper.depositToRecurringDepositAccount(recurringDepositAccountId,
                DEPOSIT_AMOUNT, expectedFirstDepositOnDate);
        Assertions.assertNotNull(transactionIdForDeposit);

        currentDate = currentDate - 1;
        interestPerMonth = (float) (interestPerDay * principal * currentDate);
        LOG.info("IPM = {}", interestPerMonth);
        principal += interestPerMonth;
        LOG.info("principal = {}", principal);

        HashMap recurringDepositPrematureData = this.recurringDepositAccountHelper
                .calculatePrematureAmountForRecurringDeposit(recurringDepositAccountId, CLOSED_ON_DATE);

        Integer prematureClosureTransactionId = (Integer) this.recurringDepositAccountHelper.prematureCloseForRecurringDeposit(
                recurringDepositAccountId, CLOSED_ON_DATE, CLOSURE_TYPE_WITHDRAW_DEPOSIT, null, CommonConstants.RESPONSE_RESOURCE_ID);
        Assertions.assertNotNull(prematureClosureTransactionId);

        recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker.getStatusOfRecurringDepositAccount(this.requestSpec,
                this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositAccountIsPrematureClosed(recurringDepositAccountStatusHashMap);

        recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec, this.responseSpec,
                recurringDepositAccountId);

        Float maturityAmount = (Float) recurringDepositAccountData.get("maturityAmount");

        Assertions.assertTrue(Math.abs(principal - maturityAmount) < THRESHOLD, "Verifying Pre-Closure maturity amount");

    }

    /***
     * Test case for verify premature closure amount with penal interest till maturity date with closure transaction
     * type withdrawal and 365 days in year
     */
    @Test
    public void testPrematureClosureAmountWithPenalInterestTillPrematureWithdrawal_With_365_Days() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.accountHelper = new AccountHelper(this.requestSpec, this.responseSpec);
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);
        DateFormat currentDateFormat = new SimpleDateFormat("dd");

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -3);
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.YEAR, 10);
        final String VALID_TO = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -1);
        todaysDate.add(Calendar.DAY_OF_MONTH, -1);
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.MONTH, 1);
        todaysDate.add(Calendar.DAY_OF_MONTH, 1);
        final String CLOSED_ON_DATE = dateFormat.format(todaysDate.getTime());

        final Account assetAccount = this.accountHelper.createAssetAccount();
        final Account incomeAccount = this.accountHelper.createIncomeAccount();
        final Account expenseAccount = this.accountHelper.createExpenseAccount();
        final Account liabilityAccount = this.accountHelper.createLiabilityAccount();

        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        final String accountingRule = NONE;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule);
        Assertions.assertNotNull(recurringDepositProductId);

        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, TILL_PREMATURE_WITHDRAWAL, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsApproved(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.activateRecurringDeposit(recurringDepositAccountId,
                ACTIVATION_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsActive(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec,
                this.responseSpec, recurringDepositAccountId);
        Float depositAmount = (Float) recurringDepositAccountData.get("mandatoryRecommendedDepositAmount");
        HashMap daysInYearMap = (HashMap) recurringDepositAccountData.get("interestCalculationDaysInYearType");
        Float preClosurePenalInterestRate = (Float) recurringDepositAccountData.get("preClosurePenalInterest");
        Integer daysInYear = (Integer) daysInYearMap.get("id");
        ArrayList<ArrayList<HashMap>> interestRateChartData = RecurringDepositProductHelper
                .getInterestRateChartSlabsByProductId(this.requestSpec, this.responseSpec, recurringDepositProductId);

        Calendar activationDate = Calendar.getInstance();
        activationDate.add(Calendar.MONTH, -1);
        activationDate.add(Calendar.DAY_OF_MONTH, -1);
        ZonedDateTime start = ZonedDateTime.ofInstant(activationDate.getTime().toInstant(), Utils.getZoneIdOfTenant());

        Calendar prematureClosureDate = Calendar.getInstance();
        ZonedDateTime end = ZonedDateTime.ofInstant(prematureClosureDate.getTime().toInstant(), Utils.getZoneIdOfTenant());

        Integer depositedPeriod = Math.toIntExact(ChronoUnit.MONTHS.between(start.toLocalDate(), end.toLocalDate()));

        Integer depositTransactionId = this.recurringDepositAccountHelper.depositToRecurringDepositAccount(recurringDepositAccountId,
                DEPOSIT_AMOUNT, expectedFirstDepositOnDate);
        Assertions.assertNotNull(depositTransactionId);

        HashMap recurringDepositSummary = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        Float principal = (Float) recurringDepositSummary.get("totalDeposits");

        Float interestRate = RecurringDepositAccountHelper.getInterestRate(interestRateChartData, depositedPeriod);
        interestRate -= preClosurePenalInterestRate;
        double interestRateInFraction = interestRate / 100;
        double perDay = (double) 1 / daysInYear;
        LOG.info("per day = {}", perDay);
        double interestPerDay = interestRateInFraction * perDay;

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        Integer currentDate = Integer.valueOf(currentDateFormat.format(calendar.getTime()));
        Integer daysInMonth = calendar.getActualMaximum(Calendar.DATE);
        daysInMonth = daysInMonth - currentDate + 1;
        Float interestPerMonth = (float) (interestPerDay * principal * daysInMonth);
        principal += interestPerMonth + depositAmount;
        calendar.add(Calendar.DATE, daysInMonth);
        LOG.info("{}", monthDayFormat.format(calendar.getTime()));

        expectedFirstDepositOnDate = dateFormat.format(calendar.getTime());
        Integer transactionIdForDeposit = this.recurringDepositAccountHelper.depositToRecurringDepositAccount(recurringDepositAccountId,
                DEPOSIT_AMOUNT, expectedFirstDepositOnDate);
        Assertions.assertNotNull(transactionIdForDeposit);

        currentDate = currentDate - 1;
        interestPerMonth = (float) (interestPerDay * principal * currentDate);
        LOG.info("IPM = {}", interestPerMonth);
        principal += interestPerMonth;
        LOG.info("principal = {}", principal);

        HashMap recurringDepositPrematureData = this.recurringDepositAccountHelper
                .calculatePrematureAmountForRecurringDeposit(recurringDepositAccountId, CLOSED_ON_DATE);

        Integer prematureClosureTransactionId = (Integer) this.recurringDepositAccountHelper.prematureCloseForRecurringDeposit(
                recurringDepositAccountId, CLOSED_ON_DATE, CLOSURE_TYPE_WITHDRAW_DEPOSIT, null, CommonConstants.RESPONSE_RESOURCE_ID);
        Assertions.assertNotNull(prematureClosureTransactionId);

        recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker.getStatusOfRecurringDepositAccount(this.requestSpec,
                this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositAccountIsPrematureClosed(recurringDepositAccountStatusHashMap);

        recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec, this.responseSpec,
                recurringDepositAccountId);

        Float maturityAmount = (Float) recurringDepositAccountData.get("maturityAmount");

        Assertions.assertTrue(Math.abs(principal - maturityAmount) < THRESHOLD, "Verifying Pre-Closure maturity amount");

    }

    /***
     * Test case verify premature closure amount with penal interest till maturity date with closure transaction type
     * withdrawal and 360 days in year
     */
    @Test
    public void testPrematureClosureAmountWithPenalInterestTillPrematureWithdrawal_With_360_Days() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.accountHelper = new AccountHelper(this.requestSpec, this.responseSpec);
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);
        DateFormat currentDateFormat = new SimpleDateFormat("dd");

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -3);
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.YEAR, 10);
        final String VALID_TO = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -1);
        todaysDate.add(Calendar.DAY_OF_MONTH, -1);
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.MONTH, 1);
        todaysDate.add(Calendar.DAY_OF_MONTH, 1);
        final String CLOSED_ON_DATE = dateFormat.format(todaysDate.getTime());

        final Account assetAccount = this.accountHelper.createAssetAccount();
        final Account incomeAccount = this.accountHelper.createIncomeAccount();
        final Account expenseAccount = this.accountHelper.createExpenseAccount();
        final Account liabilityAccount = this.accountHelper.createLiabilityAccount();

        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        final String accountingRule = NONE;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule);
        Assertions.assertNotNull(recurringDepositProductId);

        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, TILL_PREMATURE_WITHDRAWAL, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap modificationsHashMap = this.recurringDepositAccountHelper.updateInterestCalculationConfigForRecurringDeposit(
                clientId.toString(), recurringDepositProductId.toString(), recurringDepositAccountId.toString(), SUBMITTED_ON_DATE,
                VALID_FROM, VALID_TO, DAYS_360, TILL_PREMATURE_WITHDRAWAL, INTEREST_CALCULATION_USING_DAILY_BALANCE, MONTHLY, MONTHLY,
                expectedFirstDepositOnDate);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsApproved(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.activateRecurringDeposit(recurringDepositAccountId,
                ACTIVATION_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsActive(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec,
                this.responseSpec, recurringDepositAccountId);
        Float depositAmount = (Float) recurringDepositAccountData.get("mandatoryRecommendedDepositAmount");
        HashMap daysInYearMap = (HashMap) recurringDepositAccountData.get("interestCalculationDaysInYearType");
        Float preClosurePenalInterestRate = (Float) recurringDepositAccountData.get("preClosurePenalInterest");
        Integer daysInYear = (Integer) daysInYearMap.get("id");
        ArrayList<ArrayList<HashMap>> interestRateChartData = RecurringDepositProductHelper
                .getInterestRateChartSlabsByProductId(this.requestSpec, this.responseSpec, recurringDepositProductId);

        Calendar activationDate = Calendar.getInstance();
        activationDate.add(Calendar.MONTH, -1);
        activationDate.add(Calendar.DAY_OF_MONTH, -1);
        ZonedDateTime start = ZonedDateTime.ofInstant(activationDate.getTime().toInstant(), Utils.getZoneIdOfTenant());

        Calendar prematureClosureDate = Calendar.getInstance();
        ZonedDateTime end = ZonedDateTime.ofInstant(prematureClosureDate.getTime().toInstant(), Utils.getZoneIdOfTenant());

        Integer depositedPeriod = Math.toIntExact(ChronoUnit.MONTHS.between(start.toLocalDate(), end.toLocalDate()));

        Integer transactionIdForDeposit = this.recurringDepositAccountHelper.depositToRecurringDepositAccount(recurringDepositAccountId,
                DEPOSIT_AMOUNT, expectedFirstDepositOnDate);
        Assertions.assertNotNull(transactionIdForDeposit);

        HashMap recurringDepositSummary = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        Float principal = (Float) recurringDepositSummary.get("totalDeposits");

        Float interestRate = RecurringDepositAccountHelper.getInterestRate(interestRateChartData, depositedPeriod);
        interestRate -= preClosurePenalInterestRate;
        double interestRateInFraction = interestRate / 100;
        double perDay = (double) 1 / daysInYear;
        LOG.info("per day = {}", perDay);
        double interestPerDay = interestRateInFraction * perDay;

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        Integer currentDate = Integer.valueOf(currentDateFormat.format(calendar.getTime()));
        Integer daysInMonth = calendar.getActualMaximum(Calendar.DATE);
        daysInMonth = daysInMonth - currentDate + 1;
        Float interestPerMonth = (float) (interestPerDay * principal * daysInMonth);
        principal += interestPerMonth + depositAmount;
        calendar.add(Calendar.DATE, daysInMonth);
        LOG.info("{}", monthDayFormat.format(calendar.getTime()));

        expectedFirstDepositOnDate = dateFormat.format(calendar.getTime());
        Integer newTransactionIdForDeposit = this.recurringDepositAccountHelper.depositToRecurringDepositAccount(recurringDepositAccountId,
                DEPOSIT_AMOUNT, expectedFirstDepositOnDate);
        Assertions.assertNotNull(newTransactionIdForDeposit);

        currentDate = currentDate - 1;
        interestPerMonth = (float) (interestPerDay * principal * currentDate);
        LOG.info("IPM = {}", interestPerMonth);
        principal += interestPerMonth;
        LOG.info("principal = {}", principal);

        HashMap recurringDepositPrematureData = this.recurringDepositAccountHelper
                .calculatePrematureAmountForRecurringDeposit(recurringDepositAccountId, CLOSED_ON_DATE);

        Integer prematureClosureTransactionId = (Integer) this.recurringDepositAccountHelper.prematureCloseForRecurringDeposit(
                recurringDepositAccountId, CLOSED_ON_DATE, CLOSURE_TYPE_WITHDRAW_DEPOSIT, null, CommonConstants.RESPONSE_RESOURCE_ID);
        Assertions.assertNotNull(prematureClosureTransactionId);

        recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker.getStatusOfRecurringDepositAccount(this.requestSpec,
                this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositAccountIsPrematureClosed(recurringDepositAccountStatusHashMap);

        recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec, this.responseSpec,
                recurringDepositAccountId);

        Float maturityAmount = (Float) recurringDepositAccountData.get("maturityAmount");

        Assertions.assertTrue(Math.abs(principal - maturityAmount) < THRESHOLD, "Verifying Pre-Closure maturity amount");

    }

    /***
     * Test case for verify maturity amount with daily compounding and monthly posting with 365 days in year
     */
    @Test
    public void testMaturityAmountForDailyCompoundingAndMonthlyPosting_With_365_Days() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);
        DateFormat currentDateFormat = new SimpleDateFormat("dd");

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -3);
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.YEAR, 10);
        final String VALID_TO = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -1);
        Integer currentDate = Integer.valueOf(currentDateFormat.format(todaysDate.getTime()));
        todaysDate.add(Calendar.DATE, -(currentDate - 1));
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());

        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        final String accountingRule = NONE;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule);
        Assertions.assertNotNull(recurringDepositProductId);
        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap modificationsHashMap = this.recurringDepositAccountHelper.updateInterestCalculationConfigForRecurringDeposit(
                clientId.toString(), recurringDepositProductId.toString(), recurringDepositAccountId.toString(), SUBMITTED_ON_DATE,
                VALID_FROM, VALID_TO, DAYS_365, WHOLE_TERM, INTEREST_CALCULATION_USING_DAILY_BALANCE, DAILY, MONTHLY,
                expectedFirstDepositOnDate);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsApproved(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec,
                this.responseSpec, recurringDepositAccountId);
        HashMap recurringDepositSummary = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        Float principal = (Float) recurringDepositSummary.get("accountBalance");
        Float recurringDepositAmount = (Float) recurringDepositAccountData.get("mandatoryRecommendedDepositAmount");
        Float maturityAmount = (Float) recurringDepositAccountData.get("maturityAmount");
        Integer depositPeriod = (Integer) recurringDepositAccountData.get("depositPeriod");
        HashMap daysInYearMap = (HashMap) recurringDepositAccountData.get("interestCalculationDaysInYearType");
        Integer daysInYear = (Integer) daysInYearMap.get("id");
        ArrayList<ArrayList<HashMap>> interestRateChartData = RecurringDepositProductHelper
                .getInterestRateChartSlabsByProductId(this.requestSpec, this.responseSpec, recurringDepositProductId);

        Float interestRate = RecurringDepositAccountHelper.getInterestRate(interestRateChartData, depositPeriod);
        double interestRateInFraction = interestRate / 100;
        double perDay = (double) 1 / daysInYear;
        LOG.info("per day = {}", perDay);
        double interestPerDay = interestRateInFraction * perDay;

        Integer daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);

        principal = RecurringDepositAccountHelper.getPrincipalAfterCompoundingInterest(todaysDate, principal, recurringDepositAmount,
                depositPeriod, interestPerDay, DAILY_COMPOUNDING_INTERVAL, MONTHLY_INTERVAL);

        LOG.info("{}", principal.toString());
        Assertions.assertTrue(Math.abs(principal - maturityAmount) < THRESHOLD, "Verifying Maturity amount for Recurring Deposit Account");

    }

    /***
     * Test case for verify maturity amount with daily compounding and monthly posting with 360 days in year
     */
    @Test
    public void testMaturityAmountForDailyCompoundingAndMonthlyPosting_With_360_Days() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);
        DateFormat currentDateFormat = new SimpleDateFormat("dd");

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -3);
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());
        todaysDate.add(Calendar.YEAR, 10);
        final String VALID_TO = dateFormat.format(todaysDate.getTime());

        todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.MONTH, -1);
        Integer currentDate = Integer.valueOf(currentDateFormat.format(todaysDate.getTime()));
        todaysDate.add(Calendar.DATE, -(currentDate - 1));
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());

        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        final String accountingRule = NONE;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule);
        Assertions.assertNotNull(recurringDepositProductId);

        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap modificationsHashMap = this.recurringDepositAccountHelper.updateInterestCalculationConfigForRecurringDeposit(
                clientId.toString(), recurringDepositProductId.toString(), recurringDepositAccountId.toString(), SUBMITTED_ON_DATE,
                VALID_FROM, VALID_TO, DAYS_360, WHOLE_TERM, INTEREST_CALCULATION_USING_DAILY_BALANCE, DAILY, MONTHLY,
                expectedFirstDepositOnDate);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsApproved(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec,
                this.responseSpec, recurringDepositAccountId);
        HashMap recurringDepositSummary = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        Float principal = (Float) recurringDepositSummary.get("accountBalance");
        Float recurringDepositAmount = (Float) recurringDepositAccountData.get("mandatoryRecommendedDepositAmount");
        Float maturityAmount = (Float) recurringDepositAccountData.get("maturityAmount");
        Integer depositPeriod = (Integer) recurringDepositAccountData.get("depositPeriod");
        HashMap daysInYearMap = (HashMap) recurringDepositAccountData.get("interestCalculationDaysInYearType");
        Integer daysInYear = (Integer) daysInYearMap.get("id");
        ArrayList<ArrayList<HashMap>> interestRateChartData = RecurringDepositProductHelper
                .getInterestRateChartSlabsByProductId(this.requestSpec, this.responseSpec, recurringDepositProductId);

        Float interestRate = RecurringDepositAccountHelper.getInterestRate(interestRateChartData, depositPeriod);
        double interestRateInFraction = interestRate / 100;
        double perDay = (double) 1 / daysInYear;
        LOG.info("per day = {}", perDay);
        double interestPerDay = interestRateInFraction * perDay;

        Integer daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);

        principal = RecurringDepositAccountHelper.getPrincipalAfterCompoundingInterest(todaysDate, principal, recurringDepositAmount,
                depositPeriod, interestPerDay, DAILY_COMPOUNDING_INTERVAL, MONTHLY_INTERVAL);

        LOG.info("{}", principal.toString());
        Assertions.assertTrue(Math.abs(principal - maturityAmount) < THRESHOLD, "Verifying Maturity amount for Recurring Deposit Account");

    }

    /***
     * Test case for verify premature closure amount with Bi-annual interest compounding and Bi-annual interest posting
     * with 365 days in year
     */
    @Test
    public void testRecurringDepositWithBi_AnnualCompoundingAndPosting_365_Days() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.accountHelper = new AccountHelper(this.requestSpec, this.responseSpec);
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);
        DateFormat currentMonthFormat = new SimpleDateFormat("MM");
        DateFormat currentDateFormat = new SimpleDateFormat("dd");

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.YEAR, -1);
        Integer currentMonth = Integer.valueOf(currentMonthFormat.format(todaysDate.getTime()));
        Integer numberOfMonths = 12 - currentMonth;
        todaysDate.add(Calendar.MONTH, numberOfMonths);
        Integer currentDate = Integer.valueOf(currentDateFormat.format(todaysDate.getTime()));
        Integer daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);
        Integer daysLeft = daysInMonth - currentDate;
        todaysDate.add(Calendar.DATE, daysLeft + 1);
        daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);
        LOG.info("{}", dateFormat.format(todaysDate.getTime()));
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());

        final String VALID_TO = null;
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());

        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        final String accountingRule = NONE;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule);
        Assertions.assertNotNull(recurringDepositProductId);

        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap modificationsHashMap = this.recurringDepositAccountHelper.updateInterestCalculationConfigForRecurringDeposit(
                clientId.toString(), recurringDepositProductId.toString(), recurringDepositAccountId.toString(), SUBMITTED_ON_DATE,
                VALID_FROM, VALID_TO, DAYS_365, WHOLE_TERM, INTEREST_CALCULATION_USING_DAILY_BALANCE, BI_ANNUALLY, BI_ANNUALLY,
                expectedFirstDepositOnDate);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsApproved(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec,
                this.responseSpec, recurringDepositAccountId);
        HashMap recurringDepositSummary = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        Float principal = (Float) recurringDepositSummary.get("accountBalance");
        Float recurringDepositAmount = (Float) recurringDepositAccountData.get("mandatoryRecommendedDepositAmount");
        Integer depositPeriod = (Integer) recurringDepositAccountData.get("depositPeriod");
        HashMap daysInYearMap = (HashMap) recurringDepositAccountData.get("interestCalculationDaysInYearType");
        Integer daysInYear = (Integer) daysInYearMap.get("id");
        ArrayList<ArrayList<HashMap>> interestRateChartData = RecurringDepositProductHelper
                .getInterestRateChartSlabsByProductId(this.requestSpec, this.responseSpec, recurringDepositProductId);

        Float interestRate = RecurringDepositAccountHelper.getInterestRate(interestRateChartData, depositPeriod);
        double interestRateInFraction = interestRate / 100;
        double perDay = (double) 1 / daysInYear;
        LOG.info("per day = {}", perDay);
        double interestPerDay = interestRateInFraction * perDay;

        principal = RecurringDepositAccountHelper.getPrincipalAfterCompoundingInterest(todaysDate, principal, recurringDepositAmount,
                depositPeriod, interestPerDay, BIANNULLY_INTERVAL, BIANNULLY_INTERVAL);

        recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec, this.responseSpec,
                recurringDepositAccountId);

        Float expectedPrematureAmount = principal;
        Float maturityAmount = (Float) recurringDepositAccountData.get("maturityAmount");

        Assertions.assertTrue(Math.abs(expectedPrematureAmount - maturityAmount) < THRESHOLD, "Verifying Pre-Closure maturity amount");

    }

    /***
     * Test case for verify premature closure amount with Bi-annual interest compounding and Bi-annual interest posting
     * with 360 days in year
     */
    @Test
    public void testRecurringDepositWithBi_AnnualCompoundingAndPosting_360_Days() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.accountHelper = new AccountHelper(this.requestSpec, this.responseSpec);
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);
        DateFormat currentMonthFormat = new SimpleDateFormat("MM");
        DateFormat currentDateFormat = new SimpleDateFormat("dd");

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.YEAR, -1);
        Integer currentMonth = Integer.valueOf(currentMonthFormat.format(todaysDate.getTime()));
        Integer numberOfMonths = 12 - currentMonth;
        todaysDate.add(Calendar.MONTH, numberOfMonths);
        Integer currentDate = Integer.valueOf(currentDateFormat.format(todaysDate.getTime()));
        Integer daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);
        Integer daysLeft = daysInMonth - currentDate;
        todaysDate.add(Calendar.DATE, daysLeft + 1);
        daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);
        LOG.info("{}", dateFormat.format(todaysDate.getTime()));
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());

        final String VALID_TO = null;
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());

        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        final String accountingRule = NONE;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule);
        Assertions.assertNotNull(recurringDepositProductId);

        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap modificationsHashMap = this.recurringDepositAccountHelper.updateInterestCalculationConfigForRecurringDeposit(
                clientId.toString(), recurringDepositProductId.toString(), recurringDepositAccountId.toString(), SUBMITTED_ON_DATE,
                VALID_FROM, VALID_TO, DAYS_360, WHOLE_TERM, INTEREST_CALCULATION_USING_DAILY_BALANCE, BI_ANNUALLY, BI_ANNUALLY,
                expectedFirstDepositOnDate);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsApproved(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec,
                this.responseSpec, recurringDepositAccountId);
        HashMap recurringDepositSummary = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        Float principal = (Float) recurringDepositSummary.get("accountBalance");
        Float recurringDepositAmount = (Float) recurringDepositAccountData.get("mandatoryRecommendedDepositAmount");
        Integer depositPeriod = (Integer) recurringDepositAccountData.get("depositPeriod");
        HashMap daysInYearMap = (HashMap) recurringDepositAccountData.get("interestCalculationDaysInYearType");
        Integer daysInYear = (Integer) daysInYearMap.get("id");
        ArrayList<ArrayList<HashMap>> interestRateChartData = RecurringDepositProductHelper
                .getInterestRateChartSlabsByProductId(this.requestSpec, this.responseSpec, recurringDepositProductId);

        Float interestRate = RecurringDepositAccountHelper.getInterestRate(interestRateChartData, depositPeriod);
        double interestRateInFraction = interestRate / 100;
        double perDay = (double) 1 / daysInYear;
        LOG.info("per day = {} ", perDay);
        double interestPerDay = interestRateInFraction * perDay;

        principal = RecurringDepositAccountHelper.getPrincipalAfterCompoundingInterest(todaysDate, principal, recurringDepositAmount,
                depositPeriod, interestPerDay, BIANNULLY_INTERVAL, BIANNULLY_INTERVAL);

        recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec, this.responseSpec,
                recurringDepositAccountId);

        Float expectedPrematureAmount = principal;
        Float maturityAmount = (Float) recurringDepositAccountData.get("maturityAmount");

        Assertions.assertTrue(Math.abs(expectedPrematureAmount - maturityAmount) < THRESHOLD, "Verifying Pre-Closure maturity amount");

    }

    /***
     * Test case for verify maturity amount with Daily interest compounding and annual interest posting with 365 days in
     * year
     */
    @Test
    public void testMaturityAmountForDailyCompoundingAndAnnuallyPosting_With_365_Days() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);
        DateFormat currentMonthFormat = new SimpleDateFormat("MM");
        DateFormat currentDateFormat = new SimpleDateFormat("dd");

        Calendar todaysDate = Calendar.getInstance();

        todaysDate.add(Calendar.YEAR, -1);
        Integer currentMonth = Integer.valueOf(currentMonthFormat.format(todaysDate.getTime()));
        Integer numberOfMonths = 12 - currentMonth;
        todaysDate.add(Calendar.MONTH, numberOfMonths);
        Integer currentDate = Integer.valueOf(currentDateFormat.format(todaysDate.getTime()));
        Integer daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);
        Integer daysLeft = daysInMonth - currentDate;
        todaysDate.add(Calendar.DATE, daysLeft + 1);
        daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);
        LOG.info("{}", dateFormat.format(todaysDate.getTime()));
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());

        final String VALID_TO = null;
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());

        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        final String accountingRule = NONE;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule);
        Assertions.assertNotNull(recurringDepositProductId);

        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap modificationsHashMap = this.recurringDepositAccountHelper.updateInterestCalculationConfigForRecurringDeposit(
                clientId.toString(), recurringDepositProductId.toString(), recurringDepositAccountId.toString(), SUBMITTED_ON_DATE,
                VALID_FROM, VALID_TO, DAYS_365, WHOLE_TERM, INTEREST_CALCULATION_USING_DAILY_BALANCE, DAILY, ANNUALLY,
                expectedFirstDepositOnDate);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        FixedDepositAccountStatusChecker.verifyFixedDepositIsApproved(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec,
                this.responseSpec, recurringDepositAccountId);
        HashMap recurringDepositSummary = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);

        Float principal = (Float) recurringDepositSummary.get("accountBalance");
        Float recurringDepositAmount = (Float) recurringDepositAccountData.get("mandatoryRecommendedDepositAmount");
        Integer depositPeriod = (Integer) recurringDepositAccountData.get("depositPeriod");
        HashMap daysInYearMap = (HashMap) recurringDepositAccountData.get("interestCalculationDaysInYearType");
        Integer daysInYear = (Integer) daysInYearMap.get("id");
        ArrayList<ArrayList<HashMap>> interestRateChartData = RecurringDepositProductHelper
                .getInterestRateChartSlabsByProductId(this.requestSpec, this.responseSpec, recurringDepositProductId);

        Float interestRate = RecurringDepositAccountHelper.getInterestRate(interestRateChartData, depositPeriod);
        double interestRateInFraction = interestRate / 100;
        double perDay = (double) 1 / daysInYear;
        LOG.info("per day = {} ", perDay);
        double interestPerDay = interestRateInFraction * perDay;

        principal = RecurringDepositAccountHelper.getPrincipalAfterCompoundingInterest(todaysDate, principal, recurringDepositAmount,
                depositPeriod, interestPerDay, DAILY_COMPOUNDING_INTERVAL, ANNUL_INTERVAL);

        recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec, this.responseSpec,
                recurringDepositAccountId);

        Float expectedPrematureAmount = principal;
        Float maturityAmount = (Float) recurringDepositAccountData.get("maturityAmount");

        Assertions.assertTrue(Math.abs(expectedPrematureAmount - maturityAmount) < THRESHOLD, "Verifying Maturity amount");

    }

    /***
     * Test case for verify maturity amount with Daily interest compounding and annual interest posting with 360 days in
     * year
     */
    @Test
    public void testMaturityAmountForDailyCompoundingAndAnnuallyPosting_With_360_Days() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);
        DateFormat currentMonthFormat = new SimpleDateFormat("MM");
        DateFormat currentDateFormat = new SimpleDateFormat("dd");

        Calendar todaysDate = Calendar.getInstance();

        todaysDate.add(Calendar.YEAR, -1);
        Integer currentMonth = Integer.valueOf(currentMonthFormat.format(todaysDate.getTime()));
        Integer numberOfMonths = 12 - currentMonth;
        todaysDate.add(Calendar.MONTH, numberOfMonths);
        Integer currentDate = Integer.valueOf(currentDateFormat.format(todaysDate.getTime()));
        Integer daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);
        Integer daysLeft = daysInMonth - currentDate;
        todaysDate.add(Calendar.DATE, daysLeft + 1);
        daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);
        LOG.info("{}", dateFormat.format(todaysDate.getTime()));
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());

        final String VALID_TO = null;
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());

        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        final String accountingRule = NONE;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule);
        Assertions.assertNotNull(recurringDepositProductId);

        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap modificationsHashMap = this.recurringDepositAccountHelper.updateInterestCalculationConfigForRecurringDeposit(
                clientId.toString(), recurringDepositProductId.toString(), recurringDepositAccountId.toString(), SUBMITTED_ON_DATE,
                VALID_FROM, VALID_TO, DAYS_360, WHOLE_TERM, INTEREST_CALCULATION_USING_DAILY_BALANCE, DAILY, ANNUALLY,
                expectedFirstDepositOnDate);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        FixedDepositAccountStatusChecker.verifyFixedDepositIsApproved(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec,
                this.responseSpec, recurringDepositAccountId);
        HashMap recurringDepositSummary = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);

        Float principal = (Float) recurringDepositSummary.get("accountBalance");
        Float recurringDepositAmount = (Float) recurringDepositAccountData.get("mandatoryRecommendedDepositAmount");
        Integer depositPeriod = (Integer) recurringDepositAccountData.get("depositPeriod");
        HashMap daysInYearMap = (HashMap) recurringDepositAccountData.get("interestCalculationDaysInYearType");
        Integer daysInYear = (Integer) daysInYearMap.get("id");
        ArrayList<ArrayList<HashMap>> interestRateChartData = RecurringDepositProductHelper
                .getInterestRateChartSlabsByProductId(this.requestSpec, this.responseSpec, recurringDepositProductId);

        Float interestRate = RecurringDepositAccountHelper.getInterestRate(interestRateChartData, depositPeriod);
        double interestRateInFraction = interestRate / 100;
        double perDay = (double) 1 / daysInYear;
        LOG.info("per day = {} ", perDay);
        double interestPerDay = interestRateInFraction * perDay;

        principal = RecurringDepositAccountHelper.getPrincipalAfterCompoundingInterest(todaysDate, principal, recurringDepositAmount,
                depositPeriod, interestPerDay, DAILY_COMPOUNDING_INTERVAL, ANNUL_INTERVAL);

        recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec, this.responseSpec,
                recurringDepositAccountId);

        Float expectedPrematureAmount = principal;
        Float maturityAmount = (Float) recurringDepositAccountData.get("maturityAmount");

        Assertions.assertTrue(Math.abs(expectedPrematureAmount - maturityAmount) < THRESHOLD, "Verifying Maturity amount");

    }

    /***
     * Test case for verify premature closure amount with Quarterly interest compounding and Quarterly interest posting
     * with 365 days in year
     */
    @Test
    public void testRecurringDepositQuarterlyCompoundingAndQuarterlyPosting_365_Days() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.accountHelper = new AccountHelper(this.requestSpec, this.responseSpec);
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);
        DateFormat currentMonthFormat = new SimpleDateFormat("MM");
        DateFormat currentDateFormat = new SimpleDateFormat("dd");

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.YEAR, -1);
        Integer currentMonth = Integer.valueOf(currentMonthFormat.format(todaysDate.getTime()));
        Integer numberOfMonths = 12 - currentMonth;
        todaysDate.add(Calendar.MONTH, numberOfMonths);
        Integer currentDate = Integer.valueOf(currentDateFormat.format(todaysDate.getTime()));
        Integer daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);
        Integer daysLeft = daysInMonth - currentDate;
        todaysDate.add(Calendar.DATE, daysLeft + 1);
        daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);
        LOG.info("{}", dateFormat.format(todaysDate.getTime()));
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());

        final String VALID_TO = null;
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());

        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        final String accountingRule = NONE;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule);
        Assertions.assertNotNull(recurringDepositProductId);

        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap modificationsHashMap = this.recurringDepositAccountHelper.updateInterestCalculationConfigForRecurringDeposit(
                clientId.toString(), recurringDepositProductId.toString(), recurringDepositAccountId.toString(), SUBMITTED_ON_DATE,
                VALID_FROM, VALID_TO, DAYS_365, WHOLE_TERM, INTEREST_CALCULATION_USING_DAILY_BALANCE, QUARTERLY, QUARTERLY,
                expectedFirstDepositOnDate);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsApproved(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec,
                this.responseSpec, recurringDepositAccountId);
        HashMap recurringDepositSummary = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        Float principal = (Float) recurringDepositSummary.get("accountBalance");
        Float recurringDepositAmount = (Float) recurringDepositAccountData.get("mandatoryRecommendedDepositAmount");
        Integer depositPeriod = (Integer) recurringDepositAccountData.get("depositPeriod");
        HashMap daysInYearMap = (HashMap) recurringDepositAccountData.get("interestCalculationDaysInYearType");
        Integer daysInYear = (Integer) daysInYearMap.get("id");
        ArrayList<ArrayList<HashMap>> interestRateChartData = RecurringDepositProductHelper
                .getInterestRateChartSlabsByProductId(this.requestSpec, this.responseSpec, recurringDepositProductId);

        Float interestRate = RecurringDepositAccountHelper.getInterestRate(interestRateChartData, depositPeriod);
        double interestRateInFraction = interestRate / 100;
        double perDay = (double) 1 / daysInYear;
        LOG.info("per day = {} ", perDay);
        double interestPerDay = interestRateInFraction * perDay;

        principal = RecurringDepositAccountHelper.getPrincipalAfterCompoundingInterest(todaysDate, principal, recurringDepositAmount,
                depositPeriod, interestPerDay, QUARTERLY_INTERVAL, QUARTERLY_INTERVAL);

        recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec, this.responseSpec,
                recurringDepositAccountId);

        Float expectedPrematureAmount = principal;
        Float maturityAmount = (Float) recurringDepositAccountData.get("maturityAmount");

        Assertions.assertTrue(Math.abs(expectedPrematureAmount - maturityAmount) < THRESHOLD, "Verifying Pre-Closure maturity amount");

    }

    /***
     * Test case for verify premature closure amount with Quarterly interest compounding and Quarterly interest posting
     * with 360 days in year
     */
    @Test
    public void testRecurringDepositQuarterlyCompoundingAndQuarterlyPosting_360_Days() {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.accountHelper = new AccountHelper(this.requestSpec, this.responseSpec);
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
        DateFormat monthDayFormat = new SimpleDateFormat("dd MMM", Locale.US);
        DateFormat currentMonthFormat = new SimpleDateFormat("MM");
        DateFormat currentDateFormat = new SimpleDateFormat("dd");

        Calendar todaysDate = Calendar.getInstance();
        todaysDate.add(Calendar.YEAR, -1);
        Integer currentMonth = Integer.valueOf(currentMonthFormat.format(todaysDate.getTime()));
        Integer numberOfMonths = 12 - currentMonth;
        todaysDate.add(Calendar.MONTH, numberOfMonths);
        Integer currentDate = Integer.valueOf(currentDateFormat.format(todaysDate.getTime()));
        Integer daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);
        Integer daysLeft = daysInMonth - currentDate;
        todaysDate.add(Calendar.DATE, daysLeft + 1);
        daysInMonth = todaysDate.getActualMaximum(Calendar.DATE);
        LOG.info("{}", dateFormat.format(todaysDate.getTime()));
        final String VALID_FROM = dateFormat.format(todaysDate.getTime());

        final String VALID_TO = null;
        final String SUBMITTED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String APPROVED_ON_DATE = dateFormat.format(todaysDate.getTime());
        final String ACTIVATION_DATE = dateFormat.format(todaysDate.getTime());
        final String expectedFirstDepositOnDate = dateFormat.format(todaysDate.getTime());
        final String MONTH_DAY = monthDayFormat.format(todaysDate.getTime());

        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        final String accountingRule = NONE;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule);
        Assertions.assertNotNull(recurringDepositProductId);

        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, expectedFirstDepositOnDate);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap modificationsHashMap = this.recurringDepositAccountHelper.updateInterestCalculationConfigForRecurringDeposit(
                clientId.toString(), recurringDepositProductId.toString(), recurringDepositAccountId.toString(), SUBMITTED_ON_DATE,
                VALID_FROM, VALID_TO, DAYS_360, WHOLE_TERM, INTEREST_CALCULATION_USING_DAILY_BALANCE, QUARTERLY, QUARTERLY,
                expectedFirstDepositOnDate);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsApproved(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec,
                this.responseSpec, recurringDepositAccountId);
        HashMap recurringDepositSummary = this.recurringDepositAccountHelper.getRecurringDepositSummary(recurringDepositAccountId);
        Float principal = (Float) recurringDepositSummary.get("accountBalance");
        Float recurringDepositAmount = (Float) recurringDepositAccountData.get("mandatoryRecommendedDepositAmount");
        Integer depositPeriod = (Integer) recurringDepositAccountData.get("depositPeriod");
        HashMap daysInYearMap = (HashMap) recurringDepositAccountData.get("interestCalculationDaysInYearType");
        Integer daysInYear = (Integer) daysInYearMap.get("id");
        ArrayList<ArrayList<HashMap>> interestRateChartData = RecurringDepositProductHelper
                .getInterestRateChartSlabsByProductId(this.requestSpec, this.responseSpec, recurringDepositProductId);

        Float interestRate = RecurringDepositAccountHelper.getInterestRate(interestRateChartData, depositPeriod);
        double interestRateInFraction = interestRate / 100;
        double perDay = (double) 1 / daysInYear;
        LOG.info("per day = {} ", perDay);
        double interestPerDay = interestRateInFraction * perDay;

        principal = RecurringDepositAccountHelper.getPrincipalAfterCompoundingInterest(todaysDate, principal, recurringDepositAmount,
                depositPeriod, interestPerDay, QUARTERLY_INTERVAL, QUARTERLY_INTERVAL);

        recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec, this.responseSpec,
                recurringDepositAccountId);

        Float expectedPrematureAmount = principal;
        Float maturityAmount = (Float) recurringDepositAccountData.get("maturityAmount");

        Assertions.assertTrue(Math.abs(expectedPrematureAmount - maturityAmount) < THRESHOLD, "Verifying Pre-Closure maturity amount");

    }

    @Test
    public void testRecurringDepositAccountWithPeriodInterestRateChart() {
        final String chartToUse = "period";
        final String depositAmount = "1000";
        final String depositPeriod = "12";
        final Float interestRate = Float.valueOf((float) 6.0);
        testFixedDepositAccountForInterestRate(chartToUse, depositAmount, depositPeriod, interestRate);
    }

    @Test
    public void testRecurringDepositAccountWithPeriodInterestRateChart_AMOUNT_VARIATION() {
        final String chartToUse = "period";
        final String depositAmount = "10000";
        final String depositPeriod = "12";
        final Float interestRate = Float.valueOf((float) 6.0);
        testFixedDepositAccountForInterestRate(chartToUse, depositAmount, depositPeriod, interestRate);
    }

    @Test
    public void testRecurringDepositAccountWithPeriodInterestRateChart_PERIOD_VARIATION() {
        final String chartToUse = "period";
        final String depositAmount = "1000";
        final String depositPeriod = "18";
        final Float interestRate = Float.valueOf((float) 7.0);
        testFixedDepositAccountForInterestRate(chartToUse, depositAmount, depositPeriod, interestRate);
    }

    @Test
    public void testRecurringDepositAccountWithAmountInterestRateChart() {
        final String chartToUse = "amount";
        final String depositAmount = "1000";
        final String depositPeriod = "12";
        final Float interestRate = Float.valueOf((float) 8.0);
        testFixedDepositAccountForInterestRate(chartToUse, depositAmount, depositPeriod, interestRate);
    }

    @Test
    public void testRecurringDepositAccountWithAmountInterestRateChart_AMOUNT_VARIATION() {
        final String chartToUse = "amount";
        final String depositAmount = "500";
        final String depositPeriod = "12";
        final Float interestRate = Float.valueOf((float) 7.0);
        testFixedDepositAccountForInterestRate(chartToUse, depositAmount, depositPeriod, interestRate);
    }

    @Test
    public void testRecurringDepositAccountWithAmountInterestRateChart_PERIOD_VARIATION() {
        final String chartToUse = "amount";
        final String depositAmount = "500";
        final String depositPeriod = "10";
        final Float interestRate = Float.valueOf((float) 5.0);
        testFixedDepositAccountForInterestRate(chartToUse, depositAmount, depositPeriod, interestRate);
    }

    @Test
    public void testRecurringDepositAccountWithPeriodAndAmountInterestRateChart() {
        final String chartToUse = "period_amount";
        final String depositAmount = "1000";
        final String depositPeriod = "12";
        final Float interestRate = Float.valueOf((float) 7.0);
        testFixedDepositAccountForInterestRate(chartToUse, depositAmount, depositPeriod, interestRate);
    }

    @Test
    public void testRecurringDepositAccountWithPeriodAndAmountInterestRateChart_AMOUNT_VARIATION() {
        final String chartToUse = "period_amount";
        final String depositAmount = "400";
        final String depositPeriod = "12";
        final Float interestRate = Float.valueOf((float) 6.0);
        testFixedDepositAccountForInterestRate(chartToUse, depositAmount, depositPeriod, interestRate);
    }

    @Test
    public void testRecurringDepositAccountWithPeriodAndAmountInterestRateChart_PERIOD_VARIATION() {
        final String chartToUse = "period_amount";
        final String depositAmount = "1000";
        final String depositPeriod = "14";
        final Float interestRate = Float.valueOf((float) 8.0);
        testFixedDepositAccountForInterestRate(chartToUse, depositAmount, depositPeriod, interestRate);
    }

    @Test
    public void testRecurringDepositAccountWithAmountAndPeriodInterestRateChart() {
        final String chartToUse = "amount_period";
        final String depositAmount = "1000";
        final String depositPeriod = "12";
        final Float interestRate = Float.valueOf((float) 8.0);
        testFixedDepositAccountForInterestRate(chartToUse, depositAmount, depositPeriod, interestRate);
    }

    @Test
    public void testRecurringDepositAccountWithAmountAndPeriodInterestRateChart_AMOUNT_VARIATION() {
        final String chartToUse = "amount_period";
        final String depositAmount = "100";
        final String depositPeriod = "12";
        final Float interestRate = Float.valueOf((float) 6.0);
        testFixedDepositAccountForInterestRate(chartToUse, depositAmount, depositPeriod, interestRate);
    }

    @Test
    public void testRecurringDepositAccountWithAmountAndPeriodInterestRateChart_PERIOD_VARIATION() {
        final String chartToUse = "amount_period";
        final String depositAmount = "1000";
        final String depositPeriod = "6";
        final Float interestRate = Float.valueOf((float) 7.0);
        testFixedDepositAccountForInterestRate(chartToUse, depositAmount, depositPeriod, interestRate);
    }

    private void testFixedDepositAccountForInterestRate(final String chartToUse, final String depositAmount, final String depositPeriod,
            final Float interestRate) {
        this.recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec, this.responseSpec);
        this.accountHelper = new AccountHelper(this.requestSpec, this.responseSpec);
        this.savingsAccountHelper = new SavingsAccountHelper(this.requestSpec, this.responseSpec);
        this.recurringDepositAccountHelper = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec);

        final String VALID_FROM = "01 March 2014";
        final String VALID_TO = "01 March 2016";

        final String SUBMITTED_ON_DATE = "01 March 2015";
        final String APPROVED_ON_DATE = "01 March 2015";
        final String ACTIVATION_DATE = "01 March 2015";

        Integer clientId = ClientHelper.createClient(this.requestSpec, this.responseSpec);
        Assertions.assertNotNull(clientId);

        /***
         * Create FD product with CashBased accounting enabled
         */
        final String accountingRule = NONE;
        Integer recurringDepositProductId = createRecurringDepositProduct(VALID_FROM, VALID_TO, accountingRule, chartToUse);
        Assertions.assertNotNull(recurringDepositProductId);

        /***
         * Apply for FD account with created product and verify status
         */
        Integer recurringDepositAccountId = applyForRecurringDepositApplication(clientId.toString(), recurringDepositProductId.toString(),
                VALID_FROM, VALID_TO, SUBMITTED_ON_DATE, WHOLE_TERM, SUBMITTED_ON_DATE, depositAmount, depositPeriod);
        Assertions.assertNotNull(recurringDepositAccountId);

        HashMap recurringDepositAccountStatusHashMap = RecurringDepositAccountStatusChecker
                .getStatusOfRecurringDepositAccount(this.requestSpec, this.responseSpec, recurringDepositAccountId.toString());
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsPending(recurringDepositAccountStatusHashMap);

        /***
         * Approve the RD account and verify whether account is approved
         */
        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.approveRecurringDeposit(recurringDepositAccountId,
                APPROVED_ON_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsApproved(recurringDepositAccountStatusHashMap);

        /***
         * Activate the RD Account and verify whether account is activated
         */
        recurringDepositAccountStatusHashMap = this.recurringDepositAccountHelper.activateRecurringDeposit(recurringDepositAccountId,
                ACTIVATION_DATE);
        RecurringDepositAccountStatusChecker.verifyRecurringDepositIsActive(recurringDepositAccountStatusHashMap);

        HashMap recurringDepositAccountData = RecurringDepositAccountHelper.getRecurringDepositAccountById(this.requestSpec,
                this.responseSpec, recurringDepositAccountId);

        Assertions.assertEquals(interestRate, recurringDepositAccountData.get("nominalAnnualInterestRate"));
    }

    private Integer createRecurringDepositProduct(final String validFrom, final String validTo, final String accountingRule,
            Account... accounts) {
        LOG.info("------------------------------CREATING NEW RECURRING DEPOSIT PRODUCT ---------------------------------------");
        RecurringDepositProductHelper recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec,
                this.responseSpec);
        if (accountingRule.equals(CASH_BASED)) {
            recurringDepositProductHelper = recurringDepositProductHelper.withAccountingRuleAsCashBased(accounts);
        } else if (accountingRule.equals(NONE)) {
            recurringDepositProductHelper = recurringDepositProductHelper.withAccountingRuleAsNone();
        }
        final String recurringDepositProductJSON = recurringDepositProductHelper.withPeriodRangeChart().build(validFrom, validTo);
        return RecurringDepositProductHelper.createRecurringDepositProduct(recurringDepositProductJSON, requestSpec, responseSpec);
    }

    private Integer createRecurringDepositProductWithWithHoldTax(final String validFrom, final String validTo, final String taxGroupId,
            final String accountingRule, Account... accounts) {
        LOG.info("------------------------------CREATING NEW RECURRING DEPOSIT PRODUCT ---------------------------------------");
        RecurringDepositProductHelper recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec,
                this.responseSpec);
        if (accountingRule.equals(CASH_BASED)) {
            recurringDepositProductHelper = recurringDepositProductHelper.withAccountingRuleAsCashBased(accounts);
        } else if (accountingRule.equals(NONE)) {
            recurringDepositProductHelper = recurringDepositProductHelper.withAccountingRuleAsNone();
        }
        final String recurringDepositProductJSON = recurringDepositProductHelper.withPeriodRangeChart().withWithHoldTax(taxGroupId)//
                .build(validFrom, validTo);
        return RecurringDepositProductHelper.createRecurringDepositProduct(recurringDepositProductJSON, requestSpec, responseSpec);
    }

    private Integer createRecurringDepositProduct(final String validFrom, final String validTo, final String accountingRule,
            final String chartToBePicked, Account... accounts) {
        LOG.info("------------------------------CREATING NEW RECURRING DEPOSIT PRODUCT ---------------------------------------");
        RecurringDepositProductHelper recurringDepositProductHelper = new RecurringDepositProductHelper(this.requestSpec,
                this.responseSpec);
        if (accountingRule.equals(CASH_BASED)) {
            recurringDepositProductHelper = recurringDepositProductHelper.withAccountingRuleAsCashBased(accounts);
        } else if (accountingRule.equals(NONE)) {
            recurringDepositProductHelper = recurringDepositProductHelper.withAccountingRuleAsNone();
        }

        switch (chartToBePicked) {
            case "period":
                recurringDepositProductHelper = recurringDepositProductHelper.withPeriodRangeChart();
            break;
            case "amount":
                recurringDepositProductHelper = recurringDepositProductHelper.withAmountRangeChart();
            break;
            case "period_amount":
                recurringDepositProductHelper = recurringDepositProductHelper.withPeriodAndAmountRangeChart();
            break;
            case "amount_period":
                recurringDepositProductHelper = recurringDepositProductHelper.withAmountAndPeriodRangeChart();
            break;
            default:
            break;
        }
        final String recurringDepositProductJSON = recurringDepositProductHelper.build(validFrom, validTo);
        return RecurringDepositProductHelper.createRecurringDepositProduct(recurringDepositProductJSON, requestSpec, responseSpec);
    }

    private Integer applyForRecurringDepositApplication(final String clientID, final String productID, final String validFrom,
            final String validTo, final String submittedOnDate, final String penalInterestType, final String expectedFirstDepositOnDate) {
        LOG.info("--------------------------------APPLYING FOR RECURRING DEPOSIT ACCOUNT --------------------------------");
        final String recurringDepositApplicationJSON = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec)
                .withSubmittedOnDate(submittedOnDate).withExpectedFirstDepositOnDate(expectedFirstDepositOnDate)
                .build(clientID, productID, penalInterestType);
        return RecurringDepositAccountHelper.applyRecurringDepositApplication(recurringDepositApplicationJSON, this.requestSpec,
                this.responseSpec);
    }

    private Integer applyForRecurringDepositApplication(final String clientID, final String productID, final String validFrom,
            final String validTo, final String submittedOnDate, final String penalInterestType, final String expectedFirstDepositOnDate,
            final String depositAmount, final String depositPeriod) {
        LOG.info("--------------------------------APPLYING FOR RECURRING DEPOSIT ACCOUNT --------------------------------");
        final String recurringDepositApplicationJSON = new RecurringDepositAccountHelper(this.requestSpec, this.responseSpec)
                .withSubmittedOnDate(submittedOnDate).withExpectedFirstDepositOnDate(expectedFirstDepositOnDate)
                .withDepositPeriod(depositPeriod).withMandatoryDepositAmount(depositAmount).build(clientID, productID, penalInterestType);
        return RecurringDepositAccountHelper.applyRecurringDepositApplication(recurringDepositApplicationJSON, this.requestSpec,
                this.responseSpec);
    }

    private Integer createSavingsProduct(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final String minOpenningBalance, final String accountingRule, Account... accounts) {
        LOG.info("------------------------------CREATING NEW SAVINGS PRODUCT ---------------------------------------");
        SavingsProductHelper savingsProductHelper = new SavingsProductHelper();
        if (accountingRule.equals(CASH_BASED)) {
            savingsProductHelper = savingsProductHelper.withAccountingRuleAsCashBased(accounts);
        } else if (accountingRule.equals(NONE)) {
            savingsProductHelper = savingsProductHelper.withAccountingRuleAsNone();
        }
        final String savingsProductJSON = savingsProductHelper //
                .withInterestCompoundingPeriodTypeAsDaily() //
                .withInterestPostingPeriodTypeAsMonthly() //
                .withInterestCalculationPeriodTypeAsDailyBalance() //
                .withMinimumOpenningBalance(minOpenningBalance).build();
        return SavingsProductHelper.createSavingsProduct(savingsProductJSON, requestSpec, responseSpec);
    }

    private Account getMappedLiabilityFinancialAccount() {
        final Integer LIABILITY_TRANSFER_FINANCIAL_ACTIVITY_ID = FinancialActivity.LIABILITY_TRANSFER.getValue();
        List<HashMap> financialActivities = this.financialActivityAccountHelper.getAllFinancialActivityAccounts(this.responseSpec);
        final Account financialAccount;
        /***
         * if no financial activities are defined for account transfers, create liability financial accounting mappings
         */
        if (financialActivities.isEmpty()) {
            financialAccount = createLiabilityFinancialAccountTransferType(LIABILITY_TRANSFER_FINANCIAL_ACTIVITY_ID);
        } else {
            /***
             * extract mapped liability financial account
             */
            Account mappedLiabilityAccount = null;
            for (HashMap financialActivity : financialActivities) {
                HashMap financialActivityData = (HashMap) financialActivity.get("financialActivityData");
                if (financialActivityData.get("id").equals(LIABILITY_TRANSFER_FINANCIAL_ACTIVITY_ID)) {
                    HashMap glAccountData = (HashMap) financialActivity.get("glAccountData");
                    mappedLiabilityAccount = new Account((Integer) glAccountData.get("id"), AccountType.LIABILITY);
                    break;
                }
            }
            /***
             * If liability transfer is not defined create liability transfer
             */
            if (mappedLiabilityAccount == null) {
                mappedLiabilityAccount = createLiabilityFinancialAccountTransferType(LIABILITY_TRANSFER_FINANCIAL_ACTIVITY_ID);
            }
            financialAccount = mappedLiabilityAccount;
        }
        return financialAccount;
    }

    private Account createLiabilityFinancialAccountTransferType(final Integer liabilityTransferFinancialActivityId) {
        /***
         * Create and verify financial account transfer type is created
         */
        final Account liabilityAccountForMapping = this.accountHelper.createLiabilityAccount();
        Integer financialActivityAccountId = (Integer) financialActivityAccountHelper.createFinancialActivityAccount(
                liabilityTransferFinancialActivityId, liabilityAccountForMapping.getAccountID(), this.responseSpec,
                CommonConstants.RESPONSE_RESOURCE_ID);
        Assertions.assertNotNull(financialActivityAccountId);
        assertFinancialActivityAccountCreation(financialActivityAccountId, liabilityTransferFinancialActivityId,
                liabilityAccountForMapping);
        return liabilityAccountForMapping;
    }

    private void assertFinancialActivityAccountCreation(Integer financialActivityAccountId, Integer financialActivityId,
            Account glAccount) {
        HashMap mappingDetails = this.financialActivityAccountHelper.getFinancialActivityAccount(financialActivityAccountId,
                this.responseSpec);
        Assertions.assertEquals(financialActivityId, ((HashMap) mappingDetails.get("financialActivityData")).get("id"));
        Assertions.assertEquals(glAccount.getAccountID(), ((HashMap) mappingDetails.get("glAccountData")).get("id"));
    }

    private Integer createTaxGroup(final String percentage, final Account liabilityAccountForTax) {
        final Integer liabilityAccountId = liabilityAccountForTax.getAccountID();
        final Integer taxComponentId = TaxComponentHelper.createTaxComponent(this.requestSpec, this.responseSpec, percentage,
                liabilityAccountId);
        return TaxGroupHelper.createTaxGroup(this.requestSpec, this.responseSpec, Arrays.asList(taxComponentId));
    }

    /**
     * Delete the Liability transfer account
     */
    @AfterEach
    public void tearDown() {
        List<HashMap> financialActivities = this.financialActivityAccountHelper.getAllFinancialActivityAccounts(this.responseSpec);
        for (HashMap financialActivity : financialActivities) {
            Integer financialActivityAccountId = (Integer) financialActivity.get("id");
            Integer deletedFinancialActivityAccountId = this.financialActivityAccountHelper
                    .deleteFinancialActivityAccount(financialActivityAccountId, this.responseSpec, CommonConstants.RESPONSE_RESOURCE_ID);
            Assertions.assertNotNull(deletedFinancialActivityAccountId);
            Assertions.assertEquals(financialActivityAccountId, deletedFinancialActivityAccountId);
        }
    }
}
