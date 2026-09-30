# Etapa 1: compilar el backend con Java 21.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copiar Gradle y la configuración de compilación.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle

# Preparar gradlew para ejecutarse en Linux.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Resolver dependencias antes de copiar el código.
RUN ./gradlew dependencies --no-daemon > /dev/null

# Copiar el código y generar el JAR ejecutable.
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# Etapa 2: ejecutar el backend con un entorno Java más pequeño.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Ejecutar sin privilegios de administrador.
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app app

# Traer únicamente el JAR generado en la primera etapa.
COPY --from=build /app/build/libs/*.jar app.jar
USER 10001

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]