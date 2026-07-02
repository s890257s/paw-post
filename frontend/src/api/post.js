import request from './request';

// 取得貼文列表
export const getPosts = (page = 0, size = 10) => {
    return request.get('/posts', {
        params: { page, size }
    });
};

// 發布新照片
export const createPost = (formData) => {
    return request.post('/posts', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    });
};

// 對照片按讚
export const likePost = (postId) => {
    return request.post(`/posts/${postId}/likes`);
};

// 取消照片按讚
export const unlikePost = (postId) => {
    return request.delete(`/posts/${postId}/likes`);
};
