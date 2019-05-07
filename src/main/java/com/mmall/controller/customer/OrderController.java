package com.mmall.controller.customer;

import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IOrderService;
import com.mmall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/order/")
public class OrderController {
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
}
