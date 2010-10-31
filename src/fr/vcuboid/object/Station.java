package fr.vcuboid.object;

/**
 * Model class which will store the Station Items
 * 
 * @author Guillaume Delente
 *
 */

public class Station {
	private int id;
	private String network;
	private String name;
	private String address;
	private double longitude;
	private double latitude;
	private int availableBikes;
	private int freeSlots;
	boolean isOpen;

	public Station(int id, String network, String name, String address, double longitude, double latitude, int availablesBikes, int freeLocations, boolean isOpen) {
		this.id = id;
		this.network = network;
		this.address = address;
		this.name = name;
		this.longitude = longitude;
		this.latitude = latitude;
		this.availableBikes = availablesBikes;
		this.freeSlots = freeLocations;
		this.isOpen = isOpen;
	}

	public Station() {
		// TODO Auto-generated constructor stub
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public void setAddress(String address) {
		this.address = address;
	}

	public String getAddress() {
		return address;
	}
	
	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public int getAvailableBikes() {
		return availableBikes;
	}

	public void setAvailableBikes(int availablesBikes) {
		this.availableBikes = availablesBikes;
	}

	public int getFreeSlots() {
		return freeSlots;
	}

	public void setFreeSlots(int freeSlots) {
		this.freeSlots = freeSlots;
	}

	public boolean isOpen() {
		return isOpen;
	}

	public void setOpen(boolean isOpen) {
		if((this.isOpen = isOpen) == false) {
			setAvailableBikes(0);
			setFreeSlots(0);
		}
		
	}
	
	public String getNetwork() {
		return network;
	}

	public void setNetwork(String network) {
		this.network = network;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	public int getId() {
		return this.id;
	}
}