package LinguaView.syntax;

import java.util.Random;

public abstract class Value {
	public enum ValueType {
		SEM_FORM,
		AVM,
		SET_OF_AVM
	}

	int hash;

	Value() {
		Random rn = new Random();
		hash = rn.nextInt();
	}
	
	public ValueType type() {
		if (this instanceof SetOfAttributeValueMatrix)
			return ValueType.SET_OF_AVM;
		else if (this instanceof AttributeValueMatrix)
			return ValueType.AVM;
		else if (this instanceof SemanticForm)
			return ValueType.SEM_FORM;
		else 
			return null;
	}

	@Override
	public int hashCode() {
		return hash;
	}

	abstract String[] getEdsLinks();
}