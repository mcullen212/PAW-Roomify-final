package ar.edu.itba.paw.webapp.config;

import ar.edu.itba.paw.webapp.security.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import javax.ws.rs.HttpMethod;

import ar.edu.itba.paw.webapp.security.OTP.OtpFilter;
import ar.edu.itba.paw.webapp.security.basic.BasicAuthFilter;
import ar.edu.itba.paw.webapp.security.handlers.UnauthorizedRequestHandler;
import ar.edu.itba.paw.webapp.security.handlers.VerificationAccessDeniedHandler;
import ar.edu.itba.paw.webapp.security.jwt.JwtTokenFilter;
import ar.edu.itba.paw.webapp.security.jwt.JwtTokenUtil;
import ar.edu.itba.paw.webapp.security.ratelimit.RateLimitServletFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@ComponentScan("ar.edu.itba.paw.webapp.security")
public class WebAuthConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private PawUserDetailsService userDetailsService;

    @Autowired
    private JwtTokenFilter jwtTokenFilter;

    @Autowired
    private BasicAuthFilter basicAuthFilter;

    @Autowired
    private OtpFilter otpFilter;

    @Autowired
    private RateLimitServletFilter rateLimitServletFilter;

    @Autowired
    private VerificationAccessDeniedHandler verificationAccessDeniedHandler;

    @Value("${cors.url}")
    private String corsUrl;

    @Bean
    public JwtTokenUtil jwtTokenUtil(
        @Value("classpath:jwt.key") Resource jwtKeyResource,
        @Value("classpath:refresh.key") Resource refreshKeyResource
    ) throws IOException {
        return new JwtTokenUtil(jwtKeyResource, refreshKeyResource);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth)
        throws Exception {
        super.configure(auth);
        auth
            .userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        http
            .addFilterBefore(rateLimitServletFilter, UsernamePasswordAuthenticationFilter.class)
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .exceptionHandling()
            .authenticationEntryPoint(new UnauthorizedRequestHandler())
            .accessDeniedHandler(verificationAccessDeniedHandler)
            .and()
            .authorizeRequests()
            .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .antMatchers(
                "/",
                "/css/**",
                "/js/**",
                "/img/**",
                "/favicon.ico",
                "/favicon.png",
                "/index.html",
                "/token/**"
            )
            .permitAll()
            .antMatchers(HttpMethod.HEAD, "/api/")
            .permitAll()
            .antMatchers(HttpMethod.POST, "/api/users")
            .permitAll()
            .antMatchers(HttpMethod.GET, "/api/rooms/**")
            .permitAll()
            .antMatchers(HttpMethod.GET, "/api/users/**")
            .permitAll()
            .antMatchers(HttpMethod.GET, "/api/images/**")
            .permitAll()
            .antMatchers(HttpMethod.GET, "/api/reviews")
            .permitAll()
            .antMatchers(HttpMethod.GET, "/api/")
            .authenticated()
            .antMatchers("/api/**")
            .authenticated()
            .and()
            .cors()
            .and()
            .csrf()
            .disable()
            .addFilterBefore(
                basicAuthFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            .addFilterAfter(
                otpFilter,
                BasicAuthFilter.class
            )
            .addFilterAfter(
                jwtTokenFilter,
                OtpFilter.class
            )
        ;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.setAllowedOrigins(
            Collections.singletonList(corsUrl)
        );
        configuration.setAllowedMethods(
            Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
        );
        configuration.addAllowedHeader("*");

        configuration.setExposedHeaders(
            Arrays.asList(
                "Link",
                "Location",
                "access-token",
                "refresh-token"
            )
        );

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Override
    public void configure(final WebSecurity web) throws Exception {
        web
            .ignoring()
            .antMatchers(
                "/static/**",
                "/index.html",
                "/assets/**",
                "/css/**",
                "/js/**",
                "/img/**",
                "/favicon.ico"
            );
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new UnauthorizedRequestHandler();
    }
}
