package com.tempus.gss.product.hol.service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.tempus.gss.product.hol.api.entity.vo.bqy.*;
import com.tempus.gss.security.AgentUtil;
import com.tempus.gss.util.MathUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;

import com.alibaba.dubbo.config.annotation.Service;
import com.tempus.gss.bbp.util.StringUtil;
import com.tempus.gss.exception.GSSException;
import com.tempus.gss.product.hol.api.entity.HolMidBaseInfo;
import com.tempus.gss.product.hol.api.entity.ProfitPrice;
import com.tempus.gss.product.hol.api.entity.ResNameSum;
import com.tempus.gss.product.hol.api.entity.request.HotelDetailSearchReq;
import com.tempus.gss.product.hol.api.entity.request.HotelListSearchReq;
import com.tempus.gss.product.hol.api.entity.request.tc.IsBookOrderReq;
import com.tempus.gss.product.hol.api.entity.response.TCResponse;
import com.tempus.gss.product.hol.api.entity.response.tc.FacilityServices;
import com.tempus.gss.product.hol.api.entity.response.tc.ImgInfo;
import com.tempus.gss.product.hol.api.entity.response.tc.ProDetails;
import com.tempus.gss.product.hol.api.entity.response.tc.ProSaleInfoDetail;
import com.tempus.gss.product.hol.api.entity.response.tc.ResBaseInfo;
import com.tempus.gss.product.hol.api.entity.response.tc.ResProBaseInfo;
import com.tempus.gss.product.hol.api.entity.vo.bqy.request.BookOrderParam;
import com.tempus.gss.product.hol.api.entity.vo.bqy.request.QueryHotelParam;
import com.tempus.gss.product.hol.api.entity.vo.bqy.response.BookOrderResponse;
import com.tempus.gss.product.hol.api.entity.vo.bqy.room.AveragePrice;
import com.tempus.gss.product.hol.api.entity.vo.bqy.room.BaseRoomInfo;
import com.tempus.gss.product.hol.api.entity.vo.bqy.room.BedTypeInfo;
import com.tempus.gss.product.hol.api.entity.vo.bqy.room.BroadNetInfo;
import com.tempus.gss.product.hol.api.entity.vo.bqy.room.CancelLimitInfo;
import com.tempus.gss.product.hol.api.entity.vo.bqy.room.RoomBedTypeInfo;
import com.tempus.gss.product.hol.api.entity.vo.bqy.room.RoomInfoItem;
import com.tempus.gss.product.hol.api.entity.vo.bqy.room.RoomPriceDetail;
import com.tempus.gss.product.hol.api.entity.vo.bqy.room.RoomPriceInfo;
import com.tempus.gss.product.hol.api.entity.vo.bqy.room.RoomPriceItem;
import com.tempus.gss.product.hol.api.service.IBQYHotelConverService;
import com.tempus.gss.product.hol.api.service.IBQYHotelInterService;
import com.tempus.gss.product.hol.api.service.IBQYHotelSupplierService;
import com.tempus.gss.product.hol.api.service.IHolProfitService;
import com.tempus.gss.product.hol.api.util.DateUtil;
import com.tempus.gss.product.hol.utils.RedisService;
import com.tempus.gss.vo.Agent;

@Service
public class BQYHotelSupplierServiceImpl implements IBQYHotelSupplierService  {

	@Autowired
	private MongoTemplate mongoTemplate1;
	
	@Autowired
	private IBQYHotelInterService bqyHotelInterService;
	
	@Autowired
	private IBQYHotelConverService bqyHotelConverService;
	
	@Autowired
	IHolProfitService holProfitService;
	
