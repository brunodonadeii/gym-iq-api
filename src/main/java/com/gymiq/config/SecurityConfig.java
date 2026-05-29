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
                    
                .requestMatchers(HttpMethod.GET, "/api/health").permitAll()

                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/jobs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/jobs/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/presences/self-check-in").permitAll()


                .requestMatchers(HttpMethod.GET, "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")


                .requestMatchers(HttpMethod.POST, "/api/students").hasAnyRole("ADMIN", "RECEPTION")
                .requestMatchers(HttpMethod.GET,  "/api/students").hasAnyRole("ADMIN", "RECEPTION", "INSTRUCTOR")
                .requestMatchers(HttpMethod.GET,  "/api/students/address-by-zip-code/**").hasAnyRole("ADMIN", "RECEPTION")
                .requestMatchers(HttpMethod.GET,  "/api/students/**").hasAnyRole("ADMIN", "RECEPTION", "INSTRUCTOR", "STUDENT")
                .requestMatchers(HttpMethod.PUT,  "/api/students/**").hasAnyRole("ADMIN", "RECEPTION")
                .requestMatchers(HttpMethod.PATCH, "/api/students/**").hasRole("ADMIN")


                .requestMatchers(HttpMethod.GET, "/api/plans").hasAnyRole("ADMIN", "RECEPTION", "STUDENT")
                .requestMatchers(HttpMethod.GET, "/api/plans/**").hasAnyRole("ADMIN", "RECEPTION", "STUDENT")
                .requestMatchers(HttpMethod.POST, "/api/plans").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/plans/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/plans/**").hasRole("ADMIN")


                .requestMatchers(HttpMethod.POST, "/api/enrollments").hasAnyRole("ADMIN", "RECEPTION")
                .requestMatchers(HttpMethod.GET,  "/api/enrollments").hasAnyRole("ADMIN", "RECEPTION")
                .requestMatchers(HttpMethod.GET,  "/api/enrollments/**").hasAnyRole("ADMIN", "RECEPTION", "STUDENT")
                .requestMatchers(HttpMethod.PATCH, "/api/enrollments/**").hasAnyRole("ADMIN", "RECEPTION")
                .requestMatchers(HttpMethod.POST, "/api/enrollments/**").hasAnyRole("ADMIN", "RECEPTION")

                .requestMatchers(HttpMethod.GET,  "/api/payments").hasAnyRole("ADMIN", "RECEPTION")
                .requestMatchers(HttpMethod.GET,  "/api/payments/me").hasRole("STUDENT")
                .requestMatchers(HttpMethod.GET,  "/api/payments/**").hasAnyRole("ADMIN", "RECEPTION")
                .requestMatchers(HttpMethod.PATCH, "/api/payments/**").hasAnyRole("ADMIN", "RECEPTION")

                .requestMatchers(HttpMethod.POST, "/api/instructors").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,  "/api/instructors").hasAnyRole("ADMIN", "RECEPTION")
                .requestMatchers(HttpMethod.GET,  "/api/instructors/**").hasAnyRole("ADMIN", "RECEPTION", "INSTRUCTOR")
                .requestMatchers(HttpMethod.PUT,  "/api/instructors/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/instructors/**").hasRole("ADMIN")

                .requestMatchers(HttpMethod.POST, "/api/presences").hasAnyRole("ADMIN", "RECEPTION")
                .requestMatchers(HttpMethod.GET,  "/api/presences/me").hasRole("STUDENT")
                .requestMatchers(HttpMethod.GET,  "/api/presences/student/**").hasAnyRole("ADMIN", "RECEPTION")
                .requestMatchers(HttpMethod.GET,  "/api/presences/**").hasAnyRole("ADMIN", "RECEPTION")
                .requestMatchers(HttpMethod.PATCH, "/api/presences/**").hasAnyRole("ADMIN", "RECEPTION")

                .requestMatchers(HttpMethod.POST, "/api/exercises").hasAnyRole("ADMIN", "INSTRUCTOR")
                .requestMatchers(HttpMethod.GET,  "/api/exercises/**").hasAnyRole("ADMIN", "RECEPTION", "INSTRUCTOR", "STUDENT")
                .requestMatchers(HttpMethod.GET,  "/api/exercises").hasAnyRole("ADMIN", "RECEPTION", "INSTRUCTOR", "STUDENT")
                .requestMatchers(HttpMethod.PUT,  "/api/exercises/**").hasAnyRole("ADMIN", "INSTRUCTOR")
                .requestMatchers(HttpMethod.DELETE, "/api/exercises/**").hasAnyRole("ADMIN", "INSTRUCTOR")

                .requestMatchers(HttpMethod.POST, "/api/workout-sheets").hasAnyRole("ADMIN", "INSTRUCTOR")
                .requestMatchers(HttpMethod.POST, "/api/workout-sheets/**").hasAnyRole("ADMIN", "INSTRUCTOR")
                .requestMatchers(HttpMethod.GET,  "/api/workout-sheets/student/**").hasAnyRole("ADMIN", "INSTRUCTOR", "STUDENT")
                .requestMatchers(HttpMethod.GET,  "/api/workout-sheets/**").hasAnyRole("ADMIN", "INSTRUCTOR", "STUDENT")
                .requestMatchers(HttpMethod.GET,  "/api/workout-sheets").hasAnyRole("ADMIN", "INSTRUCTOR")
                .requestMatchers(HttpMethod.PUT,  "/api/workout-sheets/**").hasAnyRole("ADMIN", "INSTRUCTOR")
                .requestMatchers(HttpMethod.DELETE, "/api/workout-sheets/**").hasAnyRole("ADMIN", "INSTRUCTOR")
                .requestMatchers(HttpMethod.PUT,  "/api/workout-sheet-exercises/**").hasAnyRole("ADMIN", "INSTRUCTOR")
                .requestMatchers(HttpMethod.DELETE, "/api/workout-sheet-exercises/**").hasAnyRole("ADMIN", "INSTRUCTOR")

                .requestMatchers(HttpMethod.GET, "/api/retention-alerts/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/retention-alerts/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/retention-alerts/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/dashboard/**").hasRole("ADMIN")

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
                "https://gym-iq-web.vercel.app"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Job-Secret"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
