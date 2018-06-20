package com.tempus.gss.product.ift.api.service;

import com.tempus.gss.controller.Result;
import com.tempus.gss.order.entity.DifferenceOrder;
import com.tempus.gss.product.common.entity.RequestWithActor;
import com.tempus.gss.product.ift.api.entity.vo.ExtraOrderQueryVo;
import com.tempus.gss.vo.Agent;

public interface IExtraOrderService {
    /**
     * 创建杂费单
     * @param agent
     * @param requestWithActor
     */
    public Result createExtraOrder(RequestWithActor<DifferenceOrder> requestWithActor);

    /**
     * 更新已经支付完的杂费单状态为已完结
     *
     */
    public void updateExtraOrderStatus(RequestWithActor<DifferenceOrder> requestWithActor) throws Exception;



}
