FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/studentgig-1.0.0.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
