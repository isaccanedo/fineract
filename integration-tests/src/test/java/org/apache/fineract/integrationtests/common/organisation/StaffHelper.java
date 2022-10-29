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
package org.apache.fineract.integrationtests.common.organisation;

import com.google.gson.Gson;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.fineract.integrationtests.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StaffHelper {

    private StaffHelper() {

    }

    private static final Logger LOG = LoggerFactory.getLogger(StaffHelper.class);
    private static final String TRANSFER_STAFF_URL = "/fineract-provider/api/v1/groups";

    private static final String CREATE_STAFF_URL = "/fineract-provider/api/v1/staff";

    public static final String GROUP_ID = "groupId";

    public static Integer transferStaffToGroup(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final Integer groupId, final Integer staffToTransfer, final String note) {
        final String url = TRANSFER_STAFF_URL + "/" + groupId + "?command=transferStaff&" + Utils.TENANT_IDENTIFIER;
        return Utils.performServerPost(requestSpec, responseSpec, url, transferStaffToGroupAsJSON(staffToTransfer, note), GROUP_ID);
    }

    public static String transferStaffToGroupAsJSON(final Integer staffToTransferId, final String note) {
        final HashMap<String, Object> map = new HashMap<>();
        map.put("staffId", staffToTransferId);
        map.put("note", note);
        LOG.info("map :  {}", map);
        return new Gson().toJson(map);
    }

    public static Integer createStaff(final RequestSpecification requestSpec, final ResponseSpecification responseSpec) {
        return (Integer) createStaffWithJson(requestSpec, responseSpec, createStaffAsJSON()).get("resourceId");
    }

    public static Map<String, Object> createStaffMap(final RequestSpecification requestSpec, final ResponseSpecification responseSpec) {
        return createStaffWithJson(requestSpec, responseSpec, createStaffAsJSON());
    }

    public static Map<String, Object> createStaffWithJson(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final String json) {
        final String url = CREATE_STAFF_URL + "?" + Utils.TENANT_IDENTIFIER;
        return Utils.performServerPost(requestSpec, responseSpec, url, json, "");
    }

    public static Map<String, Object> getStaff(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final Integer staffId) {
        final String url = CREATE_STAFF_URL + "/" + staffId + "?" + Utils.TENANT_IDENTIFIER;
        return Utils.performServerGet(requestSpec, responseSpec, url, "");
    }

    public static List<Map<String, Object>> getStaffList(final RequestSpecification requestSpec, final ResponseSpecification responseSpec) {
        final String url = CREATE_STAFF_URL + "?" + Utils.TENANT_IDENTIFIER;
        return Utils.performServerGet(requestSpec, responseSpec, url, "");
    }

    public static List<Map<String, Object>> getStaffListWithState(final RequestSpecification requestSpec,
            final ResponseSpecification responseSpec, final String status) {
        final String url = CREATE_STAFF_URL + "?" + Utils.TENANT_IDENTIFIER + "&status=" + status;
        return Utils.performServerGet(requestSpec, responseSpec, url, "");
    }

    public static List<Map<String, Object>> getStaffListWithLoanOfficerStatus(final RequestSpecification requestSpec,
            final ResponseSpecification responseSpec, final String loanOfficerStatus) {
        final String url = CREATE_STAFF_URL + "?" + Utils.TENANT_IDENTIFIER + "&loanOfficersOnly=" + loanOfficerStatus;
        return Utils.performServerGet(requestSpec, responseSpec, url, "");
    }

    public static Map<String, Object> updateStaff(final RequestSpecification requestSpec, final ResponseSpecification responseSpec,
            final Integer staffId, final Map<String, Object> changes) {
        final String url = CREATE_STAFF_URL + "/" + staffId + "?" + Utils.TENANT_IDENTIFIER;
        final String json = new Gson().toJson(changes);
        return Utils.performServerPut(requestSpec, responseSpec, url, json, "");
    }

    public static String createStaffAsJSON() {

        final Map<String, Object> map = getMapWithJoiningDate();

        map.put("officeId", 1);
        map.put("firstname", Utils.randomNameGenerator("michael_", 5));
        map.put("lastname", Utils.randomNameGenerator("Doe_", 4));
        map.put("isLoanOfficer", true);

        LOG.info("map :  {}", map);
        return new Gson().toJson(map);
    }

    public static Map<String, Object> getMapWithJoiningDate() {
        HashMap<String, Object> map = new HashMap<>();

        map.put("locale", "en");
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("joiningDate", "20 September 2011");

        return map;
    }

    public static String createStaffWithJSONFields(String... fields) {
        final Map<String, Object> map = getMapWithJoiningDate();
        final List<String> fieldList = Arrays.asList(fields);

        if (fieldList.contains("officeId")) {
            map.put("officeId", 1);
        }
        if (fieldList.contains("firstname")) {
            map.put("firstname", Utils.randomNameGenerator("michael_", 5));
        }
        if (fieldList.contains("lastname")) {
            map.put("lastname", Utils.randomNameGenerator("Doe_", 4));
        }
        if (fieldList.contains("isLoanOfficer")) {
            map.put("isLoanOfficer", true);
        }
        if (fieldList.contains("mobileNo")) {
            map.put("mobileNo", "+123515198");
        }
        LOG.info("map :  {}", map);
        return new Gson().toJson(map);
    }
}
