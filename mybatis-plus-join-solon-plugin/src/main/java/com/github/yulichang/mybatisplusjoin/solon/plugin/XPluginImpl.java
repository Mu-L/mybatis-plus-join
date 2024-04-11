package com.github.yulichang.mybatisplusjoin.solon.plugin;

import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.github.yulichang.config.ConfigProperties;
import com.github.yulichang.config.MPJInterceptorConfig;
import com.github.yulichang.config.enums.IfExistsEnum;
import com.github.yulichang.config.enums.LogicDelTypeEnum;
import com.github.yulichang.extension.mapping.config.MappingConfig;
import com.github.yulichang.injector.MPJSqlInjector;
import com.github.yulichang.toolkit.SpringContentUtils;
import com.github.yulichang.toolkit.reflect.GenericTypeUtils;
import com.github.yulichang.wrapper.enums.IfExistsSqlKeyWordEnum;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.solon.MybatisAdapter;
import org.apache.ibatis.solon.integration.MybatisAdapterManager;
import org.noear.solon.Utils;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.Plugin;
import org.noear.solon.core.Props;
import org.noear.solon.core.event.AppLoadEndEvent;
import org.noear.solon.core.util.GenericUtil;

import javax.sql.DataSource;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

public class XPluginImpl implements Plugin {

    @Override
    public void start(AppContext context) {
        // MPJSqlInjector
        context.subWrapsOfType(DataSource.class, bw -> context.cfg().putIfAbsent(Utils.isEmpty(bw.name()) ?
                        "mybatis.globalConfig.sqlInjector" : ("mybatis." + bw.name() + ".globalConfig.sqlInjector"),
                MPJSqlInjector.class.getName()));
        // setGenericTypeResolver
        GenericTypeUtils.setGenericTypeResolver(GenericUtil::resolveTypeArguments);
        // SpringContext兼容
        SpringContentUtils.setSpringContext(new SpringContentUtils.SpringContext() {
            @Override
            public <T> T getBean(Class<T> clazz) {
                return context.getBean(clazz);
            }

            @Override
            public <T> void getBeansOfType(Class<T> clazz) {
                context.getBeansMapOfType(clazz);
            }
        });
        // 读取配置
        Prop prop = new Prop(context.cfg());
        ConfigProperties.banner = prop.get("banner", Boolean::parseBoolean);
        ConfigProperties.subTableLogic = prop.get("subTableLogic", Boolean::parseBoolean);
        ConfigProperties.msCache = prop.get("msCache", Boolean::parseBoolean);
        ConfigProperties.tableAlias = prop.get("tableAlias", Function.identity());
        ConfigProperties.joinPrefix = prop.get("joinPrefix", Function.identity());
        ConfigProperties.logicDelType = prop.get("logicDelType", val ->
                // fix on/off yes/no 会转为Boolean
                LogicDelTypeEnum.WHERE.name().equalsIgnoreCase(val) ? LogicDelTypeEnum.WHERE : LogicDelTypeEnum.ON);
        ConfigProperties.mappingMaxCount = prop.get("mappingMaxCount", Integer::parseInt);
        ConfigProperties.ifExists = prop.get("ifExists", val ->
                Arrays.stream(IfExistsEnum.values()).filter(e -> e.name().equalsIgnoreCase(val)).findFirst()
                        .map(m -> (BiPredicate<Object, IfExistsSqlKeyWordEnum>) (o, enums) -> m.test(o))
                        .orElseThrow(() -> ExceptionUtils.mpe("mybatis-plus-join.ifExists 配置错误")));
        // 后续操作
        context.onEvent(AppLoadEndEvent.class, e -> {
            List<SqlSessionFactory> sqlSessionFactoryList = MybatisAdapterManager.getAll().values().stream()
                    .map(MybatisAdapter::getFactory).collect(Collectors.toList());
            new MPJInterceptorConfig(sqlSessionFactoryList, ConfigProperties.banner);
            MappingConfig.init();
        });
    }

    private static class Prop {

        private final Properties props;

        public Prop(Props props) {
            this.props = props.entrySet().stream().filter(e -> format(e.getKey())
                    .startsWith(format("mybatis-plus-join."))).collect(Collectors.toMap(e -> format(e.getKey())
                    .substring(e.getKey().toString().indexOf(".") + 1)
                    .toUpperCase(Locale.ENGLISH), Map.Entry::getValue, (o, n) -> n, Properties::new));
        }

        @SuppressWarnings("unchecked")
        public <T> T get(String key, Function<String, T> convert) {
            try {
                return Optional.ofNullable(this.props.get(key.toUpperCase(Locale.ENGLISH)))
                        .map(v -> convert.apply(v.toString()))
                        .orElse((T) ConfigProperties.class.getDeclaredField(key).get(null));
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        private String format(Object key) {
            return Optional.ofNullable(key).map(k -> k.toString().replaceAll("[-_]", "")
                    .toUpperCase(Locale.ENGLISH)).orElse("");
        }
    }
}