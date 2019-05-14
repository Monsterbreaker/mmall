package com.mmall.controller.admin;

import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
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
@RequestMapping("/admin/user/")
public class UserAdminController {
    @Autowired
    IUserService iUserService;

    /**
     * 管理员登录
     *
     * @param session
     * @param username
     * @param password
     * @return
     */
    @RequestMapping(value = "login", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse login(HttpSession session, String username, String password) {
        if (session.getAttribute(Const.CURRENT_USER) != null) {
            return ServerResponse.createByErrorMessage("当前已登录，请先登出");
        }
        ServerResponse<User> response = iUserService.login(username, password, Const.RoleEnum.ROLE_ADMIN.getCode());
        if (response.isSuccess()) {
            session.setAttribute(Const.CURRENT_USER, response.getData());
        }
        return response;
    }

    /**
     * 管理员登出
     *
     * @param session
     * @return
     */
    @RequestMapping(value = "logout", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse logout(HttpSession session) {
        // 检查是否登录
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }


        // 检查是否是Admin
        ServerResponse serverResponse = iUserService.checkAdminRole(user);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        session.removeAttribute(Const.CURRENT_USER);
        return ServerResponse.createBySuccessMessage("退出登录成功");
    }

    /**
     * 根据userId冻结用户
     *
     * @param session
     * @param userId
     * @return
     */
    @RequestMapping(value = "freeze", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse freeze(HttpSession session, Integer userId) {
        // 检查是否登录
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }

        // 检查是否是Admin
        ServerResponse serverResponse = iUserService.checkAdminRole(user);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        return iUserService.freeze(userId);
    }

    /**
     * 根据userId解冻账户
     *
     * @param session
     * @param userId
     * @return
     */
    @RequestMapping(value = "thaw", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse thaw(HttpSession session, Integer userId) {
        // 检查是否登录
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }

        // 检查是否是Admin
        ServerResponse serverResponse = iUserService.checkAdminRole(user);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        return iUserService.thaw(userId);
    }

    /**
     * 根据username模糊搜索，返回顾客列表
     *
     * @param session
     * @param username
     * @param offset
     * @param limit
     * @return
     */
    @RequestMapping(value = "customer", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse getCustomerList(HttpSession session,
                                          String username,
                                          @RequestParam(value = "pageNum", defaultValue = "1") int offset,
                                          @RequestParam(value = "pageSize", defaultValue = "10") int limit) {
        // 检查是否登录
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }

        // 检查是否是Admin
        ServerResponse serverResponse = iUserService.checkAdminRole(user);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        return iUserService.getUserList(username, Const.RoleEnum.ROLE_CUMSTOMER.getCode(), offset, limit);
    }

    /**
     * 根据username模糊搜索，返回商家列表
     *
     * @param session
     * @param username
     * @param offset
     * @param limit
     * @return
     */
    @RequestMapping(value = "seller", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse getSellerList(HttpSession session,
                                        String username,
                                        @RequestParam(value = "pageNum", defaultValue = "1") int offset,
                                        @RequestParam(value = "pageSize", defaultValue = "10") int limit) {
        // 检查是否登录
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }

        // 检查是否是Admin
        ServerResponse serverResponse = iUserService.checkAdminRole(user);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        return iUserService.getUserList(username, Const.RoleEnum.ROLE_SELLER.getCode(), offset, limit);
    }
}
