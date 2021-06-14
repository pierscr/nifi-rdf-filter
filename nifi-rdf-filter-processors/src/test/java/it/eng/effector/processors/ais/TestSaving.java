package it.eng.effector.processors.ais;

import it.eng.rslab.rdf4j.inference.MultipleRulesInference;
import it.eng.rslab.rdf4j.inference.MultipleRulesInferenceFactory;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.config.SailRegistry;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.*;
import java.util.Iterator;

import static org.eclipse.rdf4j.model.util.Values.iri;

public class TestSaving {
    public static void main(String[] args) throws IOException {
        File input=new File("/home/piero/Development/sparql/testrdffilter/input");
        InputStream inputStream       = new FileInputStream(input);
        RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
        rdfParser.getParserConfig().set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
        Model model = new LinkedHashModel();
        rdfParser.setRDFHandler(new StatementCollector(model));
        rdfParser.parse(inputStream);
        for (Iterator<Statement> iterator = model.iterator(); iterator.hasNext();) {
            Statement statement = iterator.next();
            if(statement.getPredicate() == iri("http://effector.com/next")) {
                iterator.remove();
            }
        }
        File output=new File("/home/piero/Development/sparql/testrdffilter/output");
        OutputStream outputStream  = new FileOutputStream(output);
        Rio.write(model, outputStream, RDFFormat.TURTLE);

    }
}
