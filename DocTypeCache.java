package com.hit.fm.common.util;

import com.hit.fm.model.sys.DocType;
import com.hit.fm.service.sys.DocTypeService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件名：DocTypeCache.java
 * 说明：档案类型缓存
 * 作者：陈江海
 * 创建时间：2018/3/17
 * 版权所有：泉州哈工大工程技术研究院
 */
@Component
public class DocTypeCache {
    private static Logger logger = Logger.getLogger(DocCatalogCache.class);
    private static DocTypeService docTypeService;
    private static Map<Long, DocType> DOC_TYPE_MAP = new HashMap<Long, DocType>();//key为主键
    private static List<DocType> DOC_TYPE_LSIT = new ArrayList<DocType>();

    public static void cache() {
        List<DocType> list = docTypeService.findList(new DocType());
        DOC_TYPE_LSIT = list;
        if (CommonUtil.isListNotEmpty(list)) {
            for (DocType docType : list) {
                DOC_TYPE_MAP.put(docType.getId(), docType);
            }
        }
    }

    public static String getName(Long docTypeId) {
        DocType docType = DOC_TYPE_MAP.get(docTypeId);
        if (null != docType) {
            return docType.getName();
        }
        return null;
    }

    public static Map<Long, DocType> getMap() {
        return DOC_TYPE_MAP;
    }

    public static List<DocType> getList() {
        return DOC_TYPE_LSIT;
    }

    public static DocType get(Long docTypeId) {
        return DOC_TYPE_MAP.get(docTypeId);
    }

    public static DocTypeService getDocTypeService() {
        return docTypeService;
    }

    @Autowired
    public void setDocTypeService(DocTypeService docTypeService) {
        DocTypeCache.docTypeService = docTypeService;
    }
}
