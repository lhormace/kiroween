FROM public.ecr.aws/docker/library/maven:3.9-amazoncorretto-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage - Use AWS Lambda base image with Web Adapter
FROM public.ecr.aws/lambda/java:17

# Copy the Spring Boot executable JAR
COPY --from=build /app/target/health-chat-advisor-1.0.0-SNAPSHOT-exec.jar ${LAMBDA_TASK_ROOT}/app.jar

# Set the handler
CMD ["org.springframework.cloud.function.adapter.aws.FunctionInvoker::handleRequest"]
