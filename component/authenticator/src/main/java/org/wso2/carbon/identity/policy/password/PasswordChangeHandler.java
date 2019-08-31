/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.policy.password;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.identity.governance.IdentityGovernanceException;
import org.wso2.carbon.identity.governance.common.IdentityConnectorConfig;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Event Handler class which handles password update by user, password update by
 * admin and add user events.
 * <p>
 * This updates the http://wso2.org/claims/lastPasswordChangedTimestamp claim
 * upon the password change. This also publishes the password change event to IS
 * Analytics.
 */
public class PasswordChangeHandler extends AbstractEventHandler implements IdentityConnectorConfig {
    private static final Log log = LogFactory.getLog(PasswordChangeHandler.class);

    @Override
    public void handleEvent(Event event) throws IdentityEventException {
        // Fetching event properties
        String username = (String) event.getEventProperties().get(IdentityEventConstants.EventProperty.USER_NAME);
        UserStoreManager userStoreManager = (UserStoreManager) event.getEventProperties()
                .get(IdentityEventConstants.EventProperty.USER_STORE_MANAGER);
        long timestamp = System.currentTimeMillis();

        // Check minimum password lifetime if changed by a user
        if (IdentityEventConstants.Event.PRE_UPDATE_CREDENTIAL.equals(event.getEventName())) {
            long lastPasswordTimestamp = 0;
            String[] claims = { PasswordPolicyConstants.LAST_CREDENTIAL_UPDATE_TIMESTAMP_CLAIM };
            try {
                Map<String, String> currentClaimMap = userStoreManager.getUserClaimValues(username, claims, null);
                String strlastPasswordTimestamp = currentClaimMap
                        .get(PasswordPolicyConstants.LAST_CREDENTIAL_UPDATE_TIMESTAMP_CLAIM);
                if (log.isDebugEnabled()) {
                    log.debug("The claim uri " + PasswordPolicyConstants.LAST_CREDENTIAL_UPDATE_TIMESTAMP_CLAIM + " of "
                            + username + " contains '" + strlastPasswordTimestamp + "'");
                }
                if (strlastPasswordTimestamp != null) {
                    lastPasswordTimestamp = Long.parseLong(strlastPasswordTimestamp);
                }
            } catch (UserStoreException e) {
                throw new IdentityEventException(
                        "An Error Occurred when getting last password change timestamp. Please contact admin.", e);
            }

            String tenantDomain = (String) event.getEventProperties()
                    .get(IdentityEventConstants.EventProperty.TENANT_DOMAIN);
            int minLifetime = 0;
            String strMinLifetime = PasswordPolicyUtils.getIdentityEventProperty(tenantDomain,
                    PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_MIN_LIFETIME_IN_DAYS);
            if (strMinLifetime != null) {
                minLifetime = Integer.parseInt(strMinLifetime);
            }
            long differentTime = timestamp - lastPasswordTimestamp;
            if (differentTime < minLifetime * 86400000) {
                throw PasswordPolicyUtils.handleEventException(
                        PasswordPolicyConstants.ErrorMessages.ERROR_CODE_PASSWORD_MIN_LIFETIME_VIOLATE, null);
            }
        }

        // Updating the last password changed claim
        if (IdentityEventConstants.Event.POST_UPDATE_CREDENTIAL.equals(event.getEventName())
                || IdentityEventConstants.Event.POST_UPDATE_CREDENTIAL_BY_ADMIN.equals(event.getEventName())) {
            Map<String, String> claimMap = new HashMap<>();
            claimMap.put(PasswordPolicyConstants.LAST_CREDENTIAL_UPDATE_TIMESTAMP_CLAIM, Long.toString(timestamp));
            try {
                userStoreManager.setUserClaimValues(username, claimMap, null);
                if (log.isDebugEnabled()) {
                    log.debug("The claim uri " + PasswordPolicyConstants.LAST_CREDENTIAL_UPDATE_TIMESTAMP_CLAIM + " of "
                            + username + " updated with the current timestamp");
                }
            } catch (UserStoreException e) {
                throw new IdentityEventException("An Error Occurred in updating the password. Please contact admin.",
                        e);
            }
        }
    }

    @Override
    public String getName() {
        return PasswordPolicyConstants.PASSWORD_CHANGE_EVENT_HANDLER_NAME;
    }

    @Override
    public String getFriendlyName() {
        return PasswordPolicyConstants.CONNECTOR_CONFIG_FRIENDLY_NAME;
    }

    @Override
    public String getCategory() {
        return PasswordPolicyConstants.CONNECTOR_CONFIG_CATEGORY;
    }

