/**
 * @author Benjamin Fung
 * Honghui modified @2012-9-11 (line 98)
 * Extract features from e-mails and create an ARFF file.
 */

package ca.mcgill.sis.dmas.nlp.model.astyle._4_stylometricBasic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import ca.mcgill.sis.dmas.io.collection.EntryPair;
import ca.mcgill.sis.dmas.nlp.model.astyle.Document;
import ca.mcgill.sis.dmas.nlp.model.astyle._4_stylometricBasic.Stylometric.StylometricParam;

public class FMFeatureMgr {

	private FMFeatureExtractor m_extractor = null;
	private ArrayList<FMFeature> m_features = null;

	public FMFeatureMgr(StylometricParam conf) {
		m_features = new ArrayList<FMFeature>();
		m_extractor = new FMFeatureExtractor();
		m_extractor.inMem = conf.inMem;
		m_extractor.posOnly = conf.posOnly;
		this.conf = conf;
	}

	private StylometricParam conf;

	public void prepare(Iterable<Document> docs) {
		if (conf.Ngram_TopK > 0)
			if (conf.ranker == StylometricParam.NGramRanker.infoGain)
				m_extractor.constructWordList(docs, conf.Ngram_TopK, false);
			else
				m_extractor.constructWordList(docs, conf.Ngram_TopK, true);
	}

	public Map<String, double[]> extractFeatures(Iterable<Document> docs) {

		return StreamSupport.stream(docs.spliterator(), false).collect(Collectors.toMap(doc -> doc.id, doc -> {
			// loading the content and extract features.
			m_extractor.loadContent(doc);
			m_extractor.computeCharBasedFeatures();
			m_extractor.computeWordBasedFeatures();
			if (conf.Ngram_TopK > 0)
				m_extractor.calculateNGramFeature(doc);
			FMRecord newRec = extractFeaturesToOneRecord(m_features.isEmpty());
			return newRec.toVec();
		}));

	}

