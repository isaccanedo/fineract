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
package org.apache.fineract.portfolio.loanaccount.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.portfolio.account.data.AccountTransferData;
import org.apache.fineract.portfolio.paymentdetail.data.PaymentDetailData;
import org.apache.fineract.portfolio.paymenttype.data.PaymentTypeData;

/**
 * Immutable data object representing a loan transaction.
 */
@Getter
public class LoanTransactionData {

    private final Long id;
    private final Long officeId;
    private final String officeName;

    private final LoanTransactionEnumData type;

    private final LocalDate date;

    private final CurrencyData currency;
    private final PaymentDetailData paymentDetailData;

    private final BigDecimal amount;
    private final BigDecimal netDisbursalAmount;
    private final BigDecimal principalPortion;
    private final BigDecimal interestPortion;
    private final BigDecimal feeChargesPortion;
    private final BigDecimal penaltyChargesPortion;
    private final BigDecimal overpaymentPortion;
    private final BigDecimal unrecognizedIncomePortion;
    private final String externalId;
    private final AccountTransferData transfer;
    private final BigDecimal fixedEmiAmount;
    private final BigDecimal outstandingLoanBalance;
    private final LocalDate submittedOnDate;
    private final boolean manuallyReversed;
    private final LocalDate possibleNextRepaymentDate;

    private Collection<LoanChargePaidByData> loanChargePaidByList;

    // templates
    final Collection<PaymentTypeData> paymentTypeOptions;

    private Collection<CodeValueData> writeOffReasonOptions = null;

    private Integer numberOfRepayments = 0;

    // import fields
    private transient Integer rowIndex;
    private String dateFormat;
    private String locale;
    private BigDecimal transactionAmount;
    private LocalDate transactionDate;
    private Long paymentTypeId;
    private String accountNumber;
    private Integer checkNumber;
    private Integer routingCode;
    private Integer receiptNumber;
    private Integer bankNumber;
    private transient Long accountId;
    private transient String transactionType;
    private List<LoanRepaymentScheduleInstallmentData> loanRepaymentScheduleInstallments;

    // Reverse Data
    private String reversalExternalId;
    private LocalDate reversedOnDate;

    private List<LoanTransactionRelationData> transactionRelations;

    public static LoanTransactionData importInstance(BigDecimal repaymentAmount, LocalDate lastRepaymentDate, Long repaymentTypeId,
            Integer rowIndex, String locale, String dateFormat) {
        return new LoanTransactionData(repaymentAmount, lastRepaymentDate, repaymentTypeId, rowIndex, locale, dateFormat);
    }

    private LoanTransactionData(BigDecimal transactionAmount, LocalDate transactionDate, Long paymentTypeId, Integer rowIndex,
            String locale, String dateFormat) {
        this.transactionAmount = transactionAmount;
        this.transactionDate = transactionDate;
        this.paymentTypeId = paymentTypeId;
        this.rowIndex = rowIndex;
        this.dateFormat = dateFormat;
        this.locale = locale;
        this.amount = null;
        this.netDisbursalAmount = null;
        this.date = null;
        this.type = null;
        this.id = null;
        this.officeId = null;
        this.officeName = null;
        this.currency = null;
        this.paymentDetailData = null;
        this.principalPortion = null;
        this.interestPortion = null;
        this.feeChargesPortion = null;
        this.penaltyChargesPortion = null;
        this.overpaymentPortion = null;
        this.unrecognizedIncomePortion = null;
        this.externalId = null;
        this.transfer = null;
        this.fixedEmiAmount = null;
        this.outstandingLoanBalance = null;
        this.submittedOnDate = null;
        this.manuallyReversed = false;
        this.possibleNextRepaymentDate = null;
        this.paymentTypeOptions = null;
        this.writeOffReasonOptions = null;
    }

    public static LoanTransactionData importInstance(BigDecimal repaymentAmount, LocalDate repaymentDate, Long repaymentTypeId,
            String accountNumber, Integer checkNumber, Integer routingCode, Integer receiptNumber, Integer bankNumber, Long loanAccountId,
            String transactionType, Integer rowIndex, String locale, String dateFormat) {
        return new LoanTransactionData(repaymentAmount, repaymentDate, repaymentTypeId, accountNumber, checkNumber, routingCode,
                receiptNumber, bankNumber, loanAccountId, "", rowIndex, locale, dateFormat);
    }

