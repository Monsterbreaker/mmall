package com.mmall.service;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;

public interface IOrderService {
    ServerResponse createOrder(Integer userId, Integer shippingId);

    ServerResponse getOrderByCustomer(Long orderNo, Integer userId);

    ServerResponse getOrderListByCustomer(Integer userId, int offset, int limit);

    ServerResponse confirmReceipt(Integer userId, Long orderNo);

    ServerResponse cancel(Integer userId, Long orderNo);

    ServerResponse getOrderBySeller(Integer sellerId, Long orderNo);

    ServerResponse getOrderListBySeller(Integer sellerId, int offset, int limit);

    ServerResponse deliver(Integer sellerId, Long orderNo);
}
