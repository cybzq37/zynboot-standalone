package com.zynboot.sys.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zynboot.sys.domain.aggregate.OrgAggregate;
import com.zynboot.sys.domain.repository.OrgRepository;
import com.zynboot.sys.infrastructure.entity.SysOrganization;
import com.zynboot.sys.infrastructure.entity.SysUserOrg;
import com.zynboot.sys.infrastructure.mapper.SysOrganizationMapper;
import com.zynboot.sys.infrastructure.mapper.SysUserOrgMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrgRepositoryImpl implements OrgRepository {

    private final SysOrganizationMapper mapper;
    private final SysUserOrgMapper userOrgMapper;

    @Override
    public Optional<OrgAggregate> findById(String id) {
        SysOrganization entity = mapper.selectById(id);
        return entity != null ? Optional.of(OrgAggregate.from(entity)) : Optional.empty();
    }

    @Override
    public Optional<OrgAggregate> findByCode(String orgCode) {
        SysOrganization entity = mapper.selectOne(
                new LambdaQueryWrapper<SysOrganization>().eq(SysOrganization::getOrgCode, orgCode));
        return entity != null ? Optional.of(OrgAggregate.from(entity)) : Optional.empty();
    }

    @Override
    public List<OrgAggregate> findAll() {
        return mapper.selectList(null).stream().map(OrgAggregate::from).toList();
    }

    @Override
    public void save(OrgAggregate org) {
        mapper.insert(org.toEntity());
    }

    @Override
    public void update(OrgAggregate org) {
        mapper.updateById(org.toEntity());
    }

    @Override
    public void delete(String id) {
        userOrgMapper.delete(new LambdaQueryWrapper<SysUserOrg>().eq(SysUserOrg::getOrgId, id));
        mapper.deleteById(id);
    }
}
