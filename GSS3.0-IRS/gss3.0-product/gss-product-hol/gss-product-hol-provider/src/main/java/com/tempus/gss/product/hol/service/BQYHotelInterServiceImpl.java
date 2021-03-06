package com.tempus.gss.product.hol.service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.tempus.gss.product.hol.api.entity.vo.bqy.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.toolkit.IdWorker;
import com.tempus.gss.bbp.util.StringUtil;
import com.tempus.gss.cps.entity.Supplier;
import com.tempus.gss.cps.service.ISupplierService;
import com.tempus.gss.exception.GSSException;
import com.tempus.gss.order.entity.BuyOrder;
import com.tempus.gss.order.entity.SaleOrder;
import com.tempus.gss.order.service.IBuyOrderService;
import com.tempus.gss.order.service.IPlanAmountRecordService;
import com.tempus.gss.order.service.ISaleOrderService;
import com.tempus.gss.product.hol.api.entity.HolMidBaseInfo;
import com.tempus.gss.product.hol.api.entity.ResNameSum;
import com.tempus.gss.product.hol.api.entity.ResToMinPrice;
import com.tempus.gss.product.hol.api.entity.request.tc.OrderCreateReq;
import com.tempus.gss.product.hol.api.entity.response.HotelOrder;
import com.tempus.gss.product.hol.api.entity.response.tc.OwnerOrderStatus;
import com.tempus.gss.product.hol.api.entity.response.tc.ResBrandInfo;
import com.tempus.gss.product.hol.api.entity.response.tc.TcOrderStatus;
import com.tempus.gss.product.hol.api.entity.vo.bqy.request.BookOrderParam;
import com.tempus.gss.product.hol.api.entity.vo.bqy.request.CreateOrderReq;
import com.tempus.gss.product.hol.api.entity.vo.bqy.request.OrderCancelParam;
import com.tempus.gss.product.hol.api.entity.vo.bqy.request.OrderPayReq;
import com.tempus.gss.product.hol.api.entity.vo.bqy.request.QueryCityInfoParam;
import com.tempus.gss.product.hol.api.entity.vo.bqy.request.QueryHotelIdParam;
import com.tempus.gss.product.hol.api.entity.vo.bqy.request.QueryHotelInfoParam;
import com.tempus.gss.product.hol.api.entity.vo.bqy.request.QueryHotelListParam;
import com.tempus.gss.product.hol.api.entity.vo.bqy.request.QueryHotelParam;
import com.tempus.gss.product.hol.api.entity.vo.bqy.response.BookOrderResponse;
import com.tempus.gss.product.hol.api.entity.vo.bqy.response.CreateOrderRespone;
import com.tempus.gss.product.hol.api.entity.vo.bqy.response.HotelLocationEntity;
import com.tempus.gss.product.hol.api.entity.vo.bqy.response.HotelOrderInfo;
import com.tempus.gss.product.hol.api.entity.vo.bqy.response.OrderCancelResult;
import com.tempus.gss.product.hol.api.entity.vo.bqy.response.OrderPayResult;
import com.tempus.gss.product.hol.api.entity.vo.bqy.room.BaseRoomInfo;
import com.tempus.gss.product.hol.api.entity.vo.bqy.room.RoomBedTypeInfo;
import com.tempus.gss.product.hol.api.entity.vo.bqy.room.RoomInfoItem;
import com.tempus.gss.product.hol.api.entity.vo.bqy.room.RoomPriceInfo;
import com.tempus.gss.product.hol.api.entity.vo.bqy.room.RoomPriceItem;
import com.tempus.gss.product.hol.api.service.IBQYHotelInterService;
import com.tempus.gss.product.hol.api.syn.IHolMongoQuery;
import com.tempus.gss.product.hol.api.util.DocumentUtil;
import com.tempus.gss.product.hol.api.util.OrderStatusUtils;
import com.tempus.gss.product.hol.api.util.Tool;
import com.tempus.gss.product.hol.dao.HotelOrderMapper;
import com.tempus.gss.product.hol.utils.HolAnnotation;
import com.tempus.gss.product.hol.utils.HttpClientUtil;
import com.tempus.gss.system.service.IMaxNoService;
import com.tempus.gss.util.JsonUtil;
import com.tempus.gss.vo.Agent;

@Service
public class BQYHotelInterServiceImpl implements IBQYHotelInterService {

	@Value("${bqy.agentId}")
	private String BQY_AGENTID;		//代理人Id
	
	@Value("bqy.key")
	private String BQY_KEY;			
	
	private String TOKEN;
	
	@Value("${bqy.hotelId.list.url}")
	private String BQY_HOTELID_LIST_URL;		//酒店Id集合URL
	
	@Value("${bqy.hotel.info.url}")
	private String BQY_HOTEL_INFO_URL;			//酒店信息URL
	
	@Value("${bqy.all.hotel.list.url}")
	private String BQY_ALL_HOTEL_LIST; 			//酒店列表请求URL

	@Value("${bqy.hotel.detail.url}")
	private String BQY_HOTEL_DETAIL_URL; 		//酒店详情URL

	@Value("${bqy.city.info.url}")
	private String BQY_CITY_INFO_URL; 			//城市信息URL
	
	@Value("${bqy.city.info.url_2}")
	private String BQY_CITY_INFO_URL_2; 		//城市信息URL
	
	@Value("${bqy.hotel.facility.url}")
	private String BQY_HOTEL_FACILITY_URL;		//酒店设施服务详情URL

	@Value("${bqy.hotel.image.url}")
	private String BQY_HOTEL_IMAGE_URL;			//酒店图片URL
	
	@Value("${bqy.hotel.room.image.url}")
	private String BQY_HOTEL_ROOM_IMAGE_URL;	//酒店房型图片URL
	
	@Value("${bqy.hotel.room.price.url}")
	private String BQY_HOTEL_ROOM_PRICE_URL;		//酒店房间价格
	
	@Value("${bqy.hotel.book.order.url}")
	private String BQY_HOTEL_BOOK_ORDER_URL;		//酒店房间试预订
	
	@Value("${bqy.hotel.create.order.url}")
	private String BQY_HOTEL_CREATE_ORDER_URL;		//创建订单
	
	@Value("${bqy.hotel.order.pay.url}")
	private String BQY_HOTEL_ORDER_PAY_URL;			//订单支付
	
	@Value("${bqy.hotelId.by.cityCode}")
	private String BQY_HOTELID_BY_CITYCODE;			//城市ID获取酒店
	
	@Value("${bqy.hotel.order.cancel.url}")		
	private String BQY_HOTEL_ORDER_CANCEL_URL;		//取消订单
	
	@Value("${bqy.hotel.order.detail.url}")
	private String BQY_HOTEL_ORDER_DETAIL_URL;		//订单详情

	 @Value("${bqy.hotel.order.retreat.url}")
	private String BQY_HOTEL_ORDER_RETREAT_URL;		//订单退订
	
