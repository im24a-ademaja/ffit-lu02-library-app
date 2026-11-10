package ch.bzz;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final int HTTP_PORT = 8080;
    private static final int HTTPS_PORT = 8443;
    private static final int PERMANENT_REDIRECT_STATUS = 308;
    private static final String FRONTEND_ORIGIN =
            "https://alexanderpeter.github.io";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(FRONTEND_ORIGIN)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> httpRedirectConnector() {
        return factory -> {
            Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            connector.setScheme("http");
            connector.setPort(HTTP_PORT);
            connector.setSecure(false);
            connector.setRedirectPort(HTTPS_PORT);
            factory.addAdditionalTomcatConnectors(connector);
        };
    }

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> httpsRedirectFilter() {
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain) throws ServletException, IOException {
                if (request.getLocalPort() == HTTP_PORT) {
                    try {
                        URI redirect = new URI(
                                "https",
                                null,
                                request.getServerName(),
                                HTTPS_PORT,
                                request.getRequestURI(),
                                request.getQueryString(),
                                null);
                        response.setStatus(PERMANENT_REDIRECT_STATUS);
                        response.setHeader("Location", redirect.toASCIIString());
                    } catch (URISyntaxException exception) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    }
                    return;
                }
                filterChain.doFilter(request, response);
            }
        });
        registration.addUrlPatterns("/*");
        registration.setOrder(Integer.MIN_VALUE);
        return registration;
    }
}
