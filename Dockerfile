FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace
COPY backend/.mvn backend/.mvn
COPY backend/mvnw backend/mvnw
COPY backend/pom.xml backend/pom.xml
COPY frontend frontend
COPY database database
COPY backend/src backend/src

WORKDIR /workspace/backend
RUN ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
ENV PORT=8080
COPY --from=build /workspace/backend/target/insightflow-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
