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
package org.apache.fineract.portfolio.loanaccount.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.portfolio.loanaccount.data.HolidayDetailDTO;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;

public interface LoanAccountDomainService {

    LoanTransaction makeRepayment(LoanTransactionType repaymentTransactionType, Loan loan, CommandProcessingResultBuilder builderResult,
            LocalDate transactionDate, BigDecimal transactionAmount, PaymentDetail paymentDetail, String noteText, String txnExternalId,
            boolean isRecoveryRepayment, String chargeRefundChargeType, boolean isAccountTransfer, HolidayDetailDTO holidatDetailDto,
            Boolean isHolidayValidationDone);

    LoanTransaction makeRefund(Long accountId, CommandProcessingResultBuilder builderResult, LocalDate transactionDate,
            BigDecimal transactionAmount, PaymentDetail paymentDetail, String noteText, String txnExternalId);

    LoanTransaction makeDisburseTransaction(Long loanId, LocalDate transactionDate, BigDecimal transactionAmount,
            PaymentDetail paymentDetail, String noteText, String txnExternalId, boolean isLoanToLoanTransfer);

    void reverseTransfer(LoanTransaction loanTransaction);

    LoanTransaction makeChargePayment(Loan loan, Long chargeId, LocalDate transactionDate, BigDecimal transactionAmount,
            PaymentDetail paymentDetail, String noteText, String txnExternalId, Integer transactionType, Integer installmentNumber);

    LoanTransaction makeDisburseTransaction(Long loanId, LocalDate transactionDate, BigDecimal transactionAmount,
            PaymentDetail paymentDetail, String noteText, String txnExternalId);

    LoanTransaction makeRefundForActiveLoan(Long accountId, CommandProcessingResultBuilder builderResult, LocalDate transactionDate,
            BigDecimal transactionAmount, PaymentDetail paymentDetail, String noteText, String txnExternalId);

    void updateLoanCollateralTransaction(Set<LoanCollateralManagement> loanCollateralManagementList);

    void updateLoanCollateralStatus(Set<LoanCollateralManagement> loanCollateralManagementSet, boolean isReleased);

    /**
     * This method is to recalculate and accrue the income till the last accrued date. this method is used when the
     * schedule changes due to interest recalculation
     *
     * @param loan
     */
    void recalculateAccruals(Loan loan);

    /**
     * This method is to set a Delinquency Tag If the loan is overdue, If the loan after the repayment transaction is
     * not overdue and It has a Delinquency Tag, It is removed
     *
     * @param loan
     * @param transactionDate
     */
    void setLoanDelinquencyTag(Loan loan, LocalDate transactionDate);

    LoanTransaction makeRepayment(LoanTransactionType repaymentTransactionType, Loan loan, CommandProcessingResultBuilder builderResult,
            LocalDate transactionDate, BigDecimal transactionAmount, PaymentDetail paymentDetail, String noteText, String txnExternalId,
            boolean isRecoveryRepayment, String chargeRefundChargeType, boolean isAccountTransfer, HolidayDetailDTO holidayDetailDto,
            Boolean isHolidayValidationDone, boolean isLoanToLoanTransfer);

    void saveLoanWithDataIntegrityViolationChecks(Loan loan);

    Map<String, Object> foreCloseLoan(Loan loan, LocalDate foreClourseDate, String noteText);

    /**
     * Disables all standing instructions linked to a closed loan
     *
     * @param loan
     *            {@link Loan} object
     */
    void disableStandingInstructionsLinkedToClosedLoan(Loan loan);

    void recalculateAccruals(Loan loan, boolean isInterestCalcualtionHappened);

    CommandProcessingResultBuilder creditBalanceRefund(Long loanId, LocalDate transactionDate, BigDecimal transactionAmount,
            String noteText, String externalId);
}
