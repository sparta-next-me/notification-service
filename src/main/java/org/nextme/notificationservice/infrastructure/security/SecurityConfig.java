package org.nextme.notificationservice.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                // 3. 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // Actuator 경로는 인증 없이 접근 가능하도록 명시적 허용
                        .requestMatchers("/actuator/**").permitAll()
                        // 그 외 모든 요청도 현재는 허용 (테스트 단계)
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}