    @Override
    public String getSubCategory() {
        return PasswordPolicyConstants.CONNECTOR_CONFIG_SUB_CATEGORY;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Map<String, String> getPropertyNameMapping() {
        Map<String, String> nameMapping = new HashMap<>();
        nameMapping.put(PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_MIN_LIFETIME_IN_DAYS,
                PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_MIN_LIFETIME_IN_DAYS_DISPLAYED_NAME);
        nameMapping.put(PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_EXPIRY_IN_DAYS,
                PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_EXPIRY_IN_DAYS_DISPLAYED_NAME);
        nameMapping.put(PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS,
                PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS_DISPLAYED_NAME);
        nameMapping.put(PasswordPolicyConstants.CONNECTOR_CONFIG_PRIOR_REMINDER_TIME_IN_DAYS,
                PasswordPolicyConstants.CONNECTOR_CONFIG_PRIOR_REMINDER_TIME_IN_DAYS_DISPLAYED_NAME);
        return nameMapping;
    }

    @Override
    public Map<String, String> getPropertyDescriptionMapping() {
        Map<String, String> nameMapping = new HashMap<>();
        nameMapping.put(PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_MIN_LIFETIME_IN_DAYS,
                PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_MIN_LIFETIME_IN_DAYS_DESCRIPTION);
        nameMapping.put(PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_EXPIRY_IN_DAYS,
                PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_EXPIRY_IN_DAYS_DESCRIPTION);
        nameMapping.put(PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS,
                PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS_DESCRIPTION);
        nameMapping.put(PasswordPolicyConstants.CONNECTOR_CONFIG_PRIOR_REMINDER_TIME_IN_DAYS,
                PasswordPolicyConstants.CONNECTOR_CONFIG_PRIOR_REMINDER_TIME_IN_DAYS_DESCRIPTION);
        return nameMapping;
    }

    @Override
    public String[] getPropertyNames() {
        return PasswordPolicyUtils.getPasswordExpiryPropertyNames();
    }

    @Override
    public Properties getDefaultPropertyValues(String tenantDomain) throws IdentityGovernanceException {
        Properties properties = new Properties();

        // Setting the password min lifetime in days default value
        String passwordMinLifetimeInDays = PasswordPolicyUtils.getIdentityEventProperty(tenantDomain,
                PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_MIN_LIFETIME_IN_DAYS);
        if (passwordMinLifetimeInDays == null) { // To avoid null pointer exceptions if user had not added module config
            passwordMinLifetimeInDays = Integer
                    .toString(PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_MIN_LIFETIME_IN_DAYS_DEFAULT_VALUE);
            if (log.isDebugEnabled()) {
                log.debug("Using the default property value: "
                        + PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_MIN_LIFETIME_IN_DAYS_DEFAULT_VALUE
                        + " for the " + "configuration: "
                        + PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_MIN_LIFETIME_IN_DAYS
                        + " because no module configuration is present.");
            }
        }
        properties.setProperty(PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_MIN_LIFETIME_IN_DAYS,
                passwordMinLifetimeInDays);

        // Setting the password expiry in days default value
        String passwordExpiryInDays = PasswordPolicyUtils.getIdentityEventProperty(tenantDomain,
                PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_EXPIRY_IN_DAYS);
        if (passwordExpiryInDays == null) { // To avoid null pointer exceptions if user had not added module config
            passwordExpiryInDays = Integer
                    .toString(PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_EXPIRY_IN_DAYS_DEFAULT_VALUE);
            if (log.isDebugEnabled()) {
                log.debug("Using the default property value: "
                        + PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_EXPIRY_IN_DAYS_DEFAULT_VALUE + " for the "
                        + "configuration: " + PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_EXPIRY_IN_DAYS
                        + " because no module configuration is present.");
            }
        }
        properties.setProperty(PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_EXPIRY_IN_DAYS, passwordExpiryInDays);

        // Setting the enable email notifications default value
        String enableDataPublishing = PasswordPolicyUtils.getIdentityEventProperty(tenantDomain,
                PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS);
        if (enableDataPublishing == null) { // To avoid null pointer exceptions if user had not added module config
            enableDataPublishing = Boolean
                    .toString(PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS_DEFAULT_VALUE);
            if (log.isDebugEnabled()) {
                log.debug("Using the default property value: "
                        + PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS_DEFAULT_VALUE
                        + " for the " + "configuration: "
                        + PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS
                        + " because no module configuration is present.");
            }
        }
        properties.setProperty(PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS,
                enableDataPublishing);

        // Setting the prior notice in days default value
        String priorReminderTimeInDays = PasswordPolicyUtils.getIdentityEventProperty(tenantDomain,
                PasswordPolicyConstants.CONNECTOR_CONFIG_PRIOR_REMINDER_TIME_IN_DAYS);
        if (priorReminderTimeInDays == null) { // To avoid null pointer exceptions if user had not added module config
            priorReminderTimeInDays = Integer
                    .toString(PasswordPolicyConstants.CONNECTOR_CONFIG_PRIOR_NOTICE_TIME_IN_DAYS_DEFAULT_VALUE);
            if (log.isDebugEnabled()) {
                log.debug("Using the default property value: "
                        + PasswordPolicyConstants.CONNECTOR_CONFIG_PRIOR_NOTICE_TIME_IN_DAYS_DEFAULT_VALUE + " for the "
                        + "configuration: " + PasswordPolicyConstants.CONNECTOR_CONFIG_PRIOR_REMINDER_TIME_IN_DAYS
                        + " because no module configuration is present.");
            }
        }
        properties.setProperty(PasswordPolicyConstants.CONNECTOR_CONFIG_PRIOR_REMINDER_TIME_IN_DAYS,
                priorReminderTimeInDays);

        return properties;
    }

    @Override
    public Map<String, String> getDefaultPropertyValues(String[] propertyNames, String tenantDomain)
            throws IdentityGovernanceException {
        return null;
    }
}
