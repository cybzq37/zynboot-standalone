-- ============================================================
-- Zyn 系统模块 - 初始化数据
-- ============================================================

-- 内置角色
INSERT INTO sys_role (id, role_code, role_name, role_type, sort, status, data_scope) VALUES
('ae41e0470af04c7a8e3b3352bcf33afb', 'root',  '超级用户', 2, 0, 1, 1),
('cb05b80a47244344a812c33c3356ed4f', 'admin', '管理员',   2, 1, 1, 1),
('699ba2e9da5d44f1bd06440d22be651a', 'user',  '普通用户', 2, 2, 1, 4);

-- 超级用户（root，绕过权限检查）
-- 密码: Zyn@secure#99 (BCrypt)
INSERT INTO sys_user (id, username, password, nickname, real_name, status) VALUES
('3c3be4ff7b1b41989494ce19eb90e3fe', 'root', '$2b$12$95rkOnPuCUUIPGfxgWkgqeKo67yAdOWLQ973teSG6sSaryUtNqOJO', '超级用户', 'Root', 1);

-- 管理员（走 RBAC）
-- 密码: Zyn@secure#99 (BCrypt)
INSERT INTO sys_user (id, username, password, nickname, real_name, status) VALUES
('4eb17ac5fed3474aa0aa8ec492533667', 'admin', '$2b$12$95rkOnPuCUUIPGfxgWkgqeKo67yAdOWLQ973teSG6sSaryUtNqOJO', '管理员', 'Admin', 1);

-- 用户角色关联
INSERT INTO sys_user_role (id, user_id, role_id) VALUES
('505bafbdc5e54626a4c9384ebb4bde03', '3c3be4ff7b1b41989494ce19eb90e3fe', 'ae41e0470af04c7a8e3b3352bcf33afb'),
('58eac589bcdd4c0c9ea967d2fea05e2e', '4eb17ac5fed3474aa0aa8ec492533667', 'cb05b80a47244344a812c33c3356ed4f');

-- 根组织
INSERT INTO sys_organization (id, parent_id, org_code, org_name, org_type, status) VALUES
('27d0599f983d47ed850b038a60b928ad', '0', 'root', '总公司', 1, 1);

-- 用户组织关联
INSERT INTO sys_user_org (id, user_id, org_id) VALUES
('e8aa3c870a10458fbf7f061e8c4071b2', '3c3be4ff7b1b41989494ce19eb90e3fe', '27d0599f983d47ed850b038a60b928ad'),
('02a8704f6642458c845148119bbdf69f', '4eb17ac5fed3474aa0aa8ec492533667', '27d0599f983d47ed850b038a60b928ad');

-- 基础权限
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, path, sort, status) VALUES
('65e2290d58b84b0eb97103c19cc401c4', '0',                                   'system',           '系统管理', 1, '/system',         1, 1),
('6f6050cf57074542bc4eca8f0784d8ce', '65e2290d58b84b0eb97103c19cc401c4',    'system:user',      '用户管理', 2, '/system/user',    1, 1),
('edf4690044cf421c92607055f7f24541', '65e2290d58b84b0eb97103c19cc401c4',    'system:role',      '角色管理', 2, '/system/role',    2, 1),
('f0bda573e8304f75a83afff6847c556a', '65e2290d58b84b0eb97103c19cc401c4',    'system:perm',      '权限管理', 2, '/system/perm',    3, 1),
('dae64518f2a8424a94fe57d87ff44618', '65e2290d58b84b0eb97103c19cc401c4',    'system:org',       '组织管理', 2, '/system/org',     4, 1),
('e65bac1ec4a64059942389660785f12d', '65e2290d58b84b0eb97103c19cc401c4',    'system:log',       '审计日志', 2, '/system/log',     5, 1);

