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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.fineract.client.models.GetOfficesResponse;
import org.apache.fineract.client.util.JSON;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OfficeHelper {

    public static final long HEAD_OFFICE_ID = 1L; // The ID is hardcoded in the initial Liquibase migration script

    private static final Logger LOG = LoggerFactory.getLogger(OfficeHelper.class);
    private static final String OFFICE_URL = "/fineract-provider/api/v1/offices";
    private static final Gson GSON = new JSON().getGson();
    private final RequestSpecification requestSpec;
    private final ResponseSpecification responseSpec;

    public OfficeHelper(final RequestSpecification requestSpec, final ResponseSpecification responseSpec) {
        this.requestSpec = requestSpec;
        this.responseSpec = responseSpec;
    }

    public OfficeDomain retrieveOfficeByID(int id) {
        Object get = Utils.performServerGet(requestSpec, responseSpec, OFFICE_URL + "/" + id + "?" + Utils.TENANT_IDENTIFIER, "");
        final String json = new Gson().toJson(get);
        return new Gson().fromJson(json, new TypeToken<OfficeDomain>() {}.getType());
    }

    public static GetOfficesResponse getHeadOffice(final RequestSpecification requestSpec, final ResponseSpecification responseSpec) {
        String response = Utils.performServerGet(requestSpec, responseSpec,
                OFFICE_URL + "/" + HEAD_OFFICE_ID + "?" + Utils.TENANT_IDENTIFIER);
        return GSON.fromJson(response, GetOfficesResponse.class);
    }

    public Integer createOffice(final String openingDate) {
        String json = getAsJSON(openingDate);
        return Utils.performServerPost(this.requestSpec, this.responseSpec, OFFICE_URL + "?" + Utils.TENANT_IDENTIFIER, json,
                CommonConstants.RESPONSE_RESOURCE_ID);
    }

    public Integer updateOffice(int id, String name, String openingDate) {
        final HashMap<String, String> map = new HashMap<String, String>();
        map.put("name", name);
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("locale", "en");
        map.put("openingDate", openingDate);

        LOG.info("map :  {}", map);

        return Utils.performServerPut(requestSpec, responseSpec, OFFICE_URL + "/" + id + "?" + Utils.TENANT_IDENTIFIER,
                new Gson().toJson(map), "resourceId");
    }

    public static String getAsJSON(final String openingDate) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("parentId", "1");
        map.put("name", Utils.randomNameGenerator("Office_", 4));
        map.put("dateFormat", "dd MMMM yyyy");
        map.put("locale", "en");
        map.put("openingDate", openingDate);
        LOG.info("map :  {}", map);
        return new Gson().toJson(map);
    }

    public String importOfficeTemplate(File file) {
        String locale = "en";
        String dateFormat = "dd MMMM yyyy";
        requestSpec.header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA);
        return Utils.performServerTemplatePost(requestSpec, responseSpec, OFFICE_URL + "/uploadtemplate" + "?" + Utils.TENANT_IDENTIFIER,
                null, file, locale, dateFormat);

    }

    public String getOutputTemplateLocation(final String importDocumentId) {
        requestSpec.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN);
        return Utils.performServerOutputTemplateLocationGet(requestSpec, responseSpec,
                "/fineract-provider/api/v1/imports/getOutputTemplateLocation" + "?" + Utils.TENANT_IDENTIFIER, importDocumentId);
    }

    public Workbook getOfficeWorkBook(final String dateFormat) throws IOException {
        requestSpec.header(HttpHeaders.CONTENT_TYPE, "application/vnd.ms-excel");
        byte[] byteArray = Utils.performGetBinaryResponse(requestSpec, responseSpec,
                OFFICE_URL + "/downloadtemplate" + "?" + Utils.TENANT_IDENTIFIER + "&dateFormat=" + dateFormat);
        InputStream inputStream = new ByteArrayInputStream(byteArray);
        Workbook workbook = new HSSFWorkbook(inputStream);
        return workbook;
    }
}