	@Value("${bqy.count}")
	private int PAGE_SIZE;							//查询id数量
	
	@Autowired
	private MongoTemplate mongoTemplate1;
	
	@Autowired
	private IHolMongoQuery holMongoQuery;
	
	@Autowired
	private HttpClientUtil httpClientUtil;
	
	@Reference
	private ISaleOrderService saleOrderService;

	@Reference
	private IBuyOrderService buyOrderService;

	@Autowired
	private HotelOrderMapper hotelOrderMapper;
	
	@Reference
	private ISupplierService supplierService;

	@Reference
	private IMaxNoService maxNoService;
	
	@Reference
	private IPlanAmountRecordService planAmountRecordService;
	
	/*@Autowired
	private IHolMidService holMidService;*/
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	@Override
	public long queryHotelIdCount(Map<String, Object> param) {
		param.put("AgentId", Long.parseLong(BQY_AGENTID));
		param.put("Token", md5Encryption());
		param.put("PageSize", 1);
		param.put("PageNo", 1);
		String paramJson = JsonUtil.toJson(param);
		String result = httpClientUtil.doJsonPost(BQY_HOTELID_LIST_URL,paramJson);
		long totalItemCount = 0; 
		if (StringUtils.isNoneBlank(result.trim())) {
//			result = result.replace("\\", "");
			ResponseResult<String> responseResult = JsonUtil.toBean(result, new TypeReference<ResponseResult<String>>(){});
			if (responseResult != null) {
				if (responseResult.getResponseStatus() != null && responseResult.getResponseStatus().getAck() == 1) {
					String resultStr = responseResult.getResult();
					JSONObject jsonObject = JsonUtil.toBean(resultStr);
					totalItemCount = jsonObject.getLong("TotalItemCount");
				}
			}
		}else {
			throw new GSSException("获取BQY酒店ID数量", "0111", "获取BQY所有酒店ID数量失败...");
		}
		return totalItemCount;
	}

	@Override
	public List<HotelId> queryHotelIdList(Map<String, Object> param) {
		param.put("AgentId", Long.parseLong(BQY_AGENTID));
		param.put("Token", md5Encryption());
		String paramJson = JsonUtil.toJson(param);
		System.out.println("酒店id查询入参为:" + paramJson);
		String result = httpClientUtil.doJsonPost(BQY_HOTELID_LIST_URL,paramJson);
		List<HotelId> hotelIdList = null; 
		if (StringUtils.isNoneBlank(result.trim())) {
//			result = result.replace("\\", "");
			ResponseResult<String> responseResult = JsonUtil.toBean(result, new TypeReference<ResponseResult<String>>(){});
			if (responseResult != null) {
				if (responseResult.getResponseStatus() != null && responseResult.getResponseStatus().getAck() == 1) {
					String resultStr = responseResult.getResult();
					JSONObject jsonObject = JsonUtil.toBean(resultStr);
					String hotelIdJson = jsonObject.getString("Items");
					hotelIdList = JsonUtil.toList(hotelIdJson, HotelId.class);
				}
			}
		}else {
			throw new GSSException("获取BQY酒店ID", "0111", "获取BQY所有酒店ID失败...");
		}
		return hotelIdList;
	}
	
	@Override
	public List<HotelId> queryHotelIdListByCityCode(QueryHotelIdParam query) {
		logger.info("获取所有酒店ID开始...");
		query.setAgentId(Long.parseLong(BQY_AGENTID));
		query.setToken(md5Encryption());
		String paramJson = JsonUtil.toJson(query);
		String result = httpClientUtil.doJsonPost(BQY_HOTELID_BY_CITYCODE, paramJson);
		List<HotelId> hotelIdList = null; 
		if (StringUtils.isNoneBlank(result.trim())) {
			ResponseResult<String> responseResult = JsonUtil.toBean(result, new TypeReference<ResponseResult<String>>(){});
			if (responseResult != null) {
				if (responseResult.getResponseStatus() != null && responseResult.getResponseStatus().getAck() == 1) {
					String resultStr = responseResult.getResult();
					JSONObject jsonObject = JsonUtil.toBean(resultStr);
					String hotelIdJson = jsonObject.getString("Items");
					hotelIdList = JsonUtil.toList(hotelIdJson, HotelId.class);
					System.out.println(hotelIdJson);
				}
			}
		}else {
			throw new GSSException("获取BQY酒店ID", "0111", "获取BQY所有酒店ID失败...");
		}
		logger.info("获取所有酒店ID成功...");
		return hotelIdList;
	}
	
	@Override
	public HotelInfo queryHotelInfo(QueryHotelInfoParam query) {
		query.setAgentId(Long.parseLong(BQY_AGENTID));
		query.setToken(md5Encryption());
		HotelInfo hotelInfo = null;
		String paramJson = JsonUtil.toJson(query);
		String result = httpClientUtil.doJsonPost(BQY_HOTEL_INFO_URL, paramJson);
		if (StringUtils.isNoneBlank(result.trim())) {
			JSONObject jsonObject = JsonUtil.toBean(result);
			String resultJson = jsonObject.getString("Result");
			//ResponseResult<HotelInfo> responseResult = JsonUtil.toBean(resultJson, new TypeReference<ResponseResult<HotelInfo>>(){});
			/*if (responseResult != null) {
				if (responseResult.getResponseStatus() != null && responseResult.getResponseStatus().getAck() == 1) {
					hotelInfo = responseResult.getResult();
				}
			}*/
			hotelInfo = JsonUtil.toBean(resultJson, HotelInfo.class);
		}else {
			throw new GSSException("获取BQY酒店信息失败!", "0111", "BQY酒店信息返回空值");
		}
		return hotelInfo;
	}
	
	@Override
	public List<HotelInfoListEntity> queryHotelList(QueryHotelListParam query) {
		query.setAgentId(Long.parseLong(BQY_AGENTID));
		query.setToken(md5Encryption());
		List<HotelInfoListEntity> hotelList = null;
		logger.info("BQY酒店列表开始查询...");
		// 将请求参数转换为json格式
		String paramJson = JsonUtil.toJson(query);
		// 请求酒店列表
		//String result = HttpClientUtil.doJsonPost(BQY_ALL_HOTEL_LIST, paramJson);
		String result = httpClientUtil.doJsonPost(BQY_ALL_HOTEL_LIST, paramJson);
		if (StringUtils.isNoneBlank(result.trim())) {
			// 将返回数据转换成json对象
			ResponseResult<HotelList> responseResult = JsonUtil.toBean(result,
					new TypeReference<ResponseResult<HotelList>>() {
					});
			if (responseResult != null) {
				if (responseResult.getResponseStatus() != null && responseResult.getResponseStatus().getAck() == 1) {
					logger.info("BQY酒店列表获取成功!");
					hotelList = responseResult.getResult().getHotelList();
				} else {
					logger.info("BQY酒店列表获取失败!");
					throw new GSSException("获取酒店列表", "0112", "酒店列表请求失败!");
				}
			} else {
				logger.info("BQY酒店列表json格式转换实体失败!");
			}

		} else {
			logger.info("BQY酒店列表获取失败!");
			throw new GSSException("获取BQY酒店列表", "0111", "BQY酒店列表请求返回空值");
		}
		return hotelList;
	}

