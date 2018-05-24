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

package stroom.pipeline.filter;

import com.sun.xml.fastinfoset.sax.SAXDocumentSerializer;
import io.vavr.Tuple3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import stroom.entity.shared.DocRefUtil;
import stroom.entity.shared.Range;
import stroom.pipeline.errorhandler.ErrorReceiverProxy;
import stroom.pipeline.factory.ConfigurableElement;
import stroom.pipeline.factory.PipelineProperty;
import stroom.pipeline.shared.ElementIcons;
import stroom.pipeline.shared.PipelineEntity;
import stroom.pipeline.shared.data.PipelineElementType;
import stroom.pipeline.shared.data.PipelineElementType.Category;
import stroom.pipeline.state.PipelineHolder;
import stroom.pipeline.state.StreamHolder;
import stroom.query.api.v2.DocRef;
import stroom.refdata.MapStoreHolder;
import stroom.refdata.RefDataLoaderHolder;
import stroom.refdata.offheapstore.FastInfosetValue;
import stroom.refdata.offheapstore.MapDefinition;
import stroom.refdata.offheapstore.RefDataValue;
import stroom.refdata.offheapstore.RefStreamDefinition;
import stroom.refdata.offheapstore.StringValue;
import stroom.refdata.saxevents.OffHeapEventListInternPool;
import stroom.util.CharBuffer;
import stroom.util.shared.Severity;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This XML filter captures XML content that defines key, value maps to be
 * stored as reference data. The key, value map content is likely to have been
 * produced as the result of an XSL transformation of some reference data.
 */
@ConfigurableElement(
        type = "ReferenceDataFilter",
        category = Category.FILTER,
        roles = {
                PipelineElementType.ROLE_TARGET,
                PipelineElementType.ROLE_HAS_TARGETS},
        icon = ElementIcons.REFERENCE_DATA)
