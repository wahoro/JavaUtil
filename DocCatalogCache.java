package com.hit.fm.common.util;

import com.hit.fm.model.sys.DocCatalog;
import com.hit.fm.service.sys.DocCatalogService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文  件 名：DocCatalogCache.java
 * 说	明：目录数据缓存数据
 * 备注：每天2点，全量刷新一次缓存
 * 作	者：陈江海
 * 创建时间：2015-12-14
 * 版权所有：泉州哈工大工程技术研究院
 */
@Component
public class DocCatalogCache {

    private static Logger logger = Logger.getLogger(DocCatalogCache.class);
    private static DocCatalogService docCatalogService;

    private static Map<Long, DocCatalog> DOC_CATALOG_MAP = new HashMap<Long, DocCatalog>();//key为主键


    /**
     * 缓存目录数
     */
    public static void cache() {
        List<DocCatalog> list = docCatalogService.findList(new DocCatalog());
        if (CommonUtil.isListNotEmpty(list)) {
            for (DocCatalog docCatalog : list) {
                DOC_CATALOG_MAP.put(docCatalog.getId(), docCatalog);
            }
        }
    }

    public static void remove(Long catalogId) {
        DOC_CATALOG_MAP.remove(catalogId);
    }

    public static void add(DocCatalog docCatalog) {
        DOC_CATALOG_MAP.put(docCatalog.getId(), docCatalog);
    }

    /**
     * 根据ID获取数据
     *
     * @param catalogId
     * @return
     */
    public static DocCatalog get(Long catalogId) {
        if (DOC_CATALOG_MAP.containsKey(catalogId)) {
            return DOC_CATALOG_MAP.get(catalogId);
        }
        return null;
    }

    /**
     * 根据名称、层级、类型，获取目录
     *
     * @param name
     * @param level
     * @param type
     * @return
     */
    public static DocCatalog get(String name, int level, short type) {
        DocCatalog ret = null;
        for (DocCatalog catalog : DOC_CATALOG_MAP.values()) {
            if (catalog.getType().shortValue() != type || catalog.getLevel().intValue() != level
                    || !catalog.getName().equals(name)) {
                continue;
            }
            ret = catalog;
            break;
        }
        return ret;
    }

    /**
     * 根据目录获取最顶级的目录
     *
     * @param catalogId
     * @return
     */
    public static long getTopCatalogId(long catalogId) {
        DocCatalog catalog = DOC_CATALOG_MAP.get(catalogId);
        if (null == catalog) {
            return -1l;
        }
        while (catalog.getLevel().intValue() != 1) {
            catalogId = catalog.getParentId();
            catalog = DOC_CATALOG_MAP.get(catalogId);
        }
        return catalogId;
    }

    /**
     * 获取父目录ID
     *
     * @param catalogId
     * @return
     */
    public static long getParentCatalogId(long catalogId) {
        DocCatalog catalog = DOC_CATALOG_MAP.get(catalogId);
        if (null == catalog) {
            return -1l;
        }
        return catalog.getParentId();
    }

    /**
     * 获取父目录
     *
     * @param catalogId
     * @return
     */
    public static DocCatalog getParentCatalog(long catalogId) {
        DocCatalog catalog = DOC_CATALOG_MAP.get(catalogId);
        if (null == catalog) {
            return null;
        }
        return DOC_CATALOG_MAP.get(catalog.getParentId());
    }


    /**
     * 根据类型查找列表
     *
     * @param type
     * @return
     */
    public static List<DocCatalog> findListByType(short type) {
        List<DocCatalog> list = new ArrayList<DocCatalog>();
        for (DocCatalog catalog : DOC_CATALOG_MAP.values()) {
            if (catalog.getType().shortValue() == type) {
                list.add(catalog);
            }
        }
        return list;
    }

    /**
     * 是否存在下级节点
     *
     * @param catalogId
     * @return
     */
    public static boolean existsSubCatalog(long catalogId) {
        for (DocCatalog catalog : DOC_CATALOG_MAP.values()) {
            if (catalog.getParentId().longValue() == catalogId) {
                return true;
            }
        }
        return false;
    }

