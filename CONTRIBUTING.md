# Contributing

Thanks for your interest in contributing to Cypher Engine.

This project is still early and is being prepared for public open source development. Contributions are welcome when they improve correctness, security, maintainability, documentation, or the developer experience.

## Development Setup

Requirements:

- Java 21
- Maven wrapper from this repository
- Node.js compatible with the frontend toolchain
- PostgreSQL 13+
- Redis

Backend:

```bash
./mvnw test
./mvnw spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run build
npm run dev
```

Copy `frontend/.env.example` to `frontend/.env.local` for local frontend configuration.

## Frontend AI Disclosure

The current frontend implementation was created exclusively with AI assistance.

When contributing to the frontend, please review behavior carefully instead of assuming generated code is correct. Frontend pull requests should prioritize:

- Accessibility
- Responsive behavior
- Type safety
- API contract alignment
- Error and loading states
- Performance
- Maintainable component boundaries

## Before Opening a Pull Request

- Keep the change focused and explain the behavior it changes.
- Add or update tests for backend behavior changes.
- Run `./mvnw test` for backend changes.
- Run `npm run build` from `frontend/` for frontend changes.
- Update documentation when setup, APIs, configuration, or user-visible behavior changes.
- Avoid committing local secrets, real API keys, customer data, production NF-e XMLs, or environment-specific files.

## Code Style

- Prefer small, explicit domain changes over broad refactors.
- Keep risk scoring behavior explainable and testable.
- Keep tenant isolation and auditability intact.
- Use existing project patterns before introducing new abstractions.

## Reporting Issues

When reporting a bug, include:

- Expected behavior
- Actual behavior
- Steps to reproduce
- Relevant logs, request examples, or screenshots
- Backend/frontend versions or commit SHA when available

For security issues, do not open a public issue. Follow [SECURITY.md](SECURITY.md).
