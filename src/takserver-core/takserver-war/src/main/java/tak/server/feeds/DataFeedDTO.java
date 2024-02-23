package tak.server.feeds;

import java.io.Serializable;
import java.net.URL;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.collect.ComparisonChain;

import tak.server.feeds.DataFeed.DataFeedType;

/*
 * 
 * Value class that mirrors tak.server.feeds.DataFeed, but has JPA / hibernate dependencies that we 
 * don't want in the plugins library jar.
 * 
 */
@JsonInclude(Include.NON_NULL) 
@Entity
@Table(name = "data_feed")
public class DataFeedDTO implements Serializable, Comparable<DataFeedDTO> {

	private static final long serialVersionUID = -5463730874196814957L;
	
	protected Long id = 0L;
	protected String uuid = "";
	protected String name = "";
	protected String auth = "";
	protected boolean authRequired;
	protected String protocol;
	protected Integer port;
	protected String feedGroup;
	protected String iface;
	protected boolean archive;
	protected boolean anongroup;
	protected boolean archiveOnly;
	protected boolean sync;
	protected Integer coreVersion;
	protected String coreVersion2TlsVersions;
	protected int type;
	protected int syncCacheRetentionSeconds = 3600;
	protected boolean federated;
	protected boolean binaryPayloadWebsocketOnly = false;
    protected String groupVector;
    protected String predicateLang;
    protected URL dataSourceEndpoint;
    protected String predicate;
    protected String authType;
    
