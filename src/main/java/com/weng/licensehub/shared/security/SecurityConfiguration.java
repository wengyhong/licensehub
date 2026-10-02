package com.weng.licensehub.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.List;

import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
public class SecurityConfiguration {
        @Bean
        CsrfTokenRepository csrfTokenRepository() {
                return new HttpSessionCsrfTokenRepository();
        }

        @Bean
        SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        SecurityContextRepository securityContextRepository,
                        CsrfTokenRepository csrfTokenRepository) throws Exception {

                AuthenticationEntryPoint unauthorized = new HttpStatusEntryPoint(
                                HttpStatus.UNAUTHORIZED);

                http
                                .securityContext(
                                                context -> context.securityContextRepository(securityContextRepository))
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/auth/register",
                                                                "/api/auth/login")
                                                .permitAll()
                                                .requestMatchers("/api/activations/**")
                                                .permitAll()
                                                .requestMatchers(
                                                                "/actuator/health",
                                                                "/actuator/health/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/auth/csrf")
                                                .permitAll()
                                                .anyRequest()
                                                .authenticated())
                                .csrf(csrf -> csrf
                                                .csrfTokenRepository(csrfTokenRepository)
                                                .ignoringRequestMatchers(
                                                                "/api/auth/register",
                                                                "/api/activations/**"))
                                .formLogin(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)
                                .logout(logout -> logout.logoutUrl
                                        (
                                                "/api/auth/logout"
                                        ).logoutSuccessHandler(
                                                                (request, response, authentication) -> response
                                                                                .setStatus(HttpStatus.NO_CONTENT
                                                                                                .value())

                                ).permitAll())
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint(unauthorized));


                return http.build();
        }

        @Bean
        AuthenticationManager authenticationManager(
                        UserDetailsService userDetailsService,
                        PasswordEncoder passwordEncoder) {

                DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);

                provider.setPasswordEncoder(passwordEncoder);

                return new ProviderManager(provider);
        }

        @Bean
        SecurityContextRepository securityContextRepository() {
                return new HttpSessionSecurityContextRepository();
        }

        @Bean
        SessionAuthenticationStrategy sessionAuthenticationStrategy(

                        CsrfTokenRepository csrfTokenRepository) {
                return new CompositeSessionAuthenticationStrategy(
                                List.of(
                                                new ChangeSessionIdAuthenticationStrategy(),
                                                new CsrfAuthenticationStrategy(
                                                                csrfTokenRepository)));
        }
}