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
package org.apache.fineract.portfolio.loanproduct.productmix.serialization;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class ProductMixDataValidator {

    public static final String RESTRICTED_PRODUCTS = "restrictedProducts";
    /**
     * The parameters supported for this command.
     */
    private static final Set<String> SUPPORTED_PARAMETERS = new HashSet<>(List.of(RESTRICTED_PRODUCTS));
    public static final String PRODUCTMIX = "productmix";
    public static final String RESTRICTED_PRODUCT = "restrictedProduct";

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public ProductMixDataValidator(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public void validateForCreate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, SUPPORTED_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource(PRODUCTMIX);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final String[] restrictedProducts = this.fromApiJsonHelper.extractArrayNamed(RESTRICTED_PRODUCTS, element);
        baseDataValidator.reset().parameter(RESTRICTED_PRODUCTS).value(restrictedProducts).arrayNotEmpty();
        if (restrictedProducts != null) {
            validateRestrictedProducts(restrictedProducts, baseDataValidator);
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void validateRestrictedProducts(final String[] restrictedProducts, final DataValidatorBuilder baseDataValidator) {
        for (final String restrictedId : restrictedProducts) {
            baseDataValidator.reset().parameter(RESTRICTED_PRODUCT).value(restrictedId).notBlank().longGreaterThanZero();
        }
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }
    }

    public void validateForUpdate(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, SUPPORTED_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource(PRODUCTMIX);

        final JsonElement element = this.fromApiJsonHelper.parse(json);
        final String[] restrictedProducts = this.fromApiJsonHelper.extractArrayNamed(RESTRICTED_PRODUCTS, element);
        validateRestrictedProducts(restrictedProducts, baseDataValidator);

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

}
