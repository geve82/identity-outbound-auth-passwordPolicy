/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.extension.identity.authenticator.passwordpolicy.test;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.common.IdentityApplicationManagementException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.bean.ModuleConfiguration;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.governance.IdentityGovernanceException;
import org.wso2.carbon.identity.policy.password.PasswordChangeHandler;
import org.wso2.carbon.identity.policy.password.PasswordPolicyConstants;
import org.wso2.carbon.identity.policy.password.PasswordPolicyUtils;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.Map;
import java.util.Properties;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@PrepareForTest({IdentityTenantUtil.class, IdentityUtil.class, PasswordPolicyUtils.class})
public class PasswordChangeHandlerTest {
    private static final String TENANT_DOMAIN = "carbon.super";
    private static final int TENANT_ID = -1234;
    private static final String USERNAME = "user";
    private PasswordChangeHandler passwordChangeHandler;
    @Mock
    private UserStoreManager userStoreManager;
    @Mock
    private UserRealm userRealm;
    @Mock
    private RealmService realmService;
    @Captor
    private ArgumentCaptor<Map<String, String>> claimValueArguementCaptor;

    @BeforeMethod
    public void setUp() {
        passwordChangeHandler = new PasswordChangeHandler();
        initMocks(this);
    }
    
    @Test
    public void testHandlePreUpdateCredentialByAdminEvent()
            throws UserStoreException, IdentityApplicationManagementException {
        mockStatic(IdentityTenantUtil.class);
        mockStatic(IdentityUtil.class);
        mockStatic(PasswordPolicyUtils.class);

        when(IdentityTenantUtil.getTenantId(TENANT_DOMAIN)).thenReturn(TENANT_ID);
        when(IdentityTenantUtil.getRealmService()).thenReturn(realmService);
        when(realmService.getTenantUserRealm(TENANT_ID)).thenReturn(userRealm);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);

        RealmConfiguration realmConfig = new RealmConfiguration();
        realmConfig.getUserStoreProperties().put("DomainName", TENANT_DOMAIN);
        when(userStoreManager.getRealmConfiguration()).thenReturn(realmConfig);

        when(PasswordPolicyUtils.getIdentityEventProperty(TENANT_DOMAIN,
                PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS)).thenReturn("true");

        Event event = new Event("PRE_UPDATE_CREDENTIAL");
        event.getEventProperties().put(IdentityEventConstants.EventProperty.USER_NAME, USERNAME);
        event.getEventProperties().put(IdentityEventConstants.EventProperty.USER_STORE_MANAGER, userStoreManager);
        event.getEventProperties().put(IdentityEventConstants.EventProperty.TENANT_DOMAIN, TENANT_DOMAIN);

