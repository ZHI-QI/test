package com.tempus.gss.product.ift.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import com.tempus.gss.product.ift.api.entity.SaleOrderDetail;
import com.tempus.gss.product.ift.api.entity.vo.SaleOrderDetailVo;
@Component
public interface SaleOrderDetailDao extends BaseDao<SaleOrderDetail, SaleOrderDetailVo> {

	/*
	* 根据销售编号查询销售单明细
	* */
	public List<SaleOrderDetail> selectBySaleOrderNo(Long saleOrderNo);
 
	/**
	 * 根据乘客编号，行程单编号获取人+航段信息
	 * @param passengerNo
	 * @param legNo
	 * @return
	 */
	public SaleOrderDetail selectByPassengerAndLeg(Long passengerNo,Long legNo);

	/**
	 * 根据票号取人家航段
	 * @param ticketNo
	 * @return
	 */
	public List<SaleOrderDetail> selectByTicketNo(String ticketNo);

	public int batchUpdate(List<SaleOrderDetail> list);

	int updateByOrderNo(SaleOrderDetail saleOrderDetail);

	/***
	 * 根据改签单
	 * @param saleOrderDetail
	 * @return
	 */
	int updateByChangeOrderNo(SaleOrderDetail saleOrderDetail);

	/*
        * 将订单中已核价未支付的订单状态改为未核价
        * */
	int updateSaleOrderDetailStatus();

    int updateSaleOrderDetailStatusByNo(Long saleOrderNo);

	List<SaleOrderDetail> selectByPassengerAndSaleOrder(SaleOrderDetail saleOrderDetail);

    int updateSaleOrderDetailStatusByNoAndStatus(@Param("saleOrderNo") Long saleOrderNo, @Param("status")int status);

    List<SaleOrderDetail> selectBySaleChangeNoList(@Param("saleChangeNoList")List<Long> saleChangeNoList);

	/**
	 * 查询乘客最新的有票号的详情
	 * @param passengerNo
	 * @return
	 */
	SaleOrderDetail selectLastDetailByPgerNo(Long passengerNo);
}