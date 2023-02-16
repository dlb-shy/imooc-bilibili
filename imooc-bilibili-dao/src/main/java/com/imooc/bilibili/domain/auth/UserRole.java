package com.imooc.bilibili.domain.auth;
import java.util.Date;
import lombok.Data;

@Data
public class UserRole {
    private Long id;

    private Long userId;

    private Long roleId;

    private String roleName;

    private String roleCode;

    private Date createTime;
}
