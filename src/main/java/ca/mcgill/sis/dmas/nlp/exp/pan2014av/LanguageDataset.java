package ca.mcgill.sis.dmas.nlp.exp.pan2014av;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.mcgill.sis.dmas.io.collection.EntryPair;
import ca.mcgill.sis.dmas.nlp.corpus.parser.Tagger.Language;
import ca.mcgill.sis.dmas.nlp.model.astyle.Document;

public class LanguageDataset {

	private static Logger logger = LoggerFactory.getLogger(LanguageDataset.class);

	private static ObjectMapper mapper = new ObjectMapper();

	public static final String FILE_META = "meta.json";

	public static final String FILE_DOCS = "documents.json";

	public ArrayList<EntryPair<String, String>> truthMapping;

	public Language lang;

	@JsonIgnore
	public Iterable<Document> documents;

	public static LanguageDataset load(String folder) throws JsonParseException, JsonMappingException, IOException {
		LanguageDataset val = mapper.readValue(new File(folder + "//" + FILE_META), LanguageDataset.class);
		val.documents = Document.loadFromFile(new File(folder + "//" + FILE_DOCS));
		return val;
	}

	public void save(String folder) {
		File file = new File(folder);
		if (file.exists() && file.isFile())
			logger.error("Failed to load from {}. It is a file not a folder.", folder);
		if (!file.exists())
			file.mkdirs();
		File meta = new File(folder + "//" + FILE_META);
		File docs = new File(folder + "//" + FILE_DOCS);
		try {
			mapper.writeValue(meta, this);
			Document.writeToFile(documents, docs);
		} catch (IOException e) {
			logger.error("Failed to save dataset into " + folder, e);
		}

	}

	public LanguageDataset(ArrayList<EntryPair<String, String>> truths, Iterable<Document> docs, Language lang) {
		this.truthMapping = truths;
		this.documents = docs;
		this.lang = lang;
	}

	public LanguageDataset() {
	}

}