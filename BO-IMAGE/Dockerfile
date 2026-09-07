# ---------- Stage 1: build ----------
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

# Copiamos primero solo lo necesario para resolver dependencias (cachea esta capa)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

# Ahora sí copiamos el código y compilamos
COPY src ./src
RUN ./mvnw package -DskipTests -B

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]