	@Autowired
	RedisService redisService;
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	@Override
	public TCResponse<HotelInfo> queryHotelListForBack(HotelListSearchReq hotelSearchReq) {

		logger.info("bqy查询酒店列表开始");
		if (StringUtil.isNullOrEmpty(hotelSearchReq)) {
			logger.error("hotelSearchReq查询条件为空");
			throw new GSSException("获取酒店列表", "0101", "hotelSearchReq查询条件为空");
		}

		if (StringUtil.isNullOrEmpty(hotelSearchReq.getResId())
				&& StringUtil.isNullOrEmpty(hotelSearchReq.getCityCode())
				&& StringUtil.isNullOrEmpty(hotelSearchReq.getResGradeId())
				&& StringUtil.isNullOrEmpty(hotelSearchReq.getKeyword())) {
			hotelSearchReq.setCityCode("北京");
		}

		if (StringUtil.isNullOrEmpty(hotelSearchReq.getPageCount())) {
			logger.error("页码为空");
			throw new GSSException("获取酒店列表", "0107", "页码为空");
		}
		if (StringUtil.isNullOrEmpty(hotelSearchReq.getRowCount())) {
			logger.error("每页条数为空");
			throw new GSSException("获取酒店列表", "0108", "每页条数为空");
		}
		TCResponse<HotelInfo> response = new TCResponse<HotelInfo>();
		Query query = new Query();
		Criteria criatira = new Criteria();

		int skip = (hotelSearchReq.getPageCount() - 1) * (hotelSearchReq.getRowCount());
		query.skip(skip);
		query.limit(hotelSearchReq.getRowCount());

		//城市名称
		if (StringUtil.isNotNullOrEmpty(hotelSearchReq.getCityCode())) {
			criatira.and("cityName").is(hotelSearchReq.getCityCode());
		}

		//酒店名称关键字查询
		if (StringUtil.isNotNullOrEmpty(hotelSearchReq.getKeyword())) {
			String keyword = hotelSearchReq.getKeyword().trim();
			String escapeHtml = StringEscapeUtils.unescapeHtml(keyword);
			String[] fbsArr = { "(", ")" }; // "\\", "$", "(", ")", "*", "+",
											// ".", "[", "]", "?", "^", "{",
											// "}", "|"
			for (String key : fbsArr) {
				if (escapeHtml.contains(key)) {
					escapeHtml = escapeHtml.replace(key, "\\" + key);
				}
			}
			criatira.and("hotelName").regex("^.*" + escapeHtml + ".*$");// "^.*"+hotelName+".*$"
		}

		//酒店ID
		if (StringUtil.isNotNullOrEmpty(hotelSearchReq.getResId())) {
			criatira.and("_id").is(Long.valueOf(hotelSearchReq.getResId()));
		}
		
		//酒店等级
		if(StringUtil.isNotNullOrEmpty(hotelSearchReq.getResGradeId())){
			String hotelStar = hotelSearchReq.getResGradeId();
			switch (hotelStar) {
			case "23":
				//五星级
				criatira.and("hotelStar").is("5.00");
				break;
			case "24":
				//四星级
				criatira.and("hotelStar").is("4.00");
				break;
			case "26":
				//三星级
				criatira.and("hotelStar").is("3.00");
				break;
			case "27":
				//二星及二星以下
				criatira.and("hotelStar").in("2.00","1.00","0.00");
				break;
			}
		}
		
		query = query.addCriteria(criatira);
		// 获取酒店列表
		List<HotelInfo> hotelList = mongoTemplate1.find(query, HotelInfo.class);
		// 查询总数
		int totalCount = (int) mongoTemplate1.count(query, HotelInfo.class);
		// 总页数
		int totalPage = (int) (totalCount % hotelSearchReq.getRowCount() == 0
				? totalCount / hotelSearchReq.getRowCount() : (totalCount / hotelSearchReq.getRowCount() + 1));
		response.setTotalPatge(totalPage);
		response.setTotalCount(totalCount);
		response.setResponseResult(hotelList);
		logger.info("bqy查询酒店列表结束");
		return response;
	}

	@Override
	@Async
	public Future<TCResponse<ResBaseInfo>> queryHotelList(HotelListSearchReq hotelSearchReq) {
		TCResponse<HotelInfo> hotelResult = queryHotelListForBack(hotelSearchReq);
		List<HotelInfo> hotelList = hotelResult.getResponseResult();
		List<ResBaseInfo> resList = new ArrayList<>();
		for (HotelInfo h : hotelList) {
			ResBaseInfo resBaseInfo = bqyHotelConverService.bqyConvertTcHotelEntity(h);
			resList.add(resBaseInfo);
		}
		TCResponse<ResBaseInfo> result = new TCResponse<>();
		result.setResponseResult(resList);
		result.setPageNumber(hotelResult.getPageNumber());
		result.setTotalCount(hotelResult.getTotalCount());
		result.setTotalPatge(hotelResult.getTotalPatge());
		return new AsyncResult<TCResponse<ResBaseInfo>>(result);
	}

	@Override
	public int saleStatusUpdate(Agent agent, Long hotelId, Integer saleStatus) {
		if (StringUtil.isNullOrEmpty(agent)) {
			logger.error("agent对象为空");
			throw new GSSException("修改bqy酒店可售状态", "0102", "agent对象为空");
		}
		if (StringUtil.isNullOrEmpty(hotelId)) {
			throw new GSSException("修改bqy酒店可售状态", "0118", "传入RESID为空");
		}
		try {
			SimpleDateFormat sdfupdate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String updateTime = sdfupdate.format(new Date());
			Query query = Query.query(Criteria.where("_id").is(hotelId));
			Update update = Update.update("saleStatus", saleStatus).set("latestUpdateTime", updateTime);
			mongoTemplate1.upsert(query, update, HotelInfo.class);
		} catch (Exception e) {
			logger.error("修改可售状态出错" + e);
			throw new GSSException("修改可售状态", "0118", "修改可售状态失败");
		}
		return 1;
	}

