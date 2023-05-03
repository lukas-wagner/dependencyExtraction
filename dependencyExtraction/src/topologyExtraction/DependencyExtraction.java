package topologyExtraction;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Class DependencyExtraction.
 * @author Lukas Wagner - Helmut-Schmidt-Universität	
 */
public class DependencyExtraction {

	public static HashMap<String, String> attributeValuesOfExternalInterfaceIds = new HashMap<>(); 
	public static HashMap<String, String> relevantExternalInterfacesAndItsParents = new HashMap<>(); 
	public static HashMap<String, String> relevantInternalLinks = new HashMap<>(); 
	public static HashMap<String, String> directionValueExternalInterfaceID = new HashMap<>(); 
	public static HashMap<String, String> internalElementsAndDependencyType = new HashMap<>(); 
	public static HashMap<String, String> externalInterfacesAndDependencyType = new HashMap<>(); 
	public static List<String> listOfResources = new ArrayList<>();


	// List of all dependencies with external elements of type "ProcessInformation"
	public static List<Dependencies> listofDependenciesAndTheirDirection = new ArrayList<>();

	// list of all dependencies iwth external elements of types "ProcessProduct" or "ProcessInformation"
	public static List<Dependencies> listOfDependenciesWithEnergyCarriers = new ArrayList<>();

	//Logic: dependencies which are in listofDependenciesAndTheirDirection AND NOT in listOfDependenciesWithDifferentEnergyCarriers are purely logically limiting

	public DependencyExtraction () {

	}

	public DependencyExtraction (String filePath) throws Exception {
		findAllDependenciesOfResourcesAndDirectionAndType(filePath);
		findConnectedResourcesWithEnergyFlows(filePath);
		determineListOfResources(filePath);
	}

	public static void main(String[] args) throws Exception {
		String filePath = "./src/topologyExtraction/Elektrolyse_Prozess_v2.aml";
		findAllDependenciesOfResourcesAndDirectionAndType(filePath);
//		determineTypeOfDependency(filePath);
		findConnectedResourcesWithEnergyFlows(filePath);
		//		for (int i = 0; i < getListofDependenciesAndTheirDirection().size(); i++) {
		//
		//		}
	}

	/**
	 * Gets the ID of InternalLinks of both RefPartnerA and B if externalInterface == "Name" by internal elements from AML File.
	 *
	 * @param filePathOfAMLFile the file path of AML file
	 * @return the attribute by internal elements
	 * @throws Exception the exception
	 */
	public static List<Dependencies> findAllDependenciesOfResourcesAndDirectionAndType (String filePathOfAMLFile) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document amlFile = db.parse(new File(filePathOfAMLFile));

		// relevant external interfaces 
		getRelevantExternalInterfacesAndItsParents().putAll(findRelevantExternalInterfacesAndItsParents(createNodeListByElementName(amlFile, "ExternalInterface"),"RefBaseClassPath", "ProcessInformation"));

		// internal links connecting relevant external interfaces
		getRelevantInternalLinks().putAll(findInternalLinksToRelevantExternalInterfaces(createNodeListByElementName(amlFile, "InternalLink"), getRelevantExternalInterfacesAndItsParents(), "AB"));

		// direction of internal links connecting relevant external interfaces
		getDirectionValueExternalInterfaceID().putAll(findAttributeValuesOfExternalInterfaces(amlFile, getRelevantExternalInterfacesAndItsParents())); 
		determineTypeOfDependency(filePathOfAMLFile);

		// return: name of resource a, name of resource b, type of connection
		List<Dependencies> connectedResources = findConnectedResources(getRelevantExternalInterfacesAndItsParents(), getRelevantInternalLinks(), getDirectionValueExternalInterfaceID(), amlFile);
		getListofDependenciesAndTheirDirection().addAll(connectedResources);

		//		combineListOfDependencyTypeWithListOfDependencies(); 

