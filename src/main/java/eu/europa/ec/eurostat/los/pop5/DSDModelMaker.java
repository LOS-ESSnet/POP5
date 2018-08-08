package eu.europa.ec.eurostat.los.pop5;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Iterator;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.XSD;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import eu.europa.ec.eurostat.los.utils.DataCubeOntology;

/**
 * The <code>DSDModelMaker</code> class creates the Data Cube Data Structure Definition for the POP5 data set.
 * 
 * @author Franck
 */
public class DSDModelMaker {

	private static Logger logger = LogManager.getLogger(DSDModelMaker.class);

	private static Workbook wb = null;

	public static void main(String[] args) throws Exception {

		wb = new HSSFWorkbook(new FileInputStream(Configuration.POP5_FILE_NAME));

		Model pop5Model = getGeoConceptScheme();
		RDFDataMgr.write(new FileOutputStream("src/main/resources/data/cs-cog2017.ttl"), pop5Model, Lang.TURTLE);
		for (String variable : Configuration.VARIABLE_DEFINITIONS) {
			Model csModel = getConceptScheme(variable);
			String fileName = "src/main/resources/data/cs-"  + getConceptCode(variable) + ".ttl";
			RDFDataMgr.write(new FileOutputStream(fileName), csModel, Lang.TURTLE);
		}
		Model pop5DSDModel = getPOP5DSDModel();
		RDFDataMgr.write(new FileOutputStream("src/main/resources/data/dsd-pop5.ttl"), pop5DSDModel, Lang.TURTLE);
	}

	/**
	 * Reads the spreadsheet and extracts the concept scheme of geographic territories.
	 * 
	 * @return A Jena model containing the code list of geographic territories as a SKOS concept scheme.
	 */
	public static Model getGeoConceptScheme() {

		Model geoModel = ModelFactory.createDefaultModel();
		geoModel.setNsPrefix("rdfs", RDFS.getURI());
		geoModel.setNsPrefix("owl", OWL.getURI());
		geoModel.setNsPrefix("skos", SKOS.getURI());
		String basePrefix = "cog" + Configuration.REFERENCE_YEAR_GEO;
		geoModel.setNsPrefix(basePrefix, Configuration.COG_BASE_CODE_URI);
		geoModel.setNsPrefix(basePrefix + "-com", Configuration.communeURI(""));
		geoModel.setNsPrefix(basePrefix + "-arm", Configuration.arrondissementMunicipalURI(""));

		// Create the concept scheme and the associated concept
		Resource geoCS = geoModel.createResource(Configuration.GEO_CONCEPT_SCHEME_URI, SKOS.ConceptScheme);
		geoCS.addProperty(SKOS.prefLabel, geoModel.createLiteral("Liste des communes et arrondissements municipaux au 1er janvier " + Configuration.REFERENCE_YEAR_GEO, "fr"));
		geoCS.addProperty(SKOS.prefLabel, geoModel.createLiteral("List of municipalities and municipal arrondissements on 1 January " + Configuration.REFERENCE_YEAR_GEO, "en"));
		Resource geoConcept = geoModel.createResource(Configuration.GEO_CODE_CONCEPT_URI, OWL.Class);
		geoConcept.addProperty(RDF.type, RDFS.Class);
		geoConcept.addProperty(RDFS.subClassOf, SKOS.Concept);
		geoConcept.addProperty(SKOS.prefLabel, geoModel.createLiteral("Commune ou arrondissement municipal au 1er janvier " + Configuration.REFERENCE_YEAR_GEO, "fr"));
		geoConcept.addProperty(SKOS.prefLabel, geoModel.createLiteral("Municipality or municipal arrondissement on 1 January " + Configuration.REFERENCE_YEAR_GEO, "en"));
		geoConcept.addProperty(SKOS.notation, geoModel.createLiteral("COG " + Configuration.REFERENCE_YEAR_GEO, "fr"));
		geoCS.addProperty(RDFS.seeAlso, geoConcept);
		geoConcept.addProperty(RDFS.seeAlso, geoCS);

		// First process municipalities sheet
		Iterator<Row> rows = wb.getSheetAt(0).rowIterator();
		while (rows.hasNext()) {
			Row currentRow = rows.next();
			if (currentRow.getRowNum() < Configuration.FIRST_DATA_LINE_INDEX) continue; // TODO Define constant
			String code = currentRow.getCell(0, MissingCellPolicy.CREATE_NULL_AS_BLANK).toString();
			String name = currentRow.getCell(1, MissingCellPolicy.CREATE_NULL_AS_BLANK).toString();
			Resource geoEntry = geoModel.createResource(Configuration.cogItemURI(code), geoConcept);
			geoEntry.addProperty(RDF.type, SKOS.Concept); // For stupid clients
			geoEntry.addProperty(SKOS.notation, code);
			geoEntry.addProperty(SKOS.prefLabel, geoModel.createLiteral(name, "fr"));
			geoEntry.addProperty(SKOS.topConceptOf, geoCS);
		}
		// Then the arrondissements
		rows = wb.getSheetAt(1).rowIterator();
		while (rows.hasNext()) {
			Row currentRow = rows.next();
			if (currentRow.getRowNum() < Configuration.FIRST_DATA_LINE_INDEX) continue; // TODO Define constant
			String code = currentRow.getCell(0, MissingCellPolicy.CREATE_NULL_AS_BLANK).toString();
			String name = currentRow.getCell(1, MissingCellPolicy.CREATE_NULL_AS_BLANK).toString();
			Resource geoEntry = geoModel.createResource(Configuration.cogItemURI(code), geoConcept);
			geoEntry.addProperty(RDF.type, SKOS.Concept); // For stupid clients
			geoEntry.addProperty(SKOS.notation, code);
			geoEntry.addProperty(SKOS.prefLabel, geoModel.createLiteral(name, "fr"));
			geoEntry.addProperty(SKOS.inScheme, geoCS);
			String parentCode = Configuration.getParentGeoCode(code);
			if (parentCode != null) {
				Resource parent = geoModel.createResource(Configuration.cogItemURI(parentCode), geoConcept); // Normally already in the model
				parent.addProperty(SKOS.narrower, geoEntry);
				geoEntry.addProperty(SKOS.broader, parent);
			}
		}

		return geoModel;
	}

