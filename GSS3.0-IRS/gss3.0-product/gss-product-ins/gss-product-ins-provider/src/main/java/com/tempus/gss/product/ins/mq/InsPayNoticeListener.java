/**
 * InsPayNoticeListener.java
 * com.tempus.gss.product.ins.mq
 *
 * Function： TODO 
 *
 *   ver     date      		author
 * ──────────────────────────────────
 *   		 2016年12月20日 		shuo.cheng
 *
 * Copyright (c) 2016, TNT All Rights Reserved.
*/

package com.tempus.gss.product.ins.mq;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.tempus.gss.product.ins.api.entity.vo.OrderCancelVo;
import com.tempus.gss.security.AgentUtil;
import com.tempus.gss.system.service.IParamService;
import org.jfree.util.Log;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.config.annotation.Reference;
import com.tempus.gss.exception.GSSException;
import com.tempus.gss.order.entity.vo.PayNoticeVO;
import com.tempus.gss.order.service.ISaleOrderService;
import com.tempus.gss.product.common.entity.RequestWithActor;
import com.tempus.gss.product.ins.api.entity.Insurance;
import com.tempus.gss.product.ins.api.entity.SaleChangeExt;
import com.tempus.gss.product.ins.api.entity.SaleOrderDetail;
import com.tempus.gss.product.ins.api.entity.SaleOrderExt;
import com.tempus.gss.product.ins.api.service.IOrderService;
import com.tempus.gss.product.ins.dao.OrderServiceDao;
import com.tempus.gss.product.ins.dao.SaleChangeExtDao;
import com.tempus.gss.product.ins.dao.SaleOrderDetailDao;
import com.tempus.gss.vo.Agent;

/**
 * ClassName:InsPayNoticeListener
 * Function: 保险支付通知消息队列监听类
 *
 * @author   shuo.cheng
 * @version  
 * @since    Ver 1.1
 * @Date	 2016年12月20日		上午10:53:15
 *
 * @see 	 
 *  
 */
@Component
@RabbitListener(bindings = @QueueBinding(value = @Queue(value = "ins.payNoticeQue", durable = "true"), exchange = @Exchange(value = "gss-pay-exchange", type = ExchangeTypes.FANOUT, ignoreDeclarationExceptions = "true", durable = "true"), key = "pay"))
//@RabbitListener(queues = "SettleRequest2.2")
public class InsPayNoticeListener {
	protected static final Logger logger = LoggerFactory.getLogger(InsPayNoticeListener.class);

