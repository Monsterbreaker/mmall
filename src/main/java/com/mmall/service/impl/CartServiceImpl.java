package com.mmall.service.impl;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CartMapper;
import com.mmall.pojo.Cart;
import com.mmall.service.ICartService;
import org.springframework.beans.factory.annotation.Autowired;

public class CartServiceImpl implements ICartService {

    @Autowired
    private CartMapper cartMapper;
    public ServerResponse add(Integer userId,Integer produckId,Integer count){
        Cart cart=cartMapper.selectCartByUserIdProductId(userId,produckId);
        //如果本商品不在购物车中，添加本次商品的记录
        if (cart==null){
            Cart cartItem=new Cart();
            cartItem.setQuantity(count);
            cartItem.setChecked(Const.Cart.CHECKED);
            cartItem.setProductId(produckId);
            cartItem.setUserId(userId);
            cartMapper.insert(cartItem);//同时把数据插入数据库
        }
        else {
            //如果商品在里边，增加数量
            count=cart.getQuantity()+count;
            cart.setQuantity(count);
            cartMapper.updateByPrimaryKeySelective(cart);
        }
        ServerResponse yeshimeiyongde = null;
        return yeshimeiyongde;//强迫症，必须没有报错再提交

    }
}