				System.out.println(getListofDependenciesAndTheirDirection().size());
				for (int i = 0; i < getListofDependenciesAndTheirDirection().size(); i++) {
					System.out.println(getListofDependenciesAndTheirDirection().get(i).getStartResource() + " "+ getListofDependenciesAndTheirDirection().get(i).getEndResource() + " "+ getListofDependenciesAndTheirDirection().get(i).getType());
				}
		return connectedResources;
	}


	/**
	 * Find connected Resources with energy flows and saves to global List of dependencies with energy/medium flows
	 *
	 * @param filePathOfAMLFile the file path of AML file
	 * @throws Exception the exception
	 */
	public static void findConnectedResourcesWithEnergyFlows (String filePathOfAMLFile) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document amlFile = db.parse(new File(filePathOfAMLFile));
		//		-----
		// Extract ProcessProduct int links and direction values
		// List of all Resource Names which have Processes which are linked with internal elements of external interfaces of type "ProcessProduct" or "ProcessEnergy"
		HashMap<String, String> extInterfaceProcessProduct = findRelevantExternalInterfacesAndItsParents(createNodeListByElementName(amlFile, "ExternalInterface"),"RefBaseClassPath", "ProcessProduct");
		HashMap<String, String> internalLinksProcessProduct = findInternalLinksToRelevantExternalInterfaces(createNodeListByElementName(amlFile, "InternalLink"), extInterfaceProcessProduct, "AB");
		HashMap<String, String> extInterfaceProcessEnergy = findRelevantExternalInterfacesAndItsParents(createNodeListByElementName(amlFile, "ExternalInterface"),"RefBaseClassPath", "ProcessEnergy");
		HashMap<String, String> internalLinksProcessEnergy = findInternalLinksToRelevantExternalInterfaces(createNodeListByElementName(amlFile, "InternalLink"), extInterfaceProcessEnergy, "AB");
		HashMap<String, String> directionValuesProduct = findAttributeValuesOfExternalInterfaces(amlFile, extInterfaceProcessProduct);
		HashMap<String, String> mediumOfExternalInterfaceProduct = findMediumOfInternalElement(amlFile, extInterfaceProcessProduct, internalLinksProcessProduct);
		HashMap<String, String> mediumOfExternalInterfaceEnergy = findMediumOfInternalElement(amlFile, extInterfaceProcessEnergy, internalLinksProcessEnergy);
		HashMap<String, String> directionValuesEnergy = findAttributeValuesOfExternalInterfaces(amlFile, extInterfaceProcessEnergy);

		// identify connected resources	
		getListOfDependenciesWithEnergyCarriers().addAll(findConnectedResourcesAndMedium(extInterfaceProcessProduct, internalLinksProcessProduct, directionValuesProduct, mediumOfExternalInterfaceProduct, amlFile));
		getListOfDependenciesWithEnergyCarriers().addAll(findConnectedResourcesAndMedium(extInterfaceProcessEnergy, internalLinksProcessEnergy, directionValuesEnergy, mediumOfExternalInterfaceEnergy, amlFile));
		//		System.out.println(getListOfDependenciesWithEnergyCarriers().size());
		//		for (int i = 0; i < getListOfDependenciesWithEnergyCarriers().size(); i++) {
		//			System.out.println(getListOfDependenciesWithEnergyCarriers().get(i).getStartResource() + " "+ getListOfDependenciesWithEnergyCarriers().get(i).getEndResource() +" " + getListOfDependenciesWithEnergyCarriers().get(i).getMedium());
		//		}
	}

	/**
	 * Gets the list of resources.
	 *
	 * @param amlFile the aml file
	 * @return the list of resources connected to processes which are connected to others via internal links
	 * @throws ParserConfigurationException 
	 */
	public static void determineListOfResources (String filePathOfAMLFile) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document amlFile = db.parse(new File(filePathOfAMLFile));

		NodeList internalElements = createNodeListByElementName(amlFile, "InternalElement");
		List<String> Resources = new ArrayList<>(); 
		String Resource = "";

		for (int i = 0; i < internalElements.getLength(); i++) {

			if  (internalElements.item(i).getAttributes().getNamedItem("RefBaseSystemUnitPath").getNodeValue().contains("Resource")) {
				Resource = internalElements.item(i).getAttributes().getNamedItem("Name").getNodeValue();
				//System.out.println("Resource " + i+  " "  + Resource);
				Resources.add(Resource);
			}
		}
		getListOfResources().addAll(Resources);
		//		return listOfResources;
	}

	/**
	 * Determine type of dependency.
	 * if SUC is  "SystemUnitClassLib/Information/Dependency" then Restrictive else Correlative 
	 * and "return" in List of dependencies and the ID of int. element
	 * @throws Exception 
	 */
	public static void determineTypeOfDependency (String filePathOfAMLFile) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document amlFile = db.parse(new File(filePathOfAMLFile));

		NodeList internalElements = createNodeListByElementName(amlFile, "InternalElement");


		HashMap<String, String> dependencyTypeOfIE = new HashMap<>();
		String restrictive = "RestrictiveDependency"; 
		String correlative = "CorrelativeDependency";
		String idOfIE; 
		for (int i = 0; i < internalElements.getLength(); i++) {
			if (internalElements.item(i).getAttributes().getNamedItem("RefBaseSystemUnitPath").getNodeValue().equals("SystemUnitClassLib/Information/Dependency/RestrictiveDependency")==true) {
				// Restrictive
				idOfIE = internalElements.item(i).getAttributes().getNamedItem("ID").getNodeValue();
				dependencyTypeOfIE.put(idOfIE, restrictive);
//				System.out.println(idOfIE+ " " + restrictive);


			} else if (internalElements.item(i).getAttributes().getNamedItem("RefBaseSystemUnitPath").getNodeValue().equals("SystemUnitClassLib/Information/Dependency")==true) {
				// Correlative
				idOfIE = internalElements.item(i).getAttributes().getNamedItem("ID").getNodeValue();
				dependencyTypeOfIE.put(idOfIE, correlative);
//				System.out.println(idOfIE+ " " + correlative);
			}
		}
		String[] dependencyTypeOfIEArray = new String[dependencyTypeOfIE.size()];
		int aaaaaaaaa = 0; 
		for (Entry<String, String> entry : dependencyTypeOfIE.entrySet()) {
			dependencyTypeOfIEArray[aaaaaaaaa] = entry.getKey();
			//			System.out.println(i+ " "+ entry.getKey());
			aaaaaaaaa++;
		}


		//				mit value of relevantEIandParentNames den key der ext interfaces zuordnen
		getRelevantExternalInterfacesAndItsParents();
		String[] externalInterfaceIDs = new String[getRelevantExternalInterfacesAndItsParents().size()];
		String[] internalElementIDs = new String[getRelevantExternalInterfacesAndItsParents().size()];
		int ddddd = 0; 
		for (Entry<String, String> entry : getRelevantExternalInterfacesAndItsParents().entrySet()) {
			externalInterfaceIDs[ddddd] = entry.getKey();
			internalElementIDs[ddddd] = entry.getValue();
//			System.out.println(ddddd+ " "+ entry.getKey() + " " + entry.getValue());
			ddddd++;
		}
		HashMap<String, String> dependencyTypeOfExtInt = new HashMap<>();
		for (int i = 0; i < getRelevantExternalInterfacesAndItsParents().size(); i++) {
			for (int j = 0; j < dependencyTypeOfIEArray.length; j++) {
				if (internalElementIDs[i].equals(dependencyTypeOfIEArray[j])) {
					dependencyTypeOfExtInt.put(externalInterfaceIDs[i], dependencyTypeOfIE.get(dependencyTypeOfIEArray[j]));
//					System.out.println("fjfjfj "+ externalInterfaceIDs[i]+ " " +dependencyTypeOfIE.get(dependencyTypeOfIEArray[j])); 
				}
			}
		}

		getInternalElementsAndDependencyType().putAll(dependencyTypeOfIE);
		
		// col. 1: id of external elements, col. 2 type of dep.
		getExternalInterfacesAndDependencyType().putAll(dependencyTypeOfExtInt);
		
		
		HashMap<String, String> intLinksExtInt = findInternalLinksToRelevantExternalInterfaces(createNodeListByElementName(amlFile, "InternalLink"), getExternalInterfacesAndDependencyType(), "AB");
		for (Entry<String, String> entry : intLinksExtInt.entrySet()) {
//			System.out.println("uedududud"+ entry.getKey() + " " + entry.getValue() + " " + getExternalInterfacesAndDependencyType().get(entry.getKey()));
		}