	@Override
	@Async
	public Future<HotelEntity> queryHotelDetail(QueryHotelParam query) {
		query.setAgentId(Long.parseLong(BQY_AGENTID));
		query.setToken(md5Encryption());
		HotelEntity hotelEntity = null;
		String paramJson = JsonUtil.toJson(query);
		logger.info("bqy酒店详情查询入参:" + paramJson);
		String result = httpClientUtil.doJsonPost(BQY_HOTEL_DETAIL_URL, paramJson);
		if (StringUtils.isNoneBlank(result.trim())) {
			// 将返回数据转换成json对象
			ResponseResult<HotelEntity> responseResult = JsonUtil.toBean(result,
					new TypeReference<ResponseResult<HotelEntity>>() {});
			if (responseResult != null) {
				if (responseResult.getResponseStatus() != null && responseResult.getResponseStatus().getAck() == 1) {
					hotelEntity = responseResult.getResult();
				}
			} else {
				logger.info("BQY酒店详情请求返回空值,酒店id为:" + query.getHotelId());
			}
		} else {
			throw new GSSException("获取TC酒店详情", "0111", "BQY酒店详情请求返回空值,酒店id为:" + query.getHotelId());
		}
		return new AsyncResult<HotelEntity>(hotelEntity);
		//return hotelEntity;
	}

	@Override
	public CityInfo queryCityInfo(QueryCityInfoParam query) {
		query.setAgentId(Long.parseLong(BQY_AGENTID));
		query.setToken(md5Encryption());
		CityInfo cityInfo = null;
		String paramJson = JsonUtil.toJson(query);
		logger.info("bqy城市详情查询入参:" + paramJson);
		String result = httpClientUtil.doJsonPost(BQY_CITY_INFO_URL, paramJson);
		if (StringUtils.isNoneBlank(result.trim())) {
			ResponseResult<CityInfo> responseResult = JsonUtil.toBean(result, new TypeReference<ResponseResult<CityInfo>>(){});
			if (responseResult != null) {
				if (responseResult.getResponseStatus() != null && responseResult.getResponseStatus().getAck() == 1) {
					cityInfo = responseResult.getResult();
				}
			}else {
				logger.info("BQY城市信息返回为空!");
			}
		}else {
			throw new GSSException("获取BQY城市信息失败!", "0111", "BQY城市信息返回空值");
		}
		return cityInfo;
	}

	@Override
	public List<HotelRoomFacility> queryHotelRoomFacility(QueryHotelParam query) {
		query.setAgentId(Long.parseLong(BQY_AGENTID));
		query.setToken(md5Encryption());
		List<HotelRoomFacility> roomFacilityList = null;
		String paramJson = JsonUtil.toJson(query);
		logger.info("bqy酒店房间设施查询入参:" + paramJson);
		String result = httpClientUtil.doJsonPost(BQY_HOTEL_FACILITY_URL, paramJson);
		if (StringUtils.isNoneBlank(result.trim())) {
			ResponseResult<List<HotelRoomFacility>> responseResult = JsonUtil.toBean(result, new TypeReference<ResponseResult<List<HotelRoomFacility>>>(){});
			if (responseResult != null) {
				if (responseResult.getResponseStatus() != null && responseResult.getResponseStatus().getAck() == 1) {
					roomFacilityList = responseResult.getResult();
					if (roomFacilityList == null || roomFacilityList.size() == 0) {
						logger.info("bqy酒店房间设施查询返回为空!");
					}
				}
			}else {
				logger.info("bqy酒店房间设施查询返回为空!");
			}
		}else {
			throw new GSSException("获取BQY酒店房间设施信息失败!", "0111", "BQY酒店房间设施信息返回空值");
		}
		return roomFacilityList;
	}

	@Override
	public List<ImageList> queryHotelImage(QueryHotelParam query) {
		query.setAgentId(Long.parseLong(BQY_AGENTID));
		query.setToken(md5Encryption());
		List<ImageList> imageList = null;
		String paramJson = JsonUtil.toJson(query);
		logger.info("bqy酒店图片查询入参:" + paramJson);
		String result = httpClientUtil.doJsonPost(BQY_HOTEL_IMAGE_URL, paramJson);
		logger.info("bqy酒店图片查询结果:" + result);
		if (StringUtils.isNoneBlank(result.trim())) {
			ResponseResult<List<ImageList>> responseResult = JsonUtil.toBean(result, new TypeReference<ResponseResult<List<ImageList>>>(){});
			if (responseResult != null) {
				if (responseResult.getResponseStatus() != null && responseResult.getResponseStatus().getAck() == 1) {
					imageList = responseResult.getResult();
					if (imageList == null || imageList.size() == 0) {
						logger.info("bqy酒店图片查询返回为空!");
					}
				}
			}
		}else {
			throw new GSSException("获取BQY酒店图片失败!", "0111", "BQY酒店图片返回空值");
		}
		return imageList;
	}
	

	@Override
	public List<RoomImageList> queryHotelRoomImage(QueryHotelParam query) {
		query.setAgentId(Long.parseLong(BQY_AGENTID));
		query.setToken(md5Encryption());
		List<RoomImageList> imageList = null;
		String paramJson = JsonUtil.toJson(query);
		String result = httpClientUtil.doJsonPost(BQY_HOTEL_ROOM_IMAGE_URL, paramJson);
		if (StringUtils.isNoneBlank(result.trim())) {
			ResponseResult<List<RoomImageList>> responseResult = JsonUtil.toBean(result, new TypeReference<ResponseResult<List<RoomImageList>>>(){});
			if (responseResult != null) {
				if (responseResult.getResponseStatus() != null && responseResult.getResponseStatus().getAck() == 1) {
					imageList = responseResult.getResult();
				} 
			}
		}else {
			throw new GSSException("获取BQY酒店房型图片失败!", "0111", "BQY酒店房型图片返回空值");
		}
		return imageList;
	}
	
