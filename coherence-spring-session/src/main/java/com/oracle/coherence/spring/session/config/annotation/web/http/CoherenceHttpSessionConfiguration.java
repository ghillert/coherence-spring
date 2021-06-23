/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.spring.session.config.annotation.web.http;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.oracle.coherence.spring.configuration.CoherenceSpringConfiguration;
import com.oracle.coherence.spring.session.CoherenceIndexedSessionRepository;
import com.oracle.coherence.spring.session.config.annotation.SpringSessionCoherenceInstance;
import com.tangosol.net.Coherence;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.FlushMode;
import org.springframework.session.IndexResolver;
import org.springframework.session.MapSession;
import org.springframework.session.SaveMode;
import org.springframework.session.Session;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.util.StringUtils;

/**
 * Exposes the {@link SessionRepositoryFilter} as a bean named {@code springSessionRepositoryFilter}. In order to use
 * this, {@link CoherenceSpringConfiguration} must be active as well, typically using the
 * {@link com.oracle.coherence.spring.configuration.annotation.EnableCoherence} annotation.
 *
 * @author Gunnar Hillert
 * @since 3.0
 * @see EnableCoherenceHttpSession
 * @see com.oracle.coherence.spring.configuration.annotation.EnableCoherence
 */
@Configuration(proxyBeanMethods = false)
public class CoherenceHttpSessionConfiguration extends SpringHttpSessionConfiguration implements ImportAware {

	private Integer maxInactiveIntervalInSeconds = MapSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS;

	private String sessionMapName = CoherenceIndexedSessionRepository.DEFAULT_SESSION_MAP_NAME;
	private String coherenceSessionName;

	private FlushMode flushMode = FlushMode.ON_SAVE;
	private SaveMode saveMode = SaveMode.ON_SET_ATTRIBUTE;

	private Coherence coherence;

	private IndexResolver<Session> indexResolver;
	private List<SessionRepositoryCustomizer<CoherenceIndexedSessionRepository>> sessionRepositoryCustomizers;

	@Bean
	@DependsOn(CoherenceSpringConfiguration.COHERENCE_SERVER_BEAN_NAME)
	public FindByIndexNameSessionRepository<?> sessionRepository() {
		return createCoherenceIndexedSessionRepository();
	}

	@Autowired
	public void setCoherence(
			@SpringSessionCoherenceInstance ObjectProvider<Coherence> springSessionCoherenceInstance,
			ObjectProvider<Coherence> coherenceInstance) {
		Coherence coherenceInstanceToUse = springSessionCoherenceInstance.getIfAvailable();
		if (coherenceInstanceToUse == null) {
			coherenceInstanceToUse = coherenceInstance.getObject();
		}
		this.coherence = coherenceInstanceToUse;
	}

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		final Map<String, Object> attributeMap = importMetadata
				.getAnnotationAttributes(EnableCoherenceHttpSession.class.getName());
		final AnnotationAttributes attributes = AnnotationAttributes.fromMap(attributeMap);
		this.maxInactiveIntervalInSeconds = attributes.getNumber("sessionTimeoutInSeconds");
		final String sessionMapNameValue = attributes.getString("cache");
		if (StringUtils.hasText(sessionMapNameValue)) {
			this.sessionMapName = sessionMapNameValue;
		}
		final String coherenceSessionNameValue = attributes.getString("session");
		if (StringUtils.hasText(coherenceSessionNameValue)) {
			this.coherenceSessionName = coherenceSessionNameValue;
		}
		this.flushMode = attributes.getEnum("flushMode");
		this.saveMode = attributes.getEnum("saveMode");
	}

	@Autowired(required = false)
	public void setIndexResolver(IndexResolver<Session> indexResolver) {
		this.indexResolver = indexResolver;
	}

	@Autowired(required = false)
	public void setSessionRepositoryCustomizer(
			ObjectProvider<SessionRepositoryCustomizer<CoherenceIndexedSessionRepository>> sessionRepositoryCustomizers) {
		this.sessionRepositoryCustomizers = sessionRepositoryCustomizers.orderedStream().collect(Collectors.toList());
	}

	public void setMaxInactiveIntervalInSeconds(int maxInactiveIntervalInSeconds) {
		this.maxInactiveIntervalInSeconds = maxInactiveIntervalInSeconds;
	}

	public void setSessionMapName(String sessionMapName) {
		this.sessionMapName = sessionMapName;
	}

	public void setFlushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
	}

	public void setSaveMode(SaveMode saveMode) {
		this.saveMode = saveMode;
	}

	private CoherenceIndexedSessionRepository createCoherenceIndexedSessionRepository() {
		final com.tangosol.net.Session coherenceSession;
		if (StringUtils.hasText(this.coherenceSessionName)) {
			coherenceSession = this.coherence.getSession(this.coherenceSessionName);
		}
		else {
			coherenceSession = this.coherence.getSession();
		}
		final CoherenceIndexedSessionRepository sessionRepository = new CoherenceIndexedSessionRepository(coherenceSession);

		if (this.indexResolver != null) {
			sessionRepository.setIndexResolver(this.indexResolver);
		}
		if (StringUtils.hasText(this.sessionMapName)) {
			sessionRepository.setSessionMapName(this.sessionMapName);
		}
		sessionRepository.setDefaultMaxInactiveInterval(Duration.ofSeconds(this.maxInactiveIntervalInSeconds));
		sessionRepository.setFlushMode(this.flushMode);
		sessionRepository.setSaveMode(this.saveMode);
		this.sessionRepositoryCustomizers
				.forEach((sessionRepositoryCustomizer) -> sessionRepositoryCustomizer.customize(sessionRepository));
		return sessionRepository;
	}
}
