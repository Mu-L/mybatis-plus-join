package com.github.yulichang.test.join;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.query.MPJQueryWrapper;
import com.github.yulichang.test.join.dto.UserDTO;
import com.github.yulichang.test.join.entity.UserDO;
import com.github.yulichang.test.join.entity.UserTenantDO;
import com.github.yulichang.test.join.mapper.UserMapper;
import com.github.yulichang.test.join.mapper.UserTenantMapper;
import com.github.yulichang.test.util.ThreadLocalUtils;
import com.github.yulichang.toolkit.JoinWrappers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.BadSqlGrammarException;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@SpringBootTest
class QueryWrapperTest {
    @Resource
    private UserMapper userMapper;

    @Resource
    private UserTenantMapper userTenantMapper;


    /**
     * 链表查询
     */
    @Test
    void test1() {
        UserDTO dto = userMapper.selectJoinOne(UserDTO.class, new MPJQueryWrapper<UserDO>()
                .selectAll(UserDO.class)
                .select("name AS nameName")
                .last("LIMIT 1"));
        System.out.println(dto);


        IPage<UserDTO> iPage1 = userTenantMapper.selectJoinPage(new Page<>(1, 10), UserDTO.class,
                JoinWrappers.query(UserTenantDO.class)
                        .selectAll(UserTenantDO.class)
                        .select("t1.name as PName")
                        .leftJoin("(select * from `user` where id <> -1) t1 on t1.id = t.user_id")
                        .apply("t.id <> -1"));

        iPage1.getRecords().forEach(System.out::println);
    }

    /**
     * 链表查询
     */
    @Test
    void table() {
        ThreadLocalUtils.set("SELECT t.id,t.pid,t.`name`,t.`json`,t.sex,t.head_img,t.create_time,t.address_id,t.address_id2,t.del,t.create_by,t.update_by,name AS nameName FROM fwear t WHERE t.del=false LIMIT 1",
                "SELECT t.id,t.pid,t.`name`,t.`json`,t.sex,t.head_img AS img,t.create_time,t.address_id,t.address_id2,t.del,t.create_by,t.update_by,name AS nameName FROM fwear t WHERE t.del=false LIMIT 1");
        MPJQueryWrapper<UserDO> wrapper = new MPJQueryWrapper<UserDO>()
                .selectAll(UserDO.class)
                .setTableName(name -> "fwear")
                .select("name AS nameName")
                .last("LIMIT 1");
        try {
            userMapper.selectJoinOne(UserDTO.class, wrapper);
        } catch (BadSqlGrammarException ignored) {
        }
    }

    @Test
    void test2() {
        List<UserDO> userDO = userMapper.selectJoinList(UserDO.class, new MPJQueryWrapper<UserDO>()
                .selectAll(UserDO.class)
                .leftJoin("address t2 on t2.user_id = t.id")
                .le("t.id ", 10));
        System.out.println(userDO);

        List<UserDTO> dto = userMapper.selectJoinList(UserDTO.class, new MPJQueryWrapper<UserDO>()
                .selectAll(UserDO.class)
                .select("t2.address AS userAddress")
                .leftJoin("address t2 on t2.user_id = t.id")
                .le("t.id ", 10));
        System.out.println(dto);
    }

    @Test
    void test3() {
        IPage<UserDO> userDO = userMapper.selectJoinPage(new Page<>(1, 10), UserDO.class, new MPJQueryWrapper<UserDO>()
                .selectAll(UserDO.class)
                .leftJoin("address t2 on t2.user_id = t.id")
                .lt("t.id ", 5));
        System.out.println(userDO);

        IPage<UserDTO> dto = userMapper.selectJoinPage(new Page<>(1, 10), UserDTO.class, new MPJQueryWrapper<UserDO>()
                .selectAll(UserDO.class)
                .select("t2.address AS userAddress")
                .leftJoin("address t2 on t2.user_id = t.id")
                .lt("t.id ", 5));
        System.out.println(dto);
    }

    @Test
    void test4() {
        List<Map<String, Object>> maps = userMapper.selectJoinMaps(new MPJQueryWrapper<UserDO>()
                .selectAll(UserDO.class)
                .leftJoin("address t2 on t2.user_id = t.id")
                .lt("t.id ", 5));
        System.out.println(maps);

        List<Map<String, Object>> joinMaps = userMapper.selectJoinMaps(new MPJQueryWrapper<UserDO>()
                .selectAll(UserDO.class)
                .select("t2.address AS userAddress")
                .leftJoin("address t2 on t2.user_id = t.id")
                .lt("t.id ", 5));
        System.out.println(joinMaps);
    }

    @Test
    void test5() {
        ThreadLocalUtils.set("SELECT t.id, t.pid, t.`name`, t.`json`, t.sex, t.head_img, t.create_time, t.address_id, " +
                "t.address_id2, t.del, t.create_by, t.update_by FROM `user` t LEFT JOIN address t2 ON t2.user_id = t.id " +
                "WHERE t.del = false AND (t.id <= ?)");
        List<UserDO> userDO = userMapper.selectJoinList(UserDO.class, new MPJQueryWrapper<UserDO>()
                .selectAll(UserDO.class)
                .leftJoin("address t2 on t2.user_id = t.id")
                .le("t.id ", 10).lambda());
        System.out.println(userDO);

        ThreadLocalUtils.set("SELECT t.id, t.pid, t.`name`, t.`json`, t.sex, t.head_img, t.create_time, t.address_id, " +
                "t.address_id2, t.del, t.create_by, t.update_by, t2.address AS userAddress FROM `user` t " +
                "LEFT JOIN address t2 ON t2.user_id = t.id WHERE t.del = false AND (t.id <= ?)");
        List<UserDTO> dto = userMapper.selectJoinList(UserDTO.class, new MPJQueryWrapper<UserDO>()
                .selectAll(UserDO.class)
                .select("t2.address AS userAddress")
                .leftJoin("address t2 on t2.user_id = t.id")
                .le("t.id ", 10).lambda());
        System.out.println(dto);
    }

    @Test
    void test6() {
        ThreadLocalUtils.set("SELECT t.id AS idea, t.user_id AS uuid, t.tenant_id FROM user_tenant t WHERE (t.id <= ?) AND t.tenant_id = 1");
        List<UserTenantDO> userDO = userTenantMapper.selectJoinList(UserTenantDO.class, new MPJQueryWrapper<UserTenantDO>()
                .selectAll(UserTenantDO.class)
                .le("t.id ", 10).lambda());
        System.out.println(userDO);
    }

}