	@Override
	public List<RoomPriceItem> queryHotelRoomPrice(QueryHotelParam query) {
		// BQY_HOTEL_HOTEL_PRICE
		query.setAgentId(Long.parseLong(BQY_AGENTID));
		query.setToken(md5Encryption());
		List<RoomPriceItem> roomPriceList = null;
		String paramJson = JsonUtil.toJson(query);
		logger.info("bqy酒店房型查询入参:" + paramJson);
		String result = httpClientUtil.doJsonPost(BQY_HOTEL_ROOM_PRICE_URL, paramJson);
		if (StringUtils.isNoneBlank(result.trim())) {
			ResponseResult<List<RoomPriceItem>> responseResult = JsonUtil.toBean(result, new TypeReference<ResponseResult<List<RoomPriceItem>>>(){});
			if (responseResult != null) {
				if (responseResult.getResponseStatus() != null && responseResult.getResponseStatus().getAck() == 1) {
					roomPriceList = responseResult.getResult();
					if (roomPriceList == null || roomPriceList.isEmpty()) {
						logger.info("BQY酒店房型查询结果为空,酒店id为:" + query.getHotelId());
					}
				} 
			}else {
				logger.info("BQY酒店房型查询结果为空,酒店id为:" + query.getHotelId());
			}
		}else {
			throw new GSSException("获取BQY酒店房型失败!", "0111", "BQY酒店房型返回空值,酒店id为:" + query.getHotelId());
		}
		return roomPriceList;
	}
	
	
	@Override
	public HotelLocationEntity queryCityInfo2(QueryHotelIdParam query) {
		query.setAgentId(Long.parseLong(BQY_AGENTID));
		query.setToken(md5Encryption());
		HotelLocationEntity cityInfo = null;
		String paramJson = JsonUtil.toJson(query);
		String result = httpClientUtil.doJsonPost(BQY_CITY_INFO_URL_2, paramJson);
		if (StringUtils.isNoneBlank(result.trim())) {
			ResponseResult<HotelLocationEntity> responseResult = JsonUtil.toBean(result, new TypeReference<ResponseResult<HotelLocationEntity>>(){});
			if (responseResult != null) {
				if (responseResult.getResponseStatus() != null && responseResult.getResponseStatus().getAck() == 1) {
					cityInfo = responseResult.getResult();
				}
			}
		}else {
			throw new GSSException("获取BQY城市信息失败!", "0111", "BQY城市信息返回空值");
		}
		return cityInfo;
	}
	
	@Override
	public BookOrderResponse isBookOrder(BookOrderParam query) {
		logger.info("BQY酒店试预定开始...");
		query.setAgentId(Long.parseLong(BQY_AGENTID));
		query.setToken(md5Encryption());
		BookOrderResponse bookOrderResponse = null;
		String paramJson = JsonUtil.toJson(query);
		logger.info("BQY酒店试预订入参:" + paramJson);
		String result = httpClientUtil.doJsonPost(BQY_HOTEL_BOOK_ORDER_URL, paramJson);
		logger.info("BQY酒店试预订返回结果:" + result);
		if (StringUtils.isNoneBlank(result.trim())) {
			ResponseResult<BookOrderResponse> responseResult = JsonUtil.toBean(result, new TypeReference<ResponseResult<BookOrderResponse>>(){});
			if (responseResult != null) {
				if (responseResult.getResponseStatus() != null && responseResult.getResponseStatus().getAck() == 1) {
					bookOrderResponse = responseResult.getResult();
				}
			}
		}else {
			throw new GSSException("酒店试预定失败!", "0111", "酒店试预定返回空值");
		}
		logger.info("BQY酒店试预定结束!");
		return bookOrderResponse;
	}
	
	@Override
	public CreateOrderRespone createOrder(CreateOrderReq createOrderReq) {
		createOrderReq.setAgentId(Long.parseLong(BQY_AGENTID));
		createOrderReq.setBookingUserId(BQY_AGENTID);
		createOrderReq.setToken(md5Encryption());
		CreateOrderRespone createOrderRespone = null;
		String paramJson = JsonUtil.toJson(createOrderReq);
		logger.info("BQY酒店订单创建入参: " + paramJson);
		String result = httpClientUtil.doJsonPost(BQY_HOTEL_CREATE_ORDER_URL, paramJson);
		logger.info("BQY酒店订单创建结果: " + result);
		if (StringUtils.isNoneBlank(result.trim())) {
			ResponseResult<CreateOrderRespone> responseResult = JsonUtil.toBean(result, new TypeReference<ResponseResult<CreateOrderRespone>>(){});
			if (responseResult != null) {
				if (responseResult.getResponseStatus() != null && responseResult.getResponseStatus().getAck() == 1) {
					createOrderRespone = responseResult.getResult();
				}else {
					logger.error("BQY酒店订单创建失败!");
					throw new GSSException("酒店订单创建失败!", "0111", "酒店订单创建返回空值");
				}
			}
		}else {
			throw new GSSException("酒店订单创建失败!", "0111", "酒店订单创建返回空值");
		}
		logger.info("BQY酒店订单创建结束!");
		return createOrderRespone;
	}
	
	@Override
	public OrderPayResult orderPay(OrderPayReq orderPayReq) {
		//BQY_HOTEL_ORDER_PAY_URL
		orderPayReq.setAgentId(Long.parseLong(BQY_AGENTID));
		orderPayReq.setToken(md5Encryption());
		OrderPayResult orderPayResult = null;
		String paramJson = JsonUtil.toJson(orderPayReq);
		logger.info("BQY酒店订单支付入参: " + paramJson);
		String result = httpClientUtil.doJsonPost(BQY_HOTEL_ORDER_PAY_URL, paramJson);
		logger.info("BQY酒店订单支付结果: "+result);
		if (StringUtils.isNoneBlank(result.trim())) {
			ResponseResult<OrderPayResult> responseResult = JsonUtil.toBean(result, new TypeReference<ResponseResult<OrderPayResult>>(){});
			if (responseResult != null) {
				if (responseResult.getResponseStatus() != null && responseResult.getResponseStatus().getAck() == 1) {
					orderPayResult = responseResult.getResult();
				}else {
					logger.error("BQY酒店订单支付失败!");
					throw new GSSException("酒店订单支付失败!", "0111", "酒店订单支付返回空值");
				}
			}
		}else {
			throw new GSSException("酒店订单支付失败!", "0111", "酒店订单支付返回空值");
		}
		return orderPayResult;
	}
	
	
	@Override
	public OrderCancelResult cancelOrder(OrderCancelParam cancelParam) {
		//BQY_HOTEL_ORDER_CANCEL_URL
		cancelParam.setAgentId(Long.parseLong(BQY_AGENTID));
		cancelParam.setToken(md5Encryption());
		cancelParam.setBookingUserId(BQY_AGENTID);
		OrderCancelResult orderCancelResult = null;
		String paramJson = JsonUtil.toJson(cancelParam);
		logger.info("取消酒店订单入参：" + paramJson);
		String result = httpClientUtil.doJsonPost(BQY_HOTEL_ORDER_CANCEL_URL, paramJson);
		logger.info("取消酒店订单返回结果：" + result);
		if (StringUtils.isNoneBlank(result.trim())) {
			ResponseResult<OrderCancelResult> responseResult = JsonUtil.toBean(result, new TypeReference<ResponseResult<OrderCancelResult>>(){});
			if (responseResult != null) {
				if (responseResult.getResponseStatus() != null && responseResult.getResponseStatus().getAck() == 1) {
					orderCancelResult = responseResult.getResult();
				}
			}
		}else {
			throw new GSSException("酒店订单取消失败!", "0111", "酒店订单取消返回空值");
		}
		return orderCancelResult;
	}

