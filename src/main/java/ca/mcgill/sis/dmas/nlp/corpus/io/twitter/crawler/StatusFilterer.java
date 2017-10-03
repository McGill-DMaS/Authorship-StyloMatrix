package ca.mcgill.sis.dmas.nlp.corpus.io.twitter.crawler;

import twitter4j.Status;

public interface StatusFilterer {

	public boolean acceptStatus(Status status);
	
}
