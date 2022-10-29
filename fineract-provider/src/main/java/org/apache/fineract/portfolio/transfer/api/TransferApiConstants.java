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
package org.apache.fineract.portfolio.transfer.api;

public final class TransferApiConstants {

    private TransferApiConstants() {

    }

    // general
    public static final String localeParamName = "locale";
    public static final String dateFormatParamName = "dateFormat";

    // request parameters
    public static final String idParamName = "id";
    public static final String destinationGroupIdParamName = "destinationGroupId";
    public static final String clients = "clients";
    public static final String inheritDestinationGroupLoanOfficer = "inheritDestinationGroupLoanOfficer";
    public static final String newStaffIdParamName = "staffId";
    public static final String transferActiveLoans = "transferActiveLoans";
    public static final String destinationOfficeIdParamName = "destinationOfficeId";
    public static final String note = "note";
    public static final String transferDate = "transferDate";
    public static final String transferClientLoanException = "error.msg.cannot.transfer.client.as.loan.transaction.present.on.or.after.transfer.date";
    public static final String transferClientLoanExceptionMessage = "error msg cannot transfer client as loan transaction present on or after transfer date";
    public static final String transferClientSavingsException = "error.msg.cannot.transfer.client.as.savings.transaction.present.on.or.after.transfer.date";
    public static final String transferClientSavingsExceptionMessage = "error msg cannot transfer client as savings transaction present on or after transfer date";
    public static final String transferClientToSameOfficeException = "error.msg.cannot.transfer.client.as.selected.office.and.current.office.are.same";
    public static final String transferClientToSameOfficeExceptionMessage = "error.msg.cannot.transfer.client.as.selected.office.and.current.office.are.same";
}
