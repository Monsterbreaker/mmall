package com.mmall.common;

public class Const {

    public static final String CURRENT_USER="current_user";

    public static final String EMAIL="email";
    public static final String USERNAME="username";
    public static final String PASSWORD="password";

    public static final String ftpHost="ftp.server.http.prefix";

    public interface ROLE{
        int ROLE_CUMSTOMER=0;//普通用户
        int ROLE_SELLER=1;//商家
        int ROLE_ADMIN=2;//管理员
    }

    public interface USER_STATUS{
        int USER_STATUS_NORMAL=0;//正常状态
        int USER_STATUS_FROZEN=1;//冻结状态
    }

    public interface Cart {
        int CHECKED = 1;//购物车商品为选中状态
        int UNCHECKED = 0;//未选中
    }

    public enum ProductStatusEnum{
        ON_SALE(1,"在线");
        private String value;
        private int code;
        ProductStatusEnum(int code,String value){
            this.code = code;
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public int getCode() {
            return code;
        }
    }

    public enum OrderStatusEnum{
        CANCELED(0,"已取消"),
        NO_PAY(10,"未支付"),
        PAID(20,"已付款"),
        SHIPPED(40,"已发货"),
        ORDER_SUCCESS(50,"订单完成"),
        ORDER_CLOSE(60,"订单关闭");


        OrderStatusEnum(int code,String value){
            this.code = code;
            this.value = value;
        }
        private String value;
        private int code;

        public String getValue() {
            return value;
        }

        public int getCode() {
            return code;
        }

        public static OrderStatusEnum codeOf(int code){
            for(OrderStatusEnum orderStatusEnum : values()){
                if(orderStatusEnum.getCode() == code){
                    return orderStatusEnum;
                }
            }
            throw new RuntimeException("没有找到对应的枚举");
        }
    }

    public enum PayPlatformEnum{
        ALIPAY(1,"支付宝");

        PayPlatformEnum(int code,String value){
            this.code = code;
            this.value = value;
        }
        private String value;
        private int code;

        public String getValue() {
            return value;
        }

        public int getCode() {
            return code;
        }

        public static PayPlatformEnum codeOf(int code){
            for(PayPlatformEnum payPlatformEnum:values()){
                if(payPlatformEnum.getCode()==code){
                    return payPlatformEnum;
                }
            }
            throw new RuntimeException("没有找到对应的枚举");
        }
    }

    public enum PaymentTypeEnum{
        ONLINE_PAY(1,"在线支付");

        PaymentTypeEnum(int code,String value){
            this.code = code;
            this.value = value;
        }
        private String value;
        private int code;

        public String getValue() {
            return value;
        }

        public int getCode() {
            return code;
        }


        public static PaymentTypeEnum codeOf(int code){
            for(PaymentTypeEnum paymentTypeEnum : values()){
                if(paymentTypeEnum.getCode() == code){
                    return paymentTypeEnum;
                }
            }
            throw new RuntimeException("没有找到对应的枚举");
        }
    }
}
