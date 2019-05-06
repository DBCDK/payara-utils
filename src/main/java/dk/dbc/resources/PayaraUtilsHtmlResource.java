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
package dk.dbc.resources;

import dk.dbc.metrics.MetricsServiceResource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class PayaraUtilsHtmlResource {

    public static byte[] loadHtml(String resource) {
        try (InputStream is = PayaraUtilsHtmlResource.class.getClassLoader()
                .getResourceAsStream(resource)) {
            return getStringFromInputStream(is)
                    .replaceAll("@APPNAME@", applicationName())
                    .getBytes(UTF_8);
        } catch (IOException ex) {
            Logger.getLogger(MetricsServiceResource.class.getName())
                    .log(java.util.logging.Level.SEVERE, null, ex);
            return null;
        }
    }

    private static String applicationName() {
        try {
            return ( (String) InitialContext.doLookup("java:app/AppName") )
                    .replaceFirst("-\\d+.\\d+-SNAPSHOT", "");
        } catch (NamingException ex) {
            return "Unknown";
        }
    }

    private static String getStringFromInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        for (int len = is.read(buffer) ; len != -1 ; len = is.read(buffer)) {
            os.write(buffer, 0, len);
        }
        return new String(os.toByteArray(), UTF_8);
    }

}
