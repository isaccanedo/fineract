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
package org.apache.fineract.batch.command.internal;

import javax.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.batch.command.CommandStrategy;
import org.apache.fineract.batch.domain.BatchRequest;
import org.apache.fineract.batch.domain.BatchResponse;
import org.apache.fineract.portfolio.savings.api.SavingsAccountsApiResource;
import org.springframework.stereotype.Component;

/**
 * Implements {@link org.apache.fineract.batch.command.CommandStrategy} and applies a new savings on an existing client.
 * It passes the contents of the body from the BatchRequest to
 * {@link org.apache.fineract.portfolio.savings.api.SavingsAccountsApiResource} and gets back the response. This class
 * will also catch any errors raised by {@link org.apache.fineract.portfolio.savings.api.SavingsAccountsApiResource} and
 * map those errors to appropriate status codes in BatchResponse.
 *
 * @author Rishabh Shukla
 *
 * @see org.apache.fineract.batch.command.CommandStrategy
 * @see org.apache.fineract.batch.domain.BatchRequest
 * @see org.apache.fineract.batch.domain.BatchResponse
 */
@Component
@RequiredArgsConstructor
public class ApplySavingsCommandStrategy implements CommandStrategy {

    private final SavingsAccountsApiResource savingsAccountsApiResource;

    @Override
    public BatchResponse execute(BatchRequest request, @SuppressWarnings("unused") UriInfo uriInfo) {

        final BatchResponse response = new BatchResponse();
        final String responseBody;

        response.setRequestId(request.getRequestId());
        response.setHeaders(request.getHeaders());

        // Calls 'submitApplication' function from
        // 'SavingsAccountsApiResource' to Apply Savings to an existing
        // client
        responseBody = savingsAccountsApiResource.submitApplication(request.getBody());

        response.setStatusCode(200);
        // Sets the body of the response after savings is successfully
        // applied
        response.setBody(responseBody);

        return response;
    }
}
