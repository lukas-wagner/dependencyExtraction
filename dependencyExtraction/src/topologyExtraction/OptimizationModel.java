package topologyExtraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.RestoreAction;

import ilog.concert.IloColumn;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

/**
 * The Class OptimizationModel.
 * @author Lukas Wagner - Helmut-Schmidt-Universität	
 */
public class OptimizationModel {

	static List<Dependencies> listOfDependencies = new ArrayList<>(); 
	static List<Dependencies> listOfDependenciesWithEnergyCarriers = new ArrayList<>(); 
	static List<String> listOfAllResources = new ArrayList<>(); 

	private static final String INPUT = "Input";
	private static final String OUTPUT = "Output";
	private static final String RESTRICTIVE = "RestrictiveDependency";
	private static final String POWER = "Electricity";
	private static final String WATER = "Water";
	private static final String HYDROGEN = "Hydrogen";
	private static final String DEFAULT_MEDIUM = "Hydrogen";


	static HashMap<String, IloNumVar[]> decisionVariables = new HashMap<>(); 
	static List<OptimizationResults> optimizationResults = new ArrayList<>(); 


	public static void main(String[] args) throws Exception {
		String filePath = "./src/topologyExtraction/Elektrolyse_Prozess_v2.aml";
		@SuppressWarnings("unused")
		DependencyExtraction dependencies = new DependencyExtraction(filePath);

		getListOfDependencies().addAll(DependencyExtraction.getListofDependenciesAndTheirDirection());
//		for (int i = 0; i < getListOfDependencies().size(); i++) {
//			System.out.println("Dependency" + i + " " + getListOfDependencies().get(i).getStartResource() + " " + getListOfDependencies().get(i).getEndResource() + " " + getListOfDependencies().get(i).getType());
//		}

		getListOfDependenciesWithEnergyCarriers().addAll(DependencyExtraction.getListOfDependenciesWithEnergyCarriers());
//		for (int i = 0; i < getListOfDependenciesWithEnergyCarriers().size(); i++) {
//			System.out.println("Dependency with Energy" + i + " " + getListOfDependenciesWithEnergyCarriers().get(i).getStartResource() + " " + getListOfDependenciesWithEnergyCarriers().get(i).getEndResource() + " " + getListOfDependenciesWithEnergyCarriers().get(i).getMedium() + " " + getListOfDependencies().get(i).getType());
//		}

		getListOfAllResources().addAll(DependencyExtraction.getListOfResources());
		//		for (int i = 0; i < getListOfAllResources().size(); i++) {
		//			System.out.println("Resource"+ i+ " "  + getListOfAllResources().get(i));
		//		}
		optimizationModel();
	}


