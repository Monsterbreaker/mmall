package com.mmall.dao;

import com.mmall.vo.GroupedCartVo;
import com.mmall.pojo.Cart;

import java.util.List;

public interface CartMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Cart record);

    int insertSelective(Cart record);

    Cart selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Cart record);

    int updateByPrimaryKey(Cart record);

    List<GroupedCartVo> selectByPrimaryKeyGroupBySellerId(Integer userId);

    Cart selectCheckedGroupBySellerId(Integer seller_id);
}