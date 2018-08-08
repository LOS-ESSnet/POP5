package eu.europa.ec.eurostat.los.utils;

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory ;
import org.apache.jena.rdf.model.Resource ;

/**
 * Vocabulary definition for the 
 * <a href="https://www.w3.org/TR/vocab-data-cube/">W3C Data Cube Recommendation</a>.
 */
public class DataCubeOntology {
	/**
	 * The RDF model that holds the Data Cube entities
	 */
	private static final OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
	/**
	 * The namespace of the Data Cube vocabulary as a string
	 */
	public static final String uri = "http://purl.org/linked-data/cube#";
	/**
	 * Returns the namespace of the Data Cube schema as a string
	 * @return the namespace of the Data Cube schema
	 */
	public static String getURI() {
		return uri;
	}
	/**
	 * The namespace of the Data Cube vocabulary
	 */
	public static final Resource NAMESPACE = model.createResource(uri);

	/* ##########################################################
	 * Data Cube Classes
	   ########################################################## */
	public static final OntClass Attachable = model.createClass(uri + "Attachable");
	public static final OntClass AttributeProperty = model.createClass(uri + "AttributeProperty");
	public static final OntClass CodedProperty = model.createClass(uri + "CodedProperty");
	public static final OntClass ComponentProperty = model.createClass(uri + "ComponentProperty");
	public static final OntClass ComponentSet = model.createClass(uri + "ComponentSet");
	public static final OntClass ComponentSpecification = model.createClass(uri + "ComponentSpecification");
	public static final OntClass DataSet = model.createClass(uri + "DataSet");
	public static final OntClass DataStructureDefinition = model.createClass(uri + "DataStructureDefinition");
	public static final OntClass DimensionProperty = model.createClass(uri + "DimensionProperty");
	public static final OntClass HierarchicalCodeList = model.createClass(uri + "HierarchicalCodeList");
	public static final OntClass MeasureProperty = model.createClass(uri + "MeasureProperty");
	public static final OntClass Observation = model.createClass(uri + "Observation");
	public static final OntClass Slice = model.createClass(uri + "Slice");
	public static final OntClass ObservationGroup = model.createClass(uri + "ObservationGroup");
	public static final OntClass SliceKey = model.createClass(uri + "SliceKey");

	/* ##########################################################
	 * Data Cube Properties
	   ########################################################## */

	// Data properties
	public static final DatatypeProperty componentRequired = model.createDatatypeProperty(uri + "componentRequired");
	public static final DatatypeProperty order = model.createDatatypeProperty(uri + "order");
	// Object properties
	public static final ObjectProperty attribute = model.createObjectProperty(uri + "attribute");
	public static final ObjectProperty codeList = model.createObjectProperty(uri + "codeList");
	public static final ObjectProperty component = model.createObjectProperty(uri + "component");
	public static final ObjectProperty componentAttachment = model.createObjectProperty(uri + "componentAttachment");
	public static final ObjectProperty componentProperty = model.createObjectProperty(uri + "componentProperty");
	public static final ObjectProperty concept = model.createObjectProperty(uri + "concept");
	public static final ObjectProperty dataSet = model.createObjectProperty(uri + "dataSet");
	public static final ObjectProperty dimension = model.createObjectProperty(uri + "dimension");
	public static final ObjectProperty hierarchyRoot = model.createObjectProperty(uri + "hierarchyRoot");
	public static final ObjectProperty measure = model.createObjectProperty(uri + "measure");
	public static final ObjectProperty measureDimension = model.createObjectProperty(uri + "measureDimension");
	public static final ObjectProperty measureType = model.createObjectProperty(uri + "measureType");
	public static final ObjectProperty observation = model.createObjectProperty(uri + "observation");
	public static final ObjectProperty observationGroup = model.createObjectProperty(uri + "observationGroup");
	public static final ObjectProperty parentChildProperty = model.createObjectProperty(uri + "parentChildProperty");
	public static final ObjectProperty slice = model.createObjectProperty(uri + "slice");
	public static final ObjectProperty sliceKey = model.createObjectProperty(uri + "sliceKey");
	public static final ObjectProperty sliceStructure = model.createObjectProperty(uri + "sliceStructure");
	public static final ObjectProperty structure = model.createObjectProperty(uri + "structure");
}
