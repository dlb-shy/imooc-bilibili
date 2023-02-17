package com.imooc.bilibili.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.List;

@Data
@Document(indexName = "videos")
public class Video {
    @Id
    private Long id;
    @Field(type = FieldType.Long)
    private Long userId;
    private String url;
    private String thumbnail;
    @Field(type = FieldType.Text)
    private String title;
    private String type;
    private String duration;
    private String area;
    private List<VideoTag> videoTagList;
    @Field(type = FieldType.Text)
    private String description;
    @Field(type = FieldType.Date)
    private Date createTime;
    @Field(type = FieldType.Date)
    private Date updateTime;

}
