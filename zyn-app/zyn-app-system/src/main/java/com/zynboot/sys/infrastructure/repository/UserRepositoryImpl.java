package com.zynboot.sys.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zynboot.sys.domain.aggregate.UserAggregate;
import com.zynboot.sys.domain.repository.UserRepository;
import com.zynboot.sys.infrastructure.entity.SysUser;
import com.zynboot.sys.infrastructure.entity.SysUserOrg;
import com.zynboot.sys.infrastructure.entity.SysUserRole;
import com.zynboot.sys.infrastructure.mapper.SysUserMapper;
import com.zynboot.sys.infrastructure.mapper.SysUserOrgMapper;
import com.zynboot.sys.infrastructure.mapper.SysUserRoleMapper;
import com.zynboot.sys.query.user.UserPageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final SysUserMapper mapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserOrgMapper userOrgMapper;

    @Override
    public Optional<UserAggregate> findById(String id) {
        SysUser entity = mapper.selectById(id);
        return entity != null ? Optional.of(UserAggregate.from(entity)) : Optional.empty();
    }

    @Override
    public Optional<UserAggregate> findByUsername(String username) {
        SysUser entity = mapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        return entity != null ? Optional.of(UserAggregate.from(entity)) : Optional.empty();
    }

    @Override
    public Page<UserAggregate> page(UserPageQuery query) {
        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(query.getUsername()), SysUser::getUsername, query.getUsername())
                .like(StringUtils.hasText(query.getNickname()), SysUser::getNickname, query.getNickname())
                .like(StringUtils.hasText(query.getPhone()), SysUser::getPhone, query.getPhone())
                .eq(query.getStatus() != null, SysUser::getStatus, query.getStatus())
                .orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = mapper.selectPage(page, wrapper);

        Page<UserAggregate> aggPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        aggPage.setRecords(result.getRecords().stream().map(UserAggregate::from).toList());
        return aggPage;
    }

    @Override
    public void save(UserAggregate user) {
        mapper.insert(user.toEntity());
    }

    @Override
    public void update(UserAggregate user) {
        mapper.updateById(user.toEntity());
    }

    @Override
    public void delete(String id) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        userOrgMapper.delete(new LambdaQueryWrapper<SysUserOrg>().eq(SysUserOrg::getUserId, id));
        mapper.deleteById(id);
    }
}
