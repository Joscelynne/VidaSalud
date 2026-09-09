# Etapa 1: Build de la aplicación Java 21 con Maven
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copiar configuración Maven y descargar dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente y empaquetar ejecutable JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Imagen de ejecución liviana con JRE 21
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar el artifact JAR generado
COPY --from=build /app/target/ms-vidasalud-bff-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080

# Configuración de ejecución con perfil de producción por defecto
ENV SPRING_PROFILES_ACTIVE=dev
ENV AZURE_TENANT_ID=common
ENV AZURE_API_CLIENT_ID=00000000-0000-0000-0000-000000000000

ENTRYPOINT ["java", "-jar", "app.jar"]
