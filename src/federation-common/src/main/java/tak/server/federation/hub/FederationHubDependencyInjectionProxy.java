package tak.server.federation.hub;

import tak.server.federation.hub.broker.*;
import tak.server.federation.hub.policy.FederationHubPolicyManager;
import tak.server.federation.hub.broker.events.RestartServerEvent;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class FederationHubDependencyInjectionProxy implements ApplicationContextAware {
    private static ApplicationContext springContext;

    private static FederationHubDependencyInjectionProxy instance = null;

    public static FederationHubDependencyInjectionProxy getInstance() {
        if (instance == null) {
            synchronized (FederationHubDependencyInjectionProxy.class) {
                if (instance == null) {
                    instance = springContext.getBean(FederationHubDependencyInjectionProxy.class);
                }
            }
        }

        return instance;
    }

    public static ApplicationContext getSpringContext() {
        return springContext;
    }

    @SuppressWarnings("static-access")
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.springContext = context;
    }

    private FederationHubPolicyManager fedHubPolicyManager = null;

    public FederationHubPolicyManager fedHubPolicyManager() {
        if (fedHubPolicyManager == null) {
            synchronized (this) {
                if (fedHubPolicyManager == null) {
                    fedHubPolicyManager = springContext.getBean(FederationHubPolicyManager.class);
                }
            }
        }

        return fedHubPolicyManager;
    }

    private SSLConfig sslConfig = null;

    public SSLConfig sslConfig() {
        if (sslConfig == null) {
            synchronized (this) {
                if (sslConfig == null) {
                    sslConfig = springContext.getBean(SSLConfig.class);
                }
            }
        }

        return sslConfig;
    }

    private FederationHubServerConfig fedHubServerConfig = null;

    public FederationHubServerConfig fedHubServerConfig() {
        if (fedHubServerConfig == null) {
            synchronized (this) {
                if (fedHubServerConfig == null) {
                    fedHubServerConfig = springContext.getBean(FederationHubServerConfig.class);
                }
            }
        }

        return fedHubServerConfig;
    }

    private FederationHubBroker federationHubBroker = null;

    public FederationHubBroker federationHubBroker() {
        if (federationHubBroker == null) {
            synchronized (this) {
                if (federationHubBroker == null) {
                	federationHubBroker = springContext.getBean(FederationHubBroker.class);
                }
            }
        }

        return federationHubBroker;
    }

    private FederationHubBrokerMetrics federationHubBrokerMetrics = null;

    public FederationHubBrokerMetrics federationHubBrokerMetrics() {
        if (federationHubBrokerMetrics == null) {
            synchronized (this) {
                if (federationHubBrokerMetrics == null) {
                    federationHubBrokerMetrics = springContext.getBean(FederationHubBrokerMetrics.class);
                }
            }
        }
        return federationHubBrokerMetrics;
    }

    private HubConnectionStore hubConnectionStore = null;

    public HubConnectionStore hubConnectionStore() {
        if (hubConnectionStore == null) {
            synchronized (this) {
                if (hubConnectionStore == null) {
                	hubConnectionStore = springContext.getBean(HubConnectionStore.class);
                }
            }
        }

        return hubConnectionStore;
    }

    public void restartV2Server() {
        springContext.publishEvent(new RestartServerEvent(this));
    }
}