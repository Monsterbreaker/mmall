package com.mmall.service;

import com.github.pagehelper.PageInfo;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.Product;
import com.mmall.vo.ProductDetailVo;

public interface IProductService {
    ServerResponse<String> saveOrUpdateProduct(Integer sellerId, String sellerName, Product product);
    ServerResponse<String> setSaleStatus(Integer sellerId, Integer productId,Integer status);
    ServerResponse<ProductDetailVo> manageProductDetail(Integer productId,Integer sellerId);
    ServerResponse<PageInfo> getProductList(int pageNum, int pageSize, int sellId);
    ServerResponse<PageInfo> searchProduct(String productName,Integer productId, int pageNum,int pageSize,int sellId);
    ServerResponse<ProductDetailVo> getProductDetail(Integer productId);
    ServerResponse<PageInfo> getProductByKeywordCategory(String keyword,Integer categoryId,int pageNum,int pageSize,String orderBy);
}
