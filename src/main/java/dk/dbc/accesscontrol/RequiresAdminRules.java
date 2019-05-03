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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.EMPTY_LIST;

/**
 * Singleton that exposes remote access verification methods based upon the
 * environment variables $ADMIN_IP_X_FORWARDED_FOR and $ADMIN_IP
 * <p>
 * Currently only supports IPv4
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class RequiresAdminRules {

    private static final RequiresAdminRules INSTANCE = new RequiresAdminRules();

    public static RequiresAdminRules instance() {
        return INSTANCE;
    }

    private static final long IPV4_MAX = 0xffffffffL;
    private static final long IPV4_BAD = 0x10BADDECAFL;

    private final List<IPRange> allowedProxyIpRanges;
    private final List<IPRange> allowedAdminIpRanges;

    private RequiresAdminRules() {
        this.allowedProxyIpRanges = listFromEnv("ADMIN_IP_X_FORWARDED_FOR", "127.0.0.0/8,192.168.0.0/16,172.16.0.0/12,10.0.0.0/8");
        this.allowedAdminIpRanges = listFromEnv("ADMIN_IP", "127.0.0.0/8,192.168.0.0/16,172.16.0.0/12,10.0.0.0/8");
    }

    RequiresAdminRules(String xForwardedFor, String allowed) {
        this.allowedProxyIpRanges = listFrom(xForwardedFor);
        this.allowedAdminIpRanges = listFrom(allowed);
    }

    /**
     * Resolve a remote ip address from a request processing optional
     * x-forwarded-for headers
     *
     * @param peer          ipv4 peer address
     * @param xForwardedFor request header for proxy resolve. X-Forwarded-For
     *                      syntax is client-ip[, proxy-ip ...]
     * @return peer ip address
     */
    public String remoteIp(String peer, String xForwardedFor) {
        long peerIp = ipOf(peer);
        if (peerIp == IPV4_BAD)
            return peer;
        if (xForwardedFor != null && !xForwardedFor.isEmpty() &&
            inIpRange(peerIp, allowedProxyIpRanges)) {
            // Proxy connected to us, and has x-forwarded-for set
            String[] xForwardedFors = xForwardedFor.split(",");
            // Ensure (optional) proxies are in our allowed list
            int pos = xForwardedFors.length;
            while (--pos > 0) {
                String proxy = xForwardedFors[pos].trim();
                long proxyIp = ipOf(proxy);
                // if proxy isnt
                if (proxyIp == IPV4_BAD)
                    return peer;
                if (!inIpRange(proxyIp, allowedProxyIpRanges)) {
                    return proxy; // Proxy is not in allowed list - proxy is our peer
                }
            }
            return xForwardedFors[pos].trim();
        }
        return peer;
    }

    /**
     * Check if an ip (from
     * {@link #remoteIp(java.lang.String, java.lang.String)} is in the admin ip
     * list
     *
     * @param peer ip number as string
     * @return if admin access is allowed
     */
    public boolean isAdminIp(String peer) {
        long ip = ipOf(peer);
        return ip != IPV4_BAD && inIpRange(ip, allowedAdminIpRanges);
    }

    /**
     * Convert a comma separated list of ip(-ranges) to an {@link IpRange} list
     *
     * @param env environment variable value
     * @return List of IpRanges that are in the input
     */
    private static List<IPRange> listFromEnv(String env, String defaultValue) {
        String value = System.getenv(env);
        if (value == null)
            value = defaultValue;
        return listFrom(value);
    }

    /**
     * Convert a comma separated list of ip(-ranges) to an {@link IpRange} list
     *
     * @param ipRangesConfig comma separated list of ip ranges
     * @return List of IpRanges that are in the input
     */
    private static List<IPRange> listFrom(String ipRangesConfig) {
        if (ipRangesConfig == null || ipRangesConfig.trim().isEmpty())
            return EMPTY_LIST;
        return Arrays.stream(ipRangesConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(RequiresAdminRules::ipRange)
                .filter(m -> m != null)
                .collect(Collectors.toList());
    }

    /**
     * Check if an ip address is in a list of ranges
     *
     * @param ip     ip to test for
     * @param ranges ip ranges
     * @return if ip is covered by any range
     */
    private static boolean inIpRange(long ip, List<IPRange> ranges) {
        return ranges.stream().anyMatch(i -> i.isInRange(ip));
    }

    /**
     * Convert a string of host/net/ranges to a list
     *
     * @param hosts ip(-range)/net list
     * @return List of ranges
     */
    private static IPRange ipRange(String hosts) {
        if (hosts.contains("-")) {
            String[] parts = hosts.split("-", 2);
            long ipMin = ipOf(parts[0]);
            long ipMax = ipOf(parts[1]);
            if (ipMin == IPV4_BAD || ipMax == IPV4_BAD) {
                return null;
            }
            return new IPRange(ipMin, ipMax);
        } else if (hosts.contains("/")) {
            String[] parts = hosts.split("/", 2);
            long ip = ipOf(parts[0]);
            if (ip == IPV4_BAD) {
                return null;
            }
            int mask = Integer.parseInt(parts[1], 10);
            if (mask < 0 || mask > 32 || !parts[1].replaceAll("[0-9]", "").isEmpty()) {
                return null;
            }
            long net = ( IPV4_MAX << ( 32 - mask ) ) & IPV4_MAX;
            return new IPRange(ip & net, ( ip | ~net ) & IPV4_MAX);
        } else {
            long ip = ipOf(hosts);
            if (ip == IPV4_BAD) {
                return null;
            }
            return new IPRange(ip, ip);
        }
    }

    /**
     * Convert a ipv4 address into a long value
     *
     * @param addr ipv4 address
     * @return 32-bit in a long
     */
    private static long ipOf(String addr) {
        System.out.println("addr = " + addr);
        if (addr.matches("(?:[1-9][0-9]?)?[0-9](\\.(?:[1-9][0-9]?)?[0-9]){3}")) {
            System.out.println("matched");
            int[] parts = Arrays.stream(addr.split("\\."))
                    .mapToInt(Integer::parseUnsignedInt)
                    .toArray();
            if (!Arrays.stream(parts)
                    .allMatch(n -> n >= 0 && n <= 255)) {
                System.out.println("bad-part");
                return IPV4_BAD;
            }
            return Arrays.stream(parts).reduce(0, (l, r) -> ( l << 8 ) + r) & IPV4_MAX;
        } else {
            return IPV4_BAD;
        }
    }

    /**
     * Class that represents an ipv4 range
     */
    private static class IPRange {

        private final long min;
        private final long max;

        private IPRange(long min, long max) {
            this.min = min;
            this.max = max;
        }

        private boolean isInRange(long ip) {
            return ip >= min && ip <= max;
        }
    }
}
