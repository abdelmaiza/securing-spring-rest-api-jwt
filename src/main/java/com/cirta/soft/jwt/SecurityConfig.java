package com.cirta.soft.jwt;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

import static org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure;
import static org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success;
import static org.springframework.security.oauth2.server.resource.BearerTokenErrorCodes.INVALID_TOKEN;


@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests(a ->{
					a.antMatchers("/h2-console/**").permitAll();
					a.anyRequest().authenticated();})
				.oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter()))
				);
		http.csrf().disable();
		http.headers().frameOptions().disable();
	}

	private JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtAuthenticationConverter authenticationConverter =
				new JwtAuthenticationConverter();
		JwtGrantedAuthoritiesConverter defaults =
				new JwtGrantedAuthoritiesConverter();
		authenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
			Collection<GrantedAuthority> authorities = defaults.convert(jwt);
			UUID userId = jwt.getClaim("user_id");
			UUID carolId = UUID.fromString("328167d1-2da3-5f7a-86d7-96b4376af2c0");
			if (carolId.equals(userId)) {
				authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
			}
			return authorities;
		});
		return authenticationConverter;
	}

	@Bean
	BiFunction<Optional<Resolution>, UUID, Boolean> owner() {
		return (resolution, userId) -> resolution
				.filter(r -> r.getOwner().equals(userId))
				.isPresent();
	}

	private MappedJwtClaimSetConverter claimSetConverter() {
		Converter<Object, UUID> converter = value ->
				UUID.fromString(value.toString());
		return MappedJwtClaimSetConverter.withDefaults
				(Collections.singletonMap("user_id", converter));
	}

	private OAuth2TokenValidator<Jwt> jwtValidator() {
		OAuth2TokenValidator<Jwt> audience = jwt ->
				Optional.ofNullable(jwt.getAudience())
						.filter(aud -> aud.contains("resolution"))
						.map(aud -> success())
						.orElse(failure(new OAuth2Error(INVALID_TOKEN, "Bad audience", "url")));
		OAuth2TokenValidator<Jwt> defaults = JwtValidators.createDefault();
		return new DelegatingOAuth2TokenValidator<>(defaults, audience);
	}

	@Autowired
	void jwtDecoder(JwtDecoder jwtDecoder) {
		((NimbusJwtDecoder) jwtDecoder).setClaimSetConverter(claimSetConverter());
		((NimbusJwtDecoder) jwtDecoder).setJwtValidator(jwtValidator());
	}
}
