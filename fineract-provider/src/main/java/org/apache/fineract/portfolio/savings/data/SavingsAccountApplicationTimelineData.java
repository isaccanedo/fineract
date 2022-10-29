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
package org.apache.fineract.portfolio.savings.data;

import java.io.Serializable;
import java.time.LocalDate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Immutable data object represent the important time-line events of a savings account application.
 */
@Getter
@RequiredArgsConstructor
public class SavingsAccountApplicationTimelineData implements Serializable {

    private final LocalDate submittedOnDate;
    private final String submittedByUsername;
    private final String submittedByFirstname;
    private final String submittedByLastname;
    private final LocalDate rejectedOnDate;
    private final String rejectedByUsername;
    private final String rejectedByFirstname;
    private final String rejectedByLastname;
    private final LocalDate withdrawnOnDate;
    private final String withdrawnByUsername;
    private final String withdrawnByFirstname;
    private final String withdrawnByLastname;
    private final LocalDate approvedOnDate;
    private final String approvedByUsername;
    private final String approvedByFirstname;
    private final String approvedByLastname;
    private final LocalDate activatedOnDate;
    private final String activatedByUsername;
    private final String activatedByFirstname;
    private final String activatedByLastname;
    private final LocalDate closedOnDate;
    private final String closedByUsername;
    private final String closedByFirstname;
    private final String closedByLastname;

    public static SavingsAccountApplicationTimelineData templateDefault() {

        final LocalDate submittedOnDate = null;
        final String submittedByUsername = null;
        final String submittedByFirstname = null;
        final String submittedByLastname = null;
        final LocalDate rejectedOnDate = null;
        final String rejectedByUsername = null;
        final String rejectedByFirstname = null;
        final String rejectedByLastname = null;
        final LocalDate withdrawnOnDate = null;
        final String withdrawnByUsername = null;
        final String withdrawnByFirstname = null;
        final String withdrawnByLastname = null;
        final LocalDate approvedOnDate = null;
        final String approvedByUsername = null;
        final String approvedByFirstname = null;
        final String approvedByLastname = null;
        final LocalDate activatedOnDate = null;
        final String activatedByUsername = null;
        final String activatedByFirstname = null;
        final String activatedByLastname = null;
        final LocalDate closedOnDate = null;
        final String closedByUsername = null;
        final String closedByFirstname = null;
        final String closedByLastname = null;

        return new SavingsAccountApplicationTimelineData(submittedOnDate, submittedByUsername, submittedByFirstname, submittedByLastname,
                rejectedOnDate, rejectedByUsername, rejectedByFirstname, rejectedByLastname, withdrawnOnDate, withdrawnByUsername,
                withdrawnByFirstname, withdrawnByLastname, approvedOnDate, approvedByUsername, approvedByFirstname, approvedByLastname,
                activatedOnDate, activatedByUsername, activatedByFirstname, activatedByLastname, closedOnDate, closedByUsername,
                closedByFirstname, closedByLastname);
    }
}
