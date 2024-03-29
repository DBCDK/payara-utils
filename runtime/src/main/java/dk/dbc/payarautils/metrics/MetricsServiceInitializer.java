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
package dk.dbc.payarautils.metrics;

import java.util.Set;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

/**
 * Service initializer that registers {@link MetricsServiceResource}
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class MetricsServiceInitializer implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> set, ServletContext context) throws ServletException {

        if (context.getServletRegistrations()
                .values()
                .stream()
                .map(Object::getClass)
                .anyMatch(MetricsServiceResource.class::equals))
            return;

        // Register a servlet with url patterns of metrics handlers
        context.addServlet("metrics-monitor", MetricsServiceResource.class)
                .addMapping("/metrics.html");
    }
}
