#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.
#

echo "Setting Up Fineract service configuration..."
kubectl create secret generic fineract-tenants-db-secret --from-literal=username=root --from-literal=password=$(head /dev/urandom | LC_CTYPE=C tr -dc A-Za-z0-9 | head -c 16)
kubectl apply -f fineractmysql-configmap.yml

echo
echo "Starting fineractmysql..."
kubectl apply -f fineractmysql-deployment.yml

fineractmysql_pod=""
while [[ ${#fineractmysql_pod} -eq 0 ]]; do
    fineractmysql_pod=$(kubectl get pods -l tier=fineractmysql --template '{{range .items}}{{.metadata.name}}{{"\n"}}{{end}}')
done

fineractmysql_status=$(kubectl get pods ${fineractmysql_pod} --no-headers -o custom-columns=":status.phase")
while [[ ${fineractmysql_status} -ne 'Running' ]]; do
    sleep 1
    fineractmysql_status=$(kubectl get pods ${fineractmysql_pod} --no-headers -o custom-columns=":status.phase")
done

echo
echo "Starting fineract server..."
kubectl apply -f fineract-server-deployment.yml

fineract_server_pod=""
while [[ ${#fineract_server_pod} -eq 0 ]]; do
    fineract_server_pod=$(kubectl get pods -l tier=backend --template '{{range .items}}{{.metadata.name}}{{"\n"}}{{end}}')
done

fineract_server_status=$(kubectl get pods ${fineract_server_pod} --no-headers -o custom-columns=":status.phase")
while [[ ${fineract_server_status} -ne 'Running' ]]; do
    sleep 1
    fineract_server_status=$(kubectl get pods ${fineract_server_pod} --no-headers -o custom-columns=":status.phase")
done

echo "Fineract server is up and running"

echo "Starting Mifos Community UI..."
kubectl apply -f fineract-mifoscommunity-deployment.yml

fineract_mifoscommunity_pod=""
while [[ ${#fineract_mifoscommunity_pod} -eq 0 ]]; do
    fineract_mifoscommunity_pod=$(kubectl get pods -l tier=backend --template '{{range .items}}{{.metadata.name}}{{"\n"}}{{end}}')
done

fineract_mifoscommunity_status=$(kubectl get pods ${fineract_mifoscommunity_pod} --no-headers -o custom-columns=":status.phase")
while [[ ${fineract_mifoscommunity_status} -ne 'Running' ]]; do
    sleep 1
    fineract_mifoscommunity_status=$(kubectl get pods ${fineract_mifoscommunity_pod} --no-headers -o custom-columns=":status.phase")
done
echo "Mifos Community UI is up and running"
