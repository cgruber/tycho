/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.remote;

import org.eclipse.equinox.internal.p2.repository.CacheManager;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.p2.impl.Activator;

@SuppressWarnings("restriction")
public class RemoteAgent implements IProvisioningAgent {

    private IProvisioningAgent delegate;

    public RemoteAgent(MavenContext mavenContext, boolean disableMirrors) throws ProvisionException {
        this.delegate = createConfiguredProvisioningAgent(mavenContext, disableMirrors);
    }

    public RemoteAgent(MavenContext mavenContext) throws ProvisionException {
        this(mavenContext, false);
    }

    private static IProvisioningAgent createConfiguredProvisioningAgent(MavenContext mavenContext,
            boolean disableMirrors) throws ProvisionException {
        // TODO set a temporary folder as persistence location
        IProvisioningAgent agent = Activator.newProvisioningAgent();

        // suppress p2.index access
        final Transport transport;
        if (mavenContext.isOffline()) {
            transport = new OfflineTransport();
            agent.registerService(Transport.SERVICE_NAME, transport);
        } else {
            transport = (Transport) agent.getService(Transport.SERVICE_NAME);
        }

        // cache indices of p2 repositories in the local Maven repository
        TychoP2RepositoryCacheManager cacheMgr = new TychoP2RepositoryCacheManager(transport, mavenContext);
        agent.registerService(CacheManager.SERVICE_NAME, cacheMgr);

        if (disableMirrors) {
            IArtifactRepositoryManager plainRepoManager = (IArtifactRepositoryManager) agent
                    .getService(IArtifactRepositoryManager.SERVICE_NAME);
            IArtifactRepositoryManager mirrorDisablingRepoManager = new MirrorDisablingArtifactRepositoryManager(
                    plainRepoManager);
            agent.registerService(IArtifactRepositoryManager.SERVICE_NAME, mirrorDisablingRepoManager);
        }

        return agent;
    }

    public Object getService(String serviceName) {
        return delegate.getService(serviceName);
    }

    public void registerService(String serviceName, Object service) {
        delegate.registerService(serviceName, service);
    }

    public <T> T getService(Class<T> type) {
        return type.cast(getService(type.getName()));
    }

    public void stop() {
        delegate.stop();
    }

    public void unregisterService(String serviceName, Object service) {
        delegate.unregisterService(serviceName, service);
    }

}
