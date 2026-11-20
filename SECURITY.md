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
