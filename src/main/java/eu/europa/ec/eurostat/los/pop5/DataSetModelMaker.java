package eu.europa.ec.eurostat.los.pop5;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import eu.europa.ec.eurostat.los.utils.DataCubeOntology;

/**
 * The <code>DataSetModelMaker</code> class creates the Data Cube Data Set for the POP5 data set.
 * 
 * @author Franck
 */
public class DataSetModelMaker {

	private static Logger logger = LogManager.getLogger(DataSetModelMaker.class);

	private static Workbook wb = null;

	public static void main(String[] args) throws Exception {

		wb = new HSSFWorkbook(new FileInputStream(Configuration.POP5_FILE_NAME));

		long totalSize = 0;
		Model pop5Model = null;
		// In order to limit memory requirements, we create the data set in 10 stages
		for (int digit = 0; digit < 10; digit++) {
			String initDigit = Integer.toString(digit);
			logger.info("Creating Jena model for POP5 data set, selecting geographic codes starting with " + initDigit);		
			pop5Model = getDataSetModel(code -> (code.startsWith(initDigit)), (digit == 0)); // Create the DSD only the first time
			totalSize += pop5Model.size();
			RDFDataMgr.write(new FileOutputStream("src/main/resources/data/ds-pop5-" + initDigit + ".ttl"), pop5Model, Lang.TURTLE);
			pop5Model.close();
			Thread.sleep(20000); // Rest a bit
		}
		// Create departemental data set
		Model pop5DepartementalObservations = getDataSetDepartementalObservations();
		totalSize += pop5DepartementalObservations.size();
		RDFDataMgr.write(new FileOutputStream("src/main/resources/data/ds-pop5-dep.ttl"), pop5DepartementalObservations, Lang.TURTLE);
		logger.info("Processing complete, total number of statements: " + totalSize);		
	}

