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
package org.apache.fineract.portfolio.client.data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.dataqueries.data.DatatableData;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.organisation.staff.data.StaffData;
import org.apache.fineract.portfolio.address.data.AddressData;
import org.apache.fineract.portfolio.collateralmanagement.domain.ClientCollateralManagement;
import org.apache.fineract.portfolio.group.data.GroupGeneralData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountData;
import org.apache.fineract.portfolio.savings.data.SavingsProductData;

/**
 * Immutable data object representing client data.
 */
@Getter
public final class ClientData implements Comparable<ClientData>, Serializable {

    private final Long id;
    private final String accountNo;
    private final String externalId;

    private final EnumOptionData status;
    private final CodeValueData subStatus;

    private final Boolean active;
    private final LocalDate activationDate;

    private final String firstname;
    private final String middlename;
    private final String lastname;
    private final String fullname;
    private final String displayName;
    private final String mobileNo;
    private final String emailAddress;
    private final LocalDate dateOfBirth;
    private final CodeValueData gender;
    private final CodeValueData clientType;
    private final CodeValueData clientClassification;
    private final Boolean isStaff;

    private final Long officeId;
    private final String officeName;
    private final Long transferToOfficeId;
    private final String transferToOfficeName;

    private final Long imageId;
    private final Boolean imagePresent;
    private final Long staffId;
    private final String staffName;
    private final ClientTimelineData timeline;

    private final Long savingsProductId;
    private final String savingsProductName;

    private final Long savingsAccountId;
    private final EnumOptionData legalForm;
    private final Set<ClientCollateralManagementData> clientCollateralManagements;

    // associations
    private final Collection<GroupGeneralData> groups;

    // template
    private final Collection<OfficeData> officeOptions;
    private final Collection<StaffData> staffOptions;
    private final Collection<CodeValueData> narrations;
    private final Collection<SavingsProductData> savingProductOptions;
    private final Collection<SavingsAccountData> savingAccountOptions;
    private final Collection<CodeValueData> genderOptions;
    private final Collection<CodeValueData> clientTypeOptions;
    private final Collection<CodeValueData> clientClassificationOptions;
    private final Collection<CodeValueData> clientNonPersonConstitutionOptions;
    private final Collection<CodeValueData> clientNonPersonMainBusinessLineOptions;
    private final List<EnumOptionData> clientLegalFormOptions;
    private final ClientFamilyMembersData familyMemberOptions;

    private final ClientNonPersonData clientNonPersonDetails;

    private final Collection<AddressData> address;

    private final Boolean isAddressEnabled;

    private final List<DatatableData> datatables;

    // import fields
    private transient Integer rowIndex;
    private String dateFormat;
    private String locale;
    private Long clientTypeId;
    private Long genderId;
    private Long clientClassificationId;
    private Long legalFormId;
    private LocalDate submittedOnDate;

    public static ClientData importClientEntityInstance(Long legalFormId, Integer rowIndex, String fullname, Long officeId,
            Long clientTypeId, Long clientClassificationId, Long staffId, Boolean active, LocalDate activationDate,
            LocalDate submittedOnDate, String externalId, LocalDate dateOfBirth, String mobileNo,
            ClientNonPersonData clientNonPersonDetails, Collection<AddressData> address, String locale, String dateFormat) {
        return new ClientData(legalFormId, rowIndex, fullname, null, null, null, submittedOnDate, activationDate, active, externalId,
                officeId, staffId, mobileNo, dateOfBirth, clientTypeId, null, clientClassificationId, null, address, clientNonPersonDetails,
                locale, dateFormat);
    }

    public static ClientData createClientForInterestPosting(final Long id, final Long officeId) {
        return new ClientData(id, officeId);
    }

