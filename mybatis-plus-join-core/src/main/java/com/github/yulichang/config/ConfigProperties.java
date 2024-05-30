package com.github.yulichang.config;

import com.github.yulichang.config.enums.IfExistsEnum;
import com.github.yulichang.config.enums.LogicDelTypeEnum;
import com.github.yulichang.wrapper.enums.IfExistsSqlKeyWordEnum;

import java.util.function.BiPredicate;

/**
 * @author yulichang
 * @since 1.3.7
 */
public class ConfigProperties {

    /**
     * 是否打印banner
     */
    public static boolean banner = true;
    /**
     * 是否开启副表逻辑删除
     */
    public static boolean subTableLogic = true;
    /**
     * 是否开启 ms 缓存
     */
    public static boolean msCache = true;
    /**
     * 表别名
     */
    public static String tableAlias = "t";
    /**
     * 字段名重复时前缀
     */
    public static String joinPrefix = "join";
    /**
     * 逻辑删除类型 支持 where on
     */
    public static LogicDelTypeEnum logicDelType = LogicDelTypeEnum.ON;
    /**
     * 映射查询最大深度
     */
    public static int mappingMaxCount = 5;
    /**
     * 子查询别名
     */
    public static String subQueryAlias = "st";
    /**
     * Wrapper IfExists 判断策略
     * <p>
     * NOT_NULL 非null
     * <p>
     * NOT_EMPTY 非空字符串   例： "" -> false, " " -> true ...
     * <p>
     * NOT_BLANK 非空白字符串  例： "" -> false, " " -> false, "\r" -> false, "abc" -> true ...
     */
    public static BiPredicate<Object, IfExistsSqlKeyWordEnum> ifExists = (val, key) -> IfExistsEnum.NOT_EMPTY.test(val);
}
