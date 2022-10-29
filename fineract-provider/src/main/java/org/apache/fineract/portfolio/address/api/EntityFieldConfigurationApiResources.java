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
package org.apache.fineract.portfolio.address.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.address.data.FieldConfigurationData;
import org.apache.fineract.portfolio.address.service.FieldConfigurationReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/fieldconfiguration/{entity}")
@Component
@Scope("singleton")
@Tag(name = "Entity Field Configuration", description = "Entity Field configuration API is a generic and extensible \n"
        + "wherein various entities and subentities can be related.\n" + "Also it gives the user an ability to enable/disable fields,\n"
        + "add regular expression for validation")
public class EntityFieldConfigurationApiResources {

    private static final Set<String> RESPONSE_DATA_PARAMETERS = new HashSet<>(
            Arrays.asList("clientAddressId", "client_id", "address_id", "address_type_id", "isActive", "fieldConfigurationId", "entity",
                    "table", "field", "is_enabled", "is_mandatory", "validation_regex"));
    private static final String RESOURCE_NAME_FOR_PERMISSIONS = "Address";
    private final PlatformSecurityContext context;
    private final FieldConfigurationReadPlatformService readPlatformServicefld;
    private final DefaultToApiJsonSerializer<FieldConfigurationData> toApiJsonSerializerfld;
    private final ApiRequestParameterHelper apiRequestParameterHelper;

    @Autowired
    public EntityFieldConfigurationApiResources(final PlatformSecurityContext context,
            final FieldConfigurationReadPlatformService readPlatformServicefld,
            final DefaultToApiJsonSerializer<FieldConfigurationData> toApiJsonSerializerfld,
            final ApiRequestParameterHelper apiRequestParameterHelper) {
        this.context = context;
        this.readPlatformServicefld = readPlatformServicefld;
        this.toApiJsonSerializerfld = toApiJsonSerializerfld;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
    }

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieves the Entity Field Configuration", description = "It retrieves all the Entity Field Configuration")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = EntityFieldConfigurationApiResourcesSwagger.GetFieldConfigurationEntityResponse.class)))) })
    public String getAddresses(@PathParam("entity") @Parameter(description = "entity") final String entityname,
            @Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);

        final Collection<FieldConfigurationData> fldconfig = this.readPlatformServicefld.retrieveFieldConfiguration(entityname);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializerfld.serialize(settings, fldconfig, RESPONSE_DATA_PARAMETERS);

    }

}
