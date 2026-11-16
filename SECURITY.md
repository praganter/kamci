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