	@Override
	public OrderCancelResult retreatOrder(OrderCancelParam cancelParam) {
		//BQY_HOTEL_ORDER_RETREAT_URL
		cancelParam.setAgentId(Long.parseLong(BQY_AGENTID));
		cancelParam.setToken(md5Encryption());
		cancelParam.setBookingUserId(BQY_AGENTID);
		OrderCancelResult orderCancelResult = null;
		String paramJson = JsonUtil.toJson(cancelParam);
		logger.info("取消酒店订单入参：" + paramJson);
		String result = httpClientUtil.doJsonPost(BQY_HOTEL_ORDER_RETREAT_URL, paramJson);
		logger.info("取消酒店订单返回结果：" + result);
		if (StringUtils.isNoneBlank(result.trim())) {
			ResponseResult<OrderCancelResult> responseResult = JsonUtil.toBean(result, new TypeReference<ResponseResult<OrderCancelResult>>(){});
			if (responseResult != null) {
				if (responseResult.getResponseStatus() != null && responseResult.getResponseStatus().getAck() == 1) {
					orderCancelResult = responseResult.getResult();
				}
			}
		}else {
			throw new GSSException("酒店订单取消失败!", "0111", "酒店订单取消返回空值");
		}
		return orderCancelResult;
	}

	@Override
	public HotelOrderInfo orderDetail(OrderPayReq orderDetailParam) {
		// TODO BQY_HOTEL_ORDER_DETAIL_URL
		orderDetailParam.setAgentId(Long.parseLong(BQY_AGENTID));
		orderDetailParam.setToken(md5Encryption());
		HotelOrderInfo hotelOrderInfo = null;
		String paramJson = JsonUtil.toJson(orderDetailParam);
		logger.info("bqy酒店订单详情查询入参:" + paramJson);
		String result = httpClientUtil.doJsonPost(BQY_HOTEL_ORDER_DETAIL_URL, paramJson);
		logger.info("bqy酒店订单详情查询结果:" + result);
		/*if (StringUtils.isNoneBlank(result.trim())) {
			ResponseResult<HotelOrderInfo> responseResult = JsonUtil.toBean(result, new TypeReference<ResponseResult<HotelOrderInfo>>(){});
			if (responseResult != null) {
				if (responseResult.getResponseStatus() != null && responseResult.getResponseStatus().getAck() == 1) {
					hotelOrderInfo = responseResult.getResult();
				}
			}
		}*/
		if (StringUtils.isNoneBlank(result.trim())) {
			ResponseResult<String> responseResult = JsonUtil.toBean(result, new TypeReference<ResponseResult<String>>(){});
			if (responseResult != null) {
				if (responseResult.getResponseStatus() != null && responseResult.getResponseStatus().getAck() == 1) {
					String resultStr = responseResult.getResult();
					JSONObject jsonObject = JsonUtil.toBean(resultStr);
					String hotelOrderInfoStr = jsonObject.getString("HotelOrderInfo");
					if (StringUtil.isNotNullOrEmpty(hotelOrderInfoStr)) {
						hotelOrderInfo = JsonUtil.toBean(hotelOrderInfoStr, HotelOrderInfo.class);
					}else {
						throw new GSSException("获取订单详情","0122", "订单详情返回为空!");
					}
				}
			}
		}else {
			throw new GSSException("获取酒店订单详情失败", "0111", "获取酒店订单详情失败!");
		}
		return hotelOrderInfo;
	}
	
	/**
	 * 根据酒店ID获取酒店信息
	 */
	@Override
	@Async
	public void pullHotelInfoByIdList(List<HotelId> hotelIdList) {
		logger.info(Thread.currentThread().getName() + "线程开始拉取数据...集合大小为：" + hotelIdList.size());
		for (HotelId hotelId : hotelIdList) {
			QueryHotelInfoParam hotelInfoParam = new QueryHotelInfoParam();
			hotelInfoParam.setHotelId(hotelId.getHotelId());
			HotelInfo hotelInfo = queryHotelInfo(hotelInfoParam);
			
			if (null == hotelInfo || hotelInfo.getHotelId() == 0) {
				System.out.println("酒店id:" + hotelId.getHotelId() + "为空!");
				continue;
			}
			
			hotelInfo.setId(hotelId.getHotelId());
			//酒店图片
			QueryHotelParam queryHotelParam = new QueryHotelParam();
			queryHotelParam.setHotelId(hotelInfo.getHotelId());
			//List<ImageList> hotelImageList = queryHotelImage(queryHotelParam);
			//hotelInfo.setHotelImageList(hotelImageList);
			hotelInfo.setSupplierNo("411805040103290132");
			hotelInfo.setLatestUpdateTime(sdf.format(new Date()));
			hotelInfo.setSaleStatus(1);
			//保存酒店信息
			//mongoTemplate1.save(hotelInfo);
			//保存中间表
			saveMidHol(hotelInfo);
		}
		logger.info(Thread.currentThread().getName() + "线程完成拉取数据...");
	}

