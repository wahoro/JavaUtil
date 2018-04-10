package com.hit.fm.common.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


/**
 * 文  件 名：DateUtil.java
 * 说	明：
 * 作	者：陈江海
 * 创建时间：2015-8-5
 * 版权所有：泉州哈工大工程技术研究院
 */
public class DateUtil {
    public final static String FORMAT_DATE_TIME = "yyyy-MM-dd HH:mm:ss";
    public final static String FORMAT_DATE = "yyyy-MM-dd";

    public static String formatDate(Date date) {
        SimpleDateFormat sf = new SimpleDateFormat(FORMAT_DATE_TIME);
        return sf.format(date);
    }

    public static String formatDate(Date date, String formatStr) {
        SimpleDateFormat sf = new SimpleDateFormat(formatStr);
        return sf.format(date);
    }

    public static Date parseDate(String dateStr, String format) {
        SimpleDateFormat sf = new SimpleDateFormat(format);
        try {
            return sf.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    public static String reFormatDateStr(String dateStr, String preFormat, String newFormat) {
        SimpleDateFormat sf = new SimpleDateFormat(preFormat);
        SimpleDateFormat newSf = new SimpleDateFormat(newFormat);
        try {
            Date date = sf.parse(dateStr);
            return newSf.format(date);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 得到指定加减天数的时间字符串
     *
     * @param addDays
     * @param isNeedTime
     * @return YYYY-MM-DD 格式的字符串
     */
    public static String getDayTimeStr(int addDays) {
        Calendar dtnow = Calendar.getInstance();
        dtnow.add(Calendar.DAY_OF_MONTH, addDays);
        int month = dtnow.get(Calendar.MONTH) + 1;
        String monthStr = (month < 10) ? "0" + month : "" + month;

        int day = dtnow.get(Calendar.DAY_OF_MONTH);
        String dayStr = (day < 10) ? "0" + day : "" + day;

        String retStr = Integer.toString(dtnow.get(Calendar.YEAR)) + "-" + monthStr + "-" + dayStr;
        return retStr;
    }
	 
    /**
     * 获取当前星期索引（周日为一周开始，索引为0）
     *
     * @param date
     * @return
     */
    public static int getWeekIndex(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_WEEK) - 1;
    }

    /**
     * 获取当前星期
     *
     * @param date
     * @return
     */
    public static String getWeekDate(Date date) {
        String[] weekDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        return weekDays[getWeekIndex(date)];
    }

    public static void main(String[] args) {
        System.out.println(formatDate(new Date(), "yyyyMMddHHmmssSSS"));
    }
}
