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

import com.google.gson.Gson;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.fineract.integrationtests.common.CenterDomain;
import org.apache.fineract.integrationtests.common.CenterHelper;
import org.apache.fineract.integrationtests.common.GroupHelper;
import org.apache.fineract.integrationtests.common.OfficeHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.organisation.StaffHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CenterIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(CenterIntegrationTest.class);
    private RequestSpecification requestSpec;
    private ResponseSpecification responseSpec;

    @BeforeEach
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
    }

    @Test
    public void testBasicCenterCreation() {
        int officeId = new OfficeHelper(requestSpec, responseSpec).createOffice("01 July 2007");

        String name = "TestBasicCreation" + new Timestamp(new java.util.Date().getTime());
        int resourceId = CenterHelper.createCenter(name, officeId, requestSpec, responseSpec);
        CenterDomain center = CenterHelper.retrieveByID(resourceId, requestSpec, responseSpec);

        Assertions.assertNotNull(center);
        Assertions.assertTrue(center.getName().equals(name));
        Assertions.assertTrue(center.getOfficeId() == officeId);
        Assertions.assertTrue(center.isActive() == false);

        // Test retrieval by listing all centers
        int id = CenterHelper.listCenters(requestSpec, responseSpec).get(0).getId();
        Assertions.assertTrue(id > 0);

        CenterDomain retrievedCenter = CenterHelper.retrieveByID(id, requestSpec, responseSpec);
        Assertions.assertNotNull(retrievedCenter);
        Assertions.assertNotNull(retrievedCenter.getName());
        Assertions.assertNotNull(retrievedCenter.getHierarchy());
        Assertions.assertNotNull(retrievedCenter.getOfficeName());

    }

    @Test
    public void testFullCenterCreation() {

        int officeId = new OfficeHelper(requestSpec, responseSpec).createOffice("01 July 2007");
        String name = "TestFullCreation" + new Timestamp(new java.util.Date().getTime());
        String externalId = Utils.randomStringGenerator("ID_", 7, "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        int staffId = StaffHelper.createStaff(requestSpec, responseSpec);
        int[] groupMembers = generateGroupMembers(3, officeId);
        int resourceId = CenterHelper.createCenter(name, officeId, externalId, staffId, groupMembers, requestSpec, responseSpec);
        CenterDomain center = CenterHelper.retrieveByID(resourceId, requestSpec, responseSpec);

        Assertions.assertNotNull(center);
        Assertions.assertTrue(center.getName().equals(name));
        Assertions.assertTrue(center.getOfficeId() == officeId);
        Assertions.assertTrue(center.getExternalId().equals(externalId));
        Assertions.assertTrue(center.getStaffId() == staffId);
        Assertions.assertTrue(center.isActive() == false);
        Assertions.assertArrayEquals(center.getGroupMembers(), groupMembers);
    }

    @Test
    public void testListCenters() {
        ArrayList<CenterDomain> paginatedList = CenterHelper.paginatedListCenters(requestSpec, responseSpec);
        ArrayList<CenterDomain> list = CenterHelper.listCenters(requestSpec, responseSpec);

        Assertions.assertNotNull(paginatedList);
        Assertions.assertNotNull(list);
        Assertions.assertTrue(
                Arrays.equals(paginatedList.toArray(new CenterDomain[paginatedList.size()]), list.toArray(new CenterDomain[list.size()])));
    }

    @Test
    public void testVoidCenterRetrieval() {
        ArrayList<CenterDomain> arr = CenterHelper.listCentersOrdered(requestSpec, responseSpec);
        int id = arr.get(arr.size() - 1).getId() + 1;
        ResponseSpecification responseSpec = new ResponseSpecBuilder().expectStatusCode(404).build();
        CenterDomain center = CenterHelper.retrieveByID(id, requestSpec, responseSpec);
        Assertions.assertNotNull(center);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testCenterUpdate() {
        int officeId = new OfficeHelper(requestSpec, responseSpec).createOffice("01 July 2007");
        String name = "TestFullCreation" + new Timestamp(new java.util.Date().getTime());
        String externalId = Utils.randomStringGenerator("ID_", 7, "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        int staffId = StaffHelper.createStaff(requestSpec, responseSpec);
        int[] groupMembers = generateGroupMembers(3, officeId);
        int resourceId = CenterHelper.createCenter(name, officeId, externalId, staffId, groupMembers, requestSpec, responseSpec);

        String newName = "TestCenterUpdateNew" + new Timestamp(new java.util.Date().getTime());
        String newExternalId = Utils.randomStringGenerator("newID_", 7, "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        int newStaffId = StaffHelper.createStaff(requestSpec, responseSpec);
        int[] associateGroupMembers = generateGroupMembers(2, officeId);

        int[] associateResponse = CenterHelper.associateGroups(resourceId, associateGroupMembers, requestSpec, responseSpec);
        Arrays.sort(associateResponse);
        Arrays.sort(associateGroupMembers);
        Assertions.assertArrayEquals(associateResponse, associateGroupMembers);

        int[] newGroupMembers = new int[5];
        for (int i = 0; i < 5; i++) {
            if (i < 3) {
                newGroupMembers[i] = groupMembers[i];
            } else {
                newGroupMembers[i] = associateGroupMembers[i % 3];
            }
        }

        HashMap request = new HashMap();
        request.put("name", newName);
        request.put("externalId", newExternalId);
        request.put("staffId", newStaffId);
        HashMap response = CenterHelper.updateCenter(resourceId, request, requestSpec, responseSpec);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(newName, response.get("name"));
        Assertions.assertEquals(newExternalId, response.get("externalId"));
        Assertions.assertEquals(Integer.valueOf(newStaffId), Integer.valueOf(response.get("staffId").toString()));

        CenterDomain center = CenterHelper.retrieveByID(resourceId, requestSpec, responseSpec);
        Assertions.assertNotNull(center);
        Assertions.assertEquals(newName, center.getName());
        Assertions.assertEquals(newExternalId, center.getExternalId());
        Assertions.assertEquals((Integer) newStaffId, center.getStaffId());
        Assertions.assertArrayEquals(newGroupMembers, center.getGroupMembers());
    }

    @Test
    public void testCenterDeletion() {
        int officeId = new OfficeHelper(requestSpec, responseSpec).createOffice("01 July 2007");
        String name = "TestBasicCreation" + new Timestamp(new java.util.Date().getTime());
        int resourceId = CenterHelper.createCenter(name, officeId, requestSpec, responseSpec);

        CenterHelper.deleteCenter(resourceId, requestSpec, responseSpec);
        ResponseSpecification responseSpec = new ResponseSpecBuilder().expectStatusCode(404).build();
        CenterDomain center = CenterHelper.retrieveByID(resourceId, requestSpec, responseSpec);
        Assertions.assertNotNull(center);
    }

    private int[] generateGroupMembers(int size, int officeId) {
        int[] groupMembers = new int[size];
        for (int i = 0; i < groupMembers.length; i++) {
            final HashMap<String, String> map = new HashMap<>();
            map.put("officeId", "" + officeId);
            map.put("name", Utils.randomStringGenerator("Group_Name_", 5));
            map.put("externalId", Utils.randomStringGenerator("ID_", 7, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"));
            map.put("dateFormat", "dd MMMM yyyy");
            map.put("locale", "en");
            map.put("active", "true");
            map.put("activationDate", "04 March 2011");

            groupMembers[i] = Utils.performServerPost(requestSpec, responseSpec,
                    "/fineract-provider/api/v1/groups?" + Utils.TENANT_IDENTIFIER, new Gson().toJson(map), "groupId");
        }
        return groupMembers;
    }

    @Test
    public void testStaffAssignmentDuringCenterCreation() {

        final Integer staffId = StaffHelper.createStaff(this.requestSpec, this.responseSpec);
        LOG.info("--------------creating first staff with id------------- {}", staffId);
        Assertions.assertNotNull(staffId);

        final int centerWithStaffId = CenterHelper.createCenterWithStaffId(this.requestSpec, this.responseSpec, staffId);
        final CenterDomain center = CenterHelper.retrieveByID(centerWithStaffId, requestSpec, responseSpec);
        Assertions.assertNotNull(center);
        Assertions.assertTrue(center.getId() == centerWithStaffId);
        Assertions.assertTrue(center.getStaffId().intValue() == staffId);
        Assertions.assertTrue(center.isActive() == true);
    }

    @Test
    public void testAssignStaffToCenter() {
        final Integer staffId = StaffHelper.createStaff(this.requestSpec, this.responseSpec);
        LOG.info("--------------creating first staff with id------------- {}", staffId);
        Assertions.assertNotNull(staffId);

        final Integer groupID = CenterHelper.createCenter(this.requestSpec, this.responseSpec);
        CenterHelper.verifyCenterCreatedOnServer(this.requestSpec, this.responseSpec, groupID);

        final HashMap assignStaffToCenterResponseMap = (HashMap) CenterHelper.assignStaff(this.requestSpec, this.responseSpec,
                groupID.toString(), staffId.longValue());
        assertEquals(assignStaffToCenterResponseMap.get("staffId"), staffId, "Verify assigned staff id is the same as id sent");

        final CenterDomain center = CenterHelper.retrieveByID(groupID, requestSpec, responseSpec);
        Assertions.assertNotNull(center);
        Assertions.assertTrue(center.getId().intValue() == groupID);
        Assertions.assertTrue(center.getStaffId().intValue() == staffId);

    }

    @Test
    public void testUnassignStaffToCenter() {
        final Integer staffId = StaffHelper.createStaff(this.requestSpec, this.responseSpec);
        LOG.info("--------------creating first staff with id------------- {}", staffId);
        Assertions.assertNotNull(staffId);

        final Integer groupID = CenterHelper.createCenter(this.requestSpec, this.responseSpec);
        CenterHelper.verifyCenterCreatedOnServer(this.requestSpec, this.responseSpec, groupID);

        final HashMap assignStaffToCenterResponseMap = (HashMap) CenterHelper.assignStaff(this.requestSpec, this.responseSpec,
                groupID.toString(), staffId.longValue());
        assertEquals(assignStaffToCenterResponseMap.get("staffId"), staffId, "Verify assigned staff id is the same as id sent");
        final CenterDomain centerWithStaffAssigned = CenterHelper.retrieveByID(groupID, requestSpec, responseSpec);
        Assertions.assertNotNull(centerWithStaffAssigned);
        Assertions.assertTrue(centerWithStaffAssigned.getId().intValue() == groupID);
        Assertions.assertTrue(centerWithStaffAssigned.getStaffId().intValue() == staffId);

        final HashMap unassignStaffToCenterResponseMap = (HashMap) CenterHelper.unassignStaff(this.requestSpec, this.responseSpec,
                groupID.toString(), staffId.longValue());
        assertEquals(unassignStaffToCenterResponseMap.get("staffId"), null, "Verify staffId is null after unassigning ");
        final CenterDomain centerWithStaffUnssigned = CenterHelper.retrieveByID(groupID, requestSpec, responseSpec);
        Assertions.assertNotNull(centerWithStaffUnssigned);
        Assertions.assertTrue(centerWithStaffUnssigned.getId().intValue() == groupID);
        Assertions.assertTrue(centerWithStaffUnssigned.getStaffId() == null);

    }

    @Test
    public void testCentersOrphanGroups() {

        int officeId = new OfficeHelper(requestSpec, responseSpec).createOffice("01 July 2007");

        String name = "TestBasicCreation" + new Timestamp(new java.util.Date().getTime());
        int resourceId = CenterHelper.createCenter(name, officeId, requestSpec, responseSpec);
        CenterDomain center = CenterHelper.retrieveByID(resourceId, requestSpec, responseSpec);

        Assertions.assertNotNull(center);

        int id = CenterHelper.listCenters(requestSpec, responseSpec).get(0).getId();
        Assertions.assertTrue(id > 0);

        CenterDomain retrievedCenter = CenterHelper.retrieveByID(id, requestSpec, responseSpec);
        Assertions.assertNotNull(retrievedCenter);
        Assertions.assertNotNull(retrievedCenter.getName());
        Assertions.assertNotNull(retrievedCenter.getHierarchy());
        Assertions.assertNotNull(retrievedCenter.getOfficeName());

        int[] groupMembers = generateGroupMembers(2, officeId);
        CenterHelper.associateGroups(resourceId, groupMembers, requestSpec, responseSpec);
        GroupHelper.verifyOrphanGroupDetails(requestSpec, responseSpec, officeId);
    }
}