    private ClientData(final Long clientId, final Long officeId) {
        this.rowIndex = null;
        this.dateFormat = null;
        this.locale = null;
        this.firstname = null;
        this.lastname = null;
        this.middlename = null;
        this.fullname = null;
        this.activationDate = null;
        this.submittedOnDate = null;
        this.active = null;
        this.externalId = null;
        this.officeId = officeId;
        this.staffId = null;
        this.legalFormId = null;
        this.mobileNo = null;
        this.dateOfBirth = null;
        this.clientTypeId = null;
        this.genderId = null;
        this.clientClassificationId = null;
        this.isStaff = false;
        this.address = null;
        this.accountNo = null;
        this.status = null;
        this.subStatus = null;
        this.displayName = null;
        this.gender = null;
        this.clientType = null;
        this.clientClassification = null;
        this.officeName = null;
        this.transferToOfficeId = null;
        this.transferToOfficeName = null;
        this.imageId = null;
        this.imagePresent = null;
        this.staffName = null;
        this.timeline = null;
        this.savingsProductId = null;
        this.savingsProductName = null;
        this.savingsAccountId = null;
        this.legalForm = null;
        this.groups = null;
        this.officeOptions = null;
        this.staffOptions = null;
        this.narrations = null;
        this.savingProductOptions = null;
        this.savingAccountOptions = null;
        this.genderOptions = null;
        this.clientTypeOptions = null;
        this.clientClassificationOptions = null;
        this.clientNonPersonConstitutionOptions = null;
        this.clientNonPersonMainBusinessLineOptions = null;
        this.clientLegalFormOptions = null;
        this.clientNonPersonDetails = null;
        this.isAddressEnabled = null;
        this.datatables = null;
        this.familyMemberOptions = null;
        this.emailAddress = null;
        this.clientCollateralManagements = null;
        this.id = clientId;
    }

    public static ClientData importClientPersonInstance(Long legalFormId, Integer rowIndex, String firstname, String lastname,
            String middlename, LocalDate submittedOn, LocalDate activationDate, Boolean active, String externalId, Long officeId,
            Long staffId, String mobileNo, LocalDate dob, Long clientTypeId, Long genderId, Long clientClassificationId, Boolean isStaff,
            Collection<AddressData> address, String locale, String dateFormat) {

        return new ClientData(legalFormId, rowIndex, null, firstname, lastname, middlename, submittedOn, activationDate, active, externalId,
                officeId, staffId, mobileNo, dob, clientTypeId, genderId, clientClassificationId, isStaff, address, null, locale,
                dateFormat);
    }

    public static ClientData emptyInstance(Long clientId) {
        return lookup(clientId, null, null, null);
    }

    private ClientData(Long legalFormId, Integer rowIndex, String fullname, String firstname, String lastname, String middlename,
            LocalDate submittedOn, LocalDate activationDate, Boolean active, String externalId, Long officeId, Long staffId,
            String mobileNo, LocalDate dob, Long clientTypeId, Long genderId, Long clientClassificationId, Boolean isStaff,
            Collection<AddressData> address, ClientNonPersonData clientNonPersonDetails, String locale, String dateFormat) {
        this.rowIndex = rowIndex;
        this.dateFormat = dateFormat;
        this.locale = locale;
        this.firstname = firstname;
        this.lastname = lastname;
        this.middlename = middlename;
        this.fullname = fullname;
        this.activationDate = activationDate;
        this.submittedOnDate = submittedOn;
        this.active = active;
        this.externalId = externalId;
        this.officeId = officeId;
        this.staffId = staffId;
        this.legalFormId = legalFormId;
        this.mobileNo = mobileNo;
        this.dateOfBirth = dob;
        this.clientTypeId = clientTypeId;
        this.genderId = genderId;
        this.clientClassificationId = clientClassificationId;
        this.isStaff = isStaff;
        this.address = address;
        this.id = null;
        this.accountNo = null;
        this.status = null;
        this.subStatus = null;
        this.displayName = null;
        this.gender = null;
        this.clientType = null;
        this.clientClassification = null;
        this.officeName = null;
        this.transferToOfficeId = null;
        this.transferToOfficeName = null;
        this.imageId = null;
        this.imagePresent = null;
        this.staffName = null;
        this.timeline = null;
        this.savingsProductId = null;
        this.savingsProductName = null;
        this.savingsAccountId = null;
        this.legalForm = null;
        this.groups = null;
        this.officeOptions = null;
        this.staffOptions = null;
        this.narrations = null;
        this.savingProductOptions = null;
        this.savingAccountOptions = null;
        this.genderOptions = null;
        this.clientTypeOptions = null;
        this.clientClassificationOptions = null;
        this.clientNonPersonConstitutionOptions = null;
        this.clientNonPersonMainBusinessLineOptions = null;
        this.clientLegalFormOptions = null;
        this.clientNonPersonDetails = null;
        this.isAddressEnabled = null;
        this.datatables = null;
        this.familyMemberOptions = null;
        this.emailAddress = null;
        this.clientCollateralManagements = null;
    }

