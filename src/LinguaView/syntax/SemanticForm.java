package LinguaView.syntax;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SemanticForm extends Value{
	private String predicate = new String();
	private Set<Argument> arguments = new LinkedHashSet<Argument>();
	private String[] edsLinks;
	
	public SemanticForm() {
		
	}
	
	public SemanticForm(String pred, Argument[] args) {
		setPred(pred);
		for(Argument arg: args) {
			arguments.add(arg);
		}
	}

	public SemanticForm(String pred, Set<Argument> args) {
		setPred(pred);
		for(Argument arg: args) {
			arguments.add(arg);
		}
	}
	
	public boolean equals(Object other) {
		if(!(other instanceof SemanticForm)) {
			return false;
		}
		if(!predicate.equals(((SemanticForm)other).predicate)) {
			return false;
		}
		else {
			return argsEqual((SemanticForm)other);
			
		}
	}
	
	private boolean argsEqual(SemanticForm other) {
		if(this.argsContainAll(other.arguments) && other.argsContainAll(this.arguments)) {
			return true;
		}
		else {
			return false;
		}
	}
	
	private boolean argsContainAll(Set<Argument> otherargs) {
		for(Argument otherarg: otherargs) {
			if(!argContains(otherarg)) {
				return false;
			}
		}
		return true;
	}
	
	private boolean argContains(Argument otherarg) {
		for(Argument arg: arguments) {
			if(arg.equals(otherarg)) {
				return true;
			}
		}
		return false;
	}
	
	public String getPred() {
		return predicate;
	}
	
	public Set<Argument> getArgs() {
		return arguments;
	}
	
	public String[] getStringArgs() {
		String[] args = new String[arguments.size()];
		int i = 0;
		for(Argument argInAttr: arguments) {
			args[i] = argInAttr.getName();
			i++;
		}
		return args;
	}

	public String[] getSemanticArgs() {
		List<String> args = new ArrayList<>();
		for(Argument argInAttr: arguments) {
			if(argInAttr.isSematicRole()) {
				args.add(argInAttr.getName());
			}
		}
		return args.toArray(new String[args.size()]);
	}

	public String[] getNonSemanticArgs() {
		List<String> args = new ArrayList<>();
		for(Argument argInAttr: arguments) {
			if(!argInAttr.isSematicRole()) {
				args.add(argInAttr.getName());
			}
		}
		return args.toArray(new String[args.size()]);
	}
	
	public void setPred(String pred) {
		predicate = pred;
	}
	
	public void addArgs(Argument arg) {
		arguments.add(arg);
	}

	@Override
	public String[] getEdsLinks() {
		return edsLinks;
	}

	public void setEdsLinks(String[] edsLinks) {
		this.edsLinks = edsLinks;
	}
}