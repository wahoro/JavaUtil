package com.hit.fm.common.util;

import com.alibaba.fastjson.JSONObject;
import com.hit.fm.common.model.HttpResp;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * 文件名：HttpClientUtil.java
 * 说明：
 * 作者：陈江海
 * 创建时间：2017/12/21
 * 版权所有：泉州哈工大工程技术研究院
 */
public class HttpClientUtil {
    private static CloseableHttpClient client;
    private static Object lock_obj = new Object();

    public static HttpResp get(String url) throws Exception {
        HttpGet get = new HttpGet(url);
        get.setHeader("Content-Type", "application/json");
        return invokeHttpRequestBase(get);
    }

    /**
     * post 发送json
     *
     * @param url
     * @param jsonObject
     * @return
     */
    public static HttpResp postJson(String url, JSONObject jsonObject) throws Exception {
        HttpPost method = new HttpPost(url);
        ByteArrayEntity entity = new ByteArrayEntity(jsonObject.toJSONString().getBytes(Charset.forName("UTF-8")));
        method.setEntity(entity);
        method.setHeader("Content-Type", "application/json");
        return invokeHttpRequestBase(method);
    }

    /**
     * 调用http
     *
     * @param req
     * @return
     */
    private static HttpResp invokeHttpRequestBase(HttpRequestBase req) throws Exception {
        CloseableHttpClient client = getClient();
        CloseableHttpResponse response = null;
        try {
            RequestConfig localConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();
            req.setConfig(localConfig);
            response = client.execute(req);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK && response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED
                    && response.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT) {
                return new HttpResp(false, null, response.getStatusLine().toString());
            }
            try {
                String result = EntityUtils.toString(response.getEntity(), Charset.forName("UTF-8"));
                System.out.println("======http返回：" + result);
                return new HttpResp(true, result, null);
            } catch (Exception e) {
                return new HttpResp(true, "", null);//204无内容
            }

        } catch (Exception e) {
            e.printStackTrace();
            try {
                req.abort();
            } catch (Exception e1) {
            }
            return new HttpResp(false, null, "http.error:" + e.getMessage());

        } finally {
            try {
                req.releaseConnection();
            } catch (Exception e) {
            }
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static CloseableHttpClient getClient() {
        if (null == client) {
            synchronized (lock_obj) {
                if (null == client) {
                    HttpClientBuilder builder = HttpClientBuilder.create();
                    builder.setMaxConnPerRoute(1000);
                    builder.setMaxConnTotal(1000);
                    client = builder.build();
                }
            }
        }
        return client;
    }
}
