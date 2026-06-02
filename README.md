# 学会会员信息采集系统

本仓库包含学会会员信息采集系统的前后端代码，支持 PDF 简历上传与解析、会员信息提交与查询。

## 目录结构

- Membership/ 后端（Spring Boot）
- frontend/ 前端（Vite + Vue）
- databases/ 数据库脚本

## 环境要求

- JDK 17+（或与项目当前配置匹配的 JDK 版本）
- Maven 3.8+
- Node.js 18+（或与前端依赖兼容的版本）
- MySQL 8+

## 本地运行

### 1) 初始化数据库

在 MySQL 中创建数据库后，执行初始化脚本：

```sql
-- 建议先创建数据库
CREATE DATABASE IF NOT EXISTS membership_system DEFAULT CHARSET utf8mb4;
USE membership_system;

-- 执行 databases/society_member_profile.sql
```

### 2) 启动后端

```powershell
cd Membership
mvn spring-boot:run
```

后端默认读取 Membership/src/main/resources/application.yaml 中的数据库配置。

### 3) 启动前端

```powershell
cd frontend
npm install
npm run dev
```

浏览器访问终端输出的开发服务器地址即可。

## 配置说明

- 后端数据库连接请在 application.yaml 中配置（主机、端口、用户名、密码）。
- 文件上传目录与公共访问前缀可在 application.yaml 的 society.file 节点中调整。

## 常见问题

- 如果出现数据库字段不存在报错，请确认数据库结构已更新为最新的 SQL 脚本。
- 扫描版 PDF 暂不支持 OCR 解析。
