package com.zynboot.map.response.instance;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PublicMapConfigRes {
    InstanceRes instance;
    List<InstanceLayerRes> layers;
}
