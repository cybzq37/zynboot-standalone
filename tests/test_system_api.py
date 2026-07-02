"""
Zyn System API 集成测试
用法: python -m pytest test_system_api.py -v
"""
import pytest
import requests
import time

SYS_BASE = "http://localhost:28081/sys"
DEMO_BASE = "http://localhost:28080/demo"
AUTH_HEADER = "Authorization"


# ── Fixtures ──────────────────────────────────────────────────

@pytest.fixture(scope="session")
def sys_token():
    """通过 system 服务直连登录获取 token"""
    r = requests.post(f"{SYS_BASE}/api/v1/auth/login",
                      json={"username": "root", "password": "Zyn@secure#99"})
    assert r.status_code == 200, f"登录失败: {r.text}"
    data = r.json()
    assert data["code"] == "0", f"登录失败: {data}"
    time.sleep(2)  # 确保 session 持久化到 Redis
    return data["data"]["token"]


@pytest.fixture(scope="session")
def demo_token():
    """通过 demo 代理登录获取 token"""
    r = requests.post(f"{DEMO_BASE}/api/v1/sys-proxy/auth/login",
                      json={"username": "root", "password": "Zyn@secure#99"},
                      headers={AUTH_HEADER: ""})
    assert r.status_code == 200, f"代理登录失败: {r.text}"
    data = r.json()
    assert data["code"] == "0", f"代理登录失败: {data}"
    return data["data"]["token"]


@pytest.fixture(scope="session")
def sys_headers(sys_token):
    return {AUTH_HEADER: sys_token}


@pytest.fixture(scope="session")
def demo_headers(demo_token):
    return {AUTH_HEADER: demo_token}


# ── 直连测试: Auth ─────────────────────────────────────────────

class TestAuthDirect:
    def test_login(self):
        r = requests.post(f"{SYS_BASE}/api/v1/auth/login",
                          json={"username": "root", "password": "Zyn@secure#99"})
        data = r.json()
        assert data["code"] == "0"
        assert "token" in data["data"]
        assert data["data"]["userInfo"]["username"] == "root"

    def test_info(self, sys_headers):
        r = requests.get(f"{SYS_BASE}/api/v1/auth/info", headers=sys_headers)
        data = r.json()
        assert data["code"] == "0", f"info 失败: {data}"
        assert data["data"]["user"]["username"] == "root"

    def test_logout(self, sys_headers):
        r = requests.post(f"{SYS_BASE}/api/v1/auth/logout", headers=sys_headers)
        assert r.json()["code"] == "0"


# ── 直连测试: User CRUD ───────────────────────────────────────

class TestUserDirect:
    created_id = None

    def test_create(self, sys_headers):
        r = requests.post(f"{SYS_BASE}/api/v1/user", headers=sys_headers,
                          json={"username": "testuser", "password": "Test@1234",
                                "nickname": "测试用户", "status": 1})
        assert r.json()["code"] == "0"

    def test_list(self, sys_headers):
        r = requests.get(f"{SYS_BASE}/api/v1/user", headers=sys_headers,
                         params={"pageNum": 1, "pageSize": 10})
        data = r.json()
        assert data["code"] == "0"
        records = data["data"]["records"]
        assert len(records) >= 1
        TestUserDirect.created_id = next(
            (u["id"] for u in records if u["username"] == "testuser"), None)

    def test_get_by_id(self, sys_headers):
        assert TestUserDirect.created_id, "依赖 test_list 创建的用户"
        r = requests.get(f"{SYS_BASE}/api/v1/user/{TestUserDirect.created_id}",
                         headers=sys_headers)
        data = r.json()
        assert data["code"] == "0"
        assert data["data"]["username"] == "testuser"

    def test_update(self, sys_headers):
        assert TestUserDirect.created_id
        r = requests.put(f"{SYS_BASE}/api/v1/user/{TestUserDirect.created_id}",
                         headers=sys_headers,
                         json={"nickname": "已更新"})
        assert r.json()["code"] == "0"
        # 验证更新
        r2 = requests.get(f"{SYS_BASE}/api/v1/user/{TestUserDirect.created_id}",
                          headers=sys_headers)
        assert r2.json()["data"]["nickname"] == "已更新"

    def test_delete(self, sys_headers):
        assert TestUserDirect.created_id
        r = requests.delete(f"{SYS_BASE}/api/v1/user/{TestUserDirect.created_id}",
                            headers=sys_headers)
        assert r.json()["code"] == "0"


# ── 直连测试: Role CRUD ───────────────────────────────────────

