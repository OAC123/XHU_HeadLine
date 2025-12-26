package com.xhu.headline_server.mapper;

import com.xhu.headline_server.entity.Comment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommentMapper {
    int insertComment(Comment comment);

    int delById(@Param("id") Integer id);

    Long findPostIdByCommentId(@Param("id") Integer id);

    int incCommentCount(@Param("postId") Long postId, @Param("delta") int delta);

    // 新增：分页按帖子查询评论
    List<Comment> listCommentsByPostId(@Param("postId") long postId,
                                       @Param("offset") int offset,
                                       @Param("size") int size);
}
