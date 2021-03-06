package de.otto.edison.mongo.configuration;

import com.mongodb.MongoClientOptions;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import de.otto.edison.status.domain.Datasource;
import org.bson.codecs.configuration.CodecRegistry;
import javax.validation.constraints.NotEmpty;
import org.slf4j.Logger;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.mongodb.MongoClientOptions.builder;
import static de.otto.edison.status.domain.Datasource.datasource;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Properties used to configure MongoDB clients.
 *
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "edison.mongo")
@Validated
public class MongoProperties {
    private static final Logger LOG = getLogger(MongoProperties.class);

    /**
     * The MongoDB servers. Comma-separated list of host:port pairs.
     */
    @NotEmpty
    private String[] host = {"localhost"};

    /**
     * The MongoDB database.
     */
    private String authenticationDb = "";

    /**
     * The MongoDB database.
     */
    @NotEmpty
    private String db;

    /**
     * database user name
     */
    private String user = "";

    /**
     * database user password
     */
    private String password = "";

    /**
     * database user password
     */
    private boolean sslEnabled;

    /**
     * Represents preferred replica set members to which a query or command can be sent.
     */
    @NotEmpty
    private String readPreference = "primaryPreferred";

    /**
     * Maximum time that a thread will block waiting for a connection.
     */
    @Min(10)
    private int maxWaitTime = 5000;

    /**
     * Connection timeout in milliseconds. Must be &gt; 0
     */
    @Min(10)
    private int connectTimeout = 5000;

    /**
     * The default timeout in milliseconds to use for reading operations.
     */
    @Min(10)
    private int defaultReadTimeout = 2000;

    /**
     * The default timeout in milliseconds to use for writing operations.
     */
    @Min(10)
    private int defaultWriteTimeout = 2000;

    /**
     * Sets the server selection timeout in milliseconds, which defines how long the driver will wait for server selection to
     * succeed before throwing an exception.
     */
    @Min(1)
    private int serverSelectionTimeout = 30000;
    @Valid
    private Status status = new Status();
    /**
     * Connection pool properties.
     */
    @Valid
    private Connectionpool connectionpool = new Connectionpool();

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public int getDefaultReadTimeout() {
        return defaultReadTimeout;
    }

    public void setDefaultReadTimeout(int defaultReadTimeout) {
        this.defaultReadTimeout = defaultReadTimeout;
    }

    public int getDefaultWriteTimeout() {
        return defaultWriteTimeout;
    }

    public void setDefaultWriteTimeout(int defaultWriteTimeout) {
        this.defaultWriteTimeout = defaultWriteTimeout;
    }

    public List<Datasource> toDatasources() {
        return Stream.of(getHost())
                .map(host -> datasource(host + "/" + getDb()))
                .collect(toList());
    }

    public List<ServerAddress> getServers() {
        return Stream.of(host)
                .filter(Objects::nonNull)
                .map(this::toServerAddress)
                .filter(Objects::nonNull)
                .collect(toList());
    }

    public String[] getHost() {
        return host;
    }

    public void setHost(final String[] host) {
        this.host = host;
    }

    public String getAuthenticationDb() {
        return authenticationDb;
    }

    public void setAuthenticationDb(final String authenticationDb) {
        this.authenticationDb = authenticationDb;
    }

    public String getDb() {
        return db;
    }

    public void setDb(final String db) {
        this.db = db;
    }

    public String getUser() {
        return user;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public void setSslEnabled(final boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    public String getReadPreference() {
        return readPreference;
    }

    public void setReadPreference(final String readPreference) {
        this.readPreference = readPreference;
    }

    public int getMaxWaitTime() {
        return maxWaitTime;
    }

    public void setMaxWaitTime(final int maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(final int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getServerSelectionTimeout() {
        return serverSelectionTimeout;
    }

    public void setServerSelectionTimeout(final int serverSelectionTimeout) {
        this.serverSelectionTimeout = serverSelectionTimeout;
    }

    public Connectionpool getConnectionpool() {
        return connectionpool;
    }

    public void setConnectionpool(final Connectionpool connectionpool) {
        this.connectionpool = connectionpool;
    }

    public MongoClientOptions toMongoClientOptions(final CodecRegistry codecRegistry) {
        return builder()
                .sslEnabled(sslEnabled)
                .codecRegistry(codecRegistry)
                .readPreference(ReadPreference.valueOf(readPreference))
                .connectTimeout(connectTimeout)
                .serverSelectionTimeout(serverSelectionTimeout)
                .cursorFinalizerEnabled(true)
                .maxWaitTime(maxWaitTime)
                .maxConnectionLifeTime(connectionpool.getMaxLifeTime())
                .threadsAllowedToBlockForConnectionMultiplier(connectionpool.getBlockedConnectionMultiplier())
                .maxConnectionIdleTime(connectionpool.getMaxIdleTime())
                .minConnectionsPerHost(connectionpool.getMinSize())
                .connectionsPerHost(connectionpool.getMaxSize())
                .build();
    }

    private ServerAddress toServerAddress(final String server) {
        try {
            if (server.contains(":")) {
                final String[] hostNamePortPair = server.split(":");
                return new ServerAddress(hostNamePortPair[0], Integer.parseInt(hostNamePortPair[1]));
            } else {
                return new ServerAddress(server);
            }
        } catch (final NumberFormatException e) {
            LOG.warn("Invalid portNumber: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Creates a StatusDetailIndicator that checks the MongoDB connection through a ping command
     */
    public static class Status {

        /**
         * Enable / disable the MongoDB StatusDetailIndicator
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }
        public void setEnabled(final boolean enabled) {
            this.enabled = enabled;
        }


    }

    public static class Connectionpool {

        /**
         * Maximum number of connections allowed per host.
         */
        @Min(1)
        private int maxSize = 100;
        /**
         * The minimum number of connections per host. A value of <code>0</code> will create connections lazily.
         */
        @Min(0)
        private int minSize = 2;
        /**
         * This multiplier, multiplied with the maxSize property, gives the maximum number of threads that may be waiting for a
         * connection to become available from the pool.
         */
        @Min(1)
        private int blockedConnectionMultiplier = 2;
        /**
         * Maximum life time for a pooled connection.
         */
        @Min(1)
        private int maxLifeTime = 100000;
        /**
         * Maximum idle time for a pooled connection.
         */
        @Min(1)
        private int maxIdleTime = 10000;

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(final int maxSize) {
            this.maxSize = maxSize;
        }

        public int getMinSize() {
            return minSize;
        }

        public void setMinSize(final int minSize) {
            this.minSize = minSize;
        }

        public int getBlockedConnectionMultiplier() {
            return blockedConnectionMultiplier;
        }

        public void setBlockedConnectionMultiplier(final int blockedConnectionMultiplier) {
            this.blockedConnectionMultiplier = blockedConnectionMultiplier;
        }

        public int getMaxLifeTime() {
            return maxLifeTime;
        }

        public void setMaxLifeTime(final int maxLifeTime) {
            this.maxLifeTime = maxLifeTime;
        }

        public int getMaxIdleTime() {
            return maxIdleTime;
        }

        public void setMaxIdleTime(final int maxIdleTime) {
            this.maxIdleTime = maxIdleTime;
        }
    }
}
