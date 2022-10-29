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
package org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.loanaccount.data.LoanChargePaidDetail;
import org.apache.fineract.portfolio.loanaccount.domain.ChangedTransactionDetail;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargePaidBy;
import org.apache.fineract.portfolio.loanaccount.domain.LoanInstallmentCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanInterestRecalcualtionAdditionalDetails;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleProcessingWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionToRepaymentScheduleMapping;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionType;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.impl.CreocoreLoanRepaymentScheduleTransactionProcessor;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.impl.HeavensFamilyLoanRepaymentScheduleTransactionProcessor;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.impl.InterestPrincipalPenaltyFeesOrderLoanRepaymentScheduleTransactionProcessor;

/**
 * Abstract implementation of {@link LoanRepaymentScheduleTransactionProcessor} which is more convenient for concrete
 * implementations to extend.
 *
 * @see InterestPrincipalPenaltyFeesOrderLoanRepaymentScheduleTransactionProcessor
 *
 * @see HeavensFamilyLoanRepaymentScheduleTransactionProcessor
 * @see CreocoreLoanRepaymentScheduleTransactionProcessor
 */
public abstract class AbstractLoanRepaymentScheduleTransactionProcessor implements LoanRepaymentScheduleTransactionProcessor {

    /**
     * Provides support for passing all {@link LoanTransaction}'s so it will completely re-process the entire loan
     * schedule. This is required in cases where the {@link LoanTransaction} being processed is in the past and falls
     * before existing transactions or and adjustment is made to an existing in which case the entire loan schedule
     * needs to be re-processed.
     */