-- 用户管理按钮
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, status) VALUES
('8b5c94abe9874f5e829677223eacc151', '6f6050cf57074542bc4eca8f0784d8ce', 'system:user:query',  '用户查询', 3, 1),
('677f536d877443f48aa1032c58de4ad3', '6f6050cf57074542bc4eca8f0784d8ce', 'system:user:create', '用户新增', 3, 1),
('4f2c3cf194cd4f44a51ac749e6681658', '6f6050cf57074542bc4eca8f0784d8ce', 'system:user:update', '用户编辑', 3, 1),
('c6aadda6730b476bb14492059ef2bfa4', '6f6050cf57074542bc4eca8f0784d8ce', 'system:user:delete', '用户删除', 3, 1);

-- 角色管理按钮
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, status) VALUES
('4895a00293df4903832014607fb3a614', 'edf4690044cf421c92607055f7f24541', 'system:role:query',  '角色查询', 3, 1),
('5cf4e7970a1743ccae59226881a09252', 'edf4690044cf421c92607055f7f24541', 'system:role:create', '角色新增', 3, 1),
('fdc03412b19546bcba5800bbfad14472', 'edf4690044cf421c92607055f7f24541', 'system:role:update', '角色编辑', 3, 1),
('2ab8635ed1b542518670e7be1fff91c6', 'edf4690044cf421c92607055f7f24541', 'system:role:delete', '角色删除', 3, 1);

-- 权限管理按钮
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, status) VALUES
('1f88ca9877884a9d84fdc3b9f4896fbb', 'f0bda573e8304f75a83afff6847c556a', 'system:perm:query',  '权限查询', 3, 1),
('95902a44f6ff45ecb127729bc21b858d', 'f0bda573e8304f75a83afff6847c556a', 'system:perm:create', '权限新增', 3, 1),
('e487edbbff05404a9ca8e3e47208ee7a', 'f0bda573e8304f75a83afff6847c556a', 'system:perm:update', '权限编辑', 3, 1),
('7b2fe3d1155e415a8799e9b4d40455ec', 'f0bda573e8304f75a83afff6847c556a', 'system:perm:delete', '权限删除', 3, 1);

-- 组织管理按钮
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, status) VALUES
('f0c6dbc597a241c1b53940eb8d64c587', 'dae64518f2a8424a94fe57d87ff44618', 'system:org:query',  '组织查询', 3, 1),
('fae3b236392c40689c198cf20ac9d575', 'dae64518f2a8424a94fe57d87ff44618', 'system:org:create', '组织新增', 3, 1),
('bfc26fad4bf740dd814c601f3b9ee311', 'dae64518f2a8424a94fe57d87ff44618', 'system:org:update', '组织编辑', 3, 1),
('f1a3cc5c94ed4921a4fd6f993e256640', 'dae64518f2a8424a94fe57d87ff44618', 'system:org:delete', '组织删除', 3, 1);

-- 资源管理菜单
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, path, sort, status) VALUES
('fa00000000004a1fbe01000000000001', '65e2290d58b84b0eb97103c19cc401c4', 'system:resource', '资源管理', 2, '/system/resource', 6, 1);

-- 资源管理按钮
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, status) VALUES
('fa00000000004a1fbe01000000000002', 'fa00000000004a1fbe01000000000001', 'system:resource:query',  '资源查询', 3, 1),
('fa00000000004a1fbe01000000000003', 'fa00000000004a1fbe01000000000001', 'system:resource:create', '资源新增', 3, 1),
('fa00000000004a1fbe01000000000004', 'fa00000000004a1fbe01000000000001', 'system:resource:update', '资源编辑', 3, 1),
('fa00000000004a1fbe01000000000005', 'fa00000000004a1fbe01000000000001', 'system:resource:delete', '资源删除', 3, 1);

