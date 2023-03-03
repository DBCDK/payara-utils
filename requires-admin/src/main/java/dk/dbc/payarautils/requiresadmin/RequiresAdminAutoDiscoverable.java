/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of payara-utils
 *
 * payara-utils is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * payara-utils is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.payarautils.requiresadmin;

import jakarta.annotation.Priority;
import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.jersey.internal.spi.ForcedAutoDiscoverable;

import static jakarta.enterprise.inject.spi.ObserverMethod.DEFAULT_PRIORITY;
import static jakarta.ws.rs.RuntimeType.SERVER;
import static org.glassfish.internal.api.Globals.getDefaultHabitat;

/**
 *
 * Auto-discoverable class that registers the \@{@link RequiresAdmin} dynamic
 * feature
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@ConstrainedTo(SERVER)
@Priority(DEFAULT_PRIORITY)
public final class RequiresAdminAutoDiscoverable implements ForcedAutoDiscoverable {

    @Override
    public void configure(FeatureContext context) {
        // from fish.payara.microprofile.jwtauth.jaxrs.RolesAllowedAutoDiscoverable
        ExtendedDeploymentContext deploymentContext =
                ( (Deployment) getDefaultHabitat().getService(Deployment.class) )
                        .getCurrentDeploymentContext();

        // Only register for application deployments (not the admin console)
        if (deploymentContext == null)
            return;
        if (!context.getConfiguration().isRegistered(RequiresAdminDynamicFeature.class))
            context.register(RequiresAdminDynamicFeature.class);
    }
}
