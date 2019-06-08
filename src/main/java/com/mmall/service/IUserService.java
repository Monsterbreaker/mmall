package com.mmall.service;

import com.github.pagehelper.PageInfo;
import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;

public interface IUserService {

    ServerResponse<User> login(String username, String password, Integer role);

    ServerResponse<String> register(User user, Integer role);

    ServerResponse<String> checkValid(String str, String type);

    ServerResponse<String> checkStatus(String username);

    ServerResponse<String> getQuestion(String username);

    ServerResponse<String> checkAnswer(String username, String answer);

    ServerResponse<String> resetPassword(String username, String newPassword, String forgetToken);

    ServerResponse<String> modifyPassword(User user, String oldPassword, String newPassword);

    ServerResponse<User> getInfo(Integer userId);

    ServerResponse<User> updateInfo(User user);

    ServerResponse checkCustomerRole(User user);

    ServerResponse checkSellerRole(User user);

    ServerResponse checkAdminRole(User user);

    ServerResponse freeze(Integer userId);

    ServerResponse thaw(Integer userId);

    ServerResponse<PageInfo> getUserList(String username, Integer role, int offset, int limit);
}
