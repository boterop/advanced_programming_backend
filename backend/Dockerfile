ARG JDK_VERSION=25

FROM eclipse-temurin:${JDK_VERSION}-jdk AS base
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
  ./mvnw -B -ntp dependency:go-offline

FROM base AS development
COPY src/ src/
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=dev
CMD ["./mvnw", "spring-boot:run"]

FROM base AS build
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 \
  ./mvnw -B -ntp package -DskipTests

FROM eclipse-temurin:${JDK_VERSION}-jdk AS production
WORKDIR /app
RUN groupadd --system spring \
  && useradd --system --gid spring --home-dir /app --shell /usr/sbin/nologin spring
COPY --from=build /workspace/target/project-0.0.1-SNAPSHOT.jar app.jar
USER spring:spring
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
