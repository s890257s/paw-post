import request from './request';

// 使用者登入
// API 呼叫統一放在 api/ 資料夾，store 只負責管理狀態（分層職責）
export const login = (loginRequest) => {
    return request.post('/login', loginRequest);
};
