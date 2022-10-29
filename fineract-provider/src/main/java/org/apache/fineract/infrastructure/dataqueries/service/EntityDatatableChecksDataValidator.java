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

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class EntityDatatableChecksDataValidator {

    public static final String ENTITY = "entity";
    public static final String DATATABLE_NAME = "datatableName";
    public static final String STATUS = "status";
    public static final String SYSTEM_DEFINED = "systemDefined";
    public static final String PRODUCT_ID = "productId";
    public static final String ENTITY_DATATABLE_CHECKS = "entityDatatableChecks";
    /**
     * The parameters supported for this command.
     */
    private static final Set<String> SUPPORTED_PARAMETERS = new HashSet<>(
            Arrays.asList(ENTITY, DATATABLE_NAME, STATUS, SYSTEM_DEFINED, PRODUCT_ID));
    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public EntityDatatableChecksDataValidator(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public void validateForCreate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {

        }.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, SUPPORTED_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource(ENTITY_DATATABLE_CHECKS);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final String entity = this.fromApiJsonHelper.extractStringNamed(ENTITY, element);
        baseDataValidator.reset().parameter(ENTITY).value(entity).notBlank().isOneOfTheseStringValues(EntityTables.getEntitiesList());

        final Integer status = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(STATUS, element);
        final Object[] entityTablesStatuses = EntityTables.getStatus(entity).toArray();

        baseDataValidator.reset().parameter(STATUS).value(status).isOneOfTheseValues(entityTablesStatuses);

        final String datatableName = this.fromApiJsonHelper.extractStringNamed(DATATABLE_NAME, element);
        baseDataValidator.reset().parameter(DATATABLE_NAME).value(datatableName).notBlank();

        if (this.fromApiJsonHelper.parameterExists(SYSTEM_DEFINED, element)) {
            final String systemDefined = this.fromApiJsonHelper.extractStringNamed(SYSTEM_DEFINED, element);
            baseDataValidator.reset().parameter(SYSTEM_DEFINED).value(systemDefined).validateForBooleanValue();
        }

        if (this.fromApiJsonHelper.parameterExists(PRODUCT_ID, element)) {
            final long productId = this.fromApiJsonHelper.extractLongNamed(PRODUCT_ID, element);
            baseDataValidator.reset().parameter(PRODUCT_ID).value(productId).integerZeroOrGreater();
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
