# StudyFlow

学习任务管理系统，帮助学生进行学习任务规划、优先级管理、任务提醒与学习进度跟踪。

## 技术栈

| 技术 | 版本 |
|------|------|
| Java | 8 |
| Spring Boot | 2.7.18 |
| MyBatis | 2.3.2 |
| MySQL | 8.x |
| Maven | 3.x |

## 快速启动

### 1. 初始化数据库

```bash
mysql -u root -p < src/main/resources/sql/schema.sql
```

### 2. 修改配置

编辑 `src/main/resources/application.yml`，修改数据库用户名和密码：

```yaml
spring:
  datasource:
    username: root
    password: root   # 改为你的密码
```

### 3. 启动项目

```bash
mvn spring-boot:run
```

服务默认运行在 `http://localhost:8080`

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/tasks` | 创建任务 |
| GET | `/api/tasks` | 查询任务列表（支持 `status`、`priority` 筛选） |
| GET | `/api/tasks/{id}` | 查询单个任务 |
| PUT | `/api/tasks/{id}` | 修改任务 |
| DELETE | `/api/tasks/{id}` | 删除任务 |
| PATCH | `/api/tasks/{id}/status` | 更新任务状态 |
| GET | `/api/tasks/stats` | 学习统计 |

## 项目结构

```
StudyFlow/
├── pom.xml
├── src/main/
│   ├── java/com/studyflow/
│   │   ├── StudyFlowApplication.java      # 启动类
│   │   ├── entity/Task.java               # 实体
│   │   ├── mapper/TaskMapper.java         # 数据访问层
│   │   ├── service/TaskService.java       # 业务接口
│   │   ├── service/impl/TaskServiceImpl.java  # 业务实现
│   │   └── controller/TaskController.java # 控制器
│   └── resources/
│       ├── application.yml                # 配置文件
│       ├── mapper/TaskMapper.xml          # SQL 映射
│       └── sql/schema.sql                 # 建表脚本
└── docs/
```
