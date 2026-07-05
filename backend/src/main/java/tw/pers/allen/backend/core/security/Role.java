package tw.pers.allen.backend.core.security;

// 使用者角色。
// 用 enum 取代散落各處的 "ADMIN" 魔法字串：
// 型別安全、可列舉所有合法值，拼錯字會在編譯期被抓到，而不是上線後才發現。
public enum Role {
    USER, ADMIN
}