/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.spring;

import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.util.MapEvent;
import com.tangosol.util.MultiplexingMapListener;

import org.springframework.beans.factory.annotation.Value;

/**
 * Stub backing map listener implementation for testing by
 * {@link SpringNamespaceHandlerTests}.
 *
 * @author Patrick Peralta
 */
public class StubBackingMapListener extends MultiplexingMapListener
{
    private volatile boolean         m_fCtxConfigured = false;
    private BackingMapManagerContext m_ctx;


    /**
     * Construct a StubBackingMapListener.
     *
     */
    public StubBackingMapListener()
    {
        super();
    }


    /**
     * Return the BackingMapManagerContext.
     *
     * @return the BackingMapManagerContext
     */
    public BackingMapManagerContext getBackingMapManagerContext()
    {
        return m_ctx;
    }


    /**
     * Set the BackingMapManagerContext.
     *
     * @param ctx the BackingMapManagerContext
     */
    @Value("#{manager-context}")
    public void setBackingMapManagerContext(BackingMapManagerContext ctx)
    {
        m_ctx = ctx;
    }


    /**
     * Return true if the BackingMapManagerContext was set via {@link #setBackingMapManagerContext}.
     *
     * @return true if the BackingMapManagerContext was set
     */
    public boolean isContextConfigured()
    {
        return m_fCtxConfigured;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void onMapEvent(MapEvent evt)
    {
        m_fCtxConfigured = m_ctx != null;
    }
}
