package com.mmall.service.impl;

import com.alipay.api.AlipayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.alipay.demo.trade.utils.ZxingUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.dao.*;
import com.mmall.pojo.*;
import com.mmall.util.DateTimeUtil;
import com.mmall.util.FTPUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.*;
import com.mmall.common.ServerResponse;
import com.mmall.service.IOrderService;
import com.mmall.util.BigDecimalUtil;
import org.apache.commons.lang.StringUtils;
import org.aspectj.weaver.ast.Or;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Service("iOrderService")
public class OrderServiceImpl implements IOrderService {

    private static final Logger logger= LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private PayInfoMapper payInfoMapper;
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
        if (shippingId == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
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
        //将Order和OrderItem批量插入数据库,减少库存,更新购物车
        createOrderUpdateDB(orderList, orderItemsList,groupedCartVoList);

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
        if (orderNo == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
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
        if (orderNo == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
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
        if (orderNo == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
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


    /**
     * 商家查看营业状况
     *
     * @param days
     * @param sellerId
     * @return
     */
    @Override
    public ServerResponse getTurnoverBySeller(Integer days, Integer sellerId) {
        if (days == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        List<TurnoverItemVo> turnoverItemVoList = Lists.newArrayList();
        BigDecimal turnover = new BigDecimal("0");
        for (int i = days; i >= 0; i--) {
            TurnoverItemVo turnoverItemVo = orderMapper.selectDayBySeller(sellerId, i);

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -i);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String date = simpleDateFormat.format(calendar.getTime());

            turnoverItemVo.setDate(date);

            turnoverItemVoList.add(turnoverItemVo);

            turnover = BigDecimalUtil.add(turnover.doubleValue(), turnoverItemVo.getTurnover().doubleValue());

        }
        TurnoverVo turnoverVo = new TurnoverVo();
        turnoverVo.setDays(days + 1);
        turnoverVo.setTurnover(turnover);
        turnoverVo.setTurnoverItemVoList(turnoverItemVoList);
        return ServerResponse.createBySuccess(turnoverVo);
    }

    /*-----------------------------------------------商家订单功能End---------------------------------------------------*/


    /*-----------------------------------------------Admin订单功能Start---------------------------------------------------*/

    /**
     * 管理员查看营业状况
     *
     * @param days
     * @return
     */
    @Override
    public ServerResponse getTurnoverByAdmin(Integer days) {
        List<TurnoverItemVo> turnoverItemVoList = Lists.newArrayList();
        BigDecimal turnover = new BigDecimal("0");
        for (int i = days; i >= 0; i--) {
            TurnoverItemVo turnoverItemVo = orderMapper.selectDayByAdmin(i);

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -i);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String date = simpleDateFormat.format(calendar.getTime());

            turnoverItemVo.setDate(date);

            turnoverItemVoList.add(turnoverItemVo);

            turnover = BigDecimalUtil.add(turnover.doubleValue(), turnoverItemVo.getTurnover().doubleValue());

        }
        TurnoverVo turnoverVo = new TurnoverVo();
        turnoverVo.setDays(days + 1);
        turnoverVo.setTurnover(turnover);
        turnoverVo.setTurnoverItemVoList(turnoverItemVoList);
        return ServerResponse.createBySuccess(turnoverVo);
    }

    /*-----------------------------------------------Admin订单功能End---------------------------------------------------*/

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
    private ServerResponse createOrderUpdateDB(List<Order> orderList,
                                               List<List<OrderItem>> orderItemsList,
                                               List<GroupedCartVo> groupedCartVoList) {
        orderMapper.batchInsert(orderList);
        for (List<OrderItem> orderItemList : orderItemsList) {
            orderItemMapper.batchInsert(orderItemList);
            reduceStock(orderItemList);
        }
        for (GroupedCartVo groupedCartVo : groupedCartVoList) {
            List<Cart> cartList = groupedCartVo.getCarts();
            for (Cart cart : cartList) {
                cartMapper.deleteByPrimaryKey(cart.getId());
            }
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








    public ServerResponse pay(Long orderNo,Integer userId,String path){
        Map<String ,String> resultMap = Maps.newHashMap();
        Order order = orderMapper.selectByOrderNoAndUserId(orderNo,userId);
        if(order == null){
            return ServerResponse.createByErrorMessage("用户没有该订单");
        }
        resultMap.put("orderNo",String.valueOf(order.getOrderNo()));



        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        String outTradeNo = order.getOrderNo().toString();


        // (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
        String subject = new StringBuilder().append("mmall扫码支付,订单号:").append(outTradeNo).toString();


        // (必填) 订单总金额，单位为元，不能超过1亿元
        String totalAmount = order.getPayment().toString();


        // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
        // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
        String undiscountableAmount = "0";



        // 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
        // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
        String sellerId = "";

        // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
        String body = new StringBuilder().append("订单").append(outTradeNo).append("购买商品共").append(totalAmount).append("元").toString();


        // 商户操作员编号，添加此参数可以为商户操作员做销售统计
        String operatorId = "test_operator_id";

        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String storeId = "test_store_id";

        // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088100200300400500");




        // 支付超时，定义为120分钟
        String timeoutExpress = "120m";

        // 商品明细列表，需填写购买商品详细信息，
        List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();

        List<OrderItem> orderItemList = orderItemMapper.selectByOrderNoAndUserId(orderNo,userId);
        for(OrderItem orderItem : orderItemList){
            GoodsDetail goods = GoodsDetail.newInstance(orderItem.getProductId().toString(), orderItem.getProductName(),
                    BigDecimalUtil.mul(orderItem.getCurrentUnitPrice().doubleValue(),new Double(100).doubleValue()).longValue(),
                    orderItem.getQuantity());
            goodsDetailList.add(goods);
        }

        // 创建扫码支付请求builder，设置请求参数
        AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder()
                .setSubject(subject).setTotalAmount(totalAmount).setOutTradeNo(outTradeNo)
                .setUndiscountableAmount(undiscountableAmount).setSellerId(sellerId).setBody(body)
                .setOperatorId(operatorId).setStoreId(storeId).setExtendParams(extendParams)
                .setTimeoutExpress(timeoutExpress)
                .setNotifyUrl(PropertiesUtil.getProperty("alipay.callback.url"))//支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置
                .setGoodsDetailList(goodsDetailList);

        Configs.init("zfbinfo.properties");
        AlipayTradeService tradeService=new AlipayTradeServiceImpl.ClientBuilder().build();

        AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);
        switch (result.getTradeStatus()) {
            case SUCCESS:
                logger.info("支付宝预下单成功: )");

                AlipayTradePrecreateResponse response = result.getResponse();
                dumpResponse(response);

                File folder = new File(path);
                if(!folder.exists()){
                    folder.setWritable(true);
                    folder.mkdirs();
                }

                // 需要修改为运行机器上的路径
                //细节细节细节
                String qrPath = String.format(path+"/qr-%s.png",response.getOutTradeNo());
                String qrFileName = String.format("qr-%s.png",response.getOutTradeNo());
                ZxingUtils.getQRCodeImge(response.getQrCode(), 256, qrPath);

                File targetFile = new File(path,qrFileName);
                try {
                    FTPUtil.uploadFile(Lists.newArrayList(targetFile));
                } catch (IOException e) {
                    logger.error("上传二维码异常",e);
                }
                logger.info("qrPath:" + qrPath);
                String qrUrl = PropertiesUtil.getProperty("ftp.server.http.prefix")+targetFile.getName();
                resultMap.put("qrUrl",qrUrl);
                return ServerResponse.createBySuccess(resultMap);
            case FAILED:
                logger.error("支付宝预下单失败!!!");
                return ServerResponse.createByErrorMessage("支付宝预下单失败!!!");

            case UNKNOWN:
                logger.error("系统异常，预下单状态未知!!!");
                return ServerResponse.createByErrorMessage("系统异常，预下单状态未知!!!");

            default:
                logger.error("不支持的交易状态，交易返回异常!!!");
                return ServerResponse.createByErrorMessage("不支持的交易状态，交易返回异常!!!");
        }

    }

    private void dumpResponse(AlipayResponse response) {
        if (response != null) {
            logger.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
            if (StringUtils.isNotEmpty(response.getSubCode())) {
                logger.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(),
                        response.getSubMsg()));
            }
            logger.info("body:" + response.getBody());
        }
    }

    public ServerResponse aliCallback(Map<String,String> params){
        Long orderNo = Long.parseLong(params.get("out_trade_no"));
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order == null){
            return ServerResponse.createByErrorMessage("非快乐慕商城的订单,回调忽略");
        }
        if(order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()){
            return ServerResponse.createBySuccess("支付宝重复调用");
        }
        if(Const.AlipayCallback.TRADE_STATUS_TRADE_SUCCESS.equals(tradeStatus)){
            order.setPaymentTime(DateTimeUtil.strToDate(params.get("gmt_payment")));
            order.setStatus(Const.OrderStatusEnum.PAID.getCode());
            orderMapper.updateByPrimaryKeySelective(order);
        }

        PayInfo payInfo = new PayInfo();
        payInfo.setUserId(order.getUserId());
        payInfo.setOrderNo(order.getOrderNo());
        payInfo.setPayPlatform(Const.PayPlatformEnum.ALIPAY.getCode());
        payInfo.setPlatformNumber(tradeNo);
        payInfo.setPlatformStatus(tradeStatus);

        payInfoMapper.insert(payInfo);

        return ServerResponse.createBySuccess();
    }

    public ServerResponse queryOrderPayStatus(Integer userId,Long orderNo){
        Order order = orderMapper.selectByOrderNoAndUserId(orderNo,userId);
        if(order == null){
            return ServerResponse.createByErrorMessage("用户没有该订单");
        }
        if(order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }
}
