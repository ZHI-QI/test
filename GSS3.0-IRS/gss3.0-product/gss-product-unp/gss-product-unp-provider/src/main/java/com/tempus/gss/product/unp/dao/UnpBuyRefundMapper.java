package com.tempus.gss.product.unp.dao;

import com.baomidou.mybatisplus.plugins.Page;
import com.tempus.gss.product.unp.api.entity.UnpBuyRefund;
import com.tempus.gss.product.unp.api.entity.vo.UnpOrderQueryVo;

/**
 * @author ZhangBro
 */
public interface UnpBuyRefundMapper {
    int deleteByPrimaryKey(Long buyRefundOrderNo);
    
    int insert(UnpBuyRefund record);
    
    int insertSelective(UnpBuyRefund record);
    
    UnpBuyRefund selectByPrimaryKey(Long buyRefundOrderNo);
    
    Page<UnpBuyRefund> queryList(Page<UnpBuyRefund> wrapper, UnpOrderQueryVo vo);
    
    int updateByPrimaryKeySelective(UnpBuyRefund record);
    
    int updateByPrimaryKey(UnpBuyRefund record);
}