
package com.cn.text;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;

/**
 * 示例demo
 */
public class ExportWord {
    public static void main(String[] args) {
        try {
            getWord();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void getWord() throws Exception {
        XWPFDocument xdoc = new XWPFDocument();
        //标题
        XWPFParagraph titleMes = xdoc.createParagraph();
        titleMes.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r1 = titleMes.createRun();
        r1.setBold(true);
        r1.setFontFamily("微软雅黑");
        r1.setText("种类报告");//活动名称
        r1.setFontSize(20);
        r1.setColor("333333");
        r1.setBold(true);

        //表格
        XWPFTable xTable = xdoc.createTable(10, 5);

        createSimpleTable(xTable, xdoc);
        //            setEmptyRow(xdoc, r1);
        // 在服务器端生成
        FileOutputStream fos = new FileOutputStream("E:/种类报告.docx");
        xdoc.write(fos);
        fos.close();
    }


    //设置表格高度
    private static XWPFTableCell getCellHight(XWPFTable xTable, int rowNomber, int cellNumber) {
        XWPFTableRow row = null;
        row = xTable.getRow(rowNomber);
        row.setHeight(100);
        XWPFTableCell cell = null;
        cell = row.getCell(cellNumber);
        return cell;
    }

    /**
     * @param xDocument
     * @param cell
     * @param text
     * @param bgcolor
     * @param width
     */
    private static void setCellText(XWPFDocument xDocument, XWPFTableCell cell,
                                    String text) {
        CTP ctp = CTP.Factory.newInstance();
        XWPFParagraph p = new XWPFParagraph(ctp, cell);
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setColor("000000");
        run.setFontSize(10);
        run.setText(text);
        CTRPr rpr = run.getCTR().isSetRPr() ? run.getCTR().getRPr() : run.getCTR().addNewRPr();
        CTFonts fonts = rpr.isSetRFonts() ? rpr.getRFonts() : rpr.addNewRFonts();
        fonts.setAscii("微软雅黑");
        fonts.setEastAsia("微软雅黑");
        fonts.setHAnsi("微软雅黑");
        cell.setParagraph(p);
    }

    //设置表格间的空行
    public static void setEmptyRow(XWPFDocument xdoc, XWPFRun r1) {
        XWPFParagraph p1 = xdoc.createParagraph();
        p1.setAlignment(ParagraphAlignment.CENTER);
        p1.setVerticalAlignment(TextAlignment.CENTER);
        r1 = p1.createRun();
    }

    /**
     * 创建计划明细表
     *
     * @param task
     * @param xTable
     * @param xdoc
     * @throws IOException
     */
    public static void createSimpleTable(XWPFTable xTable, XWPFDocument xdoc)
            throws IOException {
        String bgColor = "FFFFFF";
        CTTbl ttbl = xTable.getCTTbl();
        CTTblPr tblPr = ttbl.getTblPr() == null ? ttbl.addNewTblPr() : ttbl.getTblPr();
        CTTblWidth tblWidth = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();
        tblWidth.setW(new BigInteger("9600"));
        tblWidth.setType(STTblWidth.DXA);
        mergeCellHorizontally(xTable, 0, 0, 1);
        setCellText(xdoc, getCellHight(xTable, 0, 0), "种类");
        setCellText(xdoc, getCellHight(xTable, 0, 2), "总分");
        setCellText(xdoc, getCellHight(xTable, 0, 3), "实得");
        setCellText(xdoc, getCellHight(xTable, 0, 4), "备注");
        for (int i = 1; i < 8; i++) {
            setCellText(xdoc, getCellHight(xTable, i, 0), "种类" + i);
            setCellText(xdoc, getCellHight(xTable, i, 1), "种类细则" + i);
            setCellText(xdoc, getCellHight(xTable, i, 2), "" + i);
            setCellText(xdoc, getCellHight(xTable, i, 3), "" + i);
            setCellText(xdoc, getCellHight(xTable, i, 4), "三度空间阿萨德妇女节开发四季的风");
        }
        mergeCellHorizontally(xTable, 8, 0, 3);
        mergeCellHorizontally(xTable, 8, 3, 4);
        mergeCellHorizontally(xTable, 9, 1, 4);
        setCellText(xdoc, getCellHight(xTable, 8, 0), "合计");
        setCellText(xdoc, getCellHight(xTable, 9, 0), "总结");

        setCellText(xdoc, getCellHight(xTable, 8, 3), "50");
        setCellText(xdoc, getCellHight(xTable, 9, 1), "总结");

    }

    //列合并
    static void mergeCellVertically(XWPFTable table, int col, int fromRow, int toRow) {
        for (int rowIndex = fromRow; rowIndex <= toRow; rowIndex++) {
            CTVMerge vmerge = CTVMerge.Factory.newInstance();
            if (rowIndex == fromRow) {
                // The first merged cell is set with RESTART merge value
                vmerge.setVal(STMerge.RESTART);
            } else {
                // Cells which join (merge) the first one, are set with CONTINUE
                vmerge.setVal(STMerge.CONTINUE);
            }
            XWPFTableCell cell = table.getRow(rowIndex).getCell(col);
            // Try getting the TcPr. Not simply setting an new one every time.
            CTTcPr tcPr = cell.getCTTc().getTcPr();
            if (tcPr != null) {
                tcPr.setVMerge(vmerge);
            } else {
                // only set an new TcPr if there is not one already
                tcPr = CTTcPr.Factory.newInstance();
                tcPr.setVMerge(vmerge);
                cell.getCTTc().setTcPr(tcPr);
            }
        }
    }

    //行合并
    static void mergeCellHorizontally(XWPFTable table, int row, int fromCol, int toCol) {
        for (int colIndex = fromCol; colIndex <= toCol; colIndex++) {
            CTHMerge hmerge = CTHMerge.Factory.newInstance();
            if (colIndex == fromCol) {
                // The first merged cell is set with RESTART merge value
                hmerge.setVal(STMerge.RESTART);
            } else {
                // Cells which join (merge) the first one, are set with CONTINUE
                hmerge.setVal(STMerge.CONTINUE);
            }
            XWPFTableCell cell = table.getRow(row).getCell(colIndex);
            // Try getting the TcPr. Not simply setting an new one every time.
            CTTcPr tcPr = cell.getCTTc().getTcPr();
            if (tcPr != null) {
                tcPr.setHMerge(hmerge);
            } else {
                // only set an new TcPr if there is not one already
                tcPr = CTTcPr.Factory.newInstance();
                tcPr.setHMerge(hmerge);
                cell.getCTTc().setTcPr(tcPr);
            }
        }
    }
}