    private LoanTransactionData(BigDecimal transactionAmount, LocalDate transactionDate, Long paymentTypeId, String accountNumber,
            Integer checkNumber, Integer routingCode, Integer receiptNumber, Integer bankNumber, Long accountId, String transactionType,
            Integer rowIndex, String locale, String dateFormat) {
        this.transactionAmount = transactionAmount;
        this.transactionDate = transactionDate;
        this.paymentTypeId = paymentTypeId;
        this.accountNumber = accountNumber;
        this.checkNumber = checkNumber;
        this.routingCode = routingCode;
        this.receiptNumber = receiptNumber;
        this.bankNumber = bankNumber;
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.rowIndex = rowIndex;
        this.dateFormat = dateFormat;
        this.locale = locale;
        this.id = null;
        this.officeId = null;
        this.officeName = null;
        this.type = null;
        this.date = null;
        this.currency = null;
        this.paymentDetailData = null;
        this.amount = null;
        this.netDisbursalAmount = null;
        this.principalPortion = null;
        this.interestPortion = null;
        this.feeChargesPortion = null;
        this.penaltyChargesPortion = null;
        this.overpaymentPortion = null;
        this.unrecognizedIncomePortion = null;
        this.externalId = null;
        this.transfer = null;
        this.fixedEmiAmount = null;
        this.outstandingLoanBalance = null;
        this.submittedOnDate = null;
        this.manuallyReversed = false;
        this.possibleNextRepaymentDate = null;
        this.paymentTypeOptions = null;
        this.writeOffReasonOptions = null;
    }

    public void setNumberOfRepayments(Integer numberOfRepayments) {
        this.numberOfRepayments = numberOfRepayments;
    }

    public void setLoanRepaymentScheduleInstallments(final List<LoanRepaymentScheduleInstallmentData> loanRepaymentScheduleInstallments) {
        this.loanRepaymentScheduleInstallments = loanRepaymentScheduleInstallments;
    }

    public static LoanTransactionData templateOnTop(final LoanTransactionData loanTransactionData,
            final Collection<PaymentTypeData> paymentTypeOptions) {
        return new LoanTransactionData(loanTransactionData.id, loanTransactionData.officeId, loanTransactionData.officeName,
                loanTransactionData.type, loanTransactionData.paymentDetailData, loanTransactionData.currency, loanTransactionData.date,
                loanTransactionData.amount, loanTransactionData.netDisbursalAmount, loanTransactionData.principalPortion,
                loanTransactionData.interestPortion, loanTransactionData.feeChargesPortion, loanTransactionData.penaltyChargesPortion,
                loanTransactionData.overpaymentPortion, loanTransactionData.unrecognizedIncomePortion, paymentTypeOptions,
                loanTransactionData.externalId, loanTransactionData.transfer, loanTransactionData.fixedEmiAmount,
                loanTransactionData.outstandingLoanBalance, loanTransactionData.manuallyReversed);

    }

    public LoanTransactionData(final Long id, final Long officeId, final String officeName, final LoanTransactionEnumData transactionType,
            final PaymentDetailData paymentDetailData, final CurrencyData currency, final LocalDate date, final BigDecimal amount,
            final BigDecimal netDisbursalAmount, final BigDecimal principalPortion, final BigDecimal interestPortion,
            final BigDecimal feeChargesPortion, final BigDecimal penaltyChargesPortion, final BigDecimal overpaymentPortion,
            final String externalId, final AccountTransferData transfer, BigDecimal fixedEmiAmount, BigDecimal outstandingLoanBalance,
            final BigDecimal unrecognizedIncomePortion, final boolean manuallyReversed) {
        this(id, officeId, officeName, transactionType, paymentDetailData, currency, date, amount, netDisbursalAmount, principalPortion,
                interestPortion, feeChargesPortion, penaltyChargesPortion, overpaymentPortion, unrecognizedIncomePortion, null, externalId,
                transfer, fixedEmiAmount, outstandingLoanBalance, manuallyReversed);
    }

