package com.mmall.service.impl;

import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("iUserService")
public class UserServiceImpl implements IUserService {
    @Autowired
    private UserMapper userMapper;

    /**
     * 登录
     *
     * @param username
     * @param password
     * @return
     */
    @Override
    public ServerResponse<User> login(String username, String password) {
        //检查名字存不存在
        int resultCount = userMapper.checkUsername(username);
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("用户名不存在");
        }

        String MD5password = MD5Util.MD5EncodeUtf8(password);

        User user = userMapper.selectLogin(username, MD5password);
        if (user == null) {
            return ServerResponse.createByErrorMessage("密码错误");
        }

        return ServerResponse.createBySuccess("成功登录", user);
    }

    /**
     * 注册
     *
     * @param user
     * @return
     */
    @Override
    public ServerResponse<String> register(User user) {
        //检查用户名是否有效
        ServerResponse<String> serverResponse = checkValid(user.getUsername(), Const.USERNAME);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        //检查邮箱是否有效
        serverResponse = checkValid(user.getEmail(), Const.EMAIL);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        user.setRole(Const.ROLE.ROLE_CUMSTOMER);
        //MD5加密
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));
        user.setSalt("geelysdafaqj23ou89ZXcj@#$@#$#@KJdjklj;D../dSF.,");

        int resultCount = userMapper.insert(user);
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("注册失败");
        }
        return ServerResponse.createBySuccessMessage("注册成功");
    }

    /**
     * 检查用户名/邮箱是否有效
     *
     * @param str
     * @param type
     * @return
     */
    @Override
    public ServerResponse<String> checkValid(String str, String type) {
        if (org.apache.commons.lang3.StringUtils.isNotBlank(str)) {
            if (type == Const.USERNAME) {
                //检查名字存不存在
                int resultCount = userMapper.checkUsername(str);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("用户名已存在");
                }
            } else if (type == Const.EMAIL) {
                //检查邮箱存不存在
                int resultCount = userMapper.checkEmail(str);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("邮箱已被注册");
                }
            }
        } else {
            return ServerResponse.createByErrorMessage("参数错误");
        }
        return ServerResponse.createBySuccess();
    }
}
