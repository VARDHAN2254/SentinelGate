# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

## Reporting a Vulnerability

The SentinelGate team takes the security of our software seriously. If you discover a vulnerability, please report it responsibly:

1. **Do NOT disclose vulnerabilities publicly** (e.g. through public GitHub issues).
2. Report the vulnerability directly to the project maintainer via GitHub Security Advisories or by contacting the repository owner at [https://github.com/VARDHAN2254](https://github.com/VARDHAN2254).
3. Provide a clear description, reproduction steps, and potential impact.
4. We will acknowledge receipt of the report within 48 hours and coordinate a fix and release.

## Security Practices in SentinelGate

- **JWT Signing**: Uses HMAC-SHA512 with configurable secrets via environment variables (`JWT_SECRET`).
- **Password & Key Storage**: All user passwords and API keys are stored using strong BCrypt hashes (never in plaintext).
- **API Key Format**: Machine keys follow the format `sg_live_<prefix>_<secret>` with prefix indexing for rapid revocation without exposing raw secrets.
- **Rate Limiting**: Sliding window rate limits enforced in Redis, protecting endpoints from credential stuffing and volumetric abuse.
- **Role-Based Access Control**: Server-side authorization enforced via Spring Security reactive WebFilters.
