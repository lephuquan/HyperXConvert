# Sử dụng image Java JDK để build Spring Boot
FROM openjdk:17-jdk-slim

# Tạo thư mục trong container
WORKDIR /app

# Copy file JAR đã build
COPY target/*.jar app.jar

# Expose cổng mà app chạy (thường là 8080)
EXPOSE 8080

# Lệnh chạy app
ENTRYPOINT ["java", "-jar", "app.jar"]