    public static ClientData template(final Long officeId, final LocalDate joinedDate, final Collection<OfficeData> officeOptions,
            final Collection<StaffData> staffOptions, final Collection<CodeValueData> narrations,
            final Collection<CodeValueData> genderOptions, final Collection<SavingsProductData> savingProductOptions,
            final Collection<CodeValueData> clientTypeOptions, final Collection<CodeValueData> clientClassificationOptions,
            final Collection<CodeValueData> clientNonPersonConstitutionOptions,
            final Collection<CodeValueData> clientNonPersonMainBusinessLineOptions, final List<EnumOptionData> clientLegalFormOptions,
            final ClientFamilyMembersData familyMemberOptions, final Collection<AddressData> address, final Boolean isAddressEnabled,
            final List<DatatableData> datatables) {
        final String accountNo = null;
        final EnumOptionData status = null;
        final CodeValueData subStatus = null;
        final String officeName = null;
        final Long transferToOfficeId = null;
        final String transferToOfficeName = null;
        final Long id = null;
        final String firstname = null;
        final String middlename = null;
        final String lastname = null;
        final String fullname = null;
        final String displayName = null;
        final String externalId = null;
        final String mobileNo = null;
        final String emailAddress = null;
        final LocalDate dateOfBirth = null;
        final CodeValueData gender = null;
        final Long imageId = null;
        final Long staffId = null;
        final String staffName = null;
        final Collection<GroupGeneralData> groups = null;
        final ClientTimelineData timeline = null;
        final Long savingsProductId = null;
        final String savingsProductName = null;
        final Long savingsAccountId = null;
        final Collection<SavingsAccountData> savingAccountOptions = null;
        final CodeValueData clientType = null;
        final CodeValueData clientClassification = null;
        final EnumOptionData legalForm = null;
        final Boolean isStaff = false;
        final ClientNonPersonData clientNonPersonDetails = null;
        final Set<ClientCollateralManagementData> clientCollateralManagements = null;
        return new ClientData(accountNo, status, subStatus, officeId, officeName, transferToOfficeId, transferToOfficeName, id, firstname,
                middlename, lastname, fullname, displayName, externalId, mobileNo, emailAddress, dateOfBirth, gender, joinedDate, imageId,
                staffId, staffName, officeOptions, groups, staffOptions, narrations, genderOptions, timeline, savingProductOptions,
                savingsProductId, savingsProductName, savingsAccountId, savingAccountOptions, clientType, clientClassification,
                clientTypeOptions, clientClassificationOptions, clientNonPersonConstitutionOptions, clientNonPersonMainBusinessLineOptions,
                clientNonPersonDetails, clientLegalFormOptions, familyMemberOptions, legalForm, address, isAddressEnabled, datatables,
                isStaff, clientCollateralManagements);

    }

