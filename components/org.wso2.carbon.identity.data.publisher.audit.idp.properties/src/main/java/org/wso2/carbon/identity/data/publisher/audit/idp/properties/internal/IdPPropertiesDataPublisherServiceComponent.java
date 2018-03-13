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

package org.wso2.carbon.identity.data.publisher.audit.idp.properties.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.event.stream.core.EventStreamService;
import org.wso2.carbon.identity.data.publisher.audit.idp.properties.impl.ResidentIdPPropertiesDataPublisher;
import org.wso2.carbon.idp.mgt.listener.IdentityProviderMgtListener;

@Component(
        name = "org.wso2.carbon.identity.data.publisher.audit.idp.properties",
        immediate = true
)
public class IdPPropertiesDataPublisherServiceComponent {
    private static Log log = LogFactory.getLog(IdPPropertiesDataPublisherServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {
        try {
            ResidentIdPPropertiesDataPublisher publisher = new ResidentIdPPropertiesDataPublisher();
            context.getBundleContext().registerService(IdentityProviderMgtListener.class.getName(), publisher, null);
        } catch (Throwable e) {
            log.fatal("Error while activating the ResidentIdPPropertiesDataPublisher. " +
                    "IdP properties audit events will not be published to IS Analytics", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.debug("ResidentIdPPropertiesDataPublisher is deactivated");
        }
    }

    @Reference(
            name = "eventStreamManager.service",
            service = org.wso2.carbon.event.stream.core.EventStreamService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetEventStreamService"
    )
    protected void setEventStreamService(EventStreamService publisherService) {
        IdPPropertiesDataPublisherDataHolder.getInstance().setPublisherService(publisherService);
    }

    protected void unsetEventStreamService(EventStreamService publisherService) {
        IdPPropertiesDataPublisherDataHolder.getInstance().setPublisherService(null);
    }
}