	/**
	 * Reads the spreadsheet, extracts the data and converts it to RDF Data Cube.
	 * 
	 * @param geoSelector A predicate that can be used to produce only a part of the data set (for memory size reasons for example)
	 * @param createDS Indicates if the DataSet resource should be included in the model.
	 * @return A Jena model containing the data as a Data Cube Data Set.
	 */
	public static Model getDataSetModel(Predicate<String> geoSelector, boolean createDS) {

		Model pop5DSModel = ModelFactory.createDefaultModel();
		pop5DSModel.setNsPrefix("rdfs", RDFS.getURI());
		pop5DSModel.setNsPrefix("xsd", XSD.getURI());
		pop5DSModel.setNsPrefix("qb", DataCubeOntology.getURI());
		String basePrefix = "cog" + Configuration.REFERENCE_YEAR_GEO;
		pop5DSModel.setNsPrefix(basePrefix, Configuration.COG_BASE_CODE_URI);
		pop5DSModel.setNsPrefix(basePrefix + "-com", Configuration.communeURI(""));
		pop5DSModel.setNsPrefix(basePrefix + "-arm", Configuration.arrondissementMunicipalURI(""));
		pop5DSModel.setNsPrefix("pop5-ds", "http://id.insee.fr/meta/demo/pop5/dataSet/");
		pop5DSModel.setNsPrefix("pop5-obs", "http://id.insee.fr/meta/demo/pop5/observation/"); // TODO replace by call to dimensionURI("");
		pop5DSModel.setNsPrefix("cog2017-dim", "http://id.insee.fr/meta/cog2017/dimension/");
		pop5DSModel.setNsPrefix("dim", "http://id.insee.fr/meta/dimension/");
		pop5DSModel.setNsPrefix("mes", "http://id.insee.fr/meta/mesure/");
		pop5DSModel.setNsPrefix("cod-age", "http://id.insee.fr/codes/ageq65/");
		pop5DSModel.setNsPrefix("cod-sex", "http://id.insee.fr/codes/sexe/");
		pop5DSModel.setNsPrefix("cod-act", "http://id.insee.fr/codes/tactr/");

		// Creation of the data set
		Resource pop5DataSet = pop5DSModel.createResource(Configuration.dataSetURI(Configuration.REFERENCE_YEAR + "-depcomarm"), DataCubeOntology.DataSet);
		if (createDS) {
			String label = "POP5 - Population de 15 ans ou plus par commune ou arrondissement municipal, sexe, âge et type d'activité - France hors Mayotte - " + Configuration.REFERENCE_YEAR;
			pop5DataSet.addProperty(RDFS.label, pop5DSModel.createLiteral(label, "fr"));
			label = "POP5 - Population age 15 or more by municipality or municipal arrondissement, sex, age and type of activity - France except Mayotte - " + Configuration.REFERENCE_YEAR;
			pop5DataSet.addProperty(RDFS.label, pop5DSModel.createLiteral(label, "en"));
			pop5DataSet.addProperty(DataCubeOntology.structure, pop5DSModel.createResource(Configuration.dsdURI("depcomarm")));
			logger.info("Creating Data Set " + pop5DataSet.getURI());		
		}

		// The geometric dimension property and measure property will be useful
		Property geoDimensionProperty = pop5DSModel.createProperty(Configuration.geoDimensionURI);
		Property measureProperty = pop5DSModel.createProperty(Configuration.POP_MEASURE_URI);

		// Read the interpretative header (which is valid for both sheets)
		Sheet dataSheet = wb.getSheetAt(0);
		int firstHeaderLineIndex = Integer.parseInt(Configuration.HEADER_LINE_INDEXES.split("-")[0]);
		int lastHeaderLineIndex = Integer.parseInt(Configuration.HEADER_LINE_INDEXES.split("-")[1]);
		int headerSize = lastHeaderLineIndex - firstHeaderLineIndex + 1;
		Property[] dimensionProperties = new Property[headerSize];
		Map<Integer, String[]> header = new HashMap<Integer, String[]>();
		Map<Integer, Resource[]> headerURIs = new HashMap<Integer, Resource[]>();

		for (int index = 0; index < headerSize; index++) {
			Row headerRow = dataSheet.getRow(index + firstHeaderLineIndex);
			Iterator<Cell> cellIterator = headerRow.cellIterator();
			if (index == 0) cellIterator.next(); // Skip first column, only when it is not empty (i.e. first line of the header)
			String conceptCode = cellIterator.next().toString();
			dimensionProperties[index] = pop5DSModel.createProperty(Configuration.componentURI("dimension", conceptCode));
			while (cellIterator.hasNext()) {
				Cell headerCell = cellIterator.next();
				int headerIndex = headerCell.getColumnIndex();
				if (!header.containsKey(headerIndex)) {
					header.put(headerIndex, new String[headerSize]);
					headerURIs.put(headerIndex, new Resource[headerSize]);
				}
				header.get(headerIndex)[index] = headerCell.toString();
				headerURIs.get(headerIndex)[index] = pop5DSModel.createResource(Configuration.codeItemURI(conceptCode, headerCell.toString()));
			}
		}

		// First process municipalities sheet, then the arrondissements
		Iterator<Row> rows =null;
		for (int sheetIndex = 0; sheetIndex < 2; sheetIndex++) {
			rows = wb.getSheetAt(sheetIndex).rowIterator();
			while (rows.next().getRowNum() < Configuration.FIRST_DATA_LINE_INDEX); // Go to first data line
			while (rows.hasNext()) {
				Row currentRow = rows.next();
				Iterator<Cell> cellIterator = currentRow.cellIterator();
				// Get geographic code in first column and create associated resource
				String geoCode = cellIterator.next().toString();
				// Opportunity of sampling on municipality code
				if (!geoSelector.test(geoCode)) continue;
				Resource geoResource = pop5DSModel.createResource(Configuration.cogItemURI(geoCode));
				cellIterator.next(); // Skip geographic label
				while (cellIterator.hasNext()) {
					Cell observationCell = cellIterator.next();
					int columnNumber = observationCell.getColumnIndex();
					Resource observation = pop5DSModel.createResource(Configuration.observationURI(geoCode, header.get(columnNumber)), DataCubeOntology.Observation);
					observation.addProperty(DataCubeOntology.dataSet, pop5DataSet);
					// Add geographic dimension value
					observation.addProperty(geoDimensionProperty, geoResource);
					// Add other dimension values
					for (int index = 0; index < headerSize; index++) observation.addProperty(dimensionProperties[index], headerURIs.get(columnNumber)[index]);
					// Add measure
					float measure = (float) observationCell.getNumericCellValue();
					observation.addProperty(measureProperty, pop5DSModel.createTypedLiteral(measure));
				}
			}
		}
		logger.info("Model complete, number of statements: " + pop5DSModel.size());		

		return pop5DSModel;
	}
	
