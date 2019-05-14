package com.mmall.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by Monsterbreaker on 2019/5/14.
 */
public class TurnoverVo {
    private List<TurnoverItemVo> turnoverItemVoList;

    private BigDecimal turnover;

    private Integer days;

    public List<TurnoverItemVo> getTurnoverItemVoList() {
        return turnoverItemVoList;
    }

    public void setTurnoverItemVoList(List<TurnoverItemVo> turnoverItemVoList) {
        this.turnoverItemVoList = turnoverItemVoList;
    }

    public BigDecimal getTurnover() {
        return turnover;
    }

    public void setTurnover(BigDecimal turnover) {
        this.turnover = turnover;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }
}
