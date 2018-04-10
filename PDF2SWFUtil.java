package com.hit.fm.common.util;

import com.hit.fm.common.constants.BizConstants;
import org.apache.log4j.Logger;

import java.io.*;

public class PDF2SWFUtil {

    private static Logger logger = Logger.getLogger(PDF2SWFUtil.class);

    /**
     * 利用SWFTools工具将pdf转换成swf，转换完后的swf文件与pdf同名
     *
     * @param fileDir PDF文件存放路径（包括文件名）
     * @param exePath 转换器安装路径
     * @throws IOException
     * @author iori
     */
    public static synchronized String pdf2swf(String fileDir) throws Exception {
        //文件路径
        String filePath = fileDir.substring(0, fileDir.lastIndexOf("/"));
        //文件名，不带后缀
        String fileName = fileDir.substring((filePath.length() + 1), fileDir.lastIndexOf("."));
        String outPutPath = filePath + "/" + fileName + ".swf";
        File target = new File(outPutPath);
        if (target.exists()) {
            logger.info("文件：" + target + "已存在，无需再次转换");
            return outPutPath;
        }


        Process pro = null;
        if (isWindowsSystem()) {
            //如果是windows系统
            //命令行命令
            String cmd = BizConstants.PDF2SWF_EXE_PATH + " \"" + fileDir + "\" -o \"" + outPutPath + "\"";
            //Runtime执行后返回创建的进程对象
            pro = Runtime.getRuntime().exec(cmd);
        } else {
            //如果是linux系统,路径不能有空格，而且一定不能用双引号，否则无法创建进程
            String[] cmd = new String[3];
            cmd[0] = BizConstants.PDF2SWF_EXE_PATH;
            cmd[1] = fileDir;
            cmd[2] = outPutPath;
            //Runtime执行后返回创建的进程对象
            pro = Runtime.getRuntime().exec(cmd);
        }
        //非要读取一遍cmd的输出，要不不会flush生成文件（多线程）
        new DoOutput(pro.getInputStream()).start();
        new DoOutput(pro.getErrorStream()).start();
        try {
            //调用waitFor方法，是为了阻塞当前进程，直到cmd执行完
            pro.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return outPutPath;
    }

    /**
     * 判断是否是windows操作系统
     *
     * @return
     * @author iori
     */
    private static boolean isWindowsSystem() {
        String p = System.getProperty("os.name");
        return p.toLowerCase().indexOf("windows") >= 0 ? true : false;
    }

    /**
     * 多线程内部类
     * 读取转换时cmd进程的标准输出流和错误输出流，这样做是因为如果不读取流，进程将死锁
     *
     * @author iori
     */
    private static class DoOutput extends Thread {
        public InputStream is;

        //构造方法
        public DoOutput(InputStream is) {
            this.is = is;
        }

        public void run() {
            BufferedReader br = new BufferedReader(new InputStreamReader(this.is));
            String str = null;
            try {
                //这里并没有对流的内容进行处理，只是读了一遍
                while ((str = br.readLine()) != null) ;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 测试main方法
     *
     * @param args
     */
    public static void main(String[] args) {
        //转换器安装路径
        String exePath = "c:/Program Files/SWFTools/pdf2swf.exe";
        try {
            String path = PDF2SWFUtil.pdf2swf("E:/CSP 开发环境.pdf");
            System.out.println("转换成功，path:" + path);
        } catch (Exception e) {
            System.err.println("转换出错！");
            e.printStackTrace();
        }
    }
}