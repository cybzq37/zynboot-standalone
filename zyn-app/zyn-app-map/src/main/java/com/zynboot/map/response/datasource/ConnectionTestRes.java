package com.zynboot.map.response.datasource;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConnectionTestRes {
    String status;
    String message;
}
