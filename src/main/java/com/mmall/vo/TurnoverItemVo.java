package com.mmall.vo;

import java.math.BigDecimal;

/**
 * Created by Monsterbreaker on 2019/5/14.
 *
 */
public class TurnoverItemVo {
    private String date;

    private BigDecimal turnover;

    private Integer count;

    public TurnoverItemVo(){
    }

    public TurnoverItemVo(String date, BigDecimal turnove, Integer count) {
        this.date = date;
        this.turnover = turnove;
        this.count = count;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public BigDecimal getTurnover() {
        return turnover;
    }

    public void setTurnover(BigDecimal turnover) {
        this.turnover = turnover;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
