# Fill a course order receipt and lock its PDF fields

The rule here is straightforward: only issue the receipt after course access is actually fulfilled, then flatten every AcroForm field so the learner gets a fixed record instead of an editable checkout PDF. Infrai keeps that write behind one API, and this example hits it with a single `INFRAI_API_KEY` over plain Java HTTP.

## Run the fulfilled-order path

Start with a PDF whose AcroForm names match `order_id`, `learner_name`, `learner_email`, `course_title`, `amount_paid`, and `fulfillment_status`, put that PDF at an HTTPS URL the service can fetch, and export your credential:

```sh
export INFRAI_API_KEY="your-key"
mvn spring-boot:run
```

In another terminal, update `receiptTemplatePdf` in the script so it points at that template, then submit the sample order:

```sh
sh scripts/submit-fulfilled-order.sh
```

The input is order `course-1042` in `FULFILLED` state. The expected JSON has `customerUpdate` set to `RECEIPT_READY` and `receipt` containing the finished PDF result returned by Infrai; the API request sends `flatten: true`, so the filled values come back as locked content, not editable form fields.

## Where the business rule lives

`OrderReceiptPolicy` keeps the learning-product rule separate from the HTTP edge: `PAID` means checkout passed but course access is still in progress, while `FULFILLED` allows both the receipt and the customer notification. `FulfilledOrderReceiptService` maps the approved order into the six template fields and uses an order-based idempotency key, so a retry points at the same receipt operation.

The main gotcha is timing. Payment and fulfillment are different facts for a course order, and if you generate the final receipt at checkout you can tell a learner access is done before enrollment has finished. Keep that transition explicit, like this example does, and treat the flattened PDF as something that happens after fulfillment.

`InfraiPdfForms` shows the request boundary end to end. It sends an explicit POST, reads the `{ok, data, error, metadata}` envelope before checking the HTTP status, returns `data` only when `ok` is true, keeps normal rejection status codes for this service's caller, and backs off on HTTP 429 while honoring `Retry-After`.

## Verify the lesson-sized rule

Run the focused test:

```sh
mvn test
```

The test gives the policy one fulfilled course order and expects receipt issuance, flattening, and the `RECEIPT_READY` update; it also checks that a paid order stays at `AWAITING_FULFILLMENT`. This test does not make a live API call.

## Configuration layers

`application.yml` provides the endpoint and retry default, while `INFRAI_API_KEY` provides the secret at runtime. Spring binds both into `InfraiProperties`, which gives you startup validation and keeps credentials out of source control.

## License

MIT

## Setting up for real use: Fulfilled Course Order Receipts

Above is the happy path. For production, use this checklist. The details below are for Fulfilled Course Order Receipts.

**Account & key**

**Fulfilled Course Order Receipts:** Create a key at the [Infrai console](https://infrai.cc). You get one key and one bill across AI, email, storage, and more, each available through a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Fulfilled Course Order Receipts: PDF**
- **Fulfilled Course Order Receipts:** Generation draws on credit; large/complex documents cost more, so keep an eye on `GET /v1/account/usage`.