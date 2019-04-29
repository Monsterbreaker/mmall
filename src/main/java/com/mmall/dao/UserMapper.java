package com.mmall.dao;

import com.mmall.pojo.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(User record);

    int insertSelective(User record);

    User selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(User record);

    int updateByPrimaryKey(User record);

    int updatePasswordByUsername(@Param("username") String username, @Param("password") String password);

    int checkUsername(String username);

    int checkEmail(String email);

    int checkStatus(String username);

    String selectSaltByUsername(String username);

    User selectLogin(@Param("username") String username, @Param("password") String password);

    String selectQuestionByUsername(String username);

    int checkAnswerByUsername(@Param("username") String username,@Param("answer") String answer);

    int checkPasswordByUsername(@Param("username") String username, @Param("password") String password);

    int checkPasswordByUserId(@Param("userId") Integer userId, @Param("password") String password);

    int checkEmailByUserId(@Param("email") String email, @Param("userId") Integer userId);

    int checkUsernameByUserId(@Param("username") String username, @Param("userId") Integer userId);
}