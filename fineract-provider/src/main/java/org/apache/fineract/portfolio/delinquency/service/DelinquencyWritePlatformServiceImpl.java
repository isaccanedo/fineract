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
package org.apache.fineract.portfolio.delinquency.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.delinquency.api.DelinquencyApiConstants;
import org.apache.fineract.portfolio.delinquency.data.DelinquencyBucketData;
import org.apache.fineract.portfolio.delinquency.data.DelinquencyRangeData;
import org.apache.fineract.portfolio.delinquency.domain.DelinquencyBucket;
import org.apache.fineract.portfolio.delinquency.domain.DelinquencyBucketMappings;
import org.apache.fineract.portfolio.delinquency.domain.DelinquencyBucketMappingsRepository;
import org.apache.fineract.portfolio.delinquency.domain.DelinquencyBucketRepository;
import org.apache.fineract.portfolio.delinquency.domain.DelinquencyRange;
import org.apache.fineract.portfolio.delinquency.domain.DelinquencyRangeRepository;
import org.apache.fineract.portfolio.delinquency.domain.LoanDelinquencyTagHistory;
import org.apache.fineract.portfolio.delinquency.domain.LoanDelinquencyTagHistoryRepository;
import org.apache.fineract.portfolio.delinquency.exception.DelinquencyBucketAgesOverlapedException;
import org.apache.fineract.portfolio.delinquency.exception.DelinquencyRangeInvalidAgesException;
import org.apache.fineract.portfolio.delinquency.validator.DelinquencyBucketParseAndValidator;
import org.apache.fineract.portfolio.delinquency.validator.DelinquencyRangeParseAndValidator;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallmentRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DelinquencyWritePlatformServiceImpl implements DelinquencyWritePlatformService {

    private final DelinquencyBucketParseAndValidator dataValidatorBucket;
    private final DelinquencyRangeParseAndValidator dataValidatorRange;

    private final DelinquencyRangeRepository repositoryRange;
    private final DelinquencyBucketRepository repositoryBucket;
    private final DelinquencyBucketMappingsRepository repositoryBucketMappings;
    private final LoanDelinquencyTagHistoryRepository loanDelinquencyTagRepository;
    private final LoanRepositoryWrapper loanRepository;
    private final LoanProductRepository loanProductRepository;
    private final LoanRepaymentScheduleInstallmentRepository loanRepaymentScheduleInstallmentRepository;

    @Override
    public CommandProcessingResult createDelinquencyRange(JsonCommand command) {
        DelinquencyRangeData data = dataValidatorRange.validateAndParseUpdate(command);
        Map<String, Object> changes = new HashMap<>();
        DelinquencyRange delinquencyRange = createDelinquencyRange(data, changes);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(delinquencyRange.getId()).with(changes)
                .build();
    }

    @Override
    public CommandProcessingResult updateDelinquencyRange(Long delinquencyRangeId, JsonCommand command) {
        DelinquencyRangeData data = dataValidatorRange.validateAndParseUpdate(command);
        DelinquencyRange delinquencyRange = this.repositoryRange.getReferenceById(delinquencyRangeId);
        Map<String, Object> changes = new HashMap<>();
        delinquencyRange = updateDelinquencyRange(delinquencyRange, data, changes);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(delinquencyRange.getId()).with(changes)
                .build();
    }

    @Override
    public CommandProcessingResult deleteDelinquencyRange(Long delinquencyRangeId, JsonCommand command) {
        final DelinquencyRange delinquencyRange = repositoryRange.getReferenceById(delinquencyRangeId);
        if (delinquencyRange != null) {
            final Long delinquencyRangeLinked = this.loanDelinquencyTagRepository.countByDelinquencyRange(delinquencyRange);
            if (delinquencyRangeLinked > 0) {
                throw new PlatformDataIntegrityException("error.msg.data.integrity.issue.entity.linked",
                        "Data integrity issue with resource: " + delinquencyRange.getId());
            }
            repositoryRange.delete(delinquencyRange);
        }
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(delinquencyRange.getId()).build();
    }

    @Override
    public CommandProcessingResult createDelinquencyBucket(JsonCommand command) {
        DelinquencyBucketData data = dataValidatorBucket.validateAndParseUpdate(command);
        Map<String, Object> changes = new HashMap<>();
        DelinquencyBucket delinquencyBucket = createDelinquencyBucket(data, changes);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(delinquencyBucket.getId()).with(changes)
                .build();
    }

    @Override
    public CommandProcessingResult updateDelinquencyBucket(Long delinquencyBucketId, JsonCommand command) {
        DelinquencyBucketData data = dataValidatorBucket.validateAndParseUpdate(command);
        DelinquencyBucket delinquencyBucket = this.repositoryBucket.getReferenceById(delinquencyBucketId);

        Map<String, Object> changes = new HashMap<>();
        delinquencyBucket = updateDelinquencyBucket(delinquencyBucket, data, changes);
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(delinquencyBucket.getId()).with(changes)
                .build();
    }

    @Override
    public CommandProcessingResult deleteDelinquencyBucket(Long delinquencyBucketId, JsonCommand command) {
        final DelinquencyBucket delinquencyBucket = repositoryBucket.getReferenceById(delinquencyBucketId);
        if (delinquencyBucket != null) {
            Long delinquencyBucketLinked = this.loanProductRepository.countByDelinquencyBucket(delinquencyBucket);
            if (delinquencyBucketLinked > 0) {
                throw new PlatformDataIntegrityException("error.msg.data.integrity.issue.entity.linked",
                        "Data integrity issue with resource: " + delinquencyBucket.getId());
            }
            repositoryBucket.delete(delinquencyBucket);
        }
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(delinquencyBucket.getId()).build();
    }

    @Override
    public CommandProcessingResult applyDelinquencyTagToLoan(Long loanId, JsonCommand command) {
        Map<String, Object> changes = new HashMap<>();

        final Loan loan = this.loanRepository.findOneWithNotFoundDetection(loanId);
        final DelinquencyBucket delinquencyBucket = loan.getLoanProduct().getDelinquencyBucket();
        if (delinquencyBucket != null) {
            final LocalDate businessDate = DateUtils.getBusinessLocalDate();
            LoanRepaymentScheduleInstallment loanRepaymentSchedule = this.loanRepaymentScheduleInstallmentRepository
                    .findFirstByLoanAndDueDateLessThanEqualAndObligationsMetOnDateOrderByDueDate(loan, businessDate, null);

            Long overdueDays = 0L;
            if (loanRepaymentSchedule != null) {
                overdueDays = DateUtils.getDifferenceInDays(loanRepaymentSchedule.getDueDate(), businessDate);
            }
            changes = lookUpDelinquencyRange(loan, delinquencyBucket, overdueDays);
        }
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(loan.getId()).with(changes).build();
    }

    @Override
    public void applyDelinquencyTagToLoan(Long loanId, Long ageDays) {
        final Loan loan = this.loanRepository.findOneWithNotFoundDetection(loanId);
        applyDelinquencyTagToLoan(loan, ageDays);
    }

    @Override
    public void applyDelinquencyTagToLoan(final Loan loan, Long ageDays) {
        if (loan.hasDelinquencyBucket()) {
            lookUpDelinquencyRange(loan, loan.getLoanProduct().getDelinquencyBucket(), ageDays);
        }
    }

    @Override
    public void removeDelinquencyTagToLoan(final Loan loan) {
        setLoanDelinquencyTag(loan, null);
    }

    @Override
    public void cleanLoanDelinquencyTags(Loan loan) {
        List<LoanDelinquencyTagHistory> loanDelinquencyTags = this.loanDelinquencyTagRepository.findByLoan(loan);
        if (loanDelinquencyTags != null && loanDelinquencyTags.size() > 0) {
            this.loanDelinquencyTagRepository.deleteAll(loanDelinquencyTags);
        }
    }

    private DelinquencyRange createDelinquencyRange(DelinquencyRangeData data, Map<String, Object> changes) {
        Optional<DelinquencyRange> delinquencyRange = repositoryRange.findByClassification(data.getClassification());

        if (delinquencyRange.isEmpty()) {
            if (data.getMaximumAgeDays() != null && data.getMinimumAgeDays() > data.getMaximumAgeDays()) {
                final String errorMessage = "The age days values are invalid, the maximum age days can't be lower than minimum age days";
                throw new DelinquencyRangeInvalidAgesException(errorMessage, data.getMinimumAgeDays(), data.getMaximumAgeDays());
            }
            DelinquencyRange newDelinquencyRange = DelinquencyRange.instance(data.getClassification(), data.getMinimumAgeDays(),
                    data.getMaximumAgeDays());
            return repositoryRange.saveAndFlush(newDelinquencyRange);
        } else {
            throw new PlatformDataIntegrityException("error.msg.data.integrity.issue.entity.duplicated",
                    "Data integrity issue with resource: " + delinquencyRange.get().getId());
        }
    }

    private DelinquencyRange updateDelinquencyRange(DelinquencyRange delinquencyRange, DelinquencyRangeData data,
            Map<String, Object> changes) {
        if (!data.getClassification().equalsIgnoreCase(delinquencyRange.getClassification())) {
            delinquencyRange.setClassification(data.getClassification());
            changes.put(DelinquencyApiConstants.CLASSIFICATION_PARAM_NAME, data.getClassification());
        }
        if (!data.getMinimumAgeDays().equals(delinquencyRange.getMinimumAgeDays())) {
            delinquencyRange.setMinimumAgeDays(data.getMinimumAgeDays());
            changes.put(DelinquencyApiConstants.MINIMUMAGEDAYS_PARAM_NAME, data.getMinimumAgeDays());
        }
        if (!data.getMaximumAgeDays().equals(delinquencyRange.getMaximumAgeDays())) {
            delinquencyRange.setMaximumAgeDays(data.getMaximumAgeDays());
            changes.put(DelinquencyApiConstants.MAXIMUMAGEDAYS_PARAM_NAME, data.getMaximumAgeDays());
        }
        if (!changes.isEmpty()) {
            delinquencyRange = repositoryRange.saveAndFlush(delinquencyRange);
        }
        return delinquencyRange;
    }

    private DelinquencyBucket createDelinquencyBucket(DelinquencyBucketData data, Map<String, Object> changes) {
        Optional<DelinquencyBucket> delinquencyBucket = repositoryBucket.findByName(data.getName());

        if (delinquencyBucket.isEmpty()) {
            DelinquencyBucket newDelinquencyBucket = new DelinquencyBucket(data.getName());
            repositoryBucket.save(newDelinquencyBucket);
            setDelinquencyBucketMappings(newDelinquencyBucket, data);
            return newDelinquencyBucket;
        } else {
            throw new PlatformDataIntegrityException("error.msg.data.integrity.issue.entity.duplicated",
                    "Data integrity issue with resource: " + delinquencyBucket.get().getId());
        }
    }

    private DelinquencyBucket updateDelinquencyBucket(DelinquencyBucket delinquencyBucket, DelinquencyBucketData data,
            Map<String, Object> changes) {
        if (!data.getName().equalsIgnoreCase(delinquencyBucket.getName())) {
            delinquencyBucket.setName(data.getName());
            changes.put(DelinquencyApiConstants.NAME_PARAM_NAME, data.getName());
        }
        if (!changes.isEmpty()) {
            delinquencyBucket = repositoryBucket.save(delinquencyBucket);
        }
        setDelinquencyBucketMappings(delinquencyBucket, data);
        return delinquencyBucket;
    }

    private void setDelinquencyBucketMappings(DelinquencyBucket delinquencyBucket, DelinquencyBucketData data) {
        List<Long> rangeIds = new ArrayList<>();
        data.getRanges().forEach(dataRange -> {
            rangeIds.add(dataRange.getId());
        });

        List<DelinquencyRange> ranges = repositoryRange.findAllById(rangeIds);
        validateDelinquencyRanges(ranges);
        List<DelinquencyBucketMappings> bucketMappings = repositoryBucketMappings.findByDelinquencyBucket(delinquencyBucket);
        repositoryBucketMappings.deleteAll(bucketMappings);
        bucketMappings.clear();
        for (DelinquencyRange delinquencyRange : ranges) {
            bucketMappings.add(DelinquencyBucketMappings.instance(delinquencyBucket, delinquencyRange));
        }
        repositoryBucketMappings.saveAllAndFlush(bucketMappings);
    }

    private void validateDelinquencyRanges(List<DelinquencyRange> ranges) {
        // Sort the ranges based on the minAgeDays
        ranges = sortDelinquencyRangesByMinAge(ranges);

        DelinquencyRange prevDelinquencyRange = null;
        for (DelinquencyRange delinquencyRange : ranges) {
            if (prevDelinquencyRange != null) {
                if (isOverlapped(prevDelinquencyRange, delinquencyRange)) {
                    final String errorMessage = "The delinquency ranges age days values are overlaped";
                    throw new DelinquencyBucketAgesOverlapedException(errorMessage, prevDelinquencyRange, delinquencyRange);
                }
            }
            prevDelinquencyRange = delinquencyRange;
        }
    }

    private boolean isOverlapped(DelinquencyRange o1, DelinquencyRange o2) {
        if (o2.getMaximumAgeDays() != null) { // Max Age undefined - Last one
            return Math.max(o1.getMinimumAgeDays(), o2.getMinimumAgeDays()) <= Math.min(o1.getMaximumAgeDays(), o2.getMaximumAgeDays());
        } else {
            return Math.max(o1.getMinimumAgeDays(), o2.getMinimumAgeDays()) <= Math.min(o1.getMaximumAgeDays(), o2.getMinimumAgeDays());
        }
    }

    private Map<String, Object> lookUpDelinquencyRange(final Loan loan, final DelinquencyBucket delinquencyBucket, long ageDays) {
        Map<String, Object> changes = new HashMap<>();

        if (ageDays <= 0) { // No Delinquency
            log.debug("Loan {} without delinquency range with {} days", loan.getId(), ageDays);
            changes = setLoanDelinquencyTag(loan, null);

        } else {
            // Sort the ranges based on the minAgeDays
            final List<DelinquencyRange> ranges = sortDelinquencyRangesByMinAge(delinquencyBucket.getRanges());

            for (final DelinquencyRange delinquencyRange : ranges) {
                if (delinquencyRange.getMaximumAgeDays() == null) { // Last Range in the Bucket
                    if (delinquencyRange.getMinimumAgeDays() <= ageDays) {
                        log.debug("Loan {} with delinquency range {} with {} days", loan.getId(), delinquencyRange.getClassification(),
                                ageDays);
                        changes = setLoanDelinquencyTag(loan, delinquencyRange.getId());
                        break;
                    }
                } else {
                    if (delinquencyRange.getMinimumAgeDays() <= ageDays && delinquencyRange.getMaximumAgeDays() >= ageDays) {
                        log.debug("Loan {} with delinquency range {} with {} days", loan.getId(), delinquencyRange.getClassification(),
                                ageDays);
                        changes = setLoanDelinquencyTag(loan, delinquencyRange.getId());
                        break;
                    }
                }
            }
        }
        changes.put("ageDays", ageDays);
        return changes;
    }

    public Map<String, Object> setLoanDelinquencyTag(Loan loan, Long delinquencyRangeId) {
        Map<String, Object> changes = new HashMap<>();
        List<LoanDelinquencyTagHistory> loanDelinquencyTagHistory = new ArrayList<>();
        final LocalDate transactionDate = DateUtils.getBusinessLocalDate();
        Optional<LoanDelinquencyTagHistory> optLoanDelinquencyTag = this.loanDelinquencyTagRepository.findByLoanAndLiftedOnDate(loan, null);
        // The delinquencyRangeId in null means just goes out from Delinquency
        LoanDelinquencyTagHistory loanDelinquencyTagPrev = null;
        if (delinquencyRangeId == null) {
            // The Loan will go out from Delinquency
            if (optLoanDelinquencyTag.isPresent()) {
                loanDelinquencyTagPrev = optLoanDelinquencyTag.get();
                loanDelinquencyTagPrev.setLiftedOnDate(transactionDate);
                loanDelinquencyTagHistory.add(loanDelinquencyTagPrev);
                changes.put("previous", loanDelinquencyTagPrev.getDelinquencyRange());
            }
        } else {
            if (optLoanDelinquencyTag.isPresent()) {
                loanDelinquencyTagPrev = optLoanDelinquencyTag.get();
            }
            // If the Delinquency Tag has not changed
            if (loanDelinquencyTagPrev != null && loanDelinquencyTagPrev.getDelinquencyRange().getId().equals(delinquencyRangeId)) {
                changes.put("current", loanDelinquencyTagPrev.getDelinquencyRange());
            } else {
                // The previous Loan Delinquency Tag will set as Lifted
                if (loanDelinquencyTagPrev != null) {
                    loanDelinquencyTagPrev.setLiftedOnDate(transactionDate);
                    loanDelinquencyTagHistory.add(loanDelinquencyTagPrev);
                    changes.put("previous", loanDelinquencyTagPrev.getDelinquencyRange());
                }

                final DelinquencyRange delinquencyRange = repositoryRange.getReferenceById(delinquencyRangeId);
                LoanDelinquencyTagHistory loanDelinquencyTag = new LoanDelinquencyTagHistory(delinquencyRange, loan, transactionDate, null);
                loanDelinquencyTagHistory.add(loanDelinquencyTag);
                changes.put("current", loanDelinquencyTag.getDelinquencyRange());
            }
        }
        if (loanDelinquencyTagHistory.size() > 0) {
            this.loanDelinquencyTagRepository.saveAllAndFlush(loanDelinquencyTagHistory);
        }
        return changes;
    }

    private List<DelinquencyRange> sortDelinquencyRangesByMinAge(List<DelinquencyRange> ranges) {
        final Comparator<DelinquencyRange> orderByMinAge = new Comparator<DelinquencyRange>() {

            @Override
            public int compare(DelinquencyRange o1, DelinquencyRange o2) {
                return o1.getMinimumAgeDays().compareTo(o2.getMinimumAgeDays());
            }
        };
        Collections.sort(ranges, orderByMinAge);
        return ranges;
    }

}