    public LoanTransactionData(final Long id, final Long officeId, final String officeName, final LoanTransactionEnumData transactionType,
            final PaymentDetailData paymentDetailData, final CurrencyData currency, final LocalDate date, final BigDecimal amount,
            final BigDecimal netDisbursalAmount, final BigDecimal principalPortion, final BigDecimal interestPortion,
            final BigDecimal feeChargesPortion, final BigDecimal penaltyChargesPortion, final BigDecimal overpaymentPortion,
            BigDecimal unrecognizedIncomePortion, final Collection<PaymentTypeData> paymentTypeOptions, final String externalId,
            final AccountTransferData transfer, final BigDecimal fixedEmiAmount, BigDecimal outstandingLoanBalance,
            boolean manuallyReversed) {
        this(id, officeId, officeName, transactionType, paymentDetailData, currency, date, amount, netDisbursalAmount, principalPortion,
                interestPortion, feeChargesPortion, penaltyChargesPortion, overpaymentPortion, unrecognizedIncomePortion,
                paymentTypeOptions, externalId, transfer, fixedEmiAmount, outstandingLoanBalance, null, manuallyReversed, null, null);
    }

    public LoanTransactionData(final Long id, final Long officeId, final String officeName, final LoanTransactionEnumData transactionType,
            final PaymentDetailData paymentDetailData, final CurrencyData currency, final LocalDate date, final BigDecimal amount,
            final BigDecimal netDisbursalAmount, final BigDecimal principalPortion, final BigDecimal interestPortion,
            final BigDecimal feeChargesPortion, final BigDecimal penaltyChargesPortion, final BigDecimal overpaymentPortion,
            final BigDecimal unrecognizedIncomePortion, final String externalId, final AccountTransferData transfer,
            BigDecimal fixedEmiAmount, BigDecimal outstandingLoanBalance, LocalDate submittedOnDate, final boolean manuallyReversed,
            final String reversalExternalId, final LocalDate reversedOnDate) {
        this(id, officeId, officeName, transactionType, paymentDetailData, currency, date, amount, netDisbursalAmount, principalPortion,
                interestPortion, feeChargesPortion, penaltyChargesPortion, overpaymentPortion, unrecognizedIncomePortion, null, externalId,
                transfer, fixedEmiAmount, outstandingLoanBalance, submittedOnDate, manuallyReversed, reversalExternalId, reversedOnDate);
    }

    public LoanTransactionData(final Long id, final Long officeId, final String officeName, final LoanTransactionEnumData transactionType,
            final PaymentDetailData paymentDetailData, final CurrencyData currency, final LocalDate date, final BigDecimal amount,
            final BigDecimal netDisbursalAmount, final BigDecimal principalPortion, final BigDecimal interestPortion,
            final BigDecimal feeChargesPortion, final BigDecimal penaltyChargesPortion, final BigDecimal overpaymentPortion,
            final BigDecimal unrecognizedIncomePortion, final Collection<PaymentTypeData> paymentTypeOptions, final String externalId,
            final AccountTransferData transfer, final BigDecimal fixedEmiAmount, BigDecimal outstandingLoanBalance,
            final LocalDate submittedOnDate, final boolean manuallyReversed, final String reversalExternalId,
            final LocalDate reversedOnDate) {
        this.id = id;
        this.officeId = officeId;
        this.officeName = officeName;
        this.type = transactionType;
        this.paymentDetailData = paymentDetailData;
        this.currency = currency;
        this.date = date;
        this.amount = amount;
        this.netDisbursalAmount = netDisbursalAmount;
        this.principalPortion = principalPortion;
        this.interestPortion = interestPortion;
        this.feeChargesPortion = feeChargesPortion;
        this.penaltyChargesPortion = penaltyChargesPortion;
        this.unrecognizedIncomePortion = unrecognizedIncomePortion;
        this.paymentTypeOptions = paymentTypeOptions;
        this.externalId = externalId;
        this.transfer = transfer;
        this.overpaymentPortion = overpaymentPortion;
        this.fixedEmiAmount = fixedEmiAmount;
        this.outstandingLoanBalance = outstandingLoanBalance;
        this.submittedOnDate = submittedOnDate;
        this.manuallyReversed = manuallyReversed;
        this.possibleNextRepaymentDate = null;
        this.reversalExternalId = reversalExternalId;
        this.reversedOnDate = reversedOnDate;
    }

    public LoanTransactionData(Long id, LoanTransactionEnumData transactionType, LocalDate date, BigDecimal totalAmount,
            BigDecimal netDisbursalAmount, BigDecimal principalPortion, BigDecimal interestPortion, BigDecimal feeChargesPortion,
            BigDecimal penaltyChargesPortion, BigDecimal overpaymentPortion, BigDecimal unrecognizedIncomePortion,
            BigDecimal outstandingLoanBalance, final boolean manuallyReversed) {
        this(id, null, null, transactionType, null, null, date, totalAmount, netDisbursalAmount, principalPortion, interestPortion,
                feeChargesPortion, penaltyChargesPortion, overpaymentPortion, unrecognizedIncomePortion, null, null, null, null,
                outstandingLoanBalance, null, manuallyReversed, null, null);
    }

