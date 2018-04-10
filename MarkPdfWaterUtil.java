package com.hit.fm.common.util;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class MarkPdfWaterUtil {
    public static void main(String[] args) throws Exception {
        String inputPath = "E:/tmp/流程引擎接入指南.pdf";
        String outputPath = "E:/tmp/流程引擎接入指南-水印.pdf";
        String imagePath = "E:/tmp/cjh.jpg";//图片地址
        String text = "测试水印";

        setWatermark(inputPath, outputPath, imagePath, text);
    }

    public static void setWatermark(String srcPath, String outputPath, String imagePath, String text) throws Exception {

        PdfReader reader = new PdfReader(srcPath);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(outputPath)));
        PdfStamper stamper = new PdfStamper(reader, bos);
        int total = reader.getNumberOfPages() + 1;//总页数
        PdfContentByte content;
        BaseFont base = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.EMBEDDED);
        PdfGState gs = new PdfGState();
        for (int i = 1; i < total; i++) {
            content = stamper.getOverContent(i);// 在内容上方加水印
            //content = stamper.getUnderContent(i);//在内容下方加水印
            gs.setFillOpacity(0.2f);
            // content.setGState(gs);
            content.beginText();
            content.setColorFill(BaseColor.LIGHT_GRAY);
            content.setFontAndSize(base, 50);
            content.setTextMatrix(70, 200);
            content.showTextAligned(Element.ALIGN_CENTER, "公司内部文件，请注意保密！", 300, 350, 55);
            Image image = Image.getInstance(imagePath);
            /*img.setAlignment(Image.LEFT | Image.TEXTWRAP);
            img.setBorder(Image.BOX);
			img.setBorderWidth(10);
			img.setBorderColor(BaseColor.WHITE);
			img.scaleToFit(1000, 72);//大小
			img.setRotationDegrees(-30);//旋转 */
            image.setAbsolutePosition(200, 206); // set the first background image of the absolute
            image.scaleToFit(200, 200);//水印大小
            content.addImage(image);
            content.setColorFill(BaseColor.BLACK);
            content.setFontAndSize(base, 8);
            content.showTextAligned(Element.ALIGN_CENTER, text, 300, 10, 0);
            content.endText();
        }
        stamper.close();
    }

}
