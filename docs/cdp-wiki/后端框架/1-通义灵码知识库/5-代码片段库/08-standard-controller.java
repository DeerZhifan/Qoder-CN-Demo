package com.leatop.example.controller;

import com.leatop.cdp.core.validate.AddGroup;
import com.leatop.cdp.core.validate.UpdateGroup;
import com.leatop.cdp.data.message.Message;
import com.leatop.cdp.data.model.Page;
import com.leatop.example.model.dto.NewsItemDto;
import com.leatop.example.model.qo.NewsItemPageQo;
import com.leatop.example.service.NewsItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controller 层示例
 *
 * 规范说明：
 * - Controller 只做：参数接收、参数校验、调用 Service、返回结果
 * - 不在 Controller 中写业务逻辑
 * - 返回值统一用 Message<T> 包装
 * - 新增用 AddGroup，修改用 UpdateGroup 做分组校验
 * - URL 路径使用下划线命名，模块/功能/操作三段式：/模块名/功能名/操作
 * - 每个方法写 JavaDoc 注释（供 Smart-doc 生成接口文档）
 */
@RestController
@RequestMapping("/news_research/news_item")
public class NewsItemController {

    @Autowired
    private NewsItemService newsItemService;

    /**
     * 新增
     *
     * @param dto 新增参数
     * @return 操作结果
     */
    @PostMapping("/add")
    public Message<Boolean> add(@RequestBody @Validated(AddGroup.class) NewsItemDto dto) {
        return newsItemService.add(dto);
    }

    /**
     * 修改
     *
     * @param dto 修改参数
     * @return 操作结果
     */
    @PostMapping("/update")
    public Message<Boolean> update(@RequestBody @Validated(UpdateGroup.class) NewsItemDto dto) {
        return newsItemService.update(dto);
    }

    /**
     * 根据 ID 查询详情
     *
     * @param id 主键 ID
     * @return 详情
     */
    @GetMapping("/queryById")
    public Message<NewsItemDto> queryById(@RequestParam String id) {
        return newsItemService.queryById(id);
    }

    /**
     * 删除（支持批量，多个 id 用 ',' 分隔）
     *
     * @param ids ID 列表
     * @return 操作结果
     */
    @PostMapping("/delete/{ids}")
    public Message<Boolean> delete(@PathVariable("ids") String ids) {
        return newsItemService.delete(ids);
    }

    /**
     * 分页查询
     *
     * @param qo 查询参数
     * @return 分页列表
     */
    @PostMapping("/listPage")
    public Message<Page<NewsItemDto>> listPage(@Validated NewsItemPageQo qo) {
        return newsItemService.listPage(qo);
    }
}