    @Override
    public ChangedTransactionDetail handleTransaction(final LocalDate disbursementDate,
            final List<LoanTransaction> transactionsPostDisbursement, final MonetaryCurrency currency,
            final List<LoanRepaymentScheduleInstallment> installments, final Set<LoanCharge> charges) {

        if (charges != null) {
            for (final LoanCharge loanCharge : charges) {
                if (!loanCharge.isDueAtDisbursement()) {
                    loanCharge.resetPaidAmount(currency);
                }
            }
        }

        for (final LoanRepaymentScheduleInstallment currentInstallment : installments) {
            currentInstallment.resetDerivedComponents();
            currentInstallment.updateDerivedFields(currency, disbursementDate);
        }

        // re-process loan charges over repayment periods (picking up on waived
        // loan charges)
        final LoanRepaymentScheduleProcessingWrapper wrapper = new LoanRepaymentScheduleProcessingWrapper();
        wrapper.reprocess(currency, disbursementDate, installments, charges);

        final ChangedTransactionDetail changedTransactionDetail = new ChangedTransactionDetail();
        final List<LoanTransaction> transactionstoBeProcessed = new ArrayList<>();
        for (final LoanTransaction loanTransaction : transactionsPostDisbursement) {
            if (loanTransaction.isChargePayment()) {
                List<LoanChargePaidDetail> chargePaidDetails = new ArrayList<>();
                final Set<LoanChargePaidBy> chargePaidBies = loanTransaction.getLoanChargesPaid();
                final Set<LoanCharge> transferCharges = new HashSet<>();
                for (final LoanChargePaidBy chargePaidBy : chargePaidBies) {
                    LoanCharge loanCharge = chargePaidBy.getLoanCharge();
                    transferCharges.add(loanCharge);
                    if (loanCharge.isInstalmentFee()) {
                        chargePaidDetails.addAll(loanCharge.fetchRepaymentInstallment(currency));
                    }
                }
                LocalDate startDate = disbursementDate;
                for (final LoanRepaymentScheduleInstallment installment : installments) {
                    for (final LoanCharge loanCharge : transferCharges) {
                        if (loanCharge.isDueForCollectionFromAndUpToAndIncluding(startDate, installment.getDueDate())) {
                            Money amountForProcess = loanCharge.getAmount(currency);
                            if (amountForProcess.isGreaterThan(loanTransaction.getAmount(currency))) {
                                amountForProcess = loanTransaction.getAmount(currency);
                            }
                            LoanChargePaidDetail chargePaidDetail = new LoanChargePaidDetail(amountForProcess, installment,
                                    loanCharge.isFeeCharge());
                            chargePaidDetails.add(chargePaidDetail);
                            break;
                        }
                    }
                    startDate = installment.getDueDate();
                }
                loanTransaction.resetDerivedComponents();
                Money unprocessed = loanTransaction.getAmount(currency);
                for (LoanChargePaidDetail chargePaidDetail : chargePaidDetails) {
                    final List<LoanRepaymentScheduleInstallment> processInstallments = new ArrayList<>(1);
                    processInstallments.add(chargePaidDetail.getInstallment());
                    Money processAmt = chargePaidDetail.getAmount();
                    if (processAmt.isGreaterThan(unprocessed)) {
                        processAmt = unprocessed;
                    }
                    unprocessed = handleTransactionAndCharges(loanTransaction, currency, processInstallments, transferCharges, processAmt,
                            chargePaidDetail.isFeeCharge());
                    if (!unprocessed.isGreaterThanZero()) {
                        break;
                    }
                }

                if (unprocessed.isGreaterThanZero()) {
                    onLoanOverpayment(loanTransaction, unprocessed);
                    loanTransaction.updateOverPayments(unprocessed);
                }

            } else {
                transactionstoBeProcessed.add(loanTransaction);
            }
        }

        for (final LoanTransaction loanTransaction : transactionstoBeProcessed) {
            // TODO: analyze and remove this
            if (!loanTransaction.getTypeOf().equals(LoanTransactionType.REFUND_FOR_ACTIVE_LOAN)) {
                final Comparator<LoanRepaymentScheduleInstallment> byDate = new Comparator<LoanRepaymentScheduleInstallment>() {

                    @Override
                    public int compare(LoanRepaymentScheduleInstallment ord1, LoanRepaymentScheduleInstallment ord2) {
                        return ord1.getDueDate().compareTo(ord2.getDueDate());
                    }
                };
                Collections.sort(installments, byDate);
            }

            if (loanTransaction.isRepaymentType() || loanTransaction.isInterestWaiver() || loanTransaction.isRecoveryRepayment()) {
                // pass through for new transactions
                if (loanTransaction.getId() == null) {
                    handleTransaction(loanTransaction, currency, installments, charges);
                    loanTransaction.adjustInterestComponent(currency);
                } else {
                    /**
                     * For existing transactions, check if the re-payment breakup (principal, interest, fees, penalties)
                     * has changed.<br>
                     **/
                    final LoanTransaction newLoanTransaction = LoanTransaction.copyTransactionProperties(loanTransaction);

                    // Reset derived component of new loan transaction and
                    // re-process transaction
                    handleTransaction(newLoanTransaction, currency, installments, charges);
                    newLoanTransaction.adjustInterestComponent(currency);
                    /**
                     * Check if the transaction amounts have changed. If so, reverse the original transaction and update
                     * changedTransactionDetail accordingly
                     **/
                    if (LoanTransaction.transactionAmountsMatch(currency, loanTransaction, newLoanTransaction)) {
                        loanTransaction.updateLoanTransactionToRepaymentScheduleMappings(
                                newLoanTransaction.getLoanTransactionToRepaymentScheduleMappings());
                    } else {
                        loanTransaction.reverse(loanTransaction.getExternalId());
                        loanTransaction.updateExternalId(null);
                        changedTransactionDetail.getNewTransactionMappings().put(loanTransaction.getId(), newLoanTransaction);
                    }
                }

            } else if (loanTransaction.isWriteOff()) {
                loanTransaction.resetDerivedComponents();
                handleWriteOff(loanTransaction, currency, installments);
            } else if (loanTransaction.isRefundForActiveLoan()) {
                loanTransaction.resetDerivedComponents();

                handleRefund(loanTransaction, currency, installments, charges);
            }
        }
        return changedTransactionDetail;
    }

    /**
     * Provides support for processing the latest transaction (which should be latest transaction) against the loan
     * schedule.
     */
    @Override
    public void handleTransaction(final LoanTransaction loanTransaction, final MonetaryCurrency currency,
            final List<LoanRepaymentScheduleInstallment> installments, final Set<LoanCharge> charges) {

        final Money amountToProcess = null;
        final boolean isChargeAmount = false;
        handleTransaction(loanTransaction, currency, installments, charges, amountToProcess, isChargeAmount);

    }

