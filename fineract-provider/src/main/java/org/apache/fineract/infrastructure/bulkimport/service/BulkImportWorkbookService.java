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
package org.apache.fineract.infrastructure.bulkimport.service;

import java.io.InputStream;
import java.util.Collection;
import javax.ws.rs.core.Response;
import org.apache.fineract.infrastructure.bulkimport.data.GlobalEntityType;
import org.apache.fineract.infrastructure.bulkimport.data.ImportData;
import org.apache.fineract.infrastructure.documentmanagement.data.DocumentData;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

public interface BulkImportWorkbookService {

    Long importWorkbook(String entityType, InputStream inputStream, FormDataContentDisposition fileDetail, String locale,
            String dateFormat);

    Collection<ImportData> getImports(GlobalEntityType type);

    DocumentData getOutputTemplateLocation(String importDocumentId);

    Response getOutputTemplate(String importDocumentId);

}
