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
package org.apache.fineract.portfolio.delinquency.domain;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "m_delinquency_bucket_mappings", uniqueConstraints = {
        @UniqueConstraint(name = "uq_delinquency_bucket_mapping", columnNames = { "delinquencyBucket", "delinquencyRange" }) })
public class DelinquencyBucketMappings extends AbstractAuditableWithUTCDateTimeCustom {

    @ManyToOne
    @JoinColumn(name = "delinquency_bucket_id", nullable = false)
    private DelinquencyBucket delinquencyBucket;

    @ManyToOne
    @JoinColumn(name = "delinquency_range_id", nullable = false)
    private DelinquencyRange delinquencyRange;

    @Version
    private Long version;

    public DelinquencyBucketMappings(DelinquencyBucket delinquencyBucket, DelinquencyRange delinquencyRange) {
        this.delinquencyBucket = delinquencyBucket;
        this.delinquencyRange = delinquencyRange;
    }

    public static DelinquencyBucketMappings instance(DelinquencyBucket delinquencyBucket, DelinquencyRange delinquencyRange) {
        return new DelinquencyBucketMappings(delinquencyBucket, delinquencyRange);
    }

}
