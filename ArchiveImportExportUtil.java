package com.hit.fm.common.util;

import com.alibaba.fastjson.JSON;
import com.hit.fm.common.constants.BizConstants;
import com.hit.fm.common.exception.ImportException;
import com.hit.fm.model.TemplateItem;
import com.hit.fm.model.sys.DocCatalog;
import com.hit.fm.model.sys.DocType;
import com.hit.fm.model.sys.Org;
import com.hit.fm.service.TemplateItemService;
import com.hit.fm.service.sys.DocCatalogService;
import com.hit.fm.service.sys.DocTypeService;
import com.hit.fm.service.sys.OrgService;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件名：ArchiveImportExportUtil.java
 * 说明：档案导入导出帮助类
 * 作者：陈江海
 * 创建时间：2018/1/11
 * 版权所有：泉州哈工大工程技术研究院
 */
@Component
public class ArchiveImportExportUtil {

    private static Logger logger = Logger.getLogger(ArchiveImportExportUtil.class);
    private static OrgService orgService;
    private static DocTypeService docTypeService;
    private static TemplateItemService templateItemService;
    private static DocCatalogService docCatalogService;


    /**
     * 导出目录excel
     *
     * @param request
     * @param response
     * @param parentCatalogId
     * @param type://档案类型1：预归档；2：归档，见BizConstants.COMMON_TYPE_PRE*
     * @throws Exception
     */
    public static void exportCatalog(HttpServletRequest request, HttpServletResponse response, long parentCatalogId, short type) throws Exception {
        logger.info("正在导出目录数据.....");
        long start = System.currentTimeMillis();
        Workbook workbook = ExcelUtil.createEmptyWorkBook();
        addExportCatalogSheet(parentCatalogId, null, type, workbook);
        addOrgSheet(workbook, null);
        ExcelUtil.downloadWorkBook(workbook, request, response, "目录.xls");
        logger.info("导出目录数据成功，耗时：" + (System.currentTimeMillis() - start) + "毫秒");
    }

    /**
     * 导出模板excel
     * 备注：导出模板时，需同步执行exportCatalog，以确认模板对应的目录数据存在
     *
     * @param request
     * @param response
     * @param parentCatalogId
     * @param type：数据类型；1：预归档；2：归档。见:BizConstants.COMMON_TYPE_PRE
     * @throws Exception
     */
    public static void exportTemplateItem(HttpServletRequest request, HttpServletResponse response, long parentCatalogId, short type) throws Exception {
        logger.info("正在导出模板数据.....");
        long start = System.currentTimeMillis();
        Workbook workbook = ExcelUtil.createEmptyWorkBook();
        List<DocType> docTypeList = docTypeService.findList(new DocType());
        addTemplateItemSheet(parentCatalogId, type, workbook);
        addOrgSheet(workbook, null);
        addExportCatalogSheet(parentCatalogId, null, type, workbook);
        addDocTypeSheet(docTypeList, workbook);
        ExcelUtil.downloadWorkBook(workbook, request, response, "模板.xls");
        logger.info("导出模板数据成功，耗时：" + (System.currentTimeMillis() - start) + "毫秒");
    }

    /**
     * 导出档案数据
     *
     * @param request
     * @param response
     * @param parentCatalogId
     * @param type
     * @throws Exception
     */
    public static void exportDocDataExcel(HttpServletRequest request, HttpServletResponse response, List<Map<String, Object>> docList, short type) throws Exception {
        logger.info("正在导出档案数据(EXCEL).....");
        long start = System.currentTimeMillis();
        Workbook workbook = ExcelUtil.createEmptyWorkBook();

        List<DocType> docTypeList = docTypeService.findList(new DocType());
        //导出档案数据，每个分类一个sheet页
        addDocDataSheets(docList, docTypeList, workbook);
        addOrgSheet(workbook, null);
        addExportCatalogSheet(0l, null, type, workbook);
        addTemplateItemSheet(0l, type, workbook);
        addDocTypeSheet(docTypeList, workbook);
        ExcelUtil.downloadWorkBook(workbook, request, response, "档案数据.xls");
        logger.info("导出档案数据(EXCEL)成功，耗时：" + (System.currentTimeMillis() - start) + "毫秒");
    }

