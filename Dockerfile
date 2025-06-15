# Use official OpenJDK 21 image
FROM eclipse-temurin:21-jdk

# Set working directory inside container
WORKDIR /app

# Copy only the Gradle files first (for Docker build caching)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY system.properties .

# Download Gradle dependencies (caching layer)
RUN ./gradlew dependencies --no-daemon

# Copy rest of the project after dependencies
COPY . .

# Build application (bootJar)
RUN ./gradlew bootJar --no-daemon

# Run the application
CMD ["java", "-Dserver.port=$PORT", "-jar", "build/libs/biyahe-wise-backend-0.0.1-SNAPSHOT.jar"]
