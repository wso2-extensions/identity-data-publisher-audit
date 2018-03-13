/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.data.publisher.audit.user.operation.impl;

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.data.publisher.audit.common.AuditDataPublisherConstants;
import org.wso2.carbon.identity.data.publisher.audit.common.AuditDataPublisherUtils;
import org.wso2.carbon.identity.data.publisher.audit.user.operation.internal.UserOperationDataPublisherDataHolder;
import org.wso2.carbon.identity.data.publisher.audit.user.operation.model.UserData;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Publisher for user related operations.
 */
public class UserOperationDataPublisher extends AbstractEventHandler {
    private static final Log log = LogFactory.getLog(UserOperationDataPublisher.class);

    @Override
    public void handleEvent(Event event) throws IdentityEventException {
        switch (event.getEventName()) {
            case IdentityEventConstants.Event.POST_ADD_USER:
                handleAddUser(event);
                break;
            case IdentityEventConstants.Event.POST_DELETE_USER:
                handleDeleteUser(event);
                break;
            case IdentityEventConstants.Event.POST_UPDATE_CREDENTIAL:
            case IdentityEventConstants.Event.POST_UPDATE_CREDENTIAL_BY_ADMIN:
                handleUpdateCredential(event);
                break;
            case IdentityEventConstants.Event.POST_SET_USER_CLAIMS:
                handleSetUserClaims(event);
                break;
            default:
                if (log.isDebugEnabled()) {
                    log.debug("Ignored unsupported event " + event.getEventName());
                }
        }
    }

    /**
     * Handles POST_ADD_USER event.
     *
     * @param event The event related to the add user
     */
    private void handleAddUser(Event event) {
        UserData userData = getGeneralUserData(event);
        userData.setProfile((String) event.getEventProperties().get(IdentityEventConstants.EventProperty.PROFILE_NAME));

        // Adding new roles
        String[] roles = (String[]) event.getEventProperties().get(IdentityEventConstants.EventProperty.ROLE_LIST);
        if (roles != null && roles.length > 0) {
            userData.setNewRoleList(AuditDataPublisherUtils.getCommaSeparatedList(roles));
        }

        // Adding new claims
        Map claims = (Map) event.getEventProperties().get(IdentityEventConstants.EventProperty.USER_CLAIMS);
        if (claims == null) {
            claims = new HashMap();
        }
        userData.setClaims(new Gson().toJson(claims));

        publishUserData(userData);
    }

    /**
     * Handles POST_DELETE_USER event.
     *
     * @param event The event related to the delete user
     */
    private void handleDeleteUser(Event event) {
        UserData userData = getGeneralUserData(event);
        publishUserData(userData);
    }

    /**
     * Handle credential update related events.
     * <p>
     * Handles both POST_UPDATE_CREDENTIAL and POST_UPDATE_CREDENTIAL_BY_ADMIN
     *
     * @param event The event related to the credential update
     */
    private void handleUpdateCredential(Event event) {
        UserData userData = getGeneralUserData(event);
        publishUserData(userData);
    }

    /**
     * Handles POST_SET_USER_CLAIM and POST_SET_USER_CLAIMS events.
     *
     * @param event The event related to the set user claims.
     */
    private void handleSetUserClaims(Event event) {
        UserData userData = getGeneralUserData(event);

        // Adding updated claims
        Map claims = (Map) event.getEventProperties().get(IdentityEventConstants.EventProperty.USER_CLAIMS);
        if (claims == null) {
            claims = new HashMap();
        }
        userData.setClaims(new Gson().toJson(claims));

        publishUserData(userData);
    }

    /**
     * Publish user related data to IS Analytics.
     *
     * @param userData The user data to be published
     */
    private void publishUserData(UserData userData) {
        Object[] payloadData = new Object[10];
        payloadData[0] = userData.getAction();
        payloadData[1] = userData.getUsername();
        payloadData[2] = userData.getUserStoreDomain();
        payloadData[3] = userData.getTenantDomain();
        payloadData[4] = userData.getNewRoleList();
        payloadData[5] = userData.getDeletedRoleList();
        payloadData[6] = userData.getClaims();
        payloadData[7] = userData.getProfile();
        payloadData[8] = userData.getActionHolder();
        payloadData[9] = userData.getActionTimestamp();

        String[] publishingDomains = (String[]) userData.getParameter(AuditDataPublisherConstants.PUBLISHING_TENANT_DOMAINS);
        if (publishingDomains != null && publishingDomains.length > 0) {
            try {
                FrameworkUtils.startTenantFlow(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
                for (String publishingDomain : publishingDomains) {
                    Object[] metadataArray = AuditDataPublisherUtils.getMetaDataArray(publishingDomain);
                    org.wso2.carbon.databridge.commons.Event event = new org.wso2.carbon.databridge.commons.Event
                            (AuditDataPublisherConstants.OVERALL_USER_DATA_EVENT_STREAM_NAME, System
                                    .currentTimeMillis(), metadataArray, null, payloadData);
                    UserOperationDataPublisherDataHolder.getInstance().getPublisherService().publish(event);
                    if (log.isDebugEnabled()) {
                        log.debug("Sending out event : " + event.toString());
                    }
                }
            } finally {
                FrameworkUtils.endTenantFlow();
            }
        }
    }

    /**
     * Get the general user related data from event.
     *
     * @return General user related data in the event.
     */
    private UserData getGeneralUserData(Event event) {
        UserData userData = new UserData();
        userData.setActionTimestamp(System.currentTimeMillis());
        userData.setAction(event.getEventName());
        userData.setUsername((String) event.getEventProperties().get(IdentityEventConstants.EventProperty.USER_NAME));

        // Setting the action holder
        String actionHolderTenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        String actionHolder = CarbonContext.getThreadLocalCarbonContext().getUsername();
        try {
            String actionHolderUserStoreDomain = UserCoreUtil.getDomainName(
                    CarbonContext.getThreadLocalCarbonContext().getUserRealm().getRealmConfiguration());
            if (StringUtils.isNotBlank(actionHolder) && StringUtils.isNotBlank(actionHolderTenantDomain)
                    && StringUtils.isNotBlank(actionHolderUserStoreDomain)) {
                userData.setActionHolder(actionHolderUserStoreDomain + "/" + actionHolder
                        + "@" + actionHolderTenantDomain);
            }
        } catch (UserStoreException e) {
            log.error("Failed to fetch action holder user store domain for user " + actionHolder);
        }

        // Setting the tenant domain
        UserStoreManager userStoreManager = (UserStoreManager) event.getEventProperties()
                .get(IdentityEventConstants.EventProperty.USER_STORE_MANAGER);
        int userTenantId = userStoreManager.getRealmConfiguration().getTenantId();
        String userTenantDomain = IdentityTenantUtil.getTenantDomain(userTenantId);
        userData.setTenantDomain(userTenantDomain);

        // Setting the user store domain
        String userStoreDomain = UserCoreUtil.getDomainName(userStoreManager.getRealmConfiguration());
        if (StringUtils.isEmpty(userStoreDomain)) {
            userStoreDomain = UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME;
        }
        userData.setUserStoreDomain(userStoreDomain);

        // Adding additional properties
        userData.addParameter(AuditDataPublisherConstants.PUBLISHING_TENANT_DOMAINS,
                AuditDataPublisherUtils.getTenantDomains(actionHolderTenantDomain, userTenantDomain));

        return userData;
    }

    @Override
    public String getName() {
        return AuditDataPublisherConstants.USER_MGT_DAS_DATA_PUBLISHER;
    }
}
