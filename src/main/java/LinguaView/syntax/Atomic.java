package LinguaView.syntax;

public class Atomic extends Value {
	private String value;
	private String[] edsLinks;
	
	public Atomic() {
		
	}
	
	public Atomic(String val) {
		value = val;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String val) {
		value = val;
	}
	
	public boolean equals(Object other) {
		if(!(other instanceof Atomic)) {
			return false;
		}
		else {
			return value.equals(((Atomic)other).value);
		}
	}

	@Override
	public String[] getEdsLinks() {
		return edsLinks;
	}

	public void setEdsLinks(String[] edsLinks) {
		this.edsLinks = edsLinks;
	}
}