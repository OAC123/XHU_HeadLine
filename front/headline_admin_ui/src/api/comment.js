import http from './http'

/**
 * 分页查询评论
 * 对应后端 GET /user/news/{id}/comments
 */
export const queryCommentPageApi = (params = {}) =>
  http.get(
    `/user/news/${params.articleId}/comments`,
    {
      params: {
        page: params.pageNum ?? 1,
        size: params.pageSize ?? 10,
      },
    }
  )

/**
 * 删除评论
 * 对应后端 POST /admin/comment/delete
 * 发送 JSON Body: { "id": 123 }
 */
export const deleteCommentApi = (id) =>
  http.post(
    '/admin/comment/delete',
    { id }, // 这里将 id 包装为对象发送
    {
      headers: { 'Content-Type': 'application/json' },
    }
  )