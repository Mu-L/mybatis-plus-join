package com.github.yulichang.kt;

import com.baomidou.mybatisplus.core.conditions.ISqlSegment;
import com.baomidou.mybatisplus.core.conditions.SharedString;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.interfaces.Nested;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.enums.SqlKeyword;
import com.baomidou.mybatisplus.core.enums.SqlLike;
import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.baomidou.mybatisplus.core.toolkit.*;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlUtils;
import com.baomidou.mybatisplus.core.toolkit.sql.StringEscape;
import com.github.yulichang.config.ConfigProperties;
import com.github.yulichang.kt.interfaces.CompareIfExists;
import com.github.yulichang.kt.interfaces.Func;
import com.github.yulichang.kt.interfaces.OnCompare;
import com.github.yulichang.toolkit.KtUtils;
import com.github.yulichang.toolkit.MPJSqlInjectionUtils;
import com.github.yulichang.toolkit.Ref;
import com.github.yulichang.toolkit.TableList;
import com.github.yulichang.toolkit.sql.SqlScriptUtils;
import com.github.yulichang.wrapper.enums.IfExistsSqlKeyWordEnum;
import com.github.yulichang.wrapper.enums.PrefixEnum;
import com.github.yulichang.wrapper.interfaces.CompareStrIfExists;
import com.github.yulichang.wrapper.interfaces.FuncStr;
import com.github.yulichang.wrapper.interfaces.Join;
import kotlin.reflect.KProperty;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.baomidou.mybatisplus.core.enums.SqlKeyword.*;
import static com.baomidou.mybatisplus.core.enums.WrapperKeyword.APPLY;
import static java.util.stream.Collectors.joining;

/**
 * 查询条件封装
 * copy {@link com.baomidou.mybatisplus.core.conditions.AbstractWrapper}
 *
 * @author yulichang
 */
