package com.hit.fm.common.util;

import com.hit.fm.common.constants.BizConstants;
import org.apache.log4j.Logger;
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件名：CreateWordUtil.java
 * 说明：创建word文档帮助类
 * 作者：陈江海
 * 创建时间：2018/3/8
 * 版权所有：泉州哈工大工程技术研究院
 */
public class CreateWordUtil {
    private static Logger logger = Logger.getLogger(CreateWordUtil.class);

    public static void main(String[] args) throws Exception {
//        main22(args);
        System.out.println("开始创建word");
        List<String> keyList = new ArrayList<String>() {{
            add("标题");
            add("档案门类");
            add("全宗单位");
        }};
        List<Map<String, Object>> valList = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < 101; i++) {
            Map<String, Object> docMap = new HashMap<String, Object>();
            docMap.put("标题", "我是标题啊我是标题啊我是标题啊我是标题啊我是标题啊我是标题啊我是标题啊：" + i);
            docMap.put("档案门类", "文书档案我是标题啊：" + i);
            docMap.put("全宗单位", "测试门类我是标题啊我是标题啊：" + i);
            valList.add(docMap);
        }
        createWord("E:/word", "档案", keyList, valList, BizConstants.WORD_PAGE_SIZE);
        System.out.println("word创建成功！");
    }

    /**
     * 创建word文档并返回存储路径
     *
     * @param basePath
     * @param docTypeName
     * @param keyList
     * @param valList
     * @param pageSize
     * @return
     * @throws Exception
     */
    public static String createWord(String basePath, String docTypeName, List<String> keyList, List<Map<String, Object>> valList, int pageSize) throws Exception {

        String savePath = basePath + File.separator + docTypeName + ".doc";
        XWPFDocument document = new XWPFDocument();
        FileOutputStream out = new FileOutputStream(new File(savePath));
        try {
            //计算页数
            int totalCount = valList.size() / pageSize;
            int mod = valList.size() % pageSize;
            if (mod > 0) {
                totalCount++;
            }
            //循环创建分页
            for (int i = 0; i < totalCount; i++) {
                //添加标题
                addParagraph(document, ParagraphAlignment.CENTER, setTitleName(docTypeName), 20, "000000", false);
                //第几页
                addParagraph(document, ParagraphAlignment.RIGHT, "第" + (i + 1) + "页", 10, "000000", false);
                //档案表格
                XWPFTable dataTable = document.createTable();

                //列宽自动分割
                CTTblWidth comTableWidth = dataTable.getCTTbl().addNewTblPr().addNewTblW();
                comTableWidth.setType(STTblWidth.DXA);
                comTableWidth.setW(BigInteger.valueOf(9600));

                //表格第一行（列头）
                XWPFTableRow headerRow = dataTable.getRow(0);
                headerRow.setHeight(500);
                for (int j = 0; j < keyList.size(); j++) {
                    XWPFTableCell cell = null;
                    if (j == 0) {
                        cell = headerRow.getCell(0);
                    } else {
                        cell = headerRow.addNewTableCell();
                    }
                    cell.setText(keyList.get(j));//文本
                    cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER); //垂直居中
                    CTTc cttc = cell.getCTTc();

                    CTP ctp = cttc.getPList().get(0);
                    CTPPr ctppr = ctp.getPPr();
                    if (ctppr == null) {
                        ctppr = ctp.addNewPPr();
                    }

                    CTJc ctjc = ctppr.getJc();
                    if (ctjc == null) {
                        ctjc = ctppr.addNewJc();
                    }
                    ctjc.setVal(STJc.CENTER); //水平居中
                }
                int dataBeginIndex = i * pageSize;
                int dataEndIndex = dataBeginIndex + pageSize;
                boolean isLastPage = false;
                if (dataEndIndex > valList.size()) {
                    dataEndIndex = valList.size();
                    isLastPage = true;
                }
                //数据行
                for (int k = dataBeginIndex; k < dataEndIndex; k++) {
                    Map<String, Object> data = valList.get(k);
                    XWPFTableRow dataRow = dataTable.createRow();
                    dataRow.setHeight(300);
                    for (int j = 0; j < keyList.size(); j++) {
                        String text = data.get(keyList.get(j)) + "";
                        XWPFTableCell cell = dataRow.getCell(j);
//                      cell.setText(text);//文本
                        cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER); //垂直居中
                        setCellText(document, cell, text);
                    }
                }
                //换行
                addLine(document, false);
                //移交人接收人
                addParagraph(document, ParagraphAlignment.RIGHT, getLastRowString(), 10, "000000", false);
                //最后一页，无需幻夜
                if (!isLastPage) {
                    addLine(document, true);
                }
            }
            addPageHeaderAndFooter(document, "智慧档案馆", "智慧档案馆");
            document.write(out);

            return savePath;
        } catch (Exception e) {
            logger.error("创建word失败！" + e.getMessage(), e);
            throw new Exception("创建word失败！" + e.getMessage());
        } finally {
            try {
                out.close();
            } catch (Exception e) {
            }
        }

    }

    private static void setCellText(XWPFDocument xDocument, XWPFTableCell cell, String text) {
        CTP ctp = CTP.Factory.newInstance();
        XWPFParagraph p = new XWPFParagraph(ctp, cell);
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setColor("000000");
        run.setFontSize(14);
        run.setText(text);
        CTRPr rpr = run.getCTR().isSetRPr() ? run.getCTR().getRPr() : run.getCTR().addNewRPr();
        CTFonts fonts = rpr.isSetRFonts() ? rpr.getRFonts() : rpr.addNewRFonts();
        fonts.setAscii("宋体");
        fonts.setEastAsia("宋体");
        fonts.setHAnsi("宋体");
        cell.setParagraph(p);
        cell.getTableRow().setHeight(500);
    }

    /**
     * 添加页眉页脚
     *
     * @param document
     */
    private static void addPageHeaderAndFooter(XWPFDocument document, String headerText, String footerText) {
        CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
        XWPFHeaderFooterPolicy policy = new XWPFHeaderFooterPolicy(document, sectPr);
        if (CommonUtil.isNotNull(headerText)) {
            //添加页眉
            CTP ctpHeader = CTP.Factory.newInstance();
            CTR ctrHeader = ctpHeader.addNewR();
            CTText ctHeader = ctrHeader.addNewT();
            ctHeader.setStringValue(headerText);
            XWPFParagraph headerParagraph = new XWPFParagraph(ctpHeader, document);
            //设置为右对齐
            headerParagraph.setAlignment(ParagraphAlignment.RIGHT);
            XWPFParagraph[] parsHeader = new XWPFParagraph[1];
            parsHeader[0] = headerParagraph;
            policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT, parsHeader);
        }
        if (CommonUtil.isNotNull(footerText)) {
            //添加页脚
            CTP ctpFooter = CTP.Factory.newInstance();
            CTR ctrFooter = ctpFooter.addNewR();
            CTText ctFooter = ctrFooter.addNewT();
            ctFooter.setStringValue(footerText);
            XWPFParagraph footerParagraph = new XWPFParagraph(ctpFooter, document);
            footerParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFParagraph[] parsFooter = new XWPFParagraph[1];
            parsFooter[0] = footerParagraph;
            policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT, parsFooter);
        }


    }

    /**
     * 添加段落
     *
     * @param document：文档
     * @param alignment：对齐方式
     * @param text：文本
     * @param fontSize：字体大小
     * @param color：颜色
     * @param isPageEnd：是否结束当前页
     */
    private static void addParagraph(XWPFDocument document, ParagraphAlignment alignment, String text, int fontSize, String color, boolean isPageEnd) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(alignment);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setColor(color);
        run.setFontSize(fontSize);
        if (isPageEnd) {
            paragraph.setPageBreak(true);//当前页结束
        }
    }

    /**
     * 换行
     *
     * @param document
     * @param isPageEnd：是否结束当前页
     */
    private static void addLine(XWPFDocument document, boolean isPageEnd) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText("\r");
        if (isPageEnd) {
            paragraph.setPageBreak(true);//当前页结束
        }
    }

    private static String getLastRowString() {
        String str = "          移交人";
        for (int i = 0; i < 120; i++) {
            str += " ";
        }
        return str + "接收人";
    }

    private static String setTitleName(String docTpyeName) {
        String title = "";
        for (int i = 0; i < docTpyeName.length(); i++) {
            title += docTpyeName.charAt(i) + "    ";
        }
        return title;
    }

    /**
     * 利用poi手动生成word的各项元素
     *
     * @param args
     * @throws Exception
     */
    public static void main22(String[] args) throws Exception {
        //Blank Document
        XWPFDocument document = new XWPFDocument();
        //Write the Document in file system
        FileOutputStream out = new FileOutputStream(new File("E:/1.doc"));


        //添加标题
        XWPFParagraph titleParagraph = document.createParagraph();
        //设置段落居中
        titleParagraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleParagraphRun = titleParagraph.createRun();
        titleParagraphRun.setText("这个是标题啊");
        titleParagraphRun.setColor("000000");
        titleParagraphRun.setFontSize(20);


        //段落
        XWPFParagraph firstParagraph = document.createParagraph();
        XWPFRun run = firstParagraph.createRun();
        run.setText("Java POI 生成word文件。");
        run.setColor("696969");
        run.setFontSize(16);

        //设置段落背景颜色
        CTShd cTShd = run.getCTR().addNewRPr().addNewShd();
        cTShd.setVal(STShd.CLEAR);
        cTShd.setFill("97FFFF");


        //换行
        XWPFParagraph paragraph1 = document.createParagraph();
        XWPFRun paragraphRun1 = paragraph1.createRun();
        paragraphRun1.setText("\r");


        //基本信息表格
        XWPFTable infoTable = document.createTable();
        //去表格边框
        infoTable.getCTTbl().getTblPr().unsetTblBorders();


        //列宽自动分割
        CTTblWidth infoTableWidth = infoTable.getCTTbl().addNewTblPr().addNewTblW();
        infoTableWidth.setType(STTblWidth.DXA);
        infoTableWidth.setW(BigInteger.valueOf(9072));


        //表格第一行
        XWPFTableRow infoTableRowOne = infoTable.getRow(0);
        infoTableRowOne.getCell(0).setText("职位");
        infoTableRowOne.addNewTableCell().setText(": Java 开发工程师");

        //表格第二行
        XWPFTableRow infoTableRowTwo = infoTable.createRow();
        infoTableRowTwo.getCell(0).setText("姓名");
        infoTableRowTwo.getCell(1).setText(": seawater");

        //表格第三行
        XWPFTableRow infoTableRowThree = infoTable.createRow();
        infoTableRowThree.getCell(0).setText("生日");
        infoTableRowThree.getCell(1).setText(": xxx-xx-xx");

        //表格第四行
        XWPFTableRow infoTableRowFour = infoTable.createRow();
        infoTableRowFour.getCell(0).setText("性别");
        infoTableRowFour.getCell(1).setText(": 男");

        //表格第五行
        XWPFTableRow infoTableRowFive = infoTable.createRow();
        infoTableRowFive.getCell(0).setText("现居地");
        infoTableRowFive.getCell(1).setText(": xx");


        //两个表格之间加个换行
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun paragraphRun = paragraph.createRun();
        paragraphRun.setText("\r");


        //工作经历表格
        XWPFTable ComTable = document.createTable();


        //列宽自动分割
        CTTblWidth comTableWidth = ComTable.getCTTbl().addNewTblPr().addNewTblW();
        comTableWidth.setType(STTblWidth.DXA);
        comTableWidth.setW(BigInteger.valueOf(9072));

        //表格第一行
        XWPFTableRow comTableRowOne = ComTable.getRow(0);
        comTableRowOne.getCell(0).setText("开始时间");
        comTableRowOne.addNewTableCell().setText("结束时间");
        comTableRowOne.addNewTableCell().setText("公司名称");
        comTableRowOne.addNewTableCell().setText("title");

        //表格第二行
        XWPFTableRow comTableRowTwo = ComTable.createRow();
        comTableRowTwo.getCell(0).setText("2016-09-06");
        comTableRowTwo.getCell(1).setText("至今");
        comTableRowTwo.getCell(2).setText("seawater");
        comTableRowTwo.getCell(3).setText("Java开发工程师");

        //表格第三行
        XWPFTableRow comTableRowThree = ComTable.createRow();
        comTableRowThree.getCell(0).setText("2016-09-06");
        comTableRowThree.getCell(1).setText("至今");
        comTableRowThree.getCell(2).setText("seawater");
        comTableRowThree.getCell(3).setText("Java开发工程师");


        CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
        XWPFHeaderFooterPolicy policy = new XWPFHeaderFooterPolicy(document, sectPr);

        //添加页眉
        CTP ctpHeader = CTP.Factory.newInstance();
        CTR ctrHeader = ctpHeader.addNewR();
        CTText ctHeader = ctrHeader.addNewT();
        String headerText = "Java POI create MS word file.";
        ctHeader.setStringValue(headerText);
        XWPFParagraph headerParagraph = new XWPFParagraph(ctpHeader, document);
        //设置为右对齐
        headerParagraph.setAlignment(ParagraphAlignment.RIGHT);
        XWPFParagraph[] parsHeader = new XWPFParagraph[1];
        parsHeader[0] = headerParagraph;
        policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT, parsHeader);


        //添加页脚
        CTP ctpFooter = CTP.Factory.newInstance();
        CTR ctrFooter = ctpFooter.addNewR();
        CTText ctFooter = ctrFooter.addNewT();
        String footerText = "http://blog.csdn.net/zhouseawater";
        ctFooter.setStringValue(footerText);
        XWPFParagraph footerParagraph = new XWPFParagraph(ctpFooter, document);
        headerParagraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFParagraph[] parsFooter = new XWPFParagraph[1];
        parsFooter[0] = footerParagraph;
        policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT, parsFooter);


        document.write(out);
        out.close();
        System.out.println("create_table document written success.");
    }

}
