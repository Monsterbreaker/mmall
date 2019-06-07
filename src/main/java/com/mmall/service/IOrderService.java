package com.mmall.service;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;

import java.util.Map;

public interface IOrderService {
    ServerResponse createOrder(Integer userId, Integer shippingId);

    ServerResponse getOrderByCustomer(Long orderNo, Integer userId);

    ServerResponse getOrderListByCustomer(Integer userId, int offset, int limit);

    ServerResponse confirmReceipt(Integer userId, Long orderNo);

    ServerResponse cancel(Integer userId, Long orderNo);

    ServerResponse getOrderCartProduct(Integer userId);

    ServerResponse getOrderBySeller(Integer sellerId, Long orderNo);

    ServerResponse getOrderListBySeller(Integer sellerId, int offset, int limit);

    ServerResponse deliver(Integer sellerId, Long orderNo);

    ServerResponse getTurnoverByAdmin(Integer days);

    ServerResponse getTurnoverBySeller(Integer days,Integer sellerId);

    ServerResponse pay(Long orderNo,Integer userId,String path);

    ServerResponse aliCallback(Map<String,String> params);

    ServerResponse queryOrderPayStatus(Integer userId,Long orderNo);
}