    public static ClientData templateOnTop(final ClientData clientData, final ClientData templateData) {
        final Set<ClientCollateralManagementData> clientCollateralManagements = null;
        return new ClientData(clientData.accountNo, clientData.status, clientData.subStatus, clientData.officeId, clientData.officeName,
                clientData.transferToOfficeId, clientData.transferToOfficeName, clientData.id, clientData.firstname, clientData.middlename,
                clientData.lastname, clientData.fullname, clientData.displayName, clientData.externalId, clientData.mobileNo,
                clientData.emailAddress, clientData.dateOfBirth, clientData.gender, clientData.activationDate, clientData.imageId,
                clientData.staffId, clientData.staffName, templateData.officeOptions, clientData.groups, templateData.staffOptions,
                templateData.narrations, templateData.genderOptions, clientData.timeline, templateData.savingProductOptions,
                clientData.savingsProductId, clientData.savingsProductName, clientData.savingsAccountId, clientData.savingAccountOptions,
                clientData.clientType, clientData.clientClassification, templateData.clientTypeOptions,
                templateData.clientClassificationOptions, templateData.clientNonPersonConstitutionOptions,
                templateData.clientNonPersonMainBusinessLineOptions, clientData.clientNonPersonDetails, templateData.clientLegalFormOptions,
                templateData.familyMemberOptions, clientData.legalForm, clientData.address, clientData.isAddressEnabled, null,
                clientData.isStaff, clientCollateralManagements);

    }

    public static ClientData templateWithSavingAccountOptions(final ClientData clientData,
            final Collection<SavingsAccountData> savingAccountOptions) {
        final Set<ClientCollateralManagementData> clientCollateralManagements = null;
        return new ClientData(clientData.accountNo, clientData.status, clientData.subStatus, clientData.officeId, clientData.officeName,
                clientData.transferToOfficeId, clientData.transferToOfficeName, clientData.id, clientData.firstname, clientData.middlename,
                clientData.lastname, clientData.fullname, clientData.displayName, clientData.externalId, clientData.mobileNo,
                clientData.emailAddress, clientData.dateOfBirth, clientData.gender, clientData.activationDate, clientData.imageId,
                clientData.staffId, clientData.staffName, clientData.officeOptions, clientData.groups, clientData.staffOptions,
                clientData.narrations, clientData.genderOptions, clientData.timeline, clientData.savingProductOptions,
                clientData.savingsProductId, clientData.savingsProductName, clientData.savingsAccountId, savingAccountOptions,
                clientData.clientType, clientData.clientClassification, clientData.clientTypeOptions,
                clientData.clientClassificationOptions, clientData.clientNonPersonConstitutionOptions,
                clientData.clientNonPersonMainBusinessLineOptions, clientData.clientNonPersonDetails, clientData.clientLegalFormOptions,
                clientData.familyMemberOptions, clientData.legalForm, clientData.address, clientData.isAddressEnabled, null,
                clientData.isStaff, clientCollateralManagements);

    }

    public static ClientData setParentGroups(final ClientData clientData, final Collection<GroupGeneralData> parentGroups,
            final Set<ClientCollateralManagementData> clientCollateralManagements) {
        return new ClientData(clientData.accountNo, clientData.status, clientData.subStatus, clientData.officeId, clientData.officeName,
                clientData.transferToOfficeId, clientData.transferToOfficeName, clientData.id, clientData.firstname, clientData.middlename,
                clientData.lastname, clientData.fullname, clientData.displayName, clientData.externalId, clientData.mobileNo,
                clientData.emailAddress, clientData.dateOfBirth, clientData.gender, clientData.activationDate, clientData.imageId,
                clientData.staffId, clientData.staffName, clientData.officeOptions, parentGroups, clientData.staffOptions, null, null,
                clientData.timeline, clientData.savingProductOptions, clientData.savingsProductId, clientData.savingsProductName,
                clientData.savingsAccountId, clientData.savingAccountOptions, clientData.clientType, clientData.clientClassification,
                clientData.clientTypeOptions, clientData.clientClassificationOptions, clientData.clientNonPersonConstitutionOptions,
                clientData.clientNonPersonMainBusinessLineOptions, clientData.clientNonPersonDetails, clientData.clientLegalFormOptions,
                clientData.familyMemberOptions, clientData.legalForm, clientData.address, clientData.isAddressEnabled, null,
                clientData.isStaff, clientCollateralManagements);

    }