	public static void optimizationModel () {
		IloCplex cplex = null; 

		try {
			cplex = new IloCplex();
			// ------ general optimization parameters -------
			double timeIntervalInHours = 0.25;

			int arrayLength = 1;
			double[] electricityPrice = getElectricityPrice(arrayLength);

			// basic model from WRS+22, extended by peripherals, storage, compressor etc. 

			//-------- Transformator -----------------------
			double minPowerTransformator, maxPowerTransformator, efficiencyTransformator; 
			minPowerTransformator = 0;
			efficiencyTransformator = 0.98; 

			//-------- Water purifier -----------------------
			double minPowerWaterPurifier, maxPowerWaterPurifier, efficiencyWaterPurifier, waterCost; 
			minPowerWaterPurifier = 0; 
			maxPowerWaterPurifier = 6000; 
			efficiencyWaterPurifier = .85;
			waterCost = 2; 

			//-------- Electrolyzer -----------------------
			double minOperationPoint = 0.2; 
			double maxOperationPoint = 1;
			double electrolyzerLinearizationConstant = 226.92813;
			double electrolyzerLinearizationSlope = 0.57622;
			double maxPowerElectrolyzer, minPowerElectrolyzer, maxRatedPowerElectrolyzer; 
			maxRatedPowerElectrolyzer = 6000;
			maxPowerElectrolyzer = maxOperationPoint*maxRatedPowerElectrolyzer;
			minPowerElectrolyzer = minOperationPoint*maxRatedPowerElectrolyzer;
			double minWaterEl, maxWaterEl, waterConsumptionByHydrogen;
			minWaterEl = 0; 
			maxWaterEl = 12000; 
			waterConsumptionByHydrogen = 2; 

			//-------- Compressor -----------------------
			double minPowerCompressor, maxPowerCompressor, efficiencyCompressor; 
			minPowerCompressor = 0; 
			maxPowerCompressor = 60000; 
			efficiencyCompressor = .9; 

			//-------- Storage -----------------------
			double initialSOC,  minLevelStorage, maxCapacityOfStorage, minPowerStorage, maxPowerStorage, efficiencyStorageInput, efficiencyStorageOutput; 
			initialSOC=2000; // kWh
			minLevelStorage = 0; 
			maxCapacityOfStorage=500000; // kWh
			minPowerStorage = 0; 
			maxPowerStorage = 2500; 
			efficiencyStorageInput = 1; 
			efficiencyStorageOutput = 1; 
			double recpOfEfficiencyStorageOutput = 1/efficiencyStorageOutput;
			double constantHydrogenOutputPower = 2500;

			maxPowerTransformator = 2*maxPowerWaterPurifier+ maxPowerElectrolyzer+maxPowerCompressor;

			//// --------------OPTIMIZATION PROBLEM ----------------

			// -- Decision Variables --- 
			IloNumVar[] powerInputArrayTransformator = cplex.numVarArray(arrayLength, minPowerTransformator, maxPowerTransformator);

			getDecisionVariables().put("ElectricityTransformator"+"-"+INPUT+"-"+POWER, powerInputArrayTransformator);
			IloNumVar[] powerOutPutArrayTransformator = cplex.numVarArray(arrayLength, minPowerTransformator*efficiencyTransformator, maxPowerTransformator*efficiencyTransformator);
			getDecisionVariables().put("ElectricityTransformator"+"-"+OUTPUT+"-"+POWER, powerOutPutArrayTransformator);

			IloNumVar[] powerInputElectrolyzer =  cplex.numVarArray(arrayLength, minPowerElectrolyzer, maxPowerElectrolyzer);
			getDecisionVariables().put("Electrolyzer"+"-"+INPUT+"-"+POWER, powerInputElectrolyzer);
			IloNumVar[] waterInputElectrolyzer =  cplex.numVarArray(arrayLength, minWaterEl, maxWaterEl);
			getDecisionVariables().put("Electrolyzer"+"-"+INPUT+"-"+WATER, waterInputElectrolyzer);
			IloNumVar[] hydrogenOutputElectrolyzer =  cplex.numVarArray(arrayLength, minPowerElectrolyzer, maxPowerElectrolyzer);
			getDecisionVariables().put("Electrolyzer"+"-"+OUTPUT+"-"+HYDROGEN, hydrogenOutputElectrolyzer);

			IloNumVar[] waterInputWaterPurifier0 =  cplex.numVarArray(arrayLength, minPowerWaterPurifier, maxPowerWaterPurifier);
			getDecisionVariables().put("WaterPurifier0"+"-"+INPUT+"-"+WATER, waterInputWaterPurifier0);
			IloNumVar[] powerInputWaterPurifier0 =  cplex.numVarArray(arrayLength, minPowerWaterPurifier, maxPowerWaterPurifier);
			getDecisionVariables().put("WaterPurifier0"+"-"+INPUT+"-"+POWER, powerInputWaterPurifier0);
			IloNumVar[] waterOutputWaterPurifier0 =  cplex.numVarArray(arrayLength, minPowerWaterPurifier*efficiencyWaterPurifier, maxPowerWaterPurifier*efficiencyWaterPurifier);
			getDecisionVariables().put("WaterPurifier0"+"-"+OUTPUT+"-"+WATER, waterOutputWaterPurifier0);

			IloNumVar[] waterInputWaterPurifier1 =  cplex.numVarArray(arrayLength, minPowerWaterPurifier, maxPowerWaterPurifier);
			getDecisionVariables().put("WaterPurifier1"+"-"+INPUT+"-"+WATER, waterInputWaterPurifier1);
			IloNumVar[] powerInputWaterPurifier1 =  cplex.numVarArray(arrayLength, minPowerWaterPurifier, maxPowerWaterPurifier);
			getDecisionVariables().put("WaterPurifier1"+"-"+INPUT+"-"+POWER, powerInputWaterPurifier1);
			IloNumVar[] waterOutputWaterPurifier1 =  cplex.numVarArray(arrayLength, minPowerWaterPurifier*efficiencyWaterPurifier, maxPowerWaterPurifier*efficiencyWaterPurifier);
			getDecisionVariables().put("WaterPurifier1"+"-"+OUTPUT+"-"+WATER, waterOutputWaterPurifier1);

			IloNumVar[] hydrogenInputCompressor =  cplex.numVarArray(arrayLength, minPowerCompressor, maxPowerCompressor);
			getDecisionVariables().put("Compressor"+"-"+INPUT+"-"+HYDROGEN, hydrogenInputCompressor);
			IloNumVar[] powerInputCompressor =  cplex.numVarArray(arrayLength, minPowerCompressor, maxPowerCompressor);
			getDecisionVariables().put("Compressor"+"-"+INPUT+"-"+POWER, powerInputCompressor);
			IloNumVar[] hydrogenOutputCompressor =  cplex.numVarArray(arrayLength, minPowerCompressor*efficiencyCompressor, maxPowerCompressor*efficiencyCompressor);
			getDecisionVariables().put("Compressor"+"-"+OUTPUT+"-"+HYDROGEN, hydrogenOutputCompressor);

			IloNumVar[] hydrogenInputStorage =  cplex.numVarArray(arrayLength+1, minLevelStorage, maxCapacityOfStorage);
			getDecisionVariables().put("StorageTank"+"-"+INPUT+"-"+HYDROGEN, hydrogenInputStorage);
			IloNumVar[] hydrogenOutputStorage =  cplex.numVarArray(arrayLength, minPowerStorage, maxPowerStorage);
			getDecisionVariables().put("StorageTank"+"-"+OUTPUT+"-"+HYDROGEN, hydrogenOutputStorage);

			IloNumVar[] storageLevel =  cplex.numVarArray(arrayLength+1, minLevelStorage, maxCapacityOfStorage);

			// Systeme an sich -> Output = Input*eta / Output = const + Input*a

			for (int i = 0; i < arrayLength; i++) {
				cplex.addEq(getOutput("ElectricityTransformator", POWER)[i], 
						cplex.prod(getInput("ElectricityTransformator", POWER)[i],efficiencyTransformator));

				cplex.addEq(getOutput("WaterPurifier0", WATER)[i], 
						cplex.prod(getInput("WaterPurifier0", POWER)[i],efficiencyWaterPurifier));

				cplex.addEq(getOutput("WaterPurifier0", WATER)[i], 
						cplex.prod(getInput("WaterPurifier0", WATER)[i],efficiencyWaterPurifier));

				cplex.addEq(getOutput("WaterPurifier1", WATER)[i], 
						cplex.prod(getInput("WaterPurifier1", POWER)[i],efficiencyWaterPurifier));

				cplex.addEq(getOutput("WaterPurifier1", WATER)[i], 
						cplex.prod(getInput("WaterPurifier1", WATER)[i],efficiencyWaterPurifier));

				cplex.addEq(getOutput("Electrolyzer", HYDROGEN)[i], 
						cplex.sum(
								cplex.prod(getInput("Electrolyzer", POWER)[i],electrolyzerLinearizationSlope), 
								electrolyzerLinearizationConstant));

				cplex.addEq(getOutput("Electrolyzer", HYDROGEN)[i], 
						cplex.prod(getInput("Electrolyzer", WATER)[i], waterConsumptionByHydrogen));

				cplex.addEq(getOutput("Compressor", HYDROGEN)[i], 
						getInput("Compressor", HYDROGEN)[i]);

				cplex.addEq(getOutput("StorageTank", HYDROGEN)[i], constantHydrogenOutputPower);
			}


			//			for (int i = 0; i < arrayLength; i++) {
			//				cplex.addEq(getOutput("WaterPurifier",  WATER)[i], getInput("Electrolyzer", WATER)[i]);
			//				cplex.addEq(getOutput("Electrolyzer",  HYDROGEN)[i], getInput("Compressor", HYDROGEN)[i]);
			//				cplex.addEq(getOutput("Compressor",  HYDROGEN)[i], getInput("StorageTank", HYDROGEN)[i]);
			//			}

			cplex.addEq(storageLevel[0], initialSOC);			
			cplex.addEq(storageLevel[arrayLength], initialSOC);			

			for (int i = 1; i < arrayLength+1; i++) {
				cplex.addEq(storageLevel[i], 
						cplex.sum(storageLevel[i-1],
								(cplex.diff(
										cplex.prod(getInput("StorageTank",HYDROGEN)[i-1],timeIntervalInHours*efficiencyStorageInput), 
										cplex.prod(getOutput("StorageTank",HYDROGEN)[i-1],timeIntervalInHours*recpOfEfficiencyStorageOutput)
										)
										)
								)
						);
			}
			// Constraints --- automatically extracted from AML
						List<Dependencies> listOfAllDependenciesAndEnergyFlow = listForTest();
//			List<Dependencies> listOfAllDependenciesAndEnergyFlow = getListOfDependenciesWithEnergyCarriers();
			//List<Dependencies> listOfAllDependenciesAndEnergyFlow = createListOfAllDependenciesToBeUsedAsConstraints();
			String output; 
			String input; 
			String output1; 
			String output2; 
			String input1; 
			String input2; 
			String medium; 


			// TODO move to method
			// xor
			// restrictive dependencies
			// ------- restrictive dependencies ----- 
			// find dependencies which connect to the same start resource, output = input1 + input2, 
			// and create constraints
			//then remove those from list
			IloIntVar[] binaryPartner1 = cplex.intVarArray(arrayLength, 0, 1);
			IloIntVar[] binaryPartner2 = cplex.intVarArray(arrayLength, 0, 1);

			for (int j = 0; j < listOfAllDependenciesAndEnergyFlow.size(); j++) {
				for (int k = 0; k < listOfAllDependenciesAndEnergyFlow.size(); k++) {
					if (listOfAllDependenciesAndEnergyFlow.get(j).getType() == null) {

					} else {
						if (k != j
								&& listOfAllDependenciesAndEnergyFlow.get(j).getType().equals(RESTRICTIVE)
								&& listOfAllDependenciesAndEnergyFlow.get(k).getStartResource().equals(listOfAllDependenciesAndEnergyFlow.get(j).getStartResource())
								&& listOfAllDependenciesAndEnergyFlow.get(k).getMedium().equals(listOfAllDependenciesAndEnergyFlow.get(j).getMedium())
								) {
							output = listOfAllDependenciesAndEnergyFlow.get(k).getStartResource();
							input1 = listOfAllDependenciesAndEnergyFlow.get(k).getEndResource();
							input2 = listOfAllDependenciesAndEnergyFlow.get(j).getEndResource();
							medium = listOfAllDependenciesAndEnergyFlow.get(k).getMedium();

							listOfAllDependenciesAndEnergyFlow.remove(j);
							listOfAllDependenciesAndEnergyFlow.remove(k);

							// logical constraints for restrictive dependencies, if partner1 == 0, output = input 2 && input1 == 0
							for (int i = 0; i < arrayLength; i++) {
								cplex.addEq(cplex.sum(binaryPartner2[i], binaryPartner1[i]), 1);
								cplex.add(cplex.ifThen(cplex.eq(binaryPartner1[i], 0), cplex.eq(getOutput(output, medium)[i], getInput(input2, medium)[i])));
								cplex.add(cplex.ifThen(cplex.eq(binaryPartner1[i], 0), cplex.eq(0, getInput(input1, medium)[i])));
								cplex.add(cplex.ifThen(cplex.eq(binaryPartner1[i], 1), cplex.eq(getOutput(output, medium)[i], getInput(input1, medium)[i])));
								cplex.add(cplex.ifThen(cplex.eq(binaryPartner1[i], 1), cplex.eq(0, getInput(input2, medium)[i])));
							}
						} else if (k != j
								&& listOfAllDependenciesAndEnergyFlow.get(j).getType().equals(RESTRICTIVE)
								&& listOfAllDependenciesAndEnergyFlow.get(k).getEndResource().equals(listOfAllDependenciesAndEnergyFlow.get(j).getEndResource())
								&& listOfAllDependenciesAndEnergyFlow.get(k).getMedium().equals(listOfAllDependenciesAndEnergyFlow.get(j).getMedium())
								)  {
							System.out.println(" RESTRICTIVE Combined Start - Start - End: " + listOfAllDependenciesAndEnergyFlow.get(k).getEndResource()+ " " + listOfAllDependenciesAndEnergyFlow.get(k).getStartResource() + " " + listOfAllDependenciesAndEnergyFlow.get(j).getStartResource());
							output1 = listOfAllDependenciesAndEnergyFlow.get(k).getStartResource();
							output2 = listOfAllDependenciesAndEnergyFlow.get(j).getStartResource();
							input = listOfAllDependenciesAndEnergyFlow.get(j).getEndResource();
							medium = listOfAllDependenciesAndEnergyFlow.get(k).getMedium();

							listOfAllDependenciesAndEnergyFlow.remove(k);
							listOfAllDependenciesAndEnergyFlow.remove(j);

							// logical constraints for restrictive dependencies, if partner1 == 0, output = input 2 && input1 == 0
							for (int i = 0; i < arrayLength; i++) {
								cplex.addEq(cplex.sum(binaryPartner2[i], binaryPartner1[i]), 1);
								cplex.ifThen(cplex.addEq(binaryPartner1[i], 0), cplex.addEq(getOutput(output2, medium)[i], getInput(input, medium)[i]));
								cplex.ifThen(cplex.addEq(binaryPartner1[i], 0), cplex.addEq(0, getOutput(output1, medium)[i]));
								cplex.ifThen(cplex.addEq(binaryPartner1[i], 1), cplex.addEq(getOutput(output1, medium)[i], getInput(input, medium)[i]));
								cplex.ifThen(cplex.addEq(binaryPartner1[i], 1), cplex.addEq(0, getOutput(output2, medium)[i]));
							}
						}
					}
				}
			}

			// TODO move to method
			// ------- correlative dependencies ----- 
			// find dependencies which connect to the same start resource, output = input1 + input2, 
			// add constraint
			//then remove those from list
			for (int j = 0; j < listOfAllDependenciesAndEnergyFlow.size(); j++) {
				for (int k = 0; k < listOfAllDependenciesAndEnergyFlow.size(); k++) {
					if (k != j
							&& listOfAllDependenciesAndEnergyFlow.get(k).getStartResource().equals(listOfAllDependenciesAndEnergyFlow.get(j).getStartResource())
							&& listOfAllDependenciesAndEnergyFlow.get(k).getMedium().equals(listOfAllDependenciesAndEnergyFlow.get(j).getMedium())
							) {
						System.out.println("Combined Start - End - End : " + listOfAllDependenciesAndEnergyFlow.get(k).getStartResource()+ " " + listOfAllDependenciesAndEnergyFlow.get(k).getEndResource() + " " + listOfAllDependenciesAndEnergyFlow.get(j).getEndResource());
						output = listOfAllDependenciesAndEnergyFlow.get(k).getStartResource();
						input1 = listOfAllDependenciesAndEnergyFlow.get(k).getEndResource();
						input2 = listOfAllDependenciesAndEnergyFlow.get(j).getEndResource();
						medium = listOfAllDependenciesAndEnergyFlow.get(k).getMedium();
						listOfAllDependenciesAndEnergyFlow.remove(k);
						listOfAllDependenciesAndEnergyFlow.remove(j);
						for (int i = 0; i < arrayLength; i++) {
							cplex.addEq(cplex.sum(getInput(input1, medium)[i], getInput(input2, medium)[i]),
									getOutput(output, medium)[i]);
						}
						// andere Richtung (EndResource == EndResource)
					} else if (k != j 
							&&  listOfAllDependenciesAndEnergyFlow.get(k).getEndResource().equals(listOfAllDependenciesAndEnergyFlow.get(j).getEndResource())
							&& listOfAllDependenciesAndEnergyFlow.get(k).getMedium().equals(listOfAllDependenciesAndEnergyFlow.get(j).getMedium())
							) {
						System.out.println("Combined Start - Start - End: " + listOfAllDependenciesAndEnergyFlow.get(k).getEndResource()+ " " + listOfAllDependenciesAndEnergyFlow.get(k).getStartResource() + " " + listOfAllDependenciesAndEnergyFlow.get(j).getStartResource());
						output1 = listOfAllDependenciesAndEnergyFlow.get(k).getStartResource();
						output2 = listOfAllDependenciesAndEnergyFlow.get(j).getStartResource();
						input = listOfAllDependenciesAndEnergyFlow.get(j).getEndResource();
						medium = listOfAllDependenciesAndEnergyFlow.get(k).getMedium();
						listOfAllDependenciesAndEnergyFlow.remove(k);
						listOfAllDependenciesAndEnergyFlow.remove(j);
						for (int i = 0; i < arrayLength; i++) {
							cplex.addEq(cplex.sum(getInput(output1, medium)[i], getInput(output2, medium)[i]),
									getOutput(input, medium)[i]);
						}
					}
				}
			}


			// all other dependencies
			for (int j = 0; j < listOfAllDependenciesAndEnergyFlow.size(); j++) {
				output = listOfAllDependenciesAndEnergyFlow.get(j).getStartResource();
				input = listOfAllDependenciesAndEnergyFlow.get(j).getEndResource();
				medium = listOfAllDependenciesAndEnergyFlow.get(j).getMedium();
				System.out.println("Single: " + output + " " + input + " " + medium);
				for (int i = 0; i < arrayLength; i++) {
					cplex.addEq(getInput(input, medium)[i], getOutput(output, medium)[i]);
				}
			}			

//			System.out.println(cplex);
//			cplex.exportModel("model.lp");
			IloLinearNumExpr objective = cplex.linearNumExpr();

			for (int i = 0; i < arrayLength; i++) {
				objective.addTerm(electricityPrice[i]*timeIntervalInHours*0.001, getInput("ElectricityTransformator", POWER)[i]);
				objective.addTerm(waterCost, getInput("WaterPurifier0", WATER)[i]);
				objective.addTerm(waterCost, getInput("WaterPurifier1", WATER)[i]);
			}
			cplex.addMinimize(objective);

			if (cplex.solve()) {
				System.out.println("obj = "+cplex.getObjValue());
			} else {
				System.out.println("Model not solved");
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (cplex!=null)  {
				cplex.close();
			}
		}

	}

	public static void createDecisionVariables () {


	}

	private static double[] getElectricityPrice (int i) {
		double[] electricityPrice = new double[] {
				85.87,37.21,32.62,32.27,51.03,63.76,44.84,39.2,58.28,43.46,42.47,47.48, 58.96,42.99,43.27,50.5,
				80.21,30.64,21.47,35.41,15.67,24.15,43.98,55.36,23.58,42.56,60.2,87.49,22.46,38.95,	31.68,60.19,
				17.49,37.23,40.51,57.98,46.62,46.74,61.23,71.33,57.2,55.6,59.39,81.35,48.79,58.3,58.01,57.73,71.85,
				64.5,57.07,57.84,44.25,38.6,65.02,67.76,42.31,62.06,83.36,100.31,57.25,66.51,84.52,95.44,73.57,84.5,
				85.41,98.72,75.05,98.16,101.62,96.88,127.03,92.24,102.1,79.11,140.98,93.1,91.67,66.07,125.87,89.96,
				76.41,48.12,100.5,74.96,74.03,64.77,100.75,75.26,87.21,38.04,85.35,60.38,56.9,35.95
		};
		double[] electricityPriceNew = new double[i];

		if (i>electricityPrice.length) {
			return electricityPrice;
		} else {
			for (int j = 0; j < i; j++) {
				electricityPriceNew[j] = electricityPrice[j];
			}
			return electricityPriceNew; 
		}
	}

	public static double[] convertPriceToArbitrayIntervals (double[] price, int newLengthInMinutes) {

		int arrayLengthOld = price.length;
		int multiplier = 15/newLengthInMinutes;
		int arrayLengthNew = multiplier*arrayLengthOld;
		double[] longPrice = new double[arrayLengthNew];

		int counter = 0; 
		for (int i = 0; i < arrayLengthOld; i++) {
			for (int k = 0; k < multiplier; k++) {
				longPrice[counter] = price[i];
				counter++; 
			}
		}
		return longPrice;
	}


	public static List<Dependencies> createListOfAllDependenciesToBeUsedAsConstraints () {
		List<Dependencies> list = new ArrayList<>();
		List<Dependencies> listDepAndEnergy = new ArrayList<>();
		List<Dependencies> listDependencyOnly = new ArrayList<>();

		// .------ listDepAndEnergy-----
		for (int i = 0; i < getListOfDependencies().size(); i++) {
			for (int j = 0; j < getListOfDependenciesWithEnergyCarriers().size(); j++) {
				// if both sides of dependencies in lists are the same, set in list
				if ((getListOfDependencies().get(i).getStartResource().equals(getListOfDependenciesWithEnergyCarriers().get(j).getStartResource()) 
						&& getListOfDependencies().get(i).getEndResource().equals(getListOfDependenciesWithEnergyCarriers().get(j).getEndResource()))	
						) {
					Dependencies dependenciesWithEnergy = new Dependencies(); 
					dependenciesWithEnergy.setStartResource(getListOfDependenciesWithEnergyCarriers().get(j).getStartResource());
					dependenciesWithEnergy.setEndResource(getListOfDependenciesWithEnergyCarriers().get(j).getEndResource());
					dependenciesWithEnergy.setMedium(getListOfDependenciesWithEnergyCarriers().get(j).getMedium());
					listDepAndEnergy.add(dependenciesWithEnergy);
				} 
			}
		}
		listDependencyOnly = getListOfDependencies();

		for (int i = 0; i < listDependencyOnly.size(); i++) {
			for (int j = 0; j < getListOfDependenciesWithEnergyCarriers().size(); j++) {
				if (listDependencyOnly.get(i).getStartResource().equals(getListOfDependenciesWithEnergyCarriers().get(j).getStartResource())
						&& listDependencyOnly.get(i).getEndResource().equals(getListOfDependenciesWithEnergyCarriers().get(j).getEndResource())) {
					listDependencyOnly.remove(i);
				}
			}
		}


		// TODO Default is no the best idea, find available medium and set to common
		for (int i = 0; i < listDependencyOnly.size(); i++) {
			listDependencyOnly.get(i).setMedium(DEFAULT_MEDIUM);
		}

		list.addAll(listDepAndEnergy);
		list.addAll(listDependencyOnly);

		return list; 
	}


	public static List<Dependencies> listForTest () {
		List<Dependencies> list = new ArrayList<>();
		String[] start = new String[] {"ElectricityTransformator", "Electrolyzer", "Compressor", "ElectricityTransformator"}; 
		String[] end = new String[] {"Electrolyzer", "Compressor", "StorageTank", "WaterPurifier0"}; 
		String[] medium = new String[] {POWER, HYDROGEN, HYDROGEN, POWER, POWER}; 
		String[] type = new String[] {"", "", "", " ", "", "", ""};

		for (int i = 0; i < start.length; i++) {
			Dependencies depe = new Dependencies(); 
			depe.setStartResource(start[i]);
			depe.setEndResource(end[i]);
			depe.setMedium(medium[i]);
			depe.setType(type[i]);
			list.add(depe);
		}
		return list; 
	}

	public static IloNumVar[] getInput(String name, String medium) {
		return getDecisionVariables().get(name+"-"+INPUT+"-"+medium);
	}

	public static IloNumVar[] getOutput(String name, String medium) {
		return getDecisionVariables().get(name+"-"+OUTPUT+"-"+medium);
	}
	/**
	 * @return the listOfDependencies
	 */
	public static List<Dependencies> getListOfDependencies() {
		return listOfDependencies;
	}

	/**
	 * @param listOfDependencies the listOfDependencies to set
	 */
	public void setListOfDependencies(List<Dependencies> listOfDependencies) {
		AutomaticModelGenerationV1.listOfDependencies = listOfDependencies;
	}

	/**
	 * @return the listOfAllResources
	 */
	public static List<String> getListOfAllResources() {
		return listOfAllResources;
	}

	/**
	 * @param listOfAllResources the listOfAllResources to set
	 */
	public void setListOfAllResources(List<String> listOfAllResources) {
		AutomaticModelGenerationV1.listOfAllResources = listOfAllResources;
	}

	/**
	 * @return the decisionVariables
	 */
	public static HashMap<String, IloNumVar[]> getDecisionVariables() {
		return decisionVariables;
	}

	/**
	 * @param decisionVariables the decisionVariables to set
	 */
	public static void setDecisionVariables(HashMap<String, IloNumVar[]> decisionVariables) {
		AutomaticModelGenerationV1.decisionVariables = decisionVariables;
	}

	/**
	 * @return the optimizationResults
	 */
	public static List<OptimizationResults> getOptimizationResults() {
		return optimizationResults;
	}

	/**
	 * @param optimizationResults the optimizationResults to set
	 */
	public static void setOptimizationResults(List<OptimizationResults> optimizationResults) {
		AutomaticModelGenerationV1.optimizationResults = optimizationResults;
	}

	/**
	 * @return the listOfDependenciesWithEnergyCarriers
	 */
	public static List<Dependencies> getListOfDependenciesWithEnergyCarriers() {
		return listOfDependenciesWithEnergyCarriers;
	}

	/**
	 * @param listOfDependenciesWithEnergyCarriers the listOfDependenciesWithEnergyCarriers to set
	 */
	public static void setListOfDependenciesWithEnergyCarriers(List<Dependencies> listOfDependenciesWithEnergyCarriers) {
		AutomaticModelGenerationV1.listOfDependenciesWithEnergyCarriers = listOfDependenciesWithEnergyCarriers;
	}

}