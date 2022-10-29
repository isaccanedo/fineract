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
package org.apache.fineract.cob.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.cob.domain.LoanAccountLock;
import org.apache.fineract.cob.domain.LoanAccountLockRepository;
import org.apache.fineract.cob.domain.LockOwner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoanAccountLockServiceImpl implements LoanAccountLockService {

    private final LoanAccountLockRepository loanAccountLockRepository;

    @Override
    public List<LoanAccountLock> getLockedLoanAccountByPage(int page, int limit) {
        Pageable loanAccountLockPage = PageRequest.of(page, limit);
        Page<LoanAccountLock> loanAccountLocks = loanAccountLockRepository.findAll(loanAccountLockPage);
        return loanAccountLocks.getContent();
    }

    @Override
    public boolean isLoanHardLocked(Long loanId) {
        Optional<LoanAccountLock> loanAccountLockOptional = loanAccountLockRepository.findById(loanId);
        return loanAccountLockOptional //
                .filter(loanAccountLock -> LockOwner.LOAN_COB_CHUNK_PROCESSING.equals(loanAccountLock.getLockOwner()) //
                        || LockOwner.LOAN_INLINE_COB_PROCESSING.equals(loanAccountLock.getLockOwner())) //
                .filter(loanAccountLock -> StringUtils.isBlank(loanAccountLock.getError())).isPresent();
    }
}