    public static ClientData clientIdentifier(final Long id, final String accountNo, final String firstname, final String middlename,
            final String lastname, final String fullname, final String displayName, final Long officeId, final String officeName) {

        final Long transferToOfficeId = null;
        final String transferToOfficeName = null;
        final String externalId = null;
        final String mobileNo = null;
        final String emailAddress = null;
        final LocalDate dateOfBirth = null;
        final CodeValueData gender = null;
        final LocalDate activationDate = null;
        final Long imageId = null;
        final Long staffId = null;
        final String staffName = null;
        final Collection<OfficeData> allowedOffices = null;
        final Collection<GroupGeneralData> groups = null;
        final Collection<StaffData> staffOptions = null;
        final Collection<CodeValueData> closureReasons = null;
        final Collection<CodeValueData> genderOptions = null;
        final ClientTimelineData timeline = null;
        final Collection<SavingsProductData> savingProductOptions = null;
        final Long savingsProductId = null;
        final String savingsProductName = null;
        final Long savingsAccountId = null;
        final Collection<SavingsAccountData> savingAccountOptions = null;
        final CodeValueData clientType = null;
        final CodeValueData clientClassification = null;
        final Collection<CodeValueData> clientTypeOptions = null;
        final Collection<CodeValueData> clientClassificationOptions = null;
        final Collection<CodeValueData> clientNonPersonConstitutionOptions = null;
        final Collection<CodeValueData> clientNonPersonMainBusinessLineOptions = null;
        final List<EnumOptionData> clientLegalFormOptions = null;
        final ClientFamilyMembersData familyMemberOptions = null;
        final EnumOptionData status = null;
        final CodeValueData subStatus = null;
        final EnumOptionData legalForm = null;
        final Boolean isStaff = false;
        final ClientNonPersonData clientNonPerson = null;
        final Set<ClientCollateralManagementData> clientCollateralManagements = null;
        return new ClientData(accountNo, status, subStatus, officeId, officeName, transferToOfficeId, transferToOfficeName, id, firstname,
                middlename, lastname, fullname, displayName, externalId, mobileNo, emailAddress, dateOfBirth, gender, activationDate,
                imageId, staffId, staffName, allowedOffices, groups, staffOptions, closureReasons, genderOptions, timeline,
                savingProductOptions, savingsProductId, savingsProductName, savingsAccountId, savingAccountOptions, clientType,
                clientClassification, clientTypeOptions, clientClassificationOptions, clientNonPersonConstitutionOptions,
                clientNonPersonMainBusinessLineOptions, clientNonPerson, clientLegalFormOptions, familyMemberOptions, legalForm, null, null,
                null, isStaff, clientCollateralManagements);
    }

    public static ClientData lookup(final Long id, final String displayName, final Long officeId, final String officeName) {
        final String accountNo = null;
        final EnumOptionData status = null;
        final CodeValueData subStatus = null;
        final Long transferToOfficeId = null;
        final String transferToOfficeName = null;
        final String firstname = null;
        final String middlename = null;
        final String lastname = null;
        final String fullname = null;
        final String externalId = null;
        final String mobileNo = null;
        final String emailAddress = null;
        final LocalDate dateOfBirth = null;
        final CodeValueData gender = null;
        final LocalDate activationDate = null;
        final Long imageId = null;
        final Long staffId = null;
        final String staffName = null;
        final Collection<OfficeData> allowedOffices = null;
        final Collection<GroupGeneralData> groups = null;
        final Collection<StaffData> staffOptions = null;
        final Collection<CodeValueData> closureReasons = null;
        final Collection<CodeValueData> genderOptions = null;
        final ClientTimelineData timeline = null;
        final Collection<SavingsProductData> savingProductOptions = null;
        final Long savingsProductId = null;
        final String savingsProductName = null;
        final Long savingsAccountId = null;
        final Collection<SavingsAccountData> savingAccountOptions = null;
        final CodeValueData clientType = null;
        final CodeValueData clientClassification = null;
        final Collection<CodeValueData> clientTypeOptions = null;
        final Collection<CodeValueData> clientClassificationOptions = null;
        final Collection<CodeValueData> clientNonPersonConstitutionOptions = null;
        final Collection<CodeValueData> clientNonPersonMainBusinessLineOptions = null;
        final List<EnumOptionData> clientLegalFormOptions = null;
        final ClientFamilyMembersData familyMemberOptions = null;
        final EnumOptionData legalForm = null;
        final Boolean isStaff = false;
        final ClientNonPersonData clientNonPerson = null;
        final Set<ClientCollateralManagementData> clientCollateralManagements = null;
        return new ClientData(accountNo, status, subStatus, officeId, officeName, transferToOfficeId, transferToOfficeName, id, firstname,
                middlename, lastname, fullname, displayName, externalId, mobileNo, emailAddress, dateOfBirth, gender, activationDate,
                imageId, staffId, staffName, allowedOffices, groups, staffOptions, closureReasons, genderOptions, timeline,
                savingProductOptions, savingsProductId, savingsProductName, savingsAccountId, savingAccountOptions, clientType,
                clientClassification, clientTypeOptions, clientClassificationOptions, clientNonPersonConstitutionOptions,
                clientNonPersonMainBusinessLineOptions, clientNonPerson, clientLegalFormOptions, familyMemberOptions, legalForm, null, null,
                null, isStaff, clientCollateralManagements);

    }

