# Stoplight Classroom

A digital implementation of the stoplight active learning method for college classrooms. Teachers create live sessions where students provide real-time comprehension feedback (Green/Yellow/Red) and receive activity-mode notifications.

## Features

- **Real-time comprehension feedback** — Students signal Green (understand), Yellow (partial), or Red (struggling)
- **Activity mode broadcast** — Teachers set Group, Partner, or Silent mode; students see it instantly
- **Multi-session support** — Multiple teachers can run concurrent, isolated sessions
- **Statistics** — Per-session, per-course, and lifetime analytics
- **RESTful API** — Designed for future native clients (macOS, Android)

## Tech Stack

- Java 21, Spring Boot 3, PostgreSQL
- WebSocket (STOMP/SockJS) for real-time updates
- Thymeleaf + vanilla JS for web UI
- Gradle, Docker, GitHub Actions

## Getting Started

See [PLAN.md](PLAN.md) for the full specification and implementation roadmap.

## Deploying to AWS

Production deployment uses ECS Fargate + RDS PostgreSQL + ALB, defined in Terraform under [`infra/terraform/`](infra/terraform/). See [`infra/terraform/README.md`](infra/terraform/README.md) for the full runbook (~$80/mo, ~30 min for first deploy).

The flow at a glance:

1. Bootstrap the Terraform state backend ([`infra/terraform/bootstrap/`](infra/terraform/bootstrap/))
2. Apply ECR, push the first image manually
3. Apply the rest of the stack
4. Wire `AWS_DEPLOY_ROLE_ARN` into GitHub Actions; subsequent pushes to `main` deploy automatically

## License

TBD
