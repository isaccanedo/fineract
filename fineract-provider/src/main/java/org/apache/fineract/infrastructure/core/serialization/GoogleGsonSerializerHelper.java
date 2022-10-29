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
package org.apache.fineract.infrastructure.core.serialization;

import com.google.gson.ExclusionStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.apache.fineract.infrastructure.core.api.DateAdapter;
import org.apache.fineract.infrastructure.core.api.JodaDateTimeAdapter;
import org.apache.fineract.infrastructure.core.api.JodaMonthDayAdapter;
import org.apache.fineract.infrastructure.core.api.LocalDateAdapter;
import org.apache.fineract.infrastructure.core.api.LocalDateTimeAdapter;
import org.apache.fineract.infrastructure.core.api.LocalTimeAdapter;
import org.apache.fineract.infrastructure.core.api.OffsetDateTimeAdapter;
import org.apache.fineract.infrastructure.core.api.ParameterListExclusionStrategy;
import org.apache.fineract.infrastructure.core.api.ParameterListInclusionStrategy;
import org.apache.fineract.infrastructure.core.exception.UnsupportedParameterException;
import org.springframework.stereotype.Service;

/**
 * Helper class for serialization of Java objects into JSON using Google's GSON.
 */
@Service
public final class GoogleGsonSerializerHelper {

    public Gson createGsonBuilderForPartialResponseFiltering(final boolean prettyPrint, final Set<String> responseParameters) {
        final ExclusionStrategy strategy = new ParameterListInclusionStrategy(responseParameters);

        final GsonBuilder builder = new GsonBuilder().addSerializationExclusionStrategy(strategy);
        registerTypeAdapters(builder);
        if (prettyPrint) {
            builder.setPrettyPrinting();
        }
        return builder.create();
    }

    public Gson createGsonBuilderWithParameterExclusionSerializationStrategy(final Set<String> supportedParameters,
            final boolean prettyPrint, final Set<String> responseParameters) {

        final Set<String> parameterNamesToSkip = new HashSet<>();

        if (!responseParameters.isEmpty()) {
            // strip out all known support parameters from expected response to
            // see if unsupported parameters requested for response.
            final Set<String> differentParametersDetectedSet = new HashSet<>(responseParameters);
            differentParametersDetectedSet.removeAll(supportedParameters);

            if (!differentParametersDetectedSet.isEmpty()) {
                throw new UnsupportedParameterException(new ArrayList<>(differentParametersDetectedSet));
            }

            parameterNamesToSkip.addAll(supportedParameters);
            parameterNamesToSkip.removeAll(responseParameters);
        }

        final ExclusionStrategy strategy = new ParameterListExclusionStrategy(parameterNamesToSkip);

        final GsonBuilder builder = new GsonBuilder().addSerializationExclusionStrategy(strategy);
        registerTypeAdapters(builder);
        if (prettyPrint) {
            builder.setPrettyPrinting();
        }
        return builder.create();
    }

    public String serializedJsonFrom(final Gson serializer, final Object[] dataObjects) {
        return serializer.toJson(dataObjects);
    }

    public String serializedJsonFrom(final Gson serializer, final Object singleDataObject) {
        return serializer.toJson(singleDataObject);
    }

    public Gson createSimpleGson() {
        return createGsonBuilder().create();
    }

    public static GsonBuilder createGsonBuilder() {
        return createGsonBuilder(false);
    }

    public static GsonBuilder createGsonBuilder(final boolean prettyPrint) {
        final GsonBuilder builder = new GsonBuilder();
        registerTypeAdapters(builder);
        if (prettyPrint) {
            builder.setPrettyPrinting();
        }
        return builder;
    }

    public static void registerTypeAdapters(final GsonBuilder builder) {
        builder.registerTypeAdapter(java.util.Date.class, new DateAdapter());
        builder.registerTypeAdapter(LocalDate.class, new LocalDateAdapter());
        // NOTE: was missing, necessary for GSON serialization with JDK 17's restrictive module access
        builder.registerTypeAdapter(LocalTime.class, new LocalTimeAdapter());
        builder.registerTypeAdapter(ZonedDateTime.class, new JodaDateTimeAdapter());
        builder.registerTypeAdapter(MonthDay.class, new JodaMonthDayAdapter());
        builder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter());
        builder.registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter());
    }
}
