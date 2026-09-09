FROM maven:3.9.8-eclipse-temurin-17-alpine AS build
WORKDIR /workspace

COPY pom.xml ./
COPY .dockerignore ./.dockerignore
RUN mkdir -p backend frontend \
    && printf 'node_modules/\n.next/\ndist/\nbuild/\ncoverage/\n.vite/\n.cache/\n*.log\n*.local\n' > .gitignore \
    && cp .dockerignore backend/.dockerignore \
    && cp .dockerignore frontend/.dockerignore
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17.0.10_7-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/target/*.jar /app/app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
