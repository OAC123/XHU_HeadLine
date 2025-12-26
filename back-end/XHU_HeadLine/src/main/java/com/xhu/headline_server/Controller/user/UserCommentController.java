package com.xhu.headline_server.Controller.user;

import com.xhu.headline_server.entity.Comment;
import com.xhu.headline_server.service.impl.CommentServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class UserCommentController {

    @Autowired
    private CommentServiceImpl commentServiceImpl;

    @PostMapping("/user/comment/list")
    public Map<String, Object> listUserComments(@RequestParam("userId") Long userId) {
        Map<String, Object> res = new HashMap<>();
        try {
            List<Comment> list = commentServiceImpl.listUserComments(userId);
            res.put("code", 1);
            res.put("data", list);
            res.put("message", "评论查询成功");
        } catch (Exception e) {
            res.put("code", 0);
            res.put("message", "评论查询失败: " + e.getMessage());
        }
        return res;
    }
}
