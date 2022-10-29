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
package org.apache.fineract.infrastructure.event.external.service.message;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.avro.MessageV1;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.event.external.repository.domain.ExternalEvent;
import org.apache.fineract.infrastructure.event.external.service.message.domain.MessageBusinessDate;
import org.apache.fineract.infrastructure.event.external.service.message.domain.MessageCategory;
import org.apache.fineract.infrastructure.event.external.service.message.domain.MessageCreatedAt;
import org.apache.fineract.infrastructure.event.external.service.message.domain.MessageData;
import org.apache.fineract.infrastructure.event.external.service.message.domain.MessageDataSchema;
import org.apache.fineract.infrastructure.event.external.service.message.domain.MessageId;
import org.apache.fineract.infrastructure.event.external.service.message.domain.MessageIdempotencyKey;
import org.apache.fineract.infrastructure.event.external.service.message.domain.MessageSource;
import org.apache.fineract.infrastructure.event.external.service.message.domain.MessageType;
import org.apache.fineract.infrastructure.event.external.service.support.ByteBufferConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageFactory implements InitializingBean {

    private final ByteBufferConverter byteBufferConverter;
    private static final String SOURCE_UUID = UUID.randomUUID().toString();

    public MessageV1 createMessage(MessageId id, MessageSource source, MessageType type, MessageCategory category,
            MessageCreatedAt createdAt, MessageBusinessDate businessDate, MessageIdempotencyKey idempotencyKey,
            MessageDataSchema dataSchema, MessageData data) {
        MessageV1 result = new MessageV1();
        result.setId(id.getId());
        result.setSource(source.getSource());
        result.setType(type.getType());
        result.setCategory(category.getCategory());
        result.setCreatedAt(getMessageCreatedAt(createdAt.getCreatedAt()));
        result.setBusinessDate(getMessageBusinessDate(businessDate.getBusinessDate()));
        result.setTenantId(getTenantId());
        result.setIdempotencyKey(idempotencyKey.getIdempotencyKey());
        result.setDataschema(dataSchema.getDataSchema());
        result.setData(data.getData());
        return result;
    }

    public MessageV1 createMessage(ExternalEvent event) {
        MessageId id = new MessageId(event.getId().intValue());
        MessageSource source = new MessageSource(SOURCE_UUID);
        MessageType type = new MessageType(event.getType());
        MessageCategory category = new MessageCategory(event.getCategory());
        MessageCreatedAt createdAt = new MessageCreatedAt(event.getCreatedAt());
        MessageBusinessDate businessDate = new MessageBusinessDate(event.getBusinessDate());
        MessageIdempotencyKey idempotencyKey = new MessageIdempotencyKey(event.getIdempotencyKey());
        MessageDataSchema dataSchema = new MessageDataSchema(event.getSchema());
        MessageData data = new MessageData(byteBufferConverter.convert(event.getData()));
        return createMessage(id, source, type, category, createdAt, businessDate, idempotencyKey, dataSchema, data);
    }

    private String getTenantId() {
        return ThreadLocalContextUtil.getTenant().getTenantIdentifier();
    }

    private String getMessageCreatedAt(OffsetDateTime createdAt) {
        return createdAt.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private String getMessageBusinessDate(LocalDate businessDate) {
        return businessDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Message source set to {}", SOURCE_UUID);
    }
}