-- admin 拥有全部权限
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
('e61682be6ce34144ad44d69353de2be8', 'cb05b80a47244344a812c33c3356ed4f', '65e2290d58b84b0eb97103c19cc401c4'),
('f272037d39d3459a86da741e7f086be6', 'cb05b80a47244344a812c33c3356ed4f', '6f6050cf57074542bc4eca8f0784d8ce'),
('076f181798b44f6599b95a50c890617a', 'cb05b80a47244344a812c33c3356ed4f', 'edf4690044cf421c92607055f7f24541'),
('5ce059fbdd414e5b852139a836e7f0dc', 'cb05b80a47244344a812c33c3356ed4f', 'f0bda573e8304f75a83afff6847c556a'),
('744fb353565d41c695367286fa6c8d98', 'cb05b80a47244344a812c33c3356ed4f', 'dae64518f2a8424a94fe57d87ff44618'),
('a50bbe35760d44f888706f4291daac2b', 'cb05b80a47244344a812c33c3356ed4f', 'e65bac1ec4a64059942389660785f12d'),
('59f4a335c84141beaf31ae91901569d8', 'cb05b80a47244344a812c33c3356ed4f', '8b5c94abe9874f5e829677223eacc151'),
('f55c3a87d2d9453baf425e292d1d61fb', 'cb05b80a47244344a812c33c3356ed4f', '677f536d877443f48aa1032c58de4ad3'),
('ca588ab1ccd145bb9b561b25ca242f92', 'cb05b80a47244344a812c33c3356ed4f', '4f2c3cf194cd4f44a51ac749e6681658'),
('3fb8b2f816e1401cae2f75b83249a960', 'cb05b80a47244344a812c33c3356ed4f', 'c6aadda6730b476bb14492059ef2bfa4'),
('e5fe64e718394d3eb7afbaa19ee40cdc', 'cb05b80a47244344a812c33c3356ed4f', '4895a00293df4903832014607fb3a614'),
('c142e732c71a4bc5b052c6ba1182adc5', 'cb05b80a47244344a812c33c3356ed4f', '5cf4e7970a1743ccae59226881a09252'),
('d573d26a65bb4bf59dc5350b18ca0448', 'cb05b80a47244344a812c33c3356ed4f', 'fdc03412b19546bcba5800bbfad14472'),
('42c91cd392a941bb85d2b3891cf37dea', 'cb05b80a47244344a812c33c3356ed4f', '2ab8635ed1b542518670e7be1fff91c6'),
('0f14b378dd63441285f180e530608323', 'cb05b80a47244344a812c33c3356ed4f', '1f88ca9877884a9d84fdc3b9f4896fbb'),
('8014ad088cbc4295b27d9b0991da6e05', 'cb05b80a47244344a812c33c3356ed4f', '95902a44f6ff45ecb127729bc21b858d'),
('ee94e43e184149f6a13e36d687233086', 'cb05b80a47244344a812c33c3356ed4f', 'e487edbbff05404a9ca8e3e47208ee7a'),
('05b83ad4e5344a96b78e2b362cfd40b5', 'cb05b80a47244344a812c33c3356ed4f', '7b2fe3d1155e415a8799e9b4d40455ec'),
('7cc7c087157d480eaad55ecc3efe0381', 'cb05b80a47244344a812c33c3356ed4f', 'f0c6dbc597a241c1b53940eb8d64c587'),
('0e73911ee8f446d38af674b0a035b0db', 'cb05b80a47244344a812c33c3356ed4f', 'fae3b236392c40689c198cf20ac9d575'),
('72bb55054bb642c7b84cf0096c659c61', 'cb05b80a47244344a812c33c3356ed4f', 'bfc26fad4bf740dd814c601f3b9ee311'),
('9dc7d04cbc2746aea5b8bd2150c88620', 'cb05b80a47244344a812c33c3356ed4f', 'f1a3cc5c94ed4921a4fd6f993e256640'),
('fb00000000004a1fbe01000000000001', 'cb05b80a47244344a812c33c3356ed4f', 'fa00000000004a1fbe01000000000001'),
('fb00000000004a1fbe01000000000002', 'cb05b80a47244344a812c33c3356ed4f', 'fa00000000004a1fbe01000000000002'),
('fb00000000004a1fbe01000000000003', 'cb05b80a47244344a812c33c3356ed4f', 'fa00000000004a1fbe01000000000003'),
('fb00000000004a1fbe01000000000004', 'cb05b80a47244344a812c33c3356ed4f', 'fa00000000004a1fbe01000000000004'),
('fb00000000004a1fbe01000000000005', 'cb05b80a47244344a812c33c3356ed4f', 'fa00000000004a1fbe01000000000005');

-- root 角色不需要权限关联（代码里直接绕过）
