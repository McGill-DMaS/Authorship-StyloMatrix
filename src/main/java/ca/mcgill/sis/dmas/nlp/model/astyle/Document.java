package ca.mcgill.sis.dmas.nlp.model.astyle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.mcgill.sis.dmas.env.StringResources;
import ca.mcgill.sis.dmas.io.LineSequenceWriter;
import ca.mcgill.sis.dmas.io.Lines;
import ca.mcgill.sis.dmas.nlp.corpus.Sentence;

public class Document implements Iterable<Sentence> {
	public String id = StringResources.STR_EMPTY;
	public ArrayList<Sentence> sentences = new ArrayList<>();
	public ArrayList<Sentence> sentences_tags = new ArrayList<>();
	public String rawContent = StringResources.STR_EMPTY;

	@Override
	public String toString() {
		return StringResources.JOINER_LINE.join(sentences);
	}

	public Document(String id, ArrayList<Sentence> sentences) {
		this.sentences = sentences;
		this.id = id;
	}

	public Document() {
	}

	public Document(String id, Sentence... sentences) {
		this.sentences = new ArrayList<>();
		for (Sentence sent : sentences)
			this.sentences.add(sent);
		this.id = id;
	}

	@Override
	public Iterator<Sentence> iterator() {
		return this.sentences.iterator();
	}

	public Iterable<Sentence[]> taggedSentences() {
		return () -> new Iterator<Sentence[]>() {

			int ind = 0;

			@Override
			public boolean hasNext() {
				return ind < sentences.size() && ind < sentences_tags.size();
			}

			@Override
			public Sentence[] next() {
				Sentence[] ret = new Sentence[] { sentences.get(ind), sentences_tags.get(ind) };
				ind += 1;
				return ret;
			}
		};
	}

	public Stream<String> tokens() {
		return this.sentences.stream().flatMap(sent -> Arrays.stream(sent.tokens));
	}

	public static Iterable<Document> loadFromFile(File file) {
		try {

			return () -> {
				try {
					Reader reader = new FileReader(file);
					return (new ObjectMapper()).readerFor(Document.class).readValues(reader);
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			};
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void writeToFile(Iterable<Document> docs, File file) {
		LineSequenceWriter writer;
		try {
			writer = Lines.getLineWriter(file.getAbsolutePath(), false);
			ObjectMapper oMapper = new ObjectMapper();
			for (Document document : docs) {
				writer.writeLine(oMapper.writerWithDefaultPrettyPrinter().writeValueAsString(document));
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
