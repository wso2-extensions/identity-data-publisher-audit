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

package org.wso2.carbon.identity.data.publisher.audit.common;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.util.UserCoreUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Audit data publisher related utilities.
 */
public class AuditDataPublisherUtils {
    private static final Log log = LogFactory.getLog(AuditDataPublisherUtils.class);

    private AuditDataPublisherUtils() {     // Prevent initializing
    }

    /**
     * Get the tenant domains to publish to based on action holder tenant domain and user tenant domain.
     * The array of tenant domains that are returned by this method are the tenant domains which are relevant
     * for the data publishing.
     *
     * @param actionHolderTenantDomain Action holder tenant domain
     * @param userTenantDomain         User tenant domain
     * @return The list of tenant domains to publish to
     */
    public static String[] getTenantDomains(String actionHolderTenantDomain, String userTenantDomain) {

        List<String> tenantDomain = new ArrayList<>(2);
        if (isTenantDomainNotBlank(userTenantDomain)) {
            tenantDomain.add(userTenantDomain);
        }
        if (isTenantDomainNotBlank(actionHolderTenantDomain)) {
            tenantDomain.add(userTenantDomain);
        }
        return tenantDomain.toArray(new String[0]);
    }

    /**
     * Get the fully qualified action holder (userStoreDomain/user@tenantdomain).
     *
     * @param actionHolder             Username of the action holder
     * @param actionHolderTenantDomain The tenant domain of the action holder
     * @return The action holder
     */
    public static String getActionHolder(String actionHolder, String actionHolderTenantDomain) {
        String fullyQualifiedActionHolderName = null;
        try {
            String actionHolderUserStoreDomain = UserCoreUtil.getDomainName(
                    CarbonContext.getThreadLocalCarbonContext().getUserRealm().getRealmConfiguration());
            if (StringUtils.isNotBlank(actionHolder) && StringUtils.isNotBlank(actionHolderTenantDomain)
                    && StringUtils.isNotBlank(actionHolderUserStoreDomain)) {
                fullyQualifiedActionHolderName = actionHolderUserStoreDomain + "/" + actionHolder
                        + "@" + actionHolderTenantDomain;
            }
        } catch (UserStoreException e) {
            log.error("Failed to fetch action holder user store domain for user " + actionHolder);
        }
        return fullyQualifiedActionHolderName;
    }

    /**
     * Check if a tenant domain is blank.
     *
     * @param tenantDomain The tenant domain to validate
     * @return True if the tenant domain is blank
     */
    private static boolean isTenantDomainNotBlank(String tenantDomain) {

        return !StringUtils.isBlank(tenantDomain)
                && !tenantDomain.equalsIgnoreCase(AuditDataPublisherConstants.NOT_AVAILABLE);
    }

    /**
     * Get metadata array for different tenants with tenant domain.
     *
     * @param tenantDomain The tenant domain of the tenant
     * @return The meta data array for the tenant
     */
    public static Object[] getMetaDataArray(String tenantDomain) {

        Object[] metaData = new Object[1];
        if (StringUtils.isBlank(tenantDomain)) {
            metaData[0] = MultitenantConstants.SUPER_TENANT_ID;
        } else {
            metaData[0] = IdentityTenantUtil.getTenantId(tenantDomain);
        }
        return metaData;
    }

    /**
     * Get a string of comma separated values from an array
     *
     * @param values The values array which needs to be converted
     * @return The string of comma separated values
     */
    public static String getCommaSeparatedList(String[] values) {

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            stringBuilder.append(values[i]);
            if (i < values.length - 1) {
                stringBuilder.append(",");
            }
        }
        return stringBuilder.toString();
    }
}
