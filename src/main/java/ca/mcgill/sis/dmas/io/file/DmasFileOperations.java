/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.io.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.mcgill.sis.dmas.env.DmasApplication;
import ca.mcgill.sis.dmas.env.StringResources;

public class DmasFileOperations {

	private static Logger logger = LoggerFactory.getLogger(DmasFileOperations.class);

	public static void deleteRecursively(String path) throws Exception {
		path = DmasApplication.applyDataContext(path);
		Path directory = Paths.get(path);
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}

		});
	}

	public static Pattern REGEX_DLL = Pattern.compile("\\.dll$", Pattern.CASE_INSENSITIVE);
	public static Pattern REGEX_EXE = Pattern.compile("\\.exe$", Pattern.CASE_INSENSITIVE);
	public static Pattern REGEX_CPP = Pattern.compile("\\.cpp$", Pattern.CASE_INSENSITIVE);
	public static Pattern REGEX_H = Pattern.compile("\\.h$", Pattern.CASE_INSENSITIVE);
	public static Pattern REGEX_ASM = Pattern.compile("\\.asm$", Pattern.CASE_INSENSITIVE);
	public static Pattern REGEX_JSON = Pattern.compile("\\.json$", Pattern.CASE_INSENSITIVE);
	public static Pattern REGEX_ALL = Pattern.compile(StringResources.REGEX_ANY);

	public static ArrayList<File> select(String path, final Pattern... patterns) throws Exception {
		final ArrayList<File> files = new ArrayList<File>();
		path = DmasApplication.applyDataContext(path);
		Path directory = Paths.get(path);
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				File tfile = file.toFile();
				for (Pattern pattern : patterns) {
					if (pattern.matcher(tfile.getName().trim()).find()) {
						files.add(tfile);
						break;
					}
				}
				return FileVisitResult.CONTINUE;
			}

		});
		return files;
	}

	public static ArrayList<File> select(String path, final String... regexs) {
		List<Pattern> patterns = Arrays.stream(regexs).map(Pattern::compile).collect(Collectors.toList());
		final ArrayList<File> files = new ArrayList<File>();
		path = DmasApplication.applyDataContext(path);
		Path directory = Paths.get(path);
		try {
			Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					File tfile = file.toFile();
					for (Pattern pattern : patterns) {
						if (pattern.matcher(tfile.getName().trim()).find()) {
							files.add(tfile);
							break;
						}
					}
					return FileVisitResult.CONTINUE;
				}

			});
		} catch (IOException e) {
			logger.error("Failed to select files from directory. Returning null.", e);
			return null;
		}
		return files;
	}

	public static ArrayList<File> selectDirectories(String path) throws Exception {
		final ArrayList<File> files = new ArrayList<File>();
		path = DmasApplication.applyDataContext(path);
		Path directory = Paths.get(path);
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				File tfile = dir.toFile();
				if (tfile.isDirectory()) {
					files.add(tfile);
				}
				return FileVisitResult.CONTINUE;
			};

		});
		return files;
	}
}
