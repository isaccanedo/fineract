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
package org.apache.fineract.portfolio.paymenttype.data;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
public class PaymentTypeData implements Serializable {

    private Long id;
    private String name;
    private String description;
    private Boolean isCashPayment;
    private Long position;
    private String codeName;
    private Boolean isSystemDefined;

    public static PaymentTypeData instance(final Long id, final String name, final String description, final Boolean isCashPayment,
            final Long position) {
        String codeName = null;
        Boolean isSystemDefined = false;
        return new PaymentTypeData(id, name, description, isCashPayment, position, codeName, isSystemDefined);
    }

    public static PaymentTypeData instance(final Long id, final String name) {
        String description = null;
        Boolean isCashPayment = null;
        Long position = null;
        String codeName = null;
        Boolean isSystemDefined = false;
        return new PaymentTypeData(id, name, description, isCashPayment, position, codeName, isSystemDefined);
    }
}
