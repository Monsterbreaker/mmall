package com.mmall.controller.seller;

import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.ICategoryService;
import com.mmall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

/**
 * Created by Monsterbreaker on 2019/6/8.
 */
@Controller
@RequestMapping("/seller/category/")
public class CategorySellerController {
    @Autowired
    ICategoryService iCategoryService;

    @Autowired
    IUserService iUserService;

    @RequestMapping("get_category.do")
    @ResponseBody
    public ServerResponse getChildrenParallelCategory(HttpSession session,@RequestParam(value = "categoryId" ,defaultValue = "0") Integer categoryId){
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
        return iCategoryService.getChildrenParallelCategory(categoryId);
    }

    @RequestMapping("get_deep_category.do")
    @ResponseBody
    public ServerResponse getCategoryAndDeepChildrenCategory(HttpSession session,@RequestParam(value = "categoryId" ,defaultValue = "0") Integer categoryId){
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
        return iCategoryService.selectCategoryAndChildrenById(categoryId);
    }
}
