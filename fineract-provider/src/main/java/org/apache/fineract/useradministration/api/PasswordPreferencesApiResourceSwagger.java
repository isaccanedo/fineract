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
package org.apache.fineract.useradministration.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Created by sanyam on 22/8/17.
 */
final class PasswordPreferencesApiResourceSwagger {

    private PasswordPreferencesApiResourceSwagger() {

    }

    @Schema(description = "GetPasswordPreferencesTemplateResponse")
    public static final class GetPasswordPreferencesTemplateResponse {

        private GetPasswordPreferencesTemplateResponse() {

        }

        @Schema(example = "1")
        public Long id;
        @Schema(example = "Password must be at least 1 character and not more that 50 characters long")
        public String description;
        @Schema(example = "true")
        public boolean active;
        @Schema(example = "simple")
        public String key;
    }

    @Schema(description = "PutPasswordPreferencesTemplateRequest")
    public static final class PutPasswordPreferencesTemplateRequest {

        private PutPasswordPreferencesTemplateRequest() {

        }

        @Schema(example = "1")
        public Long validationPolicyId;
    }

}