    public static ClientData instance(final Long id, final String displayName) {
        final Long officeId = null;
        final String officeName = null;
        return lookup(id, displayName, officeId, officeName);
    }

    public static ClientData instance(final String accountNo, final EnumOptionData status, final CodeValueData subStatus,
            final Long officeId, final String officeName, final Long transferToOfficeId, final String transferToOfficeName, final Long id,
            final String firstname, final String middlename, final String lastname, final String fullname, final String displayName,
            final String externalId, final String mobileNo, final String emailAddress, final LocalDate dateOfBirth,
            final CodeValueData gender, final LocalDate activationDate, final Long imageId, final Long staffId, final String staffName,
            final ClientTimelineData timeline, final Long savingsProductId, final String savingsProductName, final Long savingsAccountId,
            final CodeValueData clientType, final CodeValueData clientClassification, final EnumOptionData legalForm,
            final ClientNonPersonData clientNonPerson, final Boolean isStaff) {

        final Collection<OfficeData> allowedOffices = null;
        final Collection<GroupGeneralData> groups = null;
        final Collection<StaffData> staffOptions = null;
        final Collection<CodeValueData> closureReasons = null;
        final Collection<CodeValueData> genderOptions = null;
        final Collection<SavingsProductData> savingProductOptions = null;
        final Collection<CodeValueData> clientTypeOptions = null;
        final Collection<CodeValueData> clientClassificationOptions = null;
        final Collection<CodeValueData> clientNonPersonConstitutionOptions = null;
        final Collection<CodeValueData> clientNonPersonMainBusinessLineOptions = null;
        final List<EnumOptionData> clientLegalFormOptions = null;
        final ClientFamilyMembersData familyMemberOptions = null;
        final Collection<ClientCollateralManagement> clientCollateralManagements = null;
        return new ClientData(accountNo, status, subStatus, officeId, officeName, transferToOfficeId, transferToOfficeName, id, firstname,
                middlename, lastname, fullname, displayName, externalId, mobileNo, emailAddress, dateOfBirth, gender, activationDate,
                imageId, staffId, staffName, allowedOffices, groups, staffOptions, closureReasons, genderOptions, timeline,
                savingProductOptions, savingsProductId, savingsProductName, savingsAccountId, null, clientType, clientClassification,
                clientTypeOptions, clientClassificationOptions, clientNonPersonConstitutionOptions, clientNonPersonMainBusinessLineOptions,
                clientNonPerson, clientLegalFormOptions, familyMemberOptions, legalForm, null, null, null, isStaff, null);

    }

