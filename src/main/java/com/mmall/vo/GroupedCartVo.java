package com.mmall.vo;

import com.mmall.pojo.Cart;

import java.util.List;

//存放按商家分组的购物车辅助类
public class GroupedCartVo {
    private Integer userId;
    private Integer sellerId;
    private String sellerName;
    private List<Cart> carts;

    public Integer getUserId() {
        return userId;
    }

    public Integer getSellerId() {
        return sellerId;
    }

    public List<Cart> getCarts() {
        return carts;
    }

    public String getSellerName() {
        return sellerName;
    }
}
