# Security Policy

## Supported versions

| Version | Keycloak | Supported |
|---|---|---|
| 0.1.x | 26.7.x | ✅ |

The provider is compiled against a specific Keycloak version. Running it on a different
minor release is unsupported and may fail to load.

## Reporting a vulnerability

Report privately to **joakim@westlund.it**, or open a
[GitHub security advisory](https://github.com/JoWe112/kc-oidc-request-ip-mapper/security/advisories/new).
Please do not open a public issue.

Include the affected version, Keycloak version, and reproduction steps. Expect an
acknowledgement within 7 days.

## Security considerations for operators

This mapper writes a client IP address into tokens. Two things follow:

1. **The claim is only as trustworthy as your proxy configuration.** With
   `KC_PROXY_HEADERS=xforwarded`, Keycloak believes the `X-Forwarded-For` header sent by any
   peer inside `KC_PROXY_TRUSTED_ADDRESSES`. Keep that CIDR as narrow as possible and ensure
   Keycloak is only reachable through the ingress Route — otherwise a caller that can reach
   the Service directly can spoof its own address into the claim.

2. **The claim is telemetry, not an authorization input.** Use it for audit trails and
   anomaly detection. Do not use it as an access-control decision or a second factor.

An IP address is personal data under GDPR. Tokens carrying this claim are subject to the same
handling, retention and logging rules as any other personal data you process.
