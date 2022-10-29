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
package org.apache.fineract.accounting.rule.api;

import java.util.HashSet;
import java.util.Set;

/***
 * Enum of all parameters passed in while creating/updating a loan product
 ***/
public enum AccountingRuleJsonInputParams {

    ID("id"), OFFICE_ID("officeId"), ACCOUNT_TO_DEBIT("accountToDebit"), ACCOUNT_TO_CREDIT("accountToCredit"), NAME("name"), DESCRIPTION(
            "description"), SYSTEM_DEFINED("systemDefined"), DEBIT_ACCOUNT_TAGS("debitTags"), CREDIT_ACCOUNT_TAGS(
                    "creditTags"), ALLOW_MULTIPLE_CREDIT_ENTRIES(
                            "allowMultipleCreditEntries"), ALLOW_MULTIPLE_DEBIT_ENTRIES("allowMultipleDebitEntries");

    private final String value;

    AccountingRuleJsonInputParams(final String value) {
        this.value = value;
    }

    private static final Set<String> values = new HashSet<>();

    static {
        for (final AccountingRuleJsonInputParams type : AccountingRuleJsonInputParams.values()) {
            values.add(type.value);
        }
    }

    public static Set<String> getAllValues() {
        return values;
    }

    @Override
    public String toString() {
        return name().toString().replace("_", " ");
    }

    public String getValue() {
        return this.value;
    }
}
