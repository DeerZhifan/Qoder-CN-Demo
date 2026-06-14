package com.kb.manager.example;

import com.kb.manager.common.Result;
import lombok.Data;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * 通义灵码知识库列表 API 调用示例
 * 
 * API: GET /oapi/v1/lingma/organizations/{organizationId}/knowledgeBases
 * 文档: https://help.aliyun.com/zh/lingma/developer-reference/listkbs-get-the-list-of-knowledge-bases
 */
public class ListKbFilesExample {

    /**
     * 请求参数 DTO
     */
    @Data
    public static class ListKbsRequest {
        /**
         * 所属组织ID（必选）
         */
        private String organizationId;

        /**
         * 知识库名称模糊查询（可选）
         */
        private String query;

        /**
         * 排序列：gmtCreated/gmtModified（可选）
         */
        private String orderBy;

        /**
         * 排序顺序：desc/asc（可选）
         */
        private String sort;

        /**
         * 当前页，默认1（可选）
         */
        private Integer page;

        /**
         * 每页数据条数，默认20，范围[1,20]（可选）
         */
        private Integer perPage;
    }

    /**
     * 知识库信息
     */
    @Data
    public static class KbInfo {
        /**
         * 知识库 ID
         */
        private String kbId;

        /**
         * 知识库名称
         */
        private String name;

        /**
         * 知识库描述
         */
        private String description;

        /**
         * 所属组织 ID
         */
        private String organizationId;

        /**
         * 创建时间（时间戳）
         */
        private Long gmtCreated;

        /**
         * 修改时间（时间戳）
         */
        private Long gmtModified;

        /**
         * 文件数量
         */
        private Integer fileCount;
    }

    /**
     * 分页响应头信息
     */
    @Data
    public static class PaginationInfo {
        /**
         * 下一页
         */
        private Integer nextPage;

        /**
         * 当前页
         */
        private Integer page;

        /**
         * 每页大小
         */
        private Integer perPage;

        /**
         * 前一页
         */
        private Integer prevPage;

        /**
         * 总数
         */
        private Integer total;

        /**
         * 总分页数
         */
        private Integer totalPages;
    }

    /**
     * 封装后的响应结果
     */
    @Data
    public static class ListKbsResponse {
        /**
         * 知识库列表
         */
        private List<KbInfo> knowledgeBases;

        /**
         * 分页信息
         */
        private PaginationInfo pagination;
    }

    /**
     * 调用 ListKbs API
     *
     * @param domain      服务接入点（如：openapi-rdc.aliyuncs.com）
     * @param token       个人访问令牌
     * @param request     请求参数
     * @return 封装后的响应结果
     */
    public static Result<ListKbsResponse> listKbs(String domain, String token, ListKbsRequest request) {
        RestTemplate restTemplate = new RestTemplate();

        // 构建 URL
        String url = String.format(
                "https://%s/oapi/v1/lingma/organizations/%s/knowledgeBases",
                domain,
                request.getOrganizationId()
        );

        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-yunxiao-token", token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            // 使用 ParameterizedTypeReference 处理泛型类型
            ResponseEntity<List<KbInfo>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<KbInfo>>() {}
            );

            // 解析响应体
            List<KbInfo> knowledgeBases = response.getBody();
            if (knowledgeBases == null) {
                knowledgeBases = Collections.emptyList();
            }

            // 解析分页响应头
            PaginationInfo pagination = parsePaginationHeaders(response.getHeaders());

            // 封装响应结果
            ListKbsResponse resultData = new ListKbsResponse();
            resultData.setKnowledgeBases(knowledgeBases);
            resultData.setPagination(pagination);

            return Result.success(resultData);

        } catch (Exception e) {
            // 异常处理
            return Result.error(500, "调用 ListKbs API 失败: " + e.getMessage());
        }
    }


    /**
     * 解析分页响应头
     */
    private static PaginationInfo parsePaginationHeaders(HttpHeaders headers) {
        PaginationInfo pagination = new PaginationInfo();
        pagination.setNextPage(getHeaderInt(headers, "x-next-page"));
        pagination.setPage(getHeaderInt(headers, "x-page"));
        pagination.setPerPage(getHeaderInt(headers, "x-per-page"));
        pagination.setPrevPage(getHeaderInt(headers, "x-prev-page"));
        pagination.setTotal(getHeaderInt(headers, "x-total"));
        pagination.setTotalPages(getHeaderInt(headers, "x-total-pages"));
        return pagination;
    }

    /**
     * 从响应头中获取整数类型值
     */
    private static Integer getHeaderInt(HttpHeaders headers, String headerName) {
        List<String> values = headers.get(headerName);
        if (values != null && !values.isEmpty()) {
            try {
                return Integer.parseInt(values.get(0));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 使用示例
     */
    public static void main(String[] args) {
        // 配置参数
        String domain = "openapi-rdc.aliyuncs.com";
        String token = "pt-4LRst19uqcSwmplDMAR93XeY_3f6f69b4-6969-4084-8461-f787a8b54470";

        // 构建请求
        ListKbsRequest request = new ListKbsRequest();
        request.setOrganizationId("66aafc5fd623b2f55d81b443");
        request.setQuery("Lingma");          // 可选：知识库名称模糊查询
        request.setOrderBy("gmtCreated");    // 可选：按创建时间排序
        request.setSort("desc");             // 可选：倒序
        request.setPage(1);                  // 可选：第1页
        request.setPerPage(20);              // 可选：每页20条

        // 调用 API
        Result<ListKbsResponse> result = listKbs(domain, token, request);

        // 处理结果
        if (result.getCode() == 200) {
            ListKbsResponse response = result.getData();
            System.out.println("成功获取知识库列表，共 " + response.getPagination().getTotal() + " 个知识库");
            for (KbInfo kb : response.getKnowledgeBases()) {
                System.out.println("知识库名称: " + kb.getName() + ", ID: " + kb.getKbId() + ", 文件数: " + kb.getFileCount());
            }
        } else {
            System.err.println("调用失败: " + result.getMessage());
        }
    }
}
