package uss.code.global.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uss.code.global.filter.SecurityFilter;

@Configuration
public class FilterChainConfig {

    @Bean
    public FilterRegistrationBean<SecurityFilter> securityFilter() {
        FilterRegistrationBean<SecurityFilter> bean = new FilterRegistrationBean<>();

        bean.setFilter(new SecurityFilter());
        
        return bean;
    }
}
