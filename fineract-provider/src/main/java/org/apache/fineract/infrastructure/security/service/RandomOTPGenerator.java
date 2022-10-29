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
package org.apache.fineract.infrastructure.security.service;

import java.security.SecureRandom;

public class RandomOTPGenerator {

    private static final String allowedCharacters = "0123456789ABCDEFGHIJKLMNOPQRSTUVQXYZ";
    private final int tokenLength;
    private final SecureRandom secureRandom = new SecureRandom();

    public RandomOTPGenerator(int tokenLength) {
        this.tokenLength = tokenLength;
    }

    public String generate() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < tokenLength; i++) {
            builder.append(allowedCharacters.charAt((int) (secureRandom.nextDouble() * allowedCharacters.length())));
        }

        return builder.toString();
    }
}