    public static LoanTransactionData loanTransactionDataForDisbursalTemplate(final LoanTransactionEnumData transactionType,
            final LocalDate expectedDisbursedOnLocalDateForTemplate, final BigDecimal disburseAmountForTemplate,
            final BigDecimal netDisbursalAmount, final Collection<PaymentTypeData> paymentOptions, final BigDecimal retriveLastEmiAmount,
            final LocalDate possibleNextRepaymentDate) {
        final Long id = null;
        final Long officeId = null;
        final String officeName = null;
        final PaymentDetailData paymentDetailData = null;
        final CurrencyData currency = null;
        final BigDecimal unrecognizedIncomePortion = null;
        final BigDecimal principalPortion = null;
        final BigDecimal interestPortion = null;
        final BigDecimal feeChargesPortion = null;
        final BigDecimal penaltyChargesPortion = null;
        final BigDecimal overpaymentPortion = null;
        final String externalId = null;
        final BigDecimal outstandingLoanBalance = null;
        final AccountTransferData transfer = null;
        final LocalDate submittedOnDate = null;
        final boolean manuallyReversed = false;
        return new LoanTransactionData(id, officeId, officeName, transactionType, paymentDetailData, currency,
                expectedDisbursedOnLocalDateForTemplate, disburseAmountForTemplate, netDisbursalAmount, principalPortion, interestPortion,
                feeChargesPortion, penaltyChargesPortion, overpaymentPortion, unrecognizedIncomePortion, paymentOptions, transfer,
                externalId, retriveLastEmiAmount, outstandingLoanBalance, submittedOnDate, manuallyReversed, possibleNextRepaymentDate);

    }

    private LoanTransactionData(Long id, final Long officeId, final String officeName, LoanTransactionEnumData transactionType,
            final PaymentDetailData paymentDetailData, final CurrencyData currency, final LocalDate date, BigDecimal amount,
            BigDecimal netDisbursalAmount, final BigDecimal principalPortion, final BigDecimal interestPortion,
            final BigDecimal feeChargesPortion, final BigDecimal penaltyChargesPortion, final BigDecimal overpaymentPortion,
            BigDecimal unrecognizedIncomePortion, Collection<PaymentTypeData> paymentOptions, final AccountTransferData transfer,
            final String externalId, final BigDecimal fixedEmiAmount, BigDecimal outstandingLoanBalance, final LocalDate submittedOnDate,
            final boolean manuallyReversed, final LocalDate possibleNextRepaymentDate) {
        this.id = id;
        this.officeId = officeId;
        this.officeName = officeName;
        this.type = transactionType;
        this.paymentDetailData = paymentDetailData;
        this.currency = currency;
        this.date = date;
        this.amount = amount;
        this.netDisbursalAmount = netDisbursalAmount;
        this.principalPortion = principalPortion;
        this.interestPortion = interestPortion;
        this.feeChargesPortion = feeChargesPortion;
        this.penaltyChargesPortion = penaltyChargesPortion;
        this.unrecognizedIncomePortion = unrecognizedIncomePortion;
        this.paymentTypeOptions = paymentOptions;
        this.externalId = externalId;
        this.transfer = transfer;
        this.overpaymentPortion = overpaymentPortion;
        this.fixedEmiAmount = fixedEmiAmount;
        this.outstandingLoanBalance = outstandingLoanBalance;
        this.submittedOnDate = submittedOnDate;
        this.manuallyReversed = manuallyReversed;
        this.possibleNextRepaymentDate = possibleNextRepaymentDate;
    }

    public boolean isNotDisbursement() {
        return type.getId() == 1;
    }

    public void setWriteOffReasonOptions(Collection<CodeValueData> writeOffReasonOptions) {
        this.writeOffReasonOptions = writeOffReasonOptions;
    }

    public void setLoanChargePaidByList(Collection<LoanChargePaidByData> loanChargePaidByList) {
        this.loanChargePaidByList = loanChargePaidByList;
    }

    public void setLoanTransactionRelations(List<LoanTransactionRelationData> transactionRelations) {
        this.transactionRelations = transactionRelations;
    }
}
