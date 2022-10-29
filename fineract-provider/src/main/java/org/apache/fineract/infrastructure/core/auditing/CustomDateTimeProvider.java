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
package org.apache.fineract.infrastructure.core.auditing;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.auditing.DateTimeProvider;

public enum CustomDateTimeProvider implements DateTimeProvider {

    INSTANCE, TENANT;

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.data.auditing.DateTimeProvider#getNow()
     */
    @NotNull
    @Override
    public Optional<TemporalAccessor> getNow() {

        switch (this) {
            case INSTANCE -> {
                return Optional.of(LocalDateTime.now(ZoneId.systemDefault()));
            }
            case TENANT -> {
                return Optional.of(OffsetDateTime.now(DateUtils.getDateTimeZoneOfTenant()));
            }
        }
        throw new UnsupportedOperationException(this + " is not supported!");
    }
}