	private static String getConceptCode(String variable) {

		int firstLineIndex = Integer.parseInt(variable.split("-")[0]) - 1;
		String listTitle = wb.getSheetAt(2).getRow(firstLineIndex).getCell(0).toString();

		return listTitle.split(":")[0].trim().toLowerCase();
	}

	private static String getConceptName(String variable) {

		int firstLineIndex = Integer.parseInt(variable.split("-")[0]) - 1;
		String listTitle = wb.getSheetAt(2).getRow(firstLineIndex).getCell(0).toString();

		return listTitle.split(":")[1].trim();
	}

	/**
	 * Returns a model containing the code list corresponding to a variable definition.
	 * 
	 * @param variable The location of the variable definitions (lines in the input spreadsheet).
	 * @return A Jena model containing the code list associated to the variable.
	 */
	public static Model getConceptScheme(String variable) {

		int firstLineIndex = Integer.parseInt(variable.split("-")[0]) - 1;
		int lastLineIndex = Integer.parseInt(variable.split("-")[1]) - 1;

		Sheet variableListSheet = wb.getSheetAt(2);
		String listTitle = variableListSheet.getRow(firstLineIndex).getCell(0).toString();
		// Format is CODE : Concept
		String conceptCode = listTitle.split(":")[0].trim().toLowerCase();
		String conceptName = listTitle.split(":")[1].trim();

		Model conceptModel = ModelFactory.createDefaultModel();
		conceptModel.setNsPrefixes(Configuration.CONCEPT_SCHEME_PREFIXES);
		// Create the concept scheme and the associated concept
		Resource scheme = conceptModel.createResource(Configuration.conceptSchemeURI(conceptCode, conceptName), SKOS.ConceptScheme);
		scheme.addProperty(SKOS.prefLabel, conceptModel.createLiteral(Configuration.getConceptSchemeName(conceptCode, conceptName), "fr"));
		scheme.addProperty(SKOS.notation, conceptModel.createLiteral(conceptCode.toUpperCase(), "fr"));
		Resource concept = conceptModel.createResource(Configuration.codeConceptURI(conceptCode, conceptName), OWL.Class);
		concept.addProperty(RDF.type, RDFS.Class);
		concept.addProperty(RDFS.subClassOf, SKOS.Concept);
		concept.addProperty(SKOS.prefLabel, conceptModel.createLiteral(conceptName, "fr"));
		concept.addProperty(SKOS.notation, conceptModel.createLiteral(conceptCode, "fr"));
		scheme.addProperty(RDFS.seeAlso, concept);
		concept.addProperty(RDFS.seeAlso, scheme);

		for (int rowIndex = firstLineIndex + 1; rowIndex <= lastLineIndex; rowIndex++) {

			String entryTitle = variableListSheet.getRow(rowIndex).getCell(0).toString();

			String entryCode = entryTitle.split(":")[0].trim();
			String entryName = entryTitle.split(":")[1].trim();
			Resource entry = conceptModel.createResource(Configuration.codeItemURI(conceptCode, entryCode), concept);
			entry.addProperty(RDF.type, SKOS.Concept); // For stupid clients
			entry.addProperty(SKOS.notation, entryCode);
			entry.addProperty(SKOS.prefLabel, conceptModel.createLiteral(entryName, "fr"));
			entry.addProperty(SKOS.inScheme, scheme);
		}
		return conceptModel;
	}


