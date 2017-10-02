/*
 * Copyright (C) 2007 by
 * 
 * 	Xuan-Hieu Phan
 *	hieuxuan@ecei.tohoku.ac.jp or pxhieu@gmail.com
 * 	Graduate School of Information Sciences
 * 	Tohoku University
 * 
 *  Cam-Tu Nguyen
 *  ncamtu@gmail.com
 *  College of Technology
 *  Vietnam National University, Hanoi
 *
 * JGibbsLDA is a free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * JGibbsLDA is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JGibbsLDA; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package ca.mcgill.sis.dmas.nlp.model.astyle._5_LDA;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import ca.mcgill.sis.dmas.io.collection.Counter;
import ca.mcgill.sis.dmas.io.collection.DmasCollectionOperations;

public class LDADataset {
	// ---------------------------------------------------------------
	// Instance Variables
	// ---------------------------------------------------------------

	public Dictionary localDict; // local dictionary
	public LDADocument[] docs; // a list of documents
	public int M; // number of documents
	public int V; // number of words

	// map from local coordinates (id) to global ones
	// null if the global dictionary is not set
	public Map<Integer, Integer> lid2gid;

	// link to a global dictionary (optional), null for train data, not null for
	// test data
	public Dictionary globalDict;

	public ArrayList<String> docIndIdMap = new ArrayList<>();

	// --------------------------------------------------------------
	// Constructor
	// --------------------------------------------------------------
	public LDADataset() {
		localDict = new Dictionary();
		M = 0;
		V = 0;
		docs = null;

		globalDict = null;
		lid2gid = null;
	}

	public LDADataset(int M) {
		localDict = new Dictionary();
		this.M = M;
		this.V = 0;
		docs = new LDADocument[M];

		globalDict = null;
		lid2gid = null;
	}

	public LDADataset(int M, Dictionary globalDict) {
		localDict = new Dictionary();
		this.M = M;
		this.V = 0;
		docs = new LDADocument[M];

		this.globalDict = globalDict;
		lid2gid = new HashMap<Integer, Integer>();
	}

	// -------------------------------------------------------------
	// Public Instance Methods
	// -------------------------------------------------------------
	/**
	 * set the document at the index idx if idx is greater than 0 and less than
	 * M
	 * 
	 * @param doc
	 *            document to be set
	 * @param idx
	 *            index in the document array
	 */
	public void setDoc(LDADocument doc, int idx) {
		if (0 <= idx && idx < M) {
			docs[idx] = doc;
		}
	}

	/**
	 * set the document at the index idx if idx is greater than 0 and less than
	 * M
	 * 
	 * @param str
	 *            string contains doc
	 * @param idx
	 *            index in the document array
	 */
	public void setDoc(String str, int idx) {
		if (0 <= idx && idx < M) {
			String[] words = str.split("[ \\t\\n]");

			Vector<Integer> ids = new Vector<Integer>();

			for (String word : words) {
				int _id = localDict.word2id.size();

				if (localDict.contains(word))
					_id = localDict.getID(word);

				if (globalDict != null) {
					// get the global id
					Integer id = globalDict.getID(word);
					// System.out.println(id);

					if (id != null) {
						localDict.addWord(word);

						lid2gid.put(_id, id);
						ids.add(_id);
					} else { // not in global dictionary
								// do nothing currently
					}
				} else {
					localDict.addWord(word);
					ids.add(_id);
				}
			}

			LDADocument doc = new LDADocument(ids, str);
			docs[idx] = doc;
			V = localDict.word2id.size();
		}
	}

	public static LDADataset convertDocument(Iterable<ca.mcgill.sis.dmas.nlp.model.astyle.Document> docs) {
		LDADataset data = new LDADataset((int) DmasCollectionOperations.count(docs));
		data.docIndIdMap = new ArrayList<>();
		docs.forEach(doc -> {
			data.setDoc(doc.rawContent, data.docIndIdMap.size());
			data.docIndIdMap.add(doc.id);
		});
		return data;
	}

	public static LDADataset convertDocument(Iterable<ca.mcgill.sis.dmas.nlp.model.astyle.Document> docs,
			Dictionary dict) {
		LDADataset data = new LDADataset((int) DmasCollectionOperations.count(docs), dict);
		data.docIndIdMap = new ArrayList<>();
		docs.forEach(doc -> {
			data.setDoc(doc.rawContent, data.docIndIdMap.size());
			data.docIndIdMap.add(doc.id);
		});
		return data;
	}

}
