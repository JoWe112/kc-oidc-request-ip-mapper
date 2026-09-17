# syntax=docker/dockerfile:1
#
# Custom Keycloak image with the Request IP Address protocol mapper baked in.
#
#   podman build -f Containerfile -t <registry>/<namespace>/keycloak-request-ip:26.7.4 .
#
# Keep KEYCLOAK_VERSION, the base image tag/digest and the <keycloak.version> property in
# pom.xml in lockstep — a provider JAR built against a different server version may fail to
# load.

# ---------------------------------------------------------------------------------------
# Stage 1 — build the provider JAR
# ---------------------------------------------------------------------------------------
# Pinned to the build host's own platform: the output is a plain, architecture-independent
# JAR, so there is nothing to cross-compile and nothing to gain from emulating the target
# architecture here.
FROM --platform=$BUILDPLATFORM docker.io/library/maven:3.9-eclipse-temurin-21@sha256:82e2ff483b3d5a95c351379f50aff9b50263579117c2b1c090dc7cfcce6216fb AS build

WORKDIR /workspace

# Resolve dependencies in their own layer so source edits do not re-download the world.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

# ---------------------------------------------------------------------------------------
# Stage 2 — Keycloak with the provider installed and the server pre-built
# ---------------------------------------------------------------------------------------
FROM quay.io/keycloak/keycloak:26.7.4@sha256:82a77884f3af238beab1e7afd63b5f530e1b5c0590bd7aa60b40a40463e29b2c

# --chown=1000:0 keeps the JAR readable under OpenShift's arbitrary-UID model: the random
# UID always lands in supplementary group 0, and the default 0644 mode grants it read access.
COPY --from=build --chown=1000:0 /workspace/target/keycloak-request-ip-mapper-*.jar \
     /opt/keycloak/providers/

# `kc.sh build` augments the server with the build-time options below and indexes the
# providers directory. Running it here is what makes `start --optimized` legal at runtime.
#
# Every option passed here MUST NOT be passed again at runtime — with --optimized, Keycloak
# refuses to re-evaluate build-time options.
#
# >>>>>>>>>>>>>>>>>>>>>> ADD OTHER BUILD-TIME OPTIONS HERE <<<<<<<<<<<<<<<<<<<<<<
#   Common ones, uncomment as needed:
#     --health-enabled=true \
#     --metrics-enabled=true \
#     --features=token-exchange,admin-fine-grained-authz \
#     --cache=ispn \
#     --cache-stack=kubernetes \
# >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
RUN /opt/keycloak/bin/kc.sh build \
      --db=postgres

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized"]
