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
package org.apache.fineract.portfolio.rate.serialization;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
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
import org.apache.fineract.portfolio.rate.api.RateApiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RateDefinitionCommandFromApiJsonDeserializer {

    /**
     * The parameters supported for this command.
     */
    private static final Set<String> SUPPORTED_PARAMETERS = new HashSet<>(
            Arrays.asList("id", "name", "percentage", "productApply", "active", "approveUser", "locale"));

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public RateDefinitionCommandFromApiJsonDeserializer(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public void validateForCreate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();

        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, SUPPORTED_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource(RateApiConstants.rateName);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final String name = this.fromApiJsonHelper.extractStringNamed(RateApiConstants.rateName, element);
        baseDataValidator.reset().parameter(RateApiConstants.rateName).value(name).notBlank().notExceedingLengthOf(250);

        final BigDecimal percentage = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(RateApiConstants.ratePercentage, element);
        baseDataValidator.reset().parameter(RateApiConstants.ratePercentage).value(percentage).notBlank();

        final String productApply = this.fromApiJsonHelper.extractStringNamed(RateApiConstants.rateProductApply, element);
        baseDataValidator.reset().parameter(RateApiConstants.rateProductApply).value(productApply).notBlank().notExceedingLengthOf(100);

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateForUpdate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, SUPPORTED_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource(RateApiConstants.rate);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        if (this.fromApiJsonHelper.parameterExists(RateApiConstants.rateName, element)) {
            final String name = this.fromApiJsonHelper.extractStringNamed(RateApiConstants.rateName, element);
            baseDataValidator.reset().parameter(RateApiConstants.rateName).value(name).notBlank().notExceedingLengthOf(250);
        }

        if (this.fromApiJsonHelper.parameterExists(RateApiConstants.ratePercentage, element)) {
            final BigDecimal percentage = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(RateApiConstants.ratePercentage, element);
            baseDataValidator.reset().parameter(RateApiConstants.ratePercentage).value(percentage).notBlank();
        }

        if (this.fromApiJsonHelper.parameterExists(RateApiConstants.rateProductApply, element)) {
            final String productApply = this.fromApiJsonHelper.extractStringNamed(RateApiConstants.rateProductApply, element);
            baseDataValidator.reset().parameter(RateApiConstants.rateProductApply).value(productApply).notBlank();
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

}
