# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

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