    private void handleTransaction(final LoanTransaction loanTransaction, final MonetaryCurrency currency,
            final List<LoanRepaymentScheduleInstallment> installments, final Set<LoanCharge> charges, final Money chargeAmountToProcess,
            final boolean isFeeCharge) {

        Money transactionAmountUnprocessed = handleTransactionAndCharges(loanTransaction, currency, installments, charges,
                chargeAmountToProcess, isFeeCharge);

        if (transactionAmountUnprocessed.isGreaterThanZero()) {
            if (loanTransaction.isWaiver()) {
                loanTransaction.updateComponentsAndTotal(transactionAmountUnprocessed.zero(), transactionAmountUnprocessed.zero(),
                        transactionAmountUnprocessed.zero(), transactionAmountUnprocessed.zero());
            } else {
                onLoanOverpayment(loanTransaction, transactionAmountUnprocessed);
                loanTransaction.updateOverPayments(transactionAmountUnprocessed);
            }
        }
    }

    private Money handleTransactionAndCharges(final LoanTransaction loanTransaction, final MonetaryCurrency currency,
            final List<LoanRepaymentScheduleInstallment> installments, final Set<LoanCharge> charges, final Money chargeAmountToProcess,
            final boolean isFeeCharge) {
        // to.
        if (loanTransaction.isRepaymentType() || loanTransaction.isInterestWaiver() || loanTransaction.isRecoveryRepayment()) {
            loanTransaction.resetDerivedComponents();
        }
        Money transactionAmountUnprocessed = processTransaction(loanTransaction, currency, installments, chargeAmountToProcess);

        final Set<LoanCharge> loanFees = extractFeeCharges(charges);
        final Set<LoanCharge> loanPenalties = extractPenaltyCharges(charges);
        Integer installmentNumber = null;
        if (loanTransaction.isChargePayment() && installments.size() == 1) {
            installmentNumber = installments.get(0).getInstallmentNumber();
        }

        if (loanTransaction.isNotWaiver() && !loanTransaction.isAccrual()) {
            Money feeCharges = loanTransaction.getFeeChargesPortion(currency);
            Money penaltyCharges = loanTransaction.getPenaltyChargesPortion(currency);
            if (chargeAmountToProcess != null && feeCharges.isGreaterThan(chargeAmountToProcess)) {
                if (isFeeCharge) {
                    feeCharges = chargeAmountToProcess;
                } else {
                    penaltyCharges = chargeAmountToProcess;
                }
            }
            if (feeCharges.isGreaterThanZero()) {
                updateChargesPaidAmountBy(loanTransaction, feeCharges, loanFees, installmentNumber);
            }

            if (penaltyCharges.isGreaterThanZero()) {
                updateChargesPaidAmountBy(loanTransaction, penaltyCharges, loanPenalties, installmentNumber);
            }
        }
        return transactionAmountUnprocessed;
    }

    private Money processTransaction(final LoanTransaction loanTransaction, final MonetaryCurrency currency,
            final List<LoanRepaymentScheduleInstallment> installments, Money amountToProcess) {
        int installmentIndex = 0;

        final LocalDate transactionDate = loanTransaction.getTransactionDate();
        Money transactionAmountUnprocessed = loanTransaction.getAmount(currency);
        if (amountToProcess != null) {
            transactionAmountUnprocessed = amountToProcess;
        }
        List<LoanTransactionToRepaymentScheduleMapping> transactionMappings = new ArrayList<>();

        for (final LoanRepaymentScheduleInstallment currentInstallment : installments) {
            if (transactionAmountUnprocessed.isGreaterThanZero()) {
                if (currentInstallment.isNotFullyPaidOff()) {

                    // is this transaction early/late/on-time with respect to
                    // the
                    // current installment?
                    if (isTransactionInAdvanceOfInstallment(installmentIndex, installments, transactionDate,
                            transactionAmountUnprocessed)) {
                        transactionAmountUnprocessed = handleTransactionThatIsPaymentInAdvanceOfInstallment(currentInstallment,
                                installments, loanTransaction, transactionDate, transactionAmountUnprocessed, transactionMappings);
                    } else if (isTransactionALateRepaymentOnInstallment(installmentIndex, installments,
                            loanTransaction.getTransactionDate())) {
                        // does this result in a late payment of existing
                        // installment?
                        transactionAmountUnprocessed = handleTransactionThatIsALateRepaymentOfInstallment(currentInstallment, installments,
                                loanTransaction, transactionAmountUnprocessed, transactionMappings);
                    } else {
                        // standard transaction
                        transactionAmountUnprocessed = handleTransactionThatIsOnTimePaymentOfInstallment(currentInstallment,
                                loanTransaction, transactionAmountUnprocessed, transactionMappings);
                    }
                }
            }

            installmentIndex++;
        }
        loanTransaction.updateLoanTransactionToRepaymentScheduleMappings(transactionMappings);
        return transactionAmountUnprocessed;
    }

