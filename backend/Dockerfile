# ----- Dockerfile v1.0: Basic -----

# Sử dụng image Java JDK để build Spring Boot, hệ điều hành nền tảng (ở đây là Linux/Debian slim)
FROM openjdk:17-jdk-slim

# Cài các công cụ hệ thống cần thiết
# - python3, pip: để chạy pdf2docx (chuyển PDF sang DOCX)
# - ffmpeg: chuyển đổi/nén video, audio (MP4 ↔ MP3, nén video)
# - qpdf: nén file PDF
# - curl: để cài Node.js
# - libreoffice: chuyển DOCX sang PDF
RUN apt-get update && \
    apt-get install -y python3 python3-pip ffmpeg qpdf curl libreoffice && \
    apt-get clean

# Cài pdf2docx (CLI sẽ nằm ở /root/.local/bin)
# - pdf2docx: chuyển PDF sang DOCX
RUN pip3 install --user pdf2docx

# Cài Node.js (bản 18 LTS) và sharp-cli
# - sharp-cli: chuyển đổi ảnh JPG ↔ PNG
RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs && \
    npm install -g sharp-cli

# Thêm /root/.local/bin vào PATH cho mọi shell
ENV PATH="/root/.local/bin:${PATH}"

# Tạo thư mục trong container
WORKDIR /app

# Copy file JAR đã build
COPY target/*.jar app.jar

# Expose cổng ứng dụng và cổng debug
EXPOSE 8080 5005

# Cấu hình remote debug JVM qua port 5005
# -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
# Cho phép IntelliJ hoặc IDE khác kết nối debug từ xa
ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-jar", "app.jar"]


# ----- Dockerfile v2.0: low image size -----

## Stage 1: Build Spring Boot JAR
## ======================
#FROM maven:3.9.8-eclipse-temurin-17 AS builder
#WORKDIR /app
#COPY . .
#RUN mvn clean package -DskipTests
#
## ======================
## Stage 2: Runtime image
## ======================
#FROM openjdk:17-jdk-slim AS runtime
#
## Cài các công cụ cần thiết, gom thành 1 RUN để ít layer hơn
#RUN apt-get update && apt-get install -y --no-install-recommends \
#    python3 python3-pip ffmpeg qpdf curl libreoffice \
#    && apt-get clean \
#    && rm -rf /var/lib/apt/lists/*
#
## Cài pdf2docx
#RUN pip3 install --no-cache-dir --user pdf2docx
#
## Cài Node.js 18 LTS + sharp-cli
#RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash - \
#    && apt-get install -y --no-install-recommends nodejs \
#    && npm install -g sharp-cli \
#    && rm -rf /var/lib/apt/lists/*
#
## Thêm pdf2docx CLI vào PATH
#ENV PATH="/root/.local/bin:${PATH}"
#
#WORKDIR /app
#COPY --from=builder /app/target/*.jar app.jar
#
#EXPOSE 8080 5005
#
#ENTRYPOINT ["java", \
#  "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", \
#  "-jar", "app.jar"]
# ======================