	@Override
	public void saveMidHol(HotelInfo hotelInfo) {
		/**
		 * 判断中间表中是否有相同的酒店
		 * 通过酒店经纬度和酒店电话匹配是否为同一家酒店
		 */
		//纬度
		String latitude = hotelInfo.getLatitude().toString();
		//经度
		String longitude = hotelInfo.getLongitude().toString();
		//电话
		String mobile = hotelInfo.getMobile();
		
		List<HolMidBaseInfo> holMidList = null;
		if (StringUtils.isNoneBlank(latitude) && StringUtils.isNoneBlank(longitude) && StringUtils.isNoneBlank(mobile) && 
						-1 != Double.valueOf(latitude) && -1 != Double.valueOf(longitude)) {
			//查询中间表是否有纬度相同酒店
			//List<HolMidBaseInfo> holMidList = searchHoltel(latitude, longitude, mobile);
			//List<HolMidBaseInfo> holMidList = null;
			holMidList = holMongoQuery.queryAlikeHol(longitude, latitude, Tool.splitStr(mobile));
		}
		
		if (null != holMidList && holMidList.size() > 0) {
			if (holMidList.size() == 1) {
				//合并酒店
				HolMidBaseInfo holMid = holMidList.get(0);
				logger.info("有一个相同的酒店要插入的酒店id为:"+hotelInfo.getHotelId()+">>>酒店名称为:" + hotelInfo.getHotelName());
				logger.info("中间表酒店名称为:" + holMid.getResName() + ">>>酒店id为:"+ holMid.getResNameSum().get(0).getResId() +  ">>>中间表id为"+holMid.getId());
				List<ResNameSum> listHol = holMid.getResNameSum();
				if(StringUtil.isNotNullOrEmpty(listHol)) {
					boolean isBQYHotel = true;
					//判断是否存在bqy酒店id
					for (ResNameSum resNameSum : listHol) {
						if ((2 - resNameSum.getResType()) == 0) {
							isBQYHotel = false;
							resNameSum.setResId(hotelInfo.getHotelId());
							resNameSum.setResName(hotelInfo.getHotelName());
							resNameSum.setSupplierNo(hotelInfo.getSupplierNo());
							resNameSum.setResAdress(hotelInfo.getAddress());
							resNameSum.setResPhone(hotelInfo.getMobile());
							//1: TC, 2: BQY, 3: TY, 0: All
							resNameSum.setResType(2);
							break;
						}
					}
					if (isBQYHotel) {
						ResNameSum resNameSum=new ResNameSum();
						resNameSum.setResId(hotelInfo.getHotelId());
						resNameSum.setResName(hotelInfo.getHotelName());
						resNameSum.setSupplierNo(hotelInfo.getSupplierNo());
						resNameSum.setResAdress(hotelInfo.getAddress());
						resNameSum.setResPhone(hotelInfo.getMobile());
						//1: TC, 2: BQY, 3: TY, 0: All
						resNameSum.setResType(2);
						listHol.add(resNameSum);
						holMid.setResNums(holMid.getResNums() + 1);
					}
				}
				//查询价格
				ResToMinPrice resToMinPrice = mongoTemplate1.findOne(new Query(Criteria.where("_id").is(holMid.getId())), ResToMinPrice.class);
				resToMinPrice.setBqyId(hotelInfo.getHotelId());
				holMid.setResId(String.valueOf(hotelInfo.getHotelId()));
				holMid.setResName(hotelInfo.getHotelName());
 				if (null != hotelInfo.getLowPrice()) {
 					//比较酒店中间表最低价
 					BigDecimal lowPrice = hotelInfo.getLowPrice();
 					if (lowPrice != null && lowPrice.compareTo(BigDecimal.ZERO) == 1) {
 						BigDecimal holMidLowPrice = new BigDecimal(holMid.getMinPrice());
 						if (lowPrice.compareTo(holMidLowPrice) == -1) {
 							holMid.setMinPrice(lowPrice.intValue());
 						}
 						/**
 						 * 比较酒店数据表最低价
 						 */
 	 					BigDecimal resMinPrice = resToMinPrice.getMinPrice();
 	 					if (null != resToMinPrice && resMinPrice.compareTo(BigDecimal.ZERO) == 1){
 	 						if (lowPrice.compareTo(resMinPrice) == -1) {
 	 	 						resToMinPrice.setMinPrice(lowPrice);
 	 						}
 	 					}else {
 	 						resToMinPrice.setMinPrice(lowPrice);
 	 					}
 					}
 				}else {
 					String noPriceStatus = resToMinPrice.getNoPriceStatus();
 					if (StringUtils.isBlank(noPriceStatus) || !noPriceStatus.contains("2")) {
 						resToMinPrice.setNoPriceStatus("2");
 					}else {
 						resToMinPrice.setNoPriceStatus(resToMinPrice.getNoPriceStatus() + noPriceStatus);
 					}
 				}
 				mongoTemplate1.save(holMid);
 				//更新价格表
 				mongoTemplate1.save(resToMinPrice);
			
			}else if (holMidList.size() > 1) {
				logger.info("有一个相同的酒店要插入的酒店id为:"+hotelInfo.getHotelId()+">>>酒店名称为:" + hotelInfo.getHotelName());
				throw new GSSException("bqy拉取酒店信息", "0111", "酒店纬度:" + latitude +"经度:" + longitude + "电话:" + mobile + "在中间表中有" + holMidList.size() + "个");
			}
		}else {
			/**
			 * 保存中间表
			 */
			//酒店中间表类
			HolMidBaseInfo holMidBaseInfo = new HolMidBaseInfo();
			
			//价格表
			ResToMinPrice resPrice = new ResToMinPrice();
			
			List<ResNameSum> listHol = new ArrayList<>();
			ResNameSum resNameSum=new ResNameSum();
			resNameSum.setResId(hotelInfo.getHotelId());
			resNameSum.setResName(hotelInfo.getHotelName());
			resNameSum.setSupplierNo(hotelInfo.getSupplierNo());
			resNameSum.setResAdress(hotelInfo.getAddress());
			resNameSum.setResPhone(hotelInfo.getMobile());
			//1: TC, 2: BQY, 3: TY, 0: All
			resNameSum.setResType(2);
			listHol.add(resNameSum);
			holMidBaseInfo.setResNameSum(listHol);
			//中间表id
			String workerId = String.valueOf(IdWorker.getId());
			holMidBaseInfo.setId(workerId);
			resPrice.setId(workerId);
			
			holMidBaseInfo.setResId(String.valueOf(hotelInfo.getHotelId()));
			resPrice.setBqyId(hotelInfo.getHotelId());
			
			holMidBaseInfo.setResName(hotelInfo.getHotelName());
			holMidBaseInfo.setProvName(hotelInfo.getProvinceName());
			holMidBaseInfo.setCityName(hotelInfo.getCityName());
			holMidBaseInfo.setIsInter(1);
			//酒店品牌
			ResBrandInfo resBrandInfo = new ResBrandInfo();
			resBrandInfo.setResBrandName(hotelInfo.getHotelBrandName());
			resBrandInfo.setId(Long.parseLong(String.valueOf(hotelInfo.getHotelBrandId())));
			holMidBaseInfo.setBrandInfo(resBrandInfo);
			
			holMidBaseInfo.setAddress(hotelInfo.getAddress());
			holMidBaseInfo.setResPhone(hotelInfo.getMobile());
			holMidBaseInfo.setIntro(hotelInfo.getDescription());
			//酒店等级
			holMidBaseInfo.setResGrade(tcHolGrade(hotelInfo.getHotelStar()));
			
			//酒店坐标
			holMidBaseInfo.setLatLonType(hotelInfo.getCoordinatesType());
			holMidBaseInfo.setLat(hotelInfo.getLatitude().toString());
			holMidBaseInfo.setLon(hotelInfo.getLongitude().toString());
			Double[] resGpsLocation = new Double[2];
			resGpsLocation[0] = Double.valueOf(hotelInfo.getLongitude().toString());
			resGpsLocation[1] = Double.valueOf(hotelInfo.getLatitude().toString());
			holMidBaseInfo.setResPosition(resGpsLocation);
			
			//holMidBaseInfo.setResPosition(new Double[]{hotelInfo.getLongitude().doubleValue(), hotelInfo.getLatitude().doubleValue()});
			
			//holMidBaseInfo.setCountryName("中国");
			//一句话介绍...酒店描述
			//holMidBaseInfo.setShortIntro(hotelInfo.getDescription());
			
			//酒店图片
			holMidBaseInfo.setTitleImg(hotelInfo.getImgURL());
			
			holMidBaseInfo.setBookRemark(hotelInfo.getRoadCross());
			
			int saleStatus = 0;
			if (null != hotelInfo.getLowPrice()) {
				BigDecimal lowPrice = hotelInfo.getLowPrice();
				if (lowPrice.compareTo(BigDecimal.ZERO) == 1) {
					holMidBaseInfo.setMinPrice(lowPrice.intValue());
					resPrice.setMinPrice(lowPrice);
					saleStatus = 1;
				}
			}
			if (0 == saleStatus) {
				resPrice.setNoPriceStatus("2");
			}
			holMidBaseInfo.setSaleStatus(saleStatus);
			holMidBaseInfo.setSupplierNo(hotelInfo.getSupplierNo());
			holMidBaseInfo.setLatestUpdateTime(hotelInfo.getLatestUpdateTime());
			holMidBaseInfo.setBookTimes(1L);
			holMidBaseInfo.setResNums(1);
			mongoTemplate1.save(holMidBaseInfo);
			
			//保存价格数据
			mongoTemplate1.save(resPrice);
		}
		mongoTemplate1.save(hotelInfo);
	}
	

