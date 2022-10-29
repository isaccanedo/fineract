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
package org.apache.fineract.infrastructure.bulkimport.importhandler.sharedaccount;

import com.google.gson.GsonBuilder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.bulkimport.constants.SharedAccountsConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.data.Count;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandler;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandlerUtils;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.DateSerializer;
import org.apache.fineract.infrastructure.core.serialization.GoogleGsonSerializerHelper;
import org.apache.fineract.portfolio.shareaccounts.data.ShareAccountChargeData;
import org.apache.fineract.portfolio.shareaccounts.data.ShareAccountData;
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
public class SharedAccountImportHandler implements ImportHandler {

    public static final String ZERO = "0";
    public static final String SHARE = "share";
    public static final String EMPTY_STR = "";
    private static final Logger LOG = LoggerFactory.getLogger(SharedAccountImportHandler.class);
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @Autowired
    public SharedAccountImportHandler(final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
    }

    @Override
    public Count process(final Workbook workbook, final String locale, final String dateFormat) {
        List<ShareAccountData> shareAccountDataList = new ArrayList<>();
        List<String> statuses = new ArrayList<>();
        readExcelFile(workbook, shareAccountDataList, statuses, locale, dateFormat);
        return importEntity(workbook, shareAccountDataList, dateFormat);
    }

    private void readExcelFile(final Workbook workbook, final List<ShareAccountData> shareAccountDataList, final List<String> statuses,
            final String locale, String dateFormat) {
        Sheet sharedAccountsSheet = workbook.getSheet(TemplatePopulateImportConstants.SHARED_ACCOUNTS_SHEET_NAME);
        Integer noOfEntries = ImportHandlerUtils.getNumberOfRows(sharedAccountsSheet, TemplatePopulateImportConstants.FIRST_COLUMN_INDEX);
        for (int rowIndex = 1; rowIndex <= noOfEntries; rowIndex++) {
            Row row;
            row = sharedAccountsSheet.getRow(rowIndex);
            if (ImportHandlerUtils.isNotImported(row, SharedAccountsConstants.STATUS_COL)) {
                shareAccountDataList.add(readSharedAccount(workbook, row, statuses, locale, dateFormat));
            }
        }
    }

