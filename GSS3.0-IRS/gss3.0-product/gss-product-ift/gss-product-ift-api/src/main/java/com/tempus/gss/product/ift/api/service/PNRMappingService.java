package com.tempus.gss.product.ift.api.service;

import com.tempus.gss.product.common.entity.RequestWithActor;
import com.tempus.gss.product.ift.api.entity.QueryIBEDetail;
import com.tempus.gss.vo.Agent;

import java.util.Map;

/**
 * Created by 杨威 on 2017/9/20.
 */
public interface PNRMappingService {
    QueryIBEDetail pnrMapping(RequestWithActor<String> flightQueryRequest, String customerType,String isCivilTicket);
    
    QueryIBEDetail contentPnrMapping(RequestWithActor<String> flightQueryRequest, String customerType,String isCivilTicket);
    //外采pnr内容解析
    QueryIBEDetail outSourcePnrMapping(RequestWithActor<String> flightQueryRequest, String customerType,String isCivilTicket);
    
    /**
     * 验证PNR是否可用
     **/
    Map<String,String> pnrValidate(Agent agent, String pnrType, String pnrCode, String contentPnr);

}
