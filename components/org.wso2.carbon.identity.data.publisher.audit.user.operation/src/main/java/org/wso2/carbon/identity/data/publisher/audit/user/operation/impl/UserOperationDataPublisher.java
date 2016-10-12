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
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

public class UserOperationDataPublisher extends AbstractEventHandler {

    private static final Log log = LogFactory.getLog(UserOperationDataPublisher.class);
    public static final Log LOG = LogFactory.getLog(UserOperationDataPublisher.class);

    @Override
    public void handleEvent(Event event) throws IdentityEventException {

        switch (event.getEventName()) {
            case IdentityEventConstants.Event.POST_UPDATE_CREDENTIAL:
                doPostUpdateCredential(getUsername(event), getCredentials(event), getUserStoreManager(event));
            case IdentityEventConstants.Event.POST_UPDATE_CREDENTIAL_BY_ADMIN:
                doPostUpdateCredentialByAdmin(getUsername(event), getCredentials(event), getUserStoreManager(event));
        }
    }

    public void doPostUpdateCredential(String userName, Object credential, UserStoreManager userStoreManager) {

        UserData userData = new UserData();
        userData.setAction(IdentityEventConstants.Event.POST_UPDATE_CREDENTIAL);
        String actionHolderTenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        String actionHolder = CarbonContext.getThreadLocalCarbonContext().getUsername();
        int userTenantId = userStoreManager.getRealmConfiguration().getTenantId();
        String userTenantDomain = IdentityTenantUtil.getTenantDomain(userTenantId);
        String userstoreDomain = UserCoreUtil.getDomainName(userStoreManager.getRealmConfiguration());
        if (StringUtils.isEmpty(userstoreDomain)) {
            userstoreDomain = UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME;
        }
        userData.setUserStore(userstoreDomain);
        userData.setTenantDomain(userTenantDomain);
        if (StringUtils.isNotBlank(actionHolder) && StringUtils.isNotBlank(actionHolderTenantDomain)) {
            userData.setAction(actionHolder + "@" + actionHolderTenantDomain);
        }
        userData.setUsername(userName);
        userData.setCredentials(credential);
        userData.addParameter(org.wso2.carbon.identity.data.publisher.audit.common.AuditDataPublisherConstants.TENANT_ID, AuditDataPublisherUtils
                .getTenantDomains(actionHolderTenantDomain, userTenantDomain));
        doPublishUserData(userData);

    }


    public void doPostUpdateCredentialByAdmin(String userName, Object credential, UserStoreManager
            userStoreManager) {

        UserData userData = new UserData();
        userData.setAction(IdentityEventConstants.Event.POST_UPDATE_CREDENTIAL_BY_ADMIN);
        String actionHolderTenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        String actionHolder = CarbonContext.getThreadLocalCarbonContext().getUsername();
        int userTenantId = userStoreManager.getRealmConfiguration().getTenantId();
        String userTenantDomain = IdentityTenantUtil.getTenantDomain(userTenantId);
        String userstoreDomain = UserCoreUtil.getDomainName(userStoreManager.getRealmConfiguration());
        if (StringUtils.isEmpty(userstoreDomain)) {
            userstoreDomain = UserCoreConstants.PRIMARY_DEFAULT_DOMAIN_NAME;
        }
        userData.setUserStore(userstoreDomain);
        userData.setTenantDomain(userTenantDomain);
        if (StringUtils.isNotBlank(actionHolder) && StringUtils.isNotBlank(actionHolderTenantDomain)) {
            userData.setAction(actionHolder + "@" + actionHolderTenantDomain);
        }
        userData.setUsername(userName);
        userData.setCredentials(credential);
        userData.addParameter(AuditDataPublisherConstants.TENANT_ID, AuditDataPublisherUtils
                .getTenantDomains(actionHolderTenantDomain, userTenantDomain));
        doPublishUserData(userData);
    }


    public void doPublishUserData(UserData userData) {

        Object[] payloadData = new Object[10];

        payloadData[0] = userData.getAction();
        payloadData[1] = userData.getUsername();
        payloadData[2] = userData.getUserStore();
        payloadData[3] = userData.getTenantDomain();
        payloadData[4] = userData.getNewRoles();
        payloadData[5] = userData.getDeletedRoles();
        payloadData[7] = userData.getClaimValues();
        payloadData[8] = userData.getProfile();
        payloadData[9] = userData.getActionHolder();

        String[] publishingDomains = (String[]) userData.getParameter(AuditDataPublisherConstants.TENANT_ID);
        if (publishingDomains != null && publishingDomains.length > 0) {
            try {
                FrameworkUtils.startTenantFlow(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
                for (String publishingDomain : publishingDomains) {
                    Object[] metadataArray = AuditDataPublisherUtils.getMetaDataArray(publishingDomain);
                    org.wso2.carbon.databridge.commons.Event event = new org.wso2.carbon.databridge.commons.Event
                            (AuditDataPublisherConstants.USER_OPERATION_EVENT_STREAM_NAME, System
                                    .currentTimeMillis(), metadataArray, null, payloadData);
                    UserOperationDataPublisherDataHolder.getInstance().getPublisherService().publish(event);
                    if (LOG.isDebugEnabled() && event != null) {
                        LOG.debug("Sending out event : " + event.toString());
                    }
                }
            } finally {
                FrameworkUtils.endTenantFlow();
            }
        }
    }

    @Override
    public String getName() {
        return AuditDataPublisherConstants.USER_MGT_DAS_DATA_PUBLISHER;
    }

    private UserStoreManager getUserStoreManager(Event event) {
        return (UserStoreManager) event.getEventProperties().get(IdentityEventConstants.EventProperty.USER_STORE_MANAGER);
    }

    private String getUsername(Event event) {
        return (String) event.getEventProperties().get(IdentityEventConstants.EventProperty.USER_NAME);
    }

    private String getCredentials(Event event) {
        return (String) event.getEventProperties().get(IdentityEventConstants.EventProperty.CREDENTIAL);
    }

}
