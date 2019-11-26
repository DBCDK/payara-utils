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
package dk.dbc.payarautils.accesscontrol;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import static javax.ws.rs.Priorities.AUTHORIZATION;

/**
 * A container request filter that validated the request against
 * {@link RequiresAdminRules}
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Priority(AUTHORIZATION)
public class RequiresAdminFilter implements ContainerRequestFilter {

    private static final Logger log = Logger.getLogger(RequiresAdminFilter.class.getName());

    private final HttpServletRequest request;

    public RequiresAdminFilter(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public void filter(ContainerRequestContext context) throws IOException {
        RequiresAdminRules rules = RequiresAdminRules.instance();
        String originalIp = request.getRemoteAddr();
        String xForwardedFor = context.getHeaderString("x-forwarded-for");
        String peer = rules.remoteIp(originalIp, xForwardedFor);
        log.log(Level.INFO,
                "Authentication granted for {0}" +
                " with X-Forwarded-For: {1}" +
                " resolved to peer: {2}",
                new Object[] {originalIp, xForwardedFor, peer});
        if (rules.isAdminIp(peer))
            return;
        log.log(Level.SEVERE,
                "Authentication denied for {0}" +
                " with X-Forwarded-For: {1}" +
                " resolved to peer: {2}",
                new Object[] {originalIp, xForwardedFor, peer});
        throw new NotAuthorizedException("NOT AN AUTHORIZED IP ADDRESS");
    }
}
