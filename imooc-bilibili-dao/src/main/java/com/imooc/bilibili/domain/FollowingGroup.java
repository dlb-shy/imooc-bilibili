package com.imooc.bilibili.domain;
import java.util.List;
import lombok.Data;

import java.util.Date;

@Data
public class FollowingGroup {

    private Long id;
    private Long userId;
    private String name;
    private String type;
    private Date createTime;
    private Date updateTime;
    private List<UserInfo> followingUserInfoList;

}
