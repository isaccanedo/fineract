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
package org.apache.fineract.useradministration.domain;

import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;

@Entity
@Table(name = "m_appuser_previous_password")
public class AppUserPreviousPassword extends AbstractPersistableCustom {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "removal_date")
    private LocalDate removalDate;

    @Column(name = "password", nullable = false)
    private String password;

    protected AppUserPreviousPassword() {

    }

    public AppUserPreviousPassword(final AppUser user) {
        this.userId = user.getId();
        this.password = user.getPassword().trim();
        this.removalDate = DateUtils.getLocalDateOfTenant();
    }

    public String getPassword() {
        return this.password;
    }

}