	/**
	 * Reads the spreadsheet, compute departemental measures and build observations.
	 * 
	 * @return A Jena model containing the departemental data.
	 */
	public static Model getDataSetDepartementalObservations() {
		
		// Read the interpretative header
		Sheet dataSheet = wb.getSheetAt(0);
		int firstHeaderLineIndex = Integer.parseInt(Configuration.HEADER_LINE_INDEXES.split("-")[0]);
		int lastHeaderLineIndex = Integer.parseInt(Configuration.HEADER_LINE_INDEXES.split("-")[1]);
		int headerSize = lastHeaderLineIndex - firstHeaderLineIndex + 1;
		Map<Integer, String[]> header = new HashMap<Integer, String[]>();

		for (int index = 0; index < headerSize; index++) {
			Row headerRow = dataSheet.getRow(index + firstHeaderLineIndex);
			Iterator<Cell> cellIterator = headerRow.cellIterator();
			if (index == 0) cellIterator.next(); // Skip first column, only when it is not empty (i.e. first line of the header)
			while (cellIterator.hasNext()) {
				Cell headerCell = cellIterator.next();
				int headerIndex = headerCell.getColumnIndex();
				if (!header.containsKey(headerIndex)) {
					header.put(headerIndex, new String[headerSize]);
				}
				header.get(headerIndex)[index] = headerCell.toString();
			}
		}
		
		// Read pop15plus measure values
		Map<String, Float> depMeasures = new HashMap<String, Float>();
		Iterator<Row> rows = null;
		rows = wb.getSheetAt(0).rowIterator();
		while (rows.next().getRowNum() < Configuration.FIRST_DATA_LINE_INDEX); // Go to first data line
		while (rows.hasNext()) {
			Row currentRow = rows.next();
			Iterator<Cell> cellIterator = currentRow.cellIterator();
			// Get geographic code in first column and create associated resource
			String geoCode = cellIterator.next().toString();
			cellIterator.next(); // Skip geographic label
			while (cellIterator.hasNext()) {
				Cell observationCell = cellIterator.next();
				int columnNumber = observationCell.getColumnIndex();
				String key = Configuration.getDepFromCommune(geoCode) + "-" + String.join("-", header.get(columnNumber));
				if (!depMeasures.containsKey(key)) depMeasures.put(key, (float) 0);
				float measure = (float) observationCell.getNumericCellValue();
				depMeasures.put(key, depMeasures.get(key) + measure);
			}
		}
		
		Model pop5DSDepModel = ModelFactory.createDefaultModel();
		pop5DSDepModel.setNsPrefix("xsd", XSD.getURI());
		pop5DSDepModel.setNsPrefix("qb", DataCubeOntology.getURI());
		String basePrefix = "cog" + Configuration.REFERENCE_YEAR_GEO;
		pop5DSDepModel.setNsPrefix(basePrefix, Configuration.COG_BASE_CODE_URI);
		pop5DSDepModel.setNsPrefix("pop5-ds", "http://id.insee.fr/meta/demo/pop5/dataSet/");
		pop5DSDepModel.setNsPrefix("pop5-obs", "http://id.insee.fr/meta/demo/pop5/observation/"); // TODO replace by call to dimensionURI("");
		pop5DSDepModel.setNsPrefix("cog2017-dim", "http://id.insee.fr/meta/cog2017/dimension/");
		pop5DSDepModel.setNsPrefix("dim", "http://id.insee.fr/meta/dimension/");
		pop5DSDepModel.setNsPrefix("mes", "http://id.insee.fr/meta/mesure/");
		pop5DSDepModel.setNsPrefix("cod-age", "http://id.insee.fr/codes/ageq65/");
		pop5DSDepModel.setNsPrefix("cod-sex", "http://id.insee.fr/codes/sexe/");
		pop5DSDepModel.setNsPrefix("cod-act", "http://id.insee.fr/codes/tactr/");	
		
		Resource pop5DataSet = pop5DSDepModel.createResource(Configuration.dataSetURI(Configuration.REFERENCE_YEAR + "-depcomarm"), DataCubeOntology.DataSet);
		// Dimensions and measure
		Property geoDimensionProperty = pop5DSDepModel.createProperty(Configuration.geoDimensionURI);
		Property sexDimensionProperty = pop5DSDepModel.createProperty(Configuration.componentURI("dimension", "SEXE"));
		Property ageq65DimensionProperty = pop5DSDepModel.createProperty(Configuration.componentURI("dimension", "AGEQ65"));
		Property tactrDimensionProperty = pop5DSDepModel.createProperty(Configuration.componentURI("dimension", "TACTR"));
		Property measureProperty = pop5DSDepModel.createProperty(Configuration.POP_MEASURE_URI);
		
		for (Map.Entry<String, Float> entry : depMeasures.entrySet()) {
			Resource observation = pop5DSDepModel.createResource(Configuration.observationURI(entry.getKey()), DataCubeOntology.Observation);
			observation.addProperty(DataCubeOntology.dataSet, pop5DataSet);
			// Get dimension values (dep-sex-ageq65-tactr)
			String[] dimensionValues = entry.getKey().split("-");
			// Add dimension values
			Resource geoResource = pop5DSDepModel.createResource(Configuration.cogItemURI(dimensionValues[0]));
			observation.addProperty(geoDimensionProperty, geoResource);
			Resource sexResource = pop5DSDepModel.createResource(Configuration.codeItemURI("SEXE", dimensionValues[1]));
			observation.addProperty(sexDimensionProperty, sexResource);
			Resource ageq65Resource = pop5DSDepModel.createResource(Configuration.codeItemURI("AGEQ65", dimensionValues[2]));
			observation.addProperty(ageq65DimensionProperty, ageq65Resource);
			Resource tactrResource = pop5DSDepModel.createResource(Configuration.codeItemURI("TACTR", dimensionValues[3]));
			observation.addProperty(tactrDimensionProperty, tactrResource);
			// Add measure
			observation.addProperty(measureProperty, pop5DSDepModel.createTypedLiteral(entry.getValue()));
		}		
		
		return pop5DSDepModel;	
	}
}
