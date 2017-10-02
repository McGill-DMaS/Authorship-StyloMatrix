package ca.mcgill.sis.dmas.nlp.corpus.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ca.mcgill.sis.dmas.env.StringResources;
import ca.mcgill.sis.dmas.nlp.parser.greek.SmallSetFunctions;
import ca.mcgill.sis.dmas.nlp.parser.greek.WordWithCategory;

public class TaggerGreek extends Tagger {

	@Override
	public List<String> tag(String[] tokens) {
		String sent = StringResources.JOINER_TOKEN.join(tokens);
		if(sent.trim().length() < 1)
			return new ArrayList<>();
		List<WordWithCategory> tagged = SmallSetFunctions
				.smallSetClassifyString(sent);
		return tagged.stream().map(tw -> tw.getCategory()).collect(Collectors.toList());
	}

}