    /**
     * 导出预归档审批通过数据到word
     *
     * @param request
     * @param response
     * @param docCatalog
     * @throws Exception
     */
    public static String createDocDataWord(HttpServletRequest request, HttpServletResponse response, List<Map<String, Object>> docList, short type) throws Exception {
        logger.info("正在导出档案数据（WORD）....");
        long start = System.currentTimeMillis();
        Workbook workbook = ExcelUtil.createEmptyWorkBook();
        List<DocType> docTypeList = docTypeService.findList(new DocType());

        //构建档案类型map
        Map<String, String> docTypeMap = genDocTypeMap(docTypeList);
        //按照 目录—档案类型区别档案列表
        Map<String, List<Map<String, Object>>> catalogDocListMap = new HashMap<String, List<Map<String, Object>>>();
        for (Map<String, Object> docMap : docList) {
            String catalogId = docMap.get("catalogId") + "";
            String docTypeId = docMap.get("docTypeId") + "";
            String key = catalogId + "_" + docTypeId;
            List<Map<String, Object>> subList = new ArrayList<Map<String, Object>>();
            if (catalogDocListMap.containsKey(key)) {
                subList = catalogDocListMap.get(key);
            }
            subList.add(docMap);
            catalogDocListMap.put(key, subList);
        }

        String wordBasePath = BizConstants.UPLOAD_PATH + File.separator + "word_" + System.currentTimeMillis();
        File baseFile = new File(wordBasePath);
        if (!baseFile.exists()) {
            baseFile.mkdirs();
        }

        //word表格头部(全宗单位、标题、档案编号、保管期限、保密等级)
        List<String> keyList = new ArrayList<String>() {{
            add("全宗单位");
            add("年度");
            add("标题");
            add("档案编号");
            add("保管期限");
            add("保密等级");
        }};
        List<String> pathList = new ArrayList<String>();
        for (String key : catalogDocListMap.keySet()) {
            Map<String, TemplateItem> templateItemMap = genTemplateMap(key);
            List<Map<String, Object>> subList = catalogDocListMap.get(key);

            String[] keyArr = key.split("_");
            String title = DocCatalogCache.getName(Long.valueOf(keyArr[0])) + "_" + docTypeMap.get(keyArr[1]);


            List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> doc : subList) {
                Map<String, Object> data = new HashMap<String, Object>();
                String catalogId = doc.get("catalogId") + "";
                data.put("全宗单位", DocCatalogCache.getName(Long.valueOf(catalogId)));
                String yearId = doc.get("yearId") + "";
                data.put("年度", DocCatalogCache.getName(Long.valueOf(yearId)));
                data.put("标题", doc.get("title"));
                data.put("档案编号", doc.get("docNo"));
                data.put("保管期限", doc.get("savePeriod"));
                data.put("保密等级", doc.get("secretLevel"));
                dataList.add(data);
            }
            String path = CreateWordUtil.createWord(wordBasePath, title, keyList, dataList, BizConstants.WORD_PAGE_SIZE);
            pathList.add(path);
        }
        //打zip包，并返回zip包地址
        List<File> fileList = new ArrayList<File>();
        for (String path : pathList) {
            File file = new File(path);
            fileList.add(file);
        }
        String zipPath = wordBasePath + File.separator + "档案数据.zip";
        writeZip(fileList, zipPath);
        logger.info("生成档案数据（WORD）成功，耗时：" + (System.currentTimeMillis() - start) + "毫秒");
        return zipPath;
    }

    /**
     * 打包zip文件
     *
     * @param fileList
     * @param zipFullPath
     */
    private static void writeZip(Collection<File> fileList, String zipFullPath) {
        OutputStream os = null;
        ZipOutputStream zos = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(zipFullPath));
            zos = new ZipOutputStream(os);
            byte[] buf = new byte[8192];
            int len;
            for (File file : fileList) {
                ZipEntry ze = new ZipEntry(file.getName());
                zos.putNextEntry(ze);
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                while ((len = bis.read(buf)) > 0) {
                    zos.write(buf, 0, len);
                }
                zos.closeEntry();
            }
            zos.closeEntry();
            zos.close();
            logger.info("zip.create.suc");
        } catch (Exception e) {
            logger.error("zip.create.error:" + zipFullPath + " " + e.getMessage(), e);
        } finally {
            if (null != os) {
                try {
                    os.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * 导出全宗单位预归档数据导入模板
     *
     * @param request
     * @param response
     * @param catalogId：门类ID
     * @param topOrgId：全宗单位ID
     * @param type:档案类型
     * @throws Exception
     */
    public static void exprotTopOrgImportTemplate(HttpServletRequest request, HttpServletResponse response, long catalogId, long topOrgId, short type) throws Exception {
        logger.info("正在下载预归档档案数据离线填写模板.....");
        long start = System.currentTimeMillis();
        Workbook workbook = ExcelUtil.createEmptyWorkBook();
        List<DocType> docTypeList = docTypeService.findList(new DocType());
        //导出档案数据，每个档案分类一个sheet页
        addImportDataTemplateSheets(catalogId, type, docTypeList, workbook);
        String curYear = DateUtil.formatDate(new Date(), "yyyy");
        String workBookName = DocCatalogCache.getName(catalogId) + "_" + DocCatalogCache.getName(topOrgId) + "_" + curYear + "_预归档离线填写模板.xls";
        ExcelUtil.downloadWorkBook(workbook, request, response, workBookName);
        logger.info("预归档数据导入模板下载成功，耗时：" + (System.currentTimeMillis() - start) + "毫秒");
    }
    /******************************************************************************************************/
    /*****************************************sheet页 start************************************************/
    /******************************************************************************************************/


    /**
     * 添加机构sheet页
     *
     * @param wb
     * @param topOrgId :指定了topId
     */
    public static void addOrgSheet(Workbook wb, Long topOrgId) {
        logger.info("正在添加机构sheet.....");
        long start = System.currentTimeMillis();
        Org queryOrg = new Org();
        if (null != topOrgId) {
            queryOrg.setParentOrgId(topOrgId);
        }
        List<Org> orgList = orgService.findList(queryOrg);
        List<Org> newOrgList = new ArrayList<Org>();
        if (CommonUtil.isListNotEmpty(orgList)) {
            for (Org org : orgList) {
                if (org.getLevel().intValue() == 1 || org.getLevel().intValue() == 2) {
                    newOrgList.add(org);
                }
            }
        }
        orgList = newOrgList;
        if (null != topOrgId) {
            Org topOrg = orgService.get(topOrgId);
            orgList.add(topOrg);
        }
        //排序
        Collections.sort(orgList, new Comparator<Org>() {
            @Override
            public int compare(Org o1, Org o2) {
                return o1.getLevel() - o2.getLevel();
            }
        });
        ExcelUtil.addSheet("机构", wb, orgList, BizConstants.EXPORT_ORG_COLUMN_ARR, BizConstants.EXPORT_ORG_KEY_ARR);
        logger.info("添加机构sheet成功，耗时：" + (System.currentTimeMillis() - start) + "毫秒");
    }

    /**
     * 添加目录sheet页
     *
     * @param parentCatalogId:门类ID
     * @param topOrgId:全宗单位ID
     * @param type
     * @param wb
     * @throws IOException
     */
    public static void addExportCatalogSheet(long parentCatalogId, Long topOrgId, short type, Workbook wb) throws IOException {
        logger.info("正在添加目录sheet.....");
        long start = System.currentTimeMillis();
        List<DocCatalog> allList = new ArrayList<DocCatalog>();
        if (parentCatalogId == 0) {//全部
            allList = DocCatalogCache.getAllSubList(parentCatalogId, type);
        } else if (null == topOrgId) {//指定了门类
            allList = DocCatalogCache.getAllSubList(parentCatalogId, type);
            DocCatalog original = DocCatalogCache.get(parentCatalogId);
            if (null != original) {
                allList.add(original);
            }
        } else {//指定了门类及全宗单位ID
            allList = DocCatalogCache.getAllSubList(topOrgId, type);
            DocCatalog first = DocCatalogCache.get(parentCatalogId);
            DocCatalog second = DocCatalogCache.get(topOrgId);
            allList.add(first);
            allList.add(second);
        }
        //按照level排序
        Collections.sort(allList, new Comparator<DocCatalog>() {
            @Override
            public int compare(DocCatalog o1, DocCatalog o2) {
                return o1.getLevel() - o2.getLevel();//o1 - o2 为升序
            }
        });
        ExcelUtil.addSheet("目录", wb, allList, BizConstants.EXPORT_CATALOG_COLUMN_ARR, BizConstants.EXPORT_CATALOG_KEY_ARR);
        logger.info("添加目录sheet成功，耗时：" + (System.currentTimeMillis() - start) + "毫秒");
    }

    /**
     * 添加档案类型sheet页
     *
     * @param request
     * @param response
     * @param parentCatalogId
     * @throws IOException
     */
    public static void addDocTypeSheet(List<DocType> docTypeList, Workbook workbook) throws IOException {
        logger.info("正在添加档案类型sheet.....");
        long start = System.currentTimeMillis();
        if (null == docTypeList) {//没有传值过来的，才需要查询
            docTypeList = docTypeService.findList(new DocType());
        }
        ExcelUtil.addSheet("档案类型", workbook, docTypeList, new String[]{"ID", "类型", "档案编号生成规则", "父ID", "关联字段"}, new String[]{"id", "name", "docNoRule", "parentId", "relateField"});
        logger.info("添加档案类型sheet成功，耗时：" + (System.currentTimeMillis() - start) + "毫秒");
    }

    /**
     * 添加模板sheet页
     *
     * @param parentCatalogId
     * @param dataType:数据类型；1：预归档；2：归档。见:BizConstants.COMMON_TYPE_PRE
     * @param workbook
     * @throws IOException
     */
    public static void addTemplateItemSheet(long catalogId, short dataType, Workbook workbook) throws IOException {
        logger.info("正在添加模板sheet.....");
        long start = System.currentTimeMillis();
        TemplateItem item = new TemplateItem();
        item.setDataType(dataType);
        if (catalogId > 0) {
            item.setCatalogId(catalogId);
        }
        List<TemplateItem> itemList = templateItemService.findList(item);
        Collections.sort(itemList, new Comparator<TemplateItem>() {
            @Override
            public int compare(TemplateItem o1, TemplateItem o2) {
                if (o1.getCatalogId().longValue() == o2.getCatalogId().longValue()) {
                    return o1.getCatalogId().compareTo(o2.getCatalogId());
                }
                return o1.getCatalogId().compareTo(o2.getCatalogId());
            }
        });
        ExcelUtil.addSheet("档案模板", workbook, itemList, BizConstants.EXPORT_TEMPLATE_COLUMN_ARR, BizConstants.EXPORT_TEMPLATE_KEY_ARR);
        logger.info("添加模板sheet成功，耗时：" + (System.currentTimeMillis() - start) + "毫秒");
    }

    /**
     * 添加档案数据sheet页(每个分类下的数据自己一个分类)
     *
     * @param docList
     * @param workbook
     * @throws IOException
     */
    public static void addDocDataSheets(List<Map<String, Object>> docList, List<DocType> docTypeList, Workbook workbook) throws IOException {
        logger.info("正在添加档案数据sheet.....");
        long start = System.currentTimeMillis();
        //构建档案类型map
        Map<String, String> docTypeMap = genDocTypeMap(docTypeList);
        //按照 目录—档案类型区别档案列表
        Map<String, List<Map<String, Object>>> catalogDocListMap = new HashMap<String, List<Map<String, Object>>>();
        for (Map<String, Object> docMap : docList) {
            String catalogId = docMap.get("catalogId") + "";
            String docTypeId = docMap.get("docTypeId") + "";
            String key = catalogId + "_" + docTypeId;
            List<Map<String, Object>> subList = new ArrayList<Map<String, Object>>();
            if (catalogDocListMap.containsKey(key)) {
                subList = catalogDocListMap.get(key);
            }
            //移除一些无需用户填写的字段，见BizConstants.DEF_TI_SYS_LIST
            docMap.remove("id");
            docMap.remove("titleNg");
            docMap.remove("content");
            docMap.remove("contentNg");
            docMap.remove("recordType");
            docMap.remove("createTime");
            docMap.remove("boxId");
            docMap.remove("isOpen");
            docMap.remove("isDestroy");
            docMap.remove("preArchiveStatus");
            docMap.remove("es_metadata_id");
            docMap.remove("subOrgName");
            docMap.remove("showDocNo");

            subList.add(docMap);
            catalogDocListMap.put(key, subList);
        }
        for (String key : catalogDocListMap.keySet()) {
            //查询该分类的模板，并将code映射为name
            Map<String, TemplateItem> templateItemMap = genTemplateMap(key);
            List<Map<String, Object>> subList = catalogDocListMap.get(key);
            Map<String, Object> docMap = subList.get(0);
            String[] paramNameArr = new String[docMap.size()];
            String[] paramCodeArr = new String[docMap.size()];
            int index = 0;
            for (String docField : docMap.keySet()) {
                paramCodeArr[index] = docField;
                index++;
            }
            paramCodeArr = sortDocFiledArr(paramCodeArr);
            index = 0;
            for (String code : paramCodeArr) {
                paramNameArr[index] = code;
                if (templateItemMap.containsKey(code)) {
                    paramNameArr[index] = templateItemMap.get(code).getParamName();//显示中文字段
                }
                index++;
            }

            ExcelUtil.addSheet(getDocSheetName(key, docTypeMap), workbook, subList, paramNameArr, paramCodeArr);
        }

        logger.info("添加档案数据sheet成功，耗时：" + (System.currentTimeMillis() - start) + "毫秒");
    }

    /**
     * 增加档案离线填写模板
     *
     * @param catalogId
     * @param workbook
     * @throws IOException
     */
    public static void addImportDataTemplateSheets(long catalogId, short type, List<DocType> docTypeList, Workbook workbook) throws IOException {
        Map<String, String> docTypeMap = genDocTypeMap(docTypeList);
        //每个档案类型一个sheet
        for (DocType docType : docTypeList) {
            TemplateItem templateItem = new TemplateItem();
            templateItem.setCatalogId(catalogId);
            templateItem.setDocTypeId(docType.getId());
            templateItem.setDataType(type);
            List<TemplateItem> templateItemList = templateItemService.findList(templateItem);
            if (!CommonUtil.isListNotEmpty(templateItemList)) {
                continue;
            }
            //添加系统默认字段
            List<String> igoneFieldList = new ArrayList<String>() {{
                add("id");
                add("titleNg");
                add("content");
                add("contentNg");
                add("recordType");
                add("createTime");
                add("boxId");
                add("isOpen");
                add("isDestroy");
                add("preArchiveStatus");
                //由于是给指定的全宗单位的指定年度填写的，因此这些字段也不用填写（catalogId/topOrgId/yearId/docTypeId）
                add("catalogId");
                add("topOrgId");
                add("yearId");
                add("docTypeId");
                //附件也先不导出
                add("txtPath");
                add("pdfPath");
                add("jpegPath");
                add("txtRealName");
                add("pdfRealName");
                add("jpegRealName");
                add("showDocNo");
            }};
            for (TemplateItem item : BizConstants.DEF_TI_SYS_LIST) {
                if (!igoneFieldList.contains(item.getParamCode())) {
                    templateItemList.add(item);
                }
            }

            //手动排序模板
            templateItemList = sortTemplateItem(templateItemList);
            String[] paramNameArr = new String[templateItemList.size()];
            int index = 0;
            for (TemplateItem item : templateItemList) {
                paramNameArr[index] = item.getParamName();
                index++;
            }
//            String sheetName = getDocSheetName(catalogId, docType.getId(), docTypeMap);
            String sheetName = docType.getName();
            ExcelUtil.addSheetOnlyHeader(sheetName, workbook, paramNameArr);
        }
    }

    /**
     * 排序档案字段（为了让用户填写更人性化）
     *
     * @param docFieldArr
     * @return
     */
    private static String[] sortDocFiledArr(String[] docFieldArr) {
        List<TemplateItem> templateItemList = new ArrayList<TemplateItem>();
        //客制化排序字段
        for (String field : docFieldArr) {
            TemplateItem item = new TemplateItem();
            item.setParamCode(field);
            templateItemList.add(item);
        }
        templateItemList = sortTemplateItem(templateItemList);
        docFieldArr = new String[templateItemList.size()];
        int index = 0;
        for (TemplateItem templateItem : templateItemList) {
            docFieldArr[index] = templateItem.getParamCode();
            index++;
        }
        return docFieldArr;
    }

    /**
     * 手动排序列表
     *
     * @param list
     * @return
     */
    private static List<TemplateItem> sortTemplateItem(List<TemplateItem> list) {
        for (TemplateItem item : list) {
            int seq = 999;//默认999
            String field = item.getParamCode();
            if (field.equals("catalogId")) {
                seq = 1;
            } else if (field.equals("docTypeId")) {
                seq = 2;
            } else if (field.equals("topOrgId")) {
                seq = 3;
            } else if (field.equals("yearId")) {
                seq = 4;
            } else if (field.equals("subOrgId")) {
                seq = 5;
            } else if (field.equals("docNo")) {
                seq = 6;
            } else if (field.equals("title")) {
                seq = 7;
            } else if (field.equals("docTime")) {
                seq = 8;
            } else if (field.equals("secretLevel")) {
                seq = 9;
            } else if (field.equals("savePeriod")) {
                seq = 10;
            } else if (field.equals("subjectWord")) {
                seq = 11;
            } else if (field.equals("dataPageCount")) {
                seq = 12;
            } else if (field.equals("txtPath")) {
                seq = 13;
            } else if (field.equals("txtRealName")) {
                seq = 14;
            } else if (field.equals("pdfPath")) {
                seq = 15;
            } else if (field.equals("pdfRealName")) {
                seq = 16;
            } else if (field.equals("jpegPath")) {
                seq = 17;
            } else if (field.equals("jpegRealName")) {
                seq = 18;
            }
            item.setSeq(seq);
        }
        Collections.sort(list, new Comparator<TemplateItem>() {
            @Override
            public int compare(TemplateItem o1, TemplateItem o2) {
                return o1.getSeq() - o2.getSeq();
            }
        });
        return list;
    }

    /**
     * 构建档案类型map
     *
     * @param docTypeList
     * @return
     */
    private static Map<String, String> genDocTypeMap(List<DocType> docTypeList) {
        Map<String, String> docTypeMap = new HashMap<String, String>();
        if (CommonUtil.isListNotEmpty(docTypeList)) {
            for (DocType docType : docTypeList) {
                docTypeMap.put(docType.getId() + "", docType.getName());
            }
        }

        return docTypeMap;
    }

    /**
     * 获取档案sheet页数据
     * 返回格式：档案数据_ + "目录" + "_" + "档案类型"
     *
     * @param key
     * @return
     */
    private static String getDocSheetName(String key, Map<String, String> docTypeMap) {
        String[] subArr = key.split("_");
        return getDocSheetName(Long.valueOf(subArr[0]), Long.valueOf(subArr[1]), docTypeMap);
    }

    private static String getDocSheetName(long catalogId, long docTypeId, Map<String, String> docTypeMap) {
        String catalogName = DocCatalogCache.getName(catalogId);
        return "档案数据_" + catalogName + "_" + docTypeMap.get(docTypeId + "");
    }

    /**
     * 获取模板列表
     *
     * @param key
     * @return
     */
    private static Map<String, TemplateItem> genTemplateMap(String key) {
        Map<String, TemplateItem> templateItemMap = new HashMap<String, TemplateItem>();
        String[] subArr = key.split("_");
        TemplateItem templateItem = new TemplateItem();
        templateItem.setCatalogId(Long.valueOf(subArr[0]));
        templateItem.setDocTypeId(Long.valueOf(subArr[1]));
        List<TemplateItem> templateItemList = templateItemService.findList(templateItem);
        if (null == templateItemList) {
            templateItemList = new ArrayList<TemplateItem>();
        }
        templateItemList.addAll(BizConstants.DEF_TI_SYS_LIST);
        for (TemplateItem item : templateItemList) {
            templateItemMap.put(item.getParamCode(), item);
        }
        return templateItemMap;
    }


    /******************************************************************************************************/
    /*****************************************sheet页   end************************************************/
    /******************************************************************************************************/


    /******************************************************************************************************/
    /*****************************************解析sheet start***********************************************/
    /******************************************************************************************************/
    /**
     * 解析catalog sheet页，并做基本校验
     * 结构：{ "id", "level", "parentOrgId", "name", "code"}
     *
     * @param sheet
     * @return
     */
    public static List<Org> parseOrgSheet(Sheet sheet) throws ImportException {
        if (null == sheet) {
            throw new ImportException("机构sheet页不存在！");
        }

        List<Org> orgList = new ArrayList<Org>();
        for (short i = 1; i <= sheet.getLastRowNum(); i++) {
            Org org = new Org();
            Row row = sheet.getRow(i);
            try {
                org.setId(Long.valueOf(ExcelUtil.getCellValue(row.getCell(0))));
            } catch (Exception e) {
                throw new ImportException("机构sheet第" + (i + 1) + "行，ID不可为空，且需为正整数");
            }
            try {
                org.setLevel(Integer.valueOf(ExcelUtil.getCellValue(row.getCell(1))));
            } catch (Exception e) {
                throw new ImportException("机构sheet第" + (i + 1) + "行，层级不可为空，且需为正整数");
            }
            try {
                org.setParentOrgId(Long.valueOf(ExcelUtil.getCellValue(row.getCell(2))));
            } catch (Exception e) {
                throw new ImportException("机构sheet第" + (i + 1) + "行，父ID不可为空，且需为正整数");
            }
            String name = ExcelUtil.getCellValue(row.getCell(3));
            if (!CommonUtil.isNotNull(name)) {
                throw new ImportException("机构sheet第" + (i + 1) + "行，名称不可为空！");
            }
            org.setName(name);
            String code = ExcelUtil.getCellValue(row.getCell(4));
            if (!CommonUtil.isNotNull(name)) {
                throw new ImportException("机构sheet第" + (i + 1) + "行，编码不可为空！");
            }
            org.setCode(code);
            org.setOldId(org.getId());
            orgList.add(org);
        }
        if (!CommonUtil.isListNotEmpty(orgList)) {
            return orgList;
        }
        //按level排序
        Map<Long, Org> firstLevelMap = new HashMap<Long, Org>();
        for (Org org : orgList) {
            if (org.getLevel().intValue() == 1) {
                firstLevelMap.put(org.getId(), org);
            }
        }
        if (!CommonUtil.isListNotEmpty(orgList)) {
            throw new ImportException("机构sheet数据非法！机构均为孤立数据，无法导入！");
        }
        //校验所有二级组织的父id均需存在
        for (Org org : orgList) {
            if (org.getLevel().intValue() == 1) {
                continue;
            } else if (org.getLevel().intValue() == 2) {
                if (!firstLevelMap.containsKey(org.getParentOrgId())) {
                    throw new ImportException("机构sheet数据非法！机构：" + org.getName() + "父节点不存在！");
                }
            }
        }
        validOrgUniqueCode(orgList);
        validOrgUniqueName(BizConstants.DEFAULT_ROOT_ORG_ID, orgList);
        Collections.sort(orgList, new Comparator<Org>() {
            @Override
            public int compare(Org o1, Org o2) {
                return o1.getLevel() - o2.getLevel();
            }
        });
        return orgList;
    }

    /**
     * 校验机构全局编码唯一
     *
     * @param parentId
     * @param orgList
     */
    private static void validOrgUniqueCode(List<Org> newList) throws ImportException {
        Map<String, Org> subOrgMap = new HashMap<String, Org>();
        for (Org org : newList) {
            if (!subOrgMap.containsKey(org.getName())) {
                subOrgMap.put(org.getName(), org);
            } else {
                throw new ImportException("机构sheet数据非法！存在相同的机构编码：" + org.getCode());
            }
        }
    }

    /**
     * 校验机构统一父id下name唯一
     *
     * @param parentId
     * @param orgList
     */
    private static void validOrgUniqueName(long parentId, List<Org> newList) throws ImportException {
        Map<String, Org> subOrgMap = new HashMap<String, Org>();
        for (Org org : newList) {
            if (org.getParentOrgId().longValue() == parentId) {
                if (!subOrgMap.containsKey(org.getName())) {
                    subOrgMap.put(org.getName(), org);
                } else {
                    throw new ImportException("机构sheet数据非法！同一级别下存在相同的机构名称：" + org.getName());
                }
            }
        }
        if (subOrgMap.size() == 0) {
            return;
        }
        //递归调用
        for (Org org : subOrgMap.values()) {
            validOrgUniqueName(org.getId().longValue(), newList);
        }
    }

    /**
     * 校验导入的组织列表与数据库现有数据的约束校验
     * 机构约束：编码全局唯一；同个pid下的名称唯一
     *
     * @param newList:列表
     * @param type：数据类型
     * @throws ImportException
     */
    public static void validOrgConstraint(List<Org> newList, short type) throws ImportException {


        List<Org> existsOrgList = orgService.findList(new Org());
        Map<String, Org> orgCodeMap = new HashMap<>();
        Map<String, Org> orgNameMap = new HashMap<>();
        for (Org org : existsOrgList) {
            if (org.getLevel().intValue() == 1) {
                orgNameMap.put(org.getName(), org);
            }
            orgCodeMap.put(org.getCode(), org);
        }
        //寻找数据库中已有的数据，此部分数据无需处理
        for (Org o : newList) {
            if (orgCodeMap.containsKey(o.getCode())) {
                o.setNewId(orgCodeMap.get(o.getCode()).getId());
                logger.info("机构:" + o.getName() + "机构编码已存在，无需导入,oldId:" + o.getOldId() + "; newId:" + o.getNewId());
                continue;
            }
        }

        //模拟合并新旧两个列表，并再次执行约束即可
        Map<Long, Long> idMap = new HashMap<Long, Long>();//新旧id,map key为旧id；value为新id
        long startFakeId = -100l;//模拟新数据的主键，负数开始，防冲突
        //设置id
        for (Org org : newList) {
            if (null == org.getNewId()) {
                org.setNewId(startFakeId);
                startFakeId--;
            }
            idMap.put(org.getOldId(), org.getNewId());
        }
        //替换掉parentId
        for (Org org : newList) {
            if (org.getParentOrgId().longValue() == BizConstants.DEFAULT_ROOT_ORG_ID) {//不是根节点的，要设置对应的父节点
                continue;
            }
            //如果pid不存在，数据非法
            if (!idMap.containsKey(org.getParentOrgId())) {
                throw new ImportException("机构sheet数据非法！机构" + org.getName() + "为孤立数据，无法导入！");
            }
            org.setOldParentId(org.getParentOrgId());
            org.setParentOrgId(idMap.get(org.getOldId()));

            //不存在于已有数据库中，则加入
            if (org.getNewId().longValue() < -1) {
                existsOrgList.add(org);
            }
        }
        //existsOrgList.addAll(newList);

        //重新校验，完美~~~~~~~~~
        validOrgUniqueCode(existsOrgList);
        validOrgUniqueName(BizConstants.DEFAULT_ROOT_ORG_ID, existsOrgList);
        //校验通过后，恢复数据
        for (Org org : newList) {
            if (null != org.getNewId() && org.getNewId() < -1) {
                org.setNewId(null);
            }
            if (null != org.getParentOrgId() && org.getParentOrgId() < -1) {
                org.setParentOrgId(org.getOldParentId());
            }
        }
    }


    /**
     * 解析catalog sheet页，并做基本校验、数据关联校验
     * 结构：{"id", "name", "level", "parentId", "bizId"}
     *
     * @param sheet
     * @return
     */
    public static List<DocCatalog> parseCatalogSheet(Sheet sheet, List<Org> orgList, short type) throws ImportException {
        if (null == sheet) {
            throw new ImportException("目录sheet页不存在！");
        }
        List<DocCatalog> catalogList = new ArrayList<DocCatalog>();
        for (short i = 1; i <= sheet.getLastRowNum(); i++) {
            DocCatalog docCatalog = new DocCatalog();
            Row row = sheet.getRow(i);
            try {
                docCatalog.setId(Long.valueOf(ExcelUtil.getCellValue(row.getCell(0))));
            } catch (Exception e) {
                throw new ImportException("目录sheet第" + (i + 1) + "行，ID不可为空，且需为正整数");
            }
            String name = ExcelUtil.getCellValue(row.getCell(1));
            if (!CommonUtil.isNotNull(name)) {
                throw new ImportException("目录sheet第" + (i + 1) + "行，名称不可为空！");
            }
            docCatalog.setName(name);

            try {
                docCatalog.setLevel(Integer.valueOf(ExcelUtil.getCellValue(row.getCell(2))));
            } catch (Exception e) {
                throw new ImportException("目录sheet第" + (i + 1) + "行，层级不可为空！，且需为正整数");
            }
            try {
                docCatalog.setParentId(Long.valueOf(ExcelUtil.getCellValue(row.getCell(3))));
            } catch (Exception e) {
                throw new ImportException("目录sheet第" + (i + 1) + "行，父ID不可为空！，且需为正整数");
            }
            try {
                docCatalog.setBizId(Long.valueOf(ExcelUtil.getCellValue(row.getCell(4))));
            } catch (Exception e) {
                throw new ImportException("目录sheet第" + (i + 1) + "行，业务不可为空！，且需为正整数");
            }
            docCatalog.setOldId(docCatalog.getId());
            docCatalog.setType(type);
            catalogList.add(docCatalog);
        }
        if (!CommonUtil.isListNotEmpty(catalogList)) {
            return catalogList;
        }

        //依次校验，每条数据的父id均需存在
        Map<Long, DocCatalog> firstLevelCatalogMap = new HashMap<Long, DocCatalog>();
        for (DocCatalog docCatalog : catalogList) {
            if (docCatalog.getLevel().intValue() == 1) {
                firstLevelCatalogMap.put(docCatalog.getId(), docCatalog);
            }
        }
        Map<Long, Org> orgMap = new HashMap<Long, Org>();
        for (Org org : orgList) {
            orgMap.put(org.getId(), org);
        }
        //校验level2的均需存在，且需在组织列表中存在
        Map<Long, DocCatalog> secondLevelCatalogMap = new HashMap<Long, DocCatalog>();
        for (DocCatalog catalog : catalogList) {
            if (catalog.getLevel().intValue() == 2) {
                if (!firstLevelCatalogMap.containsKey(catalog.getParentId())) {
                    throw new ImportException("目录sheet数据非法！目录：" + catalog.getName() + "所对应的父级目录不存在！");
                }
                if (!orgMap.containsKey(catalog.getBizId())) {
                    throw new ImportException("目录sheet数据非法！目录：" + catalog.getName() + "所对应的全宗单位在机构sheet页中不存在！");
                }
                secondLevelCatalogMap.put(catalog.getId(), catalog);
            }
        }
        //校验level3的均需存在
        Map<Long, DocCatalog> thirdLevelCatalogMap = new HashMap<Long, DocCatalog>();
        for (DocCatalog catalog : catalogList) {
            if (catalog.getLevel().intValue() == 3) {
                if (!secondLevelCatalogMap.containsKey(catalog.getParentId())) {
                    throw new ImportException("目录sheet数据非法！目录：" + catalog.getName() + "所对应的父级目录不存在！");
                }
                thirdLevelCatalogMap.put(catalog.getId(), catalog);
            }
        }
        //校验level4的均需存在
        for (DocCatalog catalog : catalogList) {
            if (catalog.getLevel().intValue() == 4) {
                if (!thirdLevelCatalogMap.containsKey(catalog.getParentId())) {
                    throw new ImportException("目录sheet数据非法！目录：" + catalog.getName() + "所对应的父级目录不存在！");
                }
                if (!orgMap.containsKey(catalog.getBizId())) {
                    throw new ImportException("目录sheet数据非法！目录：" + catalog.getName() + "所对应的机构在机构sheet页中不存在！");
                }
            }
        }
        validCatalogUniqueName(0l, catalogList);
        Collections.sort(catalogList, new Comparator<DocCatalog>() {
            @Override
            public int compare(DocCatalog o1, DocCatalog o2) {
                return o1.getLevel() - o2.getLevel();
            }
        });
        return catalogList;
    }

    /**
     * 校验指定catalog下的子节点，name是否唯一
     *
     * @param catalog
     * @param newList
     */
    private static void validCatalogUniqueName(long parentCatalogId, List<DocCatalog> newList) throws ImportException {
        Map<String, DocCatalog> subCatalogMap = new HashMap<String, DocCatalog>();
        for (DocCatalog catalog : newList) {
            if (catalog.getParentId().longValue() == parentCatalogId) {
                if (!subCatalogMap.containsKey(catalog.getName())) {
                    subCatalogMap.put(catalog.getName(), catalog);
                } else {
                    throw new ImportException("目录sheet数据非法！同一级别下存在相同的目录名称：" + catalog.getName());
                }
            }
        }
        if (subCatalogMap.size() == 0) {
            return;
        }
        //递归调用
        for (DocCatalog docCatalog : subCatalogMap.values()) {
            validCatalogUniqueName(docCatalog.getId().longValue(), newList);
        }
    }

    /**
     * 校验目录约束
     * 目录约束:name + parentId 唯一约束
     *
     * @param newList
     * @throws ImportException
     */
    public static void validDocCatalogConstraint(List<DocCatalog> newList, short type) throws ImportException {
        //只验证指定type的
        DocCatalog queryDc = new DocCatalog();
        queryDc.setType(type);
        List<DocCatalog> existsList = docCatalogService.findList(queryDc);
        Map<String, DocCatalog> existsCatalogMap = new HashMap<String, DocCatalog>();//key为name + "_" + level +  "_" + type)
        for (DocCatalog docCatalog : existsList) {
            String key = docCatalog.getName() + "_" + docCatalog.getLevel() + "_" + docCatalog.getType() + "_" + docCatalog.getParentId();
            existsCatalogMap.put(key, docCatalog);
        }

        //如果一级目录在系统中存在，则需要替换掉id（父不存在，子必不存在，因此无需校验二三四级的）
        Map<Long, Long> idMap = new HashMap<Long, Long>();
        for (DocCatalog docCatalog : newList) {
            if (docCatalog.getLevel().intValue() != 1) {
                continue;
            }
            String key = docCatalog.getName() + "_" + docCatalog.getLevel() + "_" + docCatalog.getType() + "_" + docCatalog.getParentId();
            if (existsCatalogMap.containsKey(key)) {
                docCatalog.setNewId(existsCatalogMap.get(key).getId());
                idMap.put(docCatalog.getOldId(), docCatalog.getNewId());
                logger.info("目录:" + docCatalog.getName() + "已存在，无需导入,oldId:" + docCatalog.getOldId() + "; newId:" + docCatalog.getNewId());
            }
        }
        //循环判断2,3,4级的
        for (DocCatalog docCatalog : newList) {
            if (docCatalog.getLevel() == 1) {
                continue;
            }
            if (!idMap.containsKey(docCatalog.getParentId())) {//父id都不存在，本级别肯定不存在
                continue;
            }
            String key = docCatalog.getName() + "_" + docCatalog.getLevel() + "_" + docCatalog.getType() + "_" + idMap.get(docCatalog.getParentId());
            //父id存在，本级别采有可能存在

            if (existsCatalogMap.containsKey(key)) {
                docCatalog.setNewId(existsCatalogMap.get(key).getId());
                idMap.put(docCatalog.getId(), existsCatalogMap.get(key).getId());
                logger.info("目录:" + docCatalog.getName() + "已存在，无需导入,oldId:" + docCatalog.getOldId() + "; newId:" + docCatalog.getNewId());
            }
        }


        //模拟合并新旧两个列表，并再次执行约束即可
        long startFakeId = -100l;//模拟新数据的主键，负数开始，防冲突
        //设置id
        for (DocCatalog docCatalog : newList) {
            if (null == docCatalog.getNewId()) {
                docCatalog.setNewId(startFakeId);
                startFakeId--;
                idMap.put(docCatalog.getOldId(), docCatalog.getNewId());
            }

        }
        //替换掉parentId
        for (DocCatalog docCatalog : newList) {
            if (docCatalog.getParentId().longValue() == 0l) {//不是根节点的，要设置对应的父节点
                continue;
            }
            //如果pid不存在，数据非法
            if (!idMap.containsKey(docCatalog.getParentId())) {
                throw new ImportException("目录sheet数据非法！目录" + docCatalog.getName() + "为孤立数据，无法导入！");
            }
            docCatalog.setOldParentId(docCatalog.getParentId());
            docCatalog.setParentId(idMap.get(docCatalog.getOldId()));

            if (docCatalog.getNewId() < 0) {
                existsList.add(docCatalog);
            }
        }
//        existsList.addAll(newList);
        validCatalogUniqueName(0l, existsList);
        //校验通过后，恢复数据
        for (DocCatalog docCatalog : newList) {
            if (null != docCatalog.getNewId() && docCatalog.getNewId() < -1) {
                docCatalog.setNewId(null);
            }
            if (null != docCatalog.getParentId() && docCatalog.getParentId() < -1) {
                docCatalog.setParentId(docCatalog.getOldParentId());
            }
        }
    }


    /**
     * 解析档案类型列表
     *
     * @param sheet
     * @return
     * @throws ImportException
     */
    public static List<DocType> parseDocTypeSheet(Sheet sheet) throws ImportException {
        if (null == sheet) {
            throw new ImportException("档案类型sheet页不存在！");
        }
        List<DocType> docTypeList = new ArrayList<DocType>();
        for (short i = 1; i <= sheet.getLastRowNum(); i++) {
            DocType docType = new DocType();
            Row row = sheet.getRow(i);
            try {
                docType.setId(Long.valueOf(ExcelUtil.getCellValue(row.getCell(0))));
            } catch (Exception e) {
                throw new ImportException("档案类型sheet第" + (i + 1) + "行，ID不可为空！，且需为正整数");
            }
            String name = ExcelUtil.getCellValue(row.getCell(1));
            if (!CommonUtil.isNotNull(name)) {
                throw new ImportException("档案类型sheet第" + (i + 1) + "行，名称不可为空！");
            }
            docType.setName(name);

            String docNoRule = ExcelUtil.getCellValue(row.getCell(2));
            if (!CommonUtil.isNotNull(docNoRule)) {
                throw new ImportException("档案类型sheet第" + (i + 1) + "行，档案编号生成规则不可为空！");
            }
            docType.setDocNoRule(docNoRule);

            try {
                docType.setParentId(Long.valueOf(ExcelUtil.getCellValue(row.getCell(3))));
            } catch (Exception e) {
                throw new ImportException("档案类型sheet第" + (i + 1) + "行，父ID不可为空！，且需为正整数");
            }
            String relateField = ExcelUtil.getCellValue(row.getCell(4));
            if (!CommonUtil.isNotNull(relateField)) {
                relateField = "";
            }
            docType.setRelateField(relateField);
            docType.setOldId(docType.getId());
            docTypeList.add(docType);
        }
        validDocTypeUniqueName(docTypeList);
        return docTypeList;
    }

    /**
     * 验证档案类型约束(与验证组织的完成一致)
     *
     * @param newList
     * @throws ImportException
     */
    public static void validDocTypeConstraint(List<DocType> newList) throws ImportException {
        List<DocType> existsList = docTypeService.findList(new DocType());
        Map<String, DocType> docTypeMap = new HashMap<String, DocType>();
        if (CommonUtil.isListNotEmpty(existsList)) {
            for (DocType docType : existsList) {
                docTypeMap.put(docType.getName(), docType);
            }
        }
        for (DocType docType : newList) {
            if (docTypeMap.containsKey(docType.getName())) {
                docType.setNewId(docTypeMap.get(docType.getName()).getId());
                logger.info("档案类型:" + docType.getName() + "机构编码已存在，无需导入,oldId:" + docType.getOldId() + "; newId:" + docType.getNewId());
            }
        }

        //模拟合并新旧两个列表，并再次执行约束即可
        Map<Long, Long> idMap = new HashMap<Long, Long>();//新旧id,map key为旧id；value为新id
        long startFakeId = -100l;//模拟新数据的主键，负数开始，防冲突
        //设置id
        for (DocType docType : newList) {
            if (null == docType.getNewId()) {
                docType.setNewId(startFakeId);
                startFakeId--;
            }
            idMap.put(docType.getOldId(), docType.getNewId());
        }
        //替换掉parentId
        for (DocType docType : newList) {
            if (docType.getParentId().longValue() == 0l) {//不是根节点的，要设置对应的父节点
                continue;
            }
            //如果pid不存在，数据非法
            if (!idMap.containsKey(docType.getParentId())) {
                throw new ImportException("档案类型sheet数据非法！档案类型" + docType.getName() + "为孤立数据，无法导入！");
            }
            docType.setOldParentId(docType.getParentId());
            docType.setParentId(idMap.get(docType.getOldId()));

            //不存在于已有数据库中，则加入
            if (docType.getNewId().longValue() < -1) {
                existsList.add(docType);
            }
        }

        //重新校验，完美~~~~~~~~~
        validDocTypeUniqueName(existsList);
        //校验通过后，恢复数据
        for (DocType docType : newList) {
            if (null != docType.getNewId() && docType.getNewId() < -1) {
                docType.setNewId(null);
            }
            if (null != docType.getParentId() && docType.getParentId() < -1) {
                docType.setParentId(docType.getOldParentId());
            }
        }
    }

    /**
     * 校验机构全局编码唯一
     *
     * @param parentId
     * @param orgList
     */
    private static void validDocTypeUniqueName(List<DocType> newList) throws ImportException {
        Map<String, DocType> subDocTypeMap = new HashMap<String, DocType>();
        for (DocType docType : newList) {
            if (!subDocTypeMap.containsKey(docType.getName())) {
                subDocTypeMap.put(docType.getName(), docType);
            } else {
                throw new ImportException("档案类型sheet数据非法！存在相同的档案类型：" + docType.getName());
            }
        }
    }

    /**
     * 解析档案模板
     * 结构：{"id", "catalogId", "docTypeId", "paramName", "paramCode", "paramType", "paramLen", "decimalLen", "print", "notNull", "seq", "templateType"}
     *
     * @param sheet
     * @return
     * @throws ImportException
     */
    public static List<TemplateItem> parseTemplateItemSheet(Sheet sheet, List<DocCatalog> catalogList, List<DocType> docTypeList, short type) throws ImportException {
        if (null == sheet) {
            throw new ImportException("模板sheet页不存在！");
        }
        List<TemplateItem> templateItemList = new ArrayList<TemplateItem>();
        for (short i = 1; i <= sheet.getLastRowNum(); i++) {
            TemplateItem templateItem = new TemplateItem();
            Row row = sheet.getRow(i);
            try {
                templateItem.setId(Long.valueOf(ExcelUtil.getCellValue(row.getCell(0))));
            } catch (Exception e) {
                throw new ImportException("模板sheet第" + (i + 1) + "行，ID不可为空，且需为正整数");
            }
            try {
                templateItem.setCatalogId(Long.valueOf(ExcelUtil.getCellValue(row.getCell(1))));
            } catch (Exception e) {
                throw new ImportException("模板sheet第" + (i + 1) + "行，档案门类不可为空，且需为正整数");
            }
            try {
                templateItem.setDocTypeId(Long.valueOf(ExcelUtil.getCellValue(row.getCell(2))));
            } catch (Exception e) {
                throw new ImportException("模板sheet第" + (i + 1) + "行，档案类型不可为空，且需为正整数");
            }
            String paramName = ExcelUtil.getCellValue(row.getCell(3));
            if (!CommonUtil.isNotNull(paramName)) {
                throw new ImportException("模板sheet第" + (i + 1) + "行，参数名称不可为空！");
            }
            templateItem.setParamName(paramName);
            String paramCode = ExcelUtil.getCellValue(row.getCell(4));
            if (!CommonUtil.isNotNull(paramCode)) {
                throw new ImportException("模板sheet第" + (i + 1) + "行，参数编码不可为空！");
            }
            templateItem.setParamCode(paramCode);

            try {
                templateItem.setParamType(Short.valueOf(ExcelUtil.getCellValue(row.getCell(5))));
            } catch (Exception e) {
                throw new ImportException("模板sheet第" + (i + 1) + "行，字段类型不可为空，且需为正整数");
            }

            try {
                templateItem.setParamLen(Integer.valueOf(ExcelUtil.getCellValue(row.getCell(6))));
            } catch (Exception e) {
                throw new ImportException("模板sheet第" + (i + 1) + "行，参数长度不可为空，且需为正整数");
            }
            try {
                templateItem.setDecimalLen(Short.valueOf(ExcelUtil.getCellValue(row.getCell(7))));
            } catch (Exception e) {
                throw new ImportException("模板sheet第" + (i + 1) + "行，小数位长度不可为空，且需为正整数");
            }
            //  "print", "notNull", "seq", "templateType"
            try {
                templateItem.setPrint(Short.valueOf(ExcelUtil.getCellValue(row.getCell(8))));
            } catch (Exception e) {
                throw new ImportException("模板sheet第" + (i + 1) + "行，是否打印不可为空，且需为整数");
            }
            try {
                templateItem.setNotNull(Short.valueOf(ExcelUtil.getCellValue(row.getCell(9))));
            } catch (Exception e) {
                throw new ImportException("模板sheet第" + (i + 1) + "行，是否可空不可为空，且需为整数");
            }
            try {
                templateItem.setSeq(Integer.valueOf(ExcelUtil.getCellValue(row.getCell(10))));
            } catch (Exception e) {
                throw new ImportException("模板sheet第" + (i + 1) + "行，排序不可为空，且需为正整数");
            }
            try {
                templateItem.setTemplateType(Short.valueOf(ExcelUtil.getCellValue(row.getCell(11))));
            } catch (Exception e) {
                throw new ImportException("模板sheet第" + (i + 1) + "行，模板类型不可为空，且需为正整数");
            }
            try {
                String val = ExcelUtil.getCellValue(row.getCell(12));
                templateItem.setNeedShow(Short.valueOf(val));
            } catch (Exception e) {
                throw new ImportException("模板sheet第" + (i + 1) + "行，是否可见不可为空，且需为正整数");
            }
            templateItem.setDataType(type);
            templateItemList.add(templateItem);
        }
        if (!CommonUtil.isListNotEmpty(templateItemList)) {
            return templateItemList;
        }
        //档案类型map
        Map<Long, DocType> docTypeMap = new HashMap<Long, DocType>();
        for (DocType docType : docTypeList) {
            docTypeMap.put(docType.getId(), docType);
        }
        //目录map
        Map<Long, DocCatalog> docCatalogMap = new HashMap<Long, DocCatalog>();
        for (DocCatalog catalog : catalogList) {
            docCatalogMap.put(catalog.getId(), catalog);
        }
        //校验模板的档案类型，目录必须存在
        for (TemplateItem item : templateItemList) {
            if (!docTypeMap.containsKey(item.getDocTypeId())) {
                throw new ImportException("模板sheet数据非法！字段名称：" + item.getParamName() + "所属的档案类型在档案类型sheet页中不存在！");
            }
            if (!docCatalogMap.containsKey(item.getCatalogId())) {
                throw new ImportException("模板sheet数据非法！字段名称：" + item.getParamName() + "所属的目录在目录sheet页中不存在！");
            }
        }
        validTemplateUniqueCode(catalogList, docTypeList, templateItemList, type);
        validTemplateUniqueName(catalogList, docTypeList, templateItemList, type);
        Collections.sort(templateItemList, new Comparator<TemplateItem>() {
            @Override
            public int compare(TemplateItem o1, TemplateItem o2) {
                if (o1.getDocTypeId().longValue() == o2.getDocTypeId().longValue()) {
                    return o1.getCatalogId().compareTo(o2.getCatalogId());
                }
                return o1.getDocTypeId().compareTo(o2.getDocTypeId());
            }
        });
        return templateItemList;
    }

    /**
     * 校验模板项的name、code（同个目录+档案类型下的code、name需唯一）
     *
     * @param catalogList
     * @param docTypeList
     * @param templateItemList
     * @param type
     * @throws ImportException
     */
    private static void validTemplateUniqueCode(List<DocCatalog> catalogList, List<DocType> docTypeList, List<TemplateItem> templateItemList, short type) throws ImportException {
        Map<String, Map<String, TemplateItem>> templateMap = new HashMap<String, Map<String, TemplateItem>>();
        for (TemplateItem item : templateItemList) {
            String key = item.getCatalogId() + "_" + item.getDocTypeId();
            Map<String, TemplateItem> subMap = new HashMap<String, TemplateItem>();
            if (templateMap.containsKey(key)) {
                subMap = templateMap.get(key);
            }
            if (!subMap.containsKey(item.getParamCode())) {
                subMap.put(item.getParamCode(), item);

                templateMap.put(key, subMap);
            } else {
                throw new ImportException("模板sheet数据非法！catalogId " + item.getCatalogId() + "下存在相同的模板项编码：" + item.getParamCode());
            }
        }
    }

    private static void validTemplateUniqueName(List<DocCatalog> catalogList, List<DocType> docTypeList, List<TemplateItem> templateItemList, short type) throws ImportException {
        Map<String, Map<String, TemplateItem>> templateMap = new HashMap<String, Map<String, TemplateItem>>();
        for (TemplateItem item : templateItemList) {
            String key = item.getCatalogId() + "_" + item.getDocTypeId();
            Map<String, TemplateItem> subMap = new HashMap<String, TemplateItem>();
            if (templateMap.containsKey(key)) {
                subMap = templateMap.get(key);
            }
            if (!subMap.containsKey(item.getParamName())) {
                subMap.put(item.getParamName(), item);

                templateMap.put(key, subMap);
            } else {
                throw new ImportException("模板sheet数据非法！catalogId " + item.getCatalogId() + "下存在相同的模板项名称：" + item.getParamName());
            }
        }
    }

    /**
     * 验证模板项约束
     *
     * @param newList
     * @throws ImportException
     */
    public static void validTemplateItemConstraint(List<TemplateItem> newList, List<DocCatalog> catalogList, List<DocType> docTypeList, short type) throws ImportException {
        //将模板数据按照catalogId 、docTypeId分类
        Map<String, Map<String, TemplateItem>> templateMap = new HashMap<String, Map<String, TemplateItem>>();
        for (TemplateItem item : newList) {
            String key = item.getCatalogId() + "_" + item.getDocTypeId();
            Map<String, TemplateItem> subMap = new HashMap<String, TemplateItem>();
            if (templateMap.containsKey(key)) {
                subMap = templateMap.get(key);
            }
            subMap.put(item.getParamCode(), item);
            templateMap.put(key, subMap);
        }

        //目录及档案类型map
        Map<Long, DocCatalog> catalogMap = new HashMap<Long, DocCatalog>();
        for (DocCatalog catalog : catalogList) {
            catalogMap.put(catalog.getOldId(), catalog);
        }
        Map<Long, DocType> docTypeMap = new HashMap<Long, DocType>();
        for (DocType docType : docTypeList) {
            docTypeMap.put(docType.getOldId(), docType);
        }


        //验证每一组的数据是否在数据库中已有，如有，则必须完全一致，否则校验失败
        for (String key : templateMap.keySet()) {
            String[] arr = key.split("_");
            //此处必存在，因为parse时已做了校验
            Long newCatalogId = catalogMap.get(Long.valueOf(arr[0])).getNewId();
            Long newDocTypeId = docTypeMap.get(Long.valueOf(arr[1])).getNewId();
            if (null == newCatalogId || null == newDocTypeId || newCatalogId < 0 || newDocTypeId < 0) {
                continue;
            }
            TemplateItem queryTemplate = new TemplateItem();
            queryTemplate.setCatalogId(newCatalogId);
            queryTemplate.setDocTypeId(newDocTypeId);
            queryTemplate.setDataType(type);
            List<TemplateItem> existsList = templateItemService.findList(queryTemplate);
            if (!CommonUtil.isListNotEmpty(existsList)) {
                continue;
            }
            //校验导入的模板需与必须完全一致
            Map<String, TemplateItem> subMap = templateMap.get(key);
            if (subMap.size() != existsList.size()) {
                throw new ImportException("模板sheet数据非法！目录-档案类型组合：" + key + "下的模板与系统原有模板个数不一致，无法导入！");
            }
            for (TemplateItem existsItem : existsList) {
                if (!subMap.containsKey(existsItem.getParamCode())) {
                    throw new ImportException("模板sheet数据非法！目录-档案类型组合：" + key + "下的模板与系统原有模板不一致，无法导入！");
                }
                TemplateItem newItem = subMap.get(existsItem.getParamCode());
                if (!newItem.getParamName().equals(existsItem.getParamName())) {
                    throw new ImportException("模板sheet数据非法！目录-档案类型组合：" + key + "下的模板与系统原有模板不一致，无法导入！");
                }
            }
            //校验通过，赋值id
            for (TemplateItem newItem : subMap.values()) {
                newItem.setNewCatalogId(newCatalogId);
                newItem.setNewDocTypeId(newDocTypeId);
            }

        }

    }

    /**
     * 校验档案数据
     *
     * @param docList
     * @param templateItemList
     * @param catalogList
     * @param docTypeList
     * @throws ImportException
     */
    public static void validArchiveDocDataConstraint(Map<String, List<Map<String, Object>>> docListMap, List<TemplateItem> templateItemList, List<DocCatalog> catalogList, List<DocType> docTypeList) throws ImportException {
        //TODO 无需校验，因为前一步已完成所有校验
    }


    /**
     * 解析预归档离线填写档案数据
     *
     * @param sheet
     * @param docTypeId
     * @param catalogId
     * @param topOrgId
     * @param yearId
     * @param templateItemList
     * @return
     * @throws ImportException
     */
    public static List<Map<String, Object>> parsePreArchiveDocMap(Sheet sheet, long docTypeId, long catalogId, long topOrgId, long yearId, List<TemplateItem> templateItemList) throws ImportException {

        Map<String, TemplateItem> templateItemMap = new HashMap<String, TemplateItem>();
        templateItemList.addAll(BizConstants.DEF_TI_USER_WRITE_LIST);//添加系统字段字段
        templateItemList.addAll(BizConstants.DEF_TI_SYS_LIST);

        for (TemplateItem item : templateItemList) {
            templateItemMap.put(item.getParamName(), item);
        }
        List<Map<String, Object>> docMapList = new ArrayList<Map<String, Object>>();
        //解析头部信息
        Map<Integer, TemplateItem> templateIndexMap = new HashMap<Integer, TemplateItem>();//表格位置与模板对应的关系

        Row rowHeader = sheet.getRow(0);
        for (int i = 0; i < rowHeader.getLastCellNum(); i++) {
            String cellName = ExcelUtil.getCellValue(rowHeader.getCell(i));
            if (!templateItemMap.containsKey(cellName)) {
                throw new ImportException("字段：【" + cellName + "】非法！该字段在模板中不存在！");
            }
            templateIndexMap.put(i, templateItemMap.get(cellName));
        }
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            Map<String, Object> doc = new HashMap<String, Object>();
            doc.put("catalogId", catalogId);
            doc.put("topOrgId", topOrgId);
            doc.put("yearId", yearId);


            for (int j = 0; j < row.getLastCellNum(); j++) {
                TemplateItem item = templateIndexMap.get(j);
                if (item.getParamType() != BizConstants.FIELD_TYPE_DATE && item.getParamType() != BizConstants.FIELD_TYPE_DATETIME) {
                    row.getCell(j).setCellType(Cell.CELL_TYPE_STRING);//强制设置为string
                }
                String cellName = ExcelUtil.getCellValue(row.getCell(j));
                //如果列头是列名，则特殊处理
                if (item.getParamName().equals("机构")) {
                    //判断在该年度下是否存在该机构
                    DocCatalog fourthCatalog = getCatalogByName(yearId, cellName);
                    if (null == fourthCatalog) {
                        throw new ImportException("第" + (i + 1) + "行，第" + (j + 1) + "列[" + item.getParamName() + "]值" + cellName + "在档案门类中不存在！");
                    }
                    doc.put("subOrgId", fourthCatalog.getId());
                    doc.put("subOrgName", cellName);
                } else {
                    validAndSetDocField(doc, item, cellName, i, j);
                }
            }
            CommonUtil.setDocDefaultVal(doc);
            doc.put("recordType", BizConstants.COMMON_TYPE_PRE);
            doc.put("docTypeId", docTypeId);

            docMapList.add(doc);
        }
        return docMapList;
    }

    /**
     * 解析预归档文件excel导入档案管理
     *
     * @param sheet
     * @param templateItemList
     * @param catalogList
     * @param docTypeList
     * @return
     * @throws ImportException
     */
    public static List<Map<String, Object>> parseArchiveDocMap(Sheet sheet, List<TemplateItem> templateItemList, List<DocCatalog> catalogList, long catalogId, long docTypeId) throws ImportException {

        Map<String, TemplateItem> templateItemMap = new HashMap<String, TemplateItem>();
        templateItemList.addAll(BizConstants.DEF_TI_USER_WRITE_LIST);//添加系统字段字段
        templateItemList.addAll(BizConstants.DEF_TI_SYS_LIST);
        for (TemplateItem item : templateItemList) {
            templateItemMap.put(item.getParamName(), item);
        }

        List<Map<String, Object>> docMapList = new ArrayList<Map<String, Object>>();
        //解析头部信息
        Map<Integer, TemplateItem> templateIndexMap = new HashMap<Integer, TemplateItem>();//表格位置与模板对应的关系

        Row rowHeader = sheet.getRow(0);
        for (int i = 0; i < rowHeader.getLastCellNum(); i++) {
            String cellName = ExcelUtil.getCellValue(rowHeader.getCell(i));
            if (!templateItemMap.containsKey(cellName)) {
                throw new ImportException("字段：【" + cellName + "】非法！该字段在模板中不存在！");
            }
            templateIndexMap.put(i, templateItemMap.get(cellName));
        }
        //本catalogId的所有子目录列表.key为 level_catalogName
        Map<Long, DocCatalog> catalogMap = genArchiveImportCatalogMap(catalogList, catalogId);

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            Map<String, Object> doc = new HashMap<String, Object>();
            doc.put("catalogId", catalogId);
            doc.put("docTypeId", docTypeId);
            doc.put("recordType", BizConstants.COMMON_TYPE_ARCHIVE);
            for (int j = 0; j < row.getLastCellNum(); j++) {
                TemplateItem item = templateIndexMap.get(j);
                if (item.getParamType() != BizConstants.FIELD_TYPE_DATE && item.getParamType() != BizConstants.FIELD_TYPE_DATETIME) {
                    row.getCell(j).setCellType(Cell.CELL_TYPE_STRING);//强制设置为string
                }
                String cellName = ExcelUtil.getCellValue(row.getCell(j));
                //如果列头是全宗单位、年度、机构
                if (item.getParamName().equals("全宗组织ID")) {
                    //判断在该年度下是否存在该机构
                    DocCatalog secondCatalog = catalogMap.get(Long.valueOf(cellName));
                    if (null == secondCatalog) {
                        throw new ImportException("第" + (i + 1) + "行，第" + (j + 1) + "列[" + item.getParamName() + "]值" + cellName + "在档案门类中不存在！");
                    }
                    doc.put("topOrgId", secondCatalog.getId());
                    doc.put("topOrgName", cellName);
                } else if (item.getParamName().equals("年度ID")) {
                    //判断在该年度下是否存在该机构
                    DocCatalog thirdCatalog = catalogMap.get(Long.valueOf(cellName));
                    if (null == thirdCatalog) {
                        throw new ImportException("第" + (i + 1) + "行，第" + (j + 1) + "列[" + item.getParamName() + "]值" + cellName + "在档案门类中不存在！");
                    }
                    doc.put("yearId", thirdCatalog.getId());
                    doc.put("yearName", cellName);
                } else if (item.getParamName().equals("机构")) {
                    //判断在该年度下是否存在该机构
                    DocCatalog fourthCatalog = catalogMap.get(Long.valueOf(cellName));
                    if (null == fourthCatalog) {
                        throw new ImportException("第" + (i + 1) + "行，第" + (j + 1) + "列[" + item.getParamName() + "]值" + cellName + "在档案门类中不存在！");
                    }
                    doc.put("subOrgId", fourthCatalog.getId());
                    doc.put("subOrgName", cellName);
                } else {
                    validAndSetDocField(doc, item, cellName, i, j);
                }
            }
            CommonUtil.setDocDefaultVal(doc);
            docMapList.add(doc);
        }
        return docMapList;
    }

    /**
     * 构造档案管理目录列表map
     *
     * @param docCatalogList
     * @return
     */
    private static Map<Long, DocCatalog> genArchiveImportCatalogMap(List<DocCatalog> docCatalogList, long catalogId) {
        Map<Long, DocCatalog> catalogMap = new HashMap<Long, DocCatalog>();

        DocCatalog firstCatalog = null;
        for (DocCatalog catalog : docCatalogList) {
            if (catalogId == catalog.getId().longValue()) {
                firstCatalog = catalog;
                break;
            }
        }
        catalogMap.put(firstCatalog.getId(), firstCatalog);

        for (DocCatalog catalog : docCatalogList) {//2级
            if (catalog.getParentId().longValue() == firstCatalog.getId() && catalog.getLevel() == 2) {
                catalogMap.put(catalog.getId(), catalog);

            }
        }

        for (DocCatalog catalog : docCatalogList) {//3级
            if (catalogMap.containsKey(catalog.getParentId()) && catalog.getLevel() == 3) {
                catalogMap.put(catalog.getId(), catalog);

            }
        }
        for (DocCatalog catalog : docCatalogList) {//4级
            if (catalogMap.containsKey(catalog.getParentId()) && catalog.getLevel() == 4) {
                catalogMap.put(catalog.getId(), catalog);
            }
        }
        return catalogMap;
    }

    /**
     * 根据名称，以及父id获取档案目录
     *
     * @param parentCatalogId
     * @param name
     * @return
     */
    private static DocCatalog getCatalogByName(long parentCatalogId, String name) {
        List<DocCatalog> subList = DocCatalogCache.getSubList(parentCatalogId);
        if (!CommonUtil.isListNotEmpty(subList)) {
            return null;
        }
        for (DocCatalog docCatalog : subList) {
            if (docCatalog.getName().equals(name)) {
                return docCatalog;
            }
        }
        return null;
    }

    /**
     * 校验字段值
     *
     * @param item
     * @param cellVal
     * @param rowIndex
     * @param colIndex
     */
    private static void validAndSetDocField(Map<String, Object> doc, TemplateItem item, String cellVal, int rowIndex, int colIndex) throws ImportException {

        if (!CommonUtil.isNotNull(cellVal)) {
            if (item.getNotNull().shortValue() == BizConstants.NOT_NULL) {
                throw new ImportException("第" + (rowIndex + 1) + "行，第" + (colIndex + 1) + "列[" + item.getParamName() + "]不可为空！");
            } else {
                return;
            }
        }
        //不为空，需校验
        if (item.getParamType().shortValue() == BizConstants.FIELD_TYPE_STRING) {
            if (cellVal.length() > item.getParamLen()) {
                throw new ImportException("第" + (rowIndex + 1) + "行，第" + (colIndex + 1) + "列[" + item.getParamName() + "]长度不能超过" + item.getParamLen() + "！");
            }
            doc.put(item.getParamCode(), cellVal);

        } else if (item.getParamType().shortValue() == BizConstants.FIELD_TYPE_LONG) {
            try {
                Long val = Long.valueOf(cellVal);
                if (cellVal.length() > item.getParamLen()) {
                    throw new ImportException("第" + (rowIndex + 1) + "行，第" + (colIndex + 1) + "列[" + item.getParamName() + "]长度不能超过" + item.getParamLen() + "！");
                }
                doc.put(item.getParamCode(), val);
            } catch (Exception e) {
                if (e instanceof ImportException) {
                    throw e;
                }
                throw new ImportException("第" + (rowIndex + 1) + "行，第" + (colIndex + 1) + "列[" + item.getParamName() + "]必须为整数！");
            }
        } else if (item.getParamType().shortValue() == BizConstants.FIELD_TYPE_FLOAT) {
            try {
                Float val = Float.valueOf(cellVal);
                if (cellVal.length() > item.getParamLen()) {
                    throw new ImportException("第" + (rowIndex + 1) + "行，第" + (colIndex + 1) + "列[" + item.getParamName() + "]长度不能超过" + item.getParamLen() + "！");
                }
                int index = cellVal.indexOf(".");
                if (index != -1) {
                    int decimalLen = cellVal.length() - index - 1;
                    if (decimalLen > item.getDecimalLen()) {
                        throw new ImportException("第" + (rowIndex + 1) + "行，第" + (colIndex + 1) + "列[" + item.getParamName() + "]小数位不能超过" + item.getDecimalLen() + "！");
                    }
                }
                doc.put(item.getParamCode(), val);

            } catch (Exception e) {
                if (e instanceof ImportException) {
                    throw e;
                }
                throw new ImportException("第" + (rowIndex + 1) + "行，第" + (colIndex + 1) + "列[" + item.getParamName() + "]必须为小数！");

            }
        } else if (item.getParamType().shortValue() == BizConstants.FIELD_TYPE_DATE) {
            Date date = DateUtil.parseDate(cellVal, DateUtil.FORMAT_DATE);
            if (null == date) {
                throw new ImportException("第" + (rowIndex + 1) + "行，第" + (colIndex + 1) + "列[" + item.getParamName() + "]日期有误！");
            }
            doc.put(item.getParamCode(), DateUtil.formatDate(date, DateUtil.FORMAT_DATE));

        } else {
            Date date = DateUtil.parseDate(cellVal, DateUtil.FORMAT_DATE_TIME);
            if (null == date) {
                throw new ImportException("第" + (rowIndex + 1) + "行，第" + (colIndex + 1) + "列[" + item.getParamName() + "]日期时间格式有误！");
            }
            doc.put(item.getParamCode(), DateUtil.formatDate(date, DateUtil.FORMAT_DATE_TIME));
        }
    }

    public static void main(String[] args) {
        String str = "-11223.345";
        int index = str.indexOf(".");
        System.out.println("index:" + index);
        if (index != -1) {
            int decimalLen = str.length() - index - 1;
            System.out.println("decimalLen:" + decimalLen);
        }
    }


    /**
     * 解析档案数据
     *
     * @param sheet
     * @return
     * @throws ImportException
     */
    public static List<Map<String, Object>> parseDocMap(Sheet sheet, List<TemplateItem> templateItemList) throws ImportException {
        if (null == sheet) {
            throw new ImportException("档案sheet页不存在！");
        }
        List<Map<String, Object>> docMapList = new ArrayList<Map<String, Object>>();
        for (short i = 1; i <= sheet.getLastRowNum(); i++) {
            TemplateItem templateItem = new TemplateItem();
            Row row = sheet.getRow(i);
            String jsonStr = ExcelUtil.getCellValue(row.getCell(0));
            if (!CommonUtil.isNotNull(jsonStr)) {
                throw new ImportException("档案sheet第" + (i + 1) + "行，档案数据不可为空！");
            }
            Map<String, Object> docMap = JSON.parseObject(jsonStr, Map.class);

            docMapList.add(docMap);
        }
        if (!CommonUtil.isListNotEmpty(docMapList)) {
            return docMapList;
        }
        //模板map，key为catalog + "_" + docTypeId,value为该类型的模板列表
        Map<String, Map<String, TemplateItem>> templateMap = new HashMap<String, Map<String, TemplateItem>>();
        for (TemplateItem item : templateItemList) {
            String key = item.getCatalogId() + "_" + item.getDocTypeId();
            Map<String, TemplateItem> subMap = new HashMap<String, TemplateItem>();
            if (templateMap.containsKey(key)) {
                subMap = templateMap.get(key);
            }
            subMap.put(item.getParamCode(), item);
            templateMap.put(key, subMap);
        }
        //校验每一条档案数据的对应的模板项必须存在，且必填的字段都需存在，且字段类型能对应上
        for (Map<String, Object> docMap : docMapList) {
            String docNo = "";
            if (!CommonUtil.isNotNull(docMap, "docNo")) {
                throw new ImportException("档案sheet数据非法！编号不可为空！");
            }
            docNo = docMap.get("docNo").toString();
            validDocLongValue(docMap, docNo, "id");
            validDocCommonValue(docMap, docNo, "title");
            validDocLongValue(docMap, docNo, "catalogId");
            validDocLongValue(docMap, docNo, "topOrgId");
            validDocLongValue(docMap, docNo, "docTypeId");
            validDocCommonValue(docMap, docNo, "createTime");
            validDocCommonValue(docMap, docNo, "secretLevel");
            validDocCommonValue(docMap, docNo, "savePeriod");
            validDocLongValue(docMap, docNo, "dataPageCount");
            validDocCommonValue(docMap, docNo, "subjectWord");
            validDocLongValue(docMap, docNo, "isOpen");
            validDocLongValue(docMap, docNo, "isDestroy");
            //校验对应的catalogId docTypeId存在
            String key = docMap.get("catalogId") + "_" + docMap.get("docTypeId");
            if (!templateMap.containsKey(key)) {
                throw new ImportException("档案sheet数据非法！编号：" + docNo + "所属的目录/档案类型组合在模板sheet页中未找到！");
            }
            //取出模板，并校验所有必填的均有填写
            Map<String, TemplateItem> subMap = templateMap.get(key);
            for (String code : subMap.keySet()) {
                validDocCommonValue(docMap, docNo, code);
            }
        }


        return docMapList;
    }

    private static void validDocCommonValue(Map<String, Object> docMap, String docNo, String key) throws ImportException {
        if (!CommonUtil.isNotNull(docMap, key)) {
            throw new ImportException("档案sheet数据非法！，编号：" + docNo + "对应的" + key + "不可为空！");
        }
    }

    private static void validDocLongValue(Map<String, Object> docMap, String docNo, String key) throws ImportException {
        validDocCommonValue(docMap, docNo, key);
        try {
            Long.valueOf(docMap.get(key) + "");
        } catch (Exception e) {
            throw new ImportException("档案sheet数据非法！，编号：" + docNo + "对应的" + key + "必须为整数！");
        }
    }

    private static void validDocFloatValue(Map<String, Object> docMap, String docNo, String key) throws ImportException {
        validDocCommonValue(docMap, docNo, key);
        try {
            Float.valueOf(docMap.get(key) + "");
        } catch (Exception e) {
            throw new ImportException("档案sheet数据非法！，编号：" + docNo + "对应的" + key + "必须为数字！");
        }
    }

    /******************************************************************************************************/
    /*****************************************解析sheet end***********************************************/
    /******************************************************************************************************/


    /******************************************************************************************************/
    /*****************************************settter/getter***********************************************/
    /******************************************************************************************************/

    @Autowired
    public void setOrgService(OrgService orgService) {
        ArchiveImportExportUtil.orgService = orgService;
    }

    @Autowired
    public void setDocTypeService(DocTypeService docTypeService) {
        this.docTypeService = docTypeService;
    }

    @Autowired
    public void setTemplateItemService(TemplateItemService templateItemService) {
        ArchiveImportExportUtil.templateItemService = templateItemService;
    }

    @Autowired
    public void setDocCatalogService(DocCatalogService docCatalogService) {
        ArchiveImportExportUtil.docCatalogService = docCatalogService;
    }

    public static DocCatalogService getDocCatalogService() {
        return docCatalogService;
    }

    public static OrgService getOrgService() {
        return orgService;
    }

    public static DocTypeService getDocTypeService() {
        return docTypeService;
    }

    public static TemplateItemService getTemplateItemService() {
        return templateItemService;
    }


}