	/**
	 * Returns a Jena Model containing the DSD and its components (including concepts and code lists).
	 * 
	 * @return The Data Cube data structure definition as a Jena model.
	 */
	public static Model getPOP5DSDModel() { // TODO Distinguish COM and/or ARM ?

		Model pop5DSDModel = ModelFactory.createDefaultModel();
		pop5DSDModel.setNsPrefixes(Configuration.DSD_PREFIXES);

		// Creation of the DSD
		Resource pop5DSD = pop5DSDModel.createResource(Configuration.dsdURI(Configuration.REFERENCE_YEAR + "-comarm"), DataCubeOntology.DataStructureDefinition);
		pop5DSD.addProperty(RDFS.label, pop5DSDModel.createLiteral("Définition de structure de données pour POP5, année " + Configuration.REFERENCE_YEAR, "fr"));
		pop5DSD.addProperty(RDFS.label, pop5DSDModel.createLiteral("Data structure definition pour POP5, year " + Configuration.REFERENCE_YEAR, "en"));
		pop5DSD.addProperty(DC.description, pop5DSDModel.createLiteral("Population de 15 ans et plus par tranche d'âge, sexe et type d'activité, année " + Configuration.REFERENCE_YEAR, "fr"));
		pop5DSD.addProperty(DC.description, pop5DSDModel.createLiteral("Population age 15 or more by age group, sex and type of activity, year " + Configuration.REFERENCE_YEAR, "en"));
		pop5DSD.addProperty(DCTerms.identifier, pop5DSDModel.createLiteral("DSD-POP5-COMARM", "fr"));
		logger.info("Creating DSD " + pop5DSD.getURI());

		// Create the geographic dimension property
		Resource pop5GeoDimensionProperty = pop5DSDModel.createResource(Configuration.geoDimensionURI, DataCubeOntology.DimensionProperty).addProperty(RDF.type, DataCubeOntology.CodedProperty);
		pop5GeoDimensionProperty.addProperty(RDFS.subPropertyOf, pop5DSDModel.createResource("http://purl.org/linked-data/sdmx/2009/dimension#refArea"));
		pop5GeoDimensionProperty.addProperty(RDFS.label, pop5DSDModel.createLiteral("Commune ou arrondissement municipal (COG " + Configuration.REFERENCE_YEAR_GEO + ")", "fr"));
		pop5GeoDimensionProperty.addProperty(DataCubeOntology.concept, pop5DSDModel.createResource("http://purl.org/linked-data/sdmx/2009/concept#refArea")); // Could create specific sub-concept
		pop5GeoDimensionProperty.addProperty(DCTerms.identifier, pop5DSDModel.createLiteral("COG " + Configuration.REFERENCE_YEAR_GEO, "fr"));
		pop5GeoDimensionProperty.addProperty(RDFS.range, pop5DSDModel.createResource(Configuration.GEO_CODE_CONCEPT_URI));
		pop5GeoDimensionProperty.addProperty(DataCubeOntology.codeList, pop5DSDModel.createResource(Configuration.GEO_CONCEPT_SCHEME_URI));
		// Attach the geographic dimension property to the DSD through anonymous ComponentSpecification
		pop5DSD.addProperty(DataCubeOntology.component, pop5DSDModel.createResource(DataCubeOntology.ComponentSpecification).addProperty(DataCubeOntology.dimension, pop5GeoDimensionProperty));

		// Create the other dimension properties
		for (String variable : Configuration.VARIABLE_DEFINITIONS) {
			String conceptCode = getConceptCode(variable);
			String conceptName = getConceptName(variable);
			String sdmxBroaderConcept = Configuration.getSDMXBroaderConcept(conceptCode);
			// Create the dimension property
			Resource dimensionProperty = pop5DSDModel.createResource(Configuration.componentURI("dimension", conceptCode), DataCubeOntology.DimensionProperty).addProperty(RDF.type, DataCubeOntology.CodedProperty);
			if (sdmxBroaderConcept != null) dimensionProperty.addProperty(RDFS.subPropertyOf, pop5DSDModel.createResource("http://purl.org/linked-data/sdmx/2009/dimension#" + sdmxBroaderConcept));
			dimensionProperty.addProperty(RDFS.label, pop5DSDModel.createLiteral(conceptName, "fr"));
			// If there is a SDMX concept, use it (we could also create a specific sub-concept), otherwise create ad hoc concept
			if (sdmxBroaderConcept != null) dimensionProperty.addProperty(DataCubeOntology.concept, pop5DSDModel.createResource("http://purl.org/linked-data/sdmx/2009/concept#" + sdmxBroaderConcept));
			else dimensionProperty.addProperty(DataCubeOntology.concept, pop5DSDModel.createResource(Configuration.conceptURI(conceptCode,conceptName)));
			dimensionProperty.addProperty(DCTerms.identifier, pop5DSDModel.createLiteral(conceptCode, "fr"));
			dimensionProperty.addProperty(RDFS.range, pop5DSDModel.createResource(Configuration.codeConceptURI(conceptCode, conceptName)));
			dimensionProperty.addProperty(DataCubeOntology.codeList, pop5DSDModel.createResource(Configuration.conceptSchemeURI(conceptCode, conceptName)));
			// Attach the dimension property to the DSD through anonymous ComponentSpecification
			pop5DSD.addProperty(DataCubeOntology.component, pop5DSDModel.createResource(DataCubeOntology.ComponentSpecification).addProperty(DataCubeOntology.dimension, dimensionProperty));
		}

		// Create and attach the measure property
		Resource measureProperty = pop5DSDModel.createResource(Configuration.POP_MEASURE_URI, DataCubeOntology.MeasureProperty); // The component is not coded
		measureProperty.addProperty(RDFS.subPropertyOf, pop5DSDModel.createResource(Configuration.SDMX_OBS_VALUE_MEASURE_URI));
		measureProperty.addProperty(RDFS.label, pop5DSDModel.createLiteral(Configuration.POP_MEASURE_NAME, "fr"));
		measureProperty.addProperty(DCTerms.identifier, pop5DSDModel.createLiteral(Configuration.POP_MEASURE_ID, "fr"));
		measureProperty.addProperty(DataCubeOntology.concept, pop5DSDModel.createResource(Configuration.POP_CONCEPT_URI));
		measureProperty.addProperty(RDFS.range, XSD.xint);
		pop5DSD.addProperty(DataCubeOntology.component, pop5DSDModel.createResource(DataCubeOntology.ComponentSpecification).addProperty(DataCubeOntology.measure, measureProperty));

		return pop5DSDModel;
	}
}
