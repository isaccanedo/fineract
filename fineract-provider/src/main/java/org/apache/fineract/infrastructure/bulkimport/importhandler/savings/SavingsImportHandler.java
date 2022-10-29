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
package org.apache.fineract.infrastructure.bulkimport.importhandler.savings;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.bulkimport.constants.SavingsConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.data.Count;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandler;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandlerUtils;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.DateSerializer;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.EnumOptionDataIdSerializer;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.serialization.GoogleGsonSerializerHelper;
import org.apache.fineract.portfolio.savings.data.SavingsAccountChargeData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountData;
import org.apache.fineract.portfolio.savings.data.SavingsActivation;
import org.apache.fineract.portfolio.savings.data.SavingsApproval;
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
public class SavingsImportHandler implements ImportHandler {

    public static final String DAILY = "Daily";
    public static final String MONTHLY = "Monthly";
    public static final String QUARTERLY = "Quarterly";
    public static final String SEMI_ANNUAL = "Semi-Annual";
    public static final String ANNUALLY = "Annually";
    public static final String BI_ANNUAL = "BiAnnual";
    private static final Logger LOG = LoggerFactory.getLogger(SavingsImportHandler.class);
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @Autowired
    public SavingsImportHandler(final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
    }

    @Override
    public Count process(final Workbook workbook, final String locale, final String dateFormat) {
        List<SavingsAccountData> savings = new ArrayList<>();
        List<SavingsApproval> approvalDates = new ArrayList<>();
        List<SavingsActivation> activationDates = new ArrayList<>();
        List<String> statuses = new ArrayList<>();
        readExcelFile(workbook, savings, approvalDates, activationDates, statuses, locale, dateFormat);
        return importEntity(workbook, savings, approvalDates, activationDates, statuses, dateFormat);
    }

    public void readExcelFile(final Workbook workbook, final List<SavingsAccountData> savings, final List<SavingsApproval> approvalDates,
            final List<SavingsActivation> activationDates, final List<String> statuses, final String locale, final String dateFormat) {
        Sheet savingsSheet = workbook.getSheet(TemplatePopulateImportConstants.SAVINGS_ACCOUNTS_SHEET_NAME);
        Integer noOfEntries = ImportHandlerUtils.getNumberOfRows(savingsSheet, TemplatePopulateImportConstants.FIRST_COLUMN_INDEX);
        for (int rowIndex = 1; rowIndex <= noOfEntries; rowIndex++) {
            Row row;
            row = savingsSheet.getRow(rowIndex);
            if (ImportHandlerUtils.isNotImported(row, SavingsConstants.STATUS_COL)) {
                savings.add(readSavings(workbook, row, statuses, locale, dateFormat));
                approvalDates.add(readSavingsApproval(row, locale, dateFormat));
                activationDates.add(readSavingsActivation(row, locale, dateFormat));
            }
        }
    }

    private SavingsActivation readSavingsActivation(Row row, String locale, String dateFormat) {
        LocalDate activationDate = ImportHandlerUtils.readAsDate(SavingsConstants.ACTIVATION_DATE_COL, row);
        if (activationDate != null) {
            return SavingsActivation.importInstance(activationDate, row.getRowNum(), locale, dateFormat);
        } else {
            return null;
        }
    }

    private SavingsApproval readSavingsApproval(Row row, String locale, String dateFormat) {
        LocalDate approvalDate = ImportHandlerUtils.readAsDate(SavingsConstants.APPROVED_DATE_COL, row);
        if (approvalDate != null) {
            return SavingsApproval.importInstance(approvalDate, row.getRowNum(), locale, dateFormat);
        } else {
            return null;
        }
    }

