package com.hit.fm.common.util;

import com.alibaba.fastjson.JSONObject;
import com.hit.fm.common.constants.BizConstants;
import com.hit.fm.es.entity.TypeItem;
import com.hit.fm.es.query.jest.EsQueryResp;
import com.hit.fm.es.util.EsUtil;
import com.hit.fm.model.TemplateItem;
import com.hit.fm.model.sys.Grid;
import com.hit.fm.model.sys.Shelf;
import com.hit.fm.model.sys.ShelfPartition;
import com.hit.fm.model.sys.User;
import net.sourceforge.pinyin4j.PinyinHelper;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文 件 名：CommonUtil.java
 * 说 明：
 * 作 者：陈江海
 * 创建时间：2016-4-6
 * 版权所有：泉州哈工大工程技术研究院
 */
public class CommonUtil {

    private final static String KEY = "abcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * 字符串是否非空
     *
     * @param str
     * @return
     */
    public static boolean isNotNull(String str) {
        return (null == str || "".equals(str)) ? false : true;
    }

    /**
     * 列表是否非空
     *
     * @param list
     * @return
     */
    public static <T> boolean isListNotEmpty(List<T> list) {
        return (null == list || list.size() == 0) ? false : true;
    }

    public Map<String, Object> getErrResult(String errMsg) {
        ResultUtil result = new ResultUtil();
        result.setSuccess(false);
        result.setMsg(errMsg);
        return result.getResult();
    }

    public static boolean isNotNull(Map<String, Object> paramMap, String key) {
        if (!paramMap.containsKey(key)) {
            return false;
        }
        Object val = paramMap.get(key);
        if (null == val) {
            return false;
        }
        if (isNotNull(val.toString())) {
            return true;
        }
        return false;
    }

    public static boolean ifSuperAdmin(User user) {
        String configAdmin = ConfigProperties.getVal("admin_user_name");
        if (user != null && user.getUserName() != null && user.getUserName().equals(configAdmin)) {
            return true;
        } else {
            return false;
        }
    }

    public static List<String> getDefRoleList() {
        String rolesStr = ConfigProperties.getVal("default_role_id");
        List<String> roleList = new ArrayList<String>();
        String[] roleArr = rolesStr.split(",");
        for (int i = 0; i < roleArr.length; i++) {
            if (isNotNull(roleArr[i])) {
                roleList.add(roleArr[i]);
            }
        }
        return roleList;
    }

