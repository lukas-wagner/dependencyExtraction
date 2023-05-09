# dependencyExtraction
Transformation von Abhängigkeitsbeziehungen aus einem Topologiemodell in ein Optimierungsmodell


Repository zum Beitrag

L. Wagner, M. Ramonat, A. Fay: Methode zur Berücksichtigung von Abhängigkeiten energetisch flexibler Anlagen aus Topologiemodellen in Optimierungsmodellen. VDI-Kongress Automation 2023, Baden-Baden, 27.-28.06.2023.

Extraktion von Abhängigkeiten aus einem in AutomationML nach der formalisierten Prozessbeschreibung modelliertem Topologiemodell in Nebenbedingungen einen MILP-Optimierungsmodells. 



_____________________

Liste extrahierter Abhängigkeitsverbindungen aus dem AutomationML-Modell: 

ElectricityTransformator	Electrolyzer	Electricity	CorrelativeDependency

ElectricityTransformator	WaterPurifier0	Electricity	RestrictiveDependency

ElectricityTransformator	WaterPurifier1	Electricity	RestrictiveDependency

WaterPurifier1	Electrolyzer	Water	CorrelativeDependency

WaterPurifier0	Electrolyzer	Water	CorrelativeDependency

Electrolyzer	Compressor	Hydrogen	CorrelativeDependency

Compressor	StorageTank	Hydrogen	CorrelativeDependency

ElectricityTransformator	Compressor	Electricity	CorrelativeDependency

Electrolyzer	StorageTank		CorrelativeDependency

StorageTank	Electrolyzer		CorrelativeDependency