FROM eclipse-temurin:21-jdk-alpine-3.24 AS build
WORKDIR /source
COPY . /source
RUN ./gradlew bootJar -x test

FROM eclipse-temurin:21-jre-alpine-3.24
ARG APP_VERSION=0.0.1-SNAPSHOT
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build --chown=app:app /source/build/libs/*.jar app.jar
LABEL version="${APP_VERSION}"
EXPOSE 8080
USER app
ENTRYPOINT ["java", "-jar", "app.jar"]
