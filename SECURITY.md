# Security Policy

Cypher Engine handles financial risk workflows and may process sensitive invoice-related data. Please report security issues responsibly.

## Supported Versions

The project is currently pre-1.0. Security fixes target the latest `main` branch unless a release branch is explicitly published.

## Reporting a Vulnerability

Do not disclose security vulnerabilities in public issues, pull requests, or discussions.

Send a private report to the project maintainer with:

- A concise description of the vulnerability
- Steps to reproduce
- Potential impact
- Affected components or endpoints
- Any suggested remediation

If a private security advisory workflow is available on the repository host, use it. Otherwise, contact the maintainer directly through the private channel listed on the public repository profile.

## Sensitive Data

Do not include any of the following in reports, issues, tests, fixtures, screenshots, or pull requests:

- Real API keys, JWTs, passwords, or signing secrets
- Real customer, tenant, issuer, payer, or invoice data
- Production NF-e XML files
- Private infrastructure URLs or credentials

Use synthetic examples and redact identifiers whenever possible.

## Security Expectations

Contributions should preserve:

- Tenant isolation
- Authentication and authorization boundaries
- Audit logging for sensitive workflows
- Safe handling of uploaded XML content
- Explicit configuration of production secrets
