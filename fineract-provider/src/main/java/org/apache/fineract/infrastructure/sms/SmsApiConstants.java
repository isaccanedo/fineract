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
package org.apache.fineract.infrastructure.sms;

public final class SmsApiConstants {

    private SmsApiConstants() {

    }

    public static final String RESOURCE_NAME = "sms";

    // general
    public static final String localeParamName = "locale";
    public static final String dateFormatParamName = "dateFormat";

    // request parameters
    public static final String idParamName = "id";
    public static final String groupIdParamName = "groupId";
    public static final String externalIdParamName = "externalId";
    public static final String clientIdParamName = "clientId";
    public static final String staffIdParamName = "staffId";
    public static final String messageParamName = "message";
    public static final String campaignIdParamName = "campaignId";

    // response parameters
    public static final String statusParamName = "status";

}
