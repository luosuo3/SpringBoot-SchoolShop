package com.squirrel.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class MyWebMvcConfigurerAdapter extends WebMvcConfigurerAdapter {

    private static final String UPLOAD_IMAGE_PATH = "file:/Users/wangzheng/myproject/ideaproject/springboot-squirrel/src/main/resources/static/img/upload/";
//    private static final String UPLOAD_IMAGE_PATH = "file:/root/Java/data/springboot-squirrel/upload/images/";

    /**
     * 配置静态访问资源
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //自定义项目内目录
        //registry.addResourceHandler("/my/**").addResourceLocations("classpath:/my/");
        //指向外部目录
        registry.addResourceHandler("/img/upload/**").addResourceLocations(UPLOAD_IMAGE_PATH);
        super.addResourceHandlers(registry);
    }
}
