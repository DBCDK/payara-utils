# payara-utils - ©DBC

A jar that can be added to payara5(-micro).

It does require slf4j and logback at runtime, but not at deploy time, so you need
to add those too.

## Usage

### Authorization

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

It registers a webpage at `/metrics.html` that looks at the microservice-endpoint
`/metrics/` and presents that data.

### LogLevel

If exposes a webpage at `/loglevel.html` and an endpoint at `/loglevel/` that
is `@RequiresAdmin` protected.

This exposes the logback level for each java package. And allows to change it
on the fly
