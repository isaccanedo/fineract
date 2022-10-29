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
package org.apache.fineract.infrastructure.core.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.time.ZonedDateTime;

/**
 * Serializer for Joda Time {@link ZonedDateTime} that returns the date as long to match previous (Jackson)
 * functionality.
 */
public class JodaDateTimeAdapter implements JsonSerializer<ZonedDateTime> {

    @Override
    @SuppressWarnings("unused")
    public JsonElement serialize(final ZonedDateTime src, final Type typeOfSrc, final JsonSerializationContext context) {
        JsonElement element = null;
        if (src != null) {
            element = new JsonPrimitive(src.toInstant().toEpochMilli());
        }

        return element;
    }
}
