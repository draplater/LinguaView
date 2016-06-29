package LinguaView.syntax;

public class Attribute {
	private String name;
	private String[] edsLinks;
	
	public Attribute() {
		
	}
	
	public Attribute(String newName) {
		name = newName;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String newName) {
		name = newName;
	}
	
	public boolean equals(Object other) {
		if(!(other instanceof Attribute)) {
			return false;
		}
		else {
			return name.equals(((Attribute)other).name);
		}
	}

	public String[] getEdsLinks() {
		return edsLinks;
	}

	public void setEdsLinks(String[] edsLinks) {
		this.edsLinks = edsLinks;
	}
}