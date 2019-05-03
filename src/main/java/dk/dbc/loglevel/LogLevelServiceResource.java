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
package dk.dbc.loglevel;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dbc.accesscontrol.RequiresAdminRules;
import dk.dbc.metrics.MetricsServiceResource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.util.Locale;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

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
        byte[] html = loadHtml();
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

    private byte[] loadHtml() {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream(getClass().getSimpleName() + "-loglevel.html")) {
            return getStringFromInputStream(is)
                    .replaceAll("@APPNAME@", applicationName()).
                    getBytes(UTF_8);
        } catch (IOException ex) {
            Logger.getLogger(MetricsServiceResource.class.getName())
                    .log(java.util.logging.Level.SEVERE, null, ex);
            return null;
        }
    }

    private String applicationName() {
        try {
            return ( (String) InitialContext.doLookup("java:app/AppName") )
                    .replaceFirst("-\\d+.\\d+-SNAPSHOT", "");
        } catch (NamingException ex) {
            return "Unknown";
        }
    }

    private String getStringFromInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        for (int len = is.read(buffer) ; len != -1 ; len = is.read(buffer)) {
            os.write(buffer, 0, len);
        }
        return new String(os.toByteArray(), UTF_8);
    }
}
