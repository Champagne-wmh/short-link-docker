# ========== 第一阶段：构建 ==========
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline -DskipTests

COPY src ./src
RUN mvn -B clean package -DskipTests

# ========== 第二阶段：运行 ==========
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 1. 设置时区（解决日志差 8 小时的问题）
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 安装 curl 用于健康检查（顺手清理 apt 缓存减小体积）
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 2. 创建非 root 用户（使用固定 UID/GID 1000，符合企业规范）
RUN groupadd --gid 1000 appgroup && \
    useradd -m -u 1000 -g appgroup appuser

# 3. 拷包并赋予 appuser 权限（关键！否则 appuser 可能无权限读写）
# 注意这里的 --chown=appuser:appgroup
COPY --from=builder --chown=appuser:appgroup /build/target/short-link-1.0.0.jar app.jar

# 4. 切换到非 root 用户（放在 COPY 之后，保证 COPY 有权限）
USER appuser

EXPOSE 8080

# 5. 健康检查（每 30 秒检查一次首页能否访问，超时 3 秒，重试 3 次）
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD curl -f http://localhost:8080/index.html || exit 1

# 启动命令
ENTRYPOINT ["java", "-jar", "/app/app.jar"]