	@Override
	@Async
	public Future<ResBaseInfo> singleHotelDetail(Agent agent, String hotelId, String checkinDate, String checkoutDate, String cityCode) throws Exception {
		try {
			QueryHotelParam query = new QueryHotelParam();
			if (StringUtils.isBlank(checkinDate) && StringUtils.isBlank(checkoutDate)) {
				Calendar date = Calendar.getInstance();  
		        Calendar dateAdd = Calendar.getInstance();
		        dateAdd.add(Calendar.MONTH, 2);
		        dateAdd.add(Calendar.DAY_OF_MONTH, -1);
		        query.setCheckInTime(sdf.format(date.getTime()));
				query.setCheckOutTime(sdf.format(dateAdd.getTime()));
			}else {
				query.setCheckInTime(checkinDate);
				query.setCheckOutTime(checkoutDate);
			}
			
			query.setCityCode(cityCode);
			query.setHotelId(Long.valueOf(hotelId));
			//ResBaseInfo resBaseInfo = null;
			
			//异步请求酒店图片和酒店详细信息
			Future<List<ImageList>> asyncHotelImage = bqyHotelInterService.asyncHotelImage(query);
			Future<List<RoomPriceItem>> asynRoomPrice = bqyHotelInterService.asyncRoomPrice(query);
			Future<List<ProfitPrice>> computeProfitByAgentFu = null;
            computeProfitByAgentFu = holProfitService.computeProfitByAgentNum(agent, agent.getNum(), "bqy");
			/*while (asynRoomPrice.isDone() && asyncHotelImage.isDone()) {
				break;
			}*/

			List<ImageList> bqyHotelImgList = asyncHotelImage.get();
			List<RoomPriceItem> roomPriceItemList = asynRoomPrice.get();
			List<ProDetails> proDetails = new ArrayList<>();
			if (null != roomPriceItemList && roomPriceItemList.size() > 0) {
				for (RoomPriceItem roomPriceItem : roomPriceItemList) {  //房型
					ProDetails proDetail = new ProDetails();
					BaseRoomInfo baseRoomInfo = roomPriceItem.getBaseRoomInfo();
					proDetail.setProId(baseRoomInfo.getRoomTypeID());
					proDetail.setResProName(baseRoomInfo.getRoomName().replaceAll("\\s*", "").replaceAll("（", "(").replaceAll("）", ")").trim());
					proDetail.setRoomFloor(baseRoomInfo.getFloorRange());
					proDetail.setRoomSize(baseRoomInfo.getAreaRange());

					//房型图片
					if (null != bqyHotelImgList && bqyHotelImgList.size() > 0) {
						for (int i = 0, len = bqyHotelImgList.size(); i < len; i++) {
							ImageList image = bqyHotelImgList.get(i);
							if (baseRoomInfo.getRoomTypeID().equals(image.getRoomTypeId())) {
								proDetail.setImgUrl(image.getImageUrl());
								break;
							}
						}
					}

					List<RoomInfoItem> roomInfoList = roomPriceItem.getRoomInfo();
					List<ResProBaseInfo> resProBaseInfos = new ArrayList<>();
					for (RoomInfoItem roomInfo : roomInfoList) {		//房间
						ResProBaseInfo resProBaseInfo = new ResProBaseInfo();
						resProBaseInfo.setResId(Long.parseLong(hotelId));
						resProBaseInfo.setProId(baseRoomInfo.getRoomTypeID());
						resProBaseInfo.setCustomerType(roomInfo.getSupplierId());	//用来存储酒店代理人Id
						resProBaseInfo.setPaymnetType(2);							//0：All-全部；1：SelfPay-现付；2：Prepay-预付
						resProBaseInfo.setSupplierType(2);
						List<BedTypeInfo> bedTypeInfo = baseRoomInfo.getBedTypeInfo();
						if (null != bedTypeInfo && bedTypeInfo.size() > 0) {
							String bedWidth = "";
							for (int i =0, len = bedTypeInfo.size(); i < len; i++) {
								if (i == 0) {
									bedWidth =  bedTypeInfo.get(i).getBedWidth();
									continue;
								}
								if (!bedWidth.contains(bedTypeInfo.get(i).getBedWidth())) {
									bedWidth = bedWidth + "," + bedTypeInfo.get(i).getBedWidth();
								}
							}
							resProBaseInfo.setBedSize(bedWidth);
							proDetail.setBedSize(bedWidth);
						}
						//产品名称
						resProBaseInfo.setSupPriceName(roomInfo.getRoomName().trim());

						//床型
						RoomBedTypeInfo roomBedTypeInfo = roomInfo.getRoomBedTypeInfo();
						if ("T".equals(roomBedTypeInfo.getHasKingBed())) {
							resProBaseInfo.setBedTypeName("大床");
						}else if ("T".equals(roomBedTypeInfo.getHasTwinBed())) {
							resProBaseInfo.setBedTypeName("双床");
						}else if ("T".equals(roomBedTypeInfo.getHasSingleBed())) {
							resProBaseInfo.setBedTypeName("单人床");
						}
						//价格
						RoomPriceInfo roomPriceInfo = roomInfo.getRoomPriceInfo();
						AveragePrice averagePrice = roomPriceInfo.getAveragePrice();
						resProBaseInfo.setRoomFeature(roomPriceInfo.getRatePlanCategory()); //预付检查字段
						List<RoomPriceDetail> roomPriceDetail = roomPriceInfo.getRoomPriceDetail();
						resProBaseInfo.setOtherDescription(roomPriceInfo.getIsJustifyConfirm());	//是否即时确认

						//取消规则
						CancelLimitInfo cancelLimitInfo = roomInfo.getCancelLimitInfo();
						if (null != cancelLimitInfo) {
							resProBaseInfo.setBookingNotes(cancelLimitInfo.getCancelPolicyInfo().trim());
							resProBaseInfo.setPolicyRemark(cancelLimitInfo.getLastCancelTime());
							resProBaseInfo.setSourceFrom(Long.valueOf(cancelLimitInfo.getPolicyType()));
						}
						//入住人数
						resProBaseInfo.setAdultCount(Integer.valueOf(roomInfo.getPerson()));
						//预定检查类型
						resProBaseInfo.setRoomFeature(roomPriceInfo.getRatePlanCategory());
						//供应商ID
						resProBaseInfo.setCustomerType(roomInfo.getSupplierId());
						//房间数
						String roomNumStr = roomPriceInfo.getRemainingRooms();
						if (roomNumStr.contains("+")) {
							roomNumStr = roomNumStr.substring(0, roomNumStr.indexOf("+"));
						}else if ("true".equalsIgnoreCase(roomNumStr)) {
							roomNumStr = "10";
						}
						if (StringUtils.isNotBlank(roomNumStr)) {
							resProBaseInfo.setProMinInventory(Integer.valueOf(roomNumStr));
						}else {
							resProBaseInfo.setProMinInventory(1);
						}

						//房间ID
						resProBaseInfo.setProductUniqueId(roomInfo.getRoomID());
						//是否无烟
						if (null != roomInfo.getSmokeInfo()) {
							  String hasRoomInNonSmokeArea = roomInfo.getSmokeInfo().getHasRoomInNonSmokeArea();
							  if ("T".equals(hasRoomInNonSmokeArea)) {
								  resProBaseInfo.setNonSmoking((byte) 1);
							  }else {
								  resProBaseInfo.setNonSmoking((byte) 0);
							  }
						  }
						//设置控润
						if(computeProfitByAgentFu!=null) {
								List<ProfitPrice> computeProfitByAgent = computeProfitByAgentFu.get();
							if(computeProfitByAgent != null && computeProfitByAgent.size() > 0) {
								String status = computeProfitByAgent.get(0).getStatus();
								if (StringUtil.isNullOrEmpty(status) || "1".equals(status)) {	//启用
									for(ProfitPrice profit : computeProfitByAgent) {
										BigDecimal lowerPrice = profit.getPriceFrom();
										BigDecimal upPrice = profit.getPriceTo();
										BigDecimal firPrice = averagePrice.getAverageprice();	//挂牌价
										if(lowerPrice.compareTo(firPrice) <= 0 && upPrice.compareTo(firPrice) >= 0) {
											BigDecimal rate = profit.getRate();
											rate = rate.multiply(new BigDecimal(0.01)).setScale(2, BigDecimal.ROUND_HALF_UP);
											resProBaseInfo.setRebateRateProfit(rate);
											resProBaseInfo.setConPrice((firPrice.setScale(0, BigDecimal.ROUND_UP)).intValue());	//平均价
											resProBaseInfo.setFirPrice((firPrice.setScale(0, BigDecimal.ROUND_UP)).intValue());	//首日价
											resProBaseInfo.setSettleFee(averagePrice.getSettleFee());	//结算价
											//价格弹窗
											TreeMap<String, ProSaleInfoDetail> mapPro=new TreeMap<String, ProSaleInfoDetail>();
											int daysBetween = DateUtil.daysBetween(checkinDate, checkoutDate);
											for (int i = 0; i < daysBetween; i++) {
												Date checkIn = sdf.parse(checkinDate);
												String checkInFormat = sdf.format(DateUtil.offsiteDay(checkIn, i));
												ProSaleInfoDetail pid = new ProSaleInfoDetail();
												//pid.setDistributionSalePrice(averagePrice.getSettleFee().intValue());
												pid.setDistributionSalePrice(firPrice.intValue());
												pid.setTcDirectPrice((firPrice.setScale(0, BigDecimal.ROUND_UP)).intValue());	//挂牌价
												mapPro.put(checkInFormat, pid);
											}
											resProBaseInfo.setProSaleInfoDetailsTarget(mapPro);
										}
									}

								}else if("0".equals(status)) {	//禁用(返佣为0,将返佣价格减去)
									for(ProfitPrice profit : computeProfitByAgent) {
										BigDecimal lowerPrice = profit.getPriceFrom();
										BigDecimal upPrice = profit.getPriceTo();
										BigDecimal firPrice = averagePrice.getAverageprice();	//结算价
										if(lowerPrice.compareTo(firPrice) <= 0 && upPrice.compareTo(firPrice) >= 0) {
											BigDecimal rate = profit.getRate();
											rate = rate.multiply(new BigDecimal(0.01)).setScale(2, BigDecimal.ROUND_HALF_UP);
											resProBaseInfo.setRebateRateProfit(BigDecimal.ZERO);
											BigDecimal endPrice = firPrice.subtract(firPrice.multiply(rate));
											endPrice = endPrice.setScale(0, BigDecimal.ROUND_UP);
											resProBaseInfo.setConPrice(endPrice.intValue());	//平均价
											resProBaseInfo.setFirPrice(endPrice.intValue());	//首日价
											resProBaseInfo.setSettleFee(averagePrice.getSettleFee());	//结算价
											//价格弹窗
											TreeMap<String, ProSaleInfoDetail> mapPro=new TreeMap<String, ProSaleInfoDetail>();
											int daysBetween = DateUtil.daysBetween(checkinDate, checkoutDate);
											for (int i = 0; i < daysBetween; i++) {
												Date checkIn = sdf.parse(checkinDate);
												String checkInFormat = sdf.format(DateUtil.offsiteDay(checkIn, i));
												ProSaleInfoDetail pid = new ProSaleInfoDetail();
												pid.setDistributionSalePrice(endPrice.intValue());
												pid.setTcDirectPrice(firPrice.intValue());			//未做处理的挂牌价
												mapPro.put(checkInFormat, pid);
											}
											resProBaseInfo.setProSaleInfoDetailsTarget(mapPro);
										}
									}
								}

							}
						}

						//是否有宽带
						BroadNetInfo broadNetInfo = roomInfo.getBroadNetInfo();
						if (null != broadNetInfo) {
							String hasBroadnet = broadNetInfo.getHasBroadnet();
							if (!"0".equals(hasBroadnet)) {
								String hasWirelessBroadnet = broadNetInfo.getHasWirelessBroadnet();
								List<String> hasBroadband = new ArrayList<>();
								if ("T".equals(hasWirelessBroadnet)) {
									if ("0".equals(broadNetInfo.getWirelessBroadnetFee())) {
										//免费无线
										hasBroadband.add("FreeWifi");
									}else{
										//收费无线
										hasBroadband.add("ChargeWifi");
									}
								}
								String hasWiredBroadnet = broadNetInfo.getHasWiredBroadnet();
								if ("T".equals(hasWiredBroadnet)) {
									String wiredBroadnetFee = broadNetInfo.getWiredBroadnetFee();
									if ("0".equals(wiredBroadnetFee)) {
										//免费有线
										hasBroadband.add("FreeWiredBroadband");
									}else {
										//收费有线
										hasBroadband.add("ChargeWiredBroadband");
									}
								}
								resProBaseInfo.setHasBroadband(hasBroadband);
							}
						}
						//是否有早餐
						if (null != roomPriceDetail && roomPriceDetail.size() > 0) {
							//早餐(0.无早;1.一份; 2.双份; 3.三份…)
							String hasBreakfast = roomPriceDetail.get(0).getBreakfast();
							resProBaseInfo.setBreakfastCount(Integer.parseInt(hasBreakfast));
						}else {
							resProBaseInfo.setBreakfastCount(0);
						}

						//是否有窗bqy(0.无窗; 1.部分有窗; 2.有窗;)
						//	   TC(0.无窗;  1.有窗;   2.部分有窗)
						String hasWindow = roomInfo.getHasWindow();
						if ("1".equals(hasWindow)) {
							resProBaseInfo.setHasWindows((byte)2);
						}else if ("2".equals(hasWindow)) {
							resProBaseInfo.setHasWindows((byte)1);
						}else {
							resProBaseInfo.setHasWindows((byte)0);
						}
						resProBaseInfos.add(resProBaseInfo);
					}
					proDetail.setResProBaseInfoList(resProBaseInfos);
					proDetails.add(proDetail);
				}
			}
			ResBaseInfo resBaseInfo = new ResBaseInfo();
			resBaseInfo.setProDetails(proDetails);
			return new AsyncResult<ResBaseInfo>(resBaseInfo);
		} catch (ParseException e) {
			e.printStackTrace();
			logger.error("酒店详情页面错误");
		}
		return null;
	}

