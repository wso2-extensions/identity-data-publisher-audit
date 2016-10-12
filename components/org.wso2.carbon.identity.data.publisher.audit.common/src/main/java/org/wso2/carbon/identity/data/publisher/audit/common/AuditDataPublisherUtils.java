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

public class AuditDataPublisherUtils {

    public static String[] getTenantDomains(String spTenantDomain, String userTenantDomain) {

        if (StringUtils.isBlank(userTenantDomain) || userTenantDomain.equalsIgnoreCase(AuditDataPublisherConstants
                .NOT_AVAILABLE)) {
            return new String[]{spTenantDomain};
        }
        if (StringUtils.isBlank(spTenantDomain) || userTenantDomain.equalsIgnoreCase(AuditDataPublisherConstants
                .NOT_AVAILABLE)) {
            return new String[]{userTenantDomain};
        }
        if (spTenantDomain.equalsIgnoreCase(userTenantDomain)) {
            return new String[]{userTenantDomain};
        } else {
            return new String[]{userTenantDomain, spTenantDomain};
        }
    }

    /**
     * Get metadata array for different tenants with tenant domain
     *
     * @param tenantDomain
     * @return
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
}
