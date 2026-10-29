# Test coverage and expected outcomes

This matrix maps the complete documented API surface to executable checks. The suite uses unique booking data, reads back successful writes, and checks that rejected writes do not alter existing records. The source defines 69 default API test cases, including DataProvider variants, plus six optional strict defect probes; framework unit checks are separate. Each case can send several HTTP requests. These are source counts, not claimed execution results; the current run's TestNG and Allure reports are authoritative.

## Endpoint matrix

| Operation | Positive / contract coverage | Negative / boundary coverage | Test class |
|---|---|---|---|
| `GET /ping` | 201 and `Created` response body | Unsupported POST returns 404; GET has no input parameters to invent negative-input cases for | `HealthTest` |
| `POST /auth` | Valid credentials produce nonempty token; JSON schema | Each independently wrong credential, empty, and omitted credentials return exact 200 + `Bad credentials` without token; malformed JSON returns 400 | `AuthTest` |
| `GET /booking` | IDs schema, owned record present; first name, last name, combined filters; date range | Unique unmatched name gives empty array; date range excludes outside booking | `BookingReadTest` |
| `GET /booking/{id}` | Exact JSON fields/schema and XML body | Deleted, malformed, zero, negative IDs return 404; unsupported Accept returns observed 418 | `BookingReadTest` |
| `POST /booking` | Persisted JSON fields; prices 0, 1, large integer; both deposit booleans; empty/omitted additional needs; XML and form request formats | Each required field omitted individually currently returns 500; malformed JSON returns 400 | `BookingCreateTest` |
| `PUT /booking/{id}` | Token and Basic Auth; full replacement and independent GET | Missing/invalid auth gives 403 without mutation; each missing required field gives 400 without mutation; deleted ID gives 405 | `BookingUpdateTest` |
| `PATCH /booking/{id}` | Each top-level field; full nested dates; zero price and boolean change; unchanged fields preserved; Basic Auth; empty patch | Missing/invalid auth gives 403 without mutation; deleted ID gives 405 | `BookingPatchTest` |
| `DELETE /booking/{id}` | Token and Basic Auth; GET verifies absence | Missing/invalid auth gives 403 and booking remains; repeated deletion gives 405 with booking still absent | `BookingDeleteTest` |
| Complete workflow | Create → find → read → PUT → GET → PATCH → GET → DELETE → GET 404 | Each intermediate result asserted; cleanup still runs after failures | `BookingLifecycleTest` |

## Current behaviour versus strict expectations

Booker is an intentionally buggy learning service. Explicit characterization checks describe a current observable response; they do not classify that response as good API design. Exact status assertions are used, never a permissive `400 OR 500` assertion.

| Observation | Default suite handling | Strict expectation in `KnownDefectsTest` |
|---|---|---|
| OBS-001: incomplete POST returns 500 | Characterization checks identify this server error explicitly | Missing required input should produce 400 |
| OBS-002: documented inclusive check-in filter excludes equal boundary | Normal date cases use dates clearly inside/outside bounds | Equal check-in boundary should include the owned booking |
| OBS-003: one nested date PATCH drops/corrupts the other date | Standard PATCH case sends both nested dates | Changing only check-in should preserve checkout |
| OBS-004: XML response labelled `text/html` | XML body parsed explicitly; header defect not hidden by pretending it is XML | XML response should carry `application/xml` |
| OBS-005: form string `false` becomes boolean true | Normal form-format check uses true; JSON checks exercise false | Form-encoded false should remain false |
| OBS-006: checkout filter behaves opposite to documented lower bound | Unambiguous normal range cases retain observed behavior | Booking whose checkout is after the query date should match the documented lower bound |

The optional `known-defect` group is excluded by the default Maven configuration and is documented visibly in the README. Run it explicitly to reproduce the six strict assertions:

```bash
./mvnw test -Dgroups=known-defect -DexcludedGroups=
```

A failing exit status is expected while those observations remain unresolved. A later fix should make a strict test pass; do not replace the expectation with a broad allowed-status check.

## Test selection

| Group | Purpose |
|---|---|
| `smoke` | Core health/authentication and successful booking operations including lifecycle |
| `regression` | All standard API checks, including negative characterization checks |
| `negative` | Invalid input, unavailable resources, rejected authorization, and unmatched filters |
| `contract` | JSON response schemas and representative supported request/response formats |
| `lifecycle` | Independent end-to-end booking workflow |
| `framework` | Local configuration, sanitization, and helper checks; no live API dependency |
| `known-defect` | Strict expectations that reproduce tracked observations |

## Deliberate limits

- Endpoint coverage means all eight documented operations have tests; it does not mean all conceivable input combinations are exhausted.
- Date-filter boundary discrepancies are reproduced with owned records, not inferred from global seeded data.
- Shared public-demo totals and record IDs are not fixed assertions. Test writes never target seed bookings.
- Load testing and direct database access are outside this functional API suite.
- API reset and public-service availability can fail a real run. Reports retain that evidence; assertions are not weakened to hide an environment issue.
- Form/XML checks use representative combinations. This is not a claim that every encoding and header permutation is covered.
