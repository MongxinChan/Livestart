package com.mongxin.livestart.search.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mongxin.livestart.search.dao.entity.EventDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EventMapper extends BaseMapper<EventDO> {

    /**
     * 多维度过滤搜索演出
     * JOIN venue 表获取城市，JOIN ticket_skus 表获取最低价格
     *
     * @param keyword 关键词（title 模糊匹配）
     * @param eventType 演出类型：0=Livehouse, 1=演唱会，null=不限
     * @param city 城市过滤，null=不限
     * @param minPrice 价格下限，null=不限
     * @param maxPrice 价格上限，null=不限
     * @param offset 分页偏移量
     * @param limit 分页大小
     * @return 演出列表
     */
    @Select("<script>" +
            "SELECT DISTINCT e.* FROM t_event e " +
            "LEFT JOIN t_venue v ON e.venue_id = v.id " +
            "LEFT JOIN t_ticket_sku sku ON e.id = sku.event_id " +
            "WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND e.title LIKE CONCAT('%', #{keyword}, '%') " +
            "</if>" +
            "<if test='eventType != null'>" +
            "  AND e.event_type = #{eventType} " +
            "</if>" +
            "<if test='city != null and city != \"\"'>" +
            "  AND v.city LIKE CONCAT('%', #{city}, '%') " +
            "</if>" +
            "<if test='minPrice != null or maxPrice != null'>" +
            "  AND e.id IN (" +
            "    SELECT event_id FROM t_ticket_sku " +
            "    WHERE 1=1 " +
            "    <if test='minPrice != null'> AND selling_price &gt;= #{minPrice} </if>" +
            "    <if test='maxPrice != null'> AND selling_price &lt;= #{maxPrice} </if>" +
            "  ) " +
            "</if>" +
            "ORDER BY e.id DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<EventDO> searchEventsWithFilters(
            @Param("keyword") String keyword,
            @Param("eventType") Integer eventType,
            @Param("city") String city,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /**
     * 统计符合条件的演出总数（用于分页 total）
     */
    @Select("<script>" +
            "SELECT COUNT(DISTINCT e.id) FROM t_event e " +
            "LEFT JOIN t_venue v ON e.venue_id = v.id " +
            "LEFT JOIN t_ticket_sku sku ON e.id = sku.event_id " +
            "WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND e.title LIKE CONCAT('%', #{keyword}, '%') " +
            "</if>" +
            "<if test='eventType != null'>" +
            "  AND e.event_type = #{eventType} " +
            "</if>" +
            "<if test='city != null and city != \"\"'>" +
            "  AND v.city LIKE CONCAT('%', #{city}, '%') " +
            "</if>" +
            "<if test='minPrice != null or maxPrice != null'>" +
            "  AND e.id IN (" +
            "    SELECT event_id FROM t_ticket_sku " +
            "    WHERE 1=1 " +
            "    <if test='minPrice != null'> AND selling_price &gt;= #{minPrice} </if>" +
            "    <if test='maxPrice != null'> AND selling_price &lt;= #{maxPrice} </if>" +
            "  ) " +
            "</if>" +
            "</script>")
    long countEventsWithFilters(
            @Param("keyword") String keyword,
            @Param("eventType") Integer eventType,
            @Param("city") String city,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice
    );
}