	/**
	 * 移除mongoDB中的缓存信息
	 */
	@Override
	public void deleteMongoDBData() {
		mongoTemplate1.remove(new Query(), HotelInfo.class);
		//mongoTemplate1.remove(new Query(), CityDetail.class);
		//mongoTemplate1.remove(new Query(), HotelId.class);
		//TODO 需要修改中间表的清空
		mongoTemplate1.remove(new Query(), HolMidBaseInfo.class);
		mongoTemplate1.remove(new Query(), ResToMinPrice.class);
		//mongoTemplate1.remove(new Query(Criteria.where("supplierNo").is("411805040103290132")), HolMidBaseInfo.class);
	}
	
	@Override
	public List<HolMidBaseInfo> searchHoltel(String lat, String lon, String phone) {
		List<HolMidBaseInfo> holMidList = null;
		int latSubLen = lat.substring(lat.indexOf(".")).length();
		int lonSubLen = lon.substring(lon.indexOf(".")).length();
		String latitude = null;
		String longitude = null;
		
		if (latSubLen > 5) {
			latSubLen = 5;
		}
		if (lonSubLen > 5) {
			lonSubLen = 5;
		}
		latitude = lat.substring(0, lat.indexOf(".") + latSubLen);
		longitude = lon.substring(0, lon.indexOf(".") + lonSubLen);
		String mobile = null;
		//List<String> phoneList = new ArrayList<>();
		if (StringUtils.isNotBlank(phone) && phone.length() >= 7) {
			Set<String> phoneList = Tool.splitStr(phone);
			if (phoneList.size() == 1) {
				holMidList = searchHol(latitude, longitude, phoneList.iterator().next());
			}else if (phoneList.size() > 1) {
				Criteria criteria = new Criteria();
				List<Criteria> criteriaList = new ArrayList<>();
				for (String p : phoneList) {
					if (StringUtils.isNoneBlank(p) && p.length() >= 7) {
						Criteria orCriteria = createCriteria(latitude, longitude, p);
						criteriaList.add(orCriteria);
					}
				}
				if (criteriaList.size() == 0) {
					holMidList = searchHol(latitude, longitude, null);
				}else if (criteriaList.size() == 1) {
					criteria.orOperator(criteriaList.get(0));
				}else if (criteriaList.size() == 2) {
					criteria.orOperator(criteriaList.get(0), criteriaList.get(1));
				}else {
					criteria.orOperator(criteriaList.get(0), criteriaList.get(1), criteriaList.get(2));
				}
				
				holMidList = mongoTemplate1.find(new Query(criteria), HolMidBaseInfo.class);
			}
		}else {
			holMidList = searchHol(latitude, longitude, mobile);
		}
		return holMidList;
	}
	
	private List<HolMidBaseInfo> searchHol(String latitude, String longitude, String mobile) {
		Criteria criteria = createCriteria(latitude, longitude, mobile);
		List<HolMidBaseInfo> holMidList = mongoTemplate1.find(new Query(criteria), HolMidBaseInfo.class);
		return holMidList;
	}

	private Criteria createCriteria(String latitude, String longitude, String mobile) {
		Criteria criteria = new Criteria();
		criteria.and("lon").regex("^" + longitude + ".*$")
					.and("lat").regex("^" + latitude + ".*$");
		if (StringUtils.isNoneBlank(mobile) && mobile.length() >= 7) {
			criteria.and("resPhone").regex("^.*" + mobile + ".*$");
		}
		return criteria;
	}

	/**
	 * 封装城市信息
	 * @param cityDetail
	 * @param cityInfo
	 * @return
	 */
	private void packingCityLocation(CityDetail cityDetail, HotelLocationEntity cityInfo) {
		//酒店品牌
		Set<HotelBrand> hotelbrandSet = new HashSet<>();
		List<HotelBrand> hotelbrandList = cityInfo.getHotelbrand();
		for (HotelBrand hotelBrand : hotelbrandList) {
			//hotelbrandSet.add(hotelBrand.getHotelBrandName());
			String brandType = hotelBrand.getHotelBrandType();
			if ("经济".equals(brandType.trim()) || "舒适".equals(brandType.trim())) {
				hotelBrand.setHotelBrandType("1");
			}else if ("高档".equals(brandType.trim())) {
				hotelBrand.setHotelBrandType("2");
			}else if ("豪华".equals(brandType.trim())) {
				hotelBrand.setHotelBrandType("3");
			}
			hotelbrandSet.add(hotelBrand);
		}
		cityDetail.setHotelBrands(hotelbrandSet);
		
		//设施服务
		Set<String> hotelFacilitySet = new HashSet<>();
		List<HotelFacility> hotelFacilityList = cityInfo.getHotelFacility();
		
		for (HotelFacility hotelFacility : hotelFacilityList) {
			hotelFacilitySet.add(hotelFacility.getParentName());
		}
		cityDetail.setHotelFacilitys(hotelFacilitySet);
		
		//位置信息
		List<InfoShowlist> infoShowlist = cityInfo.getInfolist();
		//封装城市位置信息
		for (InfoShowlist infoShow : infoShowlist) {
			String keyword = infoShow.getKeyword();
			List<Information> infoList = infoShow.getInfolist();
			Set<String> cityAirRailWaySet = cityDetail.getCityAirRailWay();
			if (null == cityAirRailWaySet) {
				cityAirRailWaySet = new HashSet<>();
			}
			switch (keyword) {
			case "商圈":
				Set<String> businessSectionInfoSet = new HashSet<>();
				for (Information info : infoList) {
					businessSectionInfoSet.add(info.getInfoName());
				}
				cityDetail.setCityBusinessSectionInfo(businessSectionInfoSet);
				break;
			case "行政区":
				Set<String> cityAreaSet = new HashSet<>();
				for (Information info : infoList) {
					cityAreaSet.add(info.getInfoName());
				}
				cityDetail.setCityArea(cityAreaSet);
				break;
			case "地铁":
				Set<String> citySubWaySet = new HashSet<>();
				for (Information info : infoList) {
					citySubWaySet.add(info.getInfoName());
				}
				cityDetail.setCitySubWay(citySubWaySet);
				break;
			case "景点/景区":
				Set<String> cityScenicSet = new HashSet<>();
				for (Information info : infoList) {
					cityScenicSet.add(info.getInfoName());
				}
				cityDetail.setCityScenic(cityScenicSet);
				break;
			case "飞机场":
				for (Information info : infoList) {
					cityAirRailWaySet.add(info.getInfoName());
				}
				cityDetail.setCityAirRailWay(cityAirRailWaySet);
				break;
			case "火车站":
				for (Information info : infoList) {
					cityAirRailWaySet.add(info.getInfoName());
				}
				cityDetail.setCityAirRailWay(cityAirRailWaySet);
				break;
			}
		}
	}
	
