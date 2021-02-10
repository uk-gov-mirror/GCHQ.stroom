/*
 * Copyright 2020 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package stroom.event.logging.rs.impl;

import stroom.dropwizard.common.DelegatingExceptionMapper;
import stroom.event.logging.impl.LoggingConfig;
import stroom.event.logging.rs.api.RestResourceAutoLogger;
import stroom.security.api.SecurityContext;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.glassfish.jersey.message.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;

public class RestResourceAutoLoggerImpl implements RestResourceAutoLogger {
    static final Logger LOGGER = LoggerFactory.getLogger(RestResourceAutoLoggerImpl.class);

    private static final String REQUEST_LOG_INFO_PROPERTY = "stroom.rs.logging.request";

    private final RequestEventLog requestEventLog;
    private final ObjectMapper objectMapper;
    private final LoggingConfig config;
    private final SecurityContext securityContext;

    private final DelegatingExceptionMapper delegatingExceptionMapper;

    @Context
    private HttpServletRequest request;

    @Context
    private ResourceInfo resourceInfo;

    @Context
    private ResourceContext resourceContext;


    @Inject
    RestResourceAutoLoggerImpl(final SecurityContext securityContext, final RequestEventLog requestEventLog,
                               final LoggingConfig config, final DelegatingExceptionMapper delegatingExceptionMapper) {
        this.securityContext = securityContext;
        this.requestEventLog = requestEventLog;
        this.config = config;
        this.objectMapper = createObjectMapper();
        this.delegatingExceptionMapper = delegatingExceptionMapper;
    }

    //For unit test use
    RestResourceAutoLoggerImpl(final SecurityContext securityContext, final RequestEventLog requestEventLog,
                               final LoggingConfig config,
                               final ResourceInfo resourceInfo,
                               final HttpServletRequest request,
                               final DelegatingExceptionMapper delegatingExceptionMapper) {
        this.securityContext = securityContext;
        this.requestEventLog = requestEventLog;
        this.config = config;
        this.resourceInfo = resourceInfo;
        this.request = request;
        this.objectMapper = createObjectMapper();
        this.delegatingExceptionMapper = delegatingExceptionMapper;
    }

    @Override
    public Response toResponse(final Throwable exception) {
        if (request != null) {
            final Object object = request.getAttribute(REQUEST_LOG_INFO_PROPERTY);
            if (object != null) {
                RequestInfo requestInfo = (RequestInfo) object;
                requestEventLog.log(requestInfo, null, exception);
            }
        } else {
            LOGGER.warn("Unable to create audit log for exception, request is null", exception);
        }

        if (exception instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) exception;
            return wae.getResponse();
        } else {
            return delegatingExceptionMapper.toResponse(exception);
        }
    }

    @Override
    public void aroundWriteTo(final WriterInterceptorContext writerInterceptorContext)
            throws IOException, WebApplicationException {
        writerInterceptorContext.proceed();

        final Object object = request.getAttribute(REQUEST_LOG_INFO_PROPERTY);

        if (object != null) {
            RequestInfo requestInfo = (RequestInfo) object;
            requestEventLog.log (requestInfo, writerInterceptorContext.getEntity());
        }
    }

    @Override
    public void filter(final ContainerRequestContext context) throws IOException {
        ContainerResourceInfo containerResourceInfo = new ContainerResourceInfo(resourceContext, resourceInfo, context);

        if (containerResourceInfo.shouldLog(config)){
            if (context.hasEntity()) {
                final RequestEntityCapturingInputStream stream = new RequestEntityCapturingInputStream(resourceInfo, context.getEntityStream(),
                        objectMapper, MessageUtils.getCharset(context.getMediaType()));
                context.setEntityStream(stream);

                request.setAttribute(REQUEST_LOG_INFO_PROPERTY, new RequestInfo(securityContext, containerResourceInfo, stream.getRequestEntity()));
            } else {
                request.setAttribute(REQUEST_LOG_INFO_PROPERTY, new RequestInfo(securityContext, containerResourceInfo));
            }
        }
    }

    private static ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        mapper.setSerializationInclusion(Include.NON_NULL);

        return mapper;
    }

    //Needed for some unit tests
    void setResourceContext(final ResourceContext resourceContext){
        this.resourceContext = resourceContext;
    }

}