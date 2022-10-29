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
package org.apache.fineract.infrastructure.sms.data;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Immutable data object representing an outbound SMS message API response data
 **/
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class SmsMessageApiResponseData {

    private Integer httpStatusCode;
    private List<SmsMessageDeliveryReportData> data;

    /**
     * @return an instance of the SmsMessageApiResponseData class
     **/
    public static SmsMessageApiResponseData getInstance(Integer httpStatusCode, List<SmsMessageDeliveryReportData> data) {
        return new SmsMessageApiResponseData().setHttpStatusCode(httpStatusCode).setData(data);
    }
}
