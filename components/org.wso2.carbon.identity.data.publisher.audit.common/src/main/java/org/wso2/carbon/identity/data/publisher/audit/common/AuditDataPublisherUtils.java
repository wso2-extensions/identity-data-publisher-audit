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
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;

/**
 * Audit data publisher related utilities.
 */
public class AuditDataPublisherUtils {
    private AuditDataPublisherUtils() {     // Prevent initializing
    }

    /**
     * Get the tenant domains to publish to based on action holder tenant domain and user tenant domain.
     *
     * @param spTenantDomain   Action holder tenant domain
     * @param userTenantDomain User tenant domain
     * @return The list of tenant domains to publish to
     */
    public static String[] getTenantDomains(String spTenantDomain, String userTenantDomain) {
        String[] tenantDomains;
        if (StringUtils.isBlank(userTenantDomain) ||
                userTenantDomain.equalsIgnoreCase(AuditDataPublisherConstants.NOT_AVAILABLE)) {
            tenantDomains = new String[]{spTenantDomain};
        } else if (StringUtils.isBlank(spTenantDomain) ||
                userTenantDomain.equalsIgnoreCase(AuditDataPublisherConstants.NOT_AVAILABLE)) {
            tenantDomains = new String[]{userTenantDomain};
        } else if (spTenantDomain.equalsIgnoreCase(userTenantDomain)) {
            tenantDomains = new String[]{userTenantDomain};
        } else {
            tenantDomains = new String[]{userTenantDomain, spTenantDomain};
        }
        return tenantDomains;
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
