package com.xhr.and.rframework.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xhrong on 2016/12/5.
 */
public class StringUtils {
    /**
     * is null or its length is 0 or it is made by space
     * <p>
     * <pre>
     * isBlank(null) = true;
     * isBlank(&quot;&quot;) = true;
     * isBlank(&quot;  &quot;) = true;
     * isBlank(&quot;a&quot;) = false;
     * isBlank(&quot;a &quot;) = false;
     * isBlank(&quot; a&quot;) = false;
     * isBlank(&quot;a b&quot;) = false;
     * </pre>
     *
     * @param str
     * @return if string is null or its size is 0 or it is made by space, return true, else return false.
     */
    public static boolean isBlank(String str) {
        return (str == null || str.trim().length() == 0);
    }

    /**
     * is null or its length is 0
     * <p>
     * <pre>
     * isEmpty(null) = true;
     * isEmpty(&quot;&quot;) = true;
     * isEmpty(&quot;  &quot;) = false;
     * </pre>
     *
     * @param str
     * @return if string is null or its size is 0, return true, else return false.
     */
    public static boolean isEmpty(CharSequence str) {
        return (str == null || str.length() == 0);
    }

    /**
     * 去除字符串两端的指定字符串
     * @param sourceStr 源字符串
     * @param trimStr 两端需要去除的字符串
     * @return 两端去除指定字符串后的字符串
     */
    public static String trim(String sourceStr, String trimStr) {
        // null或者空字符串的时候不处理
        if (isEmpty(sourceStr) || isEmpty(trimStr)) {
            return sourceStr;
        }

        // 结束位置
        int epos = 0;

        // 正规表达式
        String regpattern = "[" + trimStr + "]*+";
        Pattern pattern = Pattern.compile(regpattern, Pattern.CASE_INSENSITIVE);

        // 去掉结尾的指定字符
        StringBuffer buffer = new StringBuffer(sourceStr).reverse();
        Matcher matcher = pattern.matcher(buffer);
        if (matcher.lookingAt()) {
            epos = matcher.end();
            sourceStr = new StringBuffer(buffer.substring(epos)).reverse().toString();
        }

        // 去掉开头的指定字符
        matcher = pattern.matcher(sourceStr);
        if (matcher.lookingAt()) {
            epos = matcher.end();
            sourceStr = sourceStr.substring(epos);
        }

        // 返回处理后的字符串
        return sourceStr;
    }


    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        System.out.println("\nsdfsdflkklkksd\n".trim());
        System.out.println(trim("sdfsdflkklkksdsd","sd"));
    }

}
