package com.mmall.dao;

import com.mmall.vo.GroupedCartVo;
import com.mmall.pojo.Cart;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CartMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Cart record);

    int insertSelective(Cart record);

    Cart selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Cart record);

    int updateByPrimaryKey(Cart record);

    Cart selectCartByUserIdProductId(@Param("userId") Integer userId,@Param("produckId") Integer productId);

    List<GroupedCartVo> selectByPrimaryKeyGroupBySellerId(Integer userId);

    Cart selectCheckedGroupBySellerId(Integer seller_id);
}