	public void setHotelServiceName(ResBaseInfo resBaseInfo, String serviceNameStr) {
		if (StringUtils.isNoneBlank(serviceNameStr)) {
            List<FacilityServices> resFacilities = new ArrayList<>();
            String[] serviceNameArr = serviceNameStr.split(",");
            for (String serviceName : serviceNameArr) {
                FacilityServices facilityServices = new FacilityServices();
                facilityServices.setFacilityServicesName(serviceName);
                resFacilities.add(facilityServices);
            }
            resBaseInfo.setResFacilities(resFacilities);
        }
	}

	@Override
	public CityDetail getCityDetailByCityCode(Agent agent, String cityName) {
		/*Criteria criteria = new Criteria();
		Criteria criteria0 = Criteria.where("cityName").regex("^.*" + cityName + ".*$").and("supplierNo").is("0");
		Criteria criteria1 = Criteria.where("cityName").regex("^.*" + cityName + ".*$").and("supplierNo").is("1");
		criteria.orOperator(criteria0, criteria1);*/
		Criteria criteria = Criteria.where("cityName").regex("^.*" + cityName + ".*$");
		List<CityDetail> cityDeatil = mongoTemplate1.find(new Query(criteria), CityDetail.class);
		if (null != cityDeatil && cityDeatil.size() > 0) {
			return cityDeatil.get(0);
		}
		return new CityDetail();
	}

