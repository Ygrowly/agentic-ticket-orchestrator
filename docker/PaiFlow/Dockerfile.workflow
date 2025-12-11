FROM docker.m.daocloud.io/library/maven:3.9.9-eclipse-temurin-21-noble AS build
WORKDIR /app

# Copy workflow source
COPY core-workflow-java /app/core-workflow-java
WORKDIR /app/core-workflow-java

# Build
RUN mvn clean package -DskipTests

# Runtime stage
FROM docker.m.daocloud.io/library/eclipse-temurin:21-jre-noble
WORKDIR /app

# Set timezone
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# Copy built jar
COPY --from=build /app/core-workflow-java/target/workflow-java.jar /app/workflow-java.jar

# Logs
RUN mkdir -p /app/logs

# Expose port
EXPOSE 7880

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:7880/actuator/health || exit 1

# Entrypoint
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Duser.timezone=Asia/Shanghai", \
    "-jar", \
    "/app/workflow-java.jar"]
