package tw.pers.allen.backend.core.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

// 設定全域 CORS 跨來源資源共用
//
// 【為什麼用 CorsFilter 而不是 WebMvcConfigurer.addCorsMappings？】
// addCorsMappings 的 CORS 處理發生在 Spring MVC（DispatcherServlet）層。
// 但本專案的 JwtAuthFilter 是 Servlet Filter，執行順序在 MVC「之前」——
// 當 JwtAuthFilter 直接回 401 時，回應根本沒進到 MVC，
// 也就不會被加上 Access-Control-Allow-Origin header。
// 瀏覽器收到沒有 CORS header 的回應會直接擋掉，
// 前端 axios 只會看到「網路錯誤」而不是 401，非常難除錯。
// 因此改用 CorsFilter（Servlet Filter 層級），並確保它排在 JwtAuthFilter 之前。
@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 教學專案允許所有來源方便本機開發。
        // 注意：這裡「不能」同時設定 setAllowCredentials(true)——
        // 「任意來源 + 允許憑證(cookie)」是危險組合，等於任何網站都能帶著
        // 使用者的 cookie 打你的 API。本專案的身分驗證走 Authorization header
        // （不是 cookie），因此完全不需要 credentials。
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        // 設為最高優先權，確保 CORS 處理永遠在 JwtAuthFilter 之前執行
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
