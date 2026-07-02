-- ============================================================
-- Zyn 系统模块 - PostgreSQL Schema
-- ============================================================

-- 清理旧表（按依赖顺序反向删除）
DROP TABLE IF EXISTS sys_user_org CASCADE;
DROP TABLE IF EXISTS sys_role_permission CASCADE;
DROP TABLE IF EXISTS sys_user_role CASCADE;
DROP TABLE IF EXISTS sys_resource CASCADE;
DROP TABLE IF EXISTS sys_permission CASCADE;
DROP TABLE IF EXISTS sys_organization CASCADE;
DROP TABLE IF EXISTS sys_role CASCADE;
DROP TABLE IF EXISTS sys_user CASCADE;

-- ------------------------------------------------
-- 用户表
-- ------------------------------------------------
CREATE TABLE sys_user (
    id              VARCHAR(64)  PRIMARY KEY,        -- 主键（UUID）
    username        VARCHAR(64)  NOT NULL,           -- 登录用户名
    password        VARCHAR(256),                    -- 加密密码
    nickname        VARCHAR(64),                     -- 显示昵称
    real_name       VARCHAR(64),                     -- 真实姓名
    email           VARCHAR(128),                    -- 邮箱
    phone           VARCHAR(32),                     -- 手机号
    avatar          VARCHAR(512),                    -- 头像 URL
    gender          SMALLINT     DEFAULT 0,          -- 性别：0=未知 1=男 2=女
    status          SMALLINT     DEFAULT 1,          -- 状态：0=禁用 1=启用
    login_ip        VARCHAR(64),                     -- 最后登录 IP
    login_time      TIMESTAMP,                       -- 最后登录时间
    pwd_update_time TIMESTAMP,                       -- 密码最后修改时间
    login_attempts  INT          DEFAULT 0,          -- 连续登录失败次数
    lock_time       TIMESTAMP,                       -- 账号锁定时间（null=未锁定）
    remark          VARCHAR(512),                    -- 备注
    version         INT          DEFAULT 0,          -- 乐观锁版本号
    deleted         BOOLEAN      DEFAULT FALSE,      -- 逻辑删除标记
    create_by       VARCHAR(64),                     -- 创建人
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    update_by       VARCHAR(64),                     -- 更新人
    update_time     TIMESTAMP,                       -- 更新时间
    CONSTRAINT uk_sys_user_username UNIQUE (username)
);

CREATE INDEX idx_sys_user_phone ON sys_user(phone);
CREATE INDEX idx_sys_user_email ON sys_user(email);
CREATE INDEX idx_sys_user_status ON sys_user(status) WHERE deleted = FALSE;

-- ------------------------------------------------
-- 角色表
-- ------------------------------------------------
CREATE TABLE sys_role (
    id          VARCHAR(64)  PRIMARY KEY,            -- 主键（UUID）
    role_code   VARCHAR(64)  NOT NULL,               -- 角色编码（唯一）
    role_name   VARCHAR(128),                        -- 角色名称
    role_type   SMALLINT     DEFAULT 1,              -- 类型：1=自定义 2=内置
    sort        INT          DEFAULT 0,              -- 排序
    status      SMALLINT     DEFAULT 1,              -- 状态：0=禁用 1=启用
    data_scope  SMALLINT     DEFAULT 1,              -- 数据权限：1=全部 2=本部门 3=本部门及下级 4=仅本人
    remark      VARCHAR(512),                        -- 备注
    version     INT          DEFAULT 0,
    deleted     BOOLEAN      DEFAULT FALSE,
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP,
    CONSTRAINT uk_sys_role_role_code UNIQUE (role_code)
);

CREATE INDEX idx_sys_role_status ON sys_role(status) WHERE deleted = FALSE;

-- ------------------------------------------------
-- 权限/菜单表
-- ------------------------------------------------
CREATE TABLE sys_permission (
    id          VARCHAR(64)  PRIMARY KEY,            -- 主键（UUID）
    parent_id   VARCHAR(64)  DEFAULT '0',            -- 父权限 ID
    perm_code   VARCHAR(128) NOT NULL,               -- 权限编码（唯一）
    perm_name   VARCHAR(128),                        -- 权限名称
    perm_type   SMALLINT     NOT NULL,               -- 类型：1=目录 2=菜单 3=按钮
    path        VARCHAR(256),                        -- 路由路径
    sort        INT          DEFAULT 0,              -- 排序
    visible     BOOLEAN      DEFAULT TRUE,           -- 菜单是否可见
    status      SMALLINT     DEFAULT 1,              -- 状态：0=禁用 1=启用
    remark      VARCHAR(512),                        -- 备注
    version     INT          DEFAULT 0,
    deleted     BOOLEAN      DEFAULT FALSE,
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP,
    CONSTRAINT uk_sys_permission_perm_code UNIQUE (perm_code)
);

