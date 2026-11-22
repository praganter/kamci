# Security Policy

This is a portfolio/showcase project and is not run in production. Security reports are still welcome.

## Reporting a vulnerability

Please report privately, either through GitHub's private vulnerability reporting (Security tab ->
"Report a vulnerability") or by email to byetgin@engineer.com. Do not open a public issue. Include the affected component (`backend/`, `infra/`, `config/`,
`k8s/`, `mobile/`), reproduction steps and impact.

## Secrets

No real credentials belong in this repository. Values such as `CHANGE_ME`, `dev:dev` and the
`.env.example` files are placeholders. Real secrets are supplied through environment variables or
sealed secrets (see `k8s/README.md`). If you find something that looks like a live credential,
report it as above.

## Internal endpoints

Paths with an `internal` segment (service-to-service calls) are protected in two layers:

1. The gateway rejects them, so they are not reachable from outside.
2. Each service requires an `X-Internal-Token` header equal to `INTERNAL_API_TOKEN` (constant-time comparison,
   403 otherwise). OpenFeign clients add the header automatically for `internal` paths.

Limitations: the token is one shared secret, not a per-service identity, and it is not rotated automatically.
If `INTERNAL_API_TOKEN` is not set, a public dev default (`dev-internal-token`) is used and a warning is logged.
Set a long random value everywhere except a local demo; with the default, the second layer is only as strong
as the network boundary.

## Store purchase notifications

`auth-service` receives server-to-server notifications from the stores on two public gateway paths,
`/subscription/apple/webhook` and `/subscription/google/webhook`. They carry no user JWT, so each request has to prove
where it came from:

- **Apple** (App Store Server Notifications): the body is a signed JWS. The signature and certificate chain are
  verified against a pinned Apple root CA, and the bundle ID (`APPLE_BUNDLE_ID`) and environment must match. Sandbox
  notifications are accepted only when `APPLE_ALLOW_SANDBOX=true`.
- **Google** (Play real-time developer notifications): delivered by a Pub/Sub push subscription that signs an OIDC
  token. The token's audience (`GOOGLE_PUBSUB_AUDIENCE`) and service-account e-mail
  (`GOOGLE_PUBSUB_SERVICE_ACCOUNT_EMAIL`) must match, and only purchases for `GOOGLE_PLAY_PACKAGE_NAME` are accepted.
- A notification that fails these checks is answered with 403 and nothing unsigned is trusted. The settings are
  optional so the service still starts without them; while a store is not configured, its endpoint answers with an explicit error (403 or 503).

Receipts sent by the app to `/subscription/verify` are checked with the store (Apple, or the Google Play Developer API
with a Play service account that is separate from the Firebase key) and require a normal user JWT.
