package com.xhu.headline_server.mapper;

import com.xhu.headline_server.entity.newsPort;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NewsPortMapper {

    newsPort getById(@Param("id") Long id);

    List<newsPort> listAll();

    List<newsPort> listByAuthorId(@Param("authorId") Long authorId);

    int insert(newsPort post);

    int update(newsPort post);

    int deleteById(@Param("id") Long id);

    int incrViewCount(@Param("id") Long id);

    int incrLikeCount(@Param("id") Long id);

    int decrLikeCount(@Param("id") Long id);

    int incrCommentCount(@Param("id") Long id);
}
