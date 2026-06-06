package com.leatop.example.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leatop.example.model.po.NewsItemPo;
import org.apache.ibatis.annotations.Mapper;

/**
 * DAO 层示例 — 继承 MyBatis-Plus BaseMapper 获得基础 CRUD 能力
 *
 * 规范说明：
 * - 接口加 @Mapper 注解，或在主类 @MapperScan 中统一扫描（推荐后者）
 * - 继承 BaseMapper<PO类型>，无需写任何代码即可使用 insert/delete/update/select
 * - 复杂 SQL 在此接口中新增方法，SQL 写在对应 XML 文件中
 * - XML 文件路径：src/main/resources/mapper/XxxDao.xml
 */
@Mapper
public interface NewsItemDao extends BaseMapper<NewsItemPo> {

    // 简单查询直接用 BaseMapper 提供的方法，复杂查询在这里声明，XML 中实现
    // 示例：List<NewsItemDto> selectByCondition(@Param("qo") NewsItemPageQo qo);
}
