# Use official OpenJDK 21 image
FROM eclipse-temurin:21-jdk

# Set work directory inside container
WORKDIR /app

# Copy Gradle wrapper files first (to leverage caching)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY system.properties .

# Download Gradle dependencies (cache optimization)
RUN ./gradlew dependencies

# Copy rest of the project
COPY . .

# Build the Spring Boot application
RUN ./gradlew build

# Run the generated JAR file
CMD ["java", "-Dserver.port=$PORT", "-jar", "build/libs/biyahewise-0.0.1-SNAPSHOT.jar"]
