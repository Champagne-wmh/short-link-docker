# 短链接系统 short-link

一个**简历级 Java 后端项目**，用「发号器 + Base62 + 302 跳转」实现短链的生成与跳转闭环，并预留了 Redis 多级缓存、MQ 异步统计、分库分表的演进路径。

> 对应需求文档：《短链接系统 需求说明书 v1.0》（桌面同名 HTML）

## 一、技术栈

| 层次 | 技术 | 版本 |
|---|---|---|
| 语言 | Java | 17（LTS） |
| 框架 | Spring Boot | 3.2.5 |
| ORM | MyBatis-Plus | 3.5.7 |
| 存储 | MySQL | 8.0 |
| 接口文档 | Knife4j（springdoc） | 4.4.0 |
| 构建 | Maven | 3.9.9 |

## 二、已实现功能（V1）

- [x] 短链生成：号段发号器生成唯一 ID → Base62 编码为短码 → 落库
- [x] 短链跳转：302 临时重定向（保证每次点击都回到后端，可统计）
- [x] 自定义短码：4-16 位字母数字，唯一索引兜底防冲突
- [x] 有效期管理：永久 / 自定义到期，惰性删除（跳转时判断）
- [x] 短链管理：按分组查询、启用/禁用、修改目标链接、逻辑删除
- [x] 接口文档：Knife4j 中文界面，访问 `/doc.html`

## 三、快速开始

swagger：
http://localhost:8080/swagger-ui/index.html
MQ:
http://localhost:15672
前端：
http://localhost:8080/index.html

### 1. 环境要求

- JDK 17（本机已装：`C:\Program Files\Java\jdk-17`）
- Maven 3.9（本机：`E:\tools\apache-maven-3.9.9`，已配阿里云镜像）
- MySQL 8.0（本机已装并运行，账号 root / 密码 123456）

### 2. 初始化数据库

```bash
mysql -uroot -p123456 < db/schema.sql
```

### 3. 编译

```bash
# Windows（PowerShell），务必用 mvn.cmd 而不是 mvn
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn clean package -DskipTests
```

### 4. 启动

```bash
java -jar target/short-link-1.0.0.jar
```

或用 IDE 直接运行 `ShortLinkApplication`。

### 5. 体验

```bash
# 生成短链
curl -X POST http://localhost:8080/api/short-link \
  -H "Content-Type: application/json" \
  -d '{"originUrl":"https://www.baidu.com"}'

# 返回示例
# {"code":0,"message":"success","data":{"shortCode":"1V6K3","shortUrl":"http://localhost:8080/1V6K3","originUrl":"https://www.baidu.com"}}

# 浏览器访问短链，观察 302 跳转
# http://localhost:8080/1V6K3
```

接口文档：<http://localhost:8080/doc.html>

## 四、目录结构

```
short-link
├── db/schema.sql                     # 建表脚本
├── pom.xml
└── src/main
    ├── java/com/shortlink
    │   ├── ShortLinkApplication.java # 启动类
    │   ├── controller/               # 接口层（REST）
    │   ├── service/                  # 业务层
    │   ├── manager/                  # 业务核心层（发号器）
    │   ├── mapper/                   # 数据访问层
    │   ├── entity/                   # 实体
    │   ├── dto/                      # 入参/出参
    │   ├── common/                   # 统一返回 + 全局异常
    │   └── util/                     # Base62 工具
    └── resources/application.yml
```

## 五、核心设计

### 5.1 写链路（生成短链）

```
校验 URL → 发号器取唯一 ID → Base62 编码为短码 → 唯一索引落库 → 返回短链
```

### 5.2 读链路（跳转）

```
请求短码 → 按唯一索引查 MySQL → 302 + Location
```

> V1 直接查库。V2 会在前面叠加「Caffeine 本地缓存 → Redis → 布隆过滤器」三级缓存。

### 5.3 为什么用 302 而不是 301

301 会让浏览器永久缓存跳转，之后点击**不再请求后端**，导致统计失效、无法下线、无法改目标。短链系统的商业价值在数据，所以选 302。

