package com.hit.fm.common.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSONObject;

public class ResultUtil {
	/** 日志 */
	private static Log logger = LogFactory.getLog(ResultUtil.class);
	// 保存json对象
	private Map<String, Object> results;
	// 消息"key
	public static final String MSG = "msg";
	// 成功标识 key
	public static final String SUCCESS = "success";
	// 单对"key
	public static final String OBJ = "obj";
	// 列表对象 key
	public static final String ORWS = "rows";
	// 总计"key
	public static final String TOTAL = "total";
	// 状" key
	public static final String STATUS = "status";

	public ResultUtil() {
		this.results = new HashMap<String, Object>();
		this.results.put(SUCCESS, true);
	}

	public Map<String, Object> getResult() {
		return this.results;
	}

	public void setResult(Map<String, Object> set) {
		this.results = set;
	}

	public boolean getSuccess() {
		return (Boolean) results.get(SUCCESS);
	}

	public String getMessage() {
		return (String) results.get(MSG);
	}

	public void setRows(Object list) {
		this.results.put(ORWS, list);
	}

	public void setTotal(Integer total) {
		this.results.put(TOTAL, total);
	}

	/**
	 * 根据"  自定义添加属"
	 * 
	 * @param key
	 *            属"标识
	 * @param value
	 */
	public void setProperty(String key, Object value) {
		try {
			this.results.put(key, value);
		} catch (RuntimeException e) {
			logger.error("-->>设置key为：" + key + "值为" + value + " Json时出错：", e);
		}
	}

	/**
	 * 设置状"信息
	 * 
	 * @param status
	 */
	public void setStatus(String status) {
		setProperty(STATUS, status);
	}

	/**
	 * 设置成功标志 " 用于程序执行是否正常
	 * 
	 * @param success
	 *            成功标识
	 */
	public void setSuccess(boolean success) {
		setProperty(SUCCESS, success);
	}

	/**
	 * 设置消息
	 * 
	 * @param msg
	 *            消息"
	 */
	public void setMsg(String msg) {
		setProperty(MSG, msg);
	}

	/**
	 * 设置总计"
	 * 
	 * @param size
	 */
	public void setTotal(int size) {
		setProperty(TOTAL, size);
	}

	public void setSize(int size) {
		setProperty(TOTAL, size);
	}

	/**
	 * 添加对象列表 数据为json格式
	 * 
	 * @param data
	 *            对象列表json格式
	 */
	public void setData(String data) {
		setProperty(ORWS, data);
	}

	/**
	 * 添加对象
	 * 
	 * @param obj
	 *            对象
	 */
	public void setObject(Object obj) {
		setProperty(OBJ, obj);
	}

	/**
	 * 返回json格式
	 */
	public String toJSONString() {
		JSONObject obj = new JSONObject();
		obj.put("data", this.results);
		return obj.toString();
	}


}
