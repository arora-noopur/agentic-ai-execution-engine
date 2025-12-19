#!/bin/bash

# Payload content
DATA="URGENT: Machine PRESS-01 is reporting an overheat error code."

echo "Sending Incident: $DATA"
echo "---------------------------------"

# Execute Request
curl -X POST -H "Content-Type: text/plain" \
     -d "$DATA" \
     http://localhost:8080/api/incidents

echo -e "\n---------------------------------"