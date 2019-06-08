package com.mmall.controller.admin;

import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.ICategoryService;
import com.mmall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin/category/")
public class CategoryManageController {
    @Autowired
    ICategoryService iCategoryService;

    @Autowired
    IUserService iUserService;

    @RequestMapping(value = "add", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse addCategory(HttpSession session, String categoryName, int parentId) {
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

        return iCategoryService.addCategory(categoryName, parentId);
    }

    /**
     * 更新分类名称
     * @param session
     * @param categoryId
     * @param categoryName
     * @return
     */
    @RequestMapping(value = "updateCategoryName", method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse updateCategoryName(HttpSession session, int categoryId, String categoryName) {
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

        return iCategoryService.updateCategoryName(categoryId,categoryName);
    }

    @RequestMapping("get_category.do")
    @ResponseBody
    public ServerResponse getChildrenParallelCategory(HttpSession session,@RequestParam(value = "categoryId" ,defaultValue = "0") Integer categoryId){
        // 判断用户是否登录
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), ResponseCode.NEED_LOGIN.getDesc());
        }

        //检验是否是管理员
        ServerResponse serverResponse = iUserService.checkAdminRole(user);
        if (!serverResponse.isSuccess()) {
            return ServerResponse.createByErrorMessage("无权限操作，需要管理员权限");
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

        //检验是否是管理员
        ServerResponse serverResponse = iUserService.checkAdminRole(user);
        if (!serverResponse.isSuccess()) {
            return ServerResponse.createByErrorMessage("无权限操作，需要管理员权限");
        }
        return iCategoryService.selectCategoryAndChildrenById(categoryId);
    }
}