    /**
     * 统计下级节点数据
     *
     * @param catalogId
     * @return
     */
    public static int countSubCatalog(long catalogId) {
        int count = 0;
        for (DocCatalog catalog : DOC_CATALOG_MAP.values()) {
            if (catalog.getParentId().longValue() == catalogId) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取名称
     *
     * @param catalogId
     * @return
     */
    public static String getName(long catalogId) {
        DocCatalog docCatalog = get(catalogId);
        if (null != docCatalog) {
            return docCatalog.getName();
        }
        return "";
    }

    /**
     * 通过全宗单位目录的的父目录ID，以及该全宗单位的业务ID，获得该全宗单位目录
     *
     * @param pid
     * @param bizId
     * @return
     */
    public static DocCatalog getTopOrgCatalog(long pid, long bizId) {
        List<DocCatalog> subList = getSubList(pid);
        for (DocCatalog docCatalog : subList) {
            if (docCatalog.getLevel().intValue() == 2 && docCatalog.getBizId().longValue() == bizId) {
                return docCatalog;
            }
        }
        return null;
    }


    /**
     * 获取下级目录列表
     *
     * @param catalogId
     * @return
     */
    public static List<DocCatalog> getSubList(long catalogId) {
        List<DocCatalog> subList = new ArrayList<DocCatalog>();
        for (DocCatalog catalog : DOC_CATALOG_MAP.values()) {
            if (catalog.getParentId().longValue() == catalogId) {
                subList.add(catalog);
            }
        }
        return subList;
    }

    /**
     * 获取下级目录列表
     *
     * @param catalogId
     * @return
     */
    public static List<DocCatalog> getSubList(long catalogId, short type) {
        List<DocCatalog> subList = new ArrayList<DocCatalog>();
        for (DocCatalog catalog : DOC_CATALOG_MAP.values()) {
            if (catalog.getParentId().longValue() == catalogId && catalog.getType().shortValue() == type) {
                subList.add(catalog);
            }
        }
        return subList;
    }

    /**
     * 获取所有下级目录
     *
     * @param catalogId
     * @param type
     * @return
     */
    public static List<DocCatalog> getAllSubList(long catalogId, short type) {
        List<DocCatalog> allList = new ArrayList<DocCatalog>();
        List<DocCatalog> subList = getSubList(catalogId, type);
        allList.addAll(subList);
        if (subList.size() > 0) {
            for (DocCatalog docCatalog : subList) {
                allList.addAll(getAllSubList(docCatalog.getId(), type));
            }
        }
        return allList;
    }

    /**
     * 根据level以及bizId查询所有符合条件的记录
     *
     * @param bizId
     * @param level
     * @return
     */
    public static List<DocCatalog> getListByBizId(long bizId, int level) {
        List<DocCatalog> allList = new ArrayList<DocCatalog>();

        for (DocCatalog catalog : DOC_CATALOG_MAP.values()) {
            if (catalog.getLevel().intValue() != level) {
                continue;
            }
            if (catalog.getBizId().longValue() != bizId) {
                continue;
            }
            allList.add(catalog);
        }
        return allList;
    }

    public static List<DocCatalog> getListByLevel(int level) {
        List<DocCatalog> allList = new ArrayList<DocCatalog>();

        for (DocCatalog catalog : DOC_CATALOG_MAP.values()) {
            if (catalog.getLevel().intValue() != level) {
                continue;
            }
            allList.add(catalog);
        }
        return allList;
    }


    /**
     * 获取目录树追溯数据（目录树四维：catalogId,topOrgId,yearId,subOrgId）
     *
     * @param catalogId
     * @return
     */
    public static Long[] getCatalogTreeTrace(long catalogId) {
        Long[] catalogArr = new Long[]{null, null, null, null};
        DocCatalog catalog = DOC_CATALOG_MAP.get(catalogId);
        catalogArr[catalog.getLevel() - 1] = catalog.getId();
        if (catalog.getLevel().intValue() == 2) {
            catalogArr[0] = getParentCatalogId(catalogId);
        } else if (catalog.getLevel().intValue() == 3) {
            catalogArr[0] = getTopCatalogId(catalogId);
            catalogArr[1] = getParentCatalogId(catalogId);

        } else if (catalog.getLevel().intValue() == 4) {
            catalogArr[0] = getTopCatalogId(catalogId);
            DocCatalog docCatalog = getParentCatalog(catalogId);
            catalogArr[1] = getParentCatalogId(docCatalog.getId());
            catalogArr[2] = docCatalog.getId();
        }
        return catalogArr;
    }
    @Autowired
    public void setDocCatalogService(DocCatalogService docCatalogService) {
        DocCatalogCache.docCatalogService = docCatalogService;
    }

    public static DocCatalogService getDocCatalogService() {
        return docCatalogService;
    }


}
