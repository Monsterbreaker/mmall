package com.mmall.controller.admin;

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
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

/**
 * Created by Monsterbreaker on 2019/5/12.
 */
@Controller
@RequestMapping("/admin/order/")
public class OrderAdminController {
    @Autowired
    IOrderService iOrderService;
    @Autowired
    IUserService iUserService;

    /**
     * 管理员查看一周营业情况
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

        //检验是否是管理员
        ServerResponse serverResponse = iUserService.checkAdminRole(user);
        if (!serverResponse.isSuccess()) {
            return ServerResponse.createByErrorMessage("无权限操作，需要管理员权限");
        }

        return iOrderService.getTurnoverByAdmin(6);
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

        //检验是否是管理员
        ServerResponse serverResponse = iUserService.checkAdminRole(user);
        if (!serverResponse.isSuccess()) {
            return ServerResponse.createByErrorMessage("无权限操作，需要管理员权限");
        }

        return iOrderService.getTurnoverByAdmin(30);
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

        //检验是否是管理员
        ServerResponse serverResponse = iUserService.checkAdminRole(user);
        if (!serverResponse.isSuccess()) {
            return ServerResponse.createByErrorMessage("无权限操作，需要管理员权限");
        }

        return iOrderService.getTurnoverByAdmin(0);
    }
}
