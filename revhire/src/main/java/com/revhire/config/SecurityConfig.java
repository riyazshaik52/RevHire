package com.revhire.config;

import com.revhire.security.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
        return authBuilder.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                // Public pages - accessible to everyone
                .antMatchers("/", "/home", "/auth/**", "/css/**", "/js/**", "/images/**", "/h2-console/**").permitAll()
                
                // Job Seeker only pages
                .antMatchers("/jobseeker/**").hasAuthority("ROLE_JOBSEEKER")
                .antMatchers("/profile/**").hasAnyAuthority("ROLE_JOBSEEKER", "ROLE_EMPLOYER")
                .antMatchers("/resume/download/**").hasAnyAuthority("ROLE_JOBSEEKER", "ROLE_EMPLOYER")
                .antMatchers("/resume/**").hasAuthority("ROLE_JOBSEEKER")
                .antMatchers("/applications/**").hasAuthority("ROLE_JOBSEEKER")
                
                // Employer only pages
                .antMatchers("/employer/**").hasAuthority("ROLE_EMPLOYER")
                .antMatchers("/jobs/post/**").hasAuthority("ROLE_EMPLOYER")
                .antMatchers("/jobs/edit/**").hasAuthority("ROLE_EMPLOYER")
                .antMatchers("/jobs/delete/**").hasAuthority("ROLE_EMPLOYER")
                .antMatchers("/jobs/close/**").hasAuthority("ROLE_EMPLOYER")
                .antMatchers("/jobs/reopen/**").hasAuthority("ROLE_EMPLOYER")
                .antMatchers("/jobs/mark-filled/**").hasAuthority("ROLE_EMPLOYER")
                
                // All other pages require authentication
                .anyRequest().authenticated()
                
            .and()
            .formLogin()
                .loginPage("/auth/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/auth/login?error=true")
                .permitAll()
                
            .and()
            .logout()
                .logoutSuccessUrl("/auth/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
                
            .and()
            .csrf().disable() // For H2 console
            .headers().frameOptions().disable(); // For H2 console

        return http.build();
    }
}
