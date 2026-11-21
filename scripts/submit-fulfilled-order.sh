#!/usr/bin/env sh
set -eu

curl --fail-with-body --request POST http://localhost:8080/orders/receipt \
  --header 'Content-Type: application/json' \
  --data '{
    "orderId": "course-1042",
    "learnerName": "Mina Chen",
    "learnerEmail": "mina@example.com",
    "courseTitle": "Practical Geometry",
    "amountPaid": 49.00,
    "fulfillmentStatus": "FULFILLED",
    "receiptTemplatePdf": "https://example.com/course-receipt.pdf"
  }'
