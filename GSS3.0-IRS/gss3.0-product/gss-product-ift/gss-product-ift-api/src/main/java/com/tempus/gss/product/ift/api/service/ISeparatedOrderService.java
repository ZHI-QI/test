package com.tempus.gss.product.ift.api.service;

import com.baomidou.mybatisplus.plugins.Page;
import com.tempus.gss.product.common.entity.RequestWithActor;
import com.tempus.gss.product.ift.api.entity.SaleOrderExt;
import com.tempus.gss.product.ift.api.entity.SeparatedOrder;
import com.tempus.gss.product.ift.api.entity.vo.SeparatedOrderVo;

/**
 * Created by Administrator on 2017/11/16.
 */
public interface ISeparatedOrderService {
    /***
     * 获取分单列表
     * @param page
     * @param requestWithActor
     * @return
     */
    Page<SeparatedOrder> pageList(Page<SeparatedOrder> page, RequestWithActor<SeparatedOrderVo> requestWithActor);

    /**
     * 修改出票人
     * @param saleOrderNo
     */
    int updateSeparatedOrder(Long saleOrderNo,String userId,String currentUserId);
}