	@Override
	public ResBaseInfo getById(Agent agent, Long bqyHolId) {
		long start = System.currentTimeMillis();
		String perKey = "BQYHOL"+bqyHolId;
		ResBaseInfo resBaseInfo = (ResBaseInfo) redisService.get(perKey);
		long end = System.currentTimeMillis();
        System.out.println("redis，耗时：" + (end - start) + "毫秒");
		return resBaseInfo;
	}



	@Override
	public BookOrderResponse isBookOrder(Agent agent, IsBookOrderReq isBookOrderReq, Integer flag) {

		BookOrderParam query = new BookOrderParam();
		query.setCheckInTime(isBookOrderReq.getComeDate());
		query.setCheckOutTime(isBookOrderReq.getLeaveDate());
		query.setLateArrivalTime(isBookOrderReq.getComeTime());
		query.setGuestCount(isBookOrderReq.getGuestCount());
		query.setQuantity(isBookOrderReq.getBookingCount());
		query.setRatePlanCode(String.valueOf(isBookOrderReq.getProductUniqueId()));
		query.setRatePlanCategory(isBookOrderReq.getRatePlanCategory());
		query.setSupplierId(isBookOrderReq.getSupplierId());
		Long holMidId = isBookOrderReq.getResId();
		HolMidBaseInfo holMidBaseInfo = mongoTemplate1.findOne(new Query(Criteria.where("_id").is(String.valueOf(holMidId))), HolMidBaseInfo.class);
		
		Long resId = null;
		if (flag - 1 == 0) {
			List<ResNameSum> resNameSumList = holMidBaseInfo.getResNameSum();
			if (null != resNameSumList && resNameSumList.size() > 0) {
				for (ResNameSum resNameSum : resNameSumList) {
					if (resNameSum.getResType() - 2 == 0) {
						resId = resNameSum.getResId();
						break;
					}
				}
			}
		}else if (flag == 0) {
			resId = isBookOrderReq.getResId();
		}
		
		query.setHotelId(resId);
		BookOrderResponse bookOrder = bqyHotelInterService.isBookOrder(query);
		if (null != bookOrder) {
			if (bookOrder.getCanBook()) {
				if (isBookOrderReq.getBookingCount() > bookOrder.getAvailableQuantity()) {
					//房间数不够
					bookOrder.setCanBook(false);
					return bookOrder;
				}
			}else {
				int saleStatus = 0;
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String updateTime = sdf.format(new Date());
				Query updatQquery  = new Query(Criteria.where("_id").is(isBookOrderReq.getResId()));
				Update update = Update.update("saleStatus", saleStatus).set("latestUpdateTime", updateTime);
				mongoTemplate1.upsert(updatQquery, update, HotelInfo.class);
				return bookOrder;
			}
		}else {
			throw new GSSException("订单是否可定信息", "0101", "解析数据异常");
		}
		return bookOrder;
	}

