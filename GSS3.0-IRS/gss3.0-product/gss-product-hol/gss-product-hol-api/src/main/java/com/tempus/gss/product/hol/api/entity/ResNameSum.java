package com.tempus.gss.product.hol.api.entity;

import java.io.Serializable;

public class ResNameSum implements Serializable{
	private static final long serialVersionUID = 1L;
	
	/**
	 * 酒店id
	 */
	private Long resId;
	
	/**
	 * 酒店名称
	 */
	private String resName;
	/**
	 * 酒店地址
	 */
	private String resAdress;
	/**
	 * 酒店电话号码
	 */
	private String resPhone;
	/**
	 * 酒店经度
	 */
	private String lon;
	/**
	 * 酒店纬度
	 */
	private String lat;
	
	/**
	 * 供应商编号
	 */
	private String supplierNo;
	
	
	/**
	 * 1: TC, 2: BQY, 3: TY, 0: All, 
	 */
	private Integer resType;


	public Long getResId() {
		return resId;
	}


	public void setResId(Long resId) {
		this.resId = resId;
	}


	public String getResName() {
		return resName;
	}


	public void setResName(String resName) {
		this.resName = resName;
	}


	public String getSupplierNo() {
		return supplierNo;
	}


	public void setSupplierNo(String supplierNo) {
		this.supplierNo = supplierNo;
	}


	public Integer getResType() {
		return resType;
	}


	public void setResType(Integer resType) {
		this.resType = resType;
	}


	public String getResAdress() {
		return resAdress;
	}


	public void setResAdress(String resAdress) {
		this.resAdress = resAdress;
	}


	public String getResPhone() {
		return resPhone;
	}


	public void setResPhone(String resPhone) {
		this.resPhone = resPhone;
	}


	public String getLon() {
		return lon;
	}


	public void setLon(String lon) {
		this.lon = lon;
	}


	public String getLat() {
		return lat;
	}


	public void setLat(String lat) {
		this.lat = lat;
	}
	
}
