package com.zynboot.map.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zynboot.map.infrastructure.entity.MapOperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MapOperationLogMapper extends BaseMapper<MapOperationLog> {
}
