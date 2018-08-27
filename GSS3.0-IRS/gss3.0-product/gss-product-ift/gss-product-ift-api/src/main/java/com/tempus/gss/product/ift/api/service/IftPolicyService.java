package com.tempus.gss.product.ift.api.service;

import com.tempus.gss.product.ift.api.entity.policy.IftPolicy;
import com.tempus.gss.vo.Agent;

/**
 * 
 * <pre>
 * <b>国际政策服务接口.</b>
 * <b>Description:</b> 
 *    
 * <b>Author:</b> cz
 * <b>Date:</b> 2018年8月27日 下午1:54:17
 * <b>Copyright:</b> Copyright ©2018 tempus.cn. All rights reserved.
 * <b>Changelog:</b>
 *   Ver   Date                    Author                Detail
 *   ----------------------------------------------------------------------
 *   0.1   2018年8月27日 下午1:54:17    cz
 *         new file.
 * </pre>
 */
public interface IftPolicyService {
	
	/**
	 * 创建国际政策
	 * 
	 * @param agent 用户信息
	 * @param iftPolicy 政策信息
	 * @return boolean 是否成功
	 */
	boolean create(Agent agent, IftPolicy iftPolicy);
}
