import request from '@/api/request';

// 使用者登入
// API 呼叫統一放在 api/ 資料夾，store 只負責管理狀態——各層各司其職
export const login = (loginRequest) => {
    return request.post('/login', loginRequest);
};
