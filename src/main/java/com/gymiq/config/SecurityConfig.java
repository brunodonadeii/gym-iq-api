package com.gymiq.config;

import com.gymiq.security.JwtAuthEntryPoint;
import com.gymiq.security.JwtAuthFilter;
import com.gymiq.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http

            .csrf(AbstractHttpConfigurer::disable)


            .cors(cors -> cors.configurationSource(corsConfigurationSource()))


            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))


            .exceptionHandling(ex ->
                ex.authenticationEntryPoint(jwtAuthEntryPoint))


            .authorizeHttpRequests(auth -> auth


                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/registro").permitAll()


                .requestMatchers(HttpMethod.POST, "/api/alunos").hasAnyRole("ADMIN", "RECEPCAO")
                .requestMatchers(HttpMethod.GET,  "/api/alunos").hasAnyRole("ADMIN", "RECEPCAO", "INSTRUTOR")
                .requestMatchers(HttpMethod.GET,  "/api/alunos/**").hasAnyRole("ADMIN", "RECEPCAO", "INSTRUTOR", "ALUNO")
                .requestMatchers(HttpMethod.PUT,  "/api/alunos/**").hasAnyRole("ADMIN", "RECEPCAO")
                .requestMatchers(HttpMethod.DELETE, "/api/alunos/**").hasRole("ADMIN")


                .requestMatchers(HttpMethod.GET, "/api/planos").hasAnyRole("ADMIN", "RECEPCAO", "ALUNO")
                .requestMatchers(HttpMethod.POST, "/api/planos").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/planos/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/planos/**").hasRole("ADMIN")


                .requestMatchers(HttpMethod.POST, "/api/matriculas").hasAnyRole("ADMIN", "RECEPCAO")
                .requestMatchers(HttpMethod.GET,  "/api/matriculas/**").hasAnyRole("ADMIN", "RECEPCAO")
                .requestMatchers(HttpMethod.PATCH, "/api/matriculas/**").hasAnyRole("ADMIN", "RECEPCAO")


                .anyRequest().authenticated()
            )


            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "https://*.vercel.app"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
