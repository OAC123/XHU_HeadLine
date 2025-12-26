// Java
package com.xhu.headline_server.service.impl;

import com.xhu.headline_server.entity.Comment;
import com.xhu.headline_server.mapper.CommentMapper;
import com.xhu.headline_server.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Override
    @Transactional
    public boolean addComment(Comment comment) {
        int rows = commentMapper.insertComment(comment);
        if (rows > 0) {
            commentMapper.incCommentCount(comment.getPostId(), 1);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean deleteComment(int commentId) {
        Long postId = commentMapper.findPostIdByCommentId(commentId);
        if (postId == null) {
            return false;
        }
        int rows = commentMapper.delById(commentId);
        if (rows > 0) {
            commentMapper.incCommentCount(postId, -1);
            return true;
        }
        return false;
    }
}
