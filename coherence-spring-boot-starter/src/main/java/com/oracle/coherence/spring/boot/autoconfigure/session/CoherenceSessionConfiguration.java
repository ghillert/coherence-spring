/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.spring.boot.autoconfigure.session;

import java.time.Duration;

import com.oracle.coherence.spring.boot.autoconfigure.CoherenceAutoConfiguration;
import com.oracle.coherence.spring.session.config.annotation.web.http.CoherenceHttpSessionConfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.session.SessionProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Coherence auto-configuration for Spring Session.
 *
 * @author Gunnar Hillert
 * @since 3.0
 */
@AutoConfigureAfter(CoherenceAutoConfiguration.class)
@Configuration
@EnableConfigurationProperties(CoherenceSessionProperties.class)
@ConditionalOnProperty(name = "coherence.spring.session.enabled", havingValue = "true")
public class CoherenceSessionConfiguration {

	@Configuration(proxyBeanMethods = false)
	@AutoConfigureBefore(name = "org.springframework.boot.actuate.autoconfigure.session.SessionsEndpointAutoConfiguration")
	public static class SpringBootCoherenceHttpSessionConfiguration extends CoherenceHttpSessionConfiguration {

		@Autowired
		public void customize(SessionProperties sessionProperties,
				CoherenceSessionProperties coherenceSessionProperties, ServerProperties serverProperties) {
			final Duration timeout = sessionProperties
					.determineTimeout(() -> serverProperties.getServlet().getSession().getTimeout());
			if (timeout != null) {
				setMaxInactiveIntervalInSeconds((int) timeout.getSeconds());
			}
			setSessionMapName(coherenceSessionProperties.getMapName());
			setFlushMode(coherenceSessionProperties.getFlushMode());
			setSaveMode(coherenceSessionProperties.getSaveMode());
		}

	}
}
