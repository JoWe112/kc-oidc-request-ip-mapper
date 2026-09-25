# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Accepted CVE-2026-84939 (FreeMarker 2.3.32) after verifying against the Keycloak 26.7.4
  sources that the path-traversal vector is not reachable: Keycloak never passes a Locale to
  FreeMarker's template lookup, and user-supplied locales are allowlisted against the realm's
  supported locales.
- Accepted four further base-image CVEs in `.trivyignore` (two unfixable `pcre2`, two
  `bcprov-jdk18on` awaiting a Keycloak release bundling 1.85), all expiring 2026-10-17
  alongside the existing entries.

- `.trivyignore` recording the base-image findings that cannot be fixed downstream, each with
  a justification and a 30-day expiry, wired into both the CI scan and the documented local
  scan command.
- Target Keycloak **26.7.4** instead of 26.6.4. The 26.6.x line ended at 26.6.4 and carries
  CVE-2026-18963 (unauthenticated account takeover via the reset-credentials flow), for which
  no 26.6.x fix was ever published. 26.7.2 is the first release containing the fix.

## [0.1.0] - 2026-09-17

### Added

- `RequestIpAddressMapper`: OIDC protocol mapper that adds the IP address of the HTTP request
  issuing the token as a configurable claim. Supports access token, ID token, userinfo and
  token introspection.
- Reads the address from the current `KeycloakContext` connection rather than the user
  session, so the claim is accurate on `refresh_token` grants.
- Multi-stage `Containerfile` producing a custom Keycloak 26.6.4 image with the provider
  installed and `kc.sh build` pre-run for `start --optimized`.
- Plain Deployment/Service/Route manifests including the `KC_PROXY_HEADERS` and
  `KC_PROXY_TRUSTED_ADDRESSES` configuration required for the claim to carry the real client
  IP behind an OpenShift Route.

[Unreleased]: https://github.com/JoWe112/kc-oidc-request-ip-mapper/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/JoWe112/kc-oidc-request-ip-mapper/releases/tag/v0.1.0
