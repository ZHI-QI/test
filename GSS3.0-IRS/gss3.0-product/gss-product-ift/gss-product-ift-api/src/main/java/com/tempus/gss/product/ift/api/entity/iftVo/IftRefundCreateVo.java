package com.tempus.gss.product.ift.api.entity.iftVo;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tempus.gss.product.ift.api.entity.vo.PassengerLegVo;
import com.tempus.gss.serializer.LongSerializer;

public class IftRefundCreateVo implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/*销售单编号*/
	@JsonSerialize(using = LongSerializer.class)
	public Long saleOrderNo;
	
	public List<PassengerLegVo> passengerLegVoList;

	/*废退类型 1.废票、2.退票*/
	public Integer refundType;
	/*废退方式：1：自愿、2：非自愿*/
	public Integer refundWay;

	/**
	 * 联系人姓名
	 */
	public String contactName;

	/**
	 * 联系人手机号
	 */
	public String contactPhone;

	/**
	 * 废退原因
	 */
	public String refundReason;
	
	/**
	 * 用户备注
	 */
	private String customerRemark;

	public List<PassengerLegVo> getPassengerLegVoList() {

		return passengerLegVoList;
	}

	public void setPassengerLegVoList(List<PassengerLegVo> passengerLegVoList) {

		this.passengerLegVoList = passengerLegVoList;
	}

	public Integer getRefundType() {

		return refundType;
	}

	public void setRefundType(Integer refundType) {

		this.refundType = refundType;
	}

	public Integer getRefundWay() {

		return refundWay;
	}

	public void setRefundWay(Integer refundWay) {

		this.refundWay = refundWay;
	}

	public String getRefundReason() {

		return refundReason;
	}

	public void setRefundReason(String refundReason) {

		this.refundReason = refundReason;
	}

	public String getContactName() {

		return contactName;
	}

	public void setContactName(String contactName) {

		this.contactName = contactName;
	}

	public String getContactPhone() {

		return contactPhone;
	}

	public void setContactPhone(String contactPhone) {

		this.contactPhone = contactPhone;
	}

	public String getCustomerRemark() {
		return customerRemark;
	}

	public void setCustomerRemark(String customerRemark) {
		this.customerRemark = customerRemark;
	}

	public Long getSaleOrderNo() {
		return saleOrderNo;
	}

	public void setSaleOrderNo(Long saleOrderNo) {
		this.saleOrderNo = saleOrderNo;
	}
	
	
}
