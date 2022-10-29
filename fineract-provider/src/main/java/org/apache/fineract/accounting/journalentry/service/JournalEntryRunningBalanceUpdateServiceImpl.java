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
package org.apache.fineract.accounting.journalentry.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.accounting.common.AccountingEnumerations;
import org.apache.fineract.accounting.glaccount.domain.GLAccountType;
import org.apache.fineract.accounting.journalentry.api.JournalEntryJsonInputParams;
import org.apache.fineract.accounting.journalentry.data.JournalEntryData;
import org.apache.fineract.accounting.journalentry.data.JournalEntryDataValidator;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryType;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JournalEntryRunningBalanceUpdateServiceImpl implements JournalEntryRunningBalanceUpdateService {

    private final JdbcTemplate jdbcTemplate;

    private final OfficeRepositoryWrapper officeRepositoryWrapper;

    private final JournalEntryDataValidator dataValidator;

    private final FromJsonHelper fromApiJsonHelper;
    private final DatabaseSpecificSQLGenerator sqlGenerator;

    private final GLJournalEntryMapper entryMapper = new GLJournalEntryMapper();

    private final PlatformSecurityContext platformSecurityContext;

    @Override
    public void updateRunningBalance() {
        String dateFinder = "select MIN(je.entry_date) as entityDate from acc_gl_journal_entry  je "
                + "where je.is_running_balance_calculated=false ";
        try {
            LocalDate entityDate = this.jdbcTemplate.queryForObject(dateFinder, LocalDate.class);
            updateOrganizationRunningBalance(entityDate);
        } catch (EmptyResultDataAccessException e) {
            log.debug("No results found for updation of running balance ");
        }
    }

    @Override
    public CommandProcessingResult updateOfficeRunningBalance(JsonCommand command) {
        this.dataValidator.validateForUpdateRunningBalance(command);
        final Long officeId = this.fromApiJsonHelper.extractLongNamed(JournalEntryJsonInputParams.OFFICE_ID.getValue(),
                command.parsedJson());
        CommandProcessingResultBuilder commandProcessingResultBuilder = new CommandProcessingResultBuilder()
                .withCommandId(command.commandId());
        if (officeId == null) {
            updateRunningBalance();
        } else {
            this.officeRepositoryWrapper.findOneWithNotFoundDetection(officeId);
            String dateFinder = "select MIN(je.entry_date) as entityDate " + "from acc_gl_journal_entry  je "
                    + "where je.is_running_balance_calculated=false  and je.office_id=?";
            try {
                LocalDate entityDate = this.jdbcTemplate.queryForObject(dateFinder, LocalDate.class, officeId);
                updateRunningBalance(officeId, entityDate);
            } catch (EmptyResultDataAccessException e) {
                log.debug("No results found for updation of office running balance with office id: {}", officeId);
            }
            commandProcessingResultBuilder.withOfficeId(officeId);
        }
        return commandProcessingResultBuilder.build();
    }

    private void updateOrganizationRunningBalance(LocalDate entityDate) {
        Map<Long, BigDecimal> runningBalanceMap = new HashMap<>(5);
        Map<Long, Map<Long, BigDecimal>> officesRunningBalance = new HashMap<>();

        final String organizationRunningBalanceQuery = "select je.organization_running_balance as runningBalance,je.account_id as accountId from acc_gl_journal_entry je "
                + "inner join (select max(id) as id from acc_gl_journal_entry where entry_date < ? group by account_id,entry_date) je2 ON je2.id = je.id "
                + "inner join (select max(entry_date) as date from acc_gl_journal_entry where entry_date < ? group by account_id) je3 ON je.entry_date = je3.date "
                + "group by je.id order by je.entry_date DESC " + sqlGenerator.limit(10000, 0);

        List<Map<String, Object>> list = jdbcTemplate.queryForList(organizationRunningBalanceQuery, // NOSONAR
                entityDate, entityDate);

        for (Map<String, Object> entries : list) {
            Long accountId = Long.parseLong(entries.get("accountId").toString()); // Drizzle
                                                                                  // is
                                                                                  // returning
                                                                                  // Big
                                                                                  // Integer
                                                                                  // where
                                                                                  // as
                                                                                  // MySQL
                                                                                  // returns
                                                                                  // Long.
            if (!runningBalanceMap.containsKey(accountId)) {
                runningBalanceMap.put(accountId, (BigDecimal) entries.get("runningBalance"));
            }
        }

        final String offlineRunningBalanceQuery = "select je.office_running_balance as runningBalance,je.account_id as accountId,je.office_id as officeId "
                + "from acc_gl_journal_entry je "
                + "inner join (select max(id) as id from acc_gl_journal_entry where entry_date < ? group by office_id,account_id,entry_date) je2 ON je2.id = je.id "
                + "inner join (select max(entry_date) as date from acc_gl_journal_entry where entry_date < ? group by office_id,account_id) je3 ON je.entry_date = je3.date "
                + "group by je.id order by je.entry_date DESC " + sqlGenerator.limit(10000, 0);

        List<Map<String, Object>> officesRunningBalanceList = jdbcTemplate.queryForList(offlineRunningBalanceQuery, // NOSONAR
                entityDate, entityDate);
        for (Map<String, Object> entries : officesRunningBalanceList) {
            Long accountId = Long.parseLong(entries.get("accountId").toString());
            Long officeId = Long.parseLong(entries.get("officeId").toString());
            Map<Long, BigDecimal> runningBalance;
            if (officesRunningBalance.containsKey(officeId)) {
                runningBalance = officesRunningBalance.get(officeId);
            } else {
                runningBalance = new HashMap<>();
                officesRunningBalance.put(officeId, runningBalance);
            }
            if (!runningBalance.containsKey(accountId)) {
                runningBalance.put(accountId, (BigDecimal) entries.get("runningBalance"));
            }
        }

        List<JournalEntryData> entryDataList = jdbcTemplate.query(entryMapper.organizationRunningBalanceSchema(), entryMapper, entityDate);
        if (entryDataList.size() > 0) {
            // run a batch update of 1000 SQL statements at a time
            final int batchUpdateSize = 1000;
            List<Object[]> params = new ArrayList<>();
            int batchIndex = 0;
            String sql = "UPDATE acc_gl_journal_entry SET is_running_balance_calculated=?, organization_running_balance=?,"
                    + "office_running_balance=?, last_modified_by=?, last_modified_on_utc=?  WHERE  id=?";
            for (int index = 0; index < entryDataList.size(); index++) {
                JournalEntryData entryData = entryDataList.get(index);
                Map<Long, BigDecimal> officeRunningBalanceMap;
                if (officesRunningBalance.containsKey(entryData.getOfficeId())) {
                    officeRunningBalanceMap = officesRunningBalance.get(entryData.getOfficeId());
                } else {
                    officeRunningBalanceMap = new HashMap<>();
                    officesRunningBalance.put(entryData.getOfficeId(), officeRunningBalanceMap);
                }
                BigDecimal officeRunningBalance = calculateRunningBalance(entryData, officeRunningBalanceMap);
                BigDecimal runningBalance = calculateRunningBalance(entryData, runningBalanceMap);

                params.add(new Object[] { Boolean.TRUE, runningBalance, officeRunningBalance,
                        platformSecurityContext.authenticatedUser().getId(), DateUtils.getOffsetDateTimeOfTenant(), entryData.getId() });
                batchIndex++;
                if (batchIndex == batchUpdateSize || index == entryDataList.size() - 1) {
                    this.jdbcTemplate.batchUpdate(sql, params);
                    // reset counter and string array
                    batchIndex = 0;
                    params.clear();
                }
            }
        }

    }

    private void updateRunningBalance(Long officeId, LocalDate entityDate) {
        Map<Long, BigDecimal> runningBalanceMap = new HashMap<>(5);

        final String offlineRunningBalanceQuery = "select je.office_running_balance as runningBalance,je.account_id as accountId from acc_gl_journal_entry je "
                + "inner join (select max(id) as id from acc_gl_journal_entry where office_id=?  and entry_date < ? group by account_id,entry_date) je2 ON je2.id = je.id "
                + "inner join (select max(entry_date) as date from acc_gl_journal_entry where office_id=? and entry_date < ? group by account_id) je3 ON je.entry_date = je3.date "
                + "group by je.id order by je.entry_date DESC " + sqlGenerator.limit(10000, 0);

        List<Map<String, Object>> list = jdbcTemplate.queryForList(offlineRunningBalanceQuery, // NOSONAR
                officeId, entityDate, officeId, entityDate);
        for (Map<String, Object> entries : list) {
            Long accountId = (Long) entries.get("accountId");
            if (!runningBalanceMap.containsKey(accountId)) {
                runningBalanceMap.put(accountId, (BigDecimal) entries.get("runningBalance"));
            }
        }
        List<JournalEntryData> entryDataList = jdbcTemplate.query(entryMapper.officeRunningBalanceSchema(), entryMapper, officeId,
                entityDate);
        List<Object[]> params = new ArrayList<>();

        String sql = "UPDATE acc_gl_journal_entry SET office_running_balance=?, last_modified_by=?, last_modified_on_utc=? WHERE id=?";
        for (JournalEntryData entryData : entryDataList) {
            BigDecimal runningBalance = calculateRunningBalance(entryData, runningBalanceMap);
            params.add(new Object[] { runningBalance, platformSecurityContext.authenticatedUser().getId(),
                    DateUtils.getOffsetDateTimeOfTenant(), entryData.getId() });
        }
        this.jdbcTemplate.batchUpdate(sql, params);
    }

    private BigDecimal calculateRunningBalance(JournalEntryData entry, Map<Long, BigDecimal> runningBalanceMap) {
        BigDecimal runningBalance = BigDecimal.ZERO;
        if (runningBalanceMap.containsKey(entry.getGlAccountId())) {
            runningBalance = runningBalanceMap.get(entry.getGlAccountId());
        }
        GLAccountType accountType = GLAccountType.fromInt(entry.getGlAccountType().getId().intValue());
        JournalEntryType entryType = JournalEntryType.fromInt(entry.getEntryType().getId().intValue());
        boolean isIncrease = false;
        switch (accountType) {
            case ASSET:
            case EXPENSE:
                if (entryType.isDebitType()) {
                    isIncrease = true;
                }
            break;
            case EQUITY:
            case INCOME:
            case LIABILITY:
                if (entryType.isCreditType()) {
                    isIncrease = true;
                }
            break;
        }
        if (isIncrease) {
            runningBalance = runningBalance.add(entry.getAmount());
        } else {
            runningBalance = runningBalance.subtract(entry.getAmount());
        }
        runningBalanceMap.put(entry.getGlAccountId(), runningBalance);
        return runningBalance;
    }

    private static final class GLJournalEntryMapper implements RowMapper<JournalEntryData> {

        public String officeRunningBalanceSchema() {
            return "select je.id as id,je.account_id as glAccountId,je.type_enum as entryType,je.amount as amount, "
                    + "glAccount.classification_enum as classification,je.office_id as officeId "
                    + "from acc_gl_journal_entry je , acc_gl_account glAccount " + "where je.account_id = glAccount.id "
                    + "and je.office_id=? and je.entry_date >= ? order by je.entry_date,je.id";
        }

        public String organizationRunningBalanceSchema() {
            return "select je.id as id,je.account_id as glAccountId," + "je.type_enum as entryType,je.amount as amount, "
                    + "glAccount.classification_enum as classification,je.office_id as officeId  "
                    + "from acc_gl_journal_entry je , acc_gl_account glAccount " + "where je.account_id = glAccount.id "
                    + "and je.entry_date >= ? order by je.entry_date,je.id";
        }

        @Override
        public JournalEntryData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final Long glAccountId = rs.getLong("glAccountId");
            final Long officeId = rs.getLong("officeId");
            final int accountTypeId = JdbcSupport.getInteger(rs, "classification");
            final EnumOptionData accountType = AccountingEnumerations.gLAccountType(accountTypeId);
            final BigDecimal amount = rs.getBigDecimal("amount");
            final int entryTypeId = JdbcSupport.getInteger(rs, "entryType");
            final EnumOptionData entryType = AccountingEnumerations.journalEntryType(entryTypeId);

            return new JournalEntryData(id, officeId, null, null, glAccountId, null, accountType, null, entryType, amount, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null);
        }
    }

}
