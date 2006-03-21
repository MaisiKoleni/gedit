package gedit.model;

public class Token {
    protected int kind;
    protected String name;
    protected int offset;
    protected int length;

    public String toString() {
    	return name + "[" + kind + "]";
    }
}
