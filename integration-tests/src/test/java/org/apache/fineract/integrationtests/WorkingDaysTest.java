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
package org.apache.fineract.integrationtests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.util.HashMap;
import java.util.List;
import org.apache.fineract.integrationtests.common.CommonConstants;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.WorkingDaysHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class WorkingDaysTest {

    private ResponseSpecification responseSpec;
    private RequestSpecification requestSpec;
    private ResponseSpecification generalResponseSpec;

    @BeforeEach
    public void setUp() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        this.generalResponseSpec = new ResponseSpecBuilder().build();

    }

    @Test
    public void updateWorkingDays() {
        HashMap response = (HashMap) WorkingDaysHelper.updateWorkingDays(requestSpec, responseSpec);
        Assertions.assertNotNull(response.get("resourceId"));
    }

    @Test
    public void updateWorkingDaysWithWrongRecurrencePattern() {
        final List<HashMap> error = (List) WorkingDaysHelper.updateWorkingDaysWithWrongRecurrence(requestSpec, generalResponseSpec,
                CommonConstants.RESPONSE_ERROR);
        assertEquals("error.msg.recurring.rule.parsing.error", error.get(0).get("userMessageGlobalisationCode"),
                "Verify wrong recurrence pattern error");
    }

}
