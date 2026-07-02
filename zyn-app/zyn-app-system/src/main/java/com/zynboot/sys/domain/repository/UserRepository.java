package com.zynboot.sys.domain.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zynboot.sys.domain.aggregate.UserAggregate;
import com.zynboot.sys.query.user.UserPageQuery;
import java.util.Optional;

/**
 * 用户仓储接口（领域层定义，基础设施层实现）。
 */
public interface UserRepository {

    Optional<UserAggregate> findById(String id);

    Optional<UserAggregate> findByUsername(String username);

    Page<UserAggregate> page(UserPageQuery query);

    void save(UserAggregate user);

    void update(UserAggregate user);

    /**
     * 删除用户及其关联数据（user_role、user_org 级联删除）。
     */
    void delete(String id);
}
