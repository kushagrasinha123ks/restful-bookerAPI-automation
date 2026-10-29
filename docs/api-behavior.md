# API behavior and known defects

Restful Booker is deliberately a buggy training application. Test expectations are drawn from its [documentation](https://restful-booker.herokuapp.com/apidoc/index.html), [generated API definitions](https://github.com/mwinteringham/restful-booker/blob/main/public/apidoc/api_data.json), and [route implementation](https://github.com/mwinteringham/restful-booker/blob/main/routes/index.js), then checked against the configured deployment.

An implementation observation is evidence of current behavior, not permission to silently redefine the desired contract.

The source review and focused live probes were performed on **29 October 2026**. The live write probes created unique disposable bookings and removed those bookings afterward. These observations describe the public deployment at that point; tests against another deployment may expose different behavior.

## Application-specific responses

| Situation | Established behavior to account for |
|---|---|
| Successful health check | HTTP 201 |
| Successful booking creation | HTTP 200 with `bookingid` and `booking` |
| Successful deletion | HTTP 201 |
| Invalid credentials | HTTP 200 with a `reason` field; a 200 response alone does not mean authentication succeeded |
| Missing booking on GET | HTTP 404 |
| Missing or invalid authorization on a protected write | HTTP 403 |
| Missing booking on PUT/PATCH/DELETE | The route implementation uses HTTP 405 |
| JSON authentication token | Pass it as `Cookie: token=<value>` for protected writes |
| Alternative protected-write authentication | Basic authentication is also supported |
| XML requests | Documented request media type is `text/xml`; response negotiation uses `application/xml` |

Treat the exact response as endpoint-specific. Do not replace assertions with a broad allowance such as "any 2xx" or "any 4xx/5xx."

Zero is a valid JSON price, and JSON `false` is a valid deposit value; focused probes verified that both survive a create/read cycle. XML requests require particular care: the current route unwraps the XML booking object only when the request `Content-Type` is exactly `text/xml`, so the client must not automatically append a charset to that media type.

## Known-defect probes

The `known-defect` group contains stricter assertions that expose discrepancies, and is excluded from the default suite explicitly. Normal negative tests can characterize the established error behavior; their names and this document explain the deviation. Known-defect probes remain executable and fail while the issue remains.

```bash
./mvnw clean test -Dgroups=known-defect -DexcludedGroups=
./mvnw allure:report
```

### Missing required creation fields can return HTTP 500

Required-field validation should report a client error. The POST route currently returns a server error on some missing-field payloads. A strict reproducer expects HTTP 400; a characterization test can separately assert the current exact response. This distinction prevents a known server error from being represented as correct validation design.

### Date-filter documentation and implementation disagree

The generated API definitions describe inclusive lower-bound comparisons. The implementation uses a strict `checkin > supplied date` comparison and a strict `checkout < supplied date` comparison against an ISO timestamp. Equality boundaries and checkout direction therefore need focused tests, using owned bookings around those boundaries.

Live probing excluded an owned record when its check-in equaled the filter date and included it when the threshold was the day before. Checkout equality did not behave like a simple strict date comparison: both equal-day and next-day bounds included the record. The implementation compares stored date strings with ISO timestamps, so reading the comparison operator alone is not enough to predict every live boundary.

The separate OBS-006 checkout-direction probe creates an owned booking checking out on `2030-06-15` and queries `checkout=2030-06-14`. The documented inclusive lower-bound rule should include it; the current deployment excludes it. This assertion is distinct from the OBS-002 check-in equality issue.

Strict probes check the documented expectation. Other date-filter scenarios use unambiguous dates to verify that owned records are included or excluded, without depending on the public service's complete data set.

### A partial nested-date patch can corrupt its sibling

A live PATCH containing only `bookingdates.checkin` returned HTTP 200 but changed the untouched checkout to `0NaN-aN-aN`. A correct partial update should preserve the existing checkout value. Passing regression scenarios that change dates send both nested dates together; a strict reproducer checks the missing-sibling preservation requirement.

### XML response body and media type disagree

A live GET with `Accept: application/xml` returned an XML body with a `text/html` response header. A representation check can parse that body explicitly to characterize current behavior. A strict media-type check should require the negotiated XML media type and expose the discrepancy.

### `additionalneeds` is effectively optional

The generated definitions mark `additionalneeds` as required. The validator does not enforce it, and a live creation request omitting it returned HTTP 200 with the field absent from the returned booking. This is a documentation/implementation mismatch; it should not be confused with one of the core required fields whose absence triggers the creation-validation defect.

### A form-encoded false deposit value can become true

The validator converts input using JavaScript's `Boolean(value)`. The nonempty string `"false"` received from a form body therefore becomes `true`. The OBS-005 probe submits form-encoded `depositpaid=false` and requires the returned value to remain false. Standard JSON scenarios verify a genuine boolean false independently; the representative passing form-format scenario uses true.

## Shared-environment limitations

The [service homepage](https://restful-booker.herokuapp.com/) states that records reset every ten minutes. Existing IDs may disappear during a scenario, and other users can create or modify data. This is why tests generate their own identifiers, avoid global-count assertions, run with modest concurrency, and attach HTTP evidence.

A transient outage, reset, or DNS failure is an execution failure to investigate. It should not be converted into a passing test. For repeatable experiments, configure a compatible instance you control with `BOOKER_BASE_URL`.
