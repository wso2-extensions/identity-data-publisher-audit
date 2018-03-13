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

package org.wso2.carbon.identity.data.publisher.audit.idp.properties.impl;

import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.common.model.IdentityProvider;
import org.wso2.carbon.identity.application.common.model.IdentityProviderProperty;
import org.wso2.carbon.identity.core.model.IdentityEventListenerConfig;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.data.publisher.audit.common.AuditDataPublisherConstants;
import org.wso2.carbon.identity.data.publisher.audit.common.AuditDataPublisherUtils;
import org.wso2.carbon.identity.data.publisher.audit.idp.properties.internal.IdPPropertiesDataPublisherDataHolder;
import org.wso2.carbon.identity.data.publisher.audit.idp.properties.model.IdPProperties;
import org.wso2.carbon.idp.mgt.IdentityProviderManagementException;
import org.wso2.carbon.idp.mgt.listener.AbstractIdentityProviderMgtListener;
import org.wso2.carbon.idp.mgt.listener.IdentityProviderMgtListener;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * IdP Properties data publisher.
 */
public class ResidentIdPPropertiesDataPublisher extends AbstractIdentityProviderMgtListener {
    private static final Log log = LogFactory.getLog(ResidentIdPPropertiesDataPublisher.class);

    @Override
    public boolean doPostUpdateResidentIdP(IdentityProvider identityProvider, String tenantDomain)
            throws IdentityProviderManagementException {
        return handlePostIdPPropertyChange(identityProvider, tenantDomain);
    }

    @Override
    public boolean doPostAddResidentIdP(IdentityProvider identityProvider, String tenantDomain)
            throws IdentityProviderManagementException {
        return handlePostIdPPropertyChange(identityProvider, tenantDomain);
    }

    /**
     * Handles all identity provider change events.
     *
     * @param identityProvider The identity provider object
     * @param tenantDomain     The tenant domain in which the properties changes
     * @return True if successful
     * @throws IdentityProviderManagementException failed to handle properties update
     */
    public boolean handlePostIdPPropertyChange(IdentityProvider identityProvider, String tenantDomain)
            throws IdentityProviderManagementException {
        IdPProperties idPProperties = new IdPProperties();
        idPProperties.setIdPName(identityProvider.getIdentityProviderName());
        idPProperties.setTenantDomain(tenantDomain);
        idPProperties.setTimestamp(System.currentTimeMillis());

        // Setting the IdP properties
        Map<String, String> idPPropertiesMap = new HashMap<>();
        for (IdentityProviderProperty identityProviderProperty : identityProvider.getIdpProperties()) {
            idPPropertiesMap.put(identityProviderProperty.getName(), identityProviderProperty.getValue());
        }
        idPProperties.setProperties(idPPropertiesMap);

        // Setting the action holder
        String actionHolderTenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        String actionHolder = CarbonContext.getThreadLocalCarbonContext().getUsername();
        try {
            String actionHolderUserStoreDomain = UserCoreUtil.getDomainName(
                    CarbonContext.getThreadLocalCarbonContext().getUserRealm().getRealmConfiguration());
            if (StringUtils.isNotBlank(actionHolder) && StringUtils.isNotBlank(actionHolderTenantDomain)
                    && StringUtils.isNotBlank(actionHolderUserStoreDomain)) {
                idPProperties.setActionHolder(actionHolderUserStoreDomain + "/" + actionHolder
                        + "@" + actionHolderTenantDomain);
            }
        } catch (UserStoreException e) {
            log.error("Failed to fetch action holder user store domain for user " + actionHolder);
        }

        idPProperties.addParameter(AuditDataPublisherConstants.PUBLISHING_TENANT_DOMAINS,
                AuditDataPublisherUtils.getTenantDomains(actionHolderTenantDomain, tenantDomain));

        publishResidentIdPData(idPProperties);
        return true;
    }

    /**
     * Publish idp related data to IS Analytics.
     *
     * @param idPProperties The IdP properties to be published.
     */
    private void publishResidentIdPData(IdPProperties idPProperties) {
        Object[] payloadData = new Object[5];
        payloadData[0] = idPProperties.getIdPName();
        payloadData[1] = idPProperties.getTenantDomain();
        payloadData[2] = new Gson().toJson(idPProperties.getProperties());
        payloadData[3] = idPProperties.getActionHolder();
        payloadData[4] = idPProperties.getTimestamp();

        String[] publishingDomains =
                (String[]) idPProperties.getParameter(AuditDataPublisherConstants.PUBLISHING_TENANT_DOMAINS);
        if (publishingDomains != null && publishingDomains.length > 0) {
            try {
                FrameworkUtils.startTenantFlow(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
                for (String publishingDomain : publishingDomains) {
                    Object[] metadataArray = AuditDataPublisherUtils.getMetaDataArray(publishingDomain);
                    org.wso2.carbon.databridge.commons.Event event = new org.wso2.carbon.databridge.commons.Event
                            (AuditDataPublisherConstants.IDP_PROPERTIES_UPDATE_EVENT_STREAM_NAME, System
                                    .currentTimeMillis(), metadataArray, null, payloadData);
                    IdPPropertiesDataPublisherDataHolder.getInstance().getPublisherService().publish(event);
                    if (log.isDebugEnabled()) {
                        log.debug("Sending out event : " + event.toString());
                    }
                }
            } finally {
                FrameworkUtils.endTenantFlow();
            }
        }
    }

    @Override
    public boolean isEnable() {
        // Extended to change the default behaviour to not publish data is the config is missing
        IdentityEventListenerConfig identityEventListenerConfig = IdentityUtil.readEventListenerProperty(
                IdentityProviderMgtListener.class.getName(), this.getClass().getName());
        return identityEventListenerConfig != null
                && StringUtils.isNotBlank(identityEventListenerConfig.getEnable())
                && Boolean.parseBoolean(identityEventListenerConfig.getEnable());
    }

    @Override
    public int getDefaultOrderId() {
        return AuditDataPublisherConstants.IDP_MGT_LISTENER_DEFAULT_ORDER_ID;
    }
}
