package com.mmall.common;

public class Const {

    public static final String CURRENT_USER="current_user";

    public static final String EMAIL="email";
    public static final String USERNAME="username";

    public interface ROLE{
        int ROLE_CUMSTOMER=0;//普通用户
        int ROLE_SELLER=1;//商家
        int ROLE_ADMIN=2;//管理员
    }
}
