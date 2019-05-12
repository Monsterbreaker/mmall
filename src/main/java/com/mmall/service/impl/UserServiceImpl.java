package com.mmall.service.impl;

import com.github.pagehelper.PageHelper;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.common.TokenCache;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.UUID;

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
    public ServerResponse<User> login(String username, String password, Integer role) {
        //检查名字存不存在
        int resultCount = userMapper.checkUsername(username);
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("用户名不存在");
        }

        //检查用户状态是否正常
        ServerResponse serverResponse = checkStatus(username);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        String salt = userMapper.selectSaltByUsername(username);
        String MD5password = MD5Util.MD5EncodeUtf8(password, salt);

        User user = userMapper.selectLogin(username, MD5password, role);
        if (user == null) {
            return ServerResponse.createByErrorMessage("密码错误");
        }

        user.setPassword(StringUtils.EMPTY);
        user.setSalt(StringUtils.EMPTY);
        return ServerResponse.createBySuccess("成功登录", user);
    }

    /**
     * 注册
     *
     * @param user
     * @return
     */
    @Override
    public ServerResponse<String> register(User user, Integer role) {
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

        //检查密码是否有效
        serverResponse = checkValid(user.getPassword(), Const.PASSWORD);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        //设置用户角色
        user.setRole(role);

        //设置用户状态
        user.setStatus(Const.USER_STATUS.USER_STATUS_NORMAL);

        //随机生成Salt
        String salt = MD5Util.createRandomSalt();
        user.setSalt(salt);

        //MD5加密
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword(), salt));

        int resultCount = userMapper.insert(user);
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("注册失败");
        }
        return ServerResponse.createBySuccessMessage("注册成功");
    }

    /**
     * 检查用户名/邮箱/密码是否有效
     *
     * @param str
     * @param type
     * @return
     */
    @Override
    public ServerResponse<String> checkValid(String str, String type) {
        if (type == Const.USERNAME) {
            //检查名字存不存在
            if (StringUtils.isNotBlank(str)) {
                int resultCount = userMapper.checkUsername(str);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("用户名已存在");
                }
            } else {
                return ServerResponse.createByErrorMessage("用户名不符合要求");
            }
        } else if (type == Const.EMAIL) {
            //检查邮箱存不存在
            if (StringUtils.isNotBlank(str)) {
                int resultCount = userMapper.checkEmail(str);
                if (resultCount > 0) {
                    return ServerResponse.createByErrorMessage("邮箱已被注册");
                }
            } else {
                return ServerResponse.createByErrorMessage("邮箱不符合要求");
            }
        } else if (type == Const.PASSWORD) {
            if (StringUtils.isNotBlank(str)) {

            } else {
                return ServerResponse.createByErrorMessage("密码不符合要求");
            }
        }
        return ServerResponse.createBySuccess();
    }

    /**
     * 检查用户状态
     *
     * @param username
     * @return
     */
    @Override
    public ServerResponse<String> checkStatus(String username) {
        //检查名字存不存在
        int resultCount = userMapper.checkUsername(username);
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("用户名不存在");
        }
        int status = userMapper.checkStatus(username);
        if (status == Const.USER_STATUS.USER_STATUS_NORMAL) {
            return ServerResponse.createBySuccessMessage("用户状态正常");
        }
        return ServerResponse.createByErrorMessage("用户已被冻结");
    }

    /**
     * 忘记密码，查看密保问题
     *
     * @param username
     * @return
     */
    @Override
    public ServerResponse<String> getQuestion(String username) {
        //检查用户名是否有效
        int resultCount = userMapper.checkUsername(username);
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("用户名不存在");
        }

        //检查用户状态是否正常
        ServerResponse serverResponse = checkStatus(username);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        String question = userMapper.selectQuestionByUsername(username);
        if (StringUtils.isNotBlank(question)) {
            return ServerResponse.createBySuccess(question);
        }
        return ServerResponse.createByErrorMessage("未设置找回密码的问题");
    }

    /**
     * 忘记密码，校验密保回答是否正确
     *
     * @param username
     * @param answer
     * @return
     */
    @Override
    public ServerResponse<String> checkAnswer(String username, String answer) {
        //检查用户状态是否正常
        ServerResponse serverResponse = checkStatus(username);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        //检验答案是否正确
        int resultCount = userMapper.checkAnswerByUsername(username, answer);
        if (resultCount > 0) {
            //token写入本地缓存
            String fogetToken = UUID.randomUUID().toString();
            TokenCache.setKey(TokenCache.TOKEN_PREFIX + username, fogetToken);

            return ServerResponse.createBySuccess(fogetToken);
        }
        return ServerResponse.createByErrorMessage("答案错误");
    }

    /**
     * 忘记密码，重置密码
     *
     * @param username
     * @param newPassword
     * @param forgetToken
     * @return
     */
    @Override
    public ServerResponse<String> resetPassword(String username, String newPassword, String forgetToken) {
        //检查forgetToken参数是否正确
        if (StringUtils.isBlank(forgetToken)) {
            return ServerResponse.createByErrorMessage("参数错误，需要传递forgetToken");
        }
        //检查username参数是否正确
        int resultCount = userMapper.checkUsername(username);
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("用户名不存在");
        }

        //检查forgetToken是否在localCache中
        String localToken = TokenCache.getKey(TokenCache.TOKEN_PREFIX + username);
        if (StringUtils.isBlank(localToken)) {
            return ServerResponse.createByErrorMessage("token过期或无效");
        }
        //检查token是否正确
        if (StringUtils.equals(localToken, forgetToken)) {
            int updateCount = userMapper.updatePasswordByUsername(username, encodePassword(username, newPassword));
            if (updateCount > 0) {
                TokenCache.removeKey(TokenCache.TOKEN_PREFIX + username);
                return ServerResponse.createBySuccessMessage("重置密码成功");
            }
        } else {
            return ServerResponse.createByErrorMessage("token错误，请重新获取token");
        }
        return ServerResponse.createByErrorMessage("重置密码失败");
    }


    /**
     * 登录状态修改用户密码
     *
     * @param user
     * @param oldPassword
     * @param newPassword
     * @return
     */
    @Override
    public ServerResponse<String> modifyPassword(User user, String oldPassword, String newPassword) {
        //验证旧密码是否正确
        int resultCount = userMapper.checkPasswordByUserId(user.getId(), encodePassword(user.getUsername(), oldPassword));
        if (resultCount == 0) {
            return ServerResponse.createByErrorMessage("密码错误");
        }

        //检查新密码是否有效
        ServerResponse serverResponse = checkValid(newPassword, Const.PASSWORD);
        if (!serverResponse.isSuccess()) {
            return serverResponse;
        }

        //更新密码
        user.setPassword(encodePassword(user.getUsername(), newPassword));
        int updateCount = userMapper.updateByPrimaryKeySelective(user);
        if (updateCount > 0) {
            return ServerResponse.createBySuccessMessage("更新密码成功");
        }
        return ServerResponse.createByErrorMessage("更新密码失败");
    }

    /**
     * 根据userId获取用户信息
     *
     * @param userId
     * @return
     */
    @Override
    public ServerResponse<User> getInfo(Integer userId) {
        //查找用户信息
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null) {
            return ServerResponse.createByErrorMessage("用户不存在");
        }

        user.setPassword(StringUtils.EMPTY);
        user.setSalt(StringUtils.EMPTY);
        return ServerResponse.createBySuccess(user);
    }

    /**
     * 修改用户信息
     *
     * @param user
     * @return
     */
    @Override
    public ServerResponse<User> updateInfo(User user) {
        if (StringUtils.isNotBlank(user.getUsername())) {
            int resultCount = userMapper.checkUsernameByUserId(user.getUsername(), user.getId());
            if (resultCount > 0) {
                return ServerResponse.createByErrorMessage("用户名已存在");
            }
        } else {
            return ServerResponse.createByErrorMessage("新用户名不符合要求");
        }
        if (StringUtils.isNotBlank(user.getUsername())) {
            int resultCount = userMapper.checkEmailByUserId(user.getEmail(), user.getId());
            if (resultCount > 0) {
                return ServerResponse.createByErrorMessage("邮箱已存在");
            }
        } else {
            return ServerResponse.createByErrorMessage("新邮箱不符合要求");
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setEmail(user.getEmail());
        updateUser.setPhone(user.getPhone());
        updateUser.setQuestion(user.getQuestion());
        updateUser.setAnswer(user.getAnswer());
        int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);
        if (updateCount > 0) {
            return ServerResponse.createBySuccess("更新用户信息成功", updateUser);
        }
        return ServerResponse.createByErrorMessage("更新用户信息失败");
    }

    /**
     * 获取Salt，对密码加密（要确保username存在）
     *
     * @param username
     * @param password
     * @return
     */
    private String encodePassword(String username, String password) {
        String salt = userMapper.selectSaltByUsername(username);
        return MD5Util.MD5EncodeUtf8(password, salt);
    }


    //检查用户角色

    /**
     * 检查当前用户是不是顾客
     *
     * @param user
     * @return
     */
    @Override
    public ServerResponse checkCustomerRole(User user) {
        if (user != null && user.getRole() == Const.RoleEnum.ROLE_CUMSTOMER.getCode()) {
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByErrorMessage("当前用户不是顾客");
    }

    /**
     * 检查当前用户是不是卖家
     *
     * @param user
     * @return
     */
    @Override
    public ServerResponse checkSellerRole(User user) {
        if (user != null && user.getRole() == Const.RoleEnum.ROLE_SELLER.getCode()) {
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByErrorMessage("当前用户不是卖家");
    }

    /**
     * 检查当前用户是不是管理员
     *
     * @param user
     * @return
     */
    @Override
    public ServerResponse checkAdminRole(User user) {
        if (user != null && user.getRole() == Const.RoleEnum.ROLE_ADMIN.getCode()) {
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByErrorMessage("当前用户不是管理员");
    }

    /**
     * 根据userId冻结用户
     *
     * @param userId
     * @return
     */
    @Override
    public ServerResponse freeze(Integer userId) {
        // 检查参数
        if (userId == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }

        // 检查参数
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null) {
            return ServerResponse.createByErrorMessage("用户不存在");
        }

        // 检查用户状态
        if (user.getStatus() == Const.USER_STATUS.USER_STATUS_FROZEN) {
            return ServerResponse.createByErrorMessage("当前用户已被冻结");
        }

        // 更新用户状态
        user.setStatus(Const.USER_STATUS.USER_STATUS_FROZEN);
        int updateCount = userMapper.updateByPrimaryKeySelective(user);
        if (updateCount > 0) {
            return ServerResponse.createBySuccess("冻结成功", user);
        }
        return ServerResponse.createByErrorMessage("冻结失败");
    }

    /**
     * 根据userId解冻账户
     *
     * @param userId
     * @return
     */
    @Override
    public ServerResponse thaw(Integer userId) {
        // 检查参数
        if (userId == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }

        // 检查参数
        User user = userMapper.selectByPrimaryKey(userId);
        if (user == null) {
            return ServerResponse.createByErrorMessage("用户不存在");
        }

        // 检查用户状态
        if (user.getStatus() == Const.USER_STATUS.USER_STATUS_NORMAL) {
            return ServerResponse.createByErrorMessage("用户未被冻结");
        }

        // 更新用户状态
        user.setStatus(Const.USER_STATUS.USER_STATUS_NORMAL);
        int updateCount = userMapper.updateByPrimaryKeySelective(user);
        if (updateCount > 0) {
            return ServerResponse.createBySuccess("解冻成功", user);
        }
        return ServerResponse.createByErrorMessage("解冻失败");
    }

    /**
     * 根据username和role模糊搜索，返回UserList
     *
     * @param username
     * @param role
     * @param offset
     * @param limit
     * @return
     */
    @Override
    public ServerResponse getUserList(String username, Integer role, int offset, int limit) {
        PageHelper.startPage(offset, limit);
        if (StringUtils.isBlank(username)) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(), ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        username = new StringBuilder().append("%").append(username).append("%").toString();
        List<User> userList = userMapper.selectUsersByUsernameAndRole(username, role);

        for (User user : userList) {
            user.setPassword(StringUtils.EMPTY);
            user.setSalt(StringUtils.EMPTY);
        }

        return ServerResponse.createBySuccess(userList);
    }
}
