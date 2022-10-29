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
package org.apache.fineract.infrastructure.event.external.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import org.apache.fineract.avro.BulkMessageItemV1;
import org.apache.fineract.avro.loan.v1.LoanAccountDataV1;
import org.apache.fineract.infrastructure.businessdate.domain.BusinessDateType;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.event.business.domain.BulkBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.BusinessEvent;
import org.apache.fineract.infrastructure.event.external.repository.ExternalEventRepository;
import org.apache.fineract.infrastructure.event.external.repository.domain.ExternalEvent;
import org.apache.fineract.infrastructure.event.external.service.idempotency.ExternalEventIdempotencyKeyGenerator;
import org.apache.fineract.infrastructure.event.external.service.message.BulkMessageItemFactory;
import org.apache.fineract.infrastructure.event.external.service.serialization.serializer.BusinessEventSerializer;
import org.apache.fineract.infrastructure.event.external.service.serialization.serializer.BusinessEventSerializerFactory;
import org.apache.fineract.infrastructure.event.external.service.support.ByteBufferConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({ "rawtypes", "unchecked" })
class ExternalEventServiceTest {

    @Mock
    private ExternalEventRepository repository;
    @Mock
    private ExternalEventIdempotencyKeyGenerator idempotencyKeyGenerator;
    @Mock
    private BusinessEventSerializerFactory serializerFactory;
    @Mock
    private ByteBufferConverter byteBufferConverter;
    @Mock
    private BulkMessageItemFactory bulkMessageItemFactory;
    @Mock
    private EntityManager entityManager;

    private ExternalEventService underTest;

    @BeforeEach
    public void setUp() {
        underTest = new ExternalEventService(repository, idempotencyKeyGenerator, serializerFactory, byteBufferConverter,
                bulkMessageItemFactory);
        underTest.setEntityManager(entityManager);
        FineractPlatformTenant tenant = new FineractPlatformTenant(1L, "default", "Default Tenant", "Europe/Budapest", null);
        ThreadLocalContextUtil.setTenant(tenant);
        ThreadLocalContextUtil
                .setBusinessDates(new HashMap<>(Map.of(BusinessDateType.BUSINESS_DATE, LocalDate.now(ZoneId.systemDefault()))));
    }

    @Test
    public void testPostEventShouldFailWhenNullEventIsGiven() {
        // given
        // when & then
        assertThatThrownBy(() -> underTest.postEvent(null)).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testPostEventShouldFailWhenEventSerializationFails() throws IOException {
        // given
        BusinessEvent event = mock(BusinessEvent.class);
        BusinessEventSerializer eventSerializer = mock(BusinessEventSerializer.class);

        given(idempotencyKeyGenerator.generate(event)).willReturn("");
        given(serializerFactory.create(event)).willReturn(eventSerializer);
        given(eventSerializer.getSupportedSchema()).will(invocation -> LoanAccountDataV1.class);
        given(eventSerializer.serialize(event)).willThrow(IOException.class);
        // when & then
        assertThatThrownBy(() -> underTest.postEvent(event)).isExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void testPostEventShouldWorkWithRegularEvent() throws IOException {
        // given
        ArgumentCaptor<ExternalEvent> externalEventArgumentCaptor = ArgumentCaptor.forClass(ExternalEvent.class);

        String eventSchema = "org.apache.fineract.avro.loan.v1.LoanAccountDataV1";
        String eventType = "TestType";
        String idempotencyKey = "key";
        BusinessEvent event = mock(BusinessEvent.class);
        BusinessEventSerializer eventSerializer = mock(BusinessEventSerializer.class);
        byte[] data = new byte[0];

        given(event.getType()).willReturn(eventType);
        given(idempotencyKeyGenerator.generate(event)).willReturn(idempotencyKey);
        given(serializerFactory.create(event)).willReturn(eventSerializer);
        given(eventSerializer.getSupportedSchema()).will(invocation -> LoanAccountDataV1.class);
        given(eventSerializer.serialize(event)).willReturn(data);
        // when
        underTest.postEvent(event);
        // then
        verify(repository).save(externalEventArgumentCaptor.capture());
        ExternalEvent externalEvent = externalEventArgumentCaptor.getValue();
        assertThat(externalEvent.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(externalEvent.getData()).isEqualTo(data);
        assertThat(externalEvent.getType()).isEqualTo(eventType);
        assertThat(externalEvent.getSchema()).isEqualTo(eventSchema);
    }

    @Test
    public void testPostEventShouldWorkWithBulkEvent() throws IOException {
        // given
        ArgumentCaptor<ExternalEvent> externalEventArgumentCaptor = ArgumentCaptor.forClass(ExternalEvent.class);
        String eventType = "BulkBusinessEvent";
        String schema = "org.apache.fineract.avro.BulkMessagePayloadV1";

        String idempotencyKey = "key";
        BusinessEvent event = mock(BusinessEvent.class);
        BulkMessageItemV1 messageItem = new BulkMessageItemV1(1, "", "", "", ByteBuffer.wrap(new byte[0]));
        BulkBusinessEvent bulkEvent = new BulkBusinessEvent(List.of(event));
        byte[] data = new byte[0];

        given(bulkMessageItemFactory.createBulkMessageItem(1, event)).willReturn(messageItem);
        given(idempotencyKeyGenerator.generate(bulkEvent)).willReturn(idempotencyKey);
        given(byteBufferConverter.convert(any(ByteBuffer.class))).willReturn(data);
        // when
        underTest.postEvent(bulkEvent);
        // then
        verify(repository).save(externalEventArgumentCaptor.capture());
        ExternalEvent externalEvent = externalEventArgumentCaptor.getValue();
        assertThat(externalEvent.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(externalEvent.getData()).isEqualTo(data);
        assertThat(externalEvent.getType()).isEqualTo(eventType);
        assertThat(externalEvent.getSchema()).isEqualTo(schema);
    }

    @Test
    public void testPostEventShouldSaveEventCategory() throws IOException {
        // given
        ArgumentCaptor<ExternalEvent> externalEventArgumentCaptor = ArgumentCaptor.forClass(ExternalEvent.class);
        String eventSchema = "org.apache.fineract.avro.loan.v1.LoanAccountDataV1";
        String eventType = "TestType";
        String eventCategory = "TestCategory";
        String idempotencyKey = "key";
        BusinessEvent event = mock(BusinessEvent.class);
        BusinessEventSerializer eventSerializer = mock(BusinessEventSerializer.class);
        byte[] data = new byte[0];

        given(event.getType()).willReturn(eventType);
        given(event.getCategory()).willReturn(eventCategory);
        given(idempotencyKeyGenerator.generate(event)).willReturn(idempotencyKey);
        given(serializerFactory.create(event)).willReturn(eventSerializer);
        given(eventSerializer.getSupportedSchema()).will(invocation -> LoanAccountDataV1.class);
        given(eventSerializer.serialize(event)).willReturn(data);
        // when
        underTest.postEvent(event);
        // then
        verify(repository).save(externalEventArgumentCaptor.capture());
        ExternalEvent externalEvent = externalEventArgumentCaptor.getValue();
        assertThat(externalEvent.getCategory()).isEqualTo(eventCategory);

    }
}
