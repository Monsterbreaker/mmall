package com.mmall.controller.customer;

import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/user/")
public class UserController {
    @Autowired
    IUserService iUserService;

    /**
     * 登录
     *
     * @param username
     * @param password
     * @param session
     * @return
     */
    @RequestMapping(value = "login", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> login(String username, String password, HttpSession session) {
        if (session.getAttribute(Const.CURRENT_USER) != null) {
            return ServerResponse.createByErrorMessage("当前已登录，请先登出");
        }
        ServerResponse<User> response = iUserService.login(username, password);
        if (response.isSuccess()) {
            session.setAttribute(Const.CURRENT_USER, response.getData());
        }
        return response;
    }

    /**
     * 注册
     *
     * @param user
     * @return
     */
    @RequestMapping(value = "register", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> register(User user) {
        ServerResponse<String> response = iUserService.register(user);
        return response;
    }

    /**
     * 登出
     *
     * @param session
     * @return
     */
    @RequestMapping(value = "logout", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<String> logout(HttpSession session) {
        if (session.getAttribute(Const.CURRENT_USER) == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录");
        }
        session.removeAttribute(Const.CURRENT_USER);
        return ServerResponse.createBySuccessMessage("退出登录成功");
    }

    /**
     * 校验用户名是否有效
     *
     * @param username
     * @return
     */
    @RequestMapping(value = "username_check", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<String> checkUsername(String username) {
        return iUserService.checkValid(username, Const.USERNAME);
    }

    /**
     * 检查邮箱是否有效
     *
     * @param email
     * @return
     */
    @RequestMapping(value = "email_check", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<String> checkEmail(String email) {
        return iUserService.checkValid(email, Const.EMAIL);
    }

    /**
     * 检查用户状态
     *
     * @param username
     * @return
     */
    @RequestMapping(value = "status_check", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<String> checkStatus(String username) {
        return iUserService.checkStatus(username);
    }

    /**
     * 获取当前登录用户的信息
     *
     * @param session
     * @return
     */
    @RequestMapping(value = "info", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<User> getUserInfo(HttpSession session) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录,无法获取当前用户的信息");
        }
        return iUserService.getInfo(user.getId());
    }

    /**
     * 忘记密码，查看密保问题
     *
     * @param username
     * @return
     */
    @RequestMapping(value = "question", method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<String> getQuestion(String username) {
        return iUserService.getQuestion(username);
    }

    /**
     * 忘记密码，验证密保问题
     *
     * @param username
     * @param answer
     * @return
     */
    @RequestMapping(value = "answer_check", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> checkAnswer(String username, String answer) {
        return iUserService.checkAnswer(username, answer);
    }

    /**
     * 忘记密码，重置密码
     *
     * @param username
     * @param newPassword
     * @param forgetToken
     * @return
     */
    @RequestMapping(value = "password_reset", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> resetPassword(String username, String newPassword, String forgetToken) {
        return iUserService.resetPassword(username, newPassword, forgetToken);
    }

    /**
     * 登录状态修改密码
     *
     * @param session
     * @param oldPassword
     * @param newPassword
     * @return
     */
    @RequestMapping(value = "password_modify", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> modifyPassword(HttpSession session, String oldPassword, String newPassword) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user != null) {
            return iUserService.modifyPassword(user, oldPassword, newPassword);
        }
        return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录,无法修改密码");
    }

    public ServerResponse<User> modifyInfo(HttpSession session, User user) {
        User currentUser = (User) session.getAttribute(Const.CURRENT_USER);
        if (currentUser == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登录，无法修改信息");
        }
        user.setId(currentUser.getId());
        return iUserService.updateInfo(user);
    }
}
