FROM node:22-bookworm-slim AS web
WORKDIR /web
COPY frontend/package*.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /src

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
COPY --from=web /web/dist ./frontend/dist
RUN mvn -B -q clean package -DskipTests -Dexec.skip=true

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /src/target/SmartCampus-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
