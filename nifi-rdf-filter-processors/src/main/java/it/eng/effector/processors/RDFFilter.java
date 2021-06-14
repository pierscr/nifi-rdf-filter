/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.eng.effector.processors;

import it.eng.rslab.rdf4j.inference.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.StreamCallback;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.rio.*;
import static org.eclipse.rdf4j.model.util.Values.iri;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.eclipse.rdf4j.rio.helpers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tags({"SPARQL"})
@CapabilityDescription("Provide a description")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class RDFFilter extends AbstractProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RDFFilter.class);


/*    public static final PropertyDescriptor SPARQL_RULES_FOLDER = new PropertyDescriptor
            .Builder().name("SPARQL_RULES_FOLDER")
            .displayName("SPARQL rules folder path")
            .description("Folder containing SPARQL rules")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();*/


    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("Sucessfully mapped attribute names")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("Failed to map attribute names")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
        //descriptors.add(SPARQL_RULES_FOLDER);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<Relationship>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {

    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }
        try{
            flowFile = session.write(flowFile,new StreamCallback() {
                @Override
                public void process(InputStream inputStream, OutputStream outputStream) throws IOException {
                    logger.debug("filtering model");
                    RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
                    rdfParser.getParserConfig().set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
                    Model model = new LinkedHashModel();
                    rdfParser.setRDFHandler(new StatementCollector(model));
                    rdfParser.parse(inputStream);
                    logger.debug("writing filtered model");
                    model.removeIf(i -> i.getPredicate().equals(iri("http://effector.com/next")) );
                    Rio.write(model, outputStream, RDFFormat.TURTLE);
             }
            });
            session.transfer(flowFile, REL_SUCCESS);
        }catch (Exception e){
            flowFile = session.write(flowFile, new StreamCallback() {
                @Override
                public void process(InputStream inputStream, OutputStream outputStream) throws IOException {
                    IOUtils.write( "{'error':'"+e.getMessage()+"','stack':'"+ ExceptionUtils.getStackTrace(e)+"'}", outputStream,  StandardCharsets.UTF_8);
                }
            });
            logger.error(ExceptionUtils.getStackTrace(e));
            session.transfer(flowFile, REL_FAILURE);
        }
    }
}
