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
package org.apache.fineract.infrastructure.bulkimport.importhandler.client;

import com.google.common.base.Splitter;
import com.google.gson.GsonBuilder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.bulkimport.constants.ClientPersonConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.data.Count;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandler;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandlerUtils;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.DateSerializer;
import org.apache.fineract.infrastructure.core.serialization.GoogleGsonSerializerHelper;
import org.apache.fineract.portfolio.address.data.AddressData;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClientPersonImportHandler implements ImportHandler {

    public static final String SEPARATOR = "-";
    private static final Logger LOG = LoggerFactory.getLogger(ClientPersonImportHandler.class);
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @Autowired
    public ClientPersonImportHandler(final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
    }

    @Override
    public Count process(final Workbook workbook, final String locale, final String dateFormat) {

        List<ClientData> clients = readExcelFile(workbook, locale, dateFormat);
        return importEntity(workbook, clients, dateFormat);
    }

    public List<ClientData> readExcelFile(final Workbook workbook, final String locale, final String dateFormat) {
        List<ClientData> clients = new ArrayList<>();
        Sheet clientSheet = workbook.getSheet(TemplatePopulateImportConstants.CLIENT_PERSON_SHEET_NAME);
        Integer noOfEntries = ImportHandlerUtils.getNumberOfRows(clientSheet, 0);
        for (int rowIndex = 1; rowIndex <= noOfEntries; rowIndex++) {
            Row row;
            row = clientSheet.getRow(rowIndex);
            if (ImportHandlerUtils.isNotImported(row, ClientPersonConstants.STATUS_COL)) {
                clients.add(readClient(workbook, row, locale, dateFormat));
            }
        }
        return clients;
    }

    private ClientData readClient(final Workbook workbook, final Row row, final String locale, final String dateFormat) {
        Long legalFormId = 1L;
        String firstName = ImportHandlerUtils.readAsString(ClientPersonConstants.FIRST_NAME_COL, row);
        String lastName = ImportHandlerUtils.readAsString(ClientPersonConstants.LAST_NAME_COL, row);
        String middleName = ImportHandlerUtils.readAsString(ClientPersonConstants.MIDDLE_NAME_COL, row);
        String officeName = ImportHandlerUtils.readAsString(ClientPersonConstants.OFFICE_NAME_COL, row);
        Long officeId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.OFFICE_SHEET_NAME), officeName);
        if (officeId == 0L) {
            officeId = null;
        }
        String staffName = ImportHandlerUtils.readAsString(ClientPersonConstants.STAFF_NAME_COL, row);
        Long staffId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.STAFF_SHEET_NAME), staffName);
        if (staffId == 0L) {
            staffId = null;
        }
        String externalId = ImportHandlerUtils.readAsString(ClientPersonConstants.EXTERNAL_ID_COL, row);
        LocalDate submittedOn = ImportHandlerUtils.readAsDate(ClientPersonConstants.SUBMITTED_ON_COL, row);
        LocalDate activationDate = ImportHandlerUtils.readAsDate(ClientPersonConstants.ACTIVATION_DATE_COL, row);
        Boolean active = ImportHandlerUtils.readAsBoolean(ClientPersonConstants.ACTIVE_COL, row);
        if (!active) {
            activationDate = submittedOn;
        }
        String mobileNo = null;
        if (ImportHandlerUtils.readAsLong(ClientPersonConstants.MOBILE_NO_COL, row) != null) {
            mobileNo = Objects.requireNonNull(ImportHandlerUtils.readAsLong(ClientPersonConstants.MOBILE_NO_COL, row)).toString();
        }
        LocalDate dob = ImportHandlerUtils.readAsDate(ClientPersonConstants.DOB_COL, row);

        String clientType = ImportHandlerUtils.readAsString(ClientPersonConstants.CLIENT_TYPE_COL, row);
        Long clientTypeId = null;
        if (clientType != null) {
            List<String> clientTypeAr = Splitter.on(SEPARATOR).splitToList(clientType);
            if (clientTypeAr.size() > 1 && clientTypeAr.get(1) != null) {
                clientTypeId = Long.parseLong(clientTypeAr.get(1));
            }
        }
        String gender = ImportHandlerUtils.readAsString(ClientPersonConstants.GENDER_COL, row);
        Long genderId = null;
        if (gender != null) {
            List<String> genderAr = Splitter.on(SEPARATOR).splitToList(gender);
            if (genderAr.size() > 1 && genderAr.get(1) != null) {
                genderId = Long.parseLong(genderAr.get(1));
            }
        }
        String clientClassification = ImportHandlerUtils.readAsString(ClientPersonConstants.CLIENT_CLASSIFICATION_COL, row);
        Long clientClassificationId = null;
        if (clientClassification != null) {
            List<String> clientClassificationAr = Splitter.on(SEPARATOR).splitToList(clientClassification);
            if (clientClassificationAr.size() > 1 && clientClassificationAr.get(1) != null) {
                clientClassificationId = Long.parseLong(clientClassificationAr.get(1));
            }
        }
        Boolean isStaff = ImportHandlerUtils.readAsBoolean(ClientPersonConstants.IS_STAFF_COL, row);

        AddressData addressDataObj = null;
        Collection<AddressData> addressList = null;
        if (ImportHandlerUtils.readAsBoolean(ClientPersonConstants.ADDRESS_ENABLED_COL, row)) {
            String addressType = ImportHandlerUtils.readAsString(ClientPersonConstants.ADDRESS_TYPE_COL, row);
            Long addressTypeId = null;
            if (addressType != null) {
                List<String> addressTypeAr = Splitter.on(SEPARATOR).splitToList(addressType);

                if (addressTypeAr.size() > 1 && addressTypeAr.get(1) != null) {
                    addressTypeId = Long.parseLong(addressTypeAr.get(1));
                }
            }
            String street = ImportHandlerUtils.readAsString(ClientPersonConstants.STREET_COL, row);
            String addressLine1 = ImportHandlerUtils.readAsString(ClientPersonConstants.ADDRESS_LINE_1_COL, row);
            String addressLine2 = ImportHandlerUtils.readAsString(ClientPersonConstants.ADDRESS_LINE_2_COL, row);
            String addressLine3 = ImportHandlerUtils.readAsString(ClientPersonConstants.ADDRESS_LINE_3_COL, row);
            String city = ImportHandlerUtils.readAsString(ClientPersonConstants.CITY_COL, row);

            String postalCode = ImportHandlerUtils.readAsString(ClientPersonConstants.POSTAL_CODE_COL, row);
            Boolean isActiveAddress = ImportHandlerUtils.readAsBoolean(ClientPersonConstants.IS_ACTIVE_ADDRESS_COL, row);

            String stateProvince = ImportHandlerUtils.readAsString(ClientPersonConstants.STATE_PROVINCE_COL, row);
            Long stateProvinceId = null;
            if (stateProvince != null) {
                List<String> stateProvinceAr = Splitter.on(SEPARATOR).splitToList(stateProvince);
                // Arkansas-AL <-- expected format of the cell
                // but probably it's either an empty cell or it is missing a
                // hyphen
                if (stateProvinceAr.size() > 1 && stateProvinceAr.get(1) != null) {
                    stateProvinceId = Long.parseLong(stateProvinceAr.get(1));
                }
            }
            String country = ImportHandlerUtils.readAsString(ClientPersonConstants.COUNTRY_COL, row);
            Long countryId = null;
            if (country != null) {
                List<String> countryAr = Splitter.on(SEPARATOR).splitToList(country);
                if (countryAr.size() > 1 && countryAr.get(1) != null) {
                    countryId = Long.parseLong(countryAr.get(1));
                }
            }
            addressDataObj = new AddressData(addressTypeId, street, addressLine1, addressLine2, addressLine3, city, postalCode,
                    isActiveAddress, stateProvinceId, countryId);
            addressList = new ArrayList<>(List.of(addressDataObj));
        }
        return ClientData.importClientPersonInstance(legalFormId, row.getRowNum(), firstName, lastName, middleName, submittedOn,
                activationDate, active, externalId, officeId, staffId, mobileNo, dob, clientTypeId, genderId, clientClassificationId,
                isStaff, addressList, locale, dateFormat);

    }

    public Count importEntity(final Workbook workbook, final List<ClientData> clients, final String dateFormat) {
        Sheet clientSheet = workbook.getSheet(TemplatePopulateImportConstants.CLIENT_PERSON_SHEET_NAME);
        int successCount = 0;
        int errorCount = 0;
        String errorMessage;
        GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        for (ClientData client : clients) {
            try {
                String payload = gsonBuilder.create().toJson(client);
                final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                        .createClient() //
                        .withJson(payload) //
                        .build(); //
                commandsSourceWritePlatformService.logCommandSource(commandRequest);
                successCount++;
                Cell statusCell = clientSheet.getRow(client.getRowIndex()).createCell(ClientPersonConstants.STATUS_COL);
                statusCell.setCellValue(TemplatePopulateImportConstants.STATUS_CELL_IMPORTED);
                statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.LIGHT_GREEN));
            } catch (RuntimeException ex) {
                errorCount++;
                LOG.error("Problem occurred in importEntity function", ex);
                errorMessage = ImportHandlerUtils.getErrorMessage(ex);
                ImportHandlerUtils.writeErrorMessage(clientSheet, client.getRowIndex(), errorMessage, ClientPersonConstants.STATUS_COL);
            }
        }
        clientSheet.setColumnWidth(ClientPersonConstants.STATUS_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        ImportHandlerUtils.writeString(ClientPersonConstants.STATUS_COL,
                clientSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX), TemplatePopulateImportConstants.STATUS_COLUMN_HEADER);

        return Count.instance(successCount, errorCount);
    }

}
