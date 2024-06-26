package com.github.yulichang.extension.mapping.config;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.github.yulichang.extension.mapping.mapper.MPJTableInfoHelper;
import com.github.yulichang.toolkit.MPJTableMapperHelper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * 关系映射配置
 *
 * @author yulichang
 * @since 1.2.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MappingConfig {

    public static void init() {
        TableInfoHelper.getTableInfos().stream().filter(f ->
                Objects.nonNull(TableInfoHelper.getTableInfo(f.getEntityType()))).forEach(i ->
                MPJTableInfoHelper.initTableInfo(i.getEntityType(), MPJTableMapperHelper.getMapper(i.getEntityType())));
    }
}
