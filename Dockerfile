# ========== 第一阶段：构建 ==========
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# 1. 先只拷 pom，预热依赖（源码变动不会重下依赖）
COPY pom.xml .
RUN mvn -B dependency:go-offline -DskipTests

# 2. 再拷源码，编译打包
COPY src ./src
RUN mvn -B clean package -DskipTests

# ========== 第二阶段：运行 ==========
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

ENV TZ=Asia/Shanghai

# 只把 jar 从 builder 拷过来
COPY --from=builder /build/target/short-link-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]