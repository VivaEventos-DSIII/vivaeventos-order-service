FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Copiar solo el POM de este servicio (independiente del monorepo)
COPY pom.xml .

# Descargar dependencias — esta capa se cachea si el pom.xml no cambia
RUN mvn dependency:go-offline -q

# Copiar fuentes y compilar
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S vivaeventos && adduser -S vivaeventos -G vivaeventos
USER vivaeventos
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
