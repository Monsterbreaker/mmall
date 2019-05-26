package com.mmall.controller.customer;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.demo.trade.config.Configs;
import com.google.common.collect.Maps;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IOrderService;
import com.mmall.service.IUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Iterator;
import java.util.Map;

@Controller
@RequestMapping("/customer/order/")
public class OrderController {

    private static final Logger logger= LoggerFactory.getLogger(OrderController.class);

    @Autowired
    IOrderService iOrderService;
    @Autowired
    IUserService iUserService;

    /**
     * 顾客创建订单
     *
     * @param session
     * @param shipping_id
     * @return
     */
    @RequestMapping(value = "create", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse createOrder(HttpSession session, Integer shipping_id) {
        //检查有没有登录
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }

        //检查是不是顾客身份
        ServerResponse serverResponse = iUserService.checkCustomerRole(user);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        return iOrderService.createOrder(user.getId(), shipping_id);
    }

    /**
     * 获取某个订单
     *
     * @param session
     * @param order_no
     * @return
     */
    @RequestMapping(value = "detail", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse getOrder(HttpSession session, Long order_no) {
        //检查有没有登录
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }

        //检查是不是顾客身份
        ServerResponse serverResponse = iUserService.checkCustomerRole(user);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        return iOrderService.getOrderByCustomer(order_no, user.getId());
    }

    /**
     * 顾客查看订单列表
     *
     * @param session
     * @param offset
     * @param limit
     * @return
     */
    @RequestMapping(value = "list", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse getOrderList(HttpSession session,
                                       @RequestParam(value = "pageNum", defaultValue = "1") int offset,
                                       @RequestParam(value = "pageSize", defaultValue = "10") int limit) {
        // 检查有没有登录
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }

        // 检查是不是顾客身份
        ServerResponse serverResponse = iUserService.checkCustomerRole(user);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        return iOrderService.getOrderListByCustomer(user.getId(), offset, limit);
    }

    // TODO: 2019/5/7 删除订单

    /**
     * 顾客确认收货
     *
     * @param session
     * @param orderNo
     * @return
     */
    @RequestMapping(value = "receipt_confirm", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse confirmReceipt(HttpSession session, Long orderNo) {
        // 检查有没有登录
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }

        // 检查是不是顾客身份
        ServerResponse serverResponse = iUserService.checkCustomerRole(user);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        return iOrderService.confirmReceipt(user.getId(), orderNo);
    }

    /**
     * 取消订单
     *
     * @param session
     * @param orderNo
     * @return
     */
    @RequestMapping(value = "cancel", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse cancel(HttpSession session, Long orderNo) {
        // 检查有没有登录
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }

        // 检查是不是顾客身份
        ServerResponse serverResponse = iUserService.checkCustomerRole(user);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        return iOrderService.cancel(user.getId(), orderNo);
    }





    @RequestMapping("pay")
    @ResponseBody
    //三个参数，session，支付的订单号，第三个用于获取upload文件夹，传二维码到ftp，再返回二维码地址？？
    public ServerResponse pay(HttpSession session, Long orderNo, HttpServletRequest request){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        String path = request.getSession().getServletContext().getRealPath("upload");
        return iOrderService.pay(orderNo,user.getId(),path);
    }

    @RequestMapping("alipay_callback")
    @ResponseBody
    public Object alipayCallback(HttpServletRequest request){
        Map<String,String> params = Maps.newHashMap();

        Map requestParams = request.getParameterMap();
        for(Iterator iter = requestParams.keySet().iterator(); iter.hasNext();){
            String name = (String)iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for(int i = 0 ; i <values.length;i++){

                valueStr = (i == values.length -1)?valueStr + values[i]:valueStr + values[i]+",";
            }
            params.put(name,valueStr);
        }
        logger.info("支付宝回调,sign:{},trade_status:{},参数:{}",params.get("sign"),params.get("trade_status"),params.toString());

        //验证回调是不是支付宝发的.并且避免重复通知

        params.remove("sign_type");
        try {
            boolean alipayRSACheckedV2 = AlipaySignature.rsaCheckV2(params, Configs.getAlipayPublicKey(),"utf-8",Configs.getSignType());

            if(!alipayRSACheckedV2){
                return ServerResponse.createByErrorMessage("非法请求,验证不通过,再恶意请求我就报警找网警了");
            }
        } catch (AlipayApiException e) {
            logger.error("支付宝验证回调异常",e);
        }


        ServerResponse serverResponse = iOrderService.aliCallback(params);
        if(serverResponse.isSuccess()){
            return Const.AlipayCallback.RESPONSE_SUCCESS;
        }
        return Const.AlipayCallback.RESPONSE_FAILED;
    }


    @RequestMapping("query_order_pay_status")
    @ResponseBody
    public ServerResponse<Boolean> queryOrderPayStatus(HttpSession session, Long orderNo){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user ==null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }

        ServerResponse serverResponse = iOrderService.queryOrderPayStatus(user.getId(),orderNo);
        if(serverResponse.isSuccess()){
            return ServerResponse.createBySuccess(true);
        }
        return ServerResponse.createBySuccess(false);
    }
}
