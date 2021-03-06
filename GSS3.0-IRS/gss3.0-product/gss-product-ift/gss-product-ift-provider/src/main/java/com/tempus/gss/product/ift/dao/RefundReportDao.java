package com.tempus.gss.product.ift.dao;

import com.baomidou.mybatisplus.mapper.AutoMapper;
import com.tempus.gss.product.ift.api.entity.vo.IftReportRefundVo;
import com.tempus.gss.product.ift.api.entity.vo.ReportRefundVo;
import com.tempus.gss.product.ift.api.entity.vo.ReportVo;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface RefundReportDao extends AutoMapper<ReportVo>{
   List<ReportVo> getAll(RowBounds page,ReportVo reportIn);

   List<ReportRefundVo> getAllWithList(RowBounds page, ReportVo reportIn);

   List<ReportVo> queryReportRecords(ReportVo reportIn);

    List<IftReportRefundVo> getAllRefundRecords(ReportVo reportIn);
}