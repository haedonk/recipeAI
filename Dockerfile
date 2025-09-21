# ---- Build ----
FROM gradle:8.7-jdk17 AS builder
WORKDIR /app

# Better layer caching for deps:
COPY gradlew ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

# Now copy sources and build
COPY . .
RUN ./gradlew clean bootJar -x test --no-daemon

# ---- Run ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Memory-friendly defaults for small containers
ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -Xms256m -Xmx512m -XX:MaxMetaspaceSize=256m"

# Optional: drop root
RUN adduser -D spring && chown -R spring:spring /app
USER spring

COPY --from=builder /app/build/libs/recipeapp-0.0.2.jar /app/app.jar

# Railway sets PORT; Spring will read it via server.port above
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=production
ENTRYPOINT ["java","-jar","/app/app.jar"]