    private ClientData(final String accountNo, final EnumOptionData status, final CodeValueData subStatus, final Long officeId,
            final String officeName, final Long transferToOfficeId, final String transferToOfficeName, final Long id,
            final String firstname, final String middlename, final String lastname, final String fullname, final String displayName,
            final String externalId, final String mobileNo, final String emailAddress, final LocalDate dateOfBirth,
            final CodeValueData gender, final LocalDate activationDate, final Long imageId, final Long staffId, final String staffName,
            final Collection<OfficeData> allowedOffices, final Collection<GroupGeneralData> groups,
            final Collection<StaffData> staffOptions, final Collection<CodeValueData> narrations,
            final Collection<CodeValueData> genderOptions, final ClientTimelineData timeline,
            final Collection<SavingsProductData> savingProductOptions, final Long savingsProductId, final String savingsProductName,
            final Long savingsAccountId, final Collection<SavingsAccountData> savingAccountOptions, final CodeValueData clientType,
            final CodeValueData clientClassification, final Collection<CodeValueData> clientTypeOptions,
            final Collection<CodeValueData> clientClassificationOptions, final Collection<CodeValueData> clientNonPersonConstitutionOptions,
            final Collection<CodeValueData> clientNonPersonMainBusinessLineOptions, final ClientNonPersonData clientNonPerson,
            final List<EnumOptionData> clientLegalFormOptions, final ClientFamilyMembersData familyMemberOptions,
            final EnumOptionData legalForm, final Collection<AddressData> address, final Boolean isAddressEnabled,
            final List<DatatableData> datatables, final Boolean isStaff,
            final Set<ClientCollateralManagementData> clientCollateralManagements) {
        this.accountNo = accountNo;
        this.status = status;
        if (status != null) {
            this.active = status.getId().equals(300L);
        } else {
            this.active = null;
        }
        this.subStatus = subStatus;
        this.officeId = officeId;
        this.officeName = officeName;
        this.transferToOfficeId = transferToOfficeId;
        this.transferToOfficeName = transferToOfficeName;
        this.id = id;
        this.firstname = StringUtils.defaultIfEmpty(firstname, null);
        this.middlename = StringUtils.defaultIfEmpty(middlename, null);
        this.lastname = StringUtils.defaultIfEmpty(lastname, null);
        this.fullname = StringUtils.defaultIfEmpty(fullname, null);
        this.displayName = StringUtils.defaultIfEmpty(displayName, null);
        this.externalId = StringUtils.defaultIfEmpty(externalId, null);
        this.mobileNo = StringUtils.defaultIfEmpty(mobileNo, null);
        this.emailAddress = StringUtils.defaultIfEmpty(emailAddress, null);
        this.activationDate = activationDate;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.clientClassification = clientClassification;
        this.clientType = clientType;
        this.imageId = imageId;
        if (imageId != null) {
            this.imagePresent = Boolean.TRUE;
        } else {
            this.imagePresent = null;
        }
        this.staffId = staffId;
        this.staffName = staffName;

        // associations
        this.groups = groups;

        // template
        this.officeOptions = allowedOffices;
        this.staffOptions = staffOptions;
        this.narrations = narrations;

        this.genderOptions = genderOptions;
        this.clientClassificationOptions = clientClassificationOptions;
        this.clientTypeOptions = clientTypeOptions;

        this.clientNonPersonConstitutionOptions = clientNonPersonConstitutionOptions;
        this.clientNonPersonMainBusinessLineOptions = clientNonPersonMainBusinessLineOptions;
        this.clientLegalFormOptions = clientLegalFormOptions;
        this.familyMemberOptions = familyMemberOptions;

        this.timeline = timeline;
        this.savingProductOptions = savingProductOptions;
        this.savingsProductId = savingsProductId;
        this.savingsProductName = savingsProductName;
        this.savingsAccountId = savingsAccountId;
        this.savingAccountOptions = savingAccountOptions;
        this.legalForm = legalForm;
        this.isStaff = isStaff;
        this.clientNonPersonDetails = clientNonPerson;

        this.address = address;
        this.isAddressEnabled = isAddressEnabled;
        this.datatables = datatables;
        this.clientCollateralManagements = clientCollateralManagements;
    }

    @Override
    public int compareTo(final ClientData obj) {
        if (obj == null) {
            return -1;
        }
        return new CompareToBuilder() //
                .append(this.id, obj.id) //
                .append(this.displayName, obj.displayName) //
                .toComparison();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final ClientData rhs = (ClientData) obj;
        return new EqualsBuilder() //
                .append(this.id, rhs.id) //
                .append(this.displayName, rhs.displayName) //
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37) //
                .append(this.id) //
                .append(this.displayName) //
                .toHashCode();
    }
}