public class ReferenceDataFilter extends AbstractXMLFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataFilter.class);

    /*
        Example xml data
        <referenceData>
            <reference>
                <map>cityToCountry</map>
                <key>cardiff</key>
                <value>Wales</value>
            </reference>
            <reference>
                <map>countryToCity</map>
                <key>wales</key>
                <value>cardiff</value>
            </reference>
            <reference>
                <map>employeeIdToCountry</map>
                <from>1001</from>
                <to>1700</to>
                <value>UK</value>
            </reference>
            <reference>
                <map>employeeIdToCountry</map>
                <key>1701</key>
                <value>USA</value>
            </reference>
            ...
        </referenceData>

        Note: <Value> can contain either XML or plain string data, e.g.
            <value><country>UK></country></value>
            <value>UK</value>
     */
    private static final String REFERENCE_ELEMENT = "reference";
    private static final String MAP_ELEMENT = "map";
    private static final String KEY_ELEMENT = "key";
    private static final String FROM_ELEMENT = "from";
    private static final String TO_ELEMENT = "to";
    private static final String VALUE_ELEMENT = "value";

    private final MapStoreHolder mapStoreHolder;
    private final StreamHolder streamHolder;
    private final PipelineHolder pipelineHolder;
    private final EventListInternPool internPool;
    private final OffHeapEventListInternPool offHeapEventListInternPool;
    private final ErrorReceiverProxy errorReceiverProxy;
    private final RefDataLoaderHolder refDataLoaderHolder;

    private final SAXDocumentSerializer saxDocumentSerializer = new SAXDocumentSerializer();
    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private final CharBuffer contentBuffer = new CharBuffer(20);
    private final List<Tuple3> keyValueEntries = new ArrayList<>();
    private final List<Tuple3> rangeValueEntries = new ArrayList<>();

    private RefStreamDefinition refStreamDefinition = null;

    private String mapName;
    private String key;
    private boolean inValue;
    private boolean haveSeenXmlInValueElement = false;
    private Long rangeFrom;
    private Long rangeTo;
    private boolean warnOnDuplicateKeys = false;
    private boolean overrideExistingValues = true;

    private enum ValueElementType {
        XML, STRING
    }

    private ValueElementType valueElementType = null;

    @Inject
    public ReferenceDataFilter(final MapStoreHolder mapStoreHolder,
                               final StreamHolder streamHolder,
                               final PipelineHolder pipelineHolder,
                               final EventListInternPool internPool,
                               final ErrorReceiverProxy errorReceiverProxy,
                               final OffHeapEventListInternPool offHeapEventListInternPool,
                               final RefDataLoaderHolder refDataLoaderHolder) {
        this.mapStoreHolder = mapStoreHolder;
        this.streamHolder = streamHolder;
        this.pipelineHolder = pipelineHolder;
        this.internPool = internPool;
        this.errorReceiverProxy = errorReceiverProxy;
        this.offHeapEventListInternPool = offHeapEventListInternPool;
        this.refDataLoaderHolder =refDataLoaderHolder;
    }


    @Override
    public void startStream() {
        super.startStream();
        // build the definition of the stream that is being processed
        final PipelineEntity pipelineEntity = Objects.requireNonNull(pipelineHolder.getPipeline());
        final DocRef pipelineDocRef = DocRefUtil.create(pipelineEntity);
        this.refStreamDefinition = new RefStreamDefinition(
                pipelineDocRef,
                pipelineEntity.getVersion(),
                streamHolder.getStream().getId(), streamNo);
        contentBuffer.clear();
        byteArrayOutputStream.reset();
        saxDocumentSerializer.reset();

        if (refDataLoaderHolder.getRefDataLoader() == null) {
            errorReceiverProxy.log(Severity.FATAL_ERROR, null, getElementId(), "RefDataLoader is missing", null);
        }
        refDataLoaderHolder.getRefDataLoader().initialise();
    }

    /**
     * This method looks for a post processing function. If it finds one it does
     * not output the element. Instead it stores data about the function and
     * sets a flag so that the function can be performed when the corresponding
     * end element is reached.
     *
     * @param uri       The element's Namespace URI, or the empty string.
     * @param localName The element's local name, or the empty string.
     * @param qName     The element's qualified (prefixed) name, or the empty string.
     * @param atts      The element's attributes.
     * @throws org.xml.sax.SAXException The client may throw an exception during processing.
     * @see stroom.pipeline.filter.AbstractXMLFilter#startElement(java.lang.String,
     * java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes atts)
            throws SAXException {
        contentBuffer.clear();

        LOGGER.trace("startElement {} {} {}", uri, localName, qName);

        if (VALUE_ELEMENT.equalsIgnoreCase(localName)) {
            inValue = true;
        } else if (inValue) {
            if (!haveSeenXmlInValueElement) {
                LOGGER.trace("first XML element inside {} element", VALUE_ELEMENT);
                // This is the first startElement inside the value element so we are dealing with XML refdata
                haveSeenXmlInValueElement = true;
                saxDocumentSerializer.reset();
            }

            saxDocumentSerializer.startElement(uri, localName, qName, atts);
        }

        super.startElement(uri, localName, qName, atts);
    }

    /**
     * This method applies a post processing function if we are currently within
     * a function element. At this stage we should have details of the function
     * to apply from the corresponding start element and content to apply it to
     * from the characters event.
     *
     * @param uri       The element's Namespace URI, or the empty string.
     * @param localName The element's local name, or the empty string.
     * @param qName     The element's qualified (prefixed) name, or the empty string.
     * @throws org.xml.sax.SAXException The client may throw an exception during processing.
     * @see stroom.pipeline.filter.AbstractXMLFilter#endElement(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        LOGGER.trace("endElement {} {} {}", uri, localName, qName);
        if (VALUE_ELEMENT.equalsIgnoreCase(localName)) {
            LOGGER.trace("Leaving {} element", VALUE_ELEMENT);
            inValue = false;
        }

        if (inValue) {
            saxDocumentSerializer.endElement(uri, localName, qName);
        } else {
            if (MAP_ELEMENT.equalsIgnoreCase(localName)) {
                // capture the name of the map that the subsequent values will belong to. A ref
                // stream can contain data for multiple maps
                mapName = contentBuffer.toString();

            } else if (KEY_ELEMENT.equalsIgnoreCase(localName)) {
                // the key for the KV pair
                key = contentBuffer.toString();

            } else if (FROM_ELEMENT.equalsIgnoreCase(localName)) {
                // the start key for the key range
                final String string = contentBuffer.toString();
                try {
                    rangeFrom = Long.parseLong(string);
                } catch (final RuntimeException e) {
                    errorReceiverProxy.log(Severity.ERROR, null, getElementId(),
                            "Unable to parse string \"" + string + "\" as long for range from", e);
                }

            } else if (TO_ELEMENT.equalsIgnoreCase(localName)) {
                // the end key for the key range
                final String string = contentBuffer.toString();
                try {
                    rangeTo = Long.parseLong(string);
                } catch (final RuntimeException e) {
                    errorReceiverProxy.log(Severity.ERROR, null, getElementId(),
                            "Unable to parse string \"" + string + "\" as long for range to", e);
                }

            } else if (REFERENCE_ELEMENT.equalsIgnoreCase(localName)) {

                // end of the ref data item so ensure it is persisted in the store
                try {
                    if (mapName != null) {
                        final MapDefinition mapDefinition = new MapDefinition(refStreamDefinition, mapName);

                        RefDataValue refDataValue = getRefDataValueFromBuffers();
                        if (key != null) {
                            // TODO we may be able to pass a Consumer<ByteBuffer> and then use a ByteBufferOutputStream
                            // to write directly to a direct byteBuffer, however we will not know the size up front
                            LOGGER.trace("Putting key {} into map {}", key, mapDefinition);
                            refDataLoaderHolder.getRefDataLoader()
                                    .put(key, refDataValue, overrideExistingValues);
                        } else if (rangeFrom != null && rangeTo != null) {
                            if (rangeFrom > rangeTo) {
                                errorReceiverProxy
                                        .log(Severity.ERROR, null, getElementId(),
                                                "Range from '" + rangeFrom
                                                        + "' must be less than or equal to range to '" + rangeTo + "'",
                                                null);
                            } else {
                                // convert from inclusive rangeTo to exclusive rangeTo
                                // if from==to we still record it as a range
                                final Range<Long> range = new Range<>(rangeFrom, rangeTo + 1);
                                LOGGER.trace("Putting range {} into map {}", range, mapDefinition);
                                refDataLoaderHolder.getRefDataLoader()
                                        .put(range, refDataValue, overrideExistingValues);
                            }
                        }
                    }
                } catch (final RuntimeException e) {
                    if (warnOnDuplicateKeys) {
                        errorReceiverProxy.log(Severity.WARNING, null, getElementId(), e.getMessage(), e);
                    }
                }

                // Set keys to null.
                mapName = null;
                key = null;
                rangeFrom = null;
                rangeTo = null;
                haveSeenXmlInValueElement = false;

                // reset our buffers ready for the next ref data item
                contentBuffer.clear();
                byteArrayOutputStream.reset();
                saxDocumentSerializer.reset();
            }
        }

        contentBuffer.clear();

        super.endElement(uri, localName, qName);
    }

    private RefDataValue getRefDataValueFromBuffers() {
        final RefDataValue refDataValue;
        if (haveSeenXmlInValueElement) {
            LOGGER.trace("Serializing fast infoset events");
            //serialize the event list using fastInfoset
            byte[] fastInfosetBytes = byteArrayOutputStream.toByteArray();
            refDataValue = FastInfosetValue.of(fastInfosetBytes);
        } else {
            LOGGER.trace("Getting string data");
            //serialize the event list using fastInfoset
            // simple string value so use content buffer
            refDataValue = StringValue.of(contentBuffer.toString());
        }
        return refDataValue;
    }

    /**
     * If we are within a function element then this method should buffer the
     * character content so that it can be operated on in the function end
     * element.
     *
     * @param ch     An array of characters.
     * @param start  The starting position in the array.
     * @param length The number of characters to use from the array.
     * @throws org.xml.sax.SAXException The client may throw an exception during processing.
     * @see stroom.pipeline.filter.AbstractXMLFilter#characters(char[],
     * int, int)
     */
    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        if (inValue && haveSeenXmlInValueElement) {
            saxDocumentSerializer.characters(ch, start, length);
        } else {
            // outside the value element so capture the chars so we can get keys, map names, etc.
            contentBuffer.append(ch, start, length);
        }

        super.characters(ch, start, length);
    }

    @Override
    public void endProcessing() {
        try {
            byteArrayOutputStream.close();
        } catch (IOException e) {
            errorReceiverProxy
                    .log(Severity.ERROR, null, null, "Error closing byteArrayOutputStream", e);
        }
        super.endProcessing();
    }

    @PipelineProperty(description = "Warn if there are duplicate keys found in the reference data?", defaultValue = "false")
    public void setWarnOnDuplicateKeys(final boolean warnOnDuplicateKeys) {
        this.warnOnDuplicateKeys = warnOnDuplicateKeys;
    }

    @PipelineProperty(description = "Allow duplicate keys to override existing values?", defaultValue = "true")
    public void setOverrideExistingValues(final boolean overrideExistingValues) {
        this.overrideExistingValues = overrideExistingValues;
    }
}
