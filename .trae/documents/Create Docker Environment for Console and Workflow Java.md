I have analyzed the project structure and requirements. Here is the plan to create a complete Docker packaging and running environment for `console` (backend & frontend) and `core-workflow-java`.

I will create a new directory `docker/PaiFlow` and place all necessary files there.

### 1. Create Dockerfiles for Multi-stage Builds
To ensure a "one-command" experience without requiring local Java/Node environment, I will create new Dockerfiles that include the build step.

*   **`docker/PaiFlow/Dockerfile.backend`**:
    *   **Build Stage**: Uses `maven:3.9.9-eclipse-temurin-21-noble`. Copies `console/backend` source code and runs `mvn clean package`.
    *   **Runtime Stage**: Uses `eclipse-temurin:21-jre-noble`. Copies the compiled `hub-server.jar`. Exposes port 8080.
*   **`docker/PaiFlow/Dockerfile.frontend`**:
    *   **Build Stage**: Uses `node:18-alpine`. Copies `console/frontend` source code and runs `npm run build-prod`.
    *   **Runtime Stage**: Uses `nginx:alpine`. Copies the built static files to `/var/www`. Includes `docker-entrypoint.sh` for runtime configuration.
*   **`docker/PaiFlow/Dockerfile.workflow`**:
    *   **Build Stage**: Uses `maven:3.9.9-eclipse-temurin-21-noble`. Copies `core-workflow-java` source code and runs `mvn clean package`.
    *   **Runtime Stage**: Uses `eclipse-temurin:21-jre-noble`. Copies `workflow-java.jar`. Exposes port 7880.

### 2. Create `docker-compose.yaml`
I will create `docker/PaiFlow/docker-compose.yaml` with the following services:

*   **Infrastructure**:
    *   `mysql`: MySQL 8.4 (Port 3306)
    *   `redis`: Redis 7 (Port 6379)
    *   `minio`: MinIO (Port 9000/9001)
*   **Applications**:
    *   `console-frontend`: Builds from `Dockerfile.frontend`. Maps port 3000 -> 80 (or 1881 as per config).
    *   `console-hub`: Builds from `Dockerfile.backend`. Depends on MySQL, Redis, MinIO.
    *   `core-workflow-java`: Builds from `Dockerfile.workflow`. Depends on MySQL, Redis.

### 3. Create Tutorial (README.md)
I will create `docker/PaiFlow/README.md` containing:
*   **Prerequisites**: Docker & Docker Compose.
*   **Step-by-step Guide**:
    1.  Navigate to `docker/PaiFlow`.
    2.  Run `docker-compose up -d --build`.
    3.  Access the services (Frontend, Backend, Workflow).
*   **Configuration**: Explanation of environment variables if customization is needed.

This approach ensures that anyone can build and run the entire stack with a single `docker-compose up` command, meeting the "complete tutorial" requirement.
