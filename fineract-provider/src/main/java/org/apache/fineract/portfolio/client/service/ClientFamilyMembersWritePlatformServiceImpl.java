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

package org.apache.fineract.portfolio.client.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepository;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientFamilyMembers;
import org.apache.fineract.portfolio.client.domain.ClientFamilyMembersRepository;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.client.serialization.ClientFamilyMemberCommandFromApiJsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClientFamilyMembersWritePlatformServiceImpl implements ClientFamilyMembersWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(ClientFamilyMembersWritePlatformServiceImpl.class);
    private final PlatformSecurityContext context;
    private final CodeValueRepository codeValueRepository;
    private final ClientFamilyMembersRepository clientFamilyRepository;
    private final ClientRepositoryWrapper clientRepositoryWrapper;
    private final ClientFamilyMemberCommandFromApiJsonDeserializer apiJsonDeserializer;

    @Autowired
    public ClientFamilyMembersWritePlatformServiceImpl(final PlatformSecurityContext context, final CodeValueRepository codeValueRepository,
            final ClientFamilyMembersRepository clientFamilyRepository, final ClientRepositoryWrapper clientRepositoryWrapper,
            final ClientFamilyMemberCommandFromApiJsonDeserializer apiJsonDeserializer) {
        this.context = context;
        this.codeValueRepository = codeValueRepository;
        this.clientFamilyRepository = clientFamilyRepository;
        this.clientRepositoryWrapper = clientRepositoryWrapper;
        this.apiJsonDeserializer = apiJsonDeserializer;

    }

    @Override
    public CommandProcessingResult addFamilyMember(final long clientId, final JsonCommand command) {

        Long relationshipId = null;
        CodeValue relationship = null;
        CodeValue maritalStatus = null;
        Long maritalStatusId = null;
        Long genderId = null;
        CodeValue gender = null;
        Long professionId = null;
        CodeValue profession = null;
        String firstName = "";
        String middleName = "";
        String lastName = "";
        String qualification = "";
        String mobileNumber = "";
        Long age = null;
        Boolean isDependent = false;
        LocalDate dateOfBirth = null;

        this.context.authenticatedUser();
        apiJsonDeserializer.validateForCreate(clientId, command.json());

        Client client = clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);
        firstName = command.stringValueOfParameterNamed("firstName");
        middleName = command.stringValueOfParameterNamed("middleName");
        lastName = command.stringValueOfParameterNamed("lastName");
        qualification = command.stringValueOfParameterNamed("qualification");
        mobileNumber = command.stringValueOfParameterNamed("mobileNumber");
        age = command.longValueOfParameterNamed("age");
        isDependent = command.booleanObjectValueOfParameterNamed("isDependent");

        if (command.longValueOfParameterNamed("relationshipId") != null) {
            relationshipId = command.longValueOfParameterNamed("relationshipId");
            relationship = this.codeValueRepository.getReferenceById(relationshipId);
        }

        if (command.longValueOfParameterNamed("maritalStatusId") != null) {
            maritalStatusId = command.longValueOfParameterNamed("maritalStatusId");
            maritalStatus = this.codeValueRepository.getReferenceById(maritalStatusId);
        }

        if (command.longValueOfParameterNamed("genderId") != null) {
            genderId = command.longValueOfParameterNamed("genderId");
            gender = this.codeValueRepository.getReferenceById(genderId);
        }

        if (command.longValueOfParameterNamed("professionId") != null) {
            professionId = command.longValueOfParameterNamed("professionId");
            profession = this.codeValueRepository.getReferenceById(professionId);
        }

        dateOfBirth = command.localDateValueOfParameterNamed("dateOfBirth");

        ClientFamilyMembers clientFamilyMembers = ClientFamilyMembers.fromJson(client, firstName, middleName, lastName, qualification,
                mobileNumber, age, isDependent, relationship, maritalStatus, gender, dateOfBirth, profession);

        this.clientFamilyRepository.saveAndFlush(clientFamilyMembers);

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(clientFamilyMembers.getId()).build();

    }

    @Override
    public CommandProcessingResult addClientFamilyMember(final Client client, final JsonCommand command) {

        Long relationshipId = null;
        CodeValue relationship = null;
        CodeValue maritalStatus = null;
        Long maritalStatusId = null;
        Long genderId = null;
        CodeValue gender = null;
        Long professionId = null;
        CodeValue profession = null;
        String firstName = "";
        String middleName = "";
        String lastName = "";
        String qualification = "";
        LocalDate dateOfBirth = null;
        String mobileNumber = "";
        Long age = null;
        Boolean isDependent = false;

        this.context.authenticatedUser();

        // Client
        // client=clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);

        ClientFamilyMembers familyMember = new ClientFamilyMembers();

        // apiJsonDeserializer.validateForCreate(command.json());

        JsonArray familyMembers = command.arrayOfParameterNamed("familyMembers");

        for (JsonElement members : familyMembers) {

            apiJsonDeserializer.validateForCreate(members.toString());

            JsonObject member = members.getAsJsonObject();

            if (member.get("firstName") != null) {
                firstName = member.get("firstName").getAsString();
            }

            if (member.get("middleName") != null) {
                middleName = member.get("middleName").getAsString();
            }

            if (member.get("lastName") != null) {
                lastName = member.get("lastName").getAsString();
            }

            if (member.get("qualification") != null) {
                qualification = member.get("qualification").getAsString();
            }

            if (member.get("mobileNumber") != null) {
                mobileNumber = member.get("mobileNumber").getAsString();
            }

            if (member.get("age") != null) {
                age = member.get("age").getAsLong();
            }

            if (member.get("isDependent") != null) {
                isDependent = member.get("isDependent").getAsBoolean();
            }

            if (member.get("relationshipId") != null) {
                relationshipId = member.get("relationshipId").getAsLong();
                relationship = this.codeValueRepository.getReferenceById(relationshipId);
            }

            if (member.get("maritalStatusId") != null) {
                maritalStatusId = member.get("maritalStatusId").getAsLong();
                maritalStatus = this.codeValueRepository.getReferenceById(maritalStatusId);
            }

            if (member.get("genderId") != null) {
                genderId = member.get("genderId").getAsLong();
                gender = this.codeValueRepository.getReferenceById(genderId);
            }

            if (member.get("professionId") != null) {
                professionId = member.get("professionId").getAsLong();
                profession = this.codeValueRepository.getReferenceById(professionId);
            }

            if (member.get("dateOfBirth") != null) {

                DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendPattern(member.get("dateFormat").getAsString())
                        .toFormatter();
                LocalDate date;
                try {
                    date = LocalDate.parse(member.get("dateOfBirth").getAsString(), formatter);
                    dateOfBirth = date;
                } catch (DateTimeParseException e) {
                    // TODO Auto-generated catch block
                    LOG.error("Problem occurred in addClientFamilyMember function", e);
                }

                /*
                 * this.fromApiJsonHelper.extractDateFormatParameter(member.get( "dateOfBirth").getAsJsonObject());
                 */

            }

            familyMember = ClientFamilyMembers.fromJson(client, firstName, middleName, lastName, qualification, mobileNumber, age,
                    isDependent, relationship, maritalStatus, gender, dateOfBirth, profession);

            this.clientFamilyRepository.saveAndFlush(familyMember);

        }

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(familyMember.getId()).build();

    }

    @Override
    public CommandProcessingResult updateFamilyMember(Long familyMemberId, JsonCommand command) {

        Long relationshipId = null;
        CodeValue relationship = null;
        CodeValue maritalStatus = null;
        Long maritalStatusId = null;
        Long genderId = null;
        CodeValue gender = null;
        Long professionId = null;
        CodeValue profession = null;
        String firstName = "";
        String middleName = "";
        String lastName = "";
        String qualification = "";
        LocalDate dateOfBirth = null;
        String mobileNumber = "";
        Long age = null;
        Boolean isDependent = false;
        // long clientFamilyMemberId=0;

        this.context.authenticatedUser();

        apiJsonDeserializer.validateForUpdate(familyMemberId, command.json());

        /*
         * if (command.stringValueOfParameterNamed("clientFamilyMemberId") != null) { clientFamilyMemberId =
         * command.longValueOfParameterNamed("clientFamilyMemberId"); }
         */

        ClientFamilyMembers clientFamilyMember = clientFamilyRepository.getReferenceById(familyMemberId);

        // Client
        // client=clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);

        if (command.stringValueOfParameterNamed("firstName") != null) {
            firstName = command.stringValueOfParameterNamed("firstName");
            clientFamilyMember.setFirstName(firstName);
        }

        if (command.stringValueOfParameterNamed("middleName") != null) {
            middleName = command.stringValueOfParameterNamed("middleName");
            clientFamilyMember.setMiddleName(middleName);
        }

        if (command.stringValueOfParameterNamed("lastName") != null) {
            lastName = command.stringValueOfParameterNamed("lastName");
            clientFamilyMember.setLastName(lastName);
        }

        if (command.stringValueOfParameterNamed("qualification") != null) {
            qualification = command.stringValueOfParameterNamed("qualification");
            clientFamilyMember.setQualification(qualification);
        }

        if (command.stringValueOfParameterNamed("mobileNumber") != null) {
            mobileNumber = command.stringValueOfParameterNamed("mobileNumber");
            clientFamilyMember.setMobileNumber(mobileNumber);
        }

        if (command.longValueOfParameterNamed("age") != null) {
            age = command.longValueOfParameterNamed("age");
            clientFamilyMember.setAge(age);
        }

        if (command.booleanObjectValueOfParameterNamed("isDependent") != null) {
            isDependent = command.booleanObjectValueOfParameterNamed("isDependent");
            clientFamilyMember.setIsDependent(isDependent);
        }

        if (command.longValueOfParameterNamed("relationShipId") != null) {
            relationshipId = command.longValueOfParameterNamed("relationShipId");
            relationship = this.codeValueRepository.getReferenceById(relationshipId);
            clientFamilyMember.setRelationship(relationship);
        }

        if (command.longValueOfParameterNamed("maritalStatusId") != 0) {
            maritalStatusId = command.longValueOfParameterNamed("maritalStatusId");
            maritalStatus = this.codeValueRepository.getReferenceById(maritalStatusId);
            clientFamilyMember.setMaritalStatus(maritalStatus);
        }

        if (command.longValueOfParameterNamed("genderId") != 0) {
            genderId = command.longValueOfParameterNamed("genderId");
            gender = this.codeValueRepository.getReferenceById(genderId);
            clientFamilyMember.setGender(gender);
        }

        if (command.longValueOfParameterNamed("professionId") != 0) {
            professionId = command.longValueOfParameterNamed("professionId");
            profession = this.codeValueRepository.getReferenceById(professionId);
            clientFamilyMember.setProfession(profession);
        }

        if (command.localDateValueOfParameterNamed("dateOfBirth") != null) {
            dateOfBirth = command.localDateValueOfParameterNamed("dateOfBirth");
            clientFamilyMember.setDateOfBirth(dateOfBirth);

        }

        // ClientFamilyMembers
        // clientFamilyMembers=ClientFamilyMembers.fromJson(client, firstName,
        // middleName, lastName, qualification, relationship, maritalStatus,
        // gender, dateOfBirth, profession);

        this.clientFamilyRepository.saveAndFlush(clientFamilyMember);

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(clientFamilyMember.getId()).build();
    }

    @Override
    public CommandProcessingResult deleteFamilyMember(Long clientFamilyMemberId, JsonCommand command) {
        // TODO Auto-generated method stub

        this.context.authenticatedUser();

        apiJsonDeserializer.validateForDelete(clientFamilyMemberId);

        ClientFamilyMembers clientFamilyMember = null;

        if (clientFamilyMemberId != null) {
            clientFamilyMember = clientFamilyRepository.getReferenceById(clientFamilyMemberId);
            clientFamilyRepository.delete(clientFamilyMember);

        }

        if (clientFamilyMember != null) {
            return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(clientFamilyMember.getId()).build();
        } else {
            return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(Long.valueOf(clientFamilyMemberId))
                    .build();
        }

    }

}