	/**
	 * 拉取城市信息
	 */
	@Override
	@Async
	public void pullCityDetail(){
		//城市xml文件
		Document document = DocumentUtil.getDocumentByFileName2("city.xml");
		Element root = document.getRootElement();
		//省份元素迭代器
		Iterator provinces = root.elementIterator();
		
		while (provinces.hasNext()) {
			//省份元素
			Element province = (Element) provinces.next();
			//省份编号
			String provinceCode = province.elementText("id");
			//市级元素迭代器
			Iterator citys = province.element("citys").elementIterator();
			//查询酒店id参数封装类
			QueryHotelIdParam cityInfoParam = new QueryHotelIdParam();
			
			while (citys.hasNext()) {
				Element city = (Element) citys.next();
				//城市编号
				String cityCode = city.elementText("id");
				cityInfoParam.setCityId(cityCode);
				
				CityDetail cityDetail = new CityDetail();
				//获取城市信息
				cityDetail.setProvince(provinceCode);	
				cityDetail.setProvinceName(province.elementText("ZHName"));
				cityDetail.setCity(city.elementText("id"));
				cityDetail.setCityName(city.elementText("ZHName"));
				
				HotelLocationEntity hotelLocationEntity = queryCityInfo2(cityInfoParam);
				packingCityLocation(cityDetail, hotelLocationEntity);
				mongoTemplate1.save(cityDetail);
			}
		}
	}

	@Override
	public void pullHotelIdByCityCode() {
		//城市xml文件
		Document document = DocumentUtil.getDocumentByFileName2("city.xml");
		Element root = document.getRootElement();
		//省份元素迭代器
		Iterator provinces = root.elementIterator();
		
		QueryHotelIdParam cityInfoParam = new QueryHotelIdParam();
		while (provinces.hasNext()) {
			//省份元素
			Element province = (Element) provinces.next();
			//市级元素迭代器
			Iterator citys = province.element("citys").elementIterator();
			//查询酒店id参数封装类
			
			while (citys.hasNext()) {
				Element city = (Element) citys.next();
				//城市编号
				String cityCode = city.elementText("id");
				cityInfoParam.setCityId(cityCode);
				listHotelIdByCityCode(cityInfoParam);
			}
		}
	}
	
	private void listHotelIdByCityCode(QueryHotelIdParam cityInfoParam) {
		int i = 0;
		while (true) {
			i++;
			cityInfoParam.setPageSize(i);
			cityInfoParam.setPageSize(PAGE_SIZE);
			List<HotelId> hotelIdList = queryHotelIdListByCityCode(cityInfoParam);
			if (CollectionUtils.isNotEmpty(hotelIdList)) {
				mongoTemplate1.save(hotelIdList);
				if (hotelIdList.size() != PAGE_SIZE) {
					break;
				}
			}
		}
	}
	
	/**
	 * bqy酒店等级转tc
	 * @param hotelStar
	 * @return
	 */
	private String tcHolGrade(String hotelStar){
		if (StringUtils.isNoneBlank(hotelStar)) {
			switch (hotelStar) {
			case "5.00":
				return "23";
			case "4.00":
				//四星级
				return "24";
			case "3.00":
				//三星级
				return "26";
			default:
				return "27";
			}
		}
		return "27";
	}
	
	@Override
	@Async
	public Future<List<ImageList>> asyncHotelImage(QueryHotelParam query) {
		List<ImageList> imageList = null;
		HotelImage hotelImage = mongoTemplate1.findOne(new Query(Criteria.where("_id").is(query.getHotelId())), HotelImage.class);
		imageList = hotelImage.getImageList();
		if (null == imageList || imageList.size() == 0) {
			imageList = queryHotelImage(query);
		}
		return new AsyncResult<List<ImageList>>(imageList);
	}

	@Override
	@Async
	public Future<HotelEntity> asyncHotelDetail(QueryHotelParam query) throws Exception {
		Future<HotelEntity> hotelEntity = queryHotelDetail(query);
		return new AsyncResult<HotelEntity>(hotelEntity.get());
	}


	@Override
	@Async
	@HolAnnotation
	public Future<List<RoomPriceItem>> asyncRoomPrice(QueryHotelParam query) throws Exception {
		List<RoomPriceItem> roomPriceItemsList = queryHotelRoomPrice(query);
		return new AsyncResult<List<RoomPriceItem>>(roomPriceItemsList);
	}

	/**
	 * 加密代理人id
	 * @return
	 */
	private String md5Encryption() {
		if (StringUtils.isBlank(TOKEN)) {
			TOKEN = md5Encryption((md5Encryption(BQY_AGENTID))+BQY_KEY);
		}
		return TOKEN;
	}

	/**
	 * md5加密
	 * @param encryption
	 * @return
	 */
	private static String md5Encryption(String encryption) {     
        MessageDigest messageDigest = null;    
        try    
        {    
            messageDigest = MessageDigest.getInstance("MD5");    
            messageDigest.reset();    
            messageDigest.update(encryption.getBytes("UTF-8"));    
        } catch (NoSuchAlgorithmException e)    
        {    
            System.out.println("NoSuchAlgorithmException caught!");    
            System.exit(-1);    
        } catch (UnsupportedEncodingException e)    
        {    
            e.printStackTrace();    
        }    
    
        byte[] byteArray = messageDigest.digest();    
    
        StringBuffer md5StrBuff = new StringBuffer();    
    
        for (int i = 0; i < byteArray.length; i++)    
        {    
            if (Integer.toHexString(0xFF & byteArray[i]).length() == 1)    
                md5StrBuff.append("0").append(Integer.toHexString(0xFF & byteArray[i]));    
            else    
                md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));    
        }    
        return md5StrBuff.toString();    
    } 
	/*public String md5Encryption(String encryption) {
		String re_md5 = new String();
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(encryption.getBytes());
			byte b[] = md.digest();

			int i;

			StringBuffer buf = new StringBuffer("");
			for (int offset = 0; offset < b.length; offset++) {
				i = b[offset];
				if (i < 0)
					i += 256;
				if (i < 16)
					buf.append("0");
				buf.append(Integer.toHexString(i));
			}

			re_md5 = buf.toString();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return re_md5;
	}*/

}

 