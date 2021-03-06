package com.mmall.controller.seller;

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

/**
 * Created by Monsterbreaker on 2019/5/7.
 */

@Controller
@RequestMapping(value = "/seller/order/")
public class OrderSellerController {
    @Autowired
    IOrderService iOrderService;
    @Autowired
    IUserService iUserService;

    /**
     * 商家查看订单详情
     *
     * @param session
     * @param orderNo
     * @return
     */
    @RequestMapping(value = "detail", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse getOrder(HttpSession session, Long orderNo) {
        // 判断用户是否登录
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }

        // 判断用户角色是否为商家
        ServerResponse serverResponse = iUserService.checkSellerRole(user);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        return iOrderService.getOrderBySeller(user.getId(), orderNo);
    }

    /**
     * 商家获取订单列表
     *
     * @param session
     * @param offset
     * @param limit
     * @return
     */
    @RequestMapping(value = "list", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse getOrderList(HttpSession session, @RequestParam(value = "pageNum", defaultValue = "1") int offset, @RequestParam(value = "pageSize", defaultValue = "10") int limit) {
        // 判断用户是否登录
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }

        // 判断用户角色是否为商家
        ServerResponse serverResponse = iUserService.checkSellerRole(user);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        return iOrderService.getOrderListBySeller(user.getId(), offset, limit);
    }

    /**
     * 商家发货
     *
     * @param session
     * @param orderNo
     * @return
     */
    @RequestMapping(value = "deliver", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse deliver(HttpSession session, Long orderNo) {
        // 判断用户是否登录
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }

        // 判断用户角色是否为商家
        ServerResponse serverResponse = iUserService.checkSellerRole(user);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        return iOrderService.deliver(user.getId(), orderNo);
    }

    /**
     * 商家查看一周营业情况
     *
     * @param session
     * @return
     */
    @RequestMapping(value = "turnover/week", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse weekTurnover(HttpSession session) {
        //检验是否有登录
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }

        //检验是否是商家
        ServerResponse serverResponse = iUserService.checkSellerRole(user);
        if (!serverResponse.isSuccess()) {
            return ServerResponse.createByErrorMessage("无权限操作，需要商家权限");
        }

        return iOrderService.getTurnoverBySeller(6,user.getId());
    }

    /**
     * 管理员查看一个月营业情况
     *
     * @param session
     * @return
     */
    @RequestMapping(value = "turnover/month", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse monthTurnover(HttpSession session) {
        //检验是否有登录
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }

        //检验是否是商家
        ServerResponse serverResponse = iUserService.checkSellerRole(user);
        if (!serverResponse.isSuccess()) {
            return ServerResponse.createByErrorMessage("无权限操作，需要商家权限");
        }

        return iOrderService.getTurnoverBySeller(30,user.getId());
    }

    /**
     * 管理员查看今天营业情况
     *
     * @param session
     * @return
     */
    @RequestMapping(value = "turnover/today", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse todayTurnover(HttpSession session) {
        //检验是否有登录
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }

        //检验是否是商家
        ServerResponse serverResponse = iUserService.checkSellerRole(user);
        if (!serverResponse.isSuccess()) {
            return ServerResponse.createByErrorMessage("无权限操作，需要商家权限");
        }

        return iOrderService.getTurnoverBySeller(0,user.getId());
    }
}
