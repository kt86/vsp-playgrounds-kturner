package playground.wrashid.sschieffer.DecentralizedSmartCharger;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;


public class VehicleType {
	
	private String name;
	Battery myBattery;
	private GasType myGasType;
	private Vehicle referenceObject;
	private double engineWatt;
	
	public VehicleType(String name,
			Battery myBattery,
			GasType myGasType, 
			Vehicle referenceObject,
			double engineWatt){
		
		this.myBattery=myBattery;
		this.myGasType=myGasType;
		this.referenceObject=referenceObject;
		this.name=name;
		this.engineWatt=engineWatt;
	}
	
	
	public Battery getBattery(){
		return myBattery;
	}
	
	public GasType getGasType(){
		return myGasType;
	}
	
	public double getWattOfEngine(){
		return engineWatt;
	}
	
	public String getName(){
		return name;
	}
	
	public Class getVehicleClassType(){
		return referenceObject.getClass();
	}
	
}
