package com.bbn.user.registration;

import com.bbn.marti.remote.config.CoreConfigFacade;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.bbn.marti.remote.CoreConfig;


public class UserManagementAuthorizationProvider {

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(com.bbn.user.registration.UserManagementAuthorizationProvider.class);

    public synchronized String getRole() {
        CoreConfig coreConfig = CoreConfigFacade.getInstance();
        try {
            if (coreConfig == null
                    || coreConfig.getRemoteConfiguration() == null
                    || coreConfig.getRemoteConfiguration().getEmail() == null
                    || coreConfig.getRemoteConfiguration().getEmail().getWhitelist() == null
                    || coreConfig.getRemoteConfiguration().getEmail().getWhitelist().size() == 0) {
                return "ROLE_NONEXISTENT";
            }

            return "ROLE_NO_CLIENT_CERT";

        } catch (Exception e) {
            logger.error("exception in getRole", e);
            return "ROLE_NONEXISTENT";
        }
    }
}
