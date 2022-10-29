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
package org.apache.fineract.accounting.journalentry.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.accounting.closure.domain.GLClosure;
import org.apache.fineract.accounting.common.AccountingConstants.CashAccountsForShares;
import org.apache.fineract.accounting.journalentry.data.ChargePaymentDTO;
import org.apache.fineract.accounting.journalentry.data.SharesDTO;
import org.apache.fineract.accounting.journalentry.data.SharesTransactionDTO;
import org.apache.fineract.organisation.office.domain.Office;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CashBasedAccountingProcessorForShares implements AccountingProcessorForShares {

    private final AccountingProcessorHelper helper;

    @Override
    public void createJournalEntriesForShares(SharesDTO sharesDTO) {
        final GLClosure latestGLClosure = this.helper.getLatestClosureByBranch(sharesDTO.getOfficeId());
        final Long shareAccountId = sharesDTO.getShareAccountId();
        final Long shareProductId = sharesDTO.getShareProductId();
        final String currencyCode = sharesDTO.getCurrencyCode();
        for (SharesTransactionDTO transactionDTO : sharesDTO.getNewTransactions()) {
            final LocalDate transactionDate = transactionDTO.getTransactionDate();
            final String transactionId = transactionDTO.getTransactionId();
            final Office office = this.helper.getOfficeById(transactionDTO.getOfficeId());
            final Long paymentTypeId = transactionDTO.getPaymentTypeId();
            final BigDecimal amount = transactionDTO.getAmount();
            final BigDecimal chargeAmount = transactionDTO.getChargeAmount();
            final List<ChargePaymentDTO> feePayments = transactionDTO.getFeePayments();

            this.helper.checkForBranchClosures(latestGLClosure, transactionDate);

            if (transactionDTO.getTransactionType().isPurchased()) {
                createJournalEntriesForPurchase(shareAccountId, shareProductId, currencyCode, transactionDTO, transactionDate,
                        transactionId, office, paymentTypeId, amount, chargeAmount, feePayments);
            } else if (transactionDTO.getTransactionType().isRedeemed() && transactionDTO.getTransactionStatus().isApproved()) {
                createJournalEntriesForRedeem(shareAccountId, shareProductId, currencyCode, transactionDate, transactionId, office,
                        paymentTypeId, amount, chargeAmount, feePayments);

            } else if (transactionDTO.getTransactionType().isChargePayment()) {
                this.helper.createCashBasedJournalEntriesForSharesCharges(office, currencyCode, CashAccountsForShares.SHARES_REFERENCE,
                        CashAccountsForShares.INCOME_FROM_FEES, shareProductId, paymentTypeId, shareAccountId, transactionId,
                        transactionDate, amount, feePayments);
            }
        }

    }

    public void createJournalEntriesForRedeem(final Long shareAccountId, final Long shareProductId, final String currencyCode,
            final LocalDate transactionDate, final String transactionId, final Office office, final Long paymentTypeId,
            final BigDecimal amount, final BigDecimal chargeAmount, final List<ChargePaymentDTO> feePayments) {
        if (chargeAmount == null || chargeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            this.helper.createJournalEntriesForShares(office, currencyCode, CashAccountsForShares.SHARES_EQUITY.getValue(),
                    CashAccountsForShares.SHARES_REFERENCE.getValue(), shareProductId, paymentTypeId, shareAccountId, transactionId,
                    transactionDate, amount);
        } else {
            this.helper.createDebitJournalEntryForShares(office, currencyCode, CashAccountsForShares.SHARES_EQUITY.getValue(),
                    shareProductId, paymentTypeId, shareAccountId, transactionId, transactionDate, amount.add(chargeAmount));
            this.helper.createCreditJournalEntryForShares(office, currencyCode, CashAccountsForShares.SHARES_REFERENCE.getValue(),
                    shareProductId, paymentTypeId, shareAccountId, transactionId, transactionDate, amount);
            this.helper.createCashBasedJournalEntryForSharesCharges(office, currencyCode, CashAccountsForShares.INCOME_FROM_FEES,
                    shareProductId, shareAccountId, transactionId, transactionDate, chargeAmount, feePayments);
        }
    }

    public void createJournalEntriesForPurchase(final Long shareAccountId, final Long shareProductId, final String currencyCode,
            SharesTransactionDTO transactionDTO, final LocalDate transactionDate, final String transactionId, final Office office,
            final Long paymentTypeId, final BigDecimal amount, final BigDecimal chargeAmount, final List<ChargePaymentDTO> feePayments) {
        if (transactionDTO.getTransactionStatus().isApplied()) {
            if (chargeAmount == null || chargeAmount.compareTo(BigDecimal.ZERO) <= 0) {
                this.helper.createJournalEntriesForShares(office, currencyCode, CashAccountsForShares.SHARES_REFERENCE.getValue(),
                        CashAccountsForShares.SHARES_SUSPENSE.getValue(), shareProductId, paymentTypeId, shareAccountId, transactionId,
                        transactionDate, amount);
            } else {
                this.helper.createDebitJournalEntryForShares(office, currencyCode, CashAccountsForShares.SHARES_REFERENCE.getValue(),
                        shareProductId, paymentTypeId, shareAccountId, transactionId, transactionDate, amount);
                this.helper.createCreditJournalEntryForShares(office, currencyCode, CashAccountsForShares.SHARES_SUSPENSE.getValue(),
                        shareProductId, paymentTypeId, shareAccountId, transactionId, transactionDate, amount.subtract(chargeAmount));
                this.helper.createCashBasedJournalEntryForSharesCharges(office, currencyCode, CashAccountsForShares.INCOME_FROM_FEES,
                        shareProductId, shareAccountId, transactionId, transactionDate, chargeAmount, feePayments);
            }
        } else if (transactionDTO.getTransactionStatus().isApproved()) {
            BigDecimal amountForJE = amount;
            if (chargeAmount != null && chargeAmount.compareTo(BigDecimal.ZERO) > 0) {
                amountForJE = amount.subtract(chargeAmount);
            }
            this.helper.createJournalEntriesForShares(office, currencyCode, CashAccountsForShares.SHARES_SUSPENSE.getValue(),
                    CashAccountsForShares.SHARES_EQUITY.getValue(), shareProductId, paymentTypeId, shareAccountId, transactionId,
                    transactionDate, amountForJE);
        } else if (transactionDTO.getTransactionStatus().isRejected()) {
            if (chargeAmount != null && chargeAmount.compareTo(BigDecimal.ZERO) > 0) {
                this.helper.revertCashBasedJournalEntryForSharesCharges(office, currencyCode, CashAccountsForShares.INCOME_FROM_FEES,
                        shareProductId, shareAccountId, transactionId, transactionDate, chargeAmount, feePayments);
                this.helper.createDebitJournalEntryForShares(office, currencyCode, CashAccountsForShares.SHARES_SUSPENSE.getValue(),
                        shareProductId, paymentTypeId, shareAccountId, transactionId, transactionDate, amount.subtract(chargeAmount));
                this.helper.createCreditJournalEntryForShares(office, currencyCode, CashAccountsForShares.SHARES_REFERENCE.getValue(),
                        shareProductId, paymentTypeId, shareAccountId, transactionId, transactionDate, amount);

            } else {
                this.helper.createJournalEntriesForShares(office, currencyCode, CashAccountsForShares.SHARES_SUSPENSE.getValue(),
                        CashAccountsForShares.SHARES_REFERENCE.getValue(), shareProductId, paymentTypeId, shareAccountId, transactionId,
                        transactionDate, amount);
            }

        }
    }

}
