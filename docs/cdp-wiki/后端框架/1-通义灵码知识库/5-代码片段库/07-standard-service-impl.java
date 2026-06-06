package com.leatop.example.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.leatop.cdp.data.exception.BusException;
import com.leatop.cdp.data.message.Message;
import com.leatop.cdp.data.model.Page;
import com.leatop.cdp.util.BeanUtil;
import com.leatop.cdp.util.StrUtil;
import com.leatop.example.dao.NewsItemDao;
import com.leatop.example.model.dto.NewsItemDto;
import com.leatop.example.model.po.NewsItemPo;
import com.leatop.example.model.qo.NewsItemPageQo;
import com.leatop.example.service.NewsItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Service 实现层示例
 *
 * 规范说明：
 * - 继承 ServiceImpl<DAO, PO>，实现对应 Service 接口
 * - PO 与 DTO 之间转换用 BeanUtil.copyProperties / BeanUtil.copyToList（框架工具类）
 * - 业务异常统一抛 BusException，不用 RuntimeException
 * - 分页使用 PageHelper.startPage + Page.of 包装结果
 * - 条件查询用 LambdaQueryWrapper，避免硬编码字段名
 * - 删除支持批量：StrUtil.split 分割 ids，再 removeBatchByIds
 */
@Service
public class NewsItemServiceImpl extends ServiceImpl<NewsItemDao, NewsItemPo>
        implements NewsItemService {

    @Override
    public Message<Boolean> add(NewsItemDto dto) {
        NewsItemPo po = BeanUtil.copyProperties(dto, NewsItemPo.class);
        return Message.success(save(po));
    }

    @Override
    public Message<Boolean> update(NewsItemDto dto) {
        NewsItemPo po = BeanUtil.copyProperties(dto, NewsItemPo.class);
        return Message.success(updateById(po));
    }

    @Override
    public Message<NewsItemDto> queryById(String id) {
        NewsItemPo po = getById(id);
        if (Objects.isNull(po)) {
            throw new BusException("数据不存在，id=" + id);
        }
        return Message.success(BeanUtil.copyProperties(po, NewsItemDto.class));
    }

    @Override
    @Transactional
    public Message<Boolean> delete(String ids) {
        List<String> idList = StrUtil.split(ids, ',');
        if (CollUtil.isEmpty(idList)) {
            throw new BusException("ids 不能为空");
        }
        return Message.success(removeBatchByIds(idList));
    }

    @Override
    public Message<Page<NewsItemDto>> listPage(NewsItemPageQo qo) {
        com.github.pagehelper.Page<NewsItemPo> page =
                PageHelper.startPage(qo.getPage(), qo.getSize());

        LambdaQueryWrapper<NewsItemPo> wrapper = new LambdaQueryWrapper<>();
        // 模糊查询：字段不为空时才加条件
        wrapper.like(StrUtil.isNotBlank(qo.getTitle()), NewsItemPo::getTitle, qo.getTitle());
        wrapper.like(StrUtil.isNotBlank(qo.getSource()), NewsItemPo::getSource, qo.getSource());
        // 精确查询
        wrapper.eq(Objects.nonNull(qo.getStatus()), NewsItemPo::getStatus, qo.getStatus());
        // 排序
        wrapper.orderByDesc(NewsItemPo::getCreateGmt);

        List<NewsItemPo> list = getBaseMapper().selectList(wrapper);
        List<NewsItemDto> results = BeanUtil.copyToList(list, NewsItemDto.class);
        return Message.success(Page.of(page.getPageNum(), page.getPageSize(), page.getTotal(), results));
    }
}
