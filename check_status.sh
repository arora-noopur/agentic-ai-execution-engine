#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: ./check_status.sh <WORKFLOW_ID>"
  exit 1
fi

WORKFLOW_ID=$1

echo "Checking status for: $WORKFLOW_ID"
echo "---------------------------------"

# Execute Request
curl http://localhost:8080/api/workflows/$WORKFLOW_ID/status

echo -e "\n---------------------------------"