	@Override
	public List<ImgInfo> listImgByHotelId(Agent agent, long hotelId) {
		List<ImageList> hotelImageList = null;
		HotelImage hotelImage = mongoTemplate1.findOne(new Query(Criteria.where("_id").is(hotelId)), HotelImage.class);
		if (null != hotelImage && null != hotelImage.getImageList() && hotelImage.getImageList().size() > 0) {
			hotelImageList = hotelImage.getImageList();
		}else {
			QueryHotelParam query = new QueryHotelParam();
			query.setHotelId(hotelId);
			hotelImageList = bqyHotelInterService.queryHotelImage(query);
		}
		List<ImgInfo> imgList = bqyHotelConverService.convertTCImg(hotelImageList);
		return imgList;
	}

	@Override
	public Map<String, Object> getProByRoomCode(Agent agent, RoomInfo roomInfo) {
		try {
			String holMidId = roomInfo.getHolMidId();
			HolMidBaseInfo holMid = mongoTemplate1.findOne(new Query(Criteria.where("_id").is(holMidId)), HolMidBaseInfo.class);
			Long resId = null;
			List<ResNameSum> resNameSumList = holMid.getResNameSum();
			for (ResNameSum resNameSum : resNameSumList) {
				if (resNameSum.getResType() - 2 == 0) {
					resId = resNameSum.getResId();
					break;
				}
			}
			//酒店基础信息
			ResBaseInfo resBaseInfo = bqyHotelConverService.bqyConvertTcHotelEntity(getHotelInfoById(agent, resId));
			int oneDayPrice = roomInfo.getPrice().intValue();
			ResProBaseInfo useRoom = new ResProBaseInfo();
			String checkInDate = roomInfo.getCheckInDate();
			String checkOutDate = roomInfo.getCheckOutDate();

			int diff  = DateUtil.daysBetween(checkInDate, checkOutDate);
			Future<List<ProfitPrice>> computeProfitByAgentFu = holProfitService.computeProfitByAgentNum(agent, agent.getNum(), "bqy");
			//设置控润
			if(computeProfitByAgentFu!=null) {
				List<ProfitPrice> computeProfitByAgent  = computeProfitByAgentFu.get();
				String status = computeProfitByAgent.get(0).getStatus();
				if (StringUtil.isNullOrEmpty(status) || "1".equals(status)) {
					if(computeProfitByAgent != null && computeProfitByAgent.size() > 0) {
						for(ProfitPrice profit : computeProfitByAgent) {
							BigDecimal lowerPrice = profit.getPriceFrom();
							BigDecimal upPrice = profit.getPriceTo();
							BigDecimal firPrice = roomInfo.getPrice();
							if(lowerPrice.compareTo(firPrice) <= 0 && upPrice.compareTo(firPrice) >= 0) {
								BigDecimal rate = profit.getRate();
								rate = rate.multiply(new BigDecimal(0.01)).setScale(2, BigDecimal.ROUND_HALF_UP);
								useRoom.setRebateRateProfit(rate);
							}
						}
					}
				}else if ("0".equals(status)){
					useRoom.setRebateRateProfit(BigDecimal.ZERO);
				}
			}
			useRoom.setUserSumPrice(oneDayPrice * diff);
			useRoom.setTotalRebateRateProfit(new BigDecimal(oneDayPrice).multiply(useRoom.getRebateRateProfit()));
			useRoom.setConPrice(roomInfo.getPrice().intValue());
			useRoom.setAdultCount(roomInfo.getPerson());
			useRoom.setResProName(roomInfo.getRoomTypeName());
			useRoom.setSupPriceName(roomInfo.getRoomName());
			useRoom.setProId(roomInfo.getRoomTypeId().toString());
			useRoom.setProductUniqueId(roomInfo.getRoomId().toString());
			useRoom.setBreakfastCount(roomInfo.getBreakfastCount());
			useRoom.setBookingNotes(roomInfo.getCancelPolicyInfo());
			useRoom.setPolicyRemark(roomInfo.getLastCancelTime());
			useRoom.setSourceFrom(Long.valueOf(roomInfo.getPolicyType()));
			useRoom.setBedTypeName(roomInfo.getBedTypeName());

			TreeMap<String, ProSaleInfoDetail> mapPro=new TreeMap<String, ProSaleInfoDetail>();
			for (int i = 0; i < diff; i++) {
				Date checkIn = sdf .parse(checkInDate);
				String checkInFormat = sdf.format(com.tempus.gss.product.hol.api.util.DateUtil.offsiteDay(checkIn, i));
				ProSaleInfoDetail pid = new ProSaleInfoDetail();
				pid.setDistributionSalePrice(oneDayPrice);
				if (null != roomInfo.getBreakfastCount()) {
					pid.setBreakfastNum(roomInfo.getBreakfastCount());
				}else {
					pid.setBreakfastNum(0);
				}
				mapPro.put(checkInFormat, pid);
			}
			useRoom.setProSaleInfoDetailsTarget(mapPro);

			Map<String, Object> map = new HashMap<>();
			map.put("resBaseInfo", resBaseInfo);
			map.put("userRoom", useRoom);
			map.put("diff", diff);
			return map;
		} catch (ParseException e) {
			e.printStackTrace();
		}catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	//@Cacheable(value = "ResBaseInfo", key = "#bqyResId", unless = "")
	public ResBaseInfo queryHotelBaseInfo(Agent agent, Long bqyResId, String checkInDate, String checkOutDate, String cityCode) throws Exception{

		ResBaseInfo resBaseInfo = getById(agent, bqyResId);
		if (null != resBaseInfo) {
			return resBaseInfo;
		}
		HotelInfo hotelInfo = mongoTemplate1.findOne(new Query(Criteria.where("_id").is(bqyResId)), HotelInfo.class);
		resBaseInfo = bqyHotelConverService.bqyConvertTcHotelEntity(hotelInfo);
		QueryHotelParam query = new QueryHotelParam();
		query.setCheckInTime(checkInDate);
		query.setCheckOutTime(checkOutDate);
		query.setCityCode(cityCode);
		query.setHotelId(bqyResId);
		/*Future<HotelEntity> hotelEntityFu = bqyHotelInterService.queryHotelDetail(query);

		HotelEntity hotelEntity = hotelEntityFu.get();
		//酒店政策
		List<Policy> policyList = hotelEntity.getPolicy();
		bqyHotelPolicyConver(resBaseInfo, policyList);
		//酒店服务
		String serviceNameStr = hotelEntity.getServiceName();
		String bulitTime = hotelEntity.getBulitTime();*/
		List<Policy> policyList = hotelInfo.getPolicy();
		bqyHotelPolicyConver(resBaseInfo, policyList);
		String serviceNameStr = hotelInfo.getServiceName();
		setHotelServiceName(resBaseInfo, serviceNameStr);
		String bulitTime = hotelInfo.getWhenBuilt();
		String defaultTime = "0000-00-00 00:00:00";
		if (StringUtils.isNoneBlank(bulitTime)) {
			resBaseInfo.setWhenBuilt(bulitTime);
		}else {
			resBaseInfo.setWhenBuilt(defaultTime);
		}
		resBaseInfo.setEstablishmentDate(defaultTime);
		resBaseInfo.setRenovationDate(defaultTime);
		redisService.set("BQYHOL"+ bqyResId, resBaseInfo, Long.valueOf(60 * 60 * 24 * 3));
		return resBaseInfo;
	}

	@Override
	public HotelInfo getHotelInfoById(Agent agent, Long bqyResId) {
		return mongoTemplate1.findOne(new Query(Criteria.where("_id").is(bqyResId)), HotelInfo.class);
	}

	public void bqyHotelPolicyConver(ResBaseInfo resBaseInfo, List<Policy> policyList) {
		for (Policy policy : policyList) {
			String policyName = policy.getPloicyName();
			switch (policyName) {
				case "ArrivalAndDeparture":
					resBaseInfo.setArrivalAndDeparture(policy.getPolicyText());
					break;
				case "Cancel":
					resBaseInfo.setCancelDescription(policy.getPolicyText());
					break;
				case "DepositAndPrepaid":
					resBaseInfo.setDepositAndPrepaid(policy.getPolicyText());
					break;
				case "Pet":
					resBaseInfo.setPetDescription(policy.getPolicyText());
					break;
				case "Child":
					resBaseInfo.setChildDescription(policy.getPolicyText());
					break;
			}
		}
	}
}
