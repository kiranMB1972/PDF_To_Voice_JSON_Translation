package com.org.pdfplatform.school.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SchoolConfig implements WebMvcConfigurer {

    private final SchoolAuthInterceptor interceptor;

    public SchoolConfig(SchoolAuthInterceptor interceptor) {
        this.interceptor = interceptor;
    }

        @Bean
    public static PasswordEncoder schoolPasswordEncoder() { // Bean for password encoding - al la claude
        return new BCryptPasswordEncoder();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/school/api/**", "/school/files/**");
    }
}
