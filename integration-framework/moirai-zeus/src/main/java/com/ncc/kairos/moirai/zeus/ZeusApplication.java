package com.ncc.kairos.moirai.zeus;

import com.ncc.kairos.moirai.zeus.dao.JwtRoleRepository;
import com.ncc.kairos.moirai.zeus.model.JwtRole;
import com.ncc.kairos.moirai.zeus.model.JwtUser;
import com.ncc.kairos.moirai.zeus.services.KairosUserService;
import com.ncc.kairos.moirai.zeus.utililty.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is the entry point to the Zeus Springboot application.
 *
 * @author vince charming
 * @author ryan scott
 * @version 0.1
 */

@SpringBootApplication
@EnableScheduling
@ServletComponentScan("com.ncc.kairos.moirai.zeus.security")
@SuppressWarnings("HideUtilityClassConstructor")
public class ZeusApplication {

    @Autowired
    private KairosUserService kairosUserService;

    @Value("${application.admin.secret}")
    private String adminSecret;

    @Autowired
    private JwtRoleRepository jwtRoleRepository;

    private static final Logger log = LoggerFactory.getLogger(ZeusApplication.class);

    /**
     * Main method to start the zeus springboot application.
     *
     * @param args Command line arguments, currently none are expected.
     */
    public static void main(String[] args) {
        SpringApplication.run(ZeusApplication.class, args);

        //Display the location of swagger-ui to enable easier development.
        log.debug("Swagger-ui's location is at: http://localhost:8000/swagger/index.html");
    }

    @Bean
    @ConditionalOnProperty(name = "kairos.cors.enabled")
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Value("${kairos.cors.api.allowed-origins}")
            private String apiAllowedOrigins;

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(this.apiAllowedOrigins.split(","))
                        .allowedMethods("PUT", "DELETE", "GET", "POST", "OPTIONS", "HEAD")
                        .allowedHeaders("*");
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry
                        .addResourceHandler("/static/**")
                        .addResourceLocations("/static/");
            }
        };
    }

    @Bean
    CommandLineRunner commandLineRunner() {
        return args -> {
            String username = "admin";

            JwtUser jwtAdmin = this.kairosUserService.findUserByUsername(username);
            if (jwtAdmin == null) {
                jwtAdmin = new JwtUser();
                jwtAdmin.setUsername(username);
                jwtAdmin.setPassword(PasswordUtil.getSecurePassword(this.adminSecret)); // If we are given a password it will get over riden later.
                jwtAdmin.setEmailAddress("generic.email@host.com");
                jwtAdmin.setTeamName("Moirai");
                jwtAdmin.setPerformerGroup("ADMIN");
            }

            List<JwtRole> adminRole = new ArrayList<>();
            adminRole.add(this.jwtRoleRepository.findByName("ADMIN"));
            jwtAdmin.setRoles(adminRole);
            this.kairosUserService.saveUser(jwtAdmin);
        };
    }
}