class TestRoleDirect:
    created_id = None

    def test_create(self, sys_headers):
        r = requests.post(f"{SYS_BASE}/api/v1/role", headers=sys_headers,
                          json={"roleCode": "test_role", "roleName": "测试角色", "status": 1})
        assert r.json()["code"] == "0"

    def test_list(self, sys_headers):
        r = requests.get(f"{SYS_BASE}/api/v1/role", headers=sys_headers)
        data = r.json()
        assert data["code"] == "0"
        TestRoleDirect.created_id = next(
            (x["id"] for x in data["data"] if x["roleCode"] == "test_role"), None)

    def test_get_by_id(self, sys_headers):
        assert TestRoleDirect.created_id
        r = requests.get(f"{SYS_BASE}/api/v1/role/{TestRoleDirect.created_id}",
                         headers=sys_headers)
        assert r.json()["data"]["roleCode"] == "test_role"

    def test_update(self, sys_headers):
        assert TestRoleDirect.created_id
        r = requests.put(f"{SYS_BASE}/api/v1/role/{TestRoleDirect.created_id}",
                         headers=sys_headers, json={"roleName": "已更新角色"})
        assert r.json()["code"] == "0"

    def test_delete(self, sys_headers):
        assert TestRoleDirect.created_id
        r = requests.delete(f"{SYS_BASE}/api/v1/role/{TestRoleDirect.created_id}",
                            headers=sys_headers)
        assert r.json()["code"] == "0"


# ── 直连测试: Permission CRUD ─────────────────────────────────

class TestPermissionDirect:
    created_id = None

    def test_list(self, sys_headers):
        r = requests.get(f"{SYS_BASE}/api/v1/permission", headers=sys_headers)
        assert r.json()["code"] == "0"

    def test_tree(self, sys_headers):
        r = requests.get(f"{SYS_BASE}/api/v1/permission/tree", headers=sys_headers)
        data = r.json()
        assert data["code"] == "0"
        assert len(data["data"]) > 0

    def test_create(self, sys_headers):
        r = requests.post(f"{SYS_BASE}/api/v1/permission", headers=sys_headers,
                          json={"parentId": "65e2290d58b84b0eb97103c19cc401c4",
                                "permCode": "test:perm", "permName": "测试权限",
                                "permType": 2, "status": 1})
        assert r.json()["code"] == "0"

    def test_get_created(self, sys_headers):
        r = requests.get(f"{SYS_BASE}/api/v1/permission", headers=sys_headers,
                         params={"permName": "测试权限"})
        data = r.json()
        assert data["code"] == "0"
        match = [x for x in data["data"] if x["permCode"] == "test:perm"]
        if match:
            TestPermissionDirect.created_id = match[0]["id"]

    def test_update(self, sys_headers):
        if not TestPermissionDirect.created_id:
            pytest.skip("未创建成功")
        r = requests.put(f"{SYS_BASE}/api/v1/permission/{TestPermissionDirect.created_id}",
                         headers=sys_headers, json={"permName": "已更新权限"})
        assert r.json()["code"] == "0"

    def test_delete(self, sys_headers):
        if not TestPermissionDirect.created_id:
            pytest.skip("未创建成功")
        r = requests.delete(f"{SYS_BASE}/api/v1/permission/{TestPermissionDirect.created_id}",
                            headers=sys_headers)
        assert r.json()["code"] == "0"


# ── 直连测试: Resource CRUD ───────────────────────────────────

class TestResourceDirect:
    created_id = None

    def test_list(self, sys_headers):
        r = requests.get(f"{SYS_BASE}/api/v1/resource", headers=sys_headers)
        assert r.json()["code"] == "0"

    def test_create(self, sys_headers):
        r = requests.post(f"{SYS_BASE}/api/v1/resource", headers=sys_headers,
                          json={"resName": "test_resource", "resType": 1,
                                "requestMethod": "GET", "requestPath": "/test",
                                "status": 1})
        assert r.json()["code"] == "0"

    def test_get_created(self, sys_headers):
        r = requests.get(f"{SYS_BASE}/api/v1/resource", headers=sys_headers)
        match = [x for x in r.json()["data"] if x.get("resName") == "test_resource"]
        if match:
            TestResourceDirect.created_id = match[0]["id"]

    def test_update(self, sys_headers):
        if not TestResourceDirect.created_id:
            pytest.skip("未创建成功")
        r = requests.put(f"{SYS_BASE}/api/v1/resource/{TestResourceDirect.created_id}",
                         headers=sys_headers, json={"resName": "updated_resource"})
        assert r.json()["code"] == "0"

    def test_delete(self, sys_headers):
        if not TestResourceDirect.created_id:
            pytest.skip("未创建成功")
        r = requests.delete(f"{SYS_BASE}/api/v1/resource/{TestResourceDirect.created_id}",
                            headers=sys_headers)
        assert r.json()["code"] == "0"


