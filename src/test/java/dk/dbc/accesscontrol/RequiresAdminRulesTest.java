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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class RequiresAdminRulesTest {

    @Test(timeout = 2_000L)
    public void resolveNoForwardedFor() throws Exception {
        System.out.println("resolveNoForwardedFor");
        RequiresAdminRules rules = new RequiresAdminRules(null, null);
        String peer = rules.remoteIp("12.34.56.78", null);
        assertThat(peer, is("12.34.56.78"));
    }

    @Test(timeout = 2_000L)
    public void resolveEmptyForwardedFor() throws Exception {
        System.out.println("resolveEmptyForwardedFor");
        RequiresAdminRules rules = new RequiresAdminRules(null, null);
        String peer = rules.remoteIp("12.34.56.78", "");
        assertThat(peer, is("12.34.56.78"));
    }

    @Test(timeout = 2_000L)
    public void resolveForwardedForOutsideRange() throws Exception {
        System.out.println("resolveForwardedForOutsideRange");
        RequiresAdminRules rules = new RequiresAdminRules(null, null);
        String peer = rules.remoteIp("12.34.56.78", "98.76.54.32");
        assertThat(peer, is("12.34.56.78"));
    }

    @Test(timeout = 2_000L)
    public void resolveForwardedForInsideRange() throws Exception {
        System.out.println("resolveForwardedForInsideRange");
        RequiresAdminRules rules = new RequiresAdminRules("12.0.0.0/8", null);
        String peer = rules.remoteIp("12.34.56.78", "98.76.54.32");
        assertThat(peer, is("98.76.54.32"));
    }

    @Test(timeout = 2_000L)
    public void resolveMultiForwardedForLeadingOutsideRange() throws Exception {
        System.out.println("resolveMultiForwardedForLeadingOutsideRange");
        RequiresAdminRules rules = new RequiresAdminRules("12.0.0.0/8", null);
        String peer = rules.remoteIp("12.34.56.78", "10.0.0.1, 98.76.54.32");
        assertThat(peer, is("98.76.54.32"));
    }

    @Test(timeout = 2_000L)
    public void resolveMultiForwardedForInsideRange() throws Exception {
        System.out.println("resolveMultiForwardedForInsideRange");
        RequiresAdminRules rules = new RequiresAdminRules("12.0.0.0/8, 98.0.0.0/8", null);
        String peer = rules.remoteIp("12.34.56.78", "10.0.0.1, 98.76.54.32");
        assertThat(peer, is("10.0.0.1"));
    }
}