	@Reference
	IOrderService orderService;
	@Reference
	ISaleOrderService saleOrderService;
	@Autowired
	SaleOrderDetailDao saleOrderDetailDao;
	@Autowired
	SaleChangeExtDao saleChangeExtDao;
	@Autowired
	OrderServiceDao orderServiceDao;
	@Reference
	private IParamService paramService;
	@RabbitHandler
	public void onMessage(PayNoticeVO payNoticeVO) {
		Integer owner = payNoticeVO.getOwner();
		// 商品类型 1 国内机票 2 国际机票 3 保险 4 酒店 5 机场服务 6 配送
		Integer goodsType = payNoticeVO.getGoodsType();
		// 业务类型 2.销售单 3.采购单 4.销售变更单 5.采购变更单
		Integer businessType = payNoticeVO.getBusinessType();
		// 变更类型(销售单,采购单时为0)1.废 2.退 3.改
		Integer changeType = payNoticeVO.getChangeType();
		//支付类型 1 为收款   2 为退款
		Integer incomeExpenseType = payNoticeVO.getIncomeExpenseType();
		//收付款单状态支付结果 1.支付成功,2.支付失败
		Integer payStatus = payNoticeVO.getPayStatus();
		try {
			logger.info("监听到支付消息队列" + payNoticeVO.toString());
			Agent agent = new Agent(payNoticeVO.getOwner(), "sys");
			// 商品类型 1 国内机票 2 国际机票 3 保险 4 酒店 5 机场服务 6 配送
			if (payNoticeVO.getGoodsType() == 3) {

				RequestWithActor<Long> requestWithActor = new RequestWithActor<>();
				requestWithActor.setAgent(agent);
				long businessNo = payNoticeVO.getBusinessNo();
				Long saleOrderNo = Long.valueOf(businessNo);
				requestWithActor.setEntity(saleOrderNo);
				SaleOrderExt saleOrderExt = orderService.querySaleOrder(requestWithActor);
				logger.info("订单子状态为："+saleOrderExt.getSaleOrder().getOrderChildStatus());
				if (payNoticeVO.getBusinessType() == 2 && payNoticeVO.getChangeType() == 0
						&& payNoticeVO.getIncomeExpenseType() == 1) {
					if (saleOrderExt == null) {
						logger.error("根据销售单号查询订单为空!");
						throw new GSSException("根据销售单号查询订单为空!", "1010", "投保失败");
					}
					Insurance insurance = saleOrderExt.getInsurance();
					if (insurance == null) {
						logger.error("订单对应的保险产品insurance为空!");
						throw new GSSException("订单对应的保险产品insurance为空!", "1010", "投保失败");
					}
					Integer buyWay = insurance.getBuyWay();
					if (buyWay == null) {
						logger.error("保险产品的购买方式buyWay为空!");
						throw new GSSException("保险产品的购买方式buyWay为空!", "1010", "投保失败");
					}
                     
					/* 判断是线上的单还是线下的单 1：线上 2：线下 */
						if (buyWay == 1) {
							logger.info("--------------------------------------------调用buyInsure------------------------------");
							if(saleOrderExt.getSaleOrder().getOrderChildStatus()==3){
								logger.info("该订单不能投保，订单为取消状态:"+saleOrderExt.getSaleOrderNo());
							}else if(saleOrderExt.getIsBind() == 2){
								logger.info("该订单不能投保，该订单绑定机票,需要等待机票出票后才可投保，订单销售单:"+saleOrderExt.getSaleOrderNo());
							}else{
								boolean flag = orderService.buyInsure(requestWithActor);
								logger.info("投保是否成功,销售单号:" + payNoticeVO.getBusinessNo() + "->" + flag);
							}
						}
				}
					//退款状态处理
					if (2 == businessType && 2 == incomeExpenseType) { // 2.销售单退款
						logger.info("支付退单状态------>"+businessType);
						//如果为收款 直接跳过
						if(incomeExpenseType != 2){
							return;
						}
						//1:退款成功2：退款失败
						if(payStatus==1){
							boolean isCancel = true;
							for(SaleOrderDetail saleOrderDetailChange:saleOrderExt.getSaleOrderDetailList()){
								if(saleOrderDetailChange.getStatus()==8){
										try{
											List<SaleChangeExt> SaleChangeExtList = saleChangeExtDao.selectByInsuredNo(saleOrderDetailChange.getInsuredNo());
											if(SaleChangeExtList!=null){
												SaleChangeExt saleChangeExt = SaleChangeExtList.get(0);
												saleChangeExt.setRefundDate(new Date());
												saleChangeExt.setChangeType(2);//更改为已经退款
												saleChangeExt.setActualRefund(saleChangeExt.getRightRefund());
												saleChangeExtDao.updateByPrimaryKey(saleChangeExt);
											}else{
												Log.error("根据保单号查询保险变更拓展单失败！！");
											}
										}catch(Exception e){
											Log.error("保险变更拓展单更新失败！！"+e);
										}
									/*SaleOrderDetail saleOrderDetailForBefore = null;
									saleOrderDetailForBefore = (SaleOrderDetail)saleOrderDetailChange.clone();
									saleOrderDetailForBefore.setStatus(saleOrderDetailChange.getStatus());*/
									//saleOrderDetailChange.getCreateTime(new Date());
											saleOrderDetailChange.setStatus(10);//10为已退款
									//保存之前存储的那条投保记录
								/*	saleOrderDetailChange.setCreateTime(new Date());
									saleOrderDetailForBefore.setStatus(2);
									saleOrderDetailForBefore.setIsReport("1");
									saleOrderDetailForBefore.setModifyTime(new Date());*/
									saleOrderDetailDao.updateByPrimaryKeySelective(saleOrderDetailChange);
									//saleOrderDetailDao.insertSelective(saleOrderDetailForBefore);
				         			isCancel = true;
				         		}
							}
							for(SaleOrderDetail saleStatus:saleOrderExt.getSaleOrderDetailList()){
								if(saleStatus.getStatus()!=10){
									isCancel = false;
									break;
								}
							}
							if(isCancel){
								saleOrderService.updateStatus(agent, saleOrderExt.getSaleOrderNo(), 10);//子订单更改为10 已经退款
								
				         	}else{
				         		saleOrderService.updateStatus(agent, saleOrderExt.getSaleOrderNo(), 9);//部分退款
				         	}
							//存储退款时间
/*				         	SaleOrderExt saleOrderExtForTime = new SaleOrderExt();
				         	saleOrderExtForTime.setModifyTime(new Date());
				         	saleOrderExtForTime.setSaleOrderNo(saleOrderExt.getSaleOrderNo());
				         	orderServiceDao.updateByPrimaryKeySelective(saleOrderExtForTime);*/
				         	logger.info("退款成功------>");
						}
						if(payStatus==2){
							boolean isCancel = false;
							 boolean isCancel2 = false;
							for(SaleOrderDetail saleOrderDetailChange:saleOrderExt.getSaleOrderDetailList()){
								if(saleOrderDetailChange.getStatus()==8){
									saleOrderDetailChange.setStatus(11);//11为退款失败
									saleOrderDetailDao.updateByPrimaryKeySelective(saleOrderDetailChange);
				         			isCancel = true;
				         		}else{
				         			isCancel2 = true;
				         		}
							}
							if(isCancel==true&&isCancel2==true){
								saleOrderService.updateStatus(agent, saleOrderExt.getSaleOrderNo(), 11);//退款 退款失败
				         	}
				         	if(isCancel==true&&isCancel2==false){
				         		saleOrderService.updateStatus(agent, saleOrderExt.getSaleOrderNo(), 11);//子订单更改为11 退款失败
				         	}
				         	logger.info("退款发生错误------>");
						}
//						saleService.pay(agent, businessNo);
					}
			}/*else if(payNoticeVO.getGoodsType() == 1){
				logger.info("-------机票发生退款保险也退款-----收到消息--交易单:"+payNoticeVO.getTraNo());
				//如果为机票付款信息则不进行处理
				if("1".equals(incomeExpenseType)){
					return;
				}
				RequestWithActor<Long> requestWithActor = new RequestWithActor<Long>();
				requestWithActor.setEntity(payNoticeVO.getTraNo());
				requestWithActor.setAgent(agent);
				List<SaleOrderExt> saleOrderExtList =  orderService.querySaleOrderForTranSaction(requestWithActor);
				//获取后台配置
				String isRefund = paramService.getValueByKey("refund_or_cancel");
				//如果后台没有设置refund_or_cancel  默认退保退款
				if(isRefund == null){
					isRefund = "1";
				}
				for(SaleOrderExt saleOrderExt:saleOrderExtList){
					for (SaleOrderDetail saleOrderDetail : saleOrderExt.getSaleOrderDetailList()) {
						String policyNo = saleOrderDetail.getPolicyNo();
						RequestWithActor<OrderCancelVo> requestWithActor2 = new RequestWithActor<>();
						OrderCancelVo orderCancelVo = new OrderCancelVo();
						List<String> policyNoList = new ArrayList<String>();
						policyNoList.add(policyNo);
						orderCancelVo.setPolicyNoList(policyNoList);
						requestWithActor2.setEntity(orderCancelVo);
						requestWithActor2.setAgent(agent);
						Boolean cancelResult = false;

						try {
							if(saleOrderExt.getOrderType()==1){
								orderService.cancelSaleOrder(requestWithActor2,Integer.parseInt(isRefund));
							} else{
								orderService.cancelSaleOrderOffline(requestWithActor2,Integer.parseInt(isRefund));
							}

							logger.info("cancelInsure end!");
						} catch (Exception e) {
							logger.error(e.getMessage());
						}
					}
				}
			}*/

		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("消费队列异常 ：", ex);
		}

	}
}