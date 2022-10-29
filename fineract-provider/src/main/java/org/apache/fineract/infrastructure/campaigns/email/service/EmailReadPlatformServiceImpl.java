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
package org.apache.fineract.infrastructure.campaigns.email.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.campaigns.email.data.EmailData;
import org.apache.fineract.infrastructure.campaigns.email.domain.EmailMessageEnumerations;
import org.apache.fineract.infrastructure.campaigns.email.domain.EmailMessageStatusType;
import org.apache.fineract.infrastructure.campaigns.email.exception.EmailNotFoundException;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailReadPlatformServiceImpl implements EmailReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final EmailMapper emailRowMapper = new EmailMapper();
    private final PaginationHelper paginationHelper;

    private static final class EmailMapper implements RowMapper<EmailData> {

        final String schema;

        EmailMapper() {
            final StringBuilder sql = new StringBuilder(300);
            sql.append(" emo.id as id, ");
            sql.append("emo.group_id as groupId, ");
            sql.append("emo.client_id as clientId, ");
            sql.append("emo.staff_id as staffId, ");
            sql.append("emo.campaign_name as campaignName, ");
            sql.append("emo.status_enum as statusId, ");
            sql.append("emo.email_address as emailAddress, ");
            sql.append("emo.submittedon_date as sentDate, ");
            sql.append("emo.email_subject as emailSubject, ");
            sql.append("emo.message as message, ");
            sql.append("emo.error_message as errorMessage ");
            sql.append("from " + tableName() + " emo");

            this.schema = sql.toString();
        }

        public String schema() {
            return this.schema;
        }

        public String tableName() {
            return "scheduled_email_messages_outbound";
        }

        @Override
        public EmailData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = JdbcSupport.getLong(rs, "id");
            final Long groupId = JdbcSupport.getLong(rs, "groupId");
            final Long clientId = JdbcSupport.getLong(rs, "clientId");
            final Long staffId = JdbcSupport.getLong(rs, "staffId");

            final String emailAddress = rs.getString("emailAddress");
            final String emailSubject = rs.getString("emailSubject");
            final String message = rs.getString("message");
            final String campaignName = rs.getString("campaignName");

            final Integer statusId = JdbcSupport.getInteger(rs, "statusId");
            final LocalDate sentDate = JdbcSupport.getLocalDate(rs, "sentDate");
            final String errorMessage = rs.getString("errorMessage");

            final EnumOptionData status = EmailMessageEnumerations.status(statusId);

            return EmailData.instance(id, groupId, clientId, staffId, status, emailAddress, emailSubject, message, null, null, null, null,
                    null, campaignName, sentDate, errorMessage);
        }
    }

    @Override
    public Collection<EmailData> retrieveAll() {

        final String sql = "select " + this.emailRowMapper.schema();

        return this.jdbcTemplate.query(sql, this.emailRowMapper); // NOSONAR
    }

    @Override
    public EmailData retrieveOne(final Long resourceId) {
        try {
            final String sql = "select " + this.emailRowMapper.schema() + " where emo.id = ?";

            return this.jdbcTemplate.queryForObject(sql, this.emailRowMapper, new Object[] { resourceId }); // NOSONAR
        } catch (final EmptyResultDataAccessException e) {
            throw new EmailNotFoundException(resourceId, e);
        }
    }

    @Override
    public Collection<EmailData> retrieveAllPending(final SearchParameters searchParameters) {
        final String sqlPlusLimit = (searchParameters.getLimit() > 0) ? " " + sqlGenerator.limit(searchParameters.getLimit()) : "";
        final String sql = "select " + this.emailRowMapper.schema() + " where emo.status_enum =? " + sqlPlusLimit;

        return this.jdbcTemplate.query(sql, this.emailRowMapper, EmailMessageStatusType.PENDING.getValue()); // NOSONAR
    }

    @Override
    public Collection<EmailData> retrieveAllSent(final SearchParameters searchParameters) {
        final String sqlPlusLimit = (searchParameters.getLimit() > 0) ? " " + sqlGenerator.limit(searchParameters.getLimit()) : "";
        final String sql = "select " + this.emailRowMapper.schema() + " where emo.status_enum = ?" + sqlPlusLimit;

        return this.jdbcTemplate.query(sql, this.emailRowMapper, EmailMessageStatusType.SENT.getValue()); // NOSONAR
    }

    @Override
    public List<Long> retrieveExternalIdsOfAllSent(final Integer limit) {
        final String sqlPlusLimit = (limit > 0) ? " " + sqlGenerator.limit(limit) : "";
        final String sql = "select external_id from " + this.emailRowMapper.tableName() + " where status_enum =? " + sqlPlusLimit;

        return this.jdbcTemplate.queryForList(sql, Long.class, EmailMessageStatusType.SENT.getValue()); // NOSONAR
    }

    @Override
    public Collection<EmailData> retrieveAllDelivered(final Integer limit) {
        final String sqlPlusLimit = (limit > 0) ? " " + sqlGenerator.limit(limit) : "";
        final String sql = "select " + this.emailRowMapper.schema() + " where emo.status_enum = ?" + sqlPlusLimit;

        return this.jdbcTemplate.query(sql, this.emailRowMapper, EmailMessageStatusType.DELIVERED.getValue()); // NOSONAR
    }

    @Override
    public Collection<EmailData> retrieveAllFailed(final SearchParameters searchParameters) {
        final String sqlPlusLimit = (searchParameters.getLimit() > 0) ? " " + sqlGenerator.limit(searchParameters.getLimit()) : "";
        final String sql = "select " + this.emailRowMapper.schema() + " where emo.status_enum = ?" + sqlPlusLimit;

        return this.jdbcTemplate.query(sql, this.emailRowMapper, EmailMessageStatusType.FAILED.getValue()); // NOSONAR
    }

    @Override
    public Page<EmailData> retrieveEmailByStatus(final Integer limit, final Integer status, final LocalDate dateFrom,
            final LocalDate dateTo) {
        final StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append("select " + sqlGenerator.calcFoundRows() + " ");
        sqlBuilder.append(this.emailRowMapper.schema());
        if (status != null) {
            sqlBuilder.append(" where emo.status_enum= ? ");
        }
        String fromDateString = null;
        String toDateString = null;
        if (dateFrom != null && dateTo != null) {
            fromDateString = DateUtils.DEFAULT_DATE_FORMATTER.format(dateFrom);
            toDateString = DateUtils.DEFAULT_DATE_FORMATTER.format(dateTo);
            sqlBuilder.append(" and emo.submittedon_date >= ? and emo.submittedon_date <= ? ");
        }
        final String sqlPlusLimit = (limit > 0) ? " " + sqlGenerator.limit(limit) : "";
        if (!sqlPlusLimit.isEmpty()) {
            sqlBuilder.append(sqlPlusLimit);
        }
        return this.paginationHelper.fetchPage(this.jdbcTemplate, sqlBuilder.toString(),
                new Object[] { status, fromDateString, toDateString }, this.emailRowMapper);
    }
}
