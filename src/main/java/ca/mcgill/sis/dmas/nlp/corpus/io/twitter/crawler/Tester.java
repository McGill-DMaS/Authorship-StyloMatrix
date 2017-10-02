package ca.mcgill.sis.dmas.nlp.corpus.io.twitter.crawler;

public class Tester {

	public static void main(String[] args) {
		TweetFieldsFetcher fetcher = new TweetFieldsFetcher();
		TweetFieldsResponse tr = fetcher.fetchTweetFields("265861098597658624");
		System.out.println(tr.toString());
	}

}
