package com.hit.fm.common.util;

import com.hit.fm.common.constants.BizConstants;
import com.hit.fm.model.sys.User;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

/**
 * 文 件 名：WaiWangSessionUtil.java
 * 说 明：外网登陆用户帮助类
 * 作 者：陈江海
 * 创建时间：2015-11-16
 * 版权所有：泉州哈工大工程技术研究院
 */
public class SessionUtil {
    public static User getCurrentUser(HttpServletRequest request) {
        Subject subject = SecurityUtils.getSubject();
        Object obj = subject.getSession().getAttribute(BizConstants.USER_INFO);
        if (null == obj) {
            return null;
        }
        return (User) obj;
    }

    public static String getCurrentUserName(HttpServletRequest request) {
        User user = getCurrentUser(request);
        if (null != user) {
            return user.getUserNickName();
        }
        return "";
    }

    public static long getCurrentUserId(HttpServletRequest request) {
        User user = getCurrentUser(request);
        if (null != user) {
            return user.getId();
        }
        return 0l;
    }

    public static void setCurrentUser(User user) {
        Subject subject = SecurityUtils.getSubject();
        subject.getSession().setAttribute(BizConstants.USER_INFO, user);
    }


    public static void setSession(String key, Object value) {
        Subject subject = SecurityUtils.getSubject();
        subject.getSession().setAttribute(key, value);
    }

    public static Object getSession(String key) {
        Subject subject = SecurityUtils.getSubject();
        return subject.getSession().getAttribute(key);
    }

    public static Collection<Object> getUserAttrKey() {
        Subject subject = SecurityUtils.getSubject();
        return subject.getSession().getAttributeKeys();
    }

    public static void removeKey(String key) {
        Subject subject = SecurityUtils.getSubject();
        subject.getSession().removeAttribute(key);
    }
    /**
     * 获取当前登录的用户ID
     *
     * @param request
     * @return
     */
    public static Long getUserId() {
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getSession().getAttribute(BizConstants.USER_INFO);
        if (null != user) {
            return user.getId();
        }
        return 0l;
    }
}
