FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN gradle clean installDist --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/install/KotlinBankAPI ./
EXPOSE 8080
ENTRYPOINT ["./bin/KotlinBankAPI"]
