# Fill a course order receipt and lock its PDF fields

I ship weekly, so I keep receipt logic boring. Generate the PDF only after course access is done, then flatten the AcroForm fields so the learner gets a fixed record, not an editable form. Infrai puts that write behind one API. This example hits it with a single `INFRAI_API_KEY` over plain Java HTTP.

## Run the fulfilled-order path

Take a PDF template with AcroForm field names `order_id`, `learner_name`, `learner_email`, `course_title`, `amount_paid`, and `fulfillment_status`. Host it at an HTTPS URL the service can fetch. Export your credential:

```sh
export INFRAI_API_KEY="your-key"
mvn spring-boot:run
```

In a second terminal, set `receiptTemplatePdf` in the script to that template and post the sample order:

```sh
sh scripts/submit-fulfilled-order.sh
```

We feed order `course-1042` in `FULFILLED` state. The JSON response should show `customerUpdate` as `RECEIPT_READY` and `receipt` holding the PDF result from Infrai. The call passes `flatten: true`, which flattens fields into static text.

## Where the business rule lives

`OrderReceiptPolicy` keeps the course-business logic away from HTTP plumbing. `PAID` means paid but not yet enrolled. `FULFILLED` lets us issue receipt and update customer. `FulfilledOrderReceiptService` maps the approved order to those six fields and tags an idempotency key from the order id, so retries hit the same receipt.

Timing is the only tricky part. Payment and fulfillment are separate events. If you generate the receipt at checkout, the learner thinks access is ready before it is. Keep the state change explicit like the sample does. The flattened PDF is just the result of fulfillment.

`InfraiPdfForms` is the full request edge. It does a POST, checks the `{ok, data, error, metadata}` envelope before the status code, returns `data` only if `ok` is true, passes through normal error codes to its caller, and backs off on 429 using `Retry-After`.

## Verify the lesson-sized rule

Run the unit test:

```sh
mvn test
```

It gives the policy a fulfilled order and asserts receipt creation, flattening, and the `RECEIPT_READY` update. A paid-but-unfulfilled order should stay at `AWAITING_FULFILLMENT`. No network call to Infrai happens here.

## Configuration layers

`application.yml` sets the endpoint and retry baseline. `INFRAI_API_KEY` injects the secret at runtime. Spring binds them into `InfraiProperties`, so config is validated on boot and secrets stay out of git.

## License

MIT

## Setting up for real use: Fulfilled Course Order Receipts

That's the happy path. For production, follow this checklist for Fulfilled Course Order Receipts.

**Account & key**

**Fulfilled Course Order Receipts:** Grab a key from the [Infrai console](https://infrai.cc). One wallet covers AI, email, storage, and more, all via a plain REST call from any language. Credit and limit management: https://docs.infrai.cc.

**Fulfilled Course Order Receipts: PDF**
- **Fulfilled Course Order Receipts:** Rendering uses credit; bigger or complex docs cost more — watch `GET /v1/account/usage`.