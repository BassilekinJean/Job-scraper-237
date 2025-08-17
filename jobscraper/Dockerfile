FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY pom.xml .
COPY mvnw .             
COPY .mvn .mvn/        

RUN chmod +x mvnw

COPY src ./src

RUN ./mvnw package -DskipTests


FROM eclipse-temurin:21-jre

RUN adduser --system --group appuser

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

USER appuser

EXPOSE 8080

LABEL maintainer="Bassilekin jean 21T2352"

ENTRYPOINT ["java", "-jar", "gestio-pro.jar"]