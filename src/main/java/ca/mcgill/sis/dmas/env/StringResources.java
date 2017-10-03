/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.env;

import java.io.File;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import com.google.common.base.Joiner;

public class StringResources {

	public static String STR_LINEBREAK = System.getProperty("line.separator");
	public static String STR_TOKENBREAK = " ";
	public static String STR_PARAGRAPHBREAK = "# # # #";
	public static String STR_EMPTY = "";
	public static Joiner JOINER_TOKEN = Joiner.on(STR_TOKENBREAK).skipNulls();
	public static Joiner JOINER_TOKEN_DOT = Joiner.on(".").skipNulls();
	public static Joiner JOINER_TOKEN_CSV = Joiner.on(",").skipNulls();
	public static Joiner JOINER_TOKEN_CSV_SPACE = Joiner.on(", ").skipNulls();
	public static Joiner JOINER_DASH = Joiner.on("-").skipNulls();
	public static Joiner JOINER_LINE = Joiner.on(STR_LINEBREAK).skipNulls();

	public static String REGEX_URL = "(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
	public static String REGEX_NON_ASCII = "[^\\x00-\\x7F]";
	public static String REGEX_NUMBER = ".*\\D.*";
	public static String REGEX_ANY = ".*";
	public static String REGEX_NOTHING = "a^";
	public static String REGEX_UTF8_BOM = "\uFEFF";

	public static DecimalFormat FORMAT_AR2D = new DecimalFormat("#.00");
	public static DecimalFormat FORMAT_AR3D = new DecimalFormat("#.000");
	public static DecimalFormat FORMAT_AR4D = new DecimalFormat("#.0000");
	public static DecimalFormat FORMAT_AR5D = new DecimalFormat("#.00000");
	public static DecimalFormat FORMAT_2R2D = new DecimalFormat("00.00");
	public static DecimalFormat FORMAT_2R3D = new DecimalFormat("00.000");
	public static DecimalFormat FORMAT_2R4D = new DecimalFormat("00.0000");
	public static DecimalFormat FORMAT_3R3D = new DecimalFormat("000.000");
	public static DecimalFormat FORMAT_4R4D = new DecimalFormat("0000.0000");
	public static DecimalFormat FORMAT_5R5D = new DecimalFormat("00000.00000");
	public static DecimalFormat FORMAT_2R = new DecimalFormat("00");
	public static DecimalFormat FORMAT_3R = new DecimalFormat("000");
	public static DecimalFormat FORMAT_4R = new DecimalFormat("0000");
	public static DecimalFormat FORMAT_5R = new DecimalFormat("00000");
	public static SimpleDateFormat FORMAT_TIME = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");

	public static String timeString() {
		return FORMAT_TIME.format(new Date());
	}

	public static int countNumber(String str) {
		int cout = 0;
		char[] chars = str.toCharArray();
		for (int i = 0; i < chars.length; ++i) {
			if (chars[i] >= '0' && chars[i] <= '9')
				cout++;
		}
		return cout;
	}

	public static String removeSpace(String str) {
		return str.replaceAll("[\\t\\n\\r]", " ");
	}

	public static double countNumericPercent(String str) {
		int cout = 0;
		char[] chars = str.toCharArray();
		for (int i = 0; i < chars.length; ++i) {
			if (chars[i] >= '0' && chars[i] <= '9')
				cout++;
		}
		return cout * 1.0 / chars.length;
	}

	public static String replaceLast(String oriString, String pattern, String subsitude) {
		int ind = oriString.lastIndexOf(pattern);
		if (ind < 0)
			return oriString;
		else
			return new StringBuilder(oriString).replace(ind, ind + pattern.length(), subsitude).toString();
	}

	public static String reverse(String str) {
		return new StringBuilder(str).reverse().toString();
	}

	private static RandomString randomString = new RandomString(10);

	public static synchronized String randomString(int length) {
		return randomString.nextString();
	}

	public static class RandomString {

		private static char[] symbols;

		public RandomString(long seed, int length) {
			random = new Random(seed);
			if (length < 1)
				throw new IllegalArgumentException("length < 1: " + length);
			buf = new char[length];
		}

		static {
			StringBuilder tmp = new StringBuilder();
			for (char ch = '0'; ch <= '9'; ++ch)
				tmp.append(ch);
			for (char ch = 'a'; ch <= 'z'; ++ch)
				tmp.append(ch);
			symbols = tmp.toString().toCharArray();
		}

		private Random random;

		private final char[] buf;

		public RandomString(int length) {
			random = new Random();
			if (length < 1)
				throw new IllegalArgumentException("length < 1: " + length);
			buf = new char[length];
		}

		public String nextString() {
			for (int idx = 0; idx < buf.length; ++idx)
				buf[idx] = symbols[random.nextInt(symbols.length)];
			return new String(buf);
		}
	}

	public static String replaceInvalidFileCharacters(String name) {
		return name.replaceAll("[^a-zA-Z0-9.-]", "_");
	}

	public static String getRootPath() {
		try {
			return new File(StringResources.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
					.getParentFile().getParentFile().getAbsolutePath();
		} catch (URISyntaxException e) {
			return "";
		}
	}
}
