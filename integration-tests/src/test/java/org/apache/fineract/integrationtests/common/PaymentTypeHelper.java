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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.fineract.client.models.GetPaymentTypesResponse;

@SuppressWarnings({ "rawtypes", "unchecked" })
public final class PaymentTypeHelper {

    private PaymentTypeHelper() {

    }

    private static final String PAYMENTTYPE_URL = "/fineract-provider/api/v1/paymenttypes";
    private static final String CREATE_PAYMENTTYPE_URL = PAYMENTTYPE_URL + "?" + Utils.TENANT_IDENTIFIER;

    public static ArrayList<GetPaymentTypesResponse> getSystemPaymentType(final RequestSpecification requestSpec,
            final ResponseSpecification responseSpec) {
        String response = Utils.performServerGet(requestSpec, responseSpec,
                PAYMENTTYPE_URL + "?onlyWithCode=true&" + Utils.TENANT_IDENTIFIER);
        Type paymentTypeList = new TypeToken<ArrayList<GetPaymentTypesResponse>>() {}.getType();
        return new Gson().fromJson(response, paymentTypeList);
    }

    public static Integer createPaymentType(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final String name, final String description, final Boolean isCashPayment, final Integer position) {
        // system.out.println("---------------------------------CREATING A
        // PAYMENT
        // TYPE---------------------------------------------");
        return Utils.performServerPost(requestSpec, responseSpec, CREATE_PAYMENTTYPE_URL,
                getJsonToCreatePaymentType(name, description, isCashPayment, position), "resourceId");
    }

    public static String getJsonToCreatePaymentType(final String name, final String description, final Boolean isCashPayment,
            final Integer position) {
        HashMap hm = new HashMap();
        hm.put("name", name);
        if (description != null) {
            hm.put("description", description);
        }
        hm.put("isCashPayment", isCashPayment);
        if (position != null) {
            hm.put("position", position);
        }

        // system.out.println("------------------------CREATING PAYMENT
        // TYPE-------------------------" + hm);
        return new Gson().toJson(hm);
    }

    public static void verifyPaymentTypeCreatedOnServer(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final Integer generatedPaymentTypeID) {
        // system.out.println("------------------------------CHECK PAYMENT
        // DETAILS------------------------------------\n");
        final String GET_PAYMENTTYPE_URL = PAYMENTTYPE_URL + "/" + generatedPaymentTypeID + "?" + Utils.TENANT_IDENTIFIER;
        final Integer responsePaymentTypeID = Utils.performServerGet(requestSpec, responseSpec, GET_PAYMENTTYPE_URL, "id");
        assertEquals(generatedPaymentTypeID, responsePaymentTypeID, "ERROR IN CREATING THE PAYMENT TYPE");
    }

    public static PaymentTypeDomain retrieveById(RequestSpecification requestSpec, ResponseSpecification responseSpec,
            final Integer paymentTypeId) {
        final String GET_PAYMENTTYPE_URL = PAYMENTTYPE_URL + "/" + paymentTypeId + "?" + Utils.TENANT_IDENTIFIER;
        // system.out.println("---------------------------------GET PAYMENT
        // TYPE---------------------------------------------");
        Object get = Utils.performServerGet(requestSpec, responseSpec, GET_PAYMENTTYPE_URL, "");
        final String jsonData = new Gson().toJson(get);
        return new Gson().fromJson(jsonData, new TypeToken<PaymentTypeDomain>() {}.getType());

    }

    public static HashMap<String, String> updatePaymentType(final int id, HashMap request, final RequestSpecification requestSpec,
            final ResponseSpecification responseSpec) {
        final String UPDATE_PAYMENTTYPE_URL = PAYMENTTYPE_URL + "/" + id + "?" + Utils.TENANT_IDENTIFIER;
        // system.out.println("---------------------------------UPDATE PAYMENT
        // TYPE " +
        // id + "---------------------------------------------");
        HashMap<String, String> hash = Utils.performServerPut(requestSpec, responseSpec, UPDATE_PAYMENTTYPE_URL, new Gson().toJson(request),
                "changes");
        return hash;
    }

    public static Integer deletePaymentType(final int id, final RequestSpecification requestSpec,
            final ResponseSpecification responseSpec) {
        final String DELETE_PAYMENTTYPE_URL = PAYMENTTYPE_URL + "/" + id + "?" + Utils.TENANT_IDENTIFIER;
        // system.out.println("---------------------------------DELETING PAYMENT
        // TYPE "
        // + id + "--------------------------------------------");
        return Utils.performServerDelete(requestSpec, responseSpec, DELETE_PAYMENTTYPE_URL, "resourceId");
    }

    public static String randomNameGenerator(final String prefix, final int lenOfRandomSuffix) {
        return Utils.randomStringGenerator(prefix, lenOfRandomSuffix);
    }

}
