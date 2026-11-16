# AI-assisted development workflow

This project was built with several AI coding agents working in parallel, coordinated by one orchestrator session.
This page describes the process, not the tooling setup.

## Roles

| Agent | Scope |
|---|---|
| orchestrator | Receives requests, splits them by area, dispatches, tracks progress and reports. Does not write code or deploy. |
| backend | Spring Boot services, their tests and builds. |
| infra | Gateway, service discovery, config server, observability. |
| config | Central configuration files (routing, rate limits, per-service settings). |
| deploy | Kubernetes manifests, secret handling, image tags, rollouts. |
| mobile | The Flutter app. |

Each agent works in its own area and has permissions limited to it. An agent that is denied something does not ask
another agent to do it; the request goes back to the human.

## Flow of a cross-cutting change

A feature touching several layers is split into per-agent tasks and run in dependency order:
backend (endpoint, events) -> config -> deploy -> infra (gateway routes) -> mobile (integration and UI).
Each step must finish and verify (build, tests, rollout) before the next starts. If a step fails the chain stops and
the failure is reported instead of pushing on.

## Coordination

- Tasks are written to be self-contained: files, service, acceptance criteria.
- After each commit an agent appends one line to a shared, append-only HANDOFF log: time, agent, repo and short hash,
  summary, and which other agents are affected. Agents read the tail of the log at the start of a session.
- When a change affects another agent's area, that agent is told directly and the change is not assumed to be safe.
- Every report states what was done, how it was verified and what remains.

## Provenance and review

- Commits written with an agent carry a `Co-Authored-By` trailer.
- A human approves anything outward facing, such as publishing, deleting or writing outside an agent's own area.
- History is scanned with gitleaks before publishing.