    /**
     * 根据属性名获取属性的值
     *
     * @param obj:对象
     * @param attrName:属性名称
     * @return
     */
    public static Object getAttrVal(Object obj, String attrName) {
        Method getMethod;
        try {
            getMethod = obj.getClass().getMethod(getGetMethodName(attrName));
            return getMethod.invoke(obj);
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * 通过字段名称获取该字段对应的get方法名称
     *
     * @param fileName
     * @return
     */
    private static String getGetMethodName(String fieldName) {
        String firstChar = fieldName.substring(0, 1);
        firstChar = firstChar.toUpperCase();
        return "get" + firstChar + fieldName.substring(1);
    }

    /**
     * 根据属性名获取属性的值
     *
     * @param obj:对象
     * @param attrName:属性名称
     * @return
     */
    public static String getAttrStringVal(Object obj, String attrName) {
        Object valObj = getAttrVal(obj, attrName);
        return (null == valObj) ? "" : valObj.toString();
    }

    public static boolean validRfid(String rfid) {
        String pattern = "^[0-9a-zA-Z]{1,}$";
        return Pattern.matches(pattern, rfid);
    }

    /**
     * 构造索引配置json串
     *
     * @return
     */
    public static JSONObject genIndexConfJson() {
        JSONObject ng_tokenizer = getSimpleJson("type", "nGram");
        ng_tokenizer.put("min_gram", "1");
        ng_tokenizer.put("max_gram", "1");
        ng_tokenizer.put("token_chars", new String[]{"letter", "digit"});
        JSONObject tokenizer = getSimpleJson("tokenizer", getSimpleJson("ng_tokenizer", ng_tokenizer));

        JSONObject ng = getSimpleJson("ng", getSimpleJson("tokenizer", "ng_tokenizer"));

        tokenizer.put("analyzer", ng);

        JSONObject analysis = getSimpleJson("analysis", tokenizer);

        JSONObject index = getSimpleJson("number_of_shards", 1);
        index.put("number_of_replicas", 0);

        analysis.put("index", index);

        JSONObject setting = getSimpleJson("settings", analysis);
        return setting;
    }

    public static JSONObject genTypeConfJson(String type, List<TypeItem> itemList) {
        JSONObject properties = new JSONObject();
        for (TypeItem item : itemList) {
            properties.put(item.getFiled(), item.getJson());
        }
        JSONObject _all = com.hit.fm.es.util.CommonUtil.getSimpleJson("analyzer", "ik_max_word");

        JSONObject typeName = com.hit.fm.es.util.CommonUtil.getSimpleJson("properties", properties);
        typeName.put("_all", _all);
        return getSimpleJson(type, typeName);
    }

    public static JSONObject getSimpleJson(String key, Object val) {
        JSONObject obj = new JSONObject();
        obj.put(key, val);
        return obj;
    }

    /**
     * 构造es 索引 名称
     *
     * @param catalogId
     * @param docTypeId
     * @return
     */
    public static String getEsIndexName(long catalogId, long docTypeId) {
        return BizConstants.ES_INDEX_PREFIX + "_" + catalogId + "_" + docTypeId;
    }

    /**
     * 构造档案数据库表名称
     *
     * @param catalogId
     * @param docTypeId
     * @return
     */
    public static String getDocTableName(long catalogId, long docTypeId) {
        return BizConstants.ES_TYPE + "_" + catalogId + "_" + docTypeId;
    }

    /**
     * 根据目录的摸版创建es type
     *
     * @param indexName:索引名称
     * @param catalogId：目录id
     * @param docTypeId：档案类型ID
     * @param list：模板列表
     * @throws Exception
     */
    public static void createEsTypeWithTemplateList(long catalogId, long docTypeId, List<TemplateItem> list) throws Exception {
        String indexName = getEsIndexName(catalogId, docTypeId);
        String typeName = BizConstants.ES_TYPE;
        List<TypeItem> itemList = new ArrayList<>();
        for (TemplateItem ti : list) {
            //需要特殊处理的字段（默认走ng，title,content走ik）
            TypeItem typeItem = null;
            if (ti.getParamCode().equals("title") || ti.getParamCode().equals("content") || ti.getParamCode().equals("subjectWord")) {//这几个走分词
                typeItem = new TypeItem(ti.getParamCode(), "string", true, "ik_max_word");
            } else if (ti.getParamCode().equals("createTime")) {//创建时间
                typeItem = new TypeItem(ti.getParamCode(), "date", "yyyy-MM-dd HH:mm:ss");
            } else if (ti.getParamCode().equals("docTime")) {//档案形成时间
                typeItem = new TypeItem(ti.getParamCode(), "date", "yyyy-MM-dd");
            } else if (ti.getParamCode().equals("secretLevel") || ti.getParamCode().equals("savePeriod") || ti.getParamCode().equals("txtPath") ||
                    ti.getParamCode().equals("pdfPath") || ti.getParamCode().equals("jpegPath") || ti.getParamCode().equals("showDocNo")) {//不分析的
                typeItem = new TypeItem(ti.getParamCode(), "string", false, null);
            } else { //一般情况
                if (ti.getParamType().shortValue() == BizConstants.FIELD_TYPE_STRING) {
                    typeItem = new TypeItem(ti.getParamCode(), "string", true, "ng");
                } else if (ti.getParamType().shortValue() == BizConstants.FIELD_TYPE_LONG) {
                    typeItem = new TypeItem(ti.getParamCode(), "long");
                } else if (ti.getParamType().shortValue() == BizConstants.FIELD_TYPE_FLOAT) {
                    typeItem = new TypeItem(ti.getParamCode(), "float");
                } else if (ti.getParamType().shortValue() == BizConstants.FIELD_TYPE_DATE) {
                    typeItem = new TypeItem(ti.getParamCode(), "date", "yyyy-MM-dd");
                } else {
                    typeItem = new TypeItem(ti.getParamCode(), "date", "yyyy-MM-dd HH:mm:ss");
                }
            }
            itemList.add(typeItem);
        }

        EsUtil.createType(indexName, typeName, itemList);
    }

    /**
     * 通过目录的摸版，构造insert 语句
     * 文本型的，长度应在设定的基础上X3（UTF-8编码）
     *
     * @param indexName
     * @param catalogId
     * @param docTypeId
     * @param list
     * @return
     */
    public static String genCreateTableSql(long catalogId, long docTypeId, List<TemplateItem> list) {
        /*
        CREATE TABLE `sys_user` (
          `id` bigint(18) NOT NULL AUTO_INCREMENT,
          `user_name` varchar(64) NOT NULL,
          `password` varchar(64) NOT NULL,
          `user_nickname` varchar(64) NOT NULL,
          `role_id` bigint(18) NOT NULL,
          PRIMARY KEY (`id`),
          UNIQUE KEY `uk_user_name` (`user_name`)
        ) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8;
        */
        String tbName = getDocTableName(catalogId, docTypeId);
        StringBuffer sb = new StringBuffer("CREATE TABLE `" + tbName + "` (");
        for (TemplateItem ti : list) {
            //固定 id 主键
            if (ti.getParamCode().equals("id")) {
                sb.append(" `" + ti.getParamCode() + "` bigint(18) NOT NULL AUTO_INCREMENT");
            } else if (ti.getParamType().shortValue() == BizConstants.FIELD_TYPE_STRING) {
                //如果长度大于2000，则必须将类型设置为clob
                if (ti.getParamLen() * 3 >= 2000) {
                    sb.append(" `" + ti.getParamCode() + "` text ");
                } else {
                    sb.append(" `" + ti.getParamCode() + "` varchar(" + ti.getParamLen() * 3 + ")");
                }


            } else if (ti.getParamType().shortValue() == BizConstants.FIELD_TYPE_LONG) {
                sb.append(" `" + ti.getParamCode() + "` bigint(" + ti.getParamLen() + ")  ");
            } else if (ti.getParamType().shortValue() == BizConstants.FIELD_TYPE_FLOAT) {
                sb.append(" `" + ti.getParamCode() + "` float(" + ti.getParamLen() + "," + ti.getDecimalLen() + ")  ");
            } else if (ti.getParamType().shortValue() == BizConstants.FIELD_TYPE_DATE) {
                sb.append(" `" + ti.getParamCode() + "` varchar(20)  ");
            } else {//dateTime
                sb.append(" `" + ti.getParamCode() + "` varchar(20)  ");
            }
            if (ti.getNotNull().shortValue() == BizConstants.NOT_NULL && !ti.getParamCode().equals("id")) {//非主键
                sb.append(" NOT NULL ");
            }
            sb.append(", ");
//            if (!ti.getParamCode().equals(list.get(list.size() - 1).getParamCode())) {
//                sb.append(", ");
//            }
        }
        sb.append("PRIMARY KEY (`id`) ");
        sb.append(") ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8;");
        return sb.toString();
    }

    /**
     * 根据目录摸版，摸版数据，构造档案批量插入语句前半部分
     * 返回值示例：INSERT INTO `sys_role`(id,name) VALUES
     *
     * @param catalogId
     * @param docTypeId
     * @param templateItemList
     * @param ignoreId
     * @return
     */
    public static String genDocInsertPrefix(long catalogId, long docTypeId, List<TemplateItem> templateItemList, boolean ignoreId) {
        String tbName = getDocTableName(catalogId, docTypeId);
        StringBuffer allSb = new StringBuffer("INSERT INTO `" + tbName + "`(");
        int i = 0;
        StringBuffer nameSb = new StringBuffer();
        for (TemplateItem item : templateItemList) {
            if (ignoreId && item.getParamCode().equals("id")) {//忽略主键
                continue;
            }

            nameSb.append(item.getParamCode() + ",");
        }
        String nameStr = nameSb.toString();
        nameStr = nameSb.substring(0, nameStr.length() - 1);
        allSb.append(nameStr + ") values");
        return allSb.toString();
    }

    /**
     * 根据目录摸版，摸版数据，构造档案批量插入语句value部分
     * 返回值示例：('125', '临时角色1kk')
     *
     * @param catalogId
     * @param docTypeId
     * @param templateItemList
     * @param data
     * @param ignoreId
     * @return
     * @throws Exception
     */
    public static String genDocInsertValueSql(long catalogId, long docTypeId, List<TemplateItem> templateItemList, Map<String, Object> data, boolean ignoreId) throws Exception {
        StringBuffer subSb = new StringBuffer("(");
        for (TemplateItem item : templateItemList) {
            if (ignoreId && item.getParamCode().equals("id")) {//忽略主键
                continue;
            }
            Object defVal = getVal(data, item);
            if (item.getNotNull().shortValue() == BizConstants.NOT_NULL && null == defVal) {
                throw new Exception(item.getParamCode() + "不可为空");
            }
            if (item.getParamType().shortValue() == BizConstants.FIELD_TYPE_STRING ||
                    item.getParamType().shortValue() == BizConstants.FIELD_TYPE_DATE ||
                    item.getParamType().shortValue() == BizConstants.FIELD_TYPE_DATETIME) {
                if (null != defVal) {
                    subSb.append("'" + defVal + "',");
                } else {
                    subSb.append("null,");
                }

            } else {
                if (null != defVal && CommonUtil.isNotNull(defVal + "")) {
                    subSb.append(defVal + ",");
                } else {
                    subSb.append("null,");
                }
            }
        }
        String subStr = subSb.toString();
        subStr = subStr.substring(0, subStr.length() - 1);
        return subStr + ")";
    }

    /**
     * 根据目录摸版，摸版数据，构造档案批量插入语句(该方法已废弃，因不能返回主键)
     *
     * @param catalogId
     * @param docTypeId
     * @param list
     * @param dataList
     * @param ignoreId:是否忽略ID
     * @return
     */
    @Deprecated
    public static String genDocInsertSql(long catalogId, int docTypeId, List<TemplateItem> templateItemList, List<Map<String, Object>> dataList, boolean ignoreId) throws Exception {
        String tbName = getDocTableName(catalogId, docTypeId);
        //插入单条记录：INSERT INTO `sys_role`(id,name) VALUES ('125', '临时角色1kk');
        //插入多条记录：INSERT INTO table1(i) VALUES(1),(2),(3),(4),(5);

        StringBuffer allSb = new StringBuffer("INSERT INTO `" + tbName + "`(");
        int i = 0;
        StringBuffer nameSb = new StringBuffer();
        for (TemplateItem item : templateItemList) {
            if (ignoreId && item.getParamCode().equals("id")) {//忽略主键
                continue;
            }

            nameSb.append(item.getParamCode() + ",");
        }
        String nameStr = nameSb.toString();
        nameStr = nameSb.substring(0, nameStr.length() - 1);
        allSb.append(nameStr + ") values");

        int j = 0;
        for (Map<String, Object> map : dataList) {
            StringBuffer subSb = new StringBuffer("(");
            for (TemplateItem item : templateItemList) {
                if (ignoreId && item.getParamCode().equals("id")) {//忽略主键
                    continue;
                }
                Object defVal = getVal(map, item);
                if (item.getNotNull().shortValue() == BizConstants.NOT_NULL && null == defVal) {
                    throw new Exception(item.getParamCode() + "不可为空");
                }
                if (item.getParamType().shortValue() == BizConstants.FIELD_TYPE_STRING ||
                        item.getParamType().shortValue() == BizConstants.FIELD_TYPE_DATE ||
                        item.getParamType().shortValue() == BizConstants.FIELD_TYPE_DATETIME) {
                    if (null != defVal) {
                        subSb.append("'" + defVal + "',");
                    } else {
                        subSb.append("null,");
                    }

                } else {
                    subSb.append(defVal + ",");
                }
            }
            String subStr = subSb.toString();
            subStr = subStr.substring(0, subStr.length() - 1);
            allSb.append(subStr + ")");
            if (j != dataList.size() - 1) {
                allSb.append(",");
            } else {
                allSb.append(";");
            }
            j++;
        }
        return allSb.toString();
    }

    /**
     * 构造文档更新语句
     *
     * @param catalogId
     * @param docTypeId
     * @param templateItemList
     * @param dataList
     * @return
     * @throws Exception
     */
    public static String genDocUpdateSql(long catalogId, long docTypeId, List<TemplateItem> templateItemList, Map<String, Object> docMap) throws Exception {
        if (!docMap.containsKey("id")) {
            throw new Exception("请指定档案ID");
        }
        String tbName = getDocTableName(catalogId, docTypeId);
        //插入单条记录：update doc_2_2 set xx=xx,yy=yy where id = #{id}

        StringBuffer allSb = new StringBuffer("update " + tbName + " ");
        boolean firstParam = true;
        for (TemplateItem item : templateItemList) {
            if (!docMap.containsKey(item.getParamCode()) || item.getParamCode().equals("id")) {//有包含该数据，才更新
                continue;
            }
            if (firstParam) {
                allSb.append(" set ");
                firstParam = false;
            }
            Object val = getVal(docMap, item);
            if (null == val || !CommonUtil.isNotNull(val + "")) {
                allSb.append(item.getParamCode() + "=null,");
            } else {
                if (item.getParamType().shortValue() == BizConstants.FIELD_TYPE_STRING ||
                        item.getParamType().shortValue() == BizConstants.FIELD_TYPE_DATE ||
                        item.getParamType().shortValue() == BizConstants.FIELD_TYPE_DATETIME) {
                    allSb.append(item.getParamCode() + "='" + val + "',");
                } else {
                    allSb.append(item.getParamCode() + "=" + val + ",");
                }
            }
        }
        if (firstParam) {
            throw new Exception("请指定需要更新的字段");
        }
        String sql = allSb.substring(0, allSb.length() - 1);
        sql += " where id=" + docMap.get("id");
        return sql;
    }

    /**
     * 构造获取文档sql
     *
     * @param catalogId
     * @param docTypeId
     * @param id
     * @return
     * @throws Exception
     */
    public static String genDocGetSql(long catalogId, long docTypeId, long id) throws Exception {
        String tbName = getDocTableName(catalogId, docTypeId);
        return "select * from " + tbName + " where id=" + id;
    }

//    /**
//     * 构建通过档案编号查询档案语句
//     * @param catalogId
//     * @param docTypeId
//     * @param docNo
//     * @param curId
//     * @return
//     * @throws Exception
//     */
//    @Deprecated
//    public static String genDocGetByDocNoSql(long catalogId, long docTypeId, String docNo, Long curId) throws Exception {
//        String tbName = getDocTableName(catalogId, docTypeId);
//        String sql = "select id,title,docNo from " + tbName + " where docNo='" + docNo + "' ";
//        if (null != curId) {
//            sql += " and id !=" + curId;
//        }
//        return sql;
//    }
//
//    /**
//     * 构建通过档案编号批量查询语句
//     * @param catalogId
//     * @param docTypeId
//     * @param docNoList
//     * @return
//     * @throws Exception
//     */
//    @Deprecated
//    public static String genBatchGetByDocNoSql(long catalogId, long docTypeId, List<String> docNoList) throws Exception {
//        String tbName = getDocTableName(catalogId, docTypeId);
//        StringBuffer sb = new StringBuffer("select id,title,docNo from " + tbName + " where docNo in (");
//        for (String docNo : docNoList) {
//            sb.append("'" + docNo + "',");
//        }
//        return sb.substring(0, sb.length() - 1) + ")";
//    }


    /**
     * 构造删除文档sql
     *
     * @param catalogId
     * @param docTypeId
     * @param id
     * @return
     * @throws Exception
     */
    public static String genDocDelSql(long catalogId, long docTypeId, long id) throws Exception {
        String tbName = getDocTableName(catalogId, docTypeId);
        return "delete from " + tbName + " where id=" + id;
    }

    /**
     * 构造统计总数文档sql
     *
     * @param catalogId
     * @param docTypeId
     * @return
     * @throws Exception
     */
    public static String genDocCountSql(long catalogId, long docTypeId) throws Exception {
        String tbName = getDocTableName(catalogId, docTypeId);
        return "select count(id) from " + tbName;
    }

    /**
     * 构造批量更新档案预归档状态语句
     *
     * @param catalogId
     * @param docTypeId
     * @param id
     * @param status
     * @return
     * @throws Exception
     */
    public static String genUpdateDocPreArchiveStatusSql(long catalogId, long docTypeId, List<Long> id, short status) {
        String tbName = getDocTableName(catalogId, docTypeId);
        String idsStr = StringUtils.join(id, ",");
        return "update " + tbName + " set preArchiveStatus =" + status + " where id in(" + idsStr + ")";
    }

    /**
     * 构造更新
     *
     * @param catalogId
     * @param docTypeId
     * @param id
     * @param status
     * @return
     * @throws Exception
     */
    public static String genUpdateDocBoxIdSql(long catalogId, long docTypeId, long id, Long boxId) {
        String tbName = getDocTableName(catalogId, docTypeId);
        if (null != boxId) {
            return "update " + tbName + " set boxId =" + boxId + " where id =" + id;
        } else {
            return "update " + tbName + " set boxId =null where id =" + id;
        }
    }

    /**
     * 构建更新开放状态语句
     *
     * @param catalogId
     * @param docTypeId
     * @param id
     * @param status
     * @return
     */
    public static String genUpdateDocOpenStatusSql(long catalogId, long docTypeId, long id, short status) {
        String tbName = getDocTableName(catalogId, docTypeId);
        return "update " + tbName + " set isOpen =" + status + " where id =" + id;
    }

    /**
     * 构建更新档案删除状态语句
     *
     * @param catalogId
     * @param docTypeId
     * @param id
     * @param status
     * @return
     */
    public static String genUpdateDocDeleteStatusSql(long catalogId, long docTypeId, long id, short status, String delTime) {
        String tbName = getDocTableName(catalogId, docTypeId);
        if (null == delTime) {
            return "update " + tbName + " set isDelete =" + status + ",deleteTime=null where id =" + id;
        } else {
            return "update " + tbName + " set isDelete =" + status + ",deleteTime='" + delTime + "' where id =" + id;
        }
    }

    /**
     * 构建更新销毁状态语句
     *
     * @param catalogId
     * @param docTypeId
     * @param id
     * @param status
     * @return
     */
    public static String genUpdateDocDestroyStatusSql(long catalogId, long docTypeId, long id, short status) {
        String tbName = getDocTableName(catalogId, docTypeId);
        return "update " + tbName + " set isDestroy =" + status + " where id =" + id;
    }

    /**
     * 获取值
     *
     * @param map
     * @param item
     * @return
     */
    private static Object getVal(Map<String, Object> map, TemplateItem item) throws Exception {

        Object retVal = null;
        if (!map.containsKey(item.getParamCode())) {
            return null;
        } else {
            retVal = map.get(item.getParamCode());
            if (item.getParamType().shortValue() == BizConstants.FIELD_TYPE_STRING) {
                if (null != retVal && CommonUtil.isNotNull(retVal + "")) {
                    return retVal.toString();
                } else {
                    return "";
                }

            } else if (item.getParamType().shortValue() == BizConstants.FIELD_TYPE_LONG) {
                if (null != retVal && CommonUtil.isNotNull(retVal + "")) {
                    return Long.valueOf(retVal.toString());
                }

            } else if (item.getParamType().shortValue() == BizConstants.FIELD_TYPE_FLOAT) {
                if (null != retVal && CommonUtil.isNotNull(retVal + "")) {
                    return Float.valueOf(retVal.toString());
                }
            } else if (item.getParamType().shortValue() == BizConstants.FIELD_TYPE_DATE) {
                if (null != retVal && CommonUtil.isNotNull(retVal + "")) {
                    return retVal.toString();
                }

            } else {
                if (null != retVal && CommonUtil.isNotNull(retVal + "")) {
                    return retVal.toString();
                }
            }
        }
        return retVal;
    }

    /**
     * 获取拼音（字段大小必须小于64）
     *
     * @param word
     * @return
     */
    public static String getPinyin(String word) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < word.length(); i++) {
            String[] subArr = PinyinHelper.toHanyuPinyinStringArray(word.charAt(i));
            if (null == subArr || subArr.length == 0) {
                sb.append(word.charAt(i));
            } else {
                sb.append(subArr[0]);
            }
        }
        String str = sb.toString();
        if (str.length() > 32) {
            str = str.substring(0, 32) + "_" + getRandomString(10);
        } else {
            str = str + "_" + getRandomString(10);
        }
        return str;

    }

