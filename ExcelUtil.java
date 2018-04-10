package com.hit.fm.common.util;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ExcelUtil {
    public static Workbook createEmptyWorkBook() {
        Workbook wb = new HSSFWorkbook();
        return wb;
    }

    @SuppressWarnings("rawtypes")
    public static Workbook createWorkBook(String sheetName, List dataList, String columnNameArr[], String[] keyArr) {
        Workbook wb = new HSSFWorkbook();
        addSheet(sheetName, wb, dataList, columnNameArr, keyArr);
        return wb;
    }

    /**
     * 创建excel文档
     *
     * @param dataList
     * @param columnNameArr
     * @param keyArr
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static void addSheet(String sheetName, Workbook wb, List dataList, String columnNameArr[], String[] keyArr) {
        // 创建第一个sheet（页），并命名
        Sheet sheet = wb.createSheet(sheetName);
        // 手动设置列宽。第一个参数表示要为第几列设；，第二个参数表示列的宽度，n为列高的像素数。
        for (int i = 0; i < keyArr.length; i++) {
            sheet.setColumnWidth(i, (short) (35.7 * 150));
        }

        // 创建第一行
        Row row = sheet.createRow(0);

        // 创建两种单元格格式
        CellStyle titleCellStyle = wb.createCellStyle();//列名的样式
        CellStyle valueCessStyle = wb.createCellStyle();//值的样式

        // 创建两种字体
        Font f = wb.createFont();
        Font f2 = wb.createFont();

        // 创建第一种字体样式（用于列名）
        f.setFontHeightInPoints((short) 10);
        f.setColor(IndexedColors.BLACK.getIndex());
        f.setBold(true);
        // 创建第二种字体样式（用于值）
        f2.setFontHeightInPoints((short) 10);
        f2.setColor(IndexedColors.BLACK.getIndex());

        // 设置第一种单元格的样式（用于列名）
        titleCellStyle.setFont(f);
        titleCellStyle.setBorderLeft(BorderStyle.THIN);
        titleCellStyle.setBorderRight(BorderStyle.THIN);
        titleCellStyle.setBorderTop(BorderStyle.THIN);
        titleCellStyle.setBorderBottom(BorderStyle.THIN);
        titleCellStyle.setAlignment(HorizontalAlignment.CENTER);

        // 设置第二种单元格的样式（用于值）
        valueCessStyle.setFont(f2);
        valueCessStyle.setBorderLeft(BorderStyle.THIN);
        valueCessStyle.setBorderRight(BorderStyle.THIN);
        valueCessStyle.setBorderTop(BorderStyle.THIN);
        valueCessStyle.setBorderBottom(BorderStyle.THIN);
        valueCessStyle.setAlignment(HorizontalAlignment.CENTER);
        DataFormat format = wb.createDataFormat();
        valueCessStyle.setDataFormat(format.getFormat("@"));//全部设置为文本格式

        // 设置列名
        for (int i = 0; i < columnNameArr.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(columnNameArr[i]);
            cell.setCellStyle(titleCellStyle);
            cell.setCellType(CellType.STRING);
        }
        // 设置每行每列的值
        for (short i = 0; i < dataList.size(); i++) {
            // Row 行,Cell 方格 , Row 和 Cell 都是从0开始计数的
            // 创建一行，在页sheet上
            Row row1 = sheet.createRow((short) (i + 1));
            // 在row行上创建一个方格
            for (short j = 0; j < keyArr.length; j++) {
                Object dataObj = dataList.get(i);
                Object attrVal;
                if (dataObj instanceof Map) {//map的直接取值
                    attrVal = ((Map) dataObj).get(keyArr[j]);
                } else {//对象的，根据反射去取属性
                    attrVal = CommonUtil.getAttrVal(dataObj, keyArr[j]);//取对应的key值
                }
                String cellVal = null != attrVal ? attrVal.toString() : "";
                Cell cell = row1.createCell(j);
                cell.setCellValue(cellVal);
                cell.setCellStyle(valueCessStyle);
//                cell.setCellType(CellType.STRING);

            }
        }
    }

    /**
     * 添加sheet（只包含标题）
     *
     * @param sheetName
     * @param wb
     * @param dataList
     * @param columnNameArr
     * @param keyArr
     */
    public static void addSheetOnlyHeader(String sheetName, Workbook wb, String columnNameArr[]) {
        // 创建第一个sheet（页），并命名
        Sheet sheet = wb.createSheet(sheetName);
        // 手动设置列宽。第一个参数表示要为第几列设；，第二个参数表示列的宽度，n为列高的像素数。
        for (int i = 0; i < columnNameArr.length; i++) {
            sheet.setColumnWidth(i, (short) (35.7 * 150));
        }
        // 创建第一行
        Row row = sheet.createRow(0);

        // 创建两种单元格格式
        CellStyle titleCellStyle = wb.createCellStyle();//列名的样式
        CellStyle valueCessStyle = wb.createCellStyle();//值的样式

        // 创建两种字体
        Font f = wb.createFont();
        Font f2 = wb.createFont();

        // 创建第一种字体样式（用于列名）
        f.setFontHeightInPoints((short) 10);
        f.setColor(IndexedColors.BLACK.getIndex());
        f.setBold(true);
        // 创建第二种字体样式（用于值）
        f2.setFontHeightInPoints((short) 10);
        f2.setColor(IndexedColors.BLACK.getIndex());

        // 设置第一种单元格的样式（用于列名）
        titleCellStyle.setFont(f);
        titleCellStyle.setBorderLeft(BorderStyle.THIN);
        titleCellStyle.setBorderRight(BorderStyle.THIN);
        titleCellStyle.setBorderTop(BorderStyle.THIN);
        titleCellStyle.setBorderBottom(BorderStyle.THIN);
        titleCellStyle.setAlignment(HorizontalAlignment.CENTER);

        // 设置第二种单元格的样式（用于值）
        valueCessStyle.setFont(f2);
        valueCessStyle.setBorderLeft(BorderStyle.THIN);
        valueCessStyle.setBorderRight(BorderStyle.THIN);
        valueCessStyle.setBorderTop(BorderStyle.THIN);
        valueCessStyle.setBorderBottom(BorderStyle.THIN);
        valueCessStyle.setAlignment(HorizontalAlignment.CENTER);
        // 设置列名
        for (int i = 0; i < columnNameArr.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(columnNameArr[i]);
            cell.setCellStyle(titleCellStyle);
            cell.setCellType(CellType.STRING);
        }
        // 设置每行每列的值（创建10行空白行）
        for (short i = 0; i < 10; i++) {
            // Row 行,Cell 方格 , Row 和 Cell 都是从0开始计数的
            // 创建一行，在页sheet上
            Row row1 = sheet.createRow((short) (i + 1));
            // 在row行上创建一个方格
            for (short j = 0; j < columnNameArr.length; j++) {
                Cell cell = row1.createCell(j);
                cell.setCellValue(" ");
                cell.setCellStyle(valueCessStyle);
                cell.setCellType(CellType.STRING);
            }
        }
    }


    /**
     * 下载指定excel
     *
     * @param wb
     * @param response
     * @param exportName
     */
    public static void downloadWorkBook(Workbook wb, HttpServletRequest request, HttpServletResponse response, String exportName) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        wb.write(os);//将wb写入os
        byte[] content = os.toByteArray();
        InputStream is = new ByteArrayInputStream(content);
        // 设置response参数，可以打开下载页面
        response.reset();
        response.setContentType("application/vnd.ms-excel;charset=utf-8");
        //response.setContentType("application/force-download");// 设置强制下载不打开
        response.addHeader("Content-Disposition", "attachment;fileName=" + CommonUtil.processDownloadFileName(request, exportName));// 设置文件名

        ServletOutputStream out = response.getOutputStream();
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(is);
            bos = new BufferedOutputStream(out);
            byte[] buff = new byte[2048];
            int bytesRead;
            while (-1 != (bytesRead = bis.read(buff, 0, buff.length))) {
                bos.write(buff, 0, bytesRead);
            }
        } catch (final IOException e) {
            throw e;
        } finally {
            if (bis != null)
                bis.close();
            if (bos != null)
                bos.close();
        }
    }

    /**
     * 获取map中的value
     *
     * @param key
     * @param map
     * @return
     */
    public static String getMapVal(Object key, Map<Long, String> map) {
        if (null == key || "".equals(key.toString())) {
            return "";
        }
        try {
            if (map.containsKey(Long.valueOf(key.toString()))) {
                return map.get(Long.valueOf(key.toString()));
            }
        } catch (Exception e) {
        }
        return "";
    }

    public static String getCellValue(Cell cell) {
        if (null == cell) {
            return null;
        }
        if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            if (HSSFDateUtil.isCellDateFormatted(cell)) {
                return DateUtil.formatDate(cell.getDateCellValue());
            } else {
                //日期解析，参考：http://yl-fighting.iteye.com/blog/1726285
                short format = cell.getCellStyle().getDataFormat();
                SimpleDateFormat sdf = null;
                if (format == 14 || format == 31 || format == 57 || format == 58) {
                    //日期
                    sdf = new SimpleDateFormat("yyyy-MM-dd");
                } else if (format == 20 || format == 32) {
                    //时间
                    sdf = new SimpleDateFormat("HH:mm");
                } else {
                    //不是时间的
                    return cell.getNumericCellValue() + "";
                }
                double value = cell.getNumericCellValue();
                Date date = org.apache.poi.ss.usermodel.DateUtil.getJavaDate(value);
                return sdf.format(date);
            }
        }
        return cell.getStringCellValue();
    }

    /**
     * 从文件中获取workbook
     *
     * @param multipartFile
     * @return
     * @throws Exception
     */
    public static Workbook getWorkBookFromMultipartFile(MultipartFile multipartFile) throws Exception {
        Workbook workBook = null;
        try {
            workBook = new XSSFWorkbook(multipartFile.getInputStream());
        } catch (Exception ex) {
            workBook = new HSSFWorkbook(multipartFile.getInputStream());
        }
        return workBook;
    }
}
