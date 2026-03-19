# ==========================================
# ETAPA 1: Construcción (Compilar el código)
# ==========================================
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# 1. Copiamos solo el pom.xml y descargamos dependencias.
# Esto hace que si cambias tu código Java, Docker no tenga que volver 
# a descargar todo el internet cada vez que construyas la imagen.
COPY pom.xml .
RUN mvn dependency:go-offline

# 2. Copiamos el código fuente y compilamos el .jar ignorando los tests
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# ETAPA 2: Ejecución (Contenedor final)
# ==========================================
# Usamos jammy (Ubuntu) por compatibilidad con librerías de imágenes/Tika
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copiamos el .jar ya compilado desde la ETAPA 1
COPY --from=builder /app/target/*.jar app.jar

# Exponemos el puerto de Spring Boot
EXPOSE 8080

# Comando para arrancar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]