    private SavingsAccountData readSavings(final Workbook workbook, final Row row, final List<String> statuses, final String locale,
            final String dateFormat) {
        String productName = ImportHandlerUtils.readAsString(SavingsConstants.PRODUCT_COL, row);
        Long productId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.PRODUCT_SHEET_NAME), productName);
        String fieldOfficerName = ImportHandlerUtils.readAsString(SavingsConstants.FIELD_OFFICER_NAME_COL, row);
        Long fieldOfficerId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.STAFF_SHEET_NAME),
                fieldOfficerName);
        LocalDate submittedOnDate = ImportHandlerUtils.readAsDate(SavingsConstants.SUBMITTED_ON_DATE_COL, row);

        BigDecimal nominalAnnualInterestRate = null;
        if (ImportHandlerUtils.readAsDouble(SavingsConstants.NOMINAL_ANNUAL_INTEREST_RATE_COL, row) != null) {
            nominalAnnualInterestRate = BigDecimal
                    .valueOf(ImportHandlerUtils.readAsDouble(SavingsConstants.NOMINAL_ANNUAL_INTEREST_RATE_COL, row));
        }
        String interestCompoundingPeriodType = ImportHandlerUtils.readAsString(SavingsConstants.INTEREST_COMPOUNDING_PERIOD_COL, row);
        Long interestCompoundingPeriodTypeId = null;
        EnumOptionData interestCompoundingPeriodTypeEnum = null;
        if (interestCompoundingPeriodType != null) {
            if (interestCompoundingPeriodType.equalsIgnoreCase(DAILY)) {
                interestCompoundingPeriodTypeId = 1L;
            } else if (interestCompoundingPeriodType.equalsIgnoreCase(MONTHLY)) {
                interestCompoundingPeriodTypeId = 4L;
            } else if (interestCompoundingPeriodType.equalsIgnoreCase(QUARTERLY)) {
                interestCompoundingPeriodTypeId = 5L;
            } else if (interestCompoundingPeriodType.equalsIgnoreCase(SEMI_ANNUAL)) {
                interestCompoundingPeriodTypeId = 6L;
            } else if (interestCompoundingPeriodType.equalsIgnoreCase(ANNUALLY)) {
                interestCompoundingPeriodTypeId = 7L;
            }
            interestCompoundingPeriodTypeEnum = new EnumOptionData(interestCompoundingPeriodTypeId, null, null);
        }
        String interestPostingPeriodType = ImportHandlerUtils.readAsString(SavingsConstants.INTEREST_POSTING_PERIOD_COL, row);
        Long interestPostingPeriodTypeId = null;
        EnumOptionData interestPostingPeriodTypeEnum = null;
        if (interestPostingPeriodType != null) {
            if (interestPostingPeriodType.equalsIgnoreCase(MONTHLY)) {
                interestPostingPeriodTypeId = 4L;
            } else if (interestPostingPeriodType.equalsIgnoreCase(QUARTERLY)) {
                interestPostingPeriodTypeId = 5L;
            } else if (interestPostingPeriodType.equalsIgnoreCase(ANNUALLY)) {
                interestPostingPeriodTypeId = 7L;
            } else if (interestPostingPeriodType.equalsIgnoreCase(BI_ANNUAL)) {
                interestPostingPeriodTypeId = 6L;
            }
            interestPostingPeriodTypeEnum = new EnumOptionData(interestPostingPeriodTypeId, null, null);
        }
        String interestCalculationType = ImportHandlerUtils.readAsString(SavingsConstants.INTEREST_CALCULATION_COL, row);
        Long interestCalculationTypeId = null;
        EnumOptionData interestCalculationTypeEnum = null;
        if (interestCalculationType != null) {
            if (interestCalculationType.equalsIgnoreCase("Daily Balance")) {
                interestCalculationTypeId = 1L;
            } else if (interestCalculationType.equalsIgnoreCase("Average Daily Balance")) {
                interestCalculationTypeId = 2L;
            }
            interestCalculationTypeEnum = new EnumOptionData(interestCalculationTypeId, null, null);
        }
        String interestCalculationDaysInYearType = ImportHandlerUtils.readAsString(SavingsConstants.INTEREST_CALCULATION_DAYS_IN_YEAR_COL,
                row);
        EnumOptionData interestCalculationDaysInYearTypeEnum = null;
        Long interestCalculationDaysInYearTypeId = null;
        if (interestCalculationDaysInYearType != null) {
            if (interestCalculationDaysInYearType.equalsIgnoreCase("360 Days")) {
                interestCalculationDaysInYearTypeId = 360L;
            } else if (interestCalculationDaysInYearType.equalsIgnoreCase("365 Days")) {
                interestCalculationDaysInYearTypeId = 365L;
            }
            interestCalculationDaysInYearTypeEnum = new EnumOptionData(interestCalculationDaysInYearTypeId, null, null);
        }
        BigDecimal minRequiredOpeningBalance = null;
        if (ImportHandlerUtils.readAsDouble(SavingsConstants.MIN_OPENING_BALANCE_COL, row) != null) {
            minRequiredOpeningBalance = BigDecimal.valueOf(ImportHandlerUtils.readAsDouble(SavingsConstants.MIN_OPENING_BALANCE_COL, row));
        }
        Integer lockinPeriodFrequency = ImportHandlerUtils.readAsInt(SavingsConstants.LOCKIN_PERIOD_COL, row);
        String lockinPeriodFrequencyType = ImportHandlerUtils.readAsString(SavingsConstants.LOCKIN_PERIOD_FREQUENCY_COL, row);
        Long lockinPeriodFrequencyTypeId = null;
        EnumOptionData lockinPeriodFrequencyTypeEnum = null;
        if (lockinPeriodFrequencyType != null) {
            if (lockinPeriodFrequencyType.equalsIgnoreCase("Days")) {
                lockinPeriodFrequencyTypeId = 0L;
            } else if (lockinPeriodFrequencyType.equalsIgnoreCase("Weeks")) {
                lockinPeriodFrequencyTypeId = 1L;
            } else if (lockinPeriodFrequencyType.equalsIgnoreCase("Months")) {
                lockinPeriodFrequencyTypeId = 2L;
            } else if (lockinPeriodFrequencyType.equalsIgnoreCase("Years")) {
                lockinPeriodFrequencyTypeId = 3L;
            }
            lockinPeriodFrequencyTypeEnum = new EnumOptionData(lockinPeriodFrequencyTypeId, null, null);
        }
        Boolean applyWithdrawalFeeForTransfers = ImportHandlerUtils.readAsBoolean(SavingsConstants.APPLY_WITHDRAWAL_FEE_FOR_TRANSFERS, row);

        String savingsType = null;
        if (ImportHandlerUtils.readAsString(SavingsConstants.SAVINGS_TYPE_COL, row) != null) {
            savingsType = ImportHandlerUtils.readAsString(SavingsConstants.SAVINGS_TYPE_COL, row).toLowerCase(Locale.ENGLISH);
        }

        String clientOrGroupName = ImportHandlerUtils.readAsString(SavingsConstants.CLIENT_NAME_COL, row);

        String externalId = ImportHandlerUtils.readAsString(SavingsConstants.EXTERNAL_ID_COL, row);
        List<SavingsAccountChargeData> charges = new ArrayList<>();

        Boolean allowOverdraft = ImportHandlerUtils.readAsBoolean(SavingsConstants.ALLOW_OVER_DRAFT_COL, row);
        BigDecimal overdraftLimit = BigDecimal.valueOf(ImportHandlerUtils.readAsDouble(SavingsConstants.OVER_DRAFT_LIMIT_COL, row));

        String charge1 = ImportHandlerUtils.readAsString(SavingsConstants.CHARGE_ID_1, row);
        String charge2 = ImportHandlerUtils.readAsString(SavingsConstants.CHARGE_ID_2, row);

        if (charge1 != null) {
            if (ImportHandlerUtils.readAsDouble(SavingsConstants.CHARGE_AMOUNT_1, row) != null) {
                charges.add(new SavingsAccountChargeData(ImportHandlerUtils.readAsLong(SavingsConstants.CHARGE_ID_1, row),
                        BigDecimal.valueOf(ImportHandlerUtils.readAsDouble(SavingsConstants.CHARGE_AMOUNT_1, row)),
                        ImportHandlerUtils.readAsDate(SavingsConstants.CHARGE_DUE_DATE_1, row)));
            } else {
                charges.add(new SavingsAccountChargeData(ImportHandlerUtils.readAsLong(SavingsConstants.CHARGE_ID_1, row), null,
                        ImportHandlerUtils.readAsDate(SavingsConstants.CHARGE_DUE_DATE_1, row)));
            }
        }

        if (charge2 != null) {
            if (ImportHandlerUtils.readAsDouble(SavingsConstants.CHARGE_AMOUNT_2, row) != null) {
                charges.add(new SavingsAccountChargeData(ImportHandlerUtils.readAsLong(SavingsConstants.CHARGE_ID_2, row),
                        BigDecimal.valueOf(ImportHandlerUtils.readAsDouble(SavingsConstants.CHARGE_AMOUNT_2, row)),
                        ImportHandlerUtils.readAsDate(SavingsConstants.CHARGE_DUE_DATE_2, row)));
            } else {
                charges.add(new SavingsAccountChargeData(ImportHandlerUtils.readAsLong(SavingsConstants.CHARGE_ID_2, row), null,
                        ImportHandlerUtils.readAsDate(SavingsConstants.CHARGE_DUE_DATE_2, row)));
            }
        }
        String status = ImportHandlerUtils.readAsString(SavingsConstants.STATUS_COL, row);
        statuses.add(status);
        if (savingsType != null) {
            if (savingsType.equals("individual")) {
                Long clientId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.CLIENT_SHEET_NAME),
                        clientOrGroupName);
                return SavingsAccountData.importInstanceIndividual(clientId, productId, fieldOfficerId, submittedOnDate,
                        nominalAnnualInterestRate, interestCompoundingPeriodTypeEnum, interestPostingPeriodTypeEnum,
                        interestCalculationTypeEnum, interestCalculationDaysInYearTypeEnum, minRequiredOpeningBalance,
                        lockinPeriodFrequency, lockinPeriodFrequencyTypeEnum, applyWithdrawalFeeForTransfers, row.getRowNum(), externalId,
                        charges, allowOverdraft, overdraftLimit, locale, dateFormat);
            }
            Long groupId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.GROUP_SHEET_NAME),
                    clientOrGroupName);
            return SavingsAccountData.importInstanceGroup(groupId, productId, fieldOfficerId, submittedOnDate, nominalAnnualInterestRate,
                    interestCompoundingPeriodTypeEnum, interestPostingPeriodTypeEnum, interestCalculationTypeEnum,
                    interestCalculationDaysInYearTypeEnum, minRequiredOpeningBalance, lockinPeriodFrequency, lockinPeriodFrequencyTypeEnum,
                    applyWithdrawalFeeForTransfers, row.getRowNum(), externalId, charges, allowOverdraft, overdraftLimit, locale,
                    dateFormat);
        } else {
            return null;
        }

    }

    public Count importEntity(final Workbook workbook, final List<SavingsAccountData> savings, final List<SavingsApproval> approvalDates,
            final List<SavingsActivation> activationDates, final List<String> statuses, final String dateFormat) {
        Sheet savingsSheet = workbook.getSheet(TemplatePopulateImportConstants.SAVINGS_ACCOUNTS_SHEET_NAME);
        int successCount = 0;
        int errorCount = 0;
        int progressLevel = 0;
        String errorMessage = "";
        Long savingsId = null;
        for (int i = 0; i < savings.size(); i++) {
            Row row = savingsSheet.getRow(savings.get(i).getRowIndex());
            Cell statusCell = row.createCell(SavingsConstants.STATUS_COL);
            Cell errorReportCell = row.createCell(SavingsConstants.FAILURE_REPORT_COL);
            try {
                String status = statuses.get(i);
                progressLevel = getProgressLevel(status);

                if (progressLevel == 0) {
                    CommandProcessingResult result = importSavings(savings, i, dateFormat);
                    savingsId = result.getSavingsId();
                    progressLevel = 1;
                } else {
                    savingsId = ImportHandlerUtils.readAsLong(SavingsConstants.SAVINGS_ID_COL,
                            savingsSheet.getRow(savings.get(i).getRowIndex()));
                }

                if (progressLevel <= 1) {
                    progressLevel = importSavingsApproval(approvalDates, savingsId, i, dateFormat);
                }

                if (progressLevel <= 2) {
                    progressLevel = importSavingsActivation(activationDates, savingsId, i, dateFormat);
                }
                successCount++;
                statusCell.setCellValue(TemplatePopulateImportConstants.STATUS_CELL_IMPORTED);
                statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.LIGHT_GREEN));
            } catch (RuntimeException ex) {
                errorCount++;
                LOG.error("Problem occurred in importEntity function", ex);
                errorMessage = ImportHandlerUtils.getErrorMessage(ex);
                writeSavingsErrorMessage(workbook, savingsId, errorMessage, progressLevel, statusCell, errorReportCell, row);
            }
        }
        setReportHeaders(savingsSheet);
        return Count.instance(successCount, errorCount);
    }

    private void writeSavingsErrorMessage(final Workbook workbook, final Long savingsId, final String errorMessage, final int progressLevel,
            final Cell statusCell, final Cell errorReportCell, final Row row) {
        String status = "";

        if (progressLevel == 0) {
            status = TemplatePopulateImportConstants.STATUS_CREATION_FAILED;
        } else if (progressLevel == 1) {
            status = TemplatePopulateImportConstants.STATUS_APPROVAL_FAILED;
        } else if (progressLevel == 2) {
            status = TemplatePopulateImportConstants.STATUS_ACTIVATION_FAILED;
        }
        statusCell.setCellValue(status);
        statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.RED));

        if (progressLevel > 0) {
            row.createCell(SavingsConstants.SAVINGS_ID_COL).setCellValue(savingsId);
        }

        errorReportCell.setCellValue(errorMessage);
    }

    private void setReportHeaders(Sheet savingsSheet) {
        savingsSheet.setColumnWidth(SavingsConstants.STATUS_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        Row rowHeader = savingsSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX);
        ImportHandlerUtils.writeString(SavingsConstants.STATUS_COL, rowHeader, TemplatePopulateImportConstants.STATUS_COL_REPORT_HEADER);
        ImportHandlerUtils.writeString(SavingsConstants.SAVINGS_ID_COL, rowHeader,
                TemplatePopulateImportConstants.SAVINGS_ID_COL_REPORT_HEADER);
        ImportHandlerUtils.writeString(SavingsConstants.FAILURE_REPORT_COL, rowHeader,
                TemplatePopulateImportConstants.FAILURE_COL_REPORT_HEADER);
    }

    private int importSavingsActivation(final List<SavingsActivation> activationDates, final Long savingsId, final int i,
            final String dateFormat) {
        if (activationDates.get(i) != null) {
            GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
            gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
            String payload = gsonBuilder.create().toJson(activationDates.get(i));
            final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                    .savingsAccountActivation(savingsId)//
                    .withJson(payload) //
                    .build(); //
            commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }
        return 3;
    }

    private int importSavingsApproval(final List<SavingsApproval> approvalDates, final Long savingsId, final int i,
            final String dateFormat) {
        if (approvalDates.get(i) != null) {
            GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
            gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
            String payload = gsonBuilder.create().toJson(approvalDates.get(i));
            final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                    .approveSavingsAccountApplication(savingsId)//
                    .withJson(payload) //
                    .build(); //
            commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }
        return 2;
    }

    private CommandProcessingResult importSavings(final List<SavingsAccountData> savings, final int i, final String dateFormat) {
        GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        gsonBuilder.registerTypeAdapter(EnumOptionData.class, new EnumOptionDataIdSerializer());
        JsonObject savingsJsonob = gsonBuilder.create().toJsonTree(savings.get(i)).getAsJsonObject();
        savingsJsonob.remove("isDormancyTrackingActive");
        String payload = savingsJsonob.toString();
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .createSavingsAccount() //
                .withJson(payload) //
                .build(); //
        return commandsSourceWritePlatformService.logCommandSource(commandRequest);
    }

    private int getProgressLevel(String status) {
        if (status == null || status.equals(TemplatePopulateImportConstants.STATUS_CREATION_FAILED)) {
            return 0;
        } else if (status.equals(TemplatePopulateImportConstants.STATUS_APPROVAL_FAILED)) {
            return 1;
        } else if (status.equals(TemplatePopulateImportConstants.STATUS_ACTIVATION_FAILED)) {
            return 2;
        }
        return 0;
    }

}
