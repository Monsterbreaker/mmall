package com.mmall.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.dao.*;
import com.mmall.pojo.*;
import com.mmall.util.DateTimeUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.GroupedCartVo;
import com.mmall.common.ServerResponse;
import com.mmall.service.IOrderService;
import com.mmall.util.BigDecimalUtil;
import com.mmall.vo.OrderItemVo;
import com.mmall.vo.OrderVo;
import com.mmall.vo.ShippingVo;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Service("iOrderService")
public class OrderServiceImpl implements IOrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ShippingMapper shippingMapper;
    @Autowired
    private UserMapper userMapper;

    /*-----------------------------------------------用户订单功能Start---------------------------------------------------*/

    /**
     * 创建订单
     *
     * @param userId
     * @param shippingId
     * @return
     */
    @Override
    public ServerResponse createOrder(Integer userId, Integer shippingId) {
        // 检查参数
        if(shippingId==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }

        // 拿购物车数据,按商家分类
        List<GroupedCartVo> groupedCartVoList = cartMapper.selectByPrimaryKeyGroupBySellerId(userId);
        if (CollectionUtils.isEmpty(groupedCartVoList)) {
            return ServerResponse.createByErrorMessage("购物车为空");
        }
        List<Order> orderList = Lists.newArrayList();
        List<List<OrderItem>> orderItemsList = Lists.newArrayList();
        for (GroupedCartVo groupedCartVo : groupedCartVoList) {
            //生成订单号
            long orderNo = generateOrderNo();
            //生成OrderItem
            ServerResponse serverResponse = getOrderItemFromCart(groupedCartVo, orderNo);
            if (!serverResponse.isSuccess()) {
                return serverResponse;
            }
            List<OrderItem> orderItemList = (List<OrderItem>) serverResponse.getData();
            orderItemsList.add(orderItemList);

            //计算订单总价
            BigDecimal payment = getOrderPayment(orderItemList);

            //todo 计算运费
            Integer postage = 0;

            //生成订单Order
            Order order = assembleOrder(orderNo, userId, groupedCartVo.getSellerId(), groupedCartVo.getSellerName(), shippingId, payment, Const.PayPlatformEnum.ALIPAY.getCode(), postage);
            orderList.add(order);
        }
        //将Order和OrderItem批量插入数据库,减少库存
        // TODO: 2019/5/3 购物车数量减少 
        createOrderUpdateDB(orderList, orderItemsList);

        //组装orderVoList
        List<OrderVo> orderVoList = assembleOrderVoList(orderList);
        return ServerResponse.createBySuccess("创建订单成功", orderItemsList);
    }

    /**
     * 顾客查看订单详情
     *
     * @param orderNo
     * @param userId
     * @return
     */
    @Override
    public ServerResponse getOrderByCustomer(Long orderNo, Integer userId) {
        // 检查参数
        if(orderNo==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }

        // 获取订单概要
        Order order = orderMapper.selectByOrderNoAndUserId(orderNo, userId);
        if (order == null) {
            return ServerResponse.createByErrorMessage("订单不存在");
        }

        // 获取订单详情
        List<OrderItem> orderItemList = orderItemMapper.selectByOrderNoAndUserId(orderNo, userId);

        // 生成OrderVo
        OrderVo orderVo = assembleOrderVo(order, orderItemList);

        return ServerResponse.createBySuccess(orderVo);
    }

    /**
     * 顾客查看订单列表
     *
     * @param userId
     * @param offset
     * @param limit
     * @return
     */
    @Override
    public ServerResponse getOrderListByCustomer(Integer userId, int offset, int limit) {
        PageHelper.offsetPage(offset, limit);
        List<Order> orderList = orderMapper.selectByUserId(userId);
        List<OrderVo> orderVoList = assembleOrderVoList(orderList);
        PageInfo pageInfo = new PageInfo(orderList);
        pageInfo.setList(orderVoList);
        return ServerResponse.createBySuccess(pageInfo);
    }

    /**
     * 顾客确认收货
     *
     * @param userId
     * @param orderNo
     * @return
     */
    @Override
    public ServerResponse confirmReceipt(Integer userId, Long orderNo) {
        // 检查参数
        if(orderNo==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }

        Order order = orderMapper.selectByOrderNoAndUserId(orderNo, userId);
        if (order == null) {
            return ServerResponse.createByErrorMessage("该订单不存在");
        }
        if (order.getStatus() != Const.OrderStatusEnum.SHIPPED.getCode()) {
            return ServerResponse.createByErrorMessage("该订单当前状态无法确认收货");
        }

        // 更新订单状态
        order.setStatus(Const.OrderStatusEnum.ORDER_SUCCESS.getCode());
        int updateCount = orderMapper.updateByPrimaryKeySelective(order);

        // 生成更新后的OrderVo
        List<OrderItem> orderItemList = orderItemMapper.selectByOrderNoAndUserId(orderNo, userId);
        OrderVo orderVo = assembleOrderVo(order, orderItemList);

        if (updateCount > 0) {
            return ServerResponse.createBySuccess("确认收货成功", orderVo);
        }

        return ServerResponse.createByErrorMessage("确认收货失败");
    }

    /**
     * 顾客取消订单
     *
     * @param userId
     * @param orderNo
     * @return
     */
    @Override
    public ServerResponse cancel(Integer userId, Long orderNo) {
        // 检查参数
        if(orderNo==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }

        Order order = orderMapper.selectByOrderNoAndUserId(orderNo, userId);
        if (order == null) {
            return ServerResponse.createByErrorMessage("该订单不存在");
        }
        if (order.getStatus() != Const.OrderStatusEnum.NO_PAY.getCode()) {
            return ServerResponse.createByErrorMessage("该订单当前状态无法取消订单");
        }

        // 更新订单状态
        order.setStatus(Const.OrderStatusEnum.CANCELED.getCode());
        int updateCount = orderMapper.updateByPrimaryKeySelective(order);

        // 生成更新后的OrderVo
        List<OrderItem> orderItemList = orderItemMapper.selectByOrderNoAndUserId(orderNo, userId);
        OrderVo orderVo = assembleOrderVo(order, orderItemList);

        if (updateCount > 0) {
            return ServerResponse.createBySuccess("取消订单成功", orderVo);
        }

        return ServerResponse.createByErrorMessage("取消订单失败");
    }

    /*-----------------------------------------------用户订单功能End---------------------------------------------------*/


    /*-----------------------------------------------商家订单功能Start---------------------------------------------------*/

    /**
     * 商家根据订单号查看订单
     *
     * @param sellerId
     * @param orderNo
     * @return
     */
    @Override
    public ServerResponse getOrderBySeller(Integer sellerId, Long orderNo) {
        // 检查参数
        if (orderNo == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }

        // 查找订单
        Order order = orderMapper.selectByOrderNoAndSellerId(orderNo, sellerId);
        if (order == null) {
            return ServerResponse.createByErrorMessage("该订单不存在");
        }

        // 查找订单包含的OrderItem
        List<OrderItem> orderItemList = orderItemMapper.selectByOrderNoAndSellerId(orderNo, sellerId);

        // 组装OrderVo
        OrderVo orderVo = assembleOrderVo(order, orderItemList);

        return ServerResponse.createBySuccess(orderVo);
    }

    /**
     * 商家获取订单列表
     *
     * @param sellerId
     * @param offset
     * @param limit
     * @return
     */
    @Override
    public ServerResponse getOrderListBySeller(Integer sellerId, int offset, int limit) {
        PageHelper.startPage(offset, limit);
        List<Order> orderList = orderMapper.selectBySellerId(sellerId);
        List<OrderVo> orderVoList = assembleOrderVoList(orderList);
        PageInfo pageInfo = new PageInfo(orderList);
        pageInfo.setList(orderVoList);
        return ServerResponse.createBySuccess(pageInfo);
    }

    /**
     * 商家发货
     *
     * @param sellerId
     * @param orderNo
     * @return
     */
    @Override
    public ServerResponse deliver(Integer sellerId, Long orderNo) {
        // 检查参数
        if (orderNo == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }

        // 查找订单
        Order order = orderMapper.selectByOrderNoAndSellerId(orderNo, sellerId);
        if (order == null) {
            return ServerResponse.createByErrorMessage("该订单不存在");
        }

        // 检查订单状态
        if (order.getStatus() != Const.OrderStatusEnum.PAID.getCode()) {
            return ServerResponse.createByErrorMessage("该订单当前状态无法发货");
        }

        // 更新订单状态
        order.setStatus(Const.OrderStatusEnum.SHIPPED.getCode());
        int updateCount = orderMapper.updateByPrimaryKeySelective(order);

        // 生成更新后的OrderVo
        List<OrderItem> orderItemList = orderItemMapper.selectByOrderNoAndSellerId(orderNo, sellerId);
        OrderVo orderVo = assembleOrderVo(order, orderItemList);

        if (updateCount > 0) {
            return ServerResponse.createBySuccess("发货成功", orderVo);
        }

        return ServerResponse.createByErrorMessage("发货失败");
    }

    /*-----------------------------------------------商家订单功能End---------------------------------------------------*/

    /**
     * 根据购物车中选中商品，生成单个商家的OrderItem
     *
     * @param groupedCartVo
     * @return
     */
    private ServerResponse getOrderItemFromCart(GroupedCartVo groupedCartVo, long orderNo) {
        List<OrderItem> orderItemList = Lists.newArrayList();
        for (Cart cart : groupedCartVo.getCarts()) {
            Product product = productMapper.selectByPrimaryKey(cart.getProductId());
            //检查商品状态
            if (product.getStatus() != Const.ProductStatusEnum.ON_SALE.getCode()) {
                return ServerResponse.createByErrorMessage("当前商品不在售卖状态");
            }
            //检查商品余量
            if (cart.getQuantity() > product.getStock()) {
                return ServerResponse.createByErrorMessage("当前商品库存不足");
            }
            //生成OrderItem
            OrderItem orderItem = new OrderItem();

            orderItem.setOrderNo(orderNo);
            orderItem.setUserId(cart.getUserId());
            orderItem.setSellerId(groupedCartVo.getSellerId());
            orderItem.setSellerName(groupedCartVo.getSellerName());
            orderItem.setProductId(cart.getProductId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setCurrentUnitPrice(product.getPrice());
            orderItem.setQuantity(cart.getQuantity());
            orderItem.setTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(), cart.getQuantity()));

            orderItemList.add(orderItem);
        }
        return ServerResponse.createBySuccess(orderItemList);
    }

    /**
     * 根据单个商家的OrderItems，生成单个商家的订单的总价
     *
     * @param orderItems
     * @return
     */
    private BigDecimal getOrderPayment(List<OrderItem> orderItems) {
        BigDecimal payment = new BigDecimal("0");
        for (OrderItem orderItem : orderItems) {
            payment = BigDecimalUtil.add(payment.doubleValue(), orderItem.getTotalPrice().doubleValue());
        }
        return payment;
    }

    /**
     * 组装Order
     *
     * @param userId
     * @param sellerId
     * @param sellerName
     * @param shippingId
     * @param payment
     * @param paymentType
     * @param postage
     * @return
     */
    private Order assembleOrder(long orderNo, Integer userId, Integer sellerId, String sellerName, Integer shippingId, BigDecimal payment, Integer paymentType, Integer postage) {
        Order order = new Order();

        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setSellerId(sellerId);
        order.setSellerName(sellerName);
        order.setShippingId(shippingId);
        order.setPayment(payment);
        order.setPaymentType(paymentType);
        order.setPostage(postage);
        order.setStatus(Const.OrderStatusEnum.NO_PAY.getCode());

        return order;
    }

    /**
     * 生成订单号
     *
     * @return
     */
    private long generateOrderNo() {
        long currentTime = System.currentTimeMillis();
        return currentTime + new Random().nextInt(100);
    }

    /**
     * 减少库存
     *
     * @param orderItemList
     */
    private void reduceStock(List<OrderItem> orderItemList) {
        for (OrderItem orderItem : orderItemList) {
            Product product = productMapper.selectByPrimaryKey(orderItem.getProductId());
            product.setStock(product.getStock() - orderItem.getQuantity());
            productMapper.updateByPrimaryKey(product);
        }
    }

    /**
     * 将order，orderItem插入数据库，同时减少库存
     *
     * @param orderList
     * @param orderItemsList
     * @return
     */
    private ServerResponse createOrderUpdateDB(List<Order> orderList, List<List<OrderItem>> orderItemsList) {
        orderMapper.batchInsert(orderList);
        for (List<OrderItem> orderItemList : orderItemsList) {
            orderItemMapper.batchInsert(orderItemList);
            reduceStock(orderItemList);
        }
        return ServerResponse.createBySuccessMessage("创建订单成功");
    }

    /**
     * 组装OrderVoList
     *
     * @param orderList
     * @return
     */
    private List<OrderVo> assembleOrderVoList(List<Order> orderList) {
        List<OrderVo> orderVoList = Lists.newArrayList();

        for (Order order : orderList) {
            //查找OrderItemList
            List<OrderItem> orderItemList = orderItemMapper.selectByOrderNo(order.getOrderNo());

            //调用组装OrderVo功能
            OrderVo orderVo = assembleOrderVo(order, orderItemList);
            orderVoList.add(orderVo);
        }

        return orderVoList;
    }

    /**
     * 组装OrderVo
     *
     * @param order
     * @param orderItemList
     * @return
     */
    private OrderVo assembleOrderVo(Order order, List<OrderItem> orderItemList) {
        OrderVo orderVo = new OrderVo();

        orderVo.setOrderNo(order.getOrderNo());
        orderVo.setUserId(order.getUserId());
        orderVo.setUsername(userMapper.selectNameByUserId(order.getUserId()));
        orderVo.setSellerId(order.getSellerId());
        orderVo.setSellerName(order.getSellerName());
        orderVo.setShippingId(order.getShippingId());
        orderVo.setPayment(order.getPayment());
        orderVo.setPaymentType(order.getPaymentType());
        orderVo.setPaymentTypeDesc(Const.PaymentTypeEnum.codeOf(order.getPaymentType()).getValue());
        orderVo.setPostage(order.getPostage());
        orderVo.setStatus(order.getStatus());
        orderVo.setStatusDesc(Const.OrderStatusEnum.codeOf(order.getStatus()).getValue());
        orderVo.setPaymentTime(DateTimeUtil.dateToStr(order.getPaymentTime()));
        orderVo.setSendTime(DateTimeUtil.dateToStr(order.getSendTime()));
        orderVo.setEndTime(DateTimeUtil.dateToStr(order.getEndTime()));
        orderVo.setCloseTime(DateTimeUtil.dateToStr(order.getCloseTime()));
        orderVo.setCreateTime(DateTimeUtil.dateToStr(order.getCreateTime()));
        orderVo.setImageHost(PropertiesUtil.getProperty(Const.ftpHost));
        orderVo.setShippingVo(assembleShippingVo(shippingMapper.selectByPrimaryKey(order.getShippingId())));

        //组装orderItemVo
        List<OrderItemVo> orderItemVoList = Lists.newArrayList();
        for (OrderItem orderItem : orderItemList) {
            orderItemVoList.add((OrderItemVo) assembleOrderItemVo(orderItem));
        }
        orderVo.setOrderItemVoList(orderItemVoList);


        return orderVo;
    }

    /**
     * 组装OrderItemVo
     *
     * @param orderItem
     * @return
     */
    private OrderItemVo assembleOrderItemVo(OrderItem orderItem) {
        OrderItemVo orderItemVo = new OrderItemVo();

        orderItemVo.setOrderNo(orderItem.getOrderNo());
        orderItemVo.setProductId(orderItem.getProductId());
        orderItemVo.setProductName(orderItem.getProductName());
        orderItemVo.setProductImage(orderItem.getProductImage());
        orderItemVo.setCurrentUnitPrice(orderItem.getCurrentUnitPrice());
        orderItemVo.setQuantity(orderItem.getQuantity());
        orderItemVo.setTotalPrice(orderItem.getTotalPrice());
        orderItemVo.setCreateTime(DateTimeUtil.dateToStr(orderItem.getCreateTime()));

        return orderItemVo;
    }

    /**
     * 组装ShippingVo
     *
     * @param shipping
     * @return
     */
    private ShippingVo assembleShippingVo(Shipping shipping) {
        ShippingVo shippingVo = new ShippingVo();

        shippingVo.setReceiverName(shipping.getReceiverName());
        shippingVo.setReceiverPhone(shipping.getReceiverPhone());
        shippingVo.setReceiverMobile(shipping.getReceiverMobile());
        shippingVo.setReceiverPhone(shipping.getReceiverProvince());
        shippingVo.setReceiverCity(shipping.getReceiverCity());
        shippingVo.setReceiverDistrict(shipping.getReceiverDistrict());
        shippingVo.setReceiverAddress(shipping.getReceiverAddress());
        shippingVo.setReceiverZip(shipping.getReceiverZip());

        return shippingVo;
    }
}
