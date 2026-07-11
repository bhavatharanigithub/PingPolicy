#!/usr/bin/env bash
# Registers a demo contract against a public test API so you can see
# PingPolicy poll it and (if the shape ever changes) log drift.
set -euo pipefail

REGISTRY_URL="${REGISTRY_URL:-http://localhost:8081}"

curl -s -X POST "$REGISTRY_URL/api/contracts" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "jsonplaceholder-users",
    "endpointUrl": "https://jsonplaceholder.typicode.com/users/1",
    "httpMethod": "GET",
    "expectedSchemaJson": "{\"id\":1,\"name\":\"str\",\"username\":\"str\",\"email\":\"str\",\"address\":{\"street\":\"str\",\"suite\":\"str\",\"city\":\"str\",\"zipcode\":\"str\",\"geo\":{\"lat\":\"str\",\"lng\":\"str\"}},\"phone\":\"str\",\"website\":\"str\",\"company\":{\"name\":\"str\",\"catchPhrase\":\"str\",\"bs\":\"str\"}}",
    "pollIntervalSeconds": 30,
    "active": true
  }' | python3 -m json.tool

echo "Contract registered. It will be polled on the next scheduler tick."
