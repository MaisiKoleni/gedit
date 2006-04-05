package gedit.model;

public class Comment extends ModelBase {
	public Comment(Object parent, String label) {
		super(parent, label);
	}

	public ModelType getType() {
		return ModelType.COMMENT;
	}

}
