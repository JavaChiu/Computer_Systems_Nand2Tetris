
/*
 *  Chung-An, Chiu 12172213 
 *  Project 0
 *  Oct 5, 2017
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ClearFile {
	public static void main(String[] args) {
		File inputFile = null;
		String outputFile = null;
		Boolean noComments = false;

		// Check arguments
		if (args.length > 0) {
			inputFile = new File(args[0]);
			outputFile = args[0].substring(0, args[0].length() - 2) + "out"; // .in->.out
			if (args.length > 1) {
				if (args[1].equals("no-comments")) {
					noComments = true;
				} else {
					System.err.println("The arguments passed in are wrong! Please see README.txt!");
					System.exit(1);
				}
			}
		} else {
			System.err.println("The arguments passed in are wrong! Please see README.txt!");
			System.exit(1);
		}

		BufferedReader bufferedReader = null;
		BufferedWriter bufferedWriter = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(inputFile));
			bufferedWriter = new BufferedWriter(new FileWriter(outputFile));
			String currentLine;
			while ((currentLine = bufferedReader.readLine()) != null) {
				currentLine = getRidOfWhiteSpaceAndComment(currentLine, noComments);
				// if not purely spaces and tabs, write it to new file and append a line return
				if (!currentLine.equals("")) {
					bufferedWriter.write(currentLine + "\n");
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
				if (bufferedWriter != null) {
					bufferedWriter.close();
				}
			} catch (IOException e) {
				System.err.println("Errors happened during file input/output! To help with this issue, please "
						+ "send your input file and the environment to chungan@uchicago.edu. Thank you!");
				e.printStackTrace();
			}
		}
	}

	/*
	 * This function return the input line with no white spaces, tabs. If passes
	 * "noComments" arguments, it will return the line without the words after
	 * comments(//).
	 */
	private static String getRidOfWhiteSpaceAndComment(String s, boolean noComments) {
		String[] sArray = s.split("");
		StringBuffer stringBuffer = new StringBuffer();
		boolean hitComment = false;
		for (int i = 0; i < sArray.length; i++) {
			if (i > 0 && sArray[i].equals("/") && sArray[i - 1].equals("/")) {
				if (noComments) {
					stringBuffer.deleteCharAt(stringBuffer.length() - 1);
					break;
				} else {
					hitComment = true;
					stringBuffer.append(sArray[i]);
					continue;
				}
			}
			if (hitComment) { // if hitComment, we don't deal with the white space
				stringBuffer.append(sArray[i]);
			} else {
				if (!sArray[i].equals(" ") && !sArray[i].equals("\t")) {
					stringBuffer.append(sArray[i]);
				}
			}
		}
		return stringBuffer.toString();
	}
}