    /**
     * 获取请求中的档案参数列表
     *
     * @param request
     * @return
     */
    public static Map<String, Object> getDocRequestParamMap(HttpServletRequest request) {
        Map<String, Object> docParamMap = new HashMap<String, Object>();

        Enumeration enu = request.getParameterNames();
        while (enu.hasMoreElements()) {
            String paramName = (String) enu.nextElement();
            docParamMap.put(paramName, request.getParameter(paramName));
        }
        return docParamMap;
    }


    public static String getRandomString(int length) {
        StringBuffer sb = new StringBuffer();
        int len = KEY.length();
        for (int i = 0; i < length; i++) {
            sb.append(KEY.charAt(getRandom(len - 1)));
        }
        return sb.toString();
    }

    private static int getRandom(int count) {
        return (int) Math.round(Math.random() * (count));
    }

    /**
     * 基础上传路径（档案附件）
     *
     * @return
     */
    public static String getUploadPath() {
        return DateUtil.formatDate(new Date(), "yyyyMMdd");
    }

    /**
     * 流程审批图片上传路径
     *
     * @return
     */
    public static String getApprovedImgUploadPath() {
        return BizConstants.APPROVED_IMG_FOLDER + File.separator + DateUtil.formatDate(new Date(), "yyyyMMdd");

    }

