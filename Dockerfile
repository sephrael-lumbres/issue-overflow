# Builds the app's jar with the Maven wrapper, then runs it on a JRE as a non-root user.
# Both images are pinned to one Temurin build; change both lines together to update Java.

FROM eclipse-temurin:21.0.13_9-jdk-resolute AS build
WORKDIR /workspace
# dependencies first, so a change to the code alone reuses this layer
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src src
# the tests run in CI, which Railway waits for before it deploys
RUN ./mvnw -B -Dmaven.test.skip=true package \
    && cp target/Issue-Overflow-0.0.1-SNAPSHOT.jar /workspace/app.jar

FROM eclipse-temurin:21.0.13_9-jre-resolute
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/app.jar app.jar
USER app
# A fixed heap and processor count keep the JVM within the service's 1 GB limit.
# An out-of-memory JVM exits, so Railway restarts it. Raise -Xmx only with the limit.
ENV JAVA_TOOL_OPTIONS="-Xmx512m -XX:ActiveProcessorCount=2 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
# -XshowSettings:system logs the memory and CPU limits the JVM detects, once at startup
ENTRYPOINT ["java", "-XshowSettings:system", "-jar", "/app/app.jar"]
