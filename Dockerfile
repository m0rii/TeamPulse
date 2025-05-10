FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy the Gradle files first to leverage Docker cache
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Download dependencies
RUN ./gradlew --no-daemon dependencies

# Copy the source code
COPY src ./src

# Build the application
RUN ./gradlew --no-daemon build -x test

# Run the application
ENTRYPOINT ["java", "-jar", "/app/build/libs/*.jar"]

# Expose the port
EXPOSE 8080 