    /**
     * 下载临时路径
     *
     * @param suffix
     * @return
     */
    public static String getDownloadPath(String suffix) {
        return DateUtil.formatDate(new Date(), "yyyyMMdd") + "_" + getRandomString(5) + "." + suffix;
    }

    /**
     * 获取文件后缀
     *
     * @param fileName
     * @return
     */
    public static String getFileSuffix(String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index != -1) {
            return fileName.substring(index + 1);
        }
        return "";
    }

    /**
     * 上传文件名
     *
     * @param suffix:后缀
     * @return
     */
    public static String genUploadFileName(String suffix) {
        return DateUtil.formatDate(new Date(), "yyyyMMddHHmmssSSS") + "." + suffix;
    }

    /**
     * 根据文件存储路径读取文件内容
     *
     * @param savePath
     * @return
     */
    public static String readFileContent(String savePath) {
        String fullPath = BizConstants.UPLOAD_PATH + File.separator + savePath;
        return readFile(fullPath);
    }

    public static String readFile(String filePath) {
        BufferedReader br = null;
        InputStreamReader is = null;
        try {
            StringBuffer sb = new StringBuffer();
            is = new InputStreamReader(new FileInputStream(filePath), "GBK");
            br = new BufferedReader(is);
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                sb.append(line + "\r\n");
            }
            br.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (null != is) {
                    is.close();
                }
            } catch (Exception e) {
            }
            try {
                if (null != br) {
                    br.close();
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * 设置目录树4维数据
     *
     * @param result
     * @param speDocCatalogId
     */
    public static void setCatalogTreeTraceResult(ResultUtil result, long speDocCatalogId) {
        //档案分类四维数据
        Long[] catalogTreeArr = DocCatalogCache.getCatalogTreeTrace(speDocCatalogId);
        String topOrgId = null != catalogTreeArr[1] ? catalogTreeArr[1].longValue() + "" : "";
        String yearId = null != catalogTreeArr[2] ? catalogTreeArr[2].longValue() + "" : "";
        String subOrgId = null != catalogTreeArr[3] ? catalogTreeArr[3].longValue() + "" : "";
        result.setProperty("catalogId", catalogTreeArr[0]);
        result.setProperty("topOrgId", topOrgId);
        result.setProperty("yearId", yearId);
        result.setProperty("subOrgId", subOrgId);
        //设置名称
        result.setProperty("catalogName", DocCatalogCache.getName(catalogTreeArr[0]));
        result.setProperty("topOrgName", "".equals(topOrgId) ? "" : DocCatalogCache.getName(Long.valueOf(topOrgId)));
        result.setProperty("yearName", "".equals(yearId) ? "" : DocCatalogCache.getName(Long.valueOf(yearId)));
        result.setProperty("subOrgName", "".equals(subOrgId) ? "" : DocCatalogCache.getName(Long.valueOf(subOrgId)));

    }

    /**
     * 设置目录树4维数据
     *
     * @param docQueryMap
     * @param speDocCatalogId
     */
    public static void setCatalogTreeTraceResult(Map<String, Object> docQueryMap, long speDocCatalogId) {
        //档案分类四维数据
        Long[] catalogTreeArr = DocCatalogCache.getCatalogTreeTrace(speDocCatalogId);
        String topOrgId = null != catalogTreeArr[1] ? catalogTreeArr[1].longValue() + "" : "";
        String yearId = null != catalogTreeArr[2] ? catalogTreeArr[2].longValue() + "" : "";
        String subOrgId = null != catalogTreeArr[3] ? catalogTreeArr[3].longValue() + "" : "";
        docQueryMap.put("catalogId", catalogTreeArr[0]);
        docQueryMap.put("topOrgId", topOrgId);
        docQueryMap.put("yearId", yearId);
        docQueryMap.put("subOrgId", subOrgId);
    }

    /**
     * 处理ie,chrom,firfox下处理文件名显示乱码
     *
     * @param request
     * @param fileNames
     * @return
     */
    public static String processDownloadFileName(HttpServletRequest request, String fileNames) {
        String codedfilename = fileNames;
        try {
            String agent = request.getHeader("USER-AGENT");
            if (null != agent && -1 != agent.indexOf("MSIE") || null != agent && -1 != agent.indexOf("Trident")) {// ie
                codedfilename = java.net.URLEncoder.encode(fileNames, "UTF8");
            } else if (null != agent && -1 != agent.indexOf("Mozilla")) {// 火狐,chrome等
                codedfilename = new String(fileNames.getBytes("UTF-8"), "iso-8859-1");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return codedfilename;
    }

    /**
     * 获取当前生成规则中的动态字段
     *
     * @param docNoRule
     * @return
     */
    public static List<String> getDocRuleDynamicField(String docNoRule) {
        List<String> docRuleFieldList = getDocNoRuleFieldList(docNoRule);
        List<String> retList = new ArrayList<String>();
        for (String field : docRuleFieldList) {
            if (!BizConstants.DEF_DOC_NO_FIELD_LIST.contains(field)) {
                retList.add(field);
            }
        }
        return retList;
    }

    /**
     * 判断模板项是否符合编号规则
     *
     * @param templateItemList
     * @param docNoRule
     * @return
     */
    public static boolean isTemplateListMatchDocNo(List<TemplateItem> templateItemList, String docNoRule) {
        if (!CommonUtil.isListNotEmpty(templateItemList)) {
            return false;
        }
        boolean ret = true;
        List<String> docRuleFieldList = getDocNoRuleFieldList(docNoRule);
        if (docRuleFieldList.size() == 0) {
            return true;
        }
        Map<String, String> templateMap = new HashMap<String, String>();
        for (TemplateItem item : templateItemList) {
            templateMap.put(item.getParamName(), item.getParamCode());
        }
        for (String field : docRuleFieldList) {
            if (BizConstants.DEF_DOC_NO_FIELD_LIST.contains(field)) {//属于档案固定字段的，无需校验
                continue;
            }
            if (!templateMap.containsKey(field)) {
                ret = false;
                break;
            }
        }

        return ret;
    }

    /**
     * 档案编号字段字符串
     *
     * @param docNoRule
     * @return
     */
    public static String getDocNoRuleFieldStrs(String docNoRule) {
        List<String> list = getDocNoRuleFieldList(docNoRule);
        //去掉系统默认的
        List<String> neededList = new ArrayList<String>();
        for (String s : list) {
            if (BizConstants.DEF_DOC_NO_FIELD_LIST.contains(s)) {
                continue;
            }
            neededList.add(s);
        }
        return StringUtils.join(neededList, ",");
    }

    /**
     * 获取档案编号规则中包含的字段
     *
     * @param docNoRule
     * @return
     */
    public static List<String> getDocNoRuleFieldList(String docNoRule) {
        List<String> fieldList = new ArrayList<String>();
        Pattern pattern = Pattern.compile("(?<=\\()(.+?)(?=\\))");
        Matcher matcher = pattern.matcher(docNoRule);
        while (matcher.find())
            fieldList.add(matcher.group());

        return fieldList;
    }

    /**
     * 设置档案开放时间查询条件
     *
     * @param docParamMap
     */
    public static void setOpenDocTimeCondition(Map<String, Object> docParamMap) {
        //获取档案开放起始年份
        String yearStr = DateUtil.formatDate(new Date(), "yyyy");
        int year = Integer.valueOf(yearStr) - BizConstants.OPEN_YEAR;
        String startDocTime = year + "-01-01";
        docParamMap.put("endDocTime", startDocTime);
    }

    /**
     * 设置档案销毁时间查询条件
     *
     * @param docParamMap
     */
    public static void setDestroyDocTimeCondition(Map<String, Object> docParamMap) {
        //获取档案开放起始年份
        String yearStr = DateUtil.formatDate(new Date(), "yyyy");
        int year = Integer.valueOf(yearStr) - BizConstants.DESTROY_YEAR;
        String startDocTime = year + "-01-01";
        docParamMap.put("endDocTime", startDocTime);
    }

    /**
     * 处理ES查询结果(es中long型的值在val=0时，会被转为double型，此处进行转换)
     *
     * @param resp
     * @return
     */
    public static void treatDocEsSearchResult(EsQueryResp<Map<String, Object>> resp) {
        if (CommonUtil.isListNotEmpty(resp.getList())) {
            for (Map<String, Object> docMap : resp.getList()) {
                treatDocMap(docMap);
            }
        }

    }

    public static void treatDocMap(Map<String, Object> docMap) {
        if (null == docMap) {
            return;
        }
        //处理long型值被处理成doble值的情况
        //isOpen isDestroy subOrgId preArchiveStatus
        Object isOpen = docMap.get("isOpen");
        if (null != isOpen) {
            if (isOpen instanceof Double || isOpen instanceof BigDecimal) {
                docMap.put("isOpen", double2Short(isOpen));
            } else {
                docMap.put("isOpen", Short.valueOf(isOpen + ""));
            }
        }
        Object isDestroy = docMap.get("isDestroy");
        if (null != isDestroy) {
            if (isDestroy instanceof Double || isDestroy instanceof BigDecimal) {
                docMap.put("isDestroy", double2Short(isDestroy));
            } else {
                docMap.put("isDestroy", Short.valueOf(isDestroy + ""));
            }
        }
        Object preArchiveStatus = docMap.get("preArchiveStatus");
        if (null != preArchiveStatus) {
            if (preArchiveStatus instanceof Double || preArchiveStatus instanceof BigDecimal) {
                docMap.put("preArchiveStatus", double2Short(preArchiveStatus));
            } else {
                docMap.put("preArchiveStatus", Short.valueOf(preArchiveStatus + ""));
            }
        }

        Object recordType = docMap.get("recordType");
        if (null != recordType) {
            if (recordType instanceof Double || recordType instanceof BigDecimal) {
                docMap.put("recordType", double2Short(recordType));
            } else {
                docMap.put("recordType", Short.valueOf(recordType + ""));
            }
        }
        Object catalogId = docMap.get("catalogId");
        if (null != catalogId) {
            if (catalogId instanceof Double) {
                docMap.put("catalogId", double2Long(catalogId));
            }
        }
        Object topOrgId = docMap.get("topOrgId");
        if (null != topOrgId) {
            if (topOrgId instanceof Double) {
                docMap.put("topOrgId", double2Long(topOrgId));
            }
        }
        Object yearId = docMap.get("yearId");
        if (null != yearId) {
            if (yearId instanceof Double) {
                docMap.put("yearId", double2Long(yearId));
            }
        }
        Object subOrgId = docMap.get("subOrgId");
        if (null != subOrgId) {
            if (subOrgId instanceof Double) {
                docMap.put("subOrgId", double2Long(subOrgId));
            }
        }
        Object docTypeId = docMap.get("docTypeId");
        if (null != docTypeId) {
            if (docTypeId instanceof Double) {
                docMap.put("docTypeId", double2Long(docTypeId));
            }
        }
        Object id = docMap.get("id");
        if (null != id) {
            if (id instanceof Double) {
                docMap.put("id", double2Long(id));
            }
        }

    }

    private static Long double2Long(Object douVal) {
        return Math.round(Double.valueOf(douVal.toString()));
    }

    private static short double2Short(Object douVal) {
        return Short.valueOf(Math.round(Float.valueOf(douVal.toString())) + "");
    }

    /**
     * 初始化密集架下的柜子（分左右侧）
     *
     * @param shelfPartition
     * @param shelf
     * @return
     */
    public static List<Grid> initShelfGridList(ShelfPartition shelfPartition, Shelf shelf) {
        List<Grid> gridList = new ArrayList<Grid>();
        int total = shelfPartition.getColNum() * shelfPartition.getRowNum();
        for (int i = 1; i <= shelfPartition.getRowNum(); i++) {
            for (int j = 1; j <= shelfPartition.getColNum(); j++) {
                Grid leftGrid = new Grid();
                leftGrid.setRowNum(i);
                leftGrid.setColNum(j);
                leftGrid.setLg(shelfPartition.getLg());
                leftGrid.setWidth(shelfPartition.getWidth());
                leftGrid.setHigh(shelfPartition.getHigh());
                leftGrid.setPartitionId(shelfPartition.getId());
                leftGrid.setShelfId(shelf.getId());
                leftGrid.setShelfDirection(BizConstants.DIRECTION_LEFT);
                gridList.add(leftGrid);

                Grid rightGrid = new Grid();
                rightGrid.setRowNum(i);
                rightGrid.setColNum(j);
                rightGrid.setLg(shelfPartition.getLg());
                rightGrid.setWidth(shelfPartition.getWidth());
                rightGrid.setHigh(shelfPartition.getHigh());
                rightGrid.setPartitionId(shelfPartition.getId());
                rightGrid.setShelfId(shelf.getId());
                rightGrid.setShelfDirection(BizConstants.DIRECTION_RIGHT);
                gridList.add(rightGrid);
            }
        }
        return gridList;
    }


    /**
     * 得到指定加减天数的时间字符串
     *
     * @param addDays
     * @param isNeedTime
     * @return
     */
    public static String getDayTimeStr(int addDays, boolean isNeedTime) {
        Calendar dtnow = Calendar.getInstance();
        dtnow.add(Calendar.DAY_OF_MONTH, addDays);
        int month = dtnow.get(Calendar.MONTH) + 1;
        String monthStr = (month < 10) ? "0" + month : "" + month;

        int day = dtnow.get(Calendar.DAY_OF_MONTH);
        String dayStr = (day < 10) ? "0" + day : "" + day;

        String lastmonth = Integer.toString(dtnow.get(Calendar.YEAR)) + "-" + monthStr + "-" + dayStr;
        if (isNeedTime) {
            lastmonth = lastmonth + " 00:00:00";
        }
        return lastmonth;
    }

    public static List<String> getAllDayList(String startTime, String endTime) {
        try {
            // 遍历之间的所有天数
            SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
            Date begin = sf.parse(startTime);
            Date end = sf.parse(endTime);

            List<String> dayList = new ArrayList<String>();
            while (begin.before(end)) {
                dayList.add(sf.format(begin));
                begin = new Date(begin.getTime() + 3600 * 1000 * 24);
            }
            return dayList;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 设置档案默认值
     *
     * @param docReqMap
     */
    public static void setDocDefaultVal(Map<String, Object> docReqMap) {

        docReqMap.put("titleNg", docReqMap.get("title"));
        docReqMap.put("showDocNo", docReqMap.get("docNo"));//不分词的档案编号
        //抽取内容
        if (CommonUtil.isNotNull(docReqMap, "txtPath")) {
            String txtPath = docReqMap.get("txtPath").toString();
            String content = CommonUtil.readFileContent(txtPath);
            docReqMap.put("content", content);
            docReqMap.put("contentNg", content);
        } else {
            docReqMap.put("content", "");
            docReqMap.put("contentNg", "");
        }
        //如果是新建的
        if (!CommonUtil.isNotNull(docReqMap, "id")) {
            docReqMap.put("createTime", DateUtil.formatDate(new Date()));
            docReqMap.put("isOpen", BizConstants.DOC_OPEN_NO);
            docReqMap.put("isDestroy", BizConstants.DOC_DESTROY_NO);
            docReqMap.put("isDelete", BizConstants.DOC_DELETE_NO);
            docReqMap.put("preArchiveStatus", BizConstants.DOC_STATUS_NO_YET);
        }
    }

    public static String getDllName() {
        String path = BizConstants.SHELF_DLL_PATH;
        int index = path.lastIndexOf("/");
        if (index != -1) {
            path = path.substring(index + 1);
        }
        path = path.replaceAll(".dll", "");
        return path;
    }

    /**
     * 是否符合ip格式
     * 需满足如下规则：
     * 任何一个1位或者两位数字，即0~99；
     * 任何一个以1开头的3位数字，即100~199；
     * 任何一个以2开头，第二位数字在0~4之间的数字即200~249；
     * 任何一个以25开头，第三位数字在0~5之间的三位数字，即250~255；
     *
     * @param ip
     * @return
     */
    public static boolean isIp(String ip) {
        String reg = "(?=(\\b|\\D))(((\\d{1,2})|(1\\d{1,2})|(2[0-4]\\d)|(25[0-5]))\\.){3}((\\d{1,2})|(1\\d{1,2})|(2[0-4]\\d)|(25[0-5]))(?=(\\b|\\D))";
        return Pattern.matches(reg, ip);
    }

    /**
     * 是否私密档案
     *
     * @param secretLevel
     * @return
     */
    public static boolean isPrivateDoc(String secretLevel) {
        if (secretLevel.equals("秘密") || secretLevel.equals("机密") || secretLevel.equals("绝密")) {
            return true;
        }
        return false;
    }
    public static void main(String[] args) throws Exception {
        System.out.println(isIp("127.1.1.1"));
    }


}
