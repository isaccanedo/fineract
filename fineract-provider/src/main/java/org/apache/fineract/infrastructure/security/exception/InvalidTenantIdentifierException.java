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
package org.apache.fineract.infrastructure.security.exception;

import org.springframework.dao.EmptyResultDataAccessException;

/**
 * {@link RuntimeException} thrown when an invalid tenant identifier is used in request to platform.
 *
 *
 */
public class InvalidTenantIdentifierException extends RuntimeException {

    public InvalidTenantIdentifierException(final String message) {
        super(message);
    }

    public InvalidTenantIdentifierException(String message, EmptyResultDataAccessException e) {
        super(message, e);
    }
}
