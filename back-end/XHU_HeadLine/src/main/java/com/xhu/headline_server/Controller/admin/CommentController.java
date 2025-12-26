package com.xhu.headline_server.Controller.admin;

import com.xhu.headline_server.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * 删除评论
     * 前端发送: { "id": 123 }
     */
    @PostMapping("/admin/comment/delete")
    public Map<String,Object> deleteComment(@RequestBody Map<String, Integer> params){
        Map<String,Object> res = new HashMap<>();

        // 从 Map 中获取 id
        Integer id = params.get("id");

        if (id == null) {
            res.put("code", 0);
            res.put("message", "参数 id 缺失");
            return res;
        }

        boolean success = commentService.deleteComment(id);
        if (success){
            res.put("code", 1);
            res.put("message", "删除评论成功");
        } else {
            res.put("code", 0);
            res.put("message", "删除评论失败");
        }
        return res;
    }
}