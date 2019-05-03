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
package dk.dbc.accesscontrol;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;

/**
 * The dynamic feature that checks if a method is annotated with
 * \@{@link RequiresAdmin} and sets a filter
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class RequiresAdminDynamicFeature implements DynamicFeature {

    private static final Logger log = Logger.getLogger(RequiresAdminDynamicFeature.class.getName());

    @Context
    private HttpServletRequest request;

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        Method method = resourceInfo.getResourceMethod();
        Class<?> clazz = resourceInfo.getResourceClass();
        if (method.isAnnotationPresent(RequiresAdmin.class) ||
            clazz.isAnnotationPresent(RequiresAdmin.class)) {
            log.log(Level.INFO, "@RequiresAdmin identified for method: ", method);
            context.register(new RequiresAdminFilter(request));
        }
    }
}