CREATE INDEX idx_sys_permission_parent_id ON sys_permission(parent_id);
CREATE INDEX idx_sys_permission_type ON sys_permission(perm_type) WHERE deleted = FALSE;

-- ------------------------------------------------
-- 组织/部门表
-- ------------------------------------------------
CREATE TABLE sys_organization (
    id          VARCHAR(64)  PRIMARY KEY,            -- 主键（UUID）
    parent_id   VARCHAR(64)  DEFAULT '0',            -- 父组织 ID
    org_code    VARCHAR(64)  NOT NULL,               -- 组织编码（唯一）
    org_name    VARCHAR(128),                        -- 组织名称
    org_type    SMALLINT     DEFAULT 1,              -- 类型：1=公司 2=部门 3=团队
    leader_id   VARCHAR(64),                         -- 负责人用户 ID
    phone       VARCHAR(32),                         -- 联系电话
    email       VARCHAR(128),                        -- 联系邮箱
    sort        INT          DEFAULT 0,              -- 排序
    status      SMALLINT     DEFAULT 1,              -- 状态：0=禁用 1=启用
    remark      VARCHAR(512),                        -- 备注
    version     INT          DEFAULT 0,
    deleted     BOOLEAN      DEFAULT FALSE,
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP,
    CONSTRAINT uk_sys_organization_org_code UNIQUE (org_code)
);

CREATE INDEX idx_sys_organization_parent_id ON sys_organization(parent_id);

-- ------------------------------------------------
-- API 资源表
-- ------------------------------------------------
CREATE TABLE sys_resource (
    id             VARCHAR(64)  PRIMARY KEY,         -- 主键（UUID）
    permission_id  VARCHAR(64),                      -- 关联权限 ID
    res_name       VARCHAR(128),                     -- 资源名称
    res_type       SMALLINT     DEFAULT 1,           -- 类型：1=API 2=按钮
    request_method VARCHAR(16),                      -- HTTP 请求方法
    request_path   VARCHAR(256),                     -- API 路径
    status         SMALLINT     DEFAULT 1,           -- 状态：0=禁用 1=启用
    remark         VARCHAR(512),                     -- 备注
    version        INT          DEFAULT 0,
    deleted        BOOLEAN      DEFAULT FALSE,
    create_by      VARCHAR(64),
    create_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by      VARCHAR(64),
    update_time    TIMESTAMP,
    CONSTRAINT uk_sys_resource_method_path UNIQUE (request_method, request_path)
);

CREATE INDEX idx_sys_resource_permission_id ON sys_resource(permission_id);
CREATE INDEX idx_sys_resource_path ON sys_resource(request_path, request_method) WHERE deleted = FALSE;

-- ------------------------------------------------
-- 用户-角色关联表（物理删除）
-- ------------------------------------------------
CREATE TABLE sys_user_role (
    id          VARCHAR(64) PRIMARY KEY,             -- 主键（UUID）
    user_id     VARCHAR(64) NOT NULL,                -- 用户 ID
    role_id     VARCHAR(64) NOT NULL,                -- 角色 ID
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_user_role UNIQUE (user_id, role_id)
);

CREATE INDEX idx_sys_user_role_user_id ON sys_user_role(user_id);
CREATE INDEX idx_sys_user_role_role_id ON sys_user_role(role_id);

-- ------------------------------------------------
-- 角色-权限关联表（物理删除）
-- ------------------------------------------------
CREATE TABLE sys_role_permission (
    id            VARCHAR(64) PRIMARY KEY,           -- 主键（UUID）
    role_id       VARCHAR(64) NOT NULL,              -- 角色 ID
    permission_id VARCHAR(64) NOT NULL,              -- 权限 ID
    create_by     VARCHAR(64),
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_role_permission UNIQUE (role_id, permission_id)
);

CREATE INDEX idx_sys_role_permission_role_id ON sys_role_permission(role_id);
CREATE INDEX idx_sys_role_permission_perm_id ON sys_role_permission(permission_id);

-- ------------------------------------------------
-- 用户-组织关联表（物理删除）
-- ------------------------------------------------
CREATE TABLE sys_user_org (
    id          VARCHAR(64) PRIMARY KEY,             -- 主键（UUID）
    user_id     VARCHAR(64) NOT NULL,                -- 用户 ID
    org_id      VARCHAR(64) NOT NULL,                -- 组织 ID
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_user_org UNIQUE (user_id, org_id)
);

CREATE INDEX idx_sys_user_org_user_id ON sys_user_org(user_id);
CREATE INDEX idx_sys_user_org_org_id ON sys_user_org(org_id);
