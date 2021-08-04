/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.spring.cache;

import java.time.Duration;
import java.util.concurrent.Callable;

import com.tangosol.net.NamedCache;

import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.util.Assert;

/**
 * Coherence-specific implementation of {@link Cache} that defines common cache operations.
 *
 * @author Gunnar Hillert
 * @since 3.0
 */
public class CoherenceCache implements Cache {

	private final NamedCache<Object, Object> cache;
	private final CoherenceCacheConfiguration cacheConfiguration;

	public CoherenceCache(NamedCache<Object, Object> cache, CoherenceCacheConfiguration cacheConfiguration) {
		super();

		Assert.notNull(cache, "The NamedCache must not be null.");
		Assert.notNull(cache, "cacheConfiguration must not be null.");

		this.cache = cache;
		this.cacheConfiguration = cacheConfiguration;

	}

	@Override
	public void clear() {
		this.cache.clear();
	}

	@Override
	public void evict(Object key) {
		this.cache.remove(key);
	}

	@Override
	public ValueWrapper get(Object key) {
		final Object value = this.cache.get(key);
		return (value != null) ? new SimpleValueWrapper(value) : null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Object key, Class<T> type) {
		final Object value = this.cache.get(key);
		if (type != null && value != null && !type.isInstance(value)) {
			throw new IllegalStateException(
				String.format("Cached value '%s' is not of required type '%s'.", value, type.getName()));
		}
		return (T) value;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Object key, Callable<T> valueLoader) {
		final Object value = this.cache.get(key);
		if (value != null) {
			return (T) value;
		}
		else {
			this.cache.lock(key);
			try {
				final Object value2 = this.cache.get(key);
				if (value2 != null) {
					return (T) value2;
				}
				else {
					return loadValue(key, valueLoader);
				}
			}
			finally {
				this.cache.unlock(key);
			}
		}
	}

	@Override
	public String getName() {
		return this.cache.getCacheName();
	}

	@Override
	public Object getNativeCache() {
		return this.cache;
	}

	private <T> T loadValue(Object key, Callable<T> valueLoader) {
		T value;
		try {
			value = valueLoader.call();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Executing the callable failed.", ex);
		}
		put(key, value);
		return value;
	}

	@Override
	public void put(Object key, Object value) {
		if (value == null) {
			return;
		}
		if (isUsingTtl(this.cacheConfiguration.getTimeToLive())) {
			this.cache.put(key, value, this.cacheConfiguration.getTimeToLive().toMillis());
		}
		else {
			this.cache.put(key, value);
		}
	}

	/**
	 * Returns the number of key-value mappings of the underlying {@link NamedCache}.
	 * @return the number of key-value mappings in this map
	 */
	public int size() {
		return this.cache.size();
	}

	/**
	 * Return the used {@link CoherenceCacheConfiguration}.
	 * @return never returns {@code null}.
	 */
	public CoherenceCacheConfiguration getCacheConfiguration() {
		return this.cacheConfiguration;
	}

	private static boolean isUsingTtl(Duration timeTolive) {
		return timeTolive != null && !timeTolive.isZero() && !timeTolive.isNegative();
	}
}