        try {
            passwordChangeHandler.handleEvent(event);   // Shouldn't throw exception
        } catch (IdentityEventException e) {
            Assert.fail("The authenticator failed the authentication flow");
        }
    }

    @Test
    public void testHandlePostUpdateCredentialByAdminEvent()
            throws UserStoreException, IdentityApplicationManagementException {
        mockStatic(IdentityTenantUtil.class);
        mockStatic(IdentityUtil.class);
        mockStatic(PasswordPolicyUtils.class);

        when(IdentityTenantUtil.getTenantId(TENANT_DOMAIN)).thenReturn(TENANT_ID);
        when(IdentityTenantUtil.getRealmService()).thenReturn(realmService);
        when(realmService.getTenantUserRealm(TENANT_ID)).thenReturn(userRealm);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);

        RealmConfiguration realmConfig = new RealmConfiguration();
        realmConfig.getUserStoreProperties().put("DomainName", TENANT_DOMAIN);
        when(userStoreManager.getRealmConfiguration()).thenReturn(realmConfig);

        when(PasswordPolicyUtils.getIdentityEventProperty(TENANT_DOMAIN,
                PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS)).thenReturn("true");

        Event event = new Event("POST_UPDATE_CREDENTIAL_BY_ADMIN");
        event.getEventProperties().put(IdentityEventConstants.EventProperty.USER_NAME, USERNAME);
        event.getEventProperties().put(IdentityEventConstants.EventProperty.USER_STORE_MANAGER, userStoreManager);
        event.getEventProperties().put(IdentityEventConstants.EventProperty.TENANT_DOMAIN, TENANT_DOMAIN);

        try {
            passwordChangeHandler.handleEvent(event);   // Shouldn't throw user store exception
        } catch (IdentityEventException e) {
            Assert.fail("The authenticator failed the authentication flow");
        }

        verify(userStoreManager, times(1))
                .setUserClaimValues(eq(USERNAME), claimValueArguementCaptor.capture(), isNull(String.class));

        Map<String, String> claims = claimValueArguementCaptor.getValue();
        Assert.assertEquals(claims.size(), 1);
        Assert.assertTrue(claims.containsKey(PasswordPolicyConstants.LAST_CREDENTIAL_UPDATE_TIMESTAMP_CLAIM));
    }

    @Test
    public void testHandlePostUpdateCredentialEvent()
            throws UserStoreException, IdentityApplicationManagementException {
        mockStatic(IdentityTenantUtil.class);
        mockStatic(IdentityUtil.class);
        mockStatic(PasswordPolicyUtils.class);

        when(IdentityTenantUtil.getTenantId(TENANT_DOMAIN)).thenReturn(TENANT_ID);
        when(IdentityTenantUtil.getRealmService()).thenReturn(realmService);
        when(realmService.getTenantUserRealm(TENANT_ID)).thenReturn(userRealm);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);

        RealmConfiguration realmConfig = new RealmConfiguration();
        realmConfig.getUserStoreProperties().put("DomainName", "domain");
        when(userStoreManager.getRealmConfiguration()).thenReturn(realmConfig);

        when(PasswordPolicyUtils.getIdentityEventProperty(TENANT_DOMAIN,
                PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS)).thenReturn("true");

        Event event = new Event("POST_UPDATE_CREDENTIAL");
        event.getEventProperties().put(IdentityEventConstants.EventProperty.USER_NAME, USERNAME);
        event.getEventProperties().put(IdentityEventConstants.EventProperty.USER_STORE_MANAGER, userStoreManager);
        event.getEventProperties().put(IdentityEventConstants.EventProperty.TENANT_DOMAIN, TENANT_DOMAIN);

        try {
            passwordChangeHandler.handleEvent(event);   // Shouldn't throw user store exception
        } catch (IdentityEventException e) {
            Assert.fail("The authenticator failed the authentication flow");
        }

        verify(userStoreManager, times(1))
                .setUserClaimValues(eq(USERNAME), claimValueArguementCaptor.capture(), isNull(String.class));

        Map<String, String> claims = claimValueArguementCaptor.getValue();
        Assert.assertEquals(claims.size(), 1);
        Assert.assertTrue(claims.containsKey(PasswordPolicyConstants.LAST_CREDENTIAL_UPDATE_TIMESTAMP_CLAIM));
    }

    @Test
    public void testHandleAddUserEvent() throws UserStoreException, IdentityApplicationManagementException {
        mockStatic(IdentityTenantUtil.class);
        mockStatic(IdentityUtil.class);
        mockStatic(PasswordPolicyUtils.class);

        when(IdentityTenantUtil.getTenantId(TENANT_DOMAIN)).thenReturn(TENANT_ID);
        when(IdentityTenantUtil.getRealmService()).thenReturn(realmService);
        when(realmService.getTenantUserRealm(TENANT_ID)).thenReturn(userRealm);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);

        RealmConfiguration realmConfig = new RealmConfiguration();
        realmConfig.getUserStoreProperties().put("DomainName", "domain");
        when(userStoreManager.getRealmConfiguration()).thenReturn(realmConfig);

        when(PasswordPolicyUtils.getIdentityEventProperty(TENANT_DOMAIN,
                PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS)).thenReturn("true");

        Event event = new Event("POST_UPDATE_CREDENTIAL");
        event.getEventProperties().put(IdentityEventConstants.EventProperty.USER_NAME, USERNAME);
        event.getEventProperties().put(IdentityEventConstants.EventProperty.USER_STORE_MANAGER, userStoreManager);
        event.getEventProperties().put(IdentityEventConstants.EventProperty.TENANT_DOMAIN, TENANT_DOMAIN);

        try {
            passwordChangeHandler.handleEvent(event);   // Shouldn't throw user store exception
        } catch (IdentityEventException e) {
            Assert.fail("The authenticator failed the authentication flow");
        }

        verify(userStoreManager, times(1))
                .setUserClaimValues(eq(USERNAME), claimValueArguementCaptor.capture(), isNull(String.class));

        Map<String, String> claims = claimValueArguementCaptor.getValue();
        Assert.assertEquals(claims.size(), 1);
        Assert.assertTrue(claims.containsKey(PasswordPolicyConstants.LAST_CREDENTIAL_UPDATE_TIMESTAMP_CLAIM));
    }

    @Test
    public void testHandleAddUserEventWithDataPublishingDisabled() throws UserStoreException, IdentityApplicationManagementException {
        mockStatic(IdentityTenantUtil.class);
        mockStatic(IdentityUtil.class);
        mockStatic(PasswordPolicyUtils.class);

        when(IdentityTenantUtil.getTenantId(TENANT_DOMAIN)).thenReturn(TENANT_ID);
        when(IdentityTenantUtil.getRealmService()).thenReturn(realmService);
        when(realmService.getTenantUserRealm(TENANT_ID)).thenReturn(userRealm);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);

        RealmConfiguration realmConfig = new RealmConfiguration();
        realmConfig.getUserStoreProperties().put("DomainName", "domain");
        when(userStoreManager.getRealmConfiguration()).thenReturn(realmConfig);

        when(PasswordPolicyUtils.getIdentityEventProperty(TENANT_DOMAIN,
                PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS)).thenReturn("false");

        Event event = new Event("POST_UPDATE_CREDENTIAL");
        event.getEventProperties().put(IdentityEventConstants.EventProperty.USER_NAME, USERNAME);
        event.getEventProperties().put(IdentityEventConstants.EventProperty.USER_STORE_MANAGER, userStoreManager);
        event.getEventProperties().put(IdentityEventConstants.EventProperty.TENANT_DOMAIN, TENANT_DOMAIN);

        try {
            passwordChangeHandler.handleEvent(event);   // Shouldn't throw user store exception
        } catch (IdentityEventException e) {
            Assert.fail("The authenticator failed the authentication flow");
        }

        verify(userStoreManager, times(1))
                .setUserClaimValues(eq(USERNAME), claimValueArguementCaptor.capture(), isNull(String.class));

        Map<String, String> claims = claimValueArguementCaptor.getValue();
        Assert.assertEquals(claims.size(), 1);
        Assert.assertTrue(claims.containsKey(PasswordPolicyConstants.LAST_CREDENTIAL_UPDATE_TIMESTAMP_CLAIM));
    }

    @Test(expectedExceptions = {IdentityEventException.class})
    public void testHandleEventWithUserStoreExceptionInSetLastPasswordUpdateUserClaim()
            throws UserStoreException, IdentityEventException {
        mockStatic(IdentityTenantUtil.class);
        mockStatic(IdentityUtil.class);
        mockStatic(PasswordPolicyUtils.class);

        when(IdentityTenantUtil.getTenantId(TENANT_DOMAIN)).thenReturn(TENANT_ID);
        when(IdentityTenantUtil.getRealmService()).thenReturn(realmService);
        when(realmService.getTenantUserRealm(TENANT_ID)).thenReturn(userRealm);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);

        RealmConfiguration realmConfig = new RealmConfiguration();
        realmConfig.getUserStoreProperties().put("DomainName", "domain");
        when(userStoreManager.getRealmConfiguration()).thenReturn(realmConfig);

        when(PasswordPolicyUtils.getIdentityEventProperty(TENANT_DOMAIN,
                PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS)).thenReturn("true");

        Event event = new Event("POST_UPDATE_CREDENTIAL");
        event.getEventProperties().put(IdentityEventConstants.EventProperty.USER_NAME, USERNAME);
        event.getEventProperties().put(IdentityEventConstants.EventProperty.USER_STORE_MANAGER, userStoreManager);
        event.getEventProperties().put(IdentityEventConstants.EventProperty.TENANT_DOMAIN, TENANT_DOMAIN);

        doThrow(new org.wso2.carbon.user.core.UserStoreException()).when(userStoreManager)
                .setUserClaimValues(eq(USERNAME), Matchers.<Map<String, String>>any(), isNull(String.class));

        passwordChangeHandler.handleEvent(event);

        verify(userStoreManager, times(1))
                .setUserClaimValues(eq(USERNAME), claimValueArguementCaptor.capture(), isNull(String.class));

        Map<String, String> claims = claimValueArguementCaptor.getValue();
        Assert.assertEquals(claims.size(), 1);
        Assert.assertTrue(claims.containsKey(PasswordPolicyConstants.LAST_CREDENTIAL_UPDATE_TIMESTAMP_CLAIM));
    }

    @Test
    public void testGetName() {
        Assert.assertEquals(passwordChangeHandler.getName(),
                PasswordPolicyConstants.PASSWORD_CHANGE_EVENT_HANDLER_NAME);
    }

    @Test
    public void testGetFriendlyName() {
        Assert.assertEquals(passwordChangeHandler.getFriendlyName(),
                PasswordPolicyConstants.CONNECTOR_CONFIG_FRIENDLY_NAME);
    }

    @Test
    public void testGetCategory() {
        Assert.assertEquals(passwordChangeHandler.getCategory(),
                PasswordPolicyConstants.CONNECTOR_CONFIG_CATEGORY);
    }

    @Test
    public void testGetSubCategory() {
        Assert.assertEquals(passwordChangeHandler.getSubCategory(),
                PasswordPolicyConstants.CONNECTOR_CONFIG_SUB_CATEGORY);
    }

    @Test
    public void testGetOrder() {
        Assert.assertEquals(passwordChangeHandler.getOrder(), 0);
    }

    @Test
    public void testGetPropertyNameMapping() {
        Map<String, String> propertyNameMapping = passwordChangeHandler.getPropertyNameMapping();
        Assert.assertEquals(propertyNameMapping.size(), 4);
        Assert.assertEquals(
                propertyNameMapping.get(PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_MIN_LIFETIME_IN_DAYS),
                PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_MIN_LIFETIME_IN_DAYS_DISPLAYED_NAME
        );
        Assert.assertEquals(
                propertyNameMapping.get(PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_EXPIRY_IN_DAYS),
                PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_EXPIRY_IN_DAYS_DISPLAYED_NAME
        );
        Assert.assertEquals(
                propertyNameMapping.get(PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS),
                PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS_DISPLAYED_NAME
        );
        Assert.assertEquals(
                propertyNameMapping.get(PasswordPolicyConstants.CONNECTOR_CONFIG_PRIOR_REMINDER_TIME_IN_DAYS),
                PasswordPolicyConstants.CONNECTOR_CONFIG_PRIOR_REMINDER_TIME_IN_DAYS_DISPLAYED_NAME
        );
    }

    @Test
    public void testGetPropertyDescriptionMapping() {
        Map<String, String> propertyDescriptionMapping = passwordChangeHandler.getPropertyDescriptionMapping();
        Assert.assertEquals(propertyDescriptionMapping.size(), 4);
        Assert.assertEquals(
                propertyDescriptionMapping.get(PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_MIN_LIFETIME_IN_DAYS),
                PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_MIN_LIFETIME_IN_DAYS_DESCRIPTION
        );
        Assert.assertEquals(
                propertyDescriptionMapping.get(PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_EXPIRY_IN_DAYS),
                PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_EXPIRY_IN_DAYS_DESCRIPTION
        );
        Assert.assertEquals(
                propertyDescriptionMapping.get(PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS),
                PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS_DESCRIPTION
        );
        Assert.assertEquals(
                propertyDescriptionMapping.get(PasswordPolicyConstants.CONNECTOR_CONFIG_PRIOR_REMINDER_TIME_IN_DAYS),
                PasswordPolicyConstants.CONNECTOR_CONFIG_PRIOR_REMINDER_TIME_IN_DAYS_DESCRIPTION
        );
    }

    @Test
    public void testGetPropertyNames() {
        String[] propertyNames = passwordChangeHandler.getPropertyNames();
        Assert.assertEquals(propertyNames.length, 4);
        Assert.assertEquals(propertyNames[0], PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_MIN_LIFETIME_IN_DAYS);
        Assert.assertEquals(propertyNames[1], PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_EXPIRY_IN_DAYS);
        Assert.assertEquals(propertyNames[2], PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS);
        Assert.assertEquals(propertyNames[3], PasswordPolicyConstants.CONNECTOR_CONFIG_PRIOR_REMINDER_TIME_IN_DAYS);
    }

    @Test
    public void testGetDefaultPropertyValues() throws IdentityGovernanceException {
        mockStatic(PasswordPolicyUtils.class);

        when(PasswordPolicyUtils.getIdentityEventProperty(TENANT_DOMAIN,
                PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_MIN_LIFETIME_IN_DAYS)).thenReturn("1");
        when(PasswordPolicyUtils.getIdentityEventProperty(TENANT_DOMAIN,
                PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_EXPIRY_IN_DAYS)).thenReturn("30");
        when(PasswordPolicyUtils.getIdentityEventProperty(TENANT_DOMAIN,
                PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS)).thenReturn("true");
        when(PasswordPolicyUtils.getIdentityEventProperty(TENANT_DOMAIN,
                PasswordPolicyConstants.CONNECTOR_CONFIG_PRIOR_REMINDER_TIME_IN_DAYS)).thenReturn("2");

        ModuleConfiguration moduleConfiguration = mock(ModuleConfiguration.class);
        Whitebox.setInternalState(passwordChangeHandler, "configs", moduleConfiguration);
        Properties moduleProperties = new Properties();
        moduleProperties.setProperty(PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_EXPIRY_IN_DAYS, "13");
        when(moduleConfiguration.getModuleProperties()).thenReturn(moduleProperties);

        Properties defaultPropertyValues = passwordChangeHandler.getDefaultPropertyValues(TENANT_DOMAIN);
        Assert.assertEquals(defaultPropertyValues.size(), 4);
        Assert.assertEquals(
                defaultPropertyValues.get(PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_MIN_LIFETIME_IN_DAYS),
                "1"
        );
        Assert.assertEquals(
                defaultPropertyValues.get(PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_EXPIRY_IN_DAYS),
                "30"
        );
        Assert.assertEquals(
                defaultPropertyValues.get(PasswordPolicyConstants.CONNECTOR_CONFIG_ENABLE_EMAIL_NOTIFICATIONS),
                "true"
        );
        Assert.assertEquals(
                defaultPropertyValues.get(PasswordPolicyConstants.CONNECTOR_CONFIG_PRIOR_REMINDER_TIME_IN_DAYS),
                "2"
        );
    }

    @Test
    public void testGetDefaultPropertyValuesWithPropertyNames() throws IdentityGovernanceException {
        Assert.assertNull(passwordChangeHandler.getDefaultPropertyValues(
                new String[]{PasswordPolicyConstants.CONNECTOR_CONFIG_PASSWORD_EXPIRY_IN_DAYS},
                TENANT_DOMAIN
        ));
    }

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }
}
