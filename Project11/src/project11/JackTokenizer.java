package project11;

/*
 *  Chung-An, Chiu 12172213 
 *  Project 11
 *  Nov 30, 2017
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JackTokenizer {
	private static Pattern tokenPatterns;

	private String keyWordPatterns = "class|method|function|constructor|int|boolean"
			+ "|char|void|var|static|field|let|do|if|else|while|return|true|false|null|this";
	private String symbolPatterns = "[\\{\\}\\(\\)\\[\\]\\.\\,\\;\\+\\-\\*\\/\\&\\|\\<\\>\\=\\~]";
	private String intPatterns = "[0-9]+";
	private String stringPatterns = "\"[^\"\n]*\""; // 0 or more word as String can be blank
	private String identifierPatterns = "[\\w_]+"; // one or more word/underscore

	private ArrayList<String> tokens; // parsed tokens string list
	private String currentToken;
	private String currentTokenType;
	private int index;
	private boolean inBlockComment = false;

	public JackTokenizer(File inputFile) {

		// implement java.util.regex.Pattern
		// initialize each String of token symbols
		tokenPatterns = Pattern.compile(keyWordPatterns + "|" + symbolPatterns + "|" + intPatterns + "|"
				+ stringPatterns + "|" + identifierPatterns);

		readFile(inputFile);

	}

	/*
	 * read the input file and parse into the tokens string list
	 */
	private void readFile(File inputFile) {
		BufferedReader bufferedReader = null;
		// BufferedWriter bufferedWriter = null;
		try {
			tokens = new ArrayList<>();
			index = 0;
			bufferedReader = new BufferedReader(new FileReader(inputFile));
			String currentLine;
			while ((currentLine = bufferedReader.readLine()) != null) {
				currentLine = getRidOfComments(currentLine);
				if (currentLine.equals("") || inBlockComment) {
					continue;
				}

				Matcher matcher = tokenPatterns.matcher(currentLine);
				while (matcher.find()) {
					tokens.add(matcher.group());
				}
			}

		} catch (FileNotFoundException e) {
			System.err.println("Cannot find the file! Please check if it exist!");
		} catch (IOException e) {
			System.err.println("Errors happened during file input/output! To help with this issue, please "
					+ "send your input file and the environment to chungan@uchicago.edu. Thank you!");
			e.printStackTrace();
		} finally {
			try {
				if (bufferedReader != null) {
					bufferedReader.close();
				}
			} catch (IOException e) {
				System.err.println("Errors happened during file input/output! To help with this issue, please "
						+ "send your input file and the environment to chungan@uchicago.edu. Thank you!");
				e.printStackTrace();
			}
		}
	}

	/*
	 * check if there are more tokens
	 */
	public boolean hasMoreTokens() {
		return index < tokens.size();
	}

	/*
	 * advance to the next token
	 */
	public void advance() throws ParseException {
		if (hasMoreTokens()) {
			currentToken = tokens.get(index++);
		} else {
			throw new ArrayIndexOutOfBoundsException("No more to advance");
		}

		// deal with the tokenType once changed
		if (currentToken.matches(keyWordPatterns)) {
			currentTokenType = "KEYWORD";
		} else if (currentToken.matches(symbolPatterns)) {
			currentTokenType = "SYMBOL";
		} else if (currentToken.matches(intPatterns)) {
			currentTokenType = "INT_CONST";
		} else if (currentToken.matches(stringPatterns)) {
			currentTokenType = "STRING_CONST";
		} else if (currentToken.matches(identifierPatterns)) {
			currentTokenType = "IDENTIFIER";
		} else {
			throw new ParseException("Can't match thes token: " + currentToken, index);
		}
	}

	/*
	 * rewind the token if pattern is not match
	 */
	public void rewind() throws ParseException {
		if (index != 0) {
			index--;
		} else {
			throw new ArrayIndexOutOfBoundsException("Cannot rewind anymore");
		}
	}

	/*
	 * return the type of the current token
	 */
	public String tokenType() {
		return currentTokenType;
	}

	/*
	 * return the keyword when the type is one of the kind
	 */
	public String keyWord() {
		if (currentTokenType == "KEYWORD") {
			return currentToken;
		} else {
			throw new IllegalStateException("The token is not a key word");
		}
	}

	/*
	 * return the symbol when the type is one of the kind
	 */
	public char symbol() {
		if (currentTokenType == "SYMBOL") {
			return currentToken.charAt(0);
		} else {
			throw new IllegalStateException("The token is not a symbol");
		}
	}

	/*
	 * return the identifier when the type is one of the kind
	 */
	public String identifier() {
		if (currentTokenType == "IDENTIFIER") {
			return currentToken;
		} else {
			throw new IllegalStateException("The token is not an identifier");
		}
	}

	/*
	 * return the int value when the type is one of the kind
	 */
	public int intVal() {
		if (currentTokenType == "INT_CONST") {
			return Integer.parseInt(currentToken);
		} else {
			throw new IllegalStateException("The token is not an int value");
		}
	}

	/*
	 * return the string value when the type is one of the kind
	 */
	public String stringVal() {
		if (currentTokenType == "STRING_CONST") {
			return currentToken.substring(1, currentToken.length() - 1);
		} else {
			throw new IllegalStateException("The token is not a string value");
		}
	}

	/*
	 * This function return the input line with no white spaces, tabs.
	 */
	private String getRidOfComments(String s) {
		if (!inBlockComment) {
			int lineComment = s.indexOf("//");
			if (lineComment >= 0) {
				return s.substring(0, lineComment);
			}

			int startBlockComment = s.indexOf("/*");
			if (startBlockComment >= 0) {
				inBlockComment = true;
			}
		}

		int endBlockComment = s.indexOf("*/");
		if (endBlockComment >= 0) {
			inBlockComment = false;
			if (s.endsWith("*/")) {
				return "";
			} else {
				return getRidOfComments(s.substring(endBlockComment + 2));
			}
		}

		return s;

	}
}