# ── 直连测试: Organization CRUD ───────────────────────────────

class TestOrgDirect:
    created_id = None

    def test_list(self, sys_headers):
        r = requests.get(f"{SYS_BASE}/api/v1/org", headers=sys_headers)
        assert r.json()["code"] == "0"

    def test_tree(self, sys_headers):
        r = requests.get(f"{SYS_BASE}/api/v1/org/tree", headers=sys_headers)
        assert r.json()["code"] == "0"

    def test_create(self, sys_headers):
        r = requests.post(f"{SYS_BASE}/api/v1/org", headers=sys_headers,
                          json={"orgCode": "test_dept", "orgName": "测试部门",
                                "orgType": 3, "status": 1})
        assert r.json()["code"] == "0"

    def test_get_created(self, sys_headers):
        r = requests.get(f"{SYS_BASE}/api/v1/org", headers=sys_headers)
        match = [x for x in r.json()["data"] if x.get("orgCode") == "test_dept"]
        if match:
            TestOrgDirect.created_id = match[0]["id"]

    def test_update(self, sys_headers):
        if not TestOrgDirect.created_id:
            pytest.skip("未创建成功")
        r = requests.put(f"{SYS_BASE}/api/v1/org/{TestOrgDirect.created_id}",
                         headers=sys_headers, json={"orgName": "已更新部门"})
        assert r.json()["code"] == "0"

    def test_delete(self, sys_headers):
        if not TestOrgDirect.created_id:
            pytest.skip("未创建成功")
        r = requests.delete(f"{SYS_BASE}/api/v1/org/{TestOrgDirect.created_id}",
                            headers=sys_headers)
        assert r.json()["code"] == "0"


# ── 代理测试: 通过 demo → system ──────────────────────────────

class TestProxyAuth:
    def test_login(self):
        r = requests.post(f"{DEMO_BASE}/api/v1/sys-proxy/auth/login",
                          json={"username": "root", "password": "Zyn@secure#99"})
        data = r.json()
        assert data["code"] == "0"
        assert "token" in data["data"]

    def test_info(self, demo_headers):
        r = requests.get(f"{DEMO_BASE}/api/v1/sys-proxy/auth/info", headers=demo_headers)
        assert r.json()["code"] == "0", f"info 失败: {r.json()}"
        assert r.json()["data"]["user"]["username"] == "root"


class TestProxyUser:
    def test_page(self, demo_headers):
        r = requests.get(f"{DEMO_BASE}/api/v1/sys-proxy/user",
                         headers=demo_headers, params={"pageNum": 1, "pageSize": 5})
        assert r.json()["code"] == "0"

    def test_get_by_id(self, demo_headers):
        # 用 seed data 中的 root 用户 ID
        root_id = "3c3be4ff7b1b41989494ce19eb90e3fe"
        r = requests.get(f"{DEMO_BASE}/api/v1/sys-proxy/user/{root_id}",
                         headers=demo_headers)
        data = r.json()
        assert data["code"] == "0"
        assert data["data"]["username"] == "root"


class TestProxyRole:
    def test_list(self, demo_headers):
        r = requests.get(f"{DEMO_BASE}/api/v1/sys-proxy/role", headers=demo_headers)
        data = r.json()
        assert data["code"] == "0"
        assert len(data["data"]) >= 1

    def test_get_by_id(self, demo_headers):
        role_id = "ae41e0470af04c7a8e3b3352bcf33afb"
        r = requests.get(f"{DEMO_BASE}/api/v1/sys-proxy/role/{role_id}",
                         headers=demo_headers)
        assert r.json()["data"]["roleCode"] == "root"


class TestProxyPermission:
    def test_list(self, demo_headers):
        r = requests.get(f"{DEMO_BASE}/api/v1/sys-proxy/permission", headers=demo_headers)
        assert r.json()["code"] == "0"

    def test_tree(self, demo_headers):
        r = requests.get(f"{DEMO_BASE}/api/v1/sys-proxy/permission/tree",
                         headers=demo_headers)
        data = r.json()
        assert data["code"] == "0"
        assert len(data["data"]) > 0


class TestProxyResource:
    def test_list(self, demo_headers):
        r = requests.get(f"{DEMO_BASE}/api/v1/sys-proxy/resource", headers=demo_headers)
        assert r.json()["code"] == "0"


class TestProxyOrg:
    def test_list(self, demo_headers):
        r = requests.get(f"{DEMO_BASE}/api/v1/sys-proxy/org", headers=demo_headers)
        assert r.json()["code"] == "0"

    def test_tree(self, demo_headers):
        r = requests.get(f"{DEMO_BASE}/api/v1/sys-proxy/org/tree", headers=demo_headers)
        assert r.json()["code"] == "0"
