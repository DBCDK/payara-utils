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

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static dk.dbc.resources.PayaraUtilsHtmlResource.loadHtml;


/**
 * Servlet, that produces the html page for metrics
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class MetricsServiceResource extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        byte[] html = loadHtml(getClass().getSimpleName() + "-metrics.html");
        if (html == null)
            throw new ServletException("Could not get html resource");
        resp.setStatus(200);
        resp.setContentType("text/html; charset=utf-8");
        try (ServletOutputStream os = resp.getOutputStream()) {
            os.write(html);
        }
    }

}
