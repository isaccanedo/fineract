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
package org.apache.fineract.infrastructure.core.service;

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenantConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Implementation that returns a new or existing connection pool datasource based on the tenant details stored in a
 * {@link ThreadLocal} variable for this request.
 *
 * {@link ThreadLocalContextUtil} is used to retrieve the {@link FineractPlatformTenant} for the request.
 */
@Service
public class TomcatJdbcDataSourcePerTenantService implements RoutingDataSourceService {

    private static final Map<Long, DataSource> TENANT_TO_DATA_SOURCE_MAP = new HashMap<>(1);
    private final DataSource tenantDataSource;

    private final DataSourcePerTenantServiceFactory dataSourcePerTenantServiceFactory;

    @Autowired
    public TomcatJdbcDataSourcePerTenantService(final @Qualifier("hikariTenantDataSource") DataSource tenantDataSource,
            final DataSourcePerTenantServiceFactory dataSourcePerTenantServiceFactory) {
        this.tenantDataSource = tenantDataSource;
        this.dataSourcePerTenantServiceFactory = dataSourcePerTenantServiceFactory;
    }

    @Override
    public DataSource retrieveDataSource() {
        // default to tenant database datasource
        DataSource actualDataSource = this.tenantDataSource;

        final FineractPlatformTenant tenant = ThreadLocalContextUtil.getTenant();
        if (tenant != null) {
            final FineractPlatformTenantConnection tenantConnection = tenant.getConnection();

            synchronized (TENANT_TO_DATA_SOURCE_MAP) {
                // if tenantConnection information available switch to the
                // appropriate datasource for that tenant.
                DataSource possibleDS = TENANT_TO_DATA_SOURCE_MAP.get(tenantConnection.getConnectionId());
                if (possibleDS != null) {
                    actualDataSource = possibleDS;
                } else {
                    actualDataSource = dataSourcePerTenantServiceFactory.createNewDataSourceFor(tenantConnection);
                    TENANT_TO_DATA_SOURCE_MAP.put(tenantConnection.getConnectionId(), actualDataSource);
                }
            }
        }

        return actualDataSource;
    }
}
