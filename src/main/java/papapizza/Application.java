/*
 * Copyright 2014-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package papapizza;


import org.salespointframework.EnableSalespoint;
import org.salespointframework.SalespointSecurityConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.nio.file.Paths;

@EnableSalespoint
@EnableCaching(proxyTargetClass = true)
public class Application {

	private static ApplicationContext appContext;

	private final Logger logger = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
		appContext = SpringApplication.run(Application.class, args);
	}

	//h2db in server mode -> connect via: jdbc:h2:tcp://localhost:9090/mem:test
	/*@Bean(initMethod = "start", destroyMethod = "stop")
	public Server h2Server() throws SQLException {
		return Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9090");
	}*/

	@Configuration
	static class WebSecurityConfiguration extends SalespointSecurityConfiguration {

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.csrf().disable();  // for lab purposes, that's ok!

			http.authorizeRequests().antMatchers("/**").access("isAuthenticated()").and()
					.formLogin().loginProcessingUrl("/login").defaultSuccessUrl("/",true).and()
					.logout().logoutUrl("/logout").logoutSuccessUrl("/");
			http.headers().frameOptions().disable(); //h2-console
		}

		@Override
		public void configure(WebSecurity web) {
			//bypass spring httpSecurity for h2 database
			web.ignoring().antMatchers("/h2db/**");
		}
	}

	@Configuration
	public class AppConfig implements WebMvcConfigurer {
		@Bean
		public ResourceBundleMessageSource messageSource(){
			var source = new ResourceBundleMessageSource();
			//validation messages otherwise not accessible by bindingResult.rejectValue(<field>,<errorCode>)
			source.setBasenames("ValidationMessages","messages");
			source.setDefaultEncoding("UTF-8");
			source.setUseCodeAsDefaultMessage(true);

			return source;
		}

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			URI resLoc = Paths.get(System.getenv("APPDATA"),"PapaPizza","Graph").toUri();
			logger.info("Graph res path:"+resLoc);
			registry.addResourceHandler("/img/graph/**").addResourceLocations(resLoc.toString());
		}
	}
}
