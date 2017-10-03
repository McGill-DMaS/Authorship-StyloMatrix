package ca.mcgill.sis.dmas.nlp.model.astyle;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import ca.mcgill.sis.dmas.io.collection.EntryPair;

public class Param implements Serializable {
	
	private static final long serialVersionUID = 375529437684174415L;

	@Override
	public String toString() {
		String val = ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
		return val.substring(val.indexOf(".")+1);
	}
	
	
}