@SuppressWarnings({"unused", "unchecked", "DuplicatedCode"})
public abstract class KtAbstractWrapper<T, Children extends KtAbstractWrapper<T, Children>> extends Wrapper<T>
        implements CompareIfExists<Children>, Nested<Children, Children>, Join<Children>, Func<Children>, OnCompare<Children>,
        CompareStrIfExists<Children, String>, FuncStr<Children, String> {

    /**
     * 占位符
     */
    protected final Children typedThis = (Children) this;
    /**
     * 表别名
     */
    @Getter
    protected Integer index;
    /**
     * 必要度量
     */
    @Getter
    protected AtomicInteger paramNameSeq;
    @Getter
    protected Map<String, Object> paramNameValuePairs;
    /**
     * 其他
     */
    /* mybatis plus 3.4.3新增 这个时wrapper的别名 不是MPJ的别名 */
    protected SharedString paramAlias = new SharedString(null);
    protected SharedString lastSql;
    /**
     * SQL注释
     */
    protected SharedString sqlComment;
    /**
     * SQL起始语句
     */
    protected SharedString sqlFirst;
    /**
     * ON sql wrapper集合
     */
    protected final List<Children> onWrappers = new ArrayList<>();
    /**
     * 数据库表映射实体类
     */
    private T entity;
    protected MergeSegments expression;
    /**
     * 实体类型(主要用于确定泛型以及取TableInfo缓存)
     */
    private Class<T> entityClass;
    /**
     * 连表实体类 on 条件 func 使用
     */
    @Getter
    protected Class<?> joinClass;

    /**
     * 连表表名
     */
    @Getter
    protected String tableName;
    /**
     * 主表wrapper
     */
    protected boolean isMain = true;
    /**
     * 是否是OnWrapper
     */
    protected boolean isNo = false;

    /**
     * 关联的表
     */
    @Getter
    protected TableList tableList;

    /**
     * 检查 SQL 注入过滤
     */
    protected boolean checkSqlInjection = false;

    /**
     * IfExists 策略
     */
    @Getter
    protected BiPredicate<Object, IfExistsSqlKeyWordEnum> ifExists = ConfigProperties.ifExists;

    @Override
    public T getEntity() {
        return entity;
    }

    public Children setEntity(T entity) {
        this.entity = entity;
        return typedThis;
    }

    public Class<T> getEntityClass() {
        if (entityClass == null && entity != null) {
            entityClass = (Class<T>) entity.getClass();
        }
        return entityClass;
    }

    public Children setEntityClass(Class<T> entityClass) {
        if (entityClass != null) {
            onWrappers.forEach(i -> i.setEntityClass(entityClass));
            this.entityClass = entityClass;
        }
        if (tableList != null) {
            tableList.setRootClass(entityClass);
        }
        return typedThis;
    }

    /**
     * 转为子类，方便自定义继承扩展
     */
    public <C extends Children> C toChildren(Ref<C> children) {
        return (C) this;
    }

    /**
     * 转为子类，方便自定义继承扩展
     * 需要子类自定义字段
     */
    public <C extends Children> C toChildren(Supplier<C> s) {
        return (C) this;
    }

    /**
     * 开启检查 SQL 注入
     */
    public Children checkSqlInjection() {
        this.checkSqlInjection = true;
        return typedThis;
    }

    public Children setIfExists(BiPredicate<Object, IfExistsSqlKeyWordEnum> IfExists) {
        this.ifExists = IfExists;
        return typedThis;
    }

    public Children setIfExists(Predicate<Object> IfExists) {
        this.ifExists = (o, k) -> IfExists.test(o);
        return typedThis;
    }

    @Override
    public Children allEq(boolean condition, Map<KProperty<?>, ?> params, boolean null2IsNull) {
        if (condition && CollectionUtils.isNotEmpty(params)) {
            params.forEach((k, v) -> {
                if (StringUtils.checkValNotNull(v)) {
                    eq(k, v);
                } else {
                    if (null2IsNull) {
                        isNull(k);
                    }
                }
            });
        }
        return typedThis;
    }

    @Override
    public Children eq(boolean condition, String alias, KProperty<?> column, Object val) {
        return addCondition(condition, alias, column, EQ, val);
    }

    @Override
    public Children ne(boolean condition, String alias, KProperty<?> column, Object val) {
        return addCondition(condition, alias, column, NE, val);
    }

    @Override
    public Children gt(boolean condition, String alias, KProperty<?> column, Object val) {
        return addCondition(condition, alias, column, GT, val);
    }

    @Override
    public Children ge(boolean condition, String alias, KProperty<?> column, Object val) {
        return addCondition(condition, alias, column, GE, val);
    }

    @Override
    public Children lt(boolean condition, String alias, KProperty<?> column, Object val) {
        return addCondition(condition, alias, column, LT, val);
    }

    @Override
    public Children le(boolean condition, String alias, KProperty<?> column, Object val) {
        return addCondition(condition, alias, column, LE, val);
    }

    @Override
    public Children like(boolean condition, String alias, KProperty<?> column, Object val) {
        return likeValue(condition, LIKE, alias, column, val, SqlLike.DEFAULT);
    }

    @Override
    public Children notLike(boolean condition, String alias, KProperty<?> column, Object val) {
        return likeValue(condition, NOT_LIKE, alias, column, val, SqlLike.DEFAULT);
    }

    @Override
    public Children likeLeft(boolean condition, String alias, KProperty<?> column, Object val) {
        return likeValue(condition, LIKE, alias, column, val, SqlLike.LEFT);
    }

    @Override
    public Children notLikeLeft(boolean condition, String alias, KProperty<?> column, Object val) {
        return likeValue(condition, NOT_LIKE, alias, column, val, SqlLike.LEFT);
    }

    @Override
    public Children likeRight(boolean condition, String alias, KProperty<?> column, Object val) {
        return likeValue(condition, LIKE, alias, column, val, SqlLike.RIGHT);
    }

    @Override
    public Children notLikeRight(boolean condition, String alias, KProperty<?> column, Object val) {
        return likeValue(condition, NOT_LIKE, alias, column, val, SqlLike.RIGHT);
    }

    @Override
    public Children between(boolean condition, String alias, KProperty<?> column, Object val1, Object val2) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(index, alias, column), BETWEEN,
                () -> formatParam(null, val1), AND, () -> formatParam(null, val2)));
    }

    @Override
    public Children notBetween(boolean condition, String alias, KProperty<?> column, Object val1, Object val2) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(index, alias, column), NOT_BETWEEN,
                () -> formatParam(null, val1), AND, () -> formatParam(null, val2)));
    }

    @Override
    public Children and(boolean condition, Consumer<Children> consumer) {
        return and(condition).addNestedCondition(condition, consumer);
    }

    @Override
    public Children or(boolean condition, Consumer<Children> consumer) {
        return or(condition).addNestedCondition(condition, consumer);
    }

    @Override
    public Children nested(boolean condition, Consumer<Children> consumer) {
        return addNestedCondition(condition, consumer);
    }

    @Override
    public Children not(boolean condition, Consumer<Children> consumer) {
        return not(condition).addNestedCondition(condition, consumer);
    }

    @Override
    public Children or(boolean condition) {
        return maybeDo(condition, () -> appendSqlSegments(OR));
    }

    @Override
    public Children apply(boolean condition, String applySql, Object... values) {
        return maybeDo(condition, () -> appendSqlSegments(APPLY,
                () -> formatSqlMaybeWithParam(applySql, null, values)));
    }

    @Override
    public Children last(boolean condition, String lastSql) {
        if (condition) {
            this.lastSql.setStringValue(this.lastSql.getStringValue() + StringPool.SPACE + lastSql);
        }
        return typedThis;
    }

    @Override
    public Children comment(boolean condition, String comment) {
        if (condition) {
            this.sqlComment.setStringValue(comment);
        }
        return typedThis;
    }

    @Override
    public Children first(boolean condition, String firstSql) {
        if (condition) {
            this.sqlFirst.setStringValue(firstSql + StringPool.SPACE + this.sqlFirst.getStringValue());
        }
        return typedThis;
    }

    @Override
    public Children around(boolean condition, String firstSql, String lastSql) {
        this.first(condition, firstSql);
        this.last(condition, lastSql);
        return typedThis;
    }

    @Override
    public Children exists(boolean condition, String existsSql, Object... values) {
        return maybeDo(condition, () -> appendSqlSegments(EXISTS,
                () -> String.format("(%s)", formatSqlMaybeWithParam(existsSql, null, values))));
    }

    @Override
    public Children notExists(boolean condition, String existsSql, Object... values) {
        return not(condition).exists(condition, existsSql, values);
    }

    @Override
    public Children isNull(boolean condition, String alias, KProperty<?> column) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(index, alias, column), IS_NULL));
    }

    @Override
    public Children isNotNull(boolean condition, String alias, KProperty<?> column) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(index, alias, column), IS_NOT_NULL));
    }

    @Override
    public Children in(boolean condition, String alias, KProperty<?> column, Collection<?> coll) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(index, alias, column), IN, inExpression(coll)));
    }

    @Override
    public Children in(boolean condition, String alias, KProperty<?> column, Object... values) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(index, alias, column), IN, inExpression(values)));
    }

    @Override
    public Children notIn(boolean condition, String alias, KProperty<?> column, Collection<?> coll) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(index, alias, column), NOT_IN, inExpression(coll)));
    }

    @Override
    public Children notIn(boolean condition, String alias, KProperty<?> column, Object... values) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(index, alias, column), NOT_IN, inExpression(values)));
    }

    @Override
    public Children inSql(boolean condition, String alias, KProperty<?> column, String inValue) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(index, alias, column), IN,
                () -> String.format("(%s)", inValue)));
    }

    @Override
    public Children notInSql(boolean condition, String alias, KProperty<?> column, String inValue) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(index, alias, column), NOT_IN,
                () -> String.format("(%s)", inValue)));
    }


    @Override
    public Children gtSql(boolean condition, String alias, KProperty<?> column, String inValue) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(index, alias, column), GT,
                () -> String.format("(%s)", inValue)));
    }

    @Override
    public Children geSql(boolean condition, String alias, KProperty<?> column, String inValue) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(index, alias, column), GE,
                () -> String.format("(%s)", inValue)));
    }

    @Override
    public Children ltSql(boolean condition, String alias, KProperty<?> column, String inValue) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(index, alias, column), LT,
                () -> String.format("(%s)", inValue)));
    }

    @Override
    public Children leSql(boolean condition, String alias, KProperty<?> column, String inValue) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(index, alias, column), LE,
                () -> String.format("(%s)", inValue)));
    }

    @Override
    public Children groupBy(boolean condition, String alias, List<KProperty<?>> columns) {
        return maybeDo(condition, () -> {
            if (CollectionUtils.isNotEmpty(columns)) {
                final String finalOne = columnsToString(index, isNo ? PrefixEnum.ON_FIRST : PrefixEnum.CD_FIRST, alias, columns);
                appendSqlSegments(GROUP_BY, () -> finalOne);
            }
        });
    }

    @Override
    public Children groupBy(boolean condition, String alias, KProperty<?>... columns) {
        return maybeDo(condition, () -> {
            if (ArrayUtils.isNotEmpty(columns)) {
                final String finalOne = columnsToString(index, isNo ? PrefixEnum.ON_FIRST : PrefixEnum.CD_FIRST, alias, columns);
                appendSqlSegments(GROUP_BY, () -> finalOne);
            }
        });
    }

    @Override
    public Children orderByAsc(boolean condition, String alias, List<KProperty<?>> columns) {
        return maybeDo(condition, () -> {
            if (CollectionUtils.isNotEmpty(columns)) {
                columns.forEach(c -> appendSqlSegments(ORDER_BY,
                        columnToSqlSegment(index, alias, columnSqlInjectFilter(c)), ASC));
            }
        });
    }

    @Override
    public Children orderByDesc(boolean condition, String alias, List<KProperty<?>> columns) {
        return maybeDo(condition, () -> {
            if (CollectionUtils.isNotEmpty(columns)) {
                columns.forEach(c -> appendSqlSegments(ORDER_BY,
                        columnToSqlSegment(index, alias, columnSqlInjectFilter(c)), DESC));
            }
        });
    }

    @Override
    public Children orderBy(boolean condition, boolean isAsc, String alias, KProperty<?>... columns) {
        return maybeDo(condition, () -> {
            final SqlKeyword mode = isAsc ? ASC : DESC;
            if (ArrayUtils.isNotEmpty(columns)) {
                Arrays.stream(columns).forEach(c -> appendSqlSegments(ORDER_BY,
                        columnToSqlSegment(index, alias, columnSqlInjectFilter(c)), mode));
            }
        });
    }

    /**
     * 字段 SQL 注入过滤处理，子类重写实现过滤逻辑
     *
     * @param column 字段内容
     * @return Children
     */
    protected KProperty<?> columnSqlInjectFilter(KProperty<?> column) {
        return column;
    }

    @Override
    public Children having(boolean condition, String sqlHaving, Object... params) {
        return maybeDo(condition, () -> appendSqlSegments(HAVING,
                () -> formatSqlMaybeWithParam(sqlHaving, null, params)));
    }

    @Override
    public Children func(boolean condition, Consumer<Children> consumer) {
        return maybeDo(condition, () -> consumer.accept(typedThis));
    }

    /**
     * 内部自用
     * <p>NOT 关键词</p>
     */
    protected Children not(boolean condition) {
        return maybeDo(condition, () -> appendSqlSegments(NOT));
    }

    /**
     * 内部自用
     * <p>拼接 AND</p>
     */
    protected Children and(boolean condition) {
        return maybeDo(condition, () -> appendSqlSegments(AND));
    }

    /**
     * 内部自用
     * <p>拼接 LIKE 以及 值</p>
     */
    protected Children likeValue(boolean condition, SqlKeyword keyword, String alias, KProperty<?> column, Object val, SqlLike sqlLike) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(index, alias, column), keyword,
                () -> formatParam(null, SqlUtils.concatLike(val, sqlLike))));
    }

    protected Children likeValue(boolean condition, SqlKeyword keyword, String column, Object val, SqlLike sqlLike) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(column), keyword,
                () -> formatParam(null, SqlUtils.concatLike(val, sqlLike))));
    }

    /**
     * 普通查询条件
     *
     * @param condition  是否执行
     * @param column     属性
     * @param sqlKeyword SQL 关键词
     * @param val        条件值
     */
    protected Children addCondition(boolean condition, String alias, KProperty<?> column, SqlKeyword sqlKeyword, Object val) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(index, alias, column), sqlKeyword,
                () -> formatParam(null, val)));
    }

    protected <X, S> Children addCondition(boolean condition, String alias, KProperty<?> column, SqlKeyword sqlKeyword, String rightAlias, KProperty<?> val) {
        Class<X> c = (Class<X>) KtUtils.ref(column);
        Class<S> v = (Class<S>) KtUtils.ref(val);
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(index, alias, column), sqlKeyword,
                isNo ?
                        columnToSqlSegmentS(index, rightAlias, val, v == c && v == joinClass) :
                        columnToSqlSegmentS(index, rightAlias, val, v == c)
        ));
    }

    protected Children addCondition(boolean condition, String column, SqlKeyword sqlKeyword, Object val) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(column), sqlKeyword,
                () -> formatParam(null, val)));
    }

    /**
     * 多重嵌套查询条件
     *
     * @param condition 查询条件值
     */
    protected Children addNestedCondition(boolean condition, Consumer<Children> consumer) {
        return maybeDo(condition, () -> {
            final Children instance = instance();
            consumer.accept(instance);
            appendSqlSegments(APPLY, instance);
        });
    }

    /**
     * 子类返回一个自己的新对象
     */
    protected abstract Children instance();

    protected abstract Children instanceEmpty();

    protected abstract Children instance(Integer index, String keyWord, Class<?> joinClass, String tableName);

    /**
     * 格式化 sql
     * <p>
     * 支持 "{0}" 这种,或者 "sql {0} sql" 这种
     *
     * @param sqlStr  可能是sql片段
     * @param mapping 例如: "javaType=int,jdbcType=NUMERIC,typeHandler=xxx.xxx.MyTypeHandler" 这种
     * @param params  参数
     * @return sql片段
     */
    @SuppressWarnings("SameParameterValue")
    protected final String formatSqlMaybeWithParam(String sqlStr, String mapping, Object... params) {
        if (StringUtils.isBlank(sqlStr)) {
            // todo 何时会这样?
            return null;
        }
        if (ArrayUtils.isNotEmpty(params)) {
            for (int i = 0; i < params.length; ++i) {
                final String target = Constants.LEFT_BRACE + i + Constants.RIGHT_BRACE;
                sqlStr = sqlStr.replace(target, formatParam(mapping, params[i]));
            }
        }
        return sqlStr;
    }

    /**
     * 处理入参
     *
     * @param mapping 例如: "javaType=int,jdbcType=NUMERIC,typeHandler=xxx.xxx.MyTypeHandler" 这种
     * @param param   参数
     * @return value
     */
    protected final String formatParam(String mapping, Object param) {
        final String genParamName = Constants.WRAPPER_PARAM + paramNameSeq.incrementAndGet();
        final String paramStr = getParamAlias() + ".paramNameValuePairs." + genParamName;
        paramNameValuePairs.put(genParamName, param);
        return SqlScriptUtils.safeParam(paramStr, mapping);
    }

    /**
     * 函数化的做事
     *
     * @param condition 做不做
     * @param something 做什么
     * @return Children
     */
    protected final Children maybeDo(boolean condition, DoSomething something) {
        if (condition) {
            something.doIt();
        }
        return typedThis;
    }

    /**
     * 获取in表达式 包含括号
     *
     * @param value 集合
     */
    protected ISqlSegment inExpression(Collection<?> value) {
        if (CollectionUtils.isEmpty(value)) {
            return () -> "()";
        }
        return () -> value.stream().map(i -> formatParam(null, i))
                .collect(joining(StringPool.COMMA, StringPool.LEFT_BRACKET, StringPool.RIGHT_BRACKET));
    }

    /**
     * 获取in表达式 包含括号
     *
     * @param values 数组
     */
    protected ISqlSegment inExpression(Object[] values) {
        if (ArrayUtils.isEmpty(values)) {
            return () -> "()";
        }
        return () -> Arrays.stream(values).map(i -> formatParam(null, i))
                .collect(joining(StringPool.COMMA, StringPool.LEFT_BRACKET, StringPool.RIGHT_BRACKET));
    }

    @Override
    public void clear() {
        entity = null;
        paramNameSeq.set(0);
        paramNameValuePairs.clear();
        expression.clear();
        paramAlias.toNull();
        if (Objects.nonNull(lastSql)) lastSql.toEmpty();
        if (Objects.nonNull(sqlComment)) sqlComment.toEmpty();
        if (Objects.nonNull(sqlFirst)) sqlFirst.toEmpty();
        tableList.clear();
        entityClass = null;
        onWrappers.clear();
        index = null;
        isMain = true;
        isNo = false;
        ifExists = ConfigProperties.ifExists;
    }

    /**
     * 添加 where 片段
     *
     * @param sqlSegments ISqlSegment 数组
     */
    protected void appendSqlSegments(ISqlSegment... sqlSegments) {
        expression.add(sqlSegments);
    }

    @Override
    public String getSqlSegment() {
        return expression.getSqlSegment() + lastSql.getStringValue();
    }

    @Override
    public String getSqlComment() {
        if (StringUtils.isNotBlank(sqlComment.getStringValue())) {
            return "/*" + StringEscape.escapeRawString(sqlComment.getStringValue()) + "*/";
        }
        return null;
    }

    @Override
    public String getSqlFirst() {
        if (StringUtils.isNotBlank(sqlFirst.getStringValue())) {
            return StringEscape.escapeRawString(sqlFirst.getStringValue());
        }
        return null;
    }

    @Override
    public MergeSegments getExpression() {
        return expression;
    }

    public String getParamAlias() {
        return paramAlias.getStringValue() == null ? Constants.WRAPPER : paramAlias.getStringValue();
    }

    /**
     * 参数别名设置，初始化时优先设置该值、重复设置异常
     *
     * @param paramAlias 参数别名
     * @return Children
     */
    @SuppressWarnings("UnusedReturnValue")
    public Children setParamAlias(String paramAlias) {
        Assert.notEmpty(paramAlias, "paramAlias can not be empty!");
        this.paramAlias.setStringValue(paramAlias);
        return typedThis;
    }

    /**
     * 获取 columnName
     */
    protected final ISqlSegment columnToSqlSegment(Integer index, String alias, KProperty<?> column) {
        return () -> columnToString(index, alias, column, false, isNo ? PrefixEnum.ON_FIRST : PrefixEnum.CD_FIRST);
    }

    protected final ISqlSegment columnToSqlSegmentS(Integer index, String alias, KProperty<?> column, boolean isJoin) {
        PrefixEnum prefixEnum;
        if (isMain) {
            prefixEnum = isNo ? PrefixEnum.ON_SECOND /* 理论上不可能有这种情况 */ : PrefixEnum.CD_SECOND;
        } else {
            prefixEnum = isNo ? PrefixEnum.ON_SECOND : PrefixEnum.CD_ON_SECOND;
        }
        return () -> columnToString(index, alias, column, isJoin, prefixEnum);
    }

    protected final ISqlSegment columnToSqlSegment(String column) {
        return () -> columnsToString(column);
    }

    /**
     * 获取 columnName
     */
    abstract String columnToString(Integer index, String alias, KProperty<?> column, boolean isJoin, PrefixEnum prefixEnum);

    protected String columnToString(String column) {
        if (checkSqlInjection && MPJSqlInjectionUtils.check(column)) {
            throw new MybatisPlusException("Discovering SQL injection column: " + column);
        }
        return column;
    }

    protected String columnsToString(List<String> columns) {
        return columns.stream().map(this::columnToString).collect(joining(StringPool.COMMA));
    }

    protected String columnsToString(String... columns) {
        return Arrays.stream(columns).map(this::columnToString).collect(joining(StringPool.COMMA));
    }


    /**
     * 多字段转换为逗号 "," 分割字符串
     *
     * @param columns 多字段
     */
    protected String columnsToString(Integer index, PrefixEnum prefixEnum, String alias, KProperty<?>... columns) {
        return Arrays.stream(columns).map(i -> this.columnToString(index, alias, i, false, prefixEnum)).collect(joining(StringPool.COMMA));
    }

    protected String columnsToString(Integer index, PrefixEnum prefixEnum, String alias, List<KProperty<?>> columns) {
        return columns.stream().map(i -> this.columnToString(index, alias, i, false, prefixEnum)).collect(joining(StringPool.COMMA));
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Children clone() {
        return SerializationUtils.clone(typedThis);
    }

    /**
     * 做事函数
     */
    @FunctionalInterface
    public interface DoSomething {

        void doIt();
    }

    /* ************************* on语句重载 *************************** */

    @Override
    public Children eq(boolean condition, String alias, KProperty<?> column, String rightAlias, KProperty<?> val) {
        return addCondition(condition, alias, column, EQ, rightAlias, val);
    }

    @Override
    public Children ne(boolean condition, String alias, KProperty<?> column, String rightAlias, KProperty<?> val) {
        return addCondition(condition, alias, column, NE, rightAlias, val);
    }

    @Override
    public Children gt(boolean condition, String alias, KProperty<?> column, String rightAlias, KProperty<?> val) {
        return addCondition(condition, alias, column, GT, rightAlias, val);
    }

    @Override
    public Children ge(boolean condition, String alias, KProperty<?> column, String rightAlias, KProperty<?> val) {
        return addCondition(condition, alias, column, GE, rightAlias, val);
    }

    @Override
    public Children lt(boolean condition, String alias, KProperty<?> column, String rightAlias, KProperty<?> val) {
        return addCondition(condition, alias, column, LT, rightAlias, val);
    }

    @Override
    public Children le(boolean condition, String alias, KProperty<?> column, String rightAlias, KProperty<?> val) {
        return addCondition(condition, alias, column, LE, rightAlias, val);
    }

    /* ****************************************** **/


    @Override
    public <V> Children allEqStr(boolean condition, Map<String, V> params, boolean null2IsNull) {
        if (condition && CollectionUtils.isNotEmpty(params)) {
            params.forEach((k, v) -> {
                if (StringUtils.checkValNotNull(v)) {
                    eq(k, v);
                } else {
                    if (null2IsNull) {
                        isNull(k);
                    }
                }
            });
        }
        return typedThis;
    }

    @Override
    public <V> Children allEqStr(boolean condition, BiPredicate<String, V> filter, Map<String, V> params, boolean null2IsNull) {
        if (condition && CollectionUtils.isNotEmpty(params)) {
            params.forEach((k, v) -> {
                if (filter.test(k, v)) {
                    if (StringUtils.checkValNotNull(v)) {
                        eq(k, v);
                    } else {
                        if (null2IsNull) {
                            isNull(k);
                        }
                    }
                }
            });
        }
        return typedThis;
    }

    @Override
    public Children eq(boolean condition, String column, Object val) {
        return addCondition(condition, column, EQ, val);
    }

    @Override
    public Children ne(boolean condition, String column, Object val) {
        return addCondition(condition, column, NE, val);
    }

    @Override
    public Children gt(boolean condition, String column, Object val) {
        return addCondition(condition, column, GT, val);
    }

    @Override
    public Children ge(boolean condition, String column, Object val) {
        return addCondition(condition, column, GE, val);
    }

    @Override
    public Children lt(boolean condition, String column, Object val) {
        return addCondition(condition, column, LT, val);
    }

    @Override
    public Children le(boolean condition, String column, Object val) {
        return addCondition(condition, column, LE, val);
    }

    @Override
    public Children like(boolean condition, String column, Object val) {
        return likeValue(condition, LIKE, column, val, SqlLike.DEFAULT);
    }

    @Override
    public Children notLike(boolean condition, String column, Object val) {
        return likeValue(condition, NOT_LIKE, column, val, SqlLike.DEFAULT);
    }

    @Override
    public Children likeLeft(boolean condition, String column, Object val) {
        return likeValue(condition, LIKE, column, val, SqlLike.LEFT);
    }

    @Override
    public Children notLikeLeft(boolean condition, String column, Object val) {
        return likeValue(condition, NOT_LIKE, column, val, SqlLike.LEFT);
    }

    @Override
    public Children likeRight(boolean condition, String column, Object val) {
        return likeValue(condition, LIKE, column, val, SqlLike.RIGHT);
    }

    @Override
    public Children notLikeRight(boolean condition, String column, Object val) {
        return likeValue(condition, NOT_LIKE, column, val, SqlLike.RIGHT);
    }

    @Override
    public Children between(boolean condition, String column, Object val1, Object val2) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(column), BETWEEN,
                () -> formatParam(null, val1), AND, () -> formatParam(null, val2)));
    }

    @Override
    public Children notBetween(boolean condition, String column, Object val1, Object val2) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(column), NOT_BETWEEN,
                () -> formatParam(null, val1), AND, () -> formatParam(null, val2)));
    }


    /* ****************************************** **/


    @Override
    public Children isNull(boolean condition, String column) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(column), IS_NULL));
    }

    @Override
    public Children isNotNull(boolean condition, String column) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(column), IS_NOT_NULL));
    }

    @Override
    public Children in(boolean condition, String column, Collection<?> coll) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(column), IN, inExpression(coll)));
    }

    @Override
    public Children in(boolean condition, String column, Object... values) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(column), IN, inExpression(values)));
    }

    @Override
    public Children notIn(boolean condition, String column, Collection<?> coll) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(column), NOT_IN, inExpression(coll)));
    }

    @Override
    public Children notIn(boolean condition, String column, Object... values) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(column), NOT_IN, inExpression(values)));
    }

    @Override
    public Children inSql(boolean condition, String column, String inValue) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(column), IN,
                () -> String.format("(%s)", inValue)));
    }

    @Override
    public Children gtSql(boolean condition, String column, String inValue) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(column), GT,
                () -> String.format("(%s)", inValue)));
    }

    @Override
    public Children geSql(boolean condition, String column, String inValue) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(column), GE,
                () -> String.format("(%s)", inValue)));
    }

    @Override
    public Children ltSql(boolean condition, String column, String inValue) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(column), LT,
                () -> String.format("(%s)", inValue)));
    }

    @Override
    public Children leSql(boolean condition, String column, String inValue) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(column), LE,
                () -> String.format("(%s)", inValue)));
    }

    @Override
    public Children eqSql(boolean condition, String column, String inValue) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(column), EQ,
                () -> String.format("(%s)", inValue)));
    }

    @Override
    public Children notInSql(boolean condition, String column, String inValue) {
        return maybeDo(condition, () -> appendSqlSegments(columnToSqlSegment(column), NOT_IN,
                () -> String.format("(%s)", inValue)));
    }

    @Override
    public Children groupBy(boolean condition, String column, String... columns) {
        return maybeDo(condition, () -> {
            String one = columnToString(column);
            if (ArrayUtils.isNotEmpty(columns)) {
                one += (StringPool.COMMA + columnsToString(columns));
            }
            final String finalOne = one;
            appendSqlSegments(GROUP_BY, () -> finalOne);
        });
    }

    @Override
    public final Children orderBy(boolean condition, boolean isAsc, String column, String... columns) {
        return maybeDo(condition, () -> {
            final SqlKeyword mode = isAsc ? ASC : DESC;
            appendSqlSegments(ORDER_BY, columnToSqlSegment(column), mode);
            if (ArrayUtils.isNotEmpty(columns)) {
                Arrays.stream(columns).forEach(c -> appendSqlSegments(ORDER_BY,
                        columnToSqlSegment(c), mode));
            }
        });
    }

    @Override
    public Children groupBy(boolean condition, String column) {
        return maybeDo(condition, () -> appendSqlSegments(GROUP_BY, () -> columnToString(column)));
    }

    @Override
    public Children groupByStr(boolean condition, List<String> columns) {
        return maybeDo(condition, () -> appendSqlSegments(GROUP_BY, () -> columnsToString(columns)));
    }

    @Override
    public Children orderBy(boolean condition, boolean isAsc, String column) {
        return maybeDo(condition, () -> appendSqlSegments(ORDER_BY, columnToSqlSegment(column),
                isAsc ? ASC : DESC));
    }

    @Override
    public Children orderByStr(boolean condition, boolean isAsc, List<String> columns) {
        return maybeDo(condition, () -> columns.forEach(c -> appendSqlSegments(ORDER_BY,
                columnToSqlSegment(c), isAsc ? ASC : DESC)));
    }
}
