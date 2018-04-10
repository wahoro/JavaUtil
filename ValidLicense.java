package com.hit.fm.common.util;

import com.hit.fm.license.client.LicenseVertifyUtil;
import org.springframework.stereotype.Component;

/**
 * 文件名：ValidLicense.java
 * 说明：
 * 作者：陈江海
 * 创建时间：2018/1/31
 * 版权所有：泉州哈工大工程技术研究院
 */
@Component
public class ValidLicense {

    public void init() throws Exception {
        LicenseVertifyUtil.verify();
    }
}
