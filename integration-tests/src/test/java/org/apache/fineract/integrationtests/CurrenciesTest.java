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
import java.util.ArrayList;
import java.util.Collections;
import org.apache.fineract.integrationtests.common.CurrenciesHelper;
import org.apache.fineract.integrationtests.common.CurrencyDomain;
import org.apache.fineract.integrationtests.common.Utils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({ "unused" })
public class CurrenciesTest {

    private ResponseSpecification responseSpec;
    private RequestSpecification requestSpec;

    @BeforeEach
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
    }

    @Test
    public void testCurrencyElements() {

        CurrencyDomain currency = CurrenciesHelper.getCurrencybyCode(requestSpec, responseSpec, "USD");
        CurrencyDomain usd = CurrencyDomain.create("USD", "US Dollar", 2, "$", "currency.USD", "US Dollar ($)").build();

        Assertions.assertTrue(currency.getDecimalPlaces() >= 0);
        Assertions.assertNotNull(currency.getName());
        Assertions.assertNotNull(currency.getDisplaySymbol());
        Assertions.assertNotNull(currency.getDisplayLabel());
        Assertions.assertNotNull(currency.getNameCode());

        Assertions.assertEquals(usd, currency);
    }

    @Test
    public void testUpdateCurrencySelection() {

        // Test updation
        ArrayList<String> currenciestoUpdate = new ArrayList<String>();
        currenciestoUpdate.add("KES");
        currenciestoUpdate.add("BND");
        currenciestoUpdate.add("LBP");
        currenciestoUpdate.add("GHC");
        currenciestoUpdate.add("USD");
        currenciestoUpdate.add("INR");

        ArrayList<String> currenciesOutput = CurrenciesHelper.updateSelectedCurrencies(this.requestSpec, this.responseSpec,
                currenciestoUpdate);
        Assertions.assertNotNull(currenciesOutput);

        Assertions.assertEquals(currenciestoUpdate, currenciesOutput, "Verifying Do Outputed Currencies Match after Updation");

        // Test that output matches updation
        ArrayList<CurrencyDomain> currenciesBeforeUpdate = new ArrayList<CurrencyDomain>();
        for (String e : currenciestoUpdate) {
            currenciesBeforeUpdate.add(CurrenciesHelper.getCurrencybyCode(requestSpec, responseSpec, e));
        }
        Collections.sort(currenciesBeforeUpdate);

        ArrayList<CurrencyDomain> currenciesAfterUpdate = CurrenciesHelper.getSelectedCurrencies(requestSpec, responseSpec);
        Assertions.assertNotNull(currenciesAfterUpdate);

        Assertions.assertEquals(currenciesBeforeUpdate, currenciesAfterUpdate, "Verifying Do Selected Currencies Match after Updation");
    }
}
