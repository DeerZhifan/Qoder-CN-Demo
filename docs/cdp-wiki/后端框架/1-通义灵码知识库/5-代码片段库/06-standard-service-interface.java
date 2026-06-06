package com.leatop.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.leatop.cdp.data.message.Message;
import com.leatop.cdp.data.model.Page;
import com.leatop.example.model.dto.NewsItemDto;
import com.leatop.example.model.po.NewsItemPo;
import com.leatop.example.model.qo.NewsItemPageQo;

/**
 * Service 接口示例
 *
 * 规范说明：
 * - 继承 IService<PO类型>，获得 MyBatis-Plus 提供的基础 CRUD 能力（save/update/remove/get/list 等）
 * - 业务方法返回值统一用 Message<T> 包装
 * - 分页返回 Message<Page<DTO>>
 * - 接口只声明方法签名，不写实现逻辑
 */
public interface NewsItemService extends IService<NewsItemPo> {

    /**
     * 新增
     */
    Message<Boolean> add(NewsItemDto dto);

    /**
     * 修改
     */
    Message<Boolean> update(NewsItemDto dto);

    /**
     * 根据 ID 查询详情
     */
    Message<NewsItemDto> queryById(String id);

    /**
     * 删除（支持批量，多个 id 用 ',' 分隔）
     */
    Message<Boolean> delete(String ids);

    /**
     * 分页查询
     */
    Message<Page<NewsItemDto>> listPage(NewsItemPageQo qo);
}
