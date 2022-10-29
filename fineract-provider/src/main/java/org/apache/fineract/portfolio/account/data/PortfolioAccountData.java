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
package org.apache.fineract.portfolio.account.data;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.organisation.monetary.data.CurrencyData;

/**
 * Immutable data object representing a savings account.
 */
@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class PortfolioAccountData implements Serializable {

    private final Long id;
    private final String accountNo;
    private final String externalId;
    private final Long groupId;
    private final String groupName;
    private final Long clientId;
    private final String clientName;
    private final Long productId;
    private final String productName;
    private final Long fieldOfficerId;
    private final String fieldOfficerName;
    private final CurrencyData currency;
    private final BigDecimal amtForTransfer;

    public static PortfolioAccountData lookup(final Long accountId, final String accountNo) {
        return new PortfolioAccountData(accountId, accountNo, null, null, null, null, null, null, null, null, null, null, null);
    }

    public PortfolioAccountData(final Long id, final String accountNo, final String externalId, final Long groupId, final String groupName,
            final Long clientId, final String clientName, final Long productId, final String productName, final Long fieldOfficerId,
            final String fieldOfficerName, final CurrencyData currency) {
        this.id = id;
        this.accountNo = accountNo;
        this.externalId = externalId;
        this.groupId = groupId;
        this.groupName = groupName;
        this.clientId = clientId;
        this.clientName = clientName;
        this.productId = productId;
        this.productName = productName;
        this.fieldOfficerId = fieldOfficerId;
        this.fieldOfficerName = fieldOfficerName;
        this.currency = currency;
        this.amtForTransfer = null;
    }

    public String getCurrencyCode() {
        return this.currency.getCode();
    }
}
