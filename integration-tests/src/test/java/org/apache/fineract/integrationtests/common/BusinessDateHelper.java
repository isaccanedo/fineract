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
package org.apache.fineract.integrationtests.common;

import com.google.gson.Gson;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.time.LocalDate;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.businessdate.domain.BusinessDateType;

@Slf4j
public final class BusinessDateHelper {

    private BusinessDateHelper() {}

    public static HashMap updateBusinessDate(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final BusinessDateType type, final LocalDate date) {
        final String BUSINESS_DATE_API = "/fineract-provider/api/v1/businessdate?" + Utils.TENANT_IDENTIFIER;
        log.info("------------------UPDATE BUSINESS DATE----------------------");
        log.info("------------------Type: {}, date: {}----------------------", type, date);
        return Utils.performServerPost(requestSpec, responseSpec, BUSINESS_DATE_API, buildBusinessDateRequest(type, date), "changes");
    }

    private static String buildBusinessDateRequest(BusinessDateType type, LocalDate date) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("type", type.name());
        map.put("date", Utils.dateFormatter.format(date));
        map.put("dateFormat", Utils.DATE_FORMAT);
        map.put("locale", "en");
        log.info("map :  {}", map);
        return new Gson().toJson(map);
    }

}
