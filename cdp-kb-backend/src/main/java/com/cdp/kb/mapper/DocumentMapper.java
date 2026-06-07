package com.cdp.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdp.kb.entity.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
    
    @Select("SELECT * FROM document WHERE category_id = #{categoryId} AND status = #{status} AND deleted = 0")
    List<Document> selectByCategoryAndStatus(@Param("categoryId") Long categoryId, 
                                              @Param("status") Integer status);
}
