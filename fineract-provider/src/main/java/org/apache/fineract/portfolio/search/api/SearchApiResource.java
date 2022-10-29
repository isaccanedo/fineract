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
package org.apache.fineract.portfolio.search.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collection;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.portfolio.search.SearchConstants.SearchResponseParameters;
import org.apache.fineract.portfolio.search.data.AdHocQueryDataValidator;
import org.apache.fineract.portfolio.search.data.AdHocQuerySearchConditions;
import org.apache.fineract.portfolio.search.data.AdHocSearchQueryData;
import org.apache.fineract.portfolio.search.data.SearchConditions;
import org.apache.fineract.portfolio.search.data.SearchData;
import org.apache.fineract.portfolio.search.service.SearchReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/search")
@Component
@Scope("singleton")
@Tag(name = "Search API", description = "Search API allows to search scoped resources clients, loans and groups on specified fields.")
public class SearchApiResource {

    private static final Set<String> SEARCH_RESPONSE_PARAMETERS = SearchResponseParameters.getAllValues();

    private final SearchReadPlatformService searchReadPlatformService;
    private final ToApiJsonSerializer<Object> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final AdHocQueryDataValidator fromApiJsonDeserializer;

    @Autowired
    public SearchApiResource(final SearchReadPlatformService searchReadPlatformService,
            final ToApiJsonSerializer<Object> toApiJsonSerializer, final ApiRequestParameterHelper apiRequestParameterHelper,
            final AdHocQueryDataValidator fromApiJsonDeserializer) {

        this.searchReadPlatformService = searchReadPlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;

    }

    @GET
    @Path("/template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrive Adhoc Search query template", description = "Mandatory Fields\n" + "\n" + "search?query=000000001\n")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = SearchApiResourceSwagger.GetSearchResponse.class))) })
    public String retrieveAdHocSearchQueryTemplate(@Context final UriInfo uriInfo) {

        final AdHocSearchQueryData templateData = this.searchReadPlatformService.retrieveAdHocQueryTemplate();

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, templateData);
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Search Resources", description = "Example Requests:\n" + "\n" + "search?query=000000001\n" + "\n" + "\n"
            + "search?query=Petra&resource=clients,groups\n" + "\n" + "\n" + "search?query=Petra&resource=clients,groups&exactMatch=true")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SearchApiResourceSwagger.GetSearchResponse.class)))) })
    public String searchData(@Context final UriInfo uriInfo, @QueryParam("query") @Parameter(description = "query") final String query,
            @QueryParam("resource") @Parameter(description = "resource") final String resource,
            @DefaultValue("false") @QueryParam("exactMatch") @Parameter(description = "exactMatch") Boolean exactMatch) {

        final SearchConditions searchConditions = new SearchConditions(query, resource, exactMatch);

        final Collection<SearchData> searchResults = this.searchReadPlatformService.retriveMatchingData(searchConditions);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, searchResults, SEARCH_RESPONSE_PARAMETERS);
    }

    @POST
    @Path("/advance")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Adhoc query search", description = "AdHocQuery search has more search options, it is a POST request, it uses request body to send search parameters\n"
            + "\n" + "\n" + "Mandatory fields:" + "entities" + "\n" + "\n" + "Optional fields:"
            + "loanStatus, loanProducts, offices, loanDateOption, loanFromDate, loanToDate, \n"
            + "includeOutStandingAmountPercentage, outStandingAmountPercentageCondition, \n"
            + "minOutStandingAmountPercentage and maxOutStandingAmountPercentage OR outStandingAmountPercentage, \n"
            + "includeOutstandingAmount, outstandingAmountCondition, \n"
            + "minOutstandingAmount and maxOutstandingAmount OR outstandingAmount")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = SearchApiResourceSwagger.PostAdhocQuerySearchRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SearchApiResourceSwagger.PostAdhocQuerySearchResponse.class)))) })
    public String advancedSearch(@Context final UriInfo uriInfo, final String json) {

        final AdHocQuerySearchConditions searchConditions = this.fromApiJsonDeserializer.retrieveSearchConditions(json);

        final Collection<AdHocSearchQueryData> searchResults = this.searchReadPlatformService
                .retrieveAdHocQueryMatchingData(searchConditions);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, searchResults);
    }
}
