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
package org.apache.fineract.infrastructure.dataqueries.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.dataqueries.data.EntityDataTableChecksData;
import org.apache.fineract.infrastructure.dataqueries.data.EntityDataTableChecksTemplateData;
import org.apache.fineract.infrastructure.dataqueries.data.GenericResultsetData;
import org.apache.fineract.infrastructure.dataqueries.service.EntityDatatableChecksReadService;
import org.apache.fineract.infrastructure.dataqueries.service.GenericDataService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/entityDatatableChecks")
@Component
@Scope("singleton")
@Tag(name = "Entity Data Table", description = "This defines Entity-Datatable Check.")
public class EntityDatatableChecksApiResource {

    private final PlatformSecurityContext context;
    private final GenericDataService genericDataService;
    private final EntityDatatableChecksReadService readEntityDatatableChecksService;
    private final ToApiJsonSerializer<GenericResultsetData> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(EntityDatatableChecksApiResource.class);

    @Autowired
    public EntityDatatableChecksApiResource(final PlatformSecurityContext context, final GenericDataService genericDataService,
            final EntityDatatableChecksReadService readEntityDatatableChecksService,
            final ToApiJsonSerializer<GenericResultsetData> toApiJsonSerializer,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.context = context;
        this.genericDataService = genericDataService;
        this.readEntityDatatableChecksService = readEntityDatatableChecksService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "List Entity-Datatable Checks", description = "The list capability of Entity-Datatable Checks can support pagination.\n"
            + "\n" + "OPTIONAL ARGUMENTS\n"
            + "offset Integer optional, defaults to 0 Indicates the result from which pagination startslimit Integer optional, defaults to 200 Restricts the size of results returned. To override the default and return all entries you must explicitly pass a non-positive integer value for limit e.g. limit=0, or limit=-1\n"
            + "Example Request:\n" + "\n" + "entityDatatableChecks?offset=0&limit=15")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = EntityDatatableChecksApiResourceSwagger.GetEntityDatatableChecksResponse.class)))) })
    public String retrieveAll(@Context final UriInfo uriInfo, @QueryParam("status") @Parameter(description = "status") final Long status,
            @QueryParam("entity") @Parameter(description = "entity") final String entity,
            @QueryParam("productId") @Parameter(description = "productId") final Long productId,
            @QueryParam("offset") @Parameter(description = "offset") final Integer offset,
            @QueryParam("limit") @Parameter(description = "limit") final Integer limit) {
        final SearchParameters searchParameters = SearchParameters.forPagination(offset, limit);
        final Page<EntityDataTableChecksData> result = this.readEntityDatatableChecksService.retrieveAll(searchParameters, status, entity,
                productId);

        final boolean prettyPrint = ApiParameterHelper.prettyPrint(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serializePretty(prettyPrint, result);
    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve Entity-Datatable Checks Template", description = "This is a convenience resource useful for building maintenance user interface screens for Entity-Datatable Checks applications. The template data returned consists of:\n"
            + "\n" + "Allowed description Lists\n" + "Example Request:\n" + "\n" + "entityDatatableChecks/template")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = EntityDatatableChecksApiResourceSwagger.GetEntityDatatableChecksTemplateResponse.class))) })
    public String getTemplate(@Context final UriInfo uriInfo) {

        final EntityDataTableChecksTemplateData result = this.readEntityDatatableChecksService.retrieveTemplate();

        final boolean prettyPrint = ApiParameterHelper.prettyPrint(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serializePretty(prettyPrint, result);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Create Entity-Datatable Checks", description = "Mandatory Fields : \n" + "entity, status, datatableName\n" + "\n"
            + "Non-Mandatory Fields : \n" + "productId")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = EntityDatatableChecksApiResourceSwagger.PostEntityDatatableChecksTemplateRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = EntityDatatableChecksApiResourceSwagger.PostEntityDatatableChecksTemplateResponse.class))) })
    public String createEntityDatatableCheck(@Parameter(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().createEntityDatatableChecks(apiRequestBodyAsJson).build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @DELETE
    @Path("{entityDatatableCheckId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Delete Entity-Datatable Checks", description = "Deletes an existing Entity-Datatable Check")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = EntityDatatableChecksApiResourceSwagger.DeleteEntityDatatableChecksTemplateResponse.class))) })
    public String deleteDatatable(
            @PathParam("entityDatatableCheckId") @Parameter(description = "entityDatatableCheckId") final long entityDatatableCheckId,
            final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .deleteEntityDatatableChecks(entityDatatableCheckId, apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

}
