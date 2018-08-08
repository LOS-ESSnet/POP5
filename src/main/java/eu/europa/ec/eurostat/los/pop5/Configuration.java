package eu.europa.ec.eurostat.los.pop5;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.XSD;

import eu.europa.ec.eurostat.los.utils.DataCubeOntology;


public class Configuration {

	public static final int REFERENCE_YEAR = 2015;
	public static final int REFERENCE_YEAR_GEO = REFERENCE_YEAR + 2;

	/** Name of the Excel file containing the POP5 data
	 * The source file is at https://insee.fr/fr/statistiques/3561090?sommaire=3561107 */
	public final static String POP5_FILE_NAME = "src/main/resources/data/BTX_TD_POP5_" + REFERENCE_YEAR + ".xls";
	public final static int FIRST_DATA_LINE_INDEX = 11;

	/** Prefix mappings */
	public static Map<String, String> DSD_PREFIXES = new HashMap<String, String>();
	static {
		DSD_PREFIXES.put("qb", DataCubeOntology.getURI());
		DSD_PREFIXES.put("rdfs", RDFS.getURI());
		DSD_PREFIXES.put("dc", DC.getURI());		
		DSD_PREFIXES.put("dc", DCTerms.getURI());		
		DSD_PREFIXES.put("xsd", XSD.getURI());
	}
	public static Map<String, String> CONCEPT_SCHEME_PREFIXES = new HashMap<String, String>();
	static {
		CONCEPT_SCHEME_PREFIXES.put("rdfs", RDFS.getURI());
		CONCEPT_SCHEME_PREFIXES.put("owl", OWL.getURI());
		CONCEPT_SCHEME_PREFIXES.put("skos", SKOS.getURI());
	}


	/** Location of the variable definitions */
	public final int DEFINITIONS_SHEET_INDEX = 2;
	public final static List<String> VARIABLE_DEFINITIONS = Arrays.asList("8-10", "12-23", "25-31");
	public final static String HEADER_LINE_INDEXES = "5-7";

	public static final String BASE_URI = "http://id.insee.fr/meta/";
	public static final String POP5_BASE_URI = BASE_URI + "demo/pop5/";

	public static final String POP_MEASURE_ID = "POP15PLUS";
	public static final String POP_MEASURE_URI = "http://id.insee.fr/meta/mesure/pop15Plus";
	public static final String POP_MEASURE_NAME = "Population de 15 ans ou plus";
	public static final String POP_CONCEPT_URI = "http://purl.org/linked-data/sdmx/2009/concept#statPop"; // Using the SDMX concept for now, maybe define a more specific one
	public static final String SDMX_OBS_VALUE_MEASURE_URI = "http://purl.org/linked-data/sdmx/2009/measure#obsValue";

	/** Naming constants and methods for geographic components */
	public static String COG_BASE_CODE_URI = "http://id.insee.fr/codes/cog" + REFERENCE_YEAR_GEO + "/";
	public static String GEO_CONCEPT_SCHEME_URI = COG_BASE_CODE_URI + "communesOuArrondissementsMunicipaux";
	public static String GEO_CODE_CONCEPT_URI = COG_BASE_CODE_URI + "CommuneOuArrondissementMunicipal";
	public static String communeURI(String code) {
		return COG_BASE_CODE_URI + "commune/" + code;	
	}
	public static String arrondissementMunicipalURI(String code) {
		return COG_BASE_CODE_URI + "arrondissementMunicipal/" + code;	
	}
	public static String cogItemURI(String code) {
		if (getParentGeoCode(code) == null) return communeURI(code);
		return arrondissementMunicipalURI(code);
	}
	public static String geoDimensionURI = "http://id.insee.fr/meta/cog" + REFERENCE_YEAR_GEO + "/dimension/communeOuArrondissementMunicipal";
	public static String getParentGeoCode(String code) {

		if (code == null) return null;
		if (code.startsWith("13") && (!"13055".equals(code))) return "13055";
		if (code.startsWith("69") && (!"69123".equals(code))) return "69123";
		if (code.startsWith("75") && (!"75056".equals(code))) return "75056";
		return null;
	}

	/** Naming constants and methods for other components */
	public static String codeItemURI(String conceptCode, String itemCode) {
		return "http://id.insee.fr/codes/" + conceptCode.toLowerCase() + "/" + itemCode;
	}

	public static String conceptSchemeURI(String conceptCode, String conceptName) {
		return "http://id.insee.fr/codes/" + conceptCode + "/" + getConceptSchemePathName(conceptCode, conceptName);
	}

	public static String codeConceptURI(String conceptCode, String conceptName) {
		return "http://id.insee.fr/codes/" + conceptCode + "/" + getConceptPathName(conceptCode, conceptName);
	}

	// A very basic implementation for now
	public static String conceptURI(String conceptCode, String conceptName) {
		return "http://id.insee.fr/concepts/" + getConceptPathName(conceptCode, conceptName);
	}

	public static String componentURI(String componentType, String conceptCode) { // Type should be 'attribute', 'dimension' or 'measure' but no control is made
		return "http://id.insee.fr/meta/" + componentType + "/" + conceptCode.toLowerCase();
	}

	public static String getSDMXBroaderConcept(String conceptCode) {

		if ("GEO".equalsIgnoreCase(conceptCode)) return "refArea";
		if ("SEXE".equalsIgnoreCase(conceptCode)) return "sex";
		if ("AGEQ65".equalsIgnoreCase(conceptCode)) return "age";
		// No SDMX concept for TACTR

		return null;
	}

	public static String getConceptSchemeName(String conceptCode, String conceptName) {

		if ("SEXE".equalsIgnoreCase(conceptCode)) return "Sexes";
		if ("AGEQ65".equalsIgnoreCase(conceptCode)) return "Âges quinquennaux";
		if ("TACTR".equalsIgnoreCase(conceptCode)) return "Types d'activité";

		return "Scheme";
	}

	public static String getConceptSchemePathName(String conceptCode, String conceptName) {

		if ("SEXE".equalsIgnoreCase(conceptCode)) return "sexes";
		if ("AGEQ65".equalsIgnoreCase(conceptCode)) return "agesQuinquennaux";
		if ("TACTR".equalsIgnoreCase(conceptCode)) return "typesDActivite";

		return "scheme";
	}

	public static String getConceptPathName(String conceptCode, String conceptName) {

		if ("SEXE".equalsIgnoreCase(conceptCode)) return "Sexe";
		if ("AGEQ65".equalsIgnoreCase(conceptCode)) return "AgeQuinquennal";
		if ("TACTR".equalsIgnoreCase(conceptCode)) return "TypeDActivite";

		return "Concept";
	}

	public static String dsdURI(String dsdId) {
		return POP5_BASE_URI + "dsd/" + dsdId;	
	}

	public static String dataSetURI(String dataSetId) {
		return POP5_BASE_URI + "dataSet/" + dataSetId;	
	}

	public static String observationURI(String geoCode, String[] dimensionValues) {
		return POP5_BASE_URI + "observation/" + REFERENCE_YEAR + "-" + geoCode + "-" + String.join("-", dimensionValues);	
	}
}
