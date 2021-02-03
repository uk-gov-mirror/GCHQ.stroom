/*
 * Copyright 2016 Crown Copyright
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

package stroom.event.logging.impl;

import stroom.activity.api.CurrentActivity;
import stroom.docref.DocRef;
import stroom.event.logging.api.ObjectInfoProvider;
import stroom.event.logging.api.ObjectType;
import stroom.event.logging.api.PurposeUtil;
import stroom.event.logging.api.StroomEventLoggingService;
import stroom.security.api.SecurityContext;
import stroom.util.io.ByteSize;
import stroom.util.shared.BuildInfo;
import stroom.util.shared.HasId;
import stroom.util.shared.HasIntegerId;
import stroom.util.shared.HasName;
import stroom.util.shared.HasUuid;
import stroom.util.time.StroomDuration;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import event.logging.BaseObject;
import event.logging.Data;
import event.logging.Device;
import event.logging.Event;
import event.logging.EventAction;
import event.logging.EventDetail;
import event.logging.EventSource;
import event.logging.EventTime;
import event.logging.OtherObject;
import event.logging.Purpose;
import event.logging.SystemDetail;
import event.logging.User;
import event.logging.impl.DefaultEventLoggingService;
import event.logging.util.DeviceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Singleton
public class StroomEventLoggingServiceImpl extends DefaultEventLoggingService implements StroomEventLoggingService {
    /**
     * Logger - should not be used for event logs
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(StroomEventLoggingServiceImpl.class);

    //todo consider making a config property
    private static int MAX_LIST_ELEMENT_COUNT = 5;

    private static final String SYSTEM = "Stroom";
    private static final String ENVIRONMENT = "";
    private static final String GENERATOR = "StroomEventLoggingService";

    private volatile boolean obtainedDevice;
    private volatile Device storedDevice;

    private final SecurityContext securityContext;
    private final Provider<HttpServletRequest> httpServletRequestProvider;
    private final CurrentActivity currentActivity;
    private final Provider<BuildInfo> buildInfoProvider;

    private final Map<ObjectType, Provider<ObjectInfoProvider>> objectInfoProviderMap;

    private final ObjectMapper objectMapper;

    @Inject
    StroomEventLoggingServiceImpl(final SecurityContext securityContext,
                                  final Provider<HttpServletRequest> httpServletRequestProvider,
                                  final Map<ObjectType, Provider<ObjectInfoProvider>> objectInfoProviderMap,
                                  final CurrentActivity currentActivity,
                                  final Provider<BuildInfo> buildInfoProvider) {
        this.securityContext = securityContext;
        this.httpServletRequestProvider = httpServletRequestProvider;
        this.objectInfoProviderMap = objectInfoProviderMap;
        this.currentActivity = currentActivity;
        this.buildInfoProvider = buildInfoProvider;
        this.objectMapper = createObjectMapper();
    }

    @Override
    public void log(final Event event) {
        try {
            super.log(event);
        } catch (Exception e) {
            // Swallow the exception so failure to log does not prevent the action being logged
            // from succeeding
            LOGGER.error("Error logging event", e);
        }
    }

    @Override
    public Event createEvent(final String typeId,
                             final String description,
                             final Purpose purpose,
                             final EventAction eventAction) {
        // Get the current request.
        final HttpServletRequest request = getRequest();

        return Event.builder()
                .withEventTime(EventTime.builder()
                        .withTimeCreated(new Date())
                        .build())
                .withEventSource(EventSource.builder()
                        .withSystem(SystemDetail.builder()
                                .withName(SYSTEM)
                                .withEnvironment(ENVIRONMENT)
                                .withVersion(buildInfoProvider.get().getBuildVersion())
                                .build())
                        .withGenerator(GENERATOR)
                        .withDevice(getClient(request))
                        .withClient(getClient(request))
                        .withUser(getUser())
                        .build())
                .withEventDetail(EventDetail.builder()
                        .withTypeId(typeId)
                        .withDescription(description)
                        .withPurpose(mergePurposes(PurposeUtil.create(currentActivity.getActivity()), purpose))
                        .withEventAction(eventAction)
                        .build())
                .build();
    }

    /**
     * Shallow merge of the two Purpose objects
     */
    private Purpose mergePurposes(final Purpose base, final Purpose override) {
        if (base == null || override == null) {
            if (base == null && override == null) {
                return null;
            }

            return Objects.requireNonNullElse(override, base);
        } else {
            final Purpose purpose = base.newCopyBuilder().build();
            mergeValue(override::getAuthorisations, purpose::setAuthorisations);
            mergeValue(override::getClassification, purpose::setClassification);
            mergeValue(override::getJustification, purpose::setJustification);
            mergeValue(override::getStakeholders, purpose::setStakeholders);
            mergeValue(override::getExpectedOutcome, purpose::setExpectedOutcome);
            mergeValue(override::getSubject, purpose::setSubject);

            // Combine the list of Data items from each
            purpose.getData().clear();
            purpose.getData().addAll(base.getData());
            purpose.getData().addAll(override.getData());
            return purpose;
        }
    }

    private <T> void mergeValue(final Supplier<T> getter, final Consumer<T> setter) {
        T value = getter.get();
        if (value != null) {
            setter.accept(value);
        }
    }

    private Device getClient(final HttpServletRequest request) {
        if (request != null) {
            try {
                String ip = request.getRemoteAddr();
                ip = DeviceUtil.getValidIP(ip);

                if (ip != null) {
                    InetAddress inetAddress = null;
                    try {
                        inetAddress = InetAddress.getByName(ip);
                    } catch (final UnknownHostException e) {
                        LOGGER.warn("Problem getting client InetAddress", e);
                    }

                    final Device client;
                    if (inetAddress != null) {
                        client = DeviceUtil.createDeviceFromInetAddress(inetAddress);
                    } else {
                        client = new Device();
                    }

                    client.setIPAddress(ip);
                    return client;
                }
            } catch (final RuntimeException e) {
                LOGGER.warn("Problem getting client IP address and host name", e);
            }
        }

        return null;
    }

    private User getUser() {
        try {
            final String userId;
            if (securityContext.isProcessingUser()) {
                // We are running as proc user so try and get the OS user,
                // though that may just be a shared account.
                // This is useful where a CLI command is being used
                final String osUser = System.getProperty("user.name");

                userId = osUser != null
                        ? osUser
                        : securityContext.getUserId();
            } else {
                userId = securityContext.getUserId();
            }

            if (userId != null) {
                return User.builder()
                        .withId(userId)
                        .build();
            }
        } catch (final RuntimeException e) {
            LOGGER.warn("Problem getting current user", e);
        }

        return null;
    }

    private synchronized Device obtainStoredDevice(final HttpServletRequest request) {
        if (!obtainedDevice) {
            // First try and get the local server IP address and host name.
            InetAddress inetAddress = null;
            try {
                inetAddress = InetAddress.getLocalHost();
            } catch (final UnknownHostException e) {
                LOGGER.warn("Problem getting device from InetAddress", e);
            }

            if (inetAddress != null) {
                storedDevice = DeviceUtil.createDeviceFromInetAddress(inetAddress);
            } else {
                // Make final attempt to set with request if we have one and
                // haven't been able to set IP and host name already.
                if (request != null) {
                    final String ip = DeviceUtil.getValidIP(request.getLocalAddr());
                    if (ip != null) {
                        try {
                            inetAddress = InetAddress.getByName(ip);
                        } catch (final UnknownHostException e) {
                            LOGGER.warn("Problem getting client InetAddress", e);
                        }

                        if (inetAddress != null) {
                            storedDevice = DeviceUtil.createDeviceFromInetAddress(inetAddress);
                        } else {
                            storedDevice = new Device();
                        }

                        storedDevice.setIPAddress(ip);
                    }
                }
            }
            obtainedDevice = true;
        }

        return storedDevice;
    }

    private Device copyDevice(final Device source, final Device dest) {
        dest.setIPAddress(source.getIPAddress());
        dest.setHostName(source.getHostName());
        dest.setMACAddress(source.getMACAddress());
        return dest;
    }

    private HttpServletRequest getRequest() {
        if (httpServletRequestProvider != null) {
            return httpServletRequestProvider.get();
        }
        return null;
    }

    @Override
    public BaseObject convert(final Supplier<?> objectSupplier) {
        if (objectSupplier != null) {
            // Run as proc user in case we are logging a user trying to access a thing they
            // don't have perms for
            final Object object = securityContext.asProcessingUserResult(objectSupplier);
            return convert(object);
        } else {
            return null;
        }
    }

    @Override
    public BaseObject convert(final Object object) {
        final BaseObject baseObj;
        final ObjectInfoProvider objectInfoAppender = getInfoAppender(object.getClass());
        if (objectInfoAppender != null){
            baseObj = objectInfoAppender.createBaseObject(object);
        } else {
            final OtherObject.Builder<Void> builder = OtherObject.builder()
                    .withType(getObjectType(object))
                    .withId(getObjectId(object))
                    .withName(getObjectName(object))
                    .withDescription(describe(object));

            builder.addData(getDataItems(object));

            baseObj = builder.build();
        }

        return baseObj;
    }

    private String getObjectType(final java.lang.Object object) {
        if (object instanceof DocRef) {
            return String.valueOf(((DocRef) object).getType());
        }

        final ObjectInfoProvider objectInfoProvider = getInfoAppender(object.getClass());
        if (objectInfoProvider == null) {
            if (object instanceof Collection) {
                Collection<?> collection = (Collection<?>) object;
                if (collection.isEmpty()) {
                    return "Empty collection";
                } else {
                    return "Collection containing " + (long) collection.size() + " "
                            + collection.stream().findFirst().get().getClass().getSimpleName() +
                            " and possibly other objects";
                }
            }
            return object.getClass().getSimpleName();
        }
        return objectInfoProvider.getObjectType(object);
    }

    private ObjectInfoProvider getInfoAppender(final Class<?> type) {
        if (type == null){
            return null;
        }
        ObjectInfoProvider appender = null;

        if (String.class.equals(type)) {
            appender = new ObjectInfoProvider() {
                @Override
                public BaseObject createBaseObject(final Object object) {
                    return OtherObject.builder()
                            .withType(object.toString())
                            .build();
                }

                @Override
                public String getObjectType(final Object object) {
                    return object.toString();
                }
            };
        } else {
            // Some providers exist for superclasses and not subclass types so keep looking through the
            // class hierarchy to find a provider.
            Class<?> currentType = type;
            Provider<ObjectInfoProvider> provider = null;
            while (currentType != null && provider == null) {
                provider = objectInfoProviderMap.get(new ObjectType(currentType));
                currentType = currentType.getSuperclass();
            }

            if (provider != null) {
                appender = provider.get();
            }
        }

        if (appender == null) {
            LOGGER.debug("No ObjectInfoProvider found for " + type.getName());
        }

        return appender;
    }

    @Override
    public String describe(final Object object) {
        if (object == null){
            return null;
        }
        final StringBuilder desc = new StringBuilder();
        final String objectType = getObjectType(object);
        if (objectType != null) {
            desc.append(objectType);
        }

        final String objectName = getObjectName(object);
        if (objectName != null) {
            desc.append(" \"");
            desc.append(objectName);
            desc.append("\"");
        }

        final String objectId = getObjectId(object);
        if (objectId != null) {
            desc.append(" id=");
            desc.append(objectId);
        }

        return desc.toString();
    }


    private String getObjectName(final java.lang.Object object) {
        if (object instanceof DocRef) {
            return ((DocRef) object).getName();
        } else if (object instanceof HasName) {
            return ((HasName) object).getName();
        }

        return null;
    }

    private String getObjectId(final java.lang.Object object) {
        if (object instanceof HasUuid) {
            return ((HasUuid) object).getUuid();
        }

        if (object instanceof HasId) {
            return String.valueOf(((HasId) object).getId());
        }

        if (object instanceof HasIntegerId) {
            return String.valueOf(((HasIntegerId) object).getId());
        }

        if (object instanceof DocRef) {
            return String.valueOf(((DocRef) object).getUuid());
        }

        return null;
    }

    /**
     * Create {@link Data} items from properties of the supplied POJO
     * @param obj POJO from which to extract properties
     * @return List of {@link Data} items representing properties of the supplied POJO
     */
    public List<Data> getDataItems(java.lang.Object obj) {
        if (obj == null){
            return null;
        }
        // Construct a Jackson JavaType for the class
        final JavaType javaType = objectMapper.getTypeFactory().constructType(obj.getClass());

        // Introspect the given type
        final BeanDescription beanDescription = objectMapper.getSerializationConfig().introspect(javaType);

        // Find properties
        final List<BeanPropertyDefinition> properties = beanDescription.findProperties();

        // Get class level ignored properties
        final Set<String> ignoredProperties = objectMapper.getSerializationConfig().getAnnotationIntrospector()
                .findPropertyIgnorals(beanDescription.getClassInfo()).getIgnored();// Filter properties removing the class level ignored ones

        final List<BeanPropertyDefinition> availableProperties = properties.stream()
                .filter(property -> !ignoredProperties.contains(property.getName()))
                .collect(Collectors.toList());

        return availableProperties.stream().map(
                beanPropDef -> {
                    final Data.Builder<?> builder = Data.builder().withName(beanPropDef.getName());
                    final Object valObj = extractPropVal(beanPropDef, obj);
                    if (valObj != null) {
                        if (valObj instanceof Collection<?>){
                            Collection<?> collection = (Collection<?>) valObj;
                            final String collectionValue = collection.stream().limit(MAX_LIST_ELEMENT_COUNT).
                                    map(Objects::toString).collect(Collectors.joining(", "));
                            if (collection.size() > MAX_LIST_ELEMENT_COUNT){
                                builder.withValue(collectionValue + "...(" + collection.size() + " elements in total).");
                            } else {
                                builder.withValue(collectionValue);
                            }
                        } else if (isLeafPropertyType(valObj.getClass())) {
                            final String value;
                            if (shouldRedact(beanPropDef.getName().toLowerCase(), valObj.getClass())) {
                                value = "********";
                            } else {
                                value = valObj.toString();
                            }
                            builder.withValue(value);
                        } else {
                            getDataItems(valObj).stream().forEach(d -> builder.addData(d));
                        }
                    }
                    return builder.build();
                }).collect(Collectors.toList());
    }

    private static boolean isLeafPropertyType(final Class<?> type) {

        boolean isLeaf = type.equals(String.class) ||
                type.equals(Byte.class) ||
                type.equals(byte.class) ||
                type.equals(Integer.class) ||
                type.equals(int.class) ||
                type.equals(Long.class) ||
                type.equals(long.class) ||
                type.equals(Short.class) ||
                type.equals(short.class) ||
                type.equals(Float.class) ||
                type.equals(float.class) ||
                type.equals(Double.class) ||
                type.equals(double.class) ||
                type.equals(Boolean.class) ||
                type.equals(boolean.class) ||
                type.equals(Character.class) ||
                type.equals(char.class) ||

                DocRef.class.isAssignableFrom(type) ||
                Enum.class.isAssignableFrom(type) ||
                Path.class.isAssignableFrom(type) ||
                StroomDuration.class.isAssignableFrom(type) ||
                ByteSize.class.isAssignableFrom(type) ||
                Date.class.isAssignableFrom(type) ||
                Instant.class.isAssignableFrom(type) ||
                (type.isArray() &&
                            (type.getComponentType().equals(Byte.class) ||
                             type.getComponentType().equals(byte.class) ||
                             type.getComponentType().equals(Character.class) ||
                             type.getComponentType().equals(char.class)));

        LOGGER.trace("isLeafPropertyType({}), returning: {}", type, isLeaf);
        return isLeaf;
    }

    private static Object extractPropVal (final BeanPropertyDefinition beanPropDef, final Object obj){
        final AnnotatedMethod method = beanPropDef.getGetter();

        if (method != null) {
            try {
                return method.callOn(obj);
            } catch (Exception e) {
                LOGGER.debug("Error calling getter of " + beanPropDef.getName() + " on class " + obj.getClass().getSimpleName(), e);
            }
        } else {
            LOGGER.debug("No getter for property " + beanPropDef.getName() + " of class " + obj.getClass().getSimpleName());
        }

        return null;
    }

    //It is possible for a resource to be annotated to prevent it being logged at all, even when the resource
    //itself is logged, e.g. due to configuration settings
    //Assess whether this field should be redacted
    public boolean shouldRedact(String propNameLowercase, Class<?> type) {
        if (Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type)){
            return false; //Don't redact boolean types
        }

        //TODO consider replacing or augmenting this hard coding
        // with a mechanism to allow properties to be selected for redaction, e.g. using annotations
        return propNameLowercase.endsWith("password") ||
                propNameLowercase.endsWith("secret") ||
                propNameLowercase.endsWith("token") ||
                propNameLowercase.endsWith("nonce") ||
                propNameLowercase.endsWith("key");
    }


    private static ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        mapper.setSerializationInclusion(Include.NON_NULL);

        return mapper;
    }

}
