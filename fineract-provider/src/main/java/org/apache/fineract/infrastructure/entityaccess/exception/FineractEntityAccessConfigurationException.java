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
package org.apache.fineract.infrastructure.entityaccess.exception;

import org.apache.fineract.infrastructure.core.exception.AbstractPlatformDomainRuleException;
import org.apache.fineract.infrastructure.entityaccess.domain.FineractEntityAccessType;
import org.apache.fineract.infrastructure.entityaccess.domain.FineractEntityType;

public class FineractEntityAccessConfigurationException extends AbstractPlatformDomainRuleException {

    public FineractEntityAccessConfigurationException(final Long firstEntityId, final FineractEntityType entityType1,
            final FineractEntityAccessType accessType, final FineractEntityType entityType2) {
        super("error.msg.entityaccess.config", "Error while getting entity access configuration for " + entityType1.getType() + ":"
                + firstEntityId + " with type " + accessType.getStr() + " against " + entityType2.getType());
    }

}