	public com.bbn.marti.config.DataFeed toInput() {
		com.bbn.marti.config.DataFeed datafeed = new com.bbn.marti.config.DataFeed();
    	datafeed.setAnongroup(anongroup);
    	datafeed.setArchive(archive);
    	datafeed.setArchiveOnly(archiveOnly);
    	datafeed.setAuth(null);
    	datafeed.setAuthRequired(false);
    	datafeed.setCoreVersion(null);
    	datafeed.setCoreVersion2TlsVersions(null);
    	datafeed.setFilter(null);
    	datafeed.setGroup(feedGroup);
    	datafeed.setIface(iface);
    	datafeed.setName(name);
    	datafeed.setPort(-1);
    	datafeed.setProtocol(null);
    	datafeed.setSync(sync);
    	datafeed.setType(DataFeedType.values()[type].toString());
    	datafeed.setUuid(uuid);
    	datafeed.setSyncCacheRetentionSeconds(syncCacheRetentionSeconds);
    	datafeed.setFederated(federated);
    	datafeed.setBinaryPayloadWebsocketOnly(binaryPayloadWebsocketOnly);
		
		return datafeed;
	}
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false, columnDefinition="long")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(name = "uuid", unique = true, nullable = false, columnDefinition="string")
    public String getUUID() {
        return uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }
    
    @Column(name = "name", unique = false, nullable = false, columnDefinition="string")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @Column(name = "auth", unique = false, nullable = true, columnDefinition="string")
    public String getAuth() {
    	return auth;
    }
    
    public void setAuth(String auth) {
    	this.auth = auth;
    }

    @Column(name = "type", unique = false, nullable = false, columnDefinition="integer")
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
	
    @Column(name = "port", unique = false, nullable = true, columnDefinition="integer")
    public Integer getPort() {
    	return port;
    }
    
    public void setPort(Integer port) {
    	this.port = port;
    }
    
    @Column(name = "auth_required", unique = false, nullable = true, columnDefinition="boolean")
    public boolean getAuthRequired() {
    	return authRequired;
    }
    
    public void setAuthRequired(boolean authRequired) {
    	this.authRequired = authRequired;
    }
    
    @Column(name = "protocol", unique = false, nullable = true, columnDefinition="string")
    public String getProtocol() {
    	return protocol;
    }
    
    public void setProtocol(String protocol) {
    	this.protocol = protocol;
    }

    @Column(name = "feed_group", unique = false, nullable = true, columnDefinition="string")
    public String getFeedGroup() {
    	return feedGroup;
    }
    
    public void setFeedGroup(String feedGroup) {
    	this.feedGroup = feedGroup;
    }
    
    @Column(name = "iface", unique = false, nullable = true, columnDefinition="string")
    public String getIface() {
    	return iface;
    }
    
    public void setIface(String iface) {
    	this.iface = iface;
    }
    
    @Column(name = "archive", unique = false, nullable = true, columnDefinition="boolean")
    public boolean getArchive() {
    	return archive;
    }
    
    public void setArchive(boolean archive) {
    	this.archive = archive;
    }
    
    @Column(name = "anongroup", unique = false, nullable = true, columnDefinition="boolean")
    public boolean getAnongroup() {
    	return anongroup;
    }
    
    public void setAnongroup(boolean anongroup) {
    	this.anongroup = anongroup;
    }
    
    @Column(name = "archive_only", unique = false, nullable = true, columnDefinition="boolean")
    public boolean getArchiveOnly() {
    	return archiveOnly;
    }
    
    public void setArchiveOnly(boolean archiveOnly) {
    	this.archiveOnly = archiveOnly;
    }

    @Column(name = "sync", unique = false, nullable = true, columnDefinition="boolean")
    public boolean isSync() {
        return sync;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }

    @Column(name = "core_version", unique = false, nullable = true, columnDefinition="integer")
    public Integer getCoreVersion() {
    	return coreVersion;
    }
    
    public void setCoreVersion(Integer coreVersion) {
    	this.coreVersion = coreVersion;
    }
    
    @Column(name = "core_version_tls_versions", unique = false, nullable = true, columnDefinition="string")
    public String getCoreVersion2TlsVersions() {
    	return coreVersion2TlsVersions;
    }
    
    public void setCoreVersion2TlsVersions(String coreVersion2TlsVersions) {
    	this.coreVersion2TlsVersions = coreVersion2TlsVersions;
    }
	
	@Column(name = "sync_cache_retention_seconds", unique = false, nullable = true, columnDefinition="integer")
	public int getSyncCacheRetentionSeconds() {
		return syncCacheRetentionSeconds;
	}

	public void setSyncCacheRetentionSeconds(int syncCacheRetentionSeconds) {
		this.syncCacheRetentionSeconds = syncCacheRetentionSeconds;
	}
	
    @Column(name = "federated", unique = false, nullable = true, columnDefinition="boolean")
    public boolean getFederated() {
    	return federated;
    }
    
    public void setFederated(boolean federated) {
    	this.federated = federated;
    }

    @Column(name = "binary_payload_websocket_only", unique = false, nullable = true, columnDefinition="boolean")
    public boolean getBinaryPayloadWebsocketOnly() {
		return binaryPayloadWebsocketOnly;
	}

	public void setBinaryPayloadWebsocketOnly(boolean binaryPayloadWebsocketOnly) {
		this.binaryPayloadWebsocketOnly = binaryPayloadWebsocketOnly;
	}

	@Column(name = "groups", columnDefinition = "bit varying")
    public String getGroupVector() {
        return groupVector;
    }

    public void setGroupVector(String groupVector) {
        this.groupVector = groupVector;
    }
    
    @Column(name = "predicate_lang", unique = false, nullable = false)
   	public String getPredicateLang() {
		return predicateLang;
	}

	public void setPredicateLang(String predicateLang) {
		this.predicateLang = predicateLang;
	}

    @Column(name = "data_source_endpoint", unique = false, nullable = false)
	public URL getDataSourceEndpoint() {
		return dataSourceEndpoint;
	}

	public void setDataSourceEndpoint(URL dataSourceEndpoint) {
		this.dataSourceEndpoint = dataSourceEndpoint;
	}

    @Column(name = "predicate", unique = false, nullable = false)
	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}

    @Column(name = "auth_type", unique = false, nullable = false)
	public String getAuthType() {
		return authType;
	}

	public void setAuthType(String authType) {
		this.authType = authType;
	}

	@Override
	public int compareTo(DataFeedDTO that) {
		return ComparisonChain.start().compare(this.getId(), that.getId()).result();
	}

	@Override
	public String toString() {
		return "DataFeedDTO [id=" + id + ", uuid=" + uuid + ", name=" + name + ", auth=" + auth + ", authRequired="
				+ authRequired + ", protocol=" + protocol + ", port=" + port + ", feedGroup=" + feedGroup + ", iface="
				+ iface + ", archive=" + archive + ", anongroup=" + anongroup + ", archiveOnly=" + archiveOnly
				+ ", sync=" + sync + ", coreVersion=" + coreVersion + ", coreVersion2TlsVersions="
				+ coreVersion2TlsVersions + ", type=" + type + ", syncCacheRetentionSeconds="
				+ syncCacheRetentionSeconds + ", federated=" + federated + ", binaryPayloadWebsocketOnly="
				+ binaryPayloadWebsocketOnly + ", groupVector=" + ((groupVector == null ? "null" : groupVector.length()) + "groups" ) + ", predicateLang=" + predicateLang
				+ ", dataSourceEndpoint=" + dataSourceEndpoint + ", predicate=" + predicate + ", authType=" + authType
				+ "]";
	}
}