    private Set<LoanCharge> extractFeeCharges(final Set<LoanCharge> loanCharges) {
        final Set<LoanCharge> feeCharges = new HashSet<>();
        for (final LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isFeeCharge()) {
                feeCharges.add(loanCharge);
            }
        }
        return feeCharges;
    }

    private Set<LoanCharge> extractPenaltyCharges(final Set<LoanCharge> loanCharges) {
        final Set<LoanCharge> penaltyCharges = new HashSet<>();
        for (final LoanCharge loanCharge : loanCharges) {
            if (loanCharge.isPenaltyCharge()) {
                penaltyCharges.add(loanCharge);
            }
        }
        return penaltyCharges;
    }

    private void updateChargesPaidAmountBy(final LoanTransaction loanTransaction, final Money feeCharges, final Set<LoanCharge> charges,
            final Integer installmentNumber) {

        Money amountRemaining = feeCharges;
        while (amountRemaining.isGreaterThanZero()) {
            final LoanCharge unpaidCharge = findEarliestUnpaidChargeFromUnOrderedSet(charges, feeCharges.getCurrency());
            Money feeAmount = feeCharges.zero();
            if (loanTransaction.isChargePayment()) {
                feeAmount = feeCharges;
            }
            if (unpaidCharge == null) {
                break; // All are trache charges
            }
            final Money amountPaidTowardsCharge = unpaidCharge.updatePaidAmountBy(amountRemaining, installmentNumber, feeAmount);
            if (!amountPaidTowardsCharge.isZero()) {
                Set<LoanChargePaidBy> chargesPaidBies = loanTransaction.getLoanChargesPaid();
                if (loanTransaction.isChargePayment()) {
                    for (final LoanChargePaidBy chargePaidBy : chargesPaidBies) {
                        LoanCharge loanCharge = chargePaidBy.getLoanCharge();
                        if (loanCharge.getId().equals(unpaidCharge.getId())) {
                            chargePaidBy.setAmount(amountPaidTowardsCharge.getAmount());
                        }
                    }
                } else {
                    final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(loanTransaction, unpaidCharge,
                            amountPaidTowardsCharge.getAmount(), installmentNumber);
                    chargesPaidBies.add(loanChargePaidBy);
                }
                amountRemaining = amountRemaining.minus(amountPaidTowardsCharge);
            }
        }

    }

    private LoanCharge findEarliestUnpaidChargeFromUnOrderedSet(final Set<LoanCharge> charges, final MonetaryCurrency currency) {
        LoanCharge earliestUnpaidCharge = null;
        LoanCharge installemntCharge = null;
        LoanInstallmentCharge chargePerInstallment = null;
        for (final LoanCharge loanCharge : charges) {
            if (loanCharge.getAmountOutstanding(currency).isGreaterThanZero() && !loanCharge.isDueAtDisbursement()) {
                if (loanCharge.isInstalmentFee()) {
                    LoanInstallmentCharge unpaidLoanChargePerInstallment = loanCharge.getUnpaidInstallmentLoanCharge();
                    if (chargePerInstallment == null || chargePerInstallment.getRepaymentInstallment().getDueDate()
                            .isAfter(unpaidLoanChargePerInstallment.getRepaymentInstallment().getDueDate())) {
                        installemntCharge = loanCharge;
                        chargePerInstallment = unpaidLoanChargePerInstallment;
                    }
                } else if (earliestUnpaidCharge == null || loanCharge.getDueLocalDate().isBefore(earliestUnpaidCharge.getDueLocalDate())) {
                    earliestUnpaidCharge = loanCharge;
                }
            }
        }
        if (earliestUnpaidCharge == null || (chargePerInstallment != null
                && earliestUnpaidCharge.getDueLocalDate().isAfter(chargePerInstallment.getRepaymentInstallment().getDueDate()))) {
            earliestUnpaidCharge = installemntCharge;
        }

        return earliestUnpaidCharge;
    }

    @Override
    public void handleWriteOff(final LoanTransaction loanTransaction, final MonetaryCurrency currency,
            final List<LoanRepaymentScheduleInstallment> installments) {

        final LocalDate transactionDate = loanTransaction.getTransactionDate();
        Money principalPortion = Money.zero(currency);
        Money interestPortion = Money.zero(currency);
        Money feeChargesPortion = Money.zero(currency);
        Money penaltychargesPortion = Money.zero(currency);

        // determine how much is written off in total and breakdown for
        // principal, interest and charges
        for (final LoanRepaymentScheduleInstallment currentInstallment : installments) {

            if (currentInstallment.isNotFullyPaidOff()) {
                principalPortion = principalPortion.plus(currentInstallment.writeOffOutstandingPrincipal(transactionDate, currency));
                interestPortion = interestPortion.plus(currentInstallment.writeOffOutstandingInterest(transactionDate, currency));
                feeChargesPortion = feeChargesPortion.plus(currentInstallment.writeOffOutstandingFeeCharges(transactionDate, currency));
                penaltychargesPortion = penaltychargesPortion
                        .plus(currentInstallment.writeOffOutstandingPenaltyCharges(transactionDate, currency));
            }
        }

        loanTransaction.updateComponentsAndTotal(principalPortion, interestPortion, feeChargesPortion, penaltychargesPortion);
    }

    // abstract interface
    /**
     * This method is responsible for checking if the current transaction is 'an advance/early payment' based on the
     * details passed through.
     *
     * Default implementation simply processes transactions as 'Late' if the transaction date is after the installment
     * due date.
     */
    protected boolean isTransactionALateRepaymentOnInstallment(final int installmentIndex,
            final List<LoanRepaymentScheduleInstallment> installments, final LocalDate transactionDate) {

        final LoanRepaymentScheduleInstallment currentInstallment = installments.get(installmentIndex);

        return transactionDate.isAfter(currentInstallment.getDueDate());
    }

    /**
     * For late repayments, how should components of installment be paid off
     *
     * @param transactionMappings
     *            TODO
     */
    protected abstract Money handleTransactionThatIsALateRepaymentOfInstallment(LoanRepaymentScheduleInstallment currentInstallment,
            List<LoanRepaymentScheduleInstallment> installments, LoanTransaction loanTransaction, Money transactionAmountUnprocessed,
            List<LoanTransactionToRepaymentScheduleMapping> transactionMappings);

    /**
     * This method is responsible for checking if the current transaction is 'an advance/early payment' based on the
     * details passed through.
     *
     * Default implementation is check transaction date is before installment due date.
     */
    protected boolean isTransactionInAdvanceOfInstallment(final int currentInstallmentIndex,
            final List<LoanRepaymentScheduleInstallment> installments, final LocalDate transactionDate,
            @SuppressWarnings("unused") final Money transactionAmount) {

        final LoanRepaymentScheduleInstallment currentInstallment = installments.get(currentInstallmentIndex);

        return transactionDate.isBefore(currentInstallment.getDueDate());
    }

    /**
     * For early/'in advance' repayments.
     *
     * @param transactionMappings
     *            TODO
     */
    protected abstract Money handleTransactionThatIsPaymentInAdvanceOfInstallment(LoanRepaymentScheduleInstallment currentInstallment,
            List<LoanRepaymentScheduleInstallment> installments, LoanTransaction loanTransaction, LocalDate transactionDate,
            Money paymentInAdvance, List<LoanTransactionToRepaymentScheduleMapping> transactionMappings);

    /**
     * For normal on-time repayments.
     *
     * @param transactionMappings
     *            TODO
     */
    protected abstract Money handleTransactionThatIsOnTimePaymentOfInstallment(LoanRepaymentScheduleInstallment currentInstallment,
            LoanTransaction loanTransaction, Money transactionAmountUnprocessed,
            List<LoanTransactionToRepaymentScheduleMapping> transactionMappings);

    /**
     * Invoked when a transaction results in an over-payment of the full loan.
     *
     * transaction amount is greater than the total expected principal and interest of the loan.
     */
    @SuppressWarnings("unused")
    protected void onLoanOverpayment(final LoanTransaction loanTransaction, final Money loanOverPaymentAmount) {
        // empty implementation by default.
    }

    @Override
    public Money handleRepaymentSchedule(final List<LoanTransaction> transactionsPostDisbursement, final MonetaryCurrency currency,
            final List<LoanRepaymentScheduleInstallment> installments) {
        Money unProcessed = Money.zero(currency);
        for (final LoanTransaction loanTransaction : transactionsPostDisbursement) {
            Money amountToProcess = null;
            if (loanTransaction.isRepaymentType() || loanTransaction.isInterestWaiver() || loanTransaction.isRecoveryRepayment()) {
                loanTransaction.resetDerivedComponents();
            }
            if (loanTransaction.isInterestWaiver()) {
                processTransaction(loanTransaction, currency, installments, amountToProcess);
            } else {
                unProcessed = processTransaction(loanTransaction, currency, installments, amountToProcess);
            }
        }
        return unProcessed;
    }

    @Override
    public boolean isInterestFirstRepaymentScheduleTransactionProcessor() {
        return false;
    }

    @Override
    public void handleChargeback(LoanTransaction loanTransaction, MonetaryCurrency currency, Money overpaidAmount,
            List<LoanRepaymentScheduleInstallment> installments) {
        List<LoanTransactionToRepaymentScheduleMapping> transactionMappings = new ArrayList<>();
        final Comparator<LoanRepaymentScheduleInstallment> byDate = new Comparator<LoanRepaymentScheduleInstallment>() {

            @Override
            public int compare(LoanRepaymentScheduleInstallment ord1, LoanRepaymentScheduleInstallment ord2) {
                return ord1.getDueDate().compareTo(ord2.getDueDate());
            }
        };
        Collections.sort(installments, byDate);
        final Money zeroMoney = Money.zero(currency);
        Money transactionAmountUnprocessed = loanTransaction.getAmount(currency);
        if (overpaidAmount.isGreaterThanZero()) {
            transactionAmountUnprocessed = loanTransaction.getAmount(currency).minus(overpaidAmount);
            if (transactionAmountUnprocessed.isLessThanZero()) {
                transactionAmountUnprocessed = zeroMoney;
            }
        }

        if (transactionAmountUnprocessed.isGreaterThanZero()) {
            final LocalDate transactionDate = loanTransaction.getTransactionDate();
            boolean loanTransactionMapped = false;
            LocalDate pastDueDate = null;
            for (final LoanRepaymentScheduleInstallment currentInstallment : installments) {
                pastDueDate = currentInstallment.getDueDate();
                if (!currentInstallment.isAdditional() && currentInstallment.getDueDate().isAfter(transactionDate)) {
                    currentInstallment.addToPrincipal(transactionDate, transactionAmountUnprocessed);
                    currentInstallment.addToCredits(transactionAmountUnprocessed.getAmount());
                    transactionMappings.add(LoanTransactionToRepaymentScheduleMapping.createFrom(loanTransaction, currentInstallment,
                            transactionAmountUnprocessed, zeroMoney, zeroMoney, zeroMoney));

                    loanTransactionMapped = true;

                    break;

                    // If already exists an additional installment just update the due date and
                    // principal from the Loan charge back transaction
                } else if (currentInstallment.isAdditional()) {
                    currentInstallment.updateDueChargeback(transactionDate, transactionAmountUnprocessed);
                    loanTransactionMapped = true;
                    break;
                }
            }

            // New installment will be added (N+1 scenario)
            if (!loanTransactionMapped) {
                Loan loan = loanTransaction.getLoan();
                final Set<LoanInterestRecalcualtionAdditionalDetails> compoundingDetails = null;
                LoanRepaymentScheduleInstallment installment = new LoanRepaymentScheduleInstallment(loan, (installments.size() + 1),
                        pastDueDate, transactionDate, transactionAmountUnprocessed.getAmount(), zeroMoney.getAmount(),
                        zeroMoney.getAmount(), zeroMoney.getAmount(), false, compoundingDetails);
                installment.markAsAdditional();
                installment.addToCredits(transactionAmountUnprocessed.getAmount());
                loan.addLoanRepaymentScheduleInstallment(installment);

                transactionMappings.add(LoanTransactionToRepaymentScheduleMapping.createFrom(loanTransaction, installment,
                        transactionAmountUnprocessed, zeroMoney, zeroMoney, zeroMoney));
            }

            loanTransaction.updateLoanTransactionToRepaymentScheduleMappings(transactionMappings);
        }
    }

    @Override
    public void handleRefund(LoanTransaction loanTransaction, MonetaryCurrency currency,
            List<LoanRepaymentScheduleInstallment> installments, final Set<LoanCharge> charges) {
        // TODO Auto-generated method stub
        List<LoanTransactionToRepaymentScheduleMapping> transactionMappings = new ArrayList<>();
        final Comparator<LoanRepaymentScheduleInstallment> byDate = new Comparator<LoanRepaymentScheduleInstallment>() {

            @Override
            public int compare(LoanRepaymentScheduleInstallment ord1, LoanRepaymentScheduleInstallment ord2) {
                return ord1.getDueDate().compareTo(ord2.getDueDate());
            }
        };
        Collections.sort(installments, Collections.reverseOrder(byDate));
        Money transactionAmountUnprocessed = loanTransaction.getAmount(currency);

        for (final LoanRepaymentScheduleInstallment currentInstallment : installments) {
            Money outstanding = currentInstallment.getTotalOutstanding(currency);
            Money due = currentInstallment.getDue(currency);

            if (outstanding.isLessThan(due)) {
                transactionAmountUnprocessed = handleRefundTransactionPaymentOfInstallment(currentInstallment, loanTransaction,
                        transactionAmountUnprocessed, transactionMappings);

            }

            if (transactionAmountUnprocessed.isZero()) {
                break;
            }

        }

        final Set<LoanCharge> loanFees = extractFeeCharges(charges);
        final Set<LoanCharge> loanPenalties = extractPenaltyCharges(charges);
        Integer installmentNumber = null;

        final Money feeCharges = loanTransaction.getFeeChargesPortion(currency);
        if (feeCharges.isGreaterThanZero()) {
            undoChargesPaidAmountBy(loanTransaction, feeCharges, loanFees, installmentNumber);
        }

        final Money penaltyCharges = loanTransaction.getPenaltyChargesPortion(currency);
        if (penaltyCharges.isGreaterThanZero()) {
            undoChargesPaidAmountBy(loanTransaction, penaltyCharges, loanPenalties, installmentNumber);
        }
        loanTransaction.updateLoanTransactionToRepaymentScheduleMappings(transactionMappings);
    }

    /**
     * Invoked when a there is a refund of an active loan or undo of an active loan
     *
     * Undoes principal, interest, fees and charges of this transaction based on the repayment strategy
     *
     * @param transactionMappings
     *            TODO
     *
     */
    protected abstract Money handleRefundTransactionPaymentOfInstallment(LoanRepaymentScheduleInstallment currentInstallment,
            LoanTransaction loanTransaction, Money transactionAmountUnprocessed,
            List<LoanTransactionToRepaymentScheduleMapping> transactionMappings);

    private void undoChargesPaidAmountBy(final LoanTransaction loanTransaction, final Money feeCharges, final Set<LoanCharge> charges,
            final Integer installmentNumber) {

        Money amountRemaining = feeCharges;
        while (amountRemaining.isGreaterThanZero()) {
            final LoanCharge paidCharge = findLatestPaidChargeFromUnOrderedSet(charges, feeCharges.getCurrency());

            if (paidCharge != null) {
                Money feeAmount = feeCharges.zero();

                final Money amountDeductedTowardsCharge = paidCharge.undoPaidOrPartiallyAmountBy(amountRemaining, installmentNumber,
                        feeAmount);
                if (amountDeductedTowardsCharge.isGreaterThanZero()) {

                    final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(loanTransaction, paidCharge,
                            amountDeductedTowardsCharge.getAmount().multiply(new BigDecimal(-1)), null);
                    loanTransaction.getLoanChargesPaid().add(loanChargePaidBy);

                    amountRemaining = amountRemaining.minus(amountDeductedTowardsCharge);
                }
            }
        }

    }

    private LoanCharge findLatestPaidChargeFromUnOrderedSet(final Set<LoanCharge> charges, MonetaryCurrency currency) {
        LoanCharge latestPaidCharge = null;
        LoanCharge installemntCharge = null;
        LoanInstallmentCharge chargePerInstallment = null;
        for (final LoanCharge loanCharge : charges) {
            boolean isPaidOrPartiallyPaid = loanCharge.isPaidOrPartiallyPaid(currency);
            if (isPaidOrPartiallyPaid && !loanCharge.isDueAtDisbursement()) {
                if (loanCharge.isInstalmentFee()) {
                    LoanInstallmentCharge paidLoanChargePerInstallment = loanCharge
                            .getLastPaidOrPartiallyPaidInstallmentLoanCharge(currency);
                    if (chargePerInstallment == null
                            || (paidLoanChargePerInstallment != null && chargePerInstallment.getRepaymentInstallment().getDueDate()
                                    .isBefore(paidLoanChargePerInstallment.getRepaymentInstallment().getDueDate()))) {
                        installemntCharge = loanCharge;
                        chargePerInstallment = paidLoanChargePerInstallment;
                    }
                } else if (latestPaidCharge == null || (loanCharge.isPaidOrPartiallyPaid(currency)
                        && loanCharge.getDueLocalDate().isAfter(latestPaidCharge.getDueLocalDate()))) {
                    latestPaidCharge = loanCharge;
                }
            }
        }
        if (latestPaidCharge == null || (chargePerInstallment != null
                && latestPaidCharge.getDueLocalDate().isAfter(chargePerInstallment.getRepaymentInstallment().getDueDate()))) {
            latestPaidCharge = installemntCharge;
        }

        return latestPaidCharge;
    }

    @Override
    public void processTransactionsFromDerivedFields(List<LoanTransaction> transactionsPostDisbursement, MonetaryCurrency currency,
            List<LoanRepaymentScheduleInstallment> installments, final Set<LoanCharge> charges) {
        for (final LoanTransaction loanTransaction : transactionsPostDisbursement) {
            if (!loanTransaction.isAccrualTransaction()) {
                processTransactionFromDerivedFields(loanTransaction, currency, installments, charges);
            }
        }
    }

    private void processTransactionFromDerivedFields(final LoanTransaction loanTransaction, MonetaryCurrency currency,
            List<LoanRepaymentScheduleInstallment> installments, final Set<LoanCharge> charges) {
        Money principal = loanTransaction.getPrincipalPortion(currency);
        Money interest = loanTransaction.getInterestPortion(currency);
        if (loanTransaction.isInterestWaiver()) {
            interest = loanTransaction.getAmount(currency);
        }
        Money feeCharges = loanTransaction.getFeeChargesPortion(currency);
        Money penaltyCharges = loanTransaction.getPenaltyChargesPortion(currency);
        final LocalDate transactionDate = loanTransaction.getTransactionDate();
        if (principal.isGreaterThanZero() || interest.isGreaterThanZero() || feeCharges.isGreaterThanZero()
                || penaltyCharges.isGreaterThanZero()) {
            for (final LoanRepaymentScheduleInstallment currentInstallment : installments) {
                if (currentInstallment.isNotFullyPaidOff()) {
                    if (penaltyCharges.isGreaterThanZero()) {
                        Money penaltyChargesPortion = Money.zero(currency);
                        if (loanTransaction.isWaiver()) {
                            penaltyChargesPortion = currentInstallment.waivePenaltyChargesComponent(transactionDate, penaltyCharges);
                        } else {
                            penaltyChargesPortion = currentInstallment.payPenaltyChargesComponent(transactionDate, penaltyCharges);
                        }
                        penaltyCharges = penaltyCharges.minus(penaltyChargesPortion);
                    }

                    if (feeCharges.isGreaterThanZero()) {
                        Money feeChargesPortion = Money.zero(currency);
                        if (loanTransaction.isWaiver()) {
                            feeChargesPortion = currentInstallment.waiveFeeChargesComponent(transactionDate, feeCharges);
                        } else {
                            feeChargesPortion = currentInstallment.payFeeChargesComponent(transactionDate, feeCharges);
                        }
                        feeCharges = feeCharges.minus(feeChargesPortion);
                    }

                    if (interest.isGreaterThanZero()) {
                        Money interestPortion = Money.zero(currency);
                        if (loanTransaction.isWaiver()) {
                            interestPortion = currentInstallment.waiveInterestComponent(transactionDate, interest);
                        } else {
                            interestPortion = currentInstallment.payInterestComponent(transactionDate, interest);
                        }
                        interest = interest.minus(interestPortion);
                    }

                    if (principal.isGreaterThanZero()) {
                        Money principalPortion = currentInstallment.payPrincipalComponent(transactionDate, principal);
                        principal = principal.minus(principalPortion);
                    }
                }
                if (!(principal.isGreaterThanZero() || interest.isGreaterThanZero() || feeCharges.isGreaterThanZero()
                        || penaltyCharges.isGreaterThanZero())) {
                    break;
                }
            }
        }

        final Set<LoanCharge> loanFees = extractFeeCharges(charges);
        final Set<LoanCharge> loanPenalties = extractPenaltyCharges(charges);
        Integer installmentNumber = null;
        if (loanTransaction.isChargePayment() && installments.size() == 1) {
            installmentNumber = installments.get(0).getInstallmentNumber();
        }

        if (loanTransaction.isNotWaiver()) {
            feeCharges = loanTransaction.getFeeChargesPortion(currency);
            penaltyCharges = loanTransaction.getPenaltyChargesPortion(currency);
            if (feeCharges.isGreaterThanZero()) {
                updateChargesPaidAmountBy(loanTransaction, feeCharges, loanFees, installmentNumber);
            }

            if (penaltyCharges.isGreaterThanZero()) {
                updateChargesPaidAmountBy(loanTransaction, penaltyCharges, loanPenalties, installmentNumber);
            }
        }
    }

}
