package com.imooc.bilibili.domain;

import lombok.Data;

import java.util.Date;

@Data
public class VideoCollection {
    private Long id;
    private Long videoId;
    private Long userId;
    private Long groupId;
    private Date createTime;
}
