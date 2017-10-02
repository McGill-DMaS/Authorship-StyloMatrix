/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class DatasetParagrahs {

	private static Logger logger = LoggerFactory.getLogger(DatasetParagrahs.class);

	/**
	 * verify whether the destination folder satisfy the dataset's requiring
	 * structure
	 * 
	 * @param folderPath
	 *            the unzipped dataset folder
	 * @return whether the folder satisfy the requirment
	 */
	public abstract boolean verifyDataset(String folderPath);

	/**
	 * retrive the training set of this dataset (Optional implementation, if it
	 * is not supported the returnning value will be null)
	 * 
	 * @return
	 */
	public abstract ParagraphsTagged getTrainingSet();

	/**
	 * retrive the testing set of this dataset (Optional implementation, if it
	 * is not supported the returnning value will be null)
	 * 
	 * @return
	 */
	public abstract ParagraphsTagged getTestingSet();

	/**
	 * retrive the validation set of this dataset (Optional implementation, if
	 * it is not supported the returnning value will be null)
	 * 
	 * @return
	 */
	public abstract ParagraphsTagged getValidationSet();

	/**
	 * retrive the unlabled training set of this dataset (Optional
	 * implementation, if it is not supported the returnning value will be null)
	 * 
	 * @return
	 */
	public abstract Paragraphs getUnlabeledDataset();

	/**
	 * return the size of all unlabeled samples
	 * 
	 * @return
	 */
	public abstract int getSize_UnlabeledDataset();

	/**
	 * return the size of labeled samples
	 * 
	 * @return
	 */
	public abstract int getSize_LabeledDataset();

	/**
	 * 
	 * @return
	 */
	public abstract ParagraphsTagged getFullLabeledDataset();

	/**
	 * split data
	 * 
	 * @param p1
	 *            percentage for training data
	 * @param p2
	 *            percentage for testing data
	 * @return an array, first element is training data, second element is
	 *         testing data.
	 */
	public ParagraphsTagged[] split(double p1, double p2) {
		if (p1 + p2 != 1 || p1 < 0 || p2 < 0) {
			logger.error(
					"cannot split dataset. Input percentage incorrect: {}, {}",
					p1, p2);
			return null;
		}

		ParagraphsTagged paragraphs = this.getFullLabeledDataset();
		int size = this.getSize_LabeledDataset();
		int t = (int) (size * p1);
		ParagraphsTagged[] result = new ParagraphsTagged[2];
		result[0] = ParagraphsTagged.limit(paragraphs, t);
		result[1] = ParagraphsTagged.skip(paragraphs, t);
		return result;
	}

	public ParagraphsTagged[][] tenFoldSplit() {
		ParagraphsTagged paragraphs = this.getFullLabeledDataset();
		ParagraphsTagged[][] result = new ParagraphsTagged[10][2];
		int size = this.getSize_LabeledDataset();
		int foldSize = size / 10;
		int residue = size % 10;
		if (residue != 0)
			foldSize++;
		for (int i = 0; i < 10; i++) {
			int t_start = i * foldSize;
			int t_end = t_start + foldSize;

			result[i][0] = ParagraphsTagged.merge(
					ParagraphsTagged.limit(paragraphs, t_start),
					ParagraphsTagged.skip(paragraphs, t_end));

			result[i][1] = ParagraphsTagged.limit(
					ParagraphsTagged.skip(paragraphs, t_start), foldSize);

			if (residue != 0) {
				residue--;
				if (residue == 0) {
					foldSize--;
				}
			}
		}
		return result;
	}

}
