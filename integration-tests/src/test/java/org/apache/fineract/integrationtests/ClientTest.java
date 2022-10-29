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

import static org.apache.fineract.integrationtests.client.IntegrationTest.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.fineract.client.models.GetClientClientIdAddressesResponse;
import org.apache.fineract.client.models.GlobalConfigurationPropertyData;
import org.apache.fineract.client.models.PostClientClientIdAddressesRequest;
import org.apache.fineract.client.models.PostClientClientIdAddressesResponse;
import org.apache.fineract.client.models.PostClientsAddressRequest;
import org.apache.fineract.client.models.PostClientsRequest;
import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.GlobalConfigurationHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.system.CodeHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClientTest {

    private static final SecureRandom rand = new SecureRandom();

    private ResponseSpecification responseSpec;
    private RequestSpecification requestSpec;
    private ClientHelper clientHelper;

    @BeforeEach
    public void setup() {
        Utils.initializeRESTAssured();
        requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        clientHelper = new ClientHelper(requestSpec, responseSpec);
    }

    @AfterEach
    public void tearDown() {
        GlobalConfigurationHelper.resetAllDefaultGlobalConfigurations(requestSpec, responseSpec);
        GlobalConfigurationHelper.verifyAllDefaultGlobalConfigurations(requestSpec, responseSpec);
    }

    @Test
    public void testClientStatus() {
        final Integer clientId = ClientHelper.createClient(requestSpec, responseSpec);
        ClientHelper.verifyClientCreatedOnServer(requestSpec, responseSpec, clientId);

        HashMap<String, Object> status = ClientHelper.getClientStatus(requestSpec, responseSpec, String.valueOf(clientId));
        ClientStatusChecker.verifyClientIsActive(status);

        HashMap<String, Object> clientStatusHashMap = clientHelper.closeClient(clientId);
        ClientStatusChecker.verifyClientClosed(clientStatusHashMap);

        clientStatusHashMap = clientHelper.reactivateClient(clientId);
        ClientStatusChecker.verifyClientPending(clientStatusHashMap);

        clientStatusHashMap = clientHelper.rejectClient(clientId);
        ClientStatusChecker.verifyClientRejected(clientStatusHashMap);

        clientStatusHashMap = clientHelper.activateClient(clientId);
        ClientStatusChecker.verifyClientActiavted(clientStatusHashMap);

        clientStatusHashMap = clientHelper.closeClient(clientId);
        ClientStatusChecker.verifyClientClosed(clientStatusHashMap);

        clientStatusHashMap = clientHelper.reactivateClient(clientId);
        ClientStatusChecker.verifyClientPending(clientStatusHashMap);

        clientStatusHashMap = clientHelper.withdrawClient(clientId);
        ClientStatusChecker.verifyClientWithdrawn(clientStatusHashMap);

    }

    @Test
    public void testClientAsPersonStatus() {
        final Integer clientId = ClientHelper.createClientAsPerson(requestSpec, responseSpec);
        ClientHelper.verifyClientCreatedOnServer(requestSpec, responseSpec, clientId);

        HashMap<String, Object> status = ClientHelper.getClientStatus(requestSpec, responseSpec, String.valueOf(clientId));
        ClientStatusChecker.verifyClientIsActive(status);

        HashMap<String, Object> clientStatusHashMap = clientHelper.closeClient(clientId);
        ClientStatusChecker.verifyClientClosed(clientStatusHashMap);

        clientStatusHashMap = clientHelper.reactivateClient(clientId);
        ClientStatusChecker.verifyClientPending(clientStatusHashMap);

        clientStatusHashMap = clientHelper.rejectClient(clientId);
        ClientStatusChecker.verifyClientRejected(clientStatusHashMap);

        clientStatusHashMap = clientHelper.activateClient(clientId);
        ClientStatusChecker.verifyClientActiavted(clientStatusHashMap);

        clientStatusHashMap = clientHelper.closeClient(clientId);
        ClientStatusChecker.verifyClientClosed(clientStatusHashMap);

        clientStatusHashMap = clientHelper.reactivateClient(clientId);
        ClientStatusChecker.verifyClientPending(clientStatusHashMap);

        clientStatusHashMap = clientHelper.withdrawClient(clientId);
        ClientStatusChecker.verifyClientWithdrawn(clientStatusHashMap);

    }

    @Test
    public void testClientAsEntityStatus() {
        final Integer clientId = ClientHelper.createClientAsEntity(requestSpec, responseSpec);
        ClientHelper.verifyClientCreatedOnServer(requestSpec, responseSpec, clientId);

        HashMap<String, Object> status = ClientHelper.getClientStatus(requestSpec, responseSpec, String.valueOf(clientId));
        ClientStatusChecker.verifyClientIsActive(status);

        HashMap<String, Object> clientStatusHashMap = clientHelper.closeClient(clientId);
        ClientStatusChecker.verifyClientClosed(clientStatusHashMap);

        clientStatusHashMap = clientHelper.reactivateClient(clientId);
        ClientStatusChecker.verifyClientPending(clientStatusHashMap);

        clientStatusHashMap = clientHelper.rejectClient(clientId);
        ClientStatusChecker.verifyClientRejected(clientStatusHashMap);

        clientStatusHashMap = clientHelper.activateClient(clientId);
        ClientStatusChecker.verifyClientActiavted(clientStatusHashMap);

        clientStatusHashMap = clientHelper.closeClient(clientId);
        ClientStatusChecker.verifyClientClosed(clientStatusHashMap);

        clientStatusHashMap = clientHelper.reactivateClient(clientId);
        ClientStatusChecker.verifyClientPending(clientStatusHashMap);

        clientStatusHashMap = clientHelper.withdrawClient(clientId);
        ClientStatusChecker.verifyClientWithdrawn(clientStatusHashMap);

    }

    @SuppressWarnings("unchecked")
    @Test
    @SuppressFBWarnings(value = {
            "DMI_RANDOM_USED_ONLY_ONCE" }, justification = "False positive for random object created and used only once")
    public void testPendingOnlyClientRequest() {

        // Add a few clients to the server and activate a random amount of them
        for (int i = 0; i < 15; i++) {
            final Integer clientId = ClientHelper.createClientAsEntity(requestSpec, responseSpec);
            if (rand.nextInt(10) % 2 == 0) {
                // Takes Client to pending status
                clientHelper.closeClient(clientId);
                clientHelper.reactivateClient(clientId);
            }
            // Other clients stay in Active status
        }
        List<HashMap<String, Object>> clientsRecieved = (List<HashMap<String, Object>>) clientHelper.getClientWithStatus(50, "pending");
        assertNotEquals(clientsRecieved.size(), 0);
        for (int i = 0; i < clientsRecieved.size(); i++) {
            HashMap<String, Object> clientStatus = ClientHelper.getClientStatus(requestSpec, responseSpec,
                    String.valueOf(clientsRecieved.get(i).get("id")));
            ClientStatusChecker.verifyClientPending(clientStatus);
        }

        clientsRecieved = (List<HashMap<String, Object>>) clientHelper.getClientWithStatus(50, "active");
        assertNotEquals(clientsRecieved.size(), 0);
        for (int i = 0; i < clientsRecieved.size(); i++) {
            HashMap<String, Object> clientStatus = ClientHelper.getClientStatus(requestSpec, responseSpec,
                    String.valueOf(clientsRecieved.get(i).get("id")));
            ClientStatusChecker.verifyClientIsActive(clientStatus);
        }
    }

    @Test
    public void testClientAddressCreationWorks() {
        // given
        GlobalConfigurationPropertyData addressEnabledConfig = GlobalConfigurationHelper.getGlobalConfigurationByName(requestSpec,
                responseSpec, "Enable-Address");
        Long configId = addressEnabledConfig.getId();

        GlobalConfigurationHelper.updateEnabledFlagForGlobalConfiguration(requestSpec, responseSpec, configId, true);
        GlobalConfigurationPropertyData updatedAddressEnabledConfig = GlobalConfigurationHelper.getGlobalConfigurationByName(requestSpec,
                responseSpec, "Enable-Address");
        boolean isAddressEnabled = BooleanUtils.toBoolean(updatedAddressEnabledConfig.getEnabled());
        assertThat(isAddressEnabled).isTrue();

        Integer addressTypeId = CodeHelper.createAddressTypeCodeValue(requestSpec, responseSpec,
                Utils.randomNameGenerator("Residential address", 4), 0);
        Integer countryId = CodeHelper.createCountryCodeValue(requestSpec, responseSpec, Utils.randomNameGenerator("Hungary", 4), 0);
        Integer stateId = CodeHelper.createStateCodeValue(requestSpec, responseSpec, Utils.randomNameGenerator("Budapest", 4), 0);
        String city = "Budapest";
        boolean addressIsActive = true;
        long postalCode = 1000L;

        // when
        PostClientsAddressRequest addressRequest = new PostClientsAddressRequest().postalCode(postalCode).city(city).countryId(countryId)
                .stateProvinceId(stateId).addressTypeId(addressTypeId.longValue()).isActive(addressIsActive);
        PostClientsRequest request = ClientHelper.defaultClientCreationRequest().address(List.of(addressRequest));
        final Integer clientId = ClientHelper.createClient(requestSpec, responseSpec, request);

        // then
        ClientHelper.verifyClientCreatedOnServer(requestSpec, responseSpec, clientId);
        List<GetClientClientIdAddressesResponse> clientAddresses = ClientHelper.getClientAddresses(requestSpec, responseSpec, clientId);
        GetClientClientIdAddressesResponse addressResponse = clientAddresses.get(0);
        assertThat(addressResponse.getCity()).isEqualTo(city);
        assertThat(addressResponse.getCountryId()).isEqualTo(countryId);
        assertThat(addressResponse.getStateProvinceId()).isEqualTo(stateId);
        assertThat(addressResponse.getAddressTypeId()).isEqualTo(addressTypeId);
        assertThat(addressResponse.getIsActive()).isEqualTo(addressIsActive);
        assertThat(addressResponse.getPostalCode()).isEqualTo(postalCode);
    }

    @Test
    public void testClientAddressCreationWorksAfterClientIsCreated() {
        // given
        Integer addressTypeId = CodeHelper.createAddressTypeCodeValue(requestSpec, responseSpec,
                Utils.randomNameGenerator("Residential address", 4), 0);
        Integer countryId = CodeHelper.createCountryCodeValue(requestSpec, responseSpec, Utils.randomNameGenerator("Hungary", 4), 0);
        Integer stateId = CodeHelper.createStateCodeValue(requestSpec, responseSpec, Utils.randomNameGenerator("Budapest", 4), 0);
        String city = "Budapest";
        boolean addressIsActive = true;
        long postalCode = 1000L;

        PostClientsRequest clientRequest = ClientHelper.defaultClientCreationRequest();
        final Integer clientId = ClientHelper.createClient(requestSpec, responseSpec, clientRequest);
        // when
        PostClientClientIdAddressesRequest request = new PostClientClientIdAddressesRequest().postalCode(postalCode).city(city)
                .countryId(countryId).stateProvinceId(stateId).isActive(addressIsActive);
        PostClientClientIdAddressesResponse response = ClientHelper.createClientAddress(requestSpec, responseSpec, clientId.longValue(),
                addressTypeId, request);
        // then
        assertThat(response.getResourceId()).isNotNull();
        List<GetClientClientIdAddressesResponse> clientAddresses = ClientHelper.getClientAddresses(requestSpec, responseSpec, clientId);
        GetClientClientIdAddressesResponse addressResponse = clientAddresses.get(0);
        assertThat(addressResponse.getCity()).isEqualTo(city);
        assertThat(addressResponse.getCountryId()).isEqualTo(countryId);
        assertThat(addressResponse.getStateProvinceId()).isEqualTo(stateId);
        assertThat(addressResponse.getAddressTypeId()).isEqualTo(addressTypeId);
        assertThat(addressResponse.getIsActive()).isEqualTo(addressIsActive);
        assertThat(addressResponse.getPostalCode()).isEqualTo(postalCode);
    }

}
