package com.zynboot.sys.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用分页响应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRes<T> {

    private List<T> records;
    private long total;
    private long pageNum;
    private long pageSize;
}
