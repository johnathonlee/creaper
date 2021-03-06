package org.wildfly.extras.creaper.core.online;

public enum ManagementProtocol {
    /** Used in JBoss AS 7. Default port 9999. */
    REMOTE("remote"),
    /** Used in WildFly. Default port 9990. */
    HTTP_REMOTING("http-remoting"),
    /** Used in WildFly. Default port 9993. */
    HTTPS_REMOTING("https-remoting"),
    /**
     * With {@code HTTP}, the management client will not use the native management protocol,
     * but will instead use the HTTP management endpoint. The {@code ModelNode}s will be serialized to JSON. Operation
     * attachments are not supported (yet), so deploying will not work. If username and password is not provided,
     * no authentication method will be used.
     * Default port 9990.
     */
    HTTP("http"),
    /**
     * {@code HTTPS} imposes the same limitations as {@link #HTTP}.
     * <b>Default port is 9443 (AS7)</b> and can be set to <b>9993 (WildFly)</b> by setting the system property
     * {@code creaper.wildfly} or by configuring the port explicitly.
     */
    HTTPS("https"),
    ;

    private final String protocolName;

    ManagementProtocol(String protocolName) {
        this.protocolName = protocolName;
    }

    /** The name of the protocol as expected by the client libraries. */
    public String protocolName() {
        return protocolName;
    }
}
