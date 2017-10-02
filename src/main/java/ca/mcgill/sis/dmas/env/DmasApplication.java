/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.env;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.mcgill.sis.dmas.io.file.DmasFileOperations;

public class DmasApplication {
	public static String STR_DATA_PATH = "";
	public static String STR_DATA_PATH_TMP = "tmp/";

	private static Logger logger = LoggerFactory.getLogger(DmasApplication.class);

	public static String applyDataContext(String filePath) {
		File file = new File(filePath);
		if (file.isAbsolute())
			return filePath;
		else
			return STR_DATA_PATH + filePath;
	}

	public static String applyTmpContext(String filePath) {
		File file = new File(filePath);
		if (file.isAbsolute())
			return filePath;
		else
			return STR_DATA_PATH_TMP + filePath;
	}

	public static void contextualize(String dataPath) {
		File file = new File(dataPath);
		if (file.isDirectory()) {
			STR_DATA_PATH = dataPath + "/";
			STR_DATA_PATH_TMP = STR_DATA_PATH + "/tmp/";
		}
		if ((new File(STR_DATA_PATH_TMP).exists())) {
			try {
				DmasFileOperations.deleteRecursively(STR_DATA_PATH_TMP);
			} catch (Exception e) {
				logger.error("Failed to delete existing tmp folder: " + STR_DATA_PATH_TMP, e);
			}
		}
		(new File(STR_DATA_PATH_TMP)).mkdirs();
	}

	public static void contextualize(String dataPath, boolean deleteTmpFolder) {
		File file = new File(dataPath);
		if (file.isDirectory()) {
			STR_DATA_PATH = dataPath + "/";
			STR_DATA_PATH_TMP = STR_DATA_PATH + "/tmp/";
		}
		if (deleteTmpFolder && (new File(STR_DATA_PATH_TMP).exists())) {
			try {
				DmasFileOperations.deleteRecursively(STR_DATA_PATH_TMP);
			} catch (Exception e) {
				logger.error("Failed to delete existing tmp folder: " + STR_DATA_PATH_TMP, e);
			}
		}
		(new File(STR_DATA_PATH_TMP)).mkdirs();
	}

	public static void contextualize(String dataPath, String tmpPath, boolean deleteTmpFolder) {
		File file = new File(dataPath);
		if (file.isDirectory()) {
			STR_DATA_PATH = dataPath + "/";
			STR_DATA_PATH_TMP = tmpPath + "/Kam1n0_tmp/";
		}
		if (deleteTmpFolder && (new File(STR_DATA_PATH_TMP).exists())) {
			try {
				DmasFileOperations.deleteRecursively(STR_DATA_PATH_TMP);
			} catch (Exception e) {
				logger.error("Failed to delete existing tmp folder: " + STR_DATA_PATH_TMP, e);
			}
		}
		(new File(STR_DATA_PATH_TMP)).mkdirs();
	}

	public static File createTmpFile(String name) {
		File file = new File(STR_DATA_PATH_TMP + name);
		try {
			file.createNewFile();
			return file;
		} catch (IOException e) {
			return null;
		}
	}

	public static File createTmpFolder(String name) {
		File file = new File(STR_DATA_PATH_TMP + name);
		file.mkdir();
		return file;
	}

	public static String removeFileExtension(String file) {
		return file.substring(0, file.lastIndexOf('.'));
	}

}
