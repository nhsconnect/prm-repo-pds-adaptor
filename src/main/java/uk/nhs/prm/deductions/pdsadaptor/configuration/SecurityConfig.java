package uk.nhs.prm.deductions.pdsadaptor.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import uk.nhs.prm.deductions.pdsadaptor.service.ReadSSMParameter;

import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${environment}")
    private String environment;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {

        if (!environment.equals("local") && !environment.equals("int-test")) {
            ReadSSMParameter ssmService = new ReadSSMParameter();
            Map<String, String> userMap = ssmService.getApiKeys(environment);

            userMap.forEach((username, apiKey) -> {
                try {
                    auth.inMemoryAuthentication()
                            .passwordEncoder(passwordEncoder())
                            .withUser(username)
                            .password(passwordEncoder().encode(apiKey))
                            .roles("USER");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            //this for local usage only
            auth.inMemoryAuthentication()
                    .passwordEncoder(passwordEncoder())
                    .withUser("admin")
                    .password(passwordEncoder().encode("admin"))
                    .roles("USER");
        }
    }

    private static final String[] AUTH_WHITELIST = {
            // -- Swagger UI v2
            "/v2/api-docs",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/actuator/**"
            // other public endpoints of your API may be appended to this array
    };

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable().
                httpBasic().and().
                authorizeRequests().
                antMatchers(AUTH_WHITELIST).permitAll().
                antMatchers("/**").authenticated();  // require authentication for any endpoint that's not whitelisted
    }

}