//		getExternalInterfacesOfDependency(filePathOfAMLFile); 
	}


	/**
	 * Gets the external interfaces of dependency.
	 *
	 * @return the external interfaces of dependency internal elements and type of dependency
	 * @throws Exception the exception
	 */
	public static void getExternalInterfacesOfDependency (String filePathOfAMLFile) throws Exception {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document amlFile = db.parse(new File(filePathOfAMLFile));

		//col 1: ExternalInterfaceID, col 2: InternalElementID
		HashMap<String, String> extIntAndParents = findRelevantExternalInterfacesAndItsParents(createNodeListByElementName(amlFile, "ExternalInterface"),"RefBaseClassPath", "ProcessInformation");

		// convert first col. of hashmap to array
		String[] externalInterfaceIDs = new String[extIntAndParents.size()];
		int i = 0; 
		for (Entry<String, String> entry : extIntAndParents.entrySet()) {
			externalInterfaceIDs[i] = entry.getKey();
			//			System.out.println(i+ " "+ entry.getKey());
			i++;
		}
		HashMap<String, String> extIntIDAndDepType = new HashMap<>(); 
		// if getInternalElementsAndDependencyType().get(i) is in extIntAndParents, save ID of extInt with dependency type
		for (int j = 0; j < externalInterfaceIDs.length; j++) {
			if (getInternalElementsAndDependencyType().get(extIntAndParents.get(externalInterfaceIDs[j]))!=null) {
				extIntIDAndDepType.put(externalInterfaceIDs[j], getInternalElementsAndDependencyType().get(extIntAndParents.get(externalInterfaceIDs[j])));
				//	System.out.println("ExternalInterface"+ " " + externalInterfaceIDs[j] + " " +  getInternalElementsAndDependencyType().get(extIntAndParents.get(externalInterfaceIDs[j])));
			}
		}


		HashMap<String, String> iLExtInt = findInternalLinksToRelevantExternalInterfaces(createNodeListByElementName(amlFile, "InternalLink"), extIntIDAndDepType, "AB");
		for (Entry<String, String> entry : iLExtInt.entrySet()) {
			System.out.println("asdf"+ " "+ entry.getKey()+ " " + entry.getValue());
		}

		// convert first col. of hashmap to array
		String[] externalInterfaceIDs2 = new String[extIntIDAndDepType.size()];
		int kk = 0; 
		for (Entry<String, String> entry : extIntIDAndDepType.entrySet()) {
			externalInterfaceIDs2[kk] = entry.getKey();
			//			System.out.println(i+ " "+ entry.getKey());
			kk++;
		}
		// value und type in hashmap schreiben
		HashMap<String, String> extIntIDAndDepType2 = new HashMap<>(); 
		for (int j = 0; j < extIntIDAndDepType.size(); j++) {
			if (iLExtInt.get(externalInterfaceIDs2[j])!=null) {
				extIntIDAndDepType2.put(iLExtInt.get(externalInterfaceIDs2[j]),extIntIDAndDepType.get(externalInterfaceIDs2[j]));
				//				System.out.println("aasaa" + iLExtInt.get(externalInterfaceIDs2[j])+ " " +" " +extIntIDAndDepType.get(externalInterfaceIDs2[j]));
			}
		}


		// save list to HashMap
		getExternalInterfacesAndDependencyType().putAll(extIntIDAndDepType2);
	}

	/**
	 * Find relevant external interfaces and its parents, if parent element NodeName == InternalElement.
	 *
	 * @param partOfAMLfile the external interface
	 * @return the hash map of key: external interface id, value: internal element id (=parent element)
	 */
	@SuppressWarnings("unused")
	public static HashMap<String, String> findRelevantExternalInterfacesAndItsParents (NodeList partOfAMLfile, String namedItem, String valueOfNamedItem) {
		HashMap<String, String> relevantEIandParentNames = new HashMap<String, String>(); 
		String extractedElement = ""; 
		String extractedParentElement = ""; 
		String extractedParentElementID = ""; 
		for (int i = 0; i < partOfAMLfile.getLength(); i++) {
			if (partOfAMLfile.item(i).getAttributes().getNamedItem(namedItem).getNodeValue().toString().contains(valueOfNamedItem)) {
				// if parent element type = internalElement
				if (partOfAMLfile.item(i).getParentNode().getNodeName().equals("InternalElement")) {
					extractedElement = partOfAMLfile.item(i).getAttributes().getNamedItem("ID").getNodeValue();
					extractedParentElement = partOfAMLfile.item(i).getParentNode().getAttributes().getNamedItem("Name").getNodeValue();
					extractedParentElementID = partOfAMLfile.item(i).getParentNode().getAttributes().getNamedItem("ID").getNodeValue(); 
					//										System.out.println("a"+ i + " " + extractedElement + " " + extractedParentElement+ " " + extractedParentElementID);
					// col 1: ExternalInterfaceID, col 2: InternalElementID
					relevantEIandParentNames.put(extractedElement, extractedParentElementID);
				}
			}
		}
		return relevantEIandParentNames;
	}

	/**
	 * Find attribute values (direction) of relevant external interfaces.
	 *
	 * @param amlFile the aml file
	 * @param relevantExternalInterfacesAndItsParents the relevant external interfaces and its parents
	 * @return the hash map externalInterfaceIDtoDirectionAttribute:  key ID of external interface, col2 /value: direction
	 */
	public static  HashMap<String, String> findAttributeValuesOfExternalInterfaces (Document amlFile, HashMap<String, String> relevantExternalInterfacesAndItsParents) {
		HashMap<String, String> externalInterfaceIDtoDirectionAttribute = new HashMap<String, String>(); 
		NodeList directions = createNodeListByElementName(amlFile, "Value");
		for (int i = 0; i < directions.getLength(); i++) {
			//String relevantExternalInterface = relevantExternalInterfacesAndItsParents.get(directions.item(i).getParentNode().getParentNode().getAttributes().getNamedItem("ID").getNodeValue());

			// if statement to ensure, that only elements wihtin the instance hierarchy are extracted, with attribute  = .../Direction and relevant external interface
			if (directions.item(i).getParentNode().getParentNode().getParentNode().getParentNode().getNodeName().equals("InstanceHierarchy") == true
					&& directions.item(i).getParentNode().getParentNode().getParentNode().getNodeName().equals("InternalElement") == true
					&& directions.item(i).getParentNode().getParentNode().getNodeName().equals("ExternalInterface") == true
					&& directions.item(i).getParentNode().getAttributes().getNamedItem("RefAttributeType").getNodeValue().equals("AutomationMLBaseAttributeTypeLib/Direction") == true
					&& relevantExternalInterfacesAndItsParents.get(directions.item(i).getParentNode().getParentNode().getAttributes().getNamedItem("ID").getNodeValue())!=null					
					) {
				//				System.out.println(i + " " + relevantExternalInterfacesAndItsParents.get(directions.item(i).getParentNode().getParentNode().getAttributes().getNamedItem("ID").getNodeValue())+ " " + directions.item(i).getTextContent());
				//String internalElementIDtoDirection = relevantExternalInterfacesAndItsParents.get(directions.item(i).getParentNode().getParentNode().getAttributes().getNamedItem("ID").getNodeValue());
				//System.out.println(directions.item(i).getParentNode().getParentNode().getAttributes().getNamedItem("ID").getNodeValue().toString() + " " + directions.item(i).getTextContent());

				// key ID of external interface, col2 /value: direction
				externalInterfaceIDtoDirectionAttribute.put(directions.item(i).getParentNode().getParentNode().getAttributes().getNamedItem("ID").getNodeValue().toString(), directions.item(i).getTextContent());
			}
		}
		return externalInterfaceIDtoDirectionAttribute;
	}


	/**
	 * Find medium of internal element.
	 *
	 * @param amlFile the aml file
	 * @param relevantExternalInterfacesAndItsParents the relevant external interfaces and its parents
	 * @param internalLinks the internal links
	 * @return the hash map of externalinterface id and name of medium, currently works only for Products and NOT energy
	 */
	public static HashMap<String, String> findMediumOfInternalElement (Document amlFile, HashMap<String, String> relevantExternalInterfacesAndItsParents, HashMap<String, String> internalLinks) {
		HashMap<String, String> mediumOfInternalElement  = new HashMap<String, String>(); 
		NodeList allEI = createNodeListByElementName(amlFile, "ExternalInterface");
		String[][] eIandParents = new String[relevantExternalInterfacesAndItsParents.size()][2]; 
		int i = 0; 
		for (Entry<String, String> entry : relevantExternalInterfacesAndItsParents.entrySet()) {
			eIandParents[i][0] = entry.getKey();
			eIandParents[i][1] = entry.getValue();
			i++;
		}
		String medium; 
		int position; 
		for (int j = 0; j < allEI.getLength(); j++) {
			for (int k = 0; k < eIandParents.length; k++) {
				if (allEI.item(j).getAttributes().getNamedItem("ID").getNodeValue().equals(eIandParents[k][0])) {
					if (allEI.item(j).getParentNode().getAttributes().getNamedItem("RefBaseSystemUnitPath").getNodeValue().contains("Products") ||  allEI.item(j).getParentNode().getAttributes().getNamedItem("RefBaseSystemUnitPath").getNodeValue().contains("Energy")) {
						position = allEI.item(j).getParentNode().getAttributes().getNamedItem("RefBaseSystemUnitPath").getNodeValue().lastIndexOf('/'); 
						medium = allEI.item(j).getParentNode().getAttributes().getNamedItem("RefBaseSystemUnitPath").getNodeValue().substring(position+1);
						mediumOfInternalElement.put(eIandParents[k][0], medium);
						//						System.out.println(eIandParents[k][0]+" " + medium);
					}
				}
			}
		}
		return mediumOfInternalElement;
	}
	/**
	 * Find internal links to relevant external interfaces in HashMap relevantEI.
	 *
	 * @param internalLinks the internal links
	 * @param relevantEI the relevant Ext. Interfaces
	 * @param sideSwitch normal "AB", if side B as key, than "BA"
	 * @return the hash map
	 */
	public static HashMap<String, String> findInternalLinksToRelevantExternalInterfaces (NodeList internalLinks, HashMap<String, String> relevantEI, String sideSwitch) {
		HashMap<String, String> internalLinksToRelevantExternalInterfaces = new HashMap<String, String>(); 
		//		ArrayList <InternalLinks> intLinks = new ArrayList<InternalLinks>();
		//		InternalLinks individualIntLinks = new InternalLinks(); 

		String refPartnerSideA = ""; 
		String refPartnerSideB = ""; 
		for (int i = 0; i < internalLinks.getLength(); i++) {
			refPartnerSideA = internalLinks.item(i).getAttributes().getNamedItem("RefPartnerSideA").getNodeValue();
			refPartnerSideB = internalLinks.item(i).getAttributes().getNamedItem("RefPartnerSideB").getNodeValue();
			if (relevantEI.get(refPartnerSideA)!=null && relevantEI.get(refPartnerSideA)!=null) {
				//System.out.println(i + " " +  "A: " + relevantEI.get(refPartnerSideA) + " B: " + relevantEI.get(refPartnerSideB));
				//System.out.println(i + "A: " +refPartnerSideA + " B: " + refPartnerSideB);
				if (sideSwitch=="AB") {
					// relevantEI.get(refPA) --> internal element
					//internalLinksToRelevantExternalInterfaces.put(relevantEI.get(refPartnerSideA), relevantEI.get(refPartnerSideB));		
					internalLinksToRelevantExternalInterfaces.put(refPartnerSideA, refPartnerSideB);

				} else {
					internalLinksToRelevantExternalInterfaces.put(relevantEI.get(refPartnerSideB), relevantEI.get(refPartnerSideA));
				}
			}
		}
		//		System.out.println(internalLinksToRelevantExternalInterfaces);
		return internalLinksToRelevantExternalInterfaces;
	}

	/**
	 * Find names to ids corresponding to internal links by finding parents elements (internal elements) to external interfaces
	 *
	 * @param nodeList the node list
	 * @return the hash map
	 */
	public static HashMap<String, String> findNamesToIds (NodeList nodeList) {
		HashMap<String, String> namesToExternalInterfaces = new HashMap<>();
		String nameOfInternalElement = ""; 
		String idOfExternalInterface = ""; 
		for(int x=0; x<nodeList.getLength(); x++) {
			idOfExternalInterface = nodeList.item(x).getAttributes().getNamedItem("ID").getNodeValue();
			nameOfInternalElement = nodeList.item(x).getParentNode().getAttributes().getNamedItem("Name").getNodeValue();
			//	System.out.println(nameOfInternalElement + " " + idOfExternalInterface);

			namesToExternalInterfaces.put(idOfExternalInterface, nameOfInternalElement);
		}
		return namesToExternalInterfaces;
	}

	/**
	 * Find both sides of dependency.
	 *
	 * @param relevantExternalInterfaces the relevant external interfaces
	 * @param relevantInternalLinks the relevant internal links
	 * @param directionValueExternalInterfaceID the direction value external interface ID
	 * @param amlFile the aml file
	 * @return the hash map of dependecies, key = start, value = end
	 */
	public static List<Dependencies> findConnectedResources(HashMap<String, String> relevantExternalInterfaces, HashMap<String, String> relevantInternalLinks, HashMap<String, String> directionValueExternalInterfaceID, Document amlFile) {
		// if side B of d1 is Side A of d2 connect

		// connected internal elements through links of external interfaces
		// 0: intElementID, 1: ext Int. ID
		String[][] intElementsSideA = new String[relevantInternalLinks.size()][2];
		String[][] intElementsSideB = new String[relevantInternalLinks.size()][2];
		int i = 0; 
		for (Entry<String, String> entry : relevantInternalLinks.entrySet()) {
			intElementsSideA[i][0] = relevantExternalInterfaces.get(entry.getKey());
			intElementsSideA[i][1] = entry.getKey();
			intElementsSideB[i][0] = relevantExternalInterfaces.get(entry.getValue());
			intElementsSideB[i][1] = entry.getValue();
			i++;
		}
		// find connections of multiple links
		String depA = ""; 
		String depB = "";
		String depAExtIntID = "";
		String depBExtIntID = "";
		String directionA = ""; 
		String directionB = ""; 

		List<Dependencies> dependencyList = new ArrayList<>();
		//		find same IDs in both arrays, then "connect" other side and find its parent element id to identify connected resources

		// put in List dependencyList, if Direction=InOut, insert both connections
		for (int j = 0; j <relevantInternalLinks.size(); j++) {
			for(int k = 0; k <relevantInternalLinks.size(); k++) {
				if (intElementsSideA[j][0].equals(intElementsSideB[k][0]) && findResourceToProcess(amlFile).get(intElementsSideA[k][0])!=null) {
					depA = findResourceToProcess(amlFile).get(intElementsSideA[k][0]);
					depB = findResourceToProcess(amlFile).get(intElementsSideB[j][0]);
					depAExtIntID =  intElementsSideA[k][1];
					depBExtIntID =  intElementsSideB[j][1];
					directionA = directionValueExternalInterfaceID.get(depAExtIntID);
					directionB = directionValueExternalInterfaceID.get(depBExtIntID);
					if (directionA.equals("In") && directionB.equals("Out")) {
						Dependencies startAndEnd = new Dependencies(); 
						startAndEnd.setStartResource(depB);
						startAndEnd.setEndResource(depA);
						startAndEnd.setType(getExternalInterfacesAndDependencyType().get(intElementsSideA[j][1]));
						dependencyList.add(startAndEnd);
					} else if (directionA.equals("Out") && directionB.equals("In")) {
						Dependencies startAndEnd = new Dependencies(); 
						startAndEnd.setStartResource(depA);
						startAndEnd.setEndResource(depB);
						startAndEnd.setType(getInternalElementsAndDependencyType().get(intElementsSideA[j][0]));
						dependencyList.add(startAndEnd);
					} else {
						// Case if in/out
						Dependencies startAndEnd = new Dependencies(); 
						startAndEnd.setStartResource(depA);
						startAndEnd.setEndResource(depB);
						startAndEnd.setType(getInternalElementsAndDependencyType().get(intElementsSideA[j][0]));
						dependencyList.add(startAndEnd);

						startAndEnd.setStartResource(depB);
						startAndEnd.setEndResource(depA);
						startAndEnd.setType(getInternalElementsAndDependencyType().get(intElementsSideA[j][0]));
						dependencyList.add(startAndEnd);
					}
				}
			}
		}
//		System.out.println(dependencyList.size());
//		for (int j = 0; j < dependencyList.size(); j++) {
//			System.out.println(dependencyList.get(j).getStartResource() + dependencyList.get(j).getEndResource() + dependencyList.get(j).getType());
//		}
		return dependencyList;
	}

	public static List<Dependencies> findConnectedResourcesAndMedium (HashMap<String, String> relevantExternalInterfaces, HashMap<String, String> relevantInternalLinks, HashMap<String, String> directionValueExternalInterfaceID, HashMap<String, String> mediumOfExternalInterface,  Document amlFile) {
		// if side B of d1 is Side A of d2 connect

		// connected internal elements through links of external interfaces
		// 0: intElementID, 1: ext Int. ID
		String[][] intElementsSideA = new String[relevantInternalLinks.size()][2];
		String[][] intElementsSideB = new String[relevantInternalLinks.size()][2];
		int i = 0; 
		for (Entry<String, String> entry : relevantInternalLinks.entrySet()) {
			intElementsSideA[i][0] = relevantExternalInterfaces.get(entry.getKey());
			intElementsSideA[i][1] = entry.getKey();
			intElementsSideB[i][0] = relevantExternalInterfaces.get(entry.getValue());
			intElementsSideB[i][1] = entry.getValue();
			i++;
		}
		// find connections of multiple links
		String depA = ""; 
		String depB = "";
		String depAExtIntID = "";
		String depBExtIntID = "";
		String directionA = ""; 
		String directionB = ""; 
		String medium = ""; 

		List<Dependencies> dependencyList = new ArrayList<>();
		//		find same IDs in both arrays, then "connect" other side and find its parent element id to identify connected resources

		// put in HashMap dependencies, check direction, key = start, value = end, of Direction=InOut, insert both connections
		for (int j = 0; j <relevantInternalLinks.size(); j++) {
			for(int k = 0; k <relevantInternalLinks.size(); k++) {
				if (intElementsSideA[j][0].equals(intElementsSideB[k][0]) && findResourceToProcess(amlFile).get(intElementsSideA[k][0])!=null) {
					depA = findResourceToProcess(amlFile).get(intElementsSideA[k][0]);
					depB = findResourceToProcess(amlFile).get(intElementsSideB[j][0]);
					depAExtIntID =  intElementsSideA[k][1];
					depBExtIntID =  intElementsSideB[j][1];
					medium = mediumOfExternalInterface.get(intElementsSideA[j][1]);
					//				System.out.println(medium);
					directionA = directionValueExternalInterfaceID.get(depAExtIntID);
					directionB = directionValueExternalInterfaceID.get(depBExtIntID);
					//					System.out.println(depA + " " + directionA + " " + depB + " " + directionB);
					if (directionA.equals("In") && directionB.equals("Out")) {
						Dependencies startAndEnd = new Dependencies(); 
						startAndEnd.setStartResource(depB);
						startAndEnd.setEndResource(depA);
						startAndEnd.setMedium(medium);
						startAndEnd.setType(getInternalElementsAndDependencyType().get(intElementsSideA[j][0]));
						dependencyList.add(startAndEnd);
					} else if (directionA.equals("Out") && directionB.equals("In")) {
						Dependencies startAndEnd = new Dependencies(); 
						startAndEnd.setStartResource(depA);
						startAndEnd.setEndResource(depB);
						startAndEnd.setMedium(medium);
						startAndEnd.setType(getInternalElementsAndDependencyType().get(intElementsSideA[j][0]));
						dependencyList.add(startAndEnd);
					} else {
						// Case if in/out
						Dependencies startAndEnd = new Dependencies(); 
						startAndEnd.setStartResource(depA);
						startAndEnd.setEndResource(depB);
						startAndEnd.setMedium(medium);
						startAndEnd.setType(getInternalElementsAndDependencyType().get(intElementsSideA[j][0]));
						dependencyList.add(startAndEnd);

						startAndEnd.setStartResource(depB);
						startAndEnd.setEndResource(depA);
						startAndEnd.setMedium(medium);
						startAndEnd.setType(getInternalElementsAndDependencyType().get(intElementsSideA[j][0]));
						dependencyList.add(startAndEnd);
					}
				}
			}
		}
		//		System.out.println(dependencyList.size());
		//		for (int j = 0; j < dependencyList.size(); j++) {
		//			System.out.println(dependencyList.get(j).getStartResource() + dependencyList.get(j).getEndResource() + dependencyList.get(j).getMedium() + dependencyList.get(j).getType());
		//		}
		return dependencyList;
	}



	/**
	 * Find resource name to process id, if Resource and process internal elements are linked with internal links by external interfaces "ProcessResource"
	 *
	 * @param amlFile the aml file
	 * @return the hash map, key: process Id, value: Resource name
	 */
	public static  HashMap<String, String>  findResourceToProcess(Document amlFile) {
		//  internalElement RefBaseSystemUnitPath contains Process
		// internal Element B  RefBaseSystemUnitPath contains Resource
		// and A and B are connected by internal Link 
		NodeList internalElements = 	createNodeListByElementName(amlFile, "InternalElement");
		List<String> processes = new ArrayList<>(); 
		HashMap<String, String> processIDprocessName = new HashMap<String, String>(); 
		String process = "";
		String processID = "";

		List<String> Resources = new ArrayList<>(); 
		HashMap<String, String> ResourceIDtoResourceName = new HashMap<String, String>(); 
		String Resource = "";
		String ResourceID = "";

		for (int i = 0; i < internalElements.getLength(); i++) {
			if  (internalElements.item(i).getAttributes().getNamedItem("RefBaseSystemUnitPath").getNodeValue().contains("Process")) {
				process = internalElements.item(i).getAttributes().getNamedItem("Name").getNodeValue();
				processID = internalElements.item(i).getAttributes().getNamedItem("ID").getNodeValue();
				//				System.out.println("Process " + i+  " "  + process + " " + processID);
				processes.add(processID);
				processIDprocessName.put(processID, process);
			}
			if  (internalElements.item(i).getAttributes().getNamedItem("RefBaseSystemUnitPath").getNodeValue().contains("Resource")) {
				Resource = internalElements.item(i).getAttributes().getNamedItem("Name").getNodeValue();
				ResourceID = internalElements.item(i).getAttributes().getNamedItem("ID").getNodeValue();
				//				System.out.println("Resource " + i+  " "  + Resource + " " + ResourceID);
				Resources.add(ResourceID);
				ResourceIDtoResourceName.put(ResourceID, Resource);
			}
		}

		//		System.out.println(Resources.size());
		// id of external interface, id of corresponding internal element
		HashMap<String, String> connectionsOfProcessResources = findRelevantExternalInterfacesAndItsParents(createNodeListByElementName(amlFile, "ExternalInterface"),"RefBaseClassPath", "ProcessResource");
		HashMap<String, String> relevantInternalLinks = findInternalLinksToRelevantExternalInterfaces(createNodeListByElementName(amlFile, "InternalLink"), connectionsOfProcessResources, "BA");
		//System.out.println(relevantInternalLinks.size());
		HashMap<String, String> ResourceNametoProcessID = new HashMap<String, String>(); 
		String ResourceIDToProcessID = ""; 
		String ResourceName = ""; 
		for (int i = 0; i < processes.size(); i++) {
			ResourceIDToProcessID = relevantInternalLinks.get(processes.get(i));
			ResourceName = ResourceIDtoResourceName.get(ResourceIDToProcessID);
			//System.out.println(ResourceName+ " " + ResourceIDToProcessID);
			//			System.out.println(processes.get(i) + " " + ResourceName);
			ResourceNametoProcessID.put(processes.get(i), ResourceName);
		}
		return ResourceNametoProcessID;
	}

	public static NodeList createNodeListByElementName (Document fileName, String elementName) {
		return fileName.getElementsByTagName(elementName); 
	}

	public static boolean findUsingLoop(String search, List<String> list) {
		boolean matches = false;

		for(String str: list) {
			if (str.contains(search)) {
				matches = true; 
			}
			else 
				matches = false; 
		}
		return matches;
	}



	public static HashMap<String, String> getAttributeValuesOfExternalInterfaceIds() {
		return attributeValuesOfExternalInterfaceIds;
	}

	public void setAttributeValuesOfExternalInterfaceIds(HashMap<String, String> attributeValuesOfExternalInterfaceIds) {
		DependencyExtraction.attributeValuesOfExternalInterfaceIds = attributeValuesOfExternalInterfaceIds;
	}

	/**
	 * @return the relevantExternalInterfacesAndItsParents
	 */
	public static HashMap<String, String> getRelevantExternalInterfacesAndItsParents() {
		return relevantExternalInterfacesAndItsParents;
	}

	/**
	 * @param relevantExternalInterfacesAndItsParents the relevantExternalInterfacesAndItsParents to set
	 */
	public static void setRelevantExternalInterfacesAndItsParents(
			HashMap<String, String> relevantExternalInterfacesAndItsParents) {
		DependencyExtraction.relevantExternalInterfacesAndItsParents = relevantExternalInterfacesAndItsParents;
	}

	/**
	 * @return the relevantInternalLinks
	 */
	public static HashMap<String, String> getRelevantInternalLinks() {
		return relevantInternalLinks;
	}

	/**
	 * @param relevantInternalLinks the relevantInternalLinks to set
	 */
	public static void setRelevantInternalLinks(HashMap<String, String> relevantInternalLinks) {
		DependencyExtraction.relevantInternalLinks = relevantInternalLinks;
	}

	/**
	 * @return the directionValueExternalInterfaceID
	 */
	public static HashMap<String, String> getDirectionValueExternalInterfaceID() {
		return directionValueExternalInterfaceID;
	}

	/**
	 * @param directionValueExternalInterfaceID the directionValueExternalInterfaceID to set
	 */
	public static void setDirectionValueExternalInterfaceID(HashMap<String, String> directionValueExternalInterfaceID) {
		DependencyExtraction.directionValueExternalInterfaceID = directionValueExternalInterfaceID;
	}

	/**
	 * @return the listOfResources
	 */
	public static List<String> getListOfResources() {
		return listOfResources;
	}

	/**
	 * @param listOfResources the listOfResources to set
	 */
	public static void setListOfResources(List<String> listOfResources) {
		DependencyExtraction.listOfResources = listOfResources;
	}

	/**
	 * @return the listofDependenciesAndItsDirection
	 */
	public static List<Dependencies> getListofDependenciesAndTheirDirection() {
		return listofDependenciesAndTheirDirection;
	}

	/**
	 * @param listofDependenciesAndItsDirection the listofDependenciesAndItsDirection to set
	 */
	public static void setListofDependenciesAndTheirDirection(List<Dependencies> listofDependenciesAndItsDirection) {
		DependencyExtraction.listofDependenciesAndTheirDirection = listofDependenciesAndItsDirection;
	}

	/**
	 * @return the listofEnergyFlowsAndTheirDirection
	 */
	public static List<Dependencies> getListOfDependenciesWithEnergyCarriers() {
		return listOfDependenciesWithEnergyCarriers;
	}

	/**
	 * @param listofEnergyFlowsAndTheirDirection the listofEnergyFlowsAndTheirDirection to set
	 */
	public static void setListOfDependenciesWithEnergyCarriers(List<Dependencies> listofEnergyFlowsAndTheirDirection) {
		DependencyExtraction.listOfDependenciesWithEnergyCarriers = listofEnergyFlowsAndTheirDirection;
	}

	/**
	 * @return the internalElementsAndDependencyType
	 */
	public static HashMap<String, String> getInternalElementsAndDependencyType() {
		return internalElementsAndDependencyType;
	}

	/**
	 * @param internalElementsAndDependencyType the internalElementsAndDependencyType to set
	 */
	public static void setInternalElementsAndDependencyType(HashMap<String, String> internalElementsAndDependencyType) {
		DependencyExtraction.internalElementsAndDependencyType = internalElementsAndDependencyType;
	}

	/**
	 * @return the externalInterfacesAndDependencyType
	 */
	public static HashMap<String, String> getExternalInterfacesAndDependencyType() {
		return externalInterfacesAndDependencyType;
	}

	/**
	 * @param externalInterfacesAndDependencyType the externalInterfacesAndDependencyType to set
	 */
	public static void setExternalInterfacesAndDependencyType(HashMap<String, String> externalInterfacesAndDependencyType) {
		DependencyExtraction.externalInterfacesAndDependencyType = externalInterfacesAndDependencyType;
	}
}
