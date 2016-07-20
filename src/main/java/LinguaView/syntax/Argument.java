package LinguaView.syntax;

/**
 * Created by draplater on 16-5-27.
 */
public class Argument {
    private String name;
    private boolean isSematicRole;

    public Argument(String name, boolean sematicRole) {
        this.name = name;
        this.isSematicRole = sematicRole;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSematicRole() {
        return isSematicRole;
    }

    public void setSematicRole(boolean sematicRole) {
        isSematicRole = sematicRole;
    }

    public boolean equals(Object other) {
        if(!(other instanceof Argument)) {
            return false;
        }
        else {
            return name.equals(((Argument)other).name) &&
                    isSematicRole == ((Argument)other).isSematicRole;
        }
    }
}
