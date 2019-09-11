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

package stroom.search.extraction;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.ReceivingContentHandler;
import net.sf.saxon.s9api.*;
import net.sf.saxon.tree.tiny.TinyBuilder;
import net.sf.saxon.tree.tiny.TinyTree;
import org.w3c.dom.*;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import stroom.dashboard.expression.v1.Val;
import stroom.dashboard.expression.v1.ValString;
import stroom.pipeline.LocationFactoryProxy;
import stroom.pipeline.errorhandler.ErrorReceiverProxy;
import stroom.pipeline.errorhandler.LoggedException;
import stroom.pipeline.factory.ConfigurableElement;
import stroom.pipeline.shared.ElementIcons;
import stroom.pipeline.shared.data.PipelineElementType;
import stroom.pipeline.shared.data.PipelineElementType.Category;
import stroom.security.api.SecurityContext;
import stroom.util.shared.Severity;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;


@ConfigurableElement(type = "XPathExtractionOutputFilter", category = Category.FILTER, roles = {
        PipelineElementType.ROLE_TARGET, PipelineElementType.ROLE_HAS_TARGETS}, icon = ElementIcons.SEARCH)
public class XPathExtractionOutputFilter extends SearchResultOutputFilter {
    private static final String EVENT = "Event";

    private static final String RECORD = "record";
    private static final String DATA = "data";
    private static final String NAME = "name";
    private static final String VALUE = "value";
    private final ErrorReceiverProxy errorReceiverProxy;
    private final SecurityContext securityContext;
    private final LocationFactoryProxy locationFactory;

    private DOMImplementation domImplementation = null;
    private Locator locator;

    private final Configuration config = new Configuration();
    private final PipelineConfiguration pipeConfig = config.makePipelineConfiguration();
    private TinyBuilder builder = null;

    private ReceivingContentHandler rch = null;
    private String topLevelElement = "";


//    //Track of current node (the selected "top level" element), separately to lower level elenent
//    private Document currentDoc = null;
//
//    //Also track element under node, in order that errors may be recovered for next node
//    private Node currentNode = null;

    @Inject
    public XPathExtractionOutputFilter(final LocationFactoryProxy locationFactory,
                                       final SecurityContext securityContext,
                                       final ErrorReceiverProxy errorReceiverProxy) {
        this.locationFactory = locationFactory;
        this.errorReceiverProxy = errorReceiverProxy;
        this.securityContext = securityContext;


    }

    /**
     * Sets the locator to use when reporting errors.
     *
     * @param locator The locator to use.
     */
    @Override
    public void setDocumentLocator(final Locator locator) {
        this.locator = locator;
        super.setDocumentLocator(locator);
    }

   private static void setAttributes (Element element, final Attributes attributes){
        for (int a = 0; a < attributes.getLength(); a++){
            element.setAttribute(attributes.getLocalName(a), attributes.getValue(a));
        }
   }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes atts)
            throws SAXException {

        try {
            if (rch == null) {
                topLevelElement = localName;
                //Start new document
                builder = new TinyBuilder(pipeConfig);
                rch = new ReceivingContentHandler();
                rch.setPipelineConfiguration(pipeConfig);
                rch.setReceiver(builder);
               // rch.startPrefixMapping(localName, uri);
                rch.startDocument();

            } else {
                //Push new element
                rch.startElement(uri, localName, qName, atts);
            }

          } catch (SAXException domException) {
            //Scrap the current document
            rch = null;
            topLevelElement = null;
            log(Severity.ERROR, "XML error creating element " + localName, domException);
        }

        super.startElement(uri, localName, qName, atts);
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        if (topLevelElement != null && topLevelElement.equals(localName)){
            try {
                rch.endDocument();
                //Finish new document and extract XPaths
                // Get the tree.

                final TinyTree tree = builder.getTree();

                System.out.println ("**Debug**");
                System.out.println (builder.getCurrentRoot().getDisplayName());

                final Processor processor = new Processor(false);
                final XQueryCompiler compiler = processor.newXQueryCompiler();
                final XQueryExecutable executable = compiler.compile("Event/EventSource/User/Id");

                // Can be reused but Not a thread safe thing.
                final XQueryEvaluator evaluator = executable.load();

                evaluator.setContextItem(new XdmNode(tree.getRootNode()));
                final XdmSequenceIterator iterator = evaluator.iterator();
                while (iterator.hasNext()) {
                    String value = iterator.next().getStringValue();
                    System.out.println(value);
                }
            } catch (SaxonApiException ex){
                log(Severity.ERROR, "Unable to evaluate XPaths", ex);
            }
            finally {
                topLevelElement = "";
                rch = null;
            }
        } else {
            //Pop element
            rch.endElement(uri, localName, qName);
        }

        super.endElement(uri, localName, qName);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (rch != null)
            rch.characters(ch, start, length);
        else
            log(Severity.ERROR, "Unexpected text node " + new String(ch, start, length) +
                    " at position " + start, null);
        //Add characters to the current node
        super.characters(ch, start, length);
    }

    /**
     * Initialise
     */
    @Override
    public void startProcessing() {

    }

    private void log(final Severity severity, final String message, final Exception e) {
        errorReceiverProxy.log(severity, locationFactory.create(locator), getElementId(), message, e);
    }


    private String stringify(Document document) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            StringWriter stringWriter = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
            return stringWriter.toString();
        } catch (TransformerException ex) {
            log(Severity.ERROR, "Cannot create XML string from DOM", ex);
            return "Error: No XML Created";
        }

    }
}
