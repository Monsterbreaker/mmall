package com.mmall.service;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;

public interface IOrderService {
    ServerResponse createOrder(Integer userId, Integer shippingId);

    ServerResponse getOrderByCustomer(Long orderNo, Integer userId);

    // TODO: 2019/5/7 生成订单列表
    ServerResponse getOrderListByCustomer(Integer userId, int offset, int limit);

    ServerResponse confirmReceipt(Integer userId, Long orderNo);

    ServerResponse cancel(Integer userId, Long orderNo);
}
