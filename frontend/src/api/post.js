import request from './request';

// 取得貼文列表
export const getPosts = (page = 0, size = 10) => {
    return request.get('/posts', {
        params: { page, size }
    });
};

// 發布新照片
// 不需要指定 Content-Type：axios 看到 FormData 會自動設為 multipart/form-data 並補上 boundary
export const createPost = (formData) => {
    return request.post('/posts', formData);
};

// 對照片按讚
export const likePost = (postId) => {
    return request.post(`/posts/${postId}/likes`);
};

// 取消照片按讚
export const unlikePost = (postId) => {
    return request.delete(`/posts/${postId}/likes`);
};

// 隱藏貼文 (管理員)
export const hidePost = (postId) => {
    return request.put(`/admin/posts/${postId}/hide`);
};

// 解除隱藏貼文 (管理員)
export const unhidePost = (postId) => {
    return request.put(`/admin/posts/${postId}/unhide`);
};
