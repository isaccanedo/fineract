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

import com.google.common.base.Splitter;
import java.util.List;
import javax.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.batch.command.CommandStrategy;
import org.apache.fineract.batch.domain.BatchRequest;
import org.apache.fineract.batch.domain.BatchResponse;
import org.apache.fineract.portfolio.loanaccount.api.LoanChargesApiResource;
import org.springframework.stereotype.Component;

/**
 * Implements {@link org.apache.fineract.batch.command.CommandStrategy} and Collect Charges for a Loan. It passes the
 * contents of the body from the BatchRequest to
 * {@link org.apache.fineract.portfolio.loanaccount.api.LoanChargesApiResource} and gets back the response. This class
 * will also catch any errors raised by {@link org.apache.fineract.portfolio.loanaccount.api.LoanChargesApiResource} and
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
public class CollectChargesCommandStrategy implements CommandStrategy {

    private final LoanChargesApiResource loanChargesApiResource;

    @Override
    public BatchResponse execute(BatchRequest request, UriInfo uriInfo) {

        final BatchResponse response = new BatchResponse();
        final String responseBody;

        response.setRequestId(request.getRequestId());
        response.setHeaders(request.getHeaders());

        final List<String> pathParameters = Splitter.on('/').splitToList(request.getRelativeUrl());

        // Pluck out the loanId out of the relative path
        Long loanId = Long.parseLong(pathParameters.get(1));

        // Calls 'retrieveAllLoanCharges' function from
        // 'LoanChargesApiResource' to Collect
        // Charges for a loan
        responseBody = loanChargesApiResource.retrieveAllLoanCharges(loanId, uriInfo);

        response.setStatusCode(200);
        // Sets the body of the response after Charges have been
        // successfully collected
        response.setBody(responseBody);

        return response;
    }
}