    private ShareAccountData readSharedAccount(final Workbook workbook, final Row row, final List<String> statuses, final String locale,
            final String dateFormat) {
        String clientName = ImportHandlerUtils.readAsString(SharedAccountsConstants.CLIENT_NAME_COL, row);
        Long clientId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.CLIENT_SHEET_NAME), clientName);

        String productName = ImportHandlerUtils.readAsString(SharedAccountsConstants.PRODUCT_COL, row);
        Long productId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.SHARED_PRODUCTS_SHEET_NAME),
                productName);

        LocalDate submittedOnDate = ImportHandlerUtils.readAsDate(SharedAccountsConstants.SUBMITTED_ON_COL, row);

        String externalId = ImportHandlerUtils.readAsString(SharedAccountsConstants.EXTERNAL_ID_COL, row);

        Integer totNoOfShares = ImportHandlerUtils.readAsInt(SharedAccountsConstants.TOTAL_NO_SHARES_COL, row);

        Long defaultSavingsAccountId = ImportHandlerUtils.readAsLong(SharedAccountsConstants.DEFAULT_SAVINGS_AC_COL, row);

        Integer minimumActivePeriodDays = ImportHandlerUtils.readAsInt(SharedAccountsConstants.MINIMUM_ACTIVE_PERIOD_IN_DAYS_COL, row);
        Integer minimumActivePeriodFrequencyType = 0;

        Integer lockInPeriod = ImportHandlerUtils.readAsInt(SharedAccountsConstants.LOCK_IN_PERIOD_COL, row);

        Integer lockPeriodFrequencyType = null;

        if (ImportHandlerUtils.readAsString(SharedAccountsConstants.LOCK_IN_PERIOD_FREQUENCY_TYPE, row) != null) {
            if (ImportHandlerUtils.readAsString(SharedAccountsConstants.LOCK_IN_PERIOD_FREQUENCY_TYPE, row)
                    .equals(TemplatePopulateImportConstants.FREQUENCY_DAYS)) {
                lockPeriodFrequencyType = 0;
            } else if (ImportHandlerUtils.readAsString(SharedAccountsConstants.LOCK_IN_PERIOD_FREQUENCY_TYPE, row)
                    .equals(TemplatePopulateImportConstants.FREQUENCY_WEEKS)) {
                lockPeriodFrequencyType = 1;
            } else if (ImportHandlerUtils.readAsString(SharedAccountsConstants.LOCK_IN_PERIOD_FREQUENCY_TYPE, row)
                    .equals(TemplatePopulateImportConstants.FREQUENCY_MONTHS)) {
                lockPeriodFrequencyType = 2;
            } else if (ImportHandlerUtils.readAsString(SharedAccountsConstants.LOCK_IN_PERIOD_FREQUENCY_TYPE, row)
                    .equals(TemplatePopulateImportConstants.FREQUENCY_YEARS)) {
                lockPeriodFrequencyType = 3;
            }
        }
        LocalDate applicationDate = ImportHandlerUtils.readAsDate(SharedAccountsConstants.APPLICATION_DATE_COL, row);
        Boolean allowDividendCalc = ImportHandlerUtils
                .readAsBoolean(SharedAccountsConstants.ALLOW_DIVIDEND_CALCULATION_FOR_INACTIVE_CLIENTS_COL, row);

        List<ShareAccountChargeData> charges = new ArrayList<>();
        for (int cellNo = SharedAccountsConstants.CHARGES_NAME_1_COL; cellNo < SharedAccountsConstants.CHARGES_NAME_3_COL; cellNo += 2) {
            String chargeName = ImportHandlerUtils.readAsString(cellNo, row);
            if (chargeName == null || chargeName.equals(ZERO)) {
                break;
            }
            Long chargeId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.SHARED_PRODUCTS_SHEET_NAME),
                    chargeName);

            BigDecimal amount = null;
            if (ImportHandlerUtils.readAsDouble(cellNo + 1, row) != null) {
                amount = BigDecimal.valueOf(ImportHandlerUtils.readAsDouble(cellNo + 1, row));
            }

            ShareAccountChargeData shareAccountChargeData = new ShareAccountChargeData(chargeId, amount);
            charges.add(shareAccountChargeData);
        }
        String status = ImportHandlerUtils.readAsString(SharedAccountsConstants.STATUS_COL, row);
        statuses.add(status);

        return ShareAccountData.importInstance(clientId, productId, totNoOfShares, externalId, submittedOnDate, minimumActivePeriodDays,
                minimumActivePeriodFrequencyType, lockInPeriod, lockPeriodFrequencyType, applicationDate, allowDividendCalc, charges,
                defaultSavingsAccountId, row.getRowNum(), locale, dateFormat);
    }

    private Count importEntity(final Workbook workbook, final List<ShareAccountData> shareAccountDataList, final String dateFormat) {
        Sheet sharedAccountsSheet = workbook.getSheet(TemplatePopulateImportConstants.SHARED_ACCOUNTS_SHEET_NAME);
        int successCount = 0;
        int errorCount = 0;
        String errorMessage;
        GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));

        for (ShareAccountData shareAccountData : shareAccountDataList) {
            try {
                String payload = gsonBuilder.create().toJson(shareAccountData);
                final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                        .createAccount(SHARE)//
                        .withJson(payload) //
                        .build(); //
                commandsSourceWritePlatformService.logCommandSource(commandRequest);
                successCount++;
                Cell statusCell = sharedAccountsSheet.getRow(shareAccountData.getRowIndex()).createCell(SharedAccountsConstants.STATUS_COL);
                statusCell.setCellValue(TemplatePopulateImportConstants.STATUS_CELL_IMPORTED);
                statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.LIGHT_GREEN));
            } catch (RuntimeException ex) {
                errorCount++;
                LOG.error("Problem occurred in importEntity function", ex);
                errorMessage = ImportHandlerUtils.getErrorMessage(ex);
                ImportHandlerUtils.writeErrorMessage(sharedAccountsSheet, shareAccountData.getRowIndex(), errorMessage,
                        SharedAccountsConstants.STATUS_COL);
            }
        }
        sharedAccountsSheet.setColumnWidth(SharedAccountsConstants.STATUS_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        ImportHandlerUtils.writeString(SharedAccountsConstants.STATUS_COL,
                sharedAccountsSheet.getRow(TemplatePopulateImportConstants.ROW_HEADER_HEIGHT),
                TemplatePopulateImportConstants.STATUS_COL_REPORT_HEADER);
        return Count.instance(successCount, errorCount);
    }

}
