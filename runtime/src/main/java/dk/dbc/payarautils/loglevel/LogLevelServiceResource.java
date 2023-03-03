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
package dk.dbc.payarautils.loglevel;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dbc.payarautils.accesscontrol.RequiresAdminRules;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Locale;
import java.util.logging.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;

import static dk.dbc.resources.PayaraUtilsHtmlResource.loadHtml;


/**
 * Servlet that allows for changing loglevel on the fly8
 * <p>
 * This user {@link RequiresAdminRules} to validate that the peer is allowed to
 * change loglevel
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class LogLevelServiceResource extends HttpServlet {

    private static final Logger log = Logger.getLogger(LogLevelServiceResource.class.getName());
    private static final ObjectMapper O = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RequiresAdminRules rules = RequiresAdminRules.instance();
        String originalIp = req.getRemoteAddr();
        String xForwardedFor = req.getHeader("x-forwarded-for");
        String peer = rules.remoteIp(originalIp, xForwardedFor);
        if (!rules.isAdminIp(peer)) {
            log.log(java.util.logging.Level.SEVERE,
                    "Authentication denied for {0}" +
                    " with X-Forwarded-For: {1}" +
                    " resolved to peer: {2}",
                    new Object[] {originalIp, xForwardedFor, peer});
            throw new AccessDeniedException("NOT IP AUTHORIZED TO MODIFY LOGLEVEL");
        }

        String servletPath = req.getServletPath();
        switch (servletPath) {
            case "/loglevel.html":
                doGetHtml(resp);
                return;
            case "/loglevel/":
                doGetConfig(req, resp);
                return;
            default:
                throw new AssertionError();
        }
    }

    /**
     * Output the html page
     *
     * @param resp the client that should have the html page
     * @throws IOException      If the page cannot be written
     * @throws ServletException If the html cannot be loaded
     */
    private void doGetHtml(HttpServletResponse resp) throws IOException, ServletException {
        byte[] html = loadHtml(getClass().getSimpleName() + "-loglevel.html");
        if (html == null)
            throw new ServletException("Could not get html resource");
        resp.setStatus(200);
        resp.setContentType("text/html; charset=utf-8");
        try (ServletOutputStream os = resp.getOutputStream()) {
            os.write(html);
        }
    }

    /**
     * Process the change log level request and output a json object repicting
     * current log configuration
     *
     * @param req  the request parameters
     * @param resp the client that chouls have current state
     * @throws IOException if the client closes connection
     */
    private void doGetConfig(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(200);
        resp.setContentType("application/json; charset=utf-8");

        String logger = req.getParameter("logger");
        if (logger != null) {
            String level = req.getParameter("level");
            log.log(java.util.logging.Level.INFO,
                    "Setting log level for {0}" +
                    " to: {1}",
                    new Object[] {logger, level});
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            context.getLogger(logger).setLevel(level == null || level.isEmpty() ? null :
                                               Level.valueOf(level.toUpperCase(Locale.ROOT)));
        }

        ObjectNode obj = O.createObjectNode();

        ( (LoggerContext) LoggerFactory.getILoggerFactory() )
                .getLoggerList()
                .stream()
                .forEach(e -> obj.put(e.getName(), levelToString(e)));

        try (ServletOutputStream os = resp.getOutputStream()) {
            O.writeValue(os, obj);
        }
    }

    /**
     * A null-safe loglevel toString()
     *
     * @param logger an optionally null logger
     * @return null is no loglevel has been set of textual name of loglevel
     */
    private static String levelToString(ch.qos.logback.classic.Logger logger) {
        Level l = logger.getLevel();
        if (l == null)
            return null;
        return l.toString();
    }

}
