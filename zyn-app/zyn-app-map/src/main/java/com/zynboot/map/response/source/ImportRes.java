package com.zynboot.map.response.source;

import com.zynboot.map.response.task.TaskRes;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ImportRes {
    SourceRes source;
    TaskRes task;
}