	private FMRecord extractFeaturesToOneRecord(boolean bMakeFeatures) {
		FMRecord newRec = new FMRecord();

		// ***********************************
		// Character-based lexical features *
		// ***********************************

		if (!this.conf.posOnly) {

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Total number of characters (C)")))
					return null;
			}
			if (!newRec.add(m_extractor.getNChars()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Number of alphabetic characters per character")))
					return null;
			}
			if (!newRec.add(m_extractor.getNAlphabetsPerChar()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Number of upper-case characters per character")))
					return null;
			}
			if (!newRec.add(m_extractor.getNUppersPerChar()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Number of digit characters per character")))
					return null;
			}
			if (!newRec.add(m_extractor.getNDigitsPerChar()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Number of white-space characters per character")))
					return null;
			}
			if (!newRec.add(m_extractor.getNWhiteSpacesPerChar()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Number of space characters per character")))
					return null;
			}
			if (!newRec.add(m_extractor.getNSpacesPerChar()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Number of space characters per white character")))
					return null;
			}
			if (!newRec.add(m_extractor.getNSpacesPerWhiteSpace()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Number of tab spaces per character")))
					return null;
			}
			if (!newRec.add(m_extractor.getNTabsPerChar()))
				return null;

			for (int i = 0; i < FMFeatureExtractor.FM_NUM_ALPHABETS; ++i) {
				if (bMakeFeatures) {
					if (!m_features.add(new FMFeature(
							"Frequency of alphabet: " + ((char) (FMFeatureExtractor.FM_NUMERIC_UPPER_A + i)))))
						return null;
				}
				if (!newRec.add(m_extractor.getNAlphabets(i)))
					return null;
			}

			for (int i = 0; i < FMFeatureExtractor.FM_SPECIAL_CHARS.length; ++i) {
				if (bMakeFeatures) {
					if (!m_features
							.add(new FMFeature("Frequency of special char: " + FMFeatureExtractor.FM_SPECIAL_CHARS[i])))
						return null;
				}
				if (!newRec.add(m_extractor.getNSpecialChars(i)))
					return null;
			}

			// ******************************
			// Word-based lexical features *
			// ******************************

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Total number of words (W)")))
					return null;
			}
			if (!newRec.add(m_extractor.getNWords()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Number of short words per word")))
					return null;
			}
			if (!newRec.add(m_extractor.getNShortWordsPerWord()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Number of characters in words per word")))
					return null;
			}
			if (!newRec.add(m_extractor.getNWordCharsPerChar()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Average word length")))
					return null;
			}
			if (!newRec.add(m_extractor.getAvgWordsLength()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Average number of characters per sentence")))
					return null;
			}
			if (!newRec.add(m_extractor.getAvgCharsPerSentence()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Average number of words per sentence")))
					return null;
			}
			if (!newRec.add(m_extractor.getAvgWordsPerSentence()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Number of distinct words per word")))
					return null;
			}
			if (!newRec.add(m_extractor.getNDistinctWordsPerWord()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Total number of hapax legomena")))
					return null;
			}
			if (!newRec.add(m_extractor.getNHapaxLegomena()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Total number of hapax legomena (D)/W")))
					return null;
			}
			if (!newRec.add(m_extractor.getNHapaxLegomenaPerWord()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Number of hapax legomena per distinct word")))
					return null;
			}
			if (!newRec.add(m_extractor.getNHapaxLegomenaPerDistincWord()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Total number of hapax dislegomena")))
					return null;
			}
			if (!newRec.add(m_extractor.getNHapaxDislegomena()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Yule's K measure")))
					return null;
			}
			if (!newRec.add(m_extractor.getYuleK()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Simpons's D measure")))
					return null;
			}
			if (!newRec.add(m_extractor.getSimpsonD()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Sichel's S measure")))
					return null;
			}
			if (!newRec.add(m_extractor.getSichelS()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Brunet's W measure")))
					return null;
			}
			if (!newRec.add(m_extractor.getBrunetW()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Honore's R measure")))
					return null;
			}
			if (!newRec.add(m_extractor.getHonoreR()))
				return null;

			for (int i = 0; i < FMFeatureExtractor.FM_MAX_WORDLENGTH; ++i) {
				if (bMakeFeatures) {
					if (!m_features.add(new FMFeature("Frequency of words with length " + i + "/W")))
						return null;
				}
				if (!newRec.add(m_extractor.getNWordsWithLengthPerWord(i)))
					return null;
			}

			// ********************
			// Syntatic features *
			// ********************

			for (int i = 0; i < FMFeatureExtractor.FM_PUNCTUATIONS.length; ++i) {
				if (bMakeFeatures) {
					if (!m_features
							.add(new FMFeature("Frequency of punctuation " + FMFeatureExtractor.FM_PUNCTUATIONS[i])))
						return null;
				}
				if (!newRec.add(m_extractor.getNPunctuations(i)))
					return null;
			}

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Number of punctuations per character")))
					return null;
			}
			if (!newRec.add(m_extractor.getNPunctuationsPerChar()))
				return null;

			for (int i = 0; i < FMFeatureExtractor.FM_FUNCTION_WORDS.length; ++i) {
				if (bMakeFeatures) {
					if (!m_features.add(
							new FMFeature("Frequency of function word " + FMFeatureExtractor.FM_FUNCTION_WORDS[i])))
						return null;
				}
				if (!newRec.add(m_extractor.getNFunctionWords(i)))
					return null;
			}

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Number of function words per word")))
					return null;
			}
			if (!newRec.add(m_extractor.getNFunctionWordsPerWord()))
				return null;

			// **********************
			// Structural features *
			// **********************

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Total number of lines")))
					return null;
			}
			if (!newRec.add(m_extractor.getNLines()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Total number of sentences")))
					return null;
			}
			if (!newRec.add(m_extractor.getNSentences()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Total number of paragraphs (P)")))
					return null;
			}
			if (!newRec.add(m_extractor.getNParagraphs()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Number of sentences per paragraph")))
					return null;
			}
			if (!newRec.add(m_extractor.getNSentencesPerParagraph()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Number of characters per paragraph")))
					return null;
			}
			if (!newRec.add(m_extractor.getNCharsPerParagraph()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Number of words per paragraph")))
					return null;
			}
			if (!newRec.add(m_extractor.getNWordsPerParagraph()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Has a greeting?")))
					return null;
			}
			if (!newRec.add(m_extractor.hasGreeting()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Has blank line between paragraphs?")))
					return null;
			}
			if (!newRec.add(m_extractor.hasBlankLineBetweenParagraphs()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Has indentation before paragraphs?")))
					return null;
			}
			if (!newRec.add(m_extractor.hasIndentBeforeParagraphs()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Has e-mail in signature?")))
					return null;
			}
			if (!newRec.add(m_extractor.hasEmailInSignature()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Has telephone in signature?")))
					return null;
			}
			if (!newRec.add(m_extractor.hasTelInSignature()))
				return null;

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Has URL in signature?")))
					return null;
			}
			if (!newRec.add(m_extractor.hasURLInSignature()))
				return null;

			// ****************************
			// Content-specific features *
			// ****************************

			for (int i = 0; i < FMFeatureExtractor.FM_CONTENT_SPECIFIC_WORDS.length; ++i) {
				if (bMakeFeatures) {
					if (!m_features.add(new FMFeature(
							"Frequency of content specific word " + FMFeatureExtractor.FM_CONTENT_SPECIFIC_WORDS[i])))
						return null;
				}
				if (!newRec.add(m_extractor.getNContentSpecificWords(i)))
					return null;
			}

			// *******************************
			// Gender-preferential features *
			// *******************************

			for (int i = 0; i < FMFeatureExtractor.FM_GENDER_PREFERENTIAL_ENDING_WITH.length; ++i) {
				if (bMakeFeatures) {
					if (!m_features.add(new FMFeature("Frequency of word ending with "
							+ FMFeatureExtractor.FM_GENDER_PREFERENTIAL_ENDING_WITH[i])))
						return null;
				}
				if (!newRec.add(m_extractor.getNWordsEndingWith(i)))
					return null;
			}

			if (bMakeFeatures) {
				if (!m_features.add(new FMFeature("Total number of sorry words")))
					return null;
			}
			if (!newRec.add(m_extractor.getNSorryWords()))
				return null;

		}

		// / top-k word-based feature:
		List<EntryPair<String, Double>> words = m_extractor.wordList;
		if (words != null) {
			for (int i = 0; i < words.size(); ++i) {
				EntryPair<String, Double> word = words.get(i);
				double freq = m_extractor.wordCount(word.key);// * word.value;
																// (it is idf)
				if (bMakeFeatures) {
					if (!m_features.add(new FMFeature("Frequency of word (" + word + ")")))
						return null;
				}
				if (!newRec.add(freq))
					return null;
			}
		}
		return newRec;
	}
}