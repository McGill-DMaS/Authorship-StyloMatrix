/*******************************************************************************
 * Copyright 2015 McGill University. All rights reserved.                       
 *                                                                               
 * Unless required by applicable law or agreed to in writing, the software      
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF      
 * ANY KIND, either express or implied.                                         
 *******************************************************************************/
package ca.mcgill.sis.dmas.nlp.corpus;

import java.util.Scanner;

import ca.mcgill.sis.dmas.io.Lines;
import ca.mcgill.sis.dmas.io.TokensOnDisk;

public class InteractiveConsole {

	public static void run(ParagraphsTagged paragraphs) {
		Scanner scanner = new Scanner(System.in);
		for (ParagraphTagged paragraph : paragraphs) {
			for (Sentence sentence : paragraph) {
				System.out.println(sentence.toString());
			}
			System.out.println(paragraph.Tags.toString());
			System.out.println(paragraph.sentences.length+" sentences");
			scanner.nextLine();
		}
		
		scanner.close();
	}
	
	public static void run(Paragraphs paragraphs) {
		Scanner scanner = new Scanner(System.in);
		for (Paragraph paragraph : paragraphs) {
			for (Sentence sentence : paragraph.sentences) {
				System.out.println(sentence.toString());
				scanner.nextLine();
			}
		}
		scanner.close();
	}

	public static void run(Sentences sentences) {
		Scanner scanner = new Scanner(System.in);
		for (Sentence sentence : sentences) {
			System.out.println(sentence.toString());
			scanner.nextLine();
		}
		scanner.close();
	}
	
	public static void run(Lines lines) {
		Scanner scanner = new Scanner(System.in);
		for (String sentence : lines) {
			System.out.println(sentence.toString());
			scanner.nextLine();
		}
		scanner.close();
	}
	
	public static void run(TokensOnDisk tokens) {
		Scanner scanner = new Scanner(System.in);
		for (String sentence : tokens) {
			System.out.println(sentence.toString());
			scanner.nextLine();
		}
		scanner.close();
	}
	
	public static void run(Iterable<Object> objects) {
		Scanner scanner = new Scanner(System.in);
		for (Object obj : objects) {
			System.out.println(obj.toString());
			scanner.nextLine();
		}
		scanner.close();
	}
}
