package com.mmall.dao;

import com.mmall.pojo.Order;
import org.apache.ibatis.annotations.Param;
import org.aspectj.weaver.ast.Or;

import java.util.List;

public interface OrderMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Order record);

    int insertSelective(Order record);

    Order selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Order record);

    int updateByPrimaryKey(Order record);

    int batchInsert(@Param("orderList") List<Order> orderList);

    Order selectByOrderNo(@Param("orderNo") Long orderNo);

    Order selectByOrderNoAndUserId(@Param("orderNo") Long orderNo, @Param("userId") Integer userId);

    Order selectByOrderNoAndSellerId(@Param("orderNo") Long orderNo, @Param("sellerId") Integer sellerId);

    List<Order> selectByUserId(Integer userId);

    List<Order> selectBySellerId(Integer sellerId);
}