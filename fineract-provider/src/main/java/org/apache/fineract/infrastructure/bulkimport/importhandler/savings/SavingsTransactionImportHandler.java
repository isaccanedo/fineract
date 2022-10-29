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
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TransactionConstants;
import org.apache.fineract.infrastructure.bulkimport.data.Count;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandler;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandlerUtils;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.DateSerializer;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.SavingsAccountTransactionEnumValueSerialiser;
import org.apache.fineract.infrastructure.core.serialization.GoogleGsonSerializerHelper;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionEnumData;
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
public class SavingsTransactionImportHandler implements ImportHandler {

    public static final String TRANSACTION_TYPE = "transactionType";
    public static final String REVERSED = "reversed";
    public static final String INTERESTED_POSTED_AS_ON = "interestedPostedAsOn";
    public static final String WITHDRAWAL = "Withdrawal";
    public static final String DEPOSIT = "Deposit";
    private static final Logger LOG = LoggerFactory.getLogger(SavingsTransactionImportHandler.class);
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @Autowired
    public SavingsTransactionImportHandler(final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
    }

    @Override
    public Count process(final Workbook workbook, final String locale, final String dateFormat) {
        List<SavingsAccountTransactionData> savingsTransactions = readExcelFile(workbook, locale, dateFormat);
        return importEntity(workbook, savingsTransactions, dateFormat);
    }

    private List<SavingsAccountTransactionData> readExcelFile(final Workbook workbook, final String locale, final String dateFormat) {
        List<SavingsAccountTransactionData> savingsTransactions = new ArrayList<>();
        Sheet savingsTransactionSheet = workbook.getSheet(TemplatePopulateImportConstants.SAVINGS_TRANSACTION_SHEET_NAME);
        Integer noOfEntries = ImportHandlerUtils.getNumberOfRows(savingsTransactionSheet, TransactionConstants.AMOUNT_COL);
        Long savingsAccountId = null;
        for (int rowIndex = 1; rowIndex <= noOfEntries; rowIndex++) {
            Row row;
            row = savingsTransactionSheet.getRow(rowIndex);
            if (ImportHandlerUtils.isNotImported(row, TransactionConstants.STATUS_COL)) {
                savingsTransactions.add(readSavingsTransaction(workbook, row, savingsAccountId, locale, dateFormat));
            }
        }
        return savingsTransactions;
    }

    private SavingsAccountTransactionData readSavingsTransaction(final Workbook workbook, final Row row, Long savingsAccountId,
            final String locale, final String dateFormat) {
        Long savingsAccountIdCheck = ImportHandlerUtils.readAsLong(TransactionConstants.SAVINGS_ACCOUNT_NO_COL, row);

        if (savingsAccountIdCheck != null) {
            savingsAccountId = savingsAccountIdCheck;
        }
        String transactionType = ImportHandlerUtils.readAsString(TransactionConstants.TRANSACTION_TYPE_COL, row);
        SavingsAccountTransactionEnumData savingsAccountTransactionEnumData = new SavingsAccountTransactionEnumData(null, null,
                transactionType);

        BigDecimal amount = null;
        if (ImportHandlerUtils.readAsDouble(TransactionConstants.AMOUNT_COL, row) != null) {
            amount = BigDecimal.valueOf(ImportHandlerUtils.readAsDouble(TransactionConstants.AMOUNT_COL, row));
        }

        LocalDate transactionDate = ImportHandlerUtils.readAsDate(TransactionConstants.TRANSACTION_DATE_COL, row);
        String paymentType = ImportHandlerUtils.readAsString(TransactionConstants.PAYMENT_TYPE_COL, row);
        Long paymentTypeId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.EXTRAS_SHEET_NAME),
                paymentType);
        String accountNumber = ImportHandlerUtils.readAsString(TransactionConstants.ACCOUNT_NO_COL, row);
        String checkNumber = ImportHandlerUtils.readAsString(TransactionConstants.CHECK_NO_COL, row);
        String routingCode = ImportHandlerUtils.readAsString(TransactionConstants.ROUTING_CODE_COL, row);
        String receiptNumber = ImportHandlerUtils.readAsString(TransactionConstants.RECEIPT_NO_COL, row);
        String bankNumber = ImportHandlerUtils.readAsString(TransactionConstants.BANK_NO_COL, row);
        return SavingsAccountTransactionData.importInstance(amount, transactionDate, paymentTypeId, accountNumber, checkNumber, routingCode,
                receiptNumber, bankNumber, savingsAccountId, savingsAccountTransactionEnumData, row.getRowNum(), locale, dateFormat);

    }

    private Count importEntity(final Workbook workbook, final List<SavingsAccountTransactionData> savingsTransactions,
            final String dateFormat) {
        Sheet savingsTransactionSheet = workbook.getSheet(TemplatePopulateImportConstants.SAVINGS_TRANSACTION_SHEET_NAME);
        int successCount = 0;
        int errorCount = 0;
        String errorMessage = "";
        GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        gsonBuilder.registerTypeAdapter(SavingsAccountTransactionEnumData.class, new SavingsAccountTransactionEnumValueSerialiser());

        for (SavingsAccountTransactionData transaction : savingsTransactions) {
            try {
                JsonObject savingsTransactionJsonob = gsonBuilder.create().toJsonTree(transaction).getAsJsonObject();
                savingsTransactionJsonob.remove(TRANSACTION_TYPE);
                savingsTransactionJsonob.remove(REVERSED);
                savingsTransactionJsonob.remove(INTERESTED_POSTED_AS_ON);
                String payload = savingsTransactionJsonob.toString();
                CommandWrapper commandRequest = null;
                if (transaction.getTransactionType().getValue().equals(WITHDRAWAL)) {
                    commandRequest = new CommandWrapperBuilder() //
                            .savingsAccountWithdrawal(transaction.getSavingsAccountId()) //
                            .withJson(payload) //
                            .build(); //

                } else if (transaction.getTransactionType().getValue().equals(DEPOSIT)) {
                    commandRequest = new CommandWrapperBuilder() //
                            .savingsAccountDeposit(transaction.getSavingsAccountId()) //
                            .withJson(payload) //
                            .build();
                }
                commandsSourceWritePlatformService.logCommandSource(commandRequest);
                successCount++;
                Cell statusCell = savingsTransactionSheet.getRow(transaction.getRowIndex()).createCell(TransactionConstants.STATUS_COL);
                statusCell.setCellValue(TemplatePopulateImportConstants.STATUS_CELL_IMPORTED);
                statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.LIGHT_GREEN));
            } catch (RuntimeException ex) {
                errorCount++;
                LOG.error("Problem occurred in importEntity function", ex);
                errorMessage = ImportHandlerUtils.getErrorMessage(ex);
                ImportHandlerUtils.writeErrorMessage(savingsTransactionSheet, transaction.getRowIndex(), errorMessage,
                        TransactionConstants.STATUS_COL);
            }
        }
        savingsTransactionSheet.setColumnWidth(TransactionConstants.STATUS_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        ImportHandlerUtils.writeString(TransactionConstants.STATUS_COL, savingsTransactionSheet.getRow(TransactionConstants.STATUS_COL),
                TemplatePopulateImportConstants.STATUS_COL_REPORT_HEADER);
        return Count.instance(successCount, errorCount);
    }

}
