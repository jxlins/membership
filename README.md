# 学会会员信息采集系统

本仓库包含学会会员信息采集系统的前后端代码，支持 PDF 简历上传与解析、会员信息提交与查询。

## 目录结构

- membership_system/ 后端（FastAPI + SQLAlchemy）
- frontend/ 前端（Vite + Vue）
- databases/ 数据库脚本

## 环境要求

- Python 3.10+（建议）
- pip / venv
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
cd membership_system
python -m uvicorn main:app --host 0.0.0.0 --port 8001
```

后端数据库配置在 membership_system/database.py 中。

### 3) 启动前端

```powershell
cd frontend
npm install
npm run dev
```

浏览器访问终端输出的开发服务器地址即可。

如需修改后端地址，可复制 frontend/.env.example 为 frontend/.env.local 并调整其中的变量。

## 配置说明

- 后端数据库连接请在 membership_system/database.py 中配置（主机、端口、用户名、密码）。
- 前端默认通过 `/api` 访问后端，Vite 开发代理目标可在 frontend/.env.local 中调整。

## 常见问题

- 如果出现数据库字段不存在报错，请确认数据库结构已更新为最新的 SQL 脚本。
- 扫描版 PDF 解析依赖后端 OCR 与文本提取逻辑，效果取决于简历版式和清晰度。
