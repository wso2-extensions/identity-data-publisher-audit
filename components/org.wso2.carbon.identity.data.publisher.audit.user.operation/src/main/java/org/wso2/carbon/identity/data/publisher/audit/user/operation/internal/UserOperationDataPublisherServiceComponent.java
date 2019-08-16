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

package org.wso2.carbon.identity.data.publisher.audit.user.operation.internal;

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
import org.wso2.carbon.identity.data.publisher.audit.user.operation.impl.UserOperationDataPublisher;
import org.wso2.carbon.identity.event.handler.AbstractEventHandler;

@Component(
        name = "org.wso2.carbon.identity.data.publisher.audit.user.operation",
        immediate = true
)
public class UserOperationDataPublisherServiceComponent {
    private static final Log log = LogFactory.getLog(UserOperationDataPublisherServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {
        try {
            UserOperationDataPublisher handler = new UserOperationDataPublisher();
            context.getBundleContext().registerService(AbstractEventHandler.class.getName(), handler, null);
        } catch (Throwable e) {
            log.fatal("Error while activating the UserOperationDataPublisher. " +
                    "User operation audit events will not be published to IS Analytics", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.debug("UserOperationDataPublisher is deactivated");
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
        UserOperationDataPublisherDataHolder.getInstance().setPublisherService(publisherService);
    }

    protected void unsetEventStreamService(EventStreamService publisherService) {
        UserOperationDataPublisherDataHolder.getInstance().setPublisherService(null);
    }
}
