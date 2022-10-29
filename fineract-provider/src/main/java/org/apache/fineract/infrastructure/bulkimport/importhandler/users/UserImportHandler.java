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
package org.apache.fineract.infrastructure.bulkimport.importhandler.users;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.UserConstants;
import org.apache.fineract.infrastructure.bulkimport.data.Count;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandler;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandlerUtils;
import org.apache.fineract.infrastructure.core.serialization.GoogleGsonSerializerHelper;
import org.apache.fineract.useradministration.data.AppUserData;
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
public class UserImportHandler implements ImportHandler {

    private static final Logger LOG = LoggerFactory.getLogger(UserImportHandler.class);

    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @Autowired
    public UserImportHandler(final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
    }

    @Override
    public Count process(final Workbook workbook, final String locale, final String dateFormat) {
        List<String> statuses = new ArrayList<>();
        List<AppUserData> users = readExcelFile(workbook, statuses);
        return importEntity(workbook, users);
    }

    private List<AppUserData> readExcelFile(final Workbook workbook, final List<String> statuses) {
        List<AppUserData> users = new ArrayList<>();
        Sheet usersSheet = workbook.getSheet(TemplatePopulateImportConstants.USER_SHEET_NAME);
        Integer noOfEntries = ImportHandlerUtils.getNumberOfRows(usersSheet, TemplatePopulateImportConstants.FIRST_COLUMN_INDEX);
        for (int rowIndex = 1; rowIndex <= noOfEntries; rowIndex++) {
            Row row;
            row = usersSheet.getRow(rowIndex);
            if (ImportHandlerUtils.isNotImported(row, UserConstants.STATUS_COL)) {
                users.add(readUsers(workbook, row, statuses));
            }
        }
        return users;
    }

    private AppUserData readUsers(final Workbook workbook, final Row row, final List<String> statuses) {
        String officeName = ImportHandlerUtils.readAsString(UserConstants.OFFICE_NAME_COL, row);
        Long officeId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.OFFICE_SHEET_NAME), officeName);
        String staffName = ImportHandlerUtils.readAsString(UserConstants.STAFF_NAME_COL, row);
        Long staffId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.STAFF_SHEET_NAME), staffName);
        String userName = ImportHandlerUtils.readAsString(UserConstants.USER_NAME_COL, row);
        String firstName = ImportHandlerUtils.readAsString(UserConstants.FIRST_NAME_COL, row);
        String lastName = ImportHandlerUtils.readAsString(UserConstants.LAST_NAME_COL, row);
        String email = ImportHandlerUtils.readAsString(UserConstants.EMAIL_COL, row);
        Boolean autoGenPw = ImportHandlerUtils.readAsBoolean(UserConstants.AUTO_GEN_PW_COL, row);
        Boolean overridepw = ImportHandlerUtils.readAsBoolean(UserConstants.OVERRIDE_PW_EXPIRY_POLICY_COL, row);
        String status = ImportHandlerUtils.readAsString(UserConstants.STATUS_COL, row);
        statuses.add(status);

        List<Long> rolesIds = new ArrayList<>();
        for (int cellNo = UserConstants.ROLE_NAME_START_COL; cellNo < UserConstants.ROLE_NAME_END_COL; cellNo++) {
            String roleName = ImportHandlerUtils.readAsString(cellNo, row);
            if (roleName == null) {
                break;
            }
            Long roleId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.ROLES_SHEET_NAME), roleName);
            if (!rolesIds.contains(roleId)) {
                rolesIds.add(roleId);
            }
        }
        return AppUserData.importInstance(officeId, staffId, userName, firstName, lastName, email, autoGenPw, overridepw, rolesIds,
                row.getRowNum());

    }

    private Count importEntity(final Workbook workbook, final List<AppUserData> users) {
        Sheet userSheet = workbook.getSheet(TemplatePopulateImportConstants.USER_SHEET_NAME);
        int successCount = 0;
        int errorCount = 0;
        String errorMessage;
        GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        for (AppUserData user : users) {
            try {
                JsonObject userJsonob = gsonBuilder.create().toJsonTree(user).getAsJsonObject();
                String payload = userJsonob.toString();
                final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                        .createUser() //
                        .withJson(payload) //
                        .build(); //
                commandsSourceWritePlatformService.logCommandSource(commandRequest);
                successCount++;
                Cell statusCell = userSheet.getRow(user.getRowIndex()).createCell(UserConstants.STATUS_COL);
                statusCell.setCellValue(TemplatePopulateImportConstants.STATUS_CELL_IMPORTED);
                statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.LIGHT_GREEN));

            } catch (RuntimeException ex) {
                errorCount++;
                LOG.error("Problem occurred in importEntity function", ex);
                errorMessage = ImportHandlerUtils.getErrorMessage(ex);
                ImportHandlerUtils.writeErrorMessage(userSheet, user.getRowIndex(), errorMessage, UserConstants.STATUS_COL);
            }
        }
        userSheet.setColumnWidth(UserConstants.STATUS_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        ImportHandlerUtils.writeString(UserConstants.STATUS_COL, userSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX),
                TemplatePopulateImportConstants.STATUS_COL_REPORT_HEADER);
        return Count.instance(successCount, errorCount);
    }

}
