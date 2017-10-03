package ca.mcgill.sis.dmas.nlp.model.astyle;

import java.io.Serializable;

public class NodeUser implements Serializable {
	private static final long serialVersionUID = 8429636742198360137L;
	public NodeWord[][] nodes;
	public String id; // id of a paragraph (a list of sentences)
}
