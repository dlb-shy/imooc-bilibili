package com.imooc.bilibili.domain;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class Video {

    private Long id;
    private Long userId;
    private String url;
    private String thumbnail;
    private String title;
    private String type;
    private String duration;
    private String area;
    private List<VideoTag> videoTagList;
    private String description;
    private Date createTime;
    private Date updateTime;

}
