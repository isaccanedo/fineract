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

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.util.HashMap;
import org.apache.fineract.integrationtests.common.PaymentTypeDomain;
import org.apache.fineract.integrationtests.common.PaymentTypeHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PaymentTypeIntegrationTest {

    private ResponseSpecification responseSpec;
    private RequestSpecification requestSpec;

    @BeforeEach
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testPaymentType() {
        String name = PaymentTypeHelper.randomNameGenerator("P_T", 5);
        String description = PaymentTypeHelper.randomNameGenerator("PT_Desc", 15);
        Boolean isCashPayment = true;
        Integer position = 1;

        Integer paymentTypeId = PaymentTypeHelper.createPaymentType(requestSpec, responseSpec, name, description, isCashPayment, position);
        Assertions.assertNotNull(paymentTypeId);
        PaymentTypeHelper.verifyPaymentTypeCreatedOnServer(requestSpec, responseSpec, paymentTypeId);
        PaymentTypeDomain paymentTypeResponse = PaymentTypeHelper.retrieveById(requestSpec, responseSpec, paymentTypeId);
        Assertions.assertEquals(name, paymentTypeResponse.getName());
        Assertions.assertEquals(description, paymentTypeResponse.getDescription());
        Assertions.assertEquals(isCashPayment, paymentTypeResponse.getIsCashPayment());
        Assertions.assertEquals(position, paymentTypeResponse.getPosition());

        // Update Payment Type
        String newName = PaymentTypeHelper.randomNameGenerator("P_TU", 5);
        String newDescription = PaymentTypeHelper.randomNameGenerator("PTU_Desc", 15);
        Boolean isCashPaymentUpdatedValue = false;
        Integer newPosition = 2;

        HashMap request = new HashMap();
        request.put("name", newName);
        request.put("description", newDescription);
        request.put("isCashPayment", isCashPaymentUpdatedValue);
        request.put("position", newPosition);
        PaymentTypeHelper.updatePaymentType(paymentTypeId, request, requestSpec, responseSpec);
        PaymentTypeDomain paymentTypeUpdatedResponse = PaymentTypeHelper.retrieveById(requestSpec, responseSpec, paymentTypeId);
        Assertions.assertEquals(newName, paymentTypeUpdatedResponse.getName());
        Assertions.assertEquals(newDescription, paymentTypeUpdatedResponse.getDescription());
        Assertions.assertEquals(isCashPaymentUpdatedValue, paymentTypeUpdatedResponse.getIsCashPayment());
        Assertions.assertEquals(newPosition, paymentTypeUpdatedResponse.getPosition());

        // Delete
        Integer deletedPaymentTypeId = PaymentTypeHelper.deletePaymentType(paymentTypeId, requestSpec, responseSpec);
        Assertions.assertEquals(paymentTypeId, deletedPaymentTypeId);
        ResponseSpecification responseSpecification = new ResponseSpecBuilder().expectStatusCode(404).build();
        PaymentTypeHelper.retrieveById(requestSpec, responseSpecification, paymentTypeId);

    }

}
