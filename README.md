# payara-utils - ©DBC

A couple of jars for payara5

## The modules

- The `payara-utils-runtime` jar - is for adding to the payara server with
  `--addjar`, however you can depend upon it, if you don't want it to be
  included in your base payara image.

- The `payara-utils-requires-admin` is a jar that you can add to your `war`
  to access tnhe ip-based access control

They do require `slf4j` and `logback` at runtime, but not at deploy time, so
you need to add those too.

## Usage

### Authorization

Accessed through:

    <dependencies>
        <dependency>
            <groupId>dk.dbc</groupId>
            <artifact>payara-utils-requires-admin</artifact>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>


It registers a request-filter that looks for `@RequiresAdmin` annotations. If
such is found, it looks at 2 environment variables: `ADMIN_IP` and
`ADMIN_IP_X_FORWARDED_FOR`,  to determine if the client is allowed to access
this JaxRS method.

These variables are comma separated lists of ip or ip/net entries.

Both values defaults to all non-routed ipv4 ranges. Currently ipv6 is not
supported.

When a request comes in the remote ip is looked at. If it is listed in
`ADMIN_IP_X_FORWARDED_FOR`, then the (optional) list of ip numbers from the
`X-Forwarded-For` HTTP header is traversed from right to left, finding the first
ip address that is not listed in `ADMIN_IP_X_FORWARDED_FOR` and uses it as peer.

Then the peer is checked against the `ADMIN_IP` ip list to determine if a
request is allowed.

### Metrics

Part of `payara-utils-runtime`

It registers a webpage at `/metrics.html` that looks at the microservice-endpoint
`/metrics/` and presents that data.

### LogLevel

Part of `payara-utils-runtime`

If exposes a webpage at `/loglevel.html` and an endpoint at `/loglevel/` that
is `@RequiresAdmin` protected.

This exposes the logback level for each java package. And allows to change it
on the fly
