package com.mmall.common;

public class Const {

    public static final String CURRENT_USER="current_user";

    public static final String EMAIL="email";
    public static final String USERNAME="username";
    public static final String PASSWORD="password";

    public interface ROLE{
        int ROLE_CUMSTOMER=0;//普通用户
        int ROLE_SELLER=1;//商家
        int ROLE_ADMIN=2;//管理员
    }

    public interface USER_STATUS{
        int USER_STATUS_NORMAL=0;//正常状态
        int USER_STATUS_FROZEN=1;//冻结状态
    }

    public interface Cart{
        int CHECKED=1;//购物车商品为选中状态
        int UNCHECKED=0;//未选中
    }
}
