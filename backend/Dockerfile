# Dockerfile = the RECIPE Docker follows to build an image of the Truvira backend.
# Each instruction (FROM, COPY, RUN...) creates a LAYER. Docker caches layers and reuses them when
# nothing in that step changed, so the ORDER of steps matters for fast rebuilds.

# =====================================================================================
# STAGE 1: "build"  - a big image with Maven + the full JDK, used only to COMPILE.
# =====================================================================================
# FROM = start from an existing official image. A fixed version tag (not "latest") means the
# build gives the same result every time.  "AS build" names this stage so stage 2 can copy from it.
FROM maven:3.9.9-eclipse-temurin-17 AS build

# WORKDIR = "cd" into this folder inside the image (it is created if missing).
WORKDIR /build

# --- Layer caching trick ---
# Copy ONLY pom.xml first and download all libraries. This layer is reused as long as pom.xml
# doesn't change, so editing Java code doesn't re-download ~100 MB of libraries every build.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Now copy the source code (changes often -> only the steps below re-run).
COPY src ./src

# Compile, run the 12 unit tests, and package one runnable .jar file.
# If a test fails, the build STOPS: a broken backend never becomes an image.
# -B = batch mode (no colours/progress bars, good for logs).
RUN mvn -B package && cp target/truvira-backend-*.jar /build/app.jar

# =====================================================================================
# STAGE 2: "run"  - a much smaller image with only the Java RUNTIME (JRE) + our .jar.
# No Maven, no compiler, no source code: smaller and safer.
# =====================================================================================
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Create a normal (non-root) user. If someone ever broke into the app, they would NOT be the
# all-powerful root user. (--system = a service account that can't log in.)
RUN groupadd --system truvira && useradd --system --gid truvira --home /app truvira \
    && mkdir -p /app/uploads && chown -R truvira:truvira /app

# Copy ONLY the finished .jar from stage 1. --from=build = "take it from the stage named build".
COPY --from=build --chown=truvira:truvira /build/app.jar /app/app.jar

# Switch to the normal user for everything that follows (including running the app).
USER truvira

# Default settings (docker-compose.yml can override them):
#   SPRING_PROFILES_ACTIVE=docker  -> load application-docker.properties
#   MaxRAMPercentage=75            -> let Java use up to 75% of the container's memory limit
ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

# EXPOSE = documentation: "this container listens on port 8080". (It does NOT open the port to
# the outside world; docker-compose decides that.)
EXPOSE 8080

# The command that runs when the container starts.
# "Exec form" (a JSON list) runs java directly, so it receives Docker's stop signal and can shut
# down cleanly (finish open requests, close database connections).
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
