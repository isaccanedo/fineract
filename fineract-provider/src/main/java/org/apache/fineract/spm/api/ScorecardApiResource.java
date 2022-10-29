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
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.spm.data.ScorecardData;
import org.apache.fineract.spm.domain.Scorecard;
import org.apache.fineract.spm.domain.Survey;
import org.apache.fineract.spm.service.ScorecardReadPlatformService;
import org.apache.fineract.spm.service.ScorecardService;
import org.apache.fineract.spm.service.SpmService;
import org.apache.fineract.spm.util.ScorecardMapper;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Path("/surveys/scorecards")
@Component
@Scope("singleton")

@Tag(name = "Score Card", description = "")
public class ScorecardApiResource {

    private final PlatformSecurityContext securityContext;
    private final SpmService spmService;
    private final ScorecardService scorecardService;
    private final ClientRepositoryWrapper clientRepositoryWrapper;
    private final ScorecardReadPlatformService scorecardReadPlatformService;

    @Autowired
    public ScorecardApiResource(final PlatformSecurityContext securityContext, final SpmService spmService,
            final ScorecardService scorecardService, final ClientRepositoryWrapper clientRepositoryWrapper,
            final ScorecardReadPlatformService scorecardReadPlatformService) {
        this.securityContext = securityContext;
        this.spmService = spmService;
        this.scorecardService = scorecardService;
        this.clientRepositoryWrapper = clientRepositoryWrapper;
        this.scorecardReadPlatformService = scorecardReadPlatformService;
    }

    @GET
    @Path("{surveyId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Transactional
    @Operation(summary = "List all Scorecard entries", description = "List all Scorecard entries for a survey.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Scorecard.class)))) })
    public List<ScorecardData> findBySurvey(@PathParam("surveyId") @Parameter(description = "Enter surveyId") final Long surveyId) {
        this.securityContext.authenticatedUser();
        this.spmService.findById(surveyId);
        return (List<ScorecardData>) this.scorecardReadPlatformService.retrieveScorecardBySurvey(surveyId);
    }

    @POST
    @Path("{surveyId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Transactional
    @Operation(summary = "Create a Scorecard entry", description = "Add a new entry to a survey.\n" + "\n" + "Mandatory Fields\n"
            + "clientId, createdOn, questionId, responseId, staffId")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "OK") })
    public void createScorecard(@PathParam("surveyId") @Parameter(description = "Enter surveyId") final Long surveyId,
            @Parameter(description = "scorecardData") final ScorecardData scorecardData) {
        final AppUser appUser = this.securityContext.authenticatedUser();
        final Survey survey = this.spmService.findById(surveyId);
        final Client client = this.clientRepositoryWrapper.findOneWithNotFoundDetection(scorecardData.getClientId());
        this.scorecardService.createScorecard(ScorecardMapper.map(scorecardData, survey, appUser, client));
    }

    @GET
    @Path("{surveyId}/clients/{clientId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Transactional
    public List<ScorecardData> findBySurveyAndClient(@PathParam("surveyId") @Parameter(description = "Enter surveyId") final Long surveyId,
            @PathParam("clientId") @Parameter(description = "Enter clientId") final Long clientId) {
        this.securityContext.authenticatedUser();
        this.spmService.findById(surveyId);
        this.clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);
        return (List<ScorecardData>) this.scorecardReadPlatformService.retrieveScorecardBySurveyAndClient(surveyId, clientId);

    }

    @GET
    @Path("clients/{clientId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Transactional
    public List<ScorecardData> findByClient(@PathParam("clientId") final Long clientId) {
        this.securityContext.authenticatedUser();
        this.clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);
        return (List<ScorecardData>) this.scorecardReadPlatformService.retrieveScorecardByClient(clientId);
    }
}
