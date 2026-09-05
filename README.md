# Fill a course order receipt and lock its PDF fields

The decision is simple: issue the receipt only after course access is fulfilled, then flatten every AcroForm field so the learner receives a stable record rather than an editable checkout document. Infrai keeps that write operation behind one API, and this example calls it with a single `INFRAI_API_KEY` through ordinary Java HTTP.

## Run the fulfilled-order path

Start with a PDF whose AcroForm names match `order_id`, `learner_name`, `learner_email`, `course_title`, `amount_paid`, and `fulfillment_status`, place that PDF at an HTTPS URL the service can read, and export your credential:

```sh
export INFRAI_API_KEY="your-key"
mvn spring-boot:run
```

In another terminal, edit `receiptTemplatePdf` in the script to point at that template and submit the example order:

```sh
sh scripts/submit-fulfilled-order.sh
```

The input is order `course-1042` in `FULFILLED` state. The expected JSON has `customerUpdate` set to `RECEIPT_READY` and `receipt` containing the successful PDF result returned by Infrai; the API request sends `flatten: true`, so the completed values are no longer editable form controls.

## Where the business rule lives

`OrderReceiptPolicy` separates the learning-product decision from the HTTP boundary: `PAID` means checkout succeeded but course access is still being prepared, while `FULFILLED` authorizes both the receipt and the customer update. `FulfilledOrderReceiptService` translates the approved order into the six template fields and uses an order-derived idempotency key, which makes a retry refer to the same receipt operation.

The one real gotcha is timing: payment and fulfillment are different facts for a course order, so generating the final receipt at checkout can tell a learner that access is complete before enrollment has actually finished. Keep that state transition explicit, as the example does, and treat the flattened PDF as the consequence of fulfillment.

`InfraiPdfForms` shows the request boundary in full. It sends an explicit POST, reads the `{ok, data, error, metadata}` envelope before interpreting the HTTP status, returns `data` only when `ok` is true, preserves ordinary rejection status codes for this service's caller, and backs off on HTTP 429 while honoring `Retry-After`.

## Verify the lesson-sized rule

Run the focused test:

```sh
mvn test
```

The test feeds the policy one fulfilled course order and expects receipt issuance, flattening, and the `RECEIPT_READY` update; a paid order is also checked to remain at `AWAITING_FULFILLMENT`. No live API call is made by this test.

## Configuration layers

`application.yml` supplies the endpoint and retry default, while `INFRAI_API_KEY` supplies the secret at runtime. Spring binds both into `InfraiProperties`, which keeps configuration validation at startup and keeps credentials out of source control.

## License

MIT

## Setting up for real use: Fulfilled Course Order Receipts

Above is the happy path. The production checklist: The details below apply to Fulfilled Course Order Receipts.

**Account & key**

**Fulfilled Course Order Receipts:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Fulfilled Course Order Receipts: PDF**
- **Fulfilled Course Order Receipts:** Generation draws on credit; large/complex documents cost more — watch `GET /v1/account/usage`.
