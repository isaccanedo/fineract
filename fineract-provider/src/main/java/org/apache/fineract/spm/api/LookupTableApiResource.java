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
package org.apache.fineract.spm.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.spm.data.LookupTableData;
import org.apache.fineract.spm.domain.LookupTable;
import org.apache.fineract.spm.domain.Survey;
import org.apache.fineract.spm.exception.LookupTableNotFoundException;
import org.apache.fineract.spm.exception.SurveyNotFoundException;
import org.apache.fineract.spm.service.LookupTableService;
import org.apache.fineract.spm.service.SpmService;
import org.apache.fineract.spm.util.LookupTableMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Path("/surveys/{surveyId}/lookuptables")
@Component
@Scope("singleton")
@Tag(name = "SPM API - LookUp Table", description = "The Apache Fineract SPM API provides the ability to create custom surveys to collect social performance measurentment data or any additional questionnaire a financial institute want to collect.")
public class LookupTableApiResource {

    private final PlatformSecurityContext securityContext;
    private final SpmService spmService;
    private final LookupTableService lookupTableService;

    @Autowired
    public LookupTableApiResource(final PlatformSecurityContext securityContext, final SpmService spmService,
            final LookupTableService lookupTableService) {

        this.securityContext = securityContext;
        this.spmService = spmService;
        this.lookupTableService = lookupTableService;
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Transactional
    @Operation(summary = "List all Lookup Table entries", description = "List all Lookup Table entries for a survey.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = LookupTableData.class)))) })
    public List<LookupTableData> fetchLookupTables(@PathParam("surveyId") @Parameter(description = "Enter surveyId") final Long surveyId) {
        this.securityContext.authenticatedUser();

        final Survey survey = findSurvey(surveyId);

        final List<LookupTable> lookupTables = this.lookupTableService.findBySurvey(survey);

        if (lookupTables != null) {
            return LookupTableMapper.map(lookupTables);
        }

        return Collections.emptyList();
    }

    @GET
    @Path("/{key}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Transactional
    @Operation(summary = "Retrieve a Lookup Table entry", description = "Retrieve a Lookup Table entry for a survey.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = LookupTableData.class))) })
    public LookupTableData findLookupTable(@PathParam("surveyId") @Parameter(description = "Enter surveyId") final Long surveyId,
            @PathParam("key") @Parameter(description = "Enter key") final String key) {
        this.securityContext.authenticatedUser();

        findSurvey(surveyId);

        final List<LookupTable> lookupTables = this.lookupTableService.findByKey(key);

        if (lookupTables == null || lookupTables.isEmpty()) {
            throw new LookupTableNotFoundException(key);
        }

        return LookupTableMapper.map(lookupTables).get(0);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Transactional
    @Operation(summary = "Create a Lookup Table entry", description = "Add a new entry to a survey.\n" + "\n" + "Mandatory Fields\n"
            + "key, score, validFrom, validTo")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "OK") })
    public void createLookupTable(@PathParam("surveyId") @Parameter(description = "Enter surveyId") final Long surveyId,
            final LookupTableData lookupTableData) {
        this.securityContext.authenticatedUser();

        final Survey survey = findSurvey(surveyId);

        this.lookupTableService.createLookupTable(LookupTableMapper.map(lookupTableData, survey));
    }

    private Survey findSurvey(final Long surveyId) {
        final Survey survey = this.spmService.findById(surveyId);
        if (survey == null) {
            throw new SurveyNotFoundException(surveyId);
        }
        return survey;
    }
}
