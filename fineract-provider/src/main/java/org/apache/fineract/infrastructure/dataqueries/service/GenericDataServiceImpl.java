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
package org.apache.fineract.infrastructure.dataqueries.service;

import static java.lang.String.format;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.infrastructure.core.service.database.DatabaseIndependentQueryService;
import org.apache.fineract.infrastructure.dataqueries.data.GenericResultsetData;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetColumnHeaderData;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetColumnValueData;
import org.apache.fineract.infrastructure.dataqueries.data.ResultsetRowData;
import org.apache.fineract.infrastructure.dataqueries.exception.DatatableNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GenericDataServiceImpl implements GenericDataService {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final DatabaseIndependentQueryService databaseIndependentQueryService;

    @Autowired
    public GenericDataServiceImpl(final RoutingDataSource dataSource, final JdbcTemplate jdbcTemplate,
            DatabaseIndependentQueryService databaseIndependentQueryService) {
        this.dataSource = dataSource;
        this.databaseIndependentQueryService = databaseIndependentQueryService;
        this.jdbcTemplate = new JdbcTemplate(this.dataSource);
    }

    @Override
    public GenericResultsetData fillGenericResultSet(final String sql) {
        try {
            final SqlRowSet rs = this.jdbcTemplate.queryForRowSet(sql); // NOSONAR

            final List<ResultsetColumnHeaderData> columnHeaders = new ArrayList<>();
            final List<ResultsetRowData> resultsetDataRows = new ArrayList<>();

            final SqlRowSetMetaData rsmd = rs.getMetaData();

            for (int i = 0; i < rsmd.getColumnCount(); i++) {

                final String columnName = rsmd.getColumnName(i + 1);
                final String columnType = rsmd.getColumnTypeName(i + 1);

                final ResultsetColumnHeaderData columnHeader = ResultsetColumnHeaderData.basic(columnName, columnType);
                columnHeaders.add(columnHeader);
            }

            while (rs.next()) {
                final List<Object> columnValues = new ArrayList<>();
                for (int i = 0; i < rsmd.getColumnCount(); i++) {
                    final String columnName = rsmd.getColumnName(i + 1);
                    final String colType = columnHeaders.get(i).getColumnType();
                    if ("DATE".equalsIgnoreCase(colType)) {
                        java.sql.Date tmpDate = (java.sql.Date) rs.getObject(columnName);
                        columnValues.add(tmpDate != null ? tmpDate.toLocalDate() : null);
                    } else if ("TIMESTAMP WITHOUT TIME ZONE".equalsIgnoreCase(colType) // PostgreSQL
                            || "DATETIME".equalsIgnoreCase(colType) || "TIMESTAMP".equalsIgnoreCase(colType)) {
                        Timestamp tmpDate = (Timestamp) rs.getObject(columnName);
                        columnValues.add(tmpDate != null ? tmpDate.toLocalDateTime() : null);
                    } else {
                        columnValues.add(rs.getObject(columnName));
                    }
                }
                final ResultsetRowData resultsetDataRow = ResultsetRowData.create(columnValues);
                resultsetDataRows.add(resultsetDataRow);
            }

            return new GenericResultsetData(columnHeaders, resultsetDataRows);
        } catch (DataAccessException e) {
            log.error("Reporting error: {}", e.getMessage());
            throw new PlatformDataIntegrityException("error.msg.report.unknown.data.integrity.issue", e.getClass().getName(), e);
        }
    }

    @Override
    public String replace(final String str, final String pattern, final String replace) {
        // JPW - this replace may / may not be any better or quicker than the
        // apache stringutils equivalent. It works, but if someone shows the
        // apache one to be about the same then this can be removed.
        int s = 0;
        int e = 0;
        final StringBuilder result = new StringBuilder();

        while ((e = str.indexOf(pattern, s)) >= 0) {
            result.append(str.substring(s, e));
            result.append(replace);
            s = e + pattern.length();
        }
        result.append(str.substring(s));
        return result.toString();
    }

    @Override
    public String wrapSQL(final String sql) {
        // wrap sql to prevent JDBC sql errors, prevent malicious sql and a
        // CachedRowSetImpl bug

        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7046875 - prevent
        // Invalid Column Name bug in sun's CachedRowSetImpl where it doesn't
        // pick up on label names, only column names
        return "select x.* from (" + sql + ") x";
    }

    @Override
    public String generateJsonFromGenericResultsetData(final GenericResultsetData grs) {

        final StringBuilder writer = new StringBuilder();

        writer.append("[");

        final List<ResultsetColumnHeaderData> columnHeaders = grs.getColumnHeaders();

        final List<ResultsetRowData> data = grs.getData();
        List<Object> row;
        Integer rSize;
        final String doubleQuote = "\"";
        final String slashDoubleQuote = "\\\"";
        String currColType;
        Object currVal;

        for (int i = 0; i < data.size(); i++) {
            writer.append("\n{");

            row = data.get(i).getRow();
            rSize = row.size();
            for (int j = 0; j < rSize; j++) {

                writer.append(doubleQuote + columnHeaders.get(j).getColumnName() + doubleQuote + ": ");
                currColType = columnHeaders.get(j).getColumnDisplayType();
                final String colType = columnHeaders.get(j).getColumnType();
                if (currColType == null && colType.equalsIgnoreCase("INT")) {
                    currColType = "INTEGER";
                }
                if (currColType == null && colType.equalsIgnoreCase("VARCHAR")) {
                    currColType = "VARCHAR";
                }
                if (currColType == null && colType.equalsIgnoreCase("DATE")) {
                    currColType = "DATE";
                }
                if (currColType == null && colType.equalsIgnoreCase("DATETIME")) {
                    currColType = "DATETIME";
                }
                currVal = row.get(j);
                if (currVal != null && currColType != null) {
                    if (currColType.equalsIgnoreCase("DECIMAL") || currColType.equalsIgnoreCase("INTEGER")
                            || currColType.equalsIgnoreCase("CODELOOKUP")) {
                        writer.append(currVal);
                    } else {
                        if (currColType.equalsIgnoreCase("DATE")) {
                            final LocalDate localDate = (LocalDate) currVal;
                            writer.append(format("[%d,%d,%d]", localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth()));
                        } else if (currColType.equalsIgnoreCase("DATETIME")) {
                            final LocalDateTime localDateTime = (LocalDateTime) currVal;
                            writer.append(format("[%d,%d,%d,%d,%d,%d,%d]", localDateTime.getYear(), localDateTime.getMonthValue(),
                                    localDateTime.getDayOfMonth(), localDateTime.getHour(), localDateTime.getMinute(),
                                    localDateTime.getSecond(), localDateTime.getNano()));
                        } else {
                            writer.append(doubleQuote + replace(String.valueOf(currVal), doubleQuote, slashDoubleQuote) + doubleQuote);
                        }
                    }
                } else {
                    writer.append("null");
                }
                if (j < (rSize - 1)) {
                    writer.append(",\n");
                }
            }

            if (i < (data.size() - 1)) {
                writer.append("},");
            } else {
                writer.append("}");
            }
        }

        writer.append("\n]");
        return writer.toString();

    }

    @Override
    public List<ResultsetColumnHeaderData> fillResultsetColumnHeaders(final String datatable) {
        final SqlRowSet columnDefinitions = getDatatableMetaData(datatable);

        final List<ResultsetColumnHeaderData> columnHeaders = new ArrayList<>();

        columnDefinitions.beforeFirst();
        while (columnDefinitions.next()) {
            final String columnName = columnDefinitions.getString(1);
            final String isNullable = columnDefinitions.getString(2);
            final String isPrimaryKey = columnDefinitions.getString(5);
            final String columnType = columnDefinitions.getString(3);
            final Long columnLength = columnDefinitions.getLong(4);

            final boolean columnNullable = "YES".equalsIgnoreCase(isNullable) || "TRUE".equalsIgnoreCase(isNullable);
            final boolean columnIsPrimaryKey = "PRI".equalsIgnoreCase(isPrimaryKey) || "TRUE".equalsIgnoreCase(isPrimaryKey);

            List<ResultsetColumnValueData> columnValues = new ArrayList<>();
            String codeName = null;
            final int codePosition = columnName.indexOf("_cd");
            if ("varchar".equalsIgnoreCase(columnType) || "int".equalsIgnoreCase(columnType) || "integer".equalsIgnoreCase(columnType)) {
                if (codePosition > 0) {
                    codeName = columnName.substring(0, codePosition);
                    columnValues = retreiveColumnValues(codeName);
                }
            }

            columnHeaders.add(ResultsetColumnHeaderData.detailed(columnName, columnType, columnLength, columnNullable, columnIsPrimaryKey,
                    columnValues, codeName));
        }

        return columnHeaders;
    }

    /*
     * Candidate for using caching there to get allowed 'column values' from code/codevalue tables
     */
    private List<ResultsetColumnValueData> retreiveColumnValues(final String codeName) {

        final List<ResultsetColumnValueData> columnValues = new ArrayList<>();

        final String sql = "select v.id, v.code_score, v.code_value from m_code m join m_code_value v on v.code_id = m.id where m.code_name = ? order by v.order_position, v.id";

        final SqlRowSet rsValues = this.jdbcTemplate.queryForRowSet(sql, new Object[] { codeName }); // NOSONAR

        rsValues.beforeFirst();
        while (rsValues.next()) {
            final Integer id = rsValues.getInt("id");
            final String codeValue = rsValues.getString("code_value");
            final Integer score = rsValues.getInt("code_score");

            columnValues.add(new ResultsetColumnValueData(id, codeValue, score));
        }

        return columnValues;
    }

    @SuppressWarnings("AvoidHidingCauseException")
    private SqlRowSet getDatatableMetaData(final String datatable) {
        try {
            return databaseIndependentQueryService.getTableColumns(dataSource, datatable);
        } catch (IllegalArgumentException e) {
            throw new DatatableNotFoundException(datatable);
        }
    }
}
