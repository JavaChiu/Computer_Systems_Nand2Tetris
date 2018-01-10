
/*
 *  Chung-An, Chiu 12172213 
 *  Project 6
 *  Oct 23, 2017
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Assembler {
	public static void main(String[] args) {
		File inputFile = null;
		String outputFile = null;

		// Check arguments
		if (args.length > 0) {
			inputFile = new File(args[0]);
			// check file name
			if (args[0].endsWith("\\.asm")) {
				System.err.println("Please input .asm file! Thank you!");
				System.exit(1);
			}
			outputFile = args[0].substring(0, args[0].length() - 3) + "hack"; // .in->.out
		} else {
			System.err.println("The arguments passed in are wrong! Please see README.txt!");
			System.exit(1);
		}

		int maxIndex = 16; // for creating new variable in RAM
		HashMap<String, Integer> symbolTable = createSymbolTable();
		HashMap<String, String> destTable = createDestTable();
		HashMap<String, String> compTable = createCompTable();
		HashMap<String, String> jumpTable = createJumpTable();

		BufferedReader bufferedReader = null;
		BufferedWriter bufferedWriter = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(inputFile));
			bufferedWriter = new BufferedWriter(new FileWriter(outputFile));
			String currentLine;
			ArrayList<String> lineList = new ArrayList<String>();

			// First pass: read in the lines and store in lineList
			while ((currentLine = bufferedReader.readLine()) != null) {
				currentLine = getRidOfWhiteSpaceAndComment(currentLine, true);
				if (!currentLine.equals("")) {
					lineList.add(currentLine);
				}
			}

			// Second pass: parse the parenthesis, insert into symbol table
			for (int i = 0; i < lineList.size(); i++) {
				String temp = lineList.get(i);
				if (temp.startsWith("(") && temp.endsWith(")")) {
					symbolTable.put(temp.substring(1, temp.length() - 1), i);
					lineList.remove(i--);
				}
			}

			// Third pass: deal with the instructions
			for (int i = 0; i < lineList.size(); i++) {
				String temp = lineList.get(i);
				if (temp.startsWith("@")) { // A_COMMAND
					int num = 0;
					try {
						num = Integer.parseInt(temp.substring(1, temp.length()));
					} catch (NumberFormatException e) {
						if (symbolTable.containsKey(temp.substring(1, temp.length()))) {
							num = symbolTable.get(temp.substring(1, temp.length()));
						} else {
							symbolTable.put(temp.substring(1, temp.length()), maxIndex);
							num = maxIndex;
							while (symbolTable.containsValue(++maxIndex))
								;
						}
					}
					StringBuffer stringBuffer = new StringBuffer(Integer.toBinaryString(num));
					while (stringBuffer.length() < 15) {
						stringBuffer.insert(0, 0);
					}
					lineList.set(i, "0" + stringBuffer.toString());
				} else { // C_COMMAND
					StringBuffer stringBuffer = new StringBuffer();
					stringBuffer.append("111");

					int indexOfEqual = temp.indexOf("=");
					int indexOfSemi = temp.indexOf(";");
					String comp = null;
					String dest = null;
					if (indexOfSemi < 0) {
						if (indexOfEqual < 0) { // C
							comp = temp;
						} else { // D = C
							dest = temp.substring(0, indexOfEqual);
							comp = temp.substring(indexOfEqual + 1, temp.length());
						}
						// start dealing with bits
						if (comp.contains("M")) {
							stringBuffer.append("1");
						} else {
							stringBuffer.append("0");
						}
						stringBuffer.append(compTable.get(comp));
						if (dest == null) {
							stringBuffer.append("000");
						} else {
							stringBuffer.append(destTable.get(dest));
						}
						stringBuffer.append("000"); // jump bits
					} else {
						if (indexOfEqual < 0) { // C ; JMP
							comp = temp.substring(0, indexOfSemi);
						} else { // D = C ; JMP
							dest = temp.substring(0, indexOfEqual);
							comp = temp.substring(indexOfEqual + 1, indexOfSemi);
						}
						// start dealing with bits
						if (comp.contains("M")) {
							stringBuffer.append("1");
						} else {
							stringBuffer.append("0");
						}
						stringBuffer.append(compTable.get(comp));
						if (dest == null) {
							stringBuffer.append("000");
						} else {
							stringBuffer.append(destTable.get(dest));
						}
						stringBuffer.append(jumpTable.get(temp.substring(indexOfSemi + 1)));
					}
					lineList.set(i, stringBuffer.toString());
				}
				bufferedWriter.write(lineList.get(i) + "\n"); // write to file
			}

			// // if not purely spaces and tabs, write it to new file and append a line
			// return
			// if (!currentLine.equals("")) {
			// bufferedWriter.write(currentLine + "\n");
			// }
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

	private static HashMap<String, Integer> createSymbolTable() {
		HashMap<String, Integer> symbolTable = new HashMap<String, Integer>();

		symbolTable.put("SP", 0);
		symbolTable.put("LCL", 1);
		symbolTable.put("ARG", 2);
		symbolTable.put("THIS", 3);
		symbolTable.put("THAT", 4);
		symbolTable.put("R0", 0);
		symbolTable.put("R1", 1);
		symbolTable.put("R2", 2);
		symbolTable.put("R3", 3);
		symbolTable.put("R4", 4);
		symbolTable.put("R5", 5);
		symbolTable.put("R6", 6);
		symbolTable.put("R7", 7);
		symbolTable.put("R8", 8);
		symbolTable.put("R9", 9);
		symbolTable.put("R10", 10);
		symbolTable.put("R11", 11);
		symbolTable.put("R12", 12);
		symbolTable.put("R13", 13);
		symbolTable.put("R14", 14);
		symbolTable.put("R15", 15);
		symbolTable.put("SCREEN", 16384);
		symbolTable.put("KBD", 24576);

		return symbolTable;
	}

	private static HashMap<String, String> createDestTable() {
		HashMap<String, String> destTable = new HashMap<String, String>();

		destTable.put("M", "001");
		destTable.put("D", "010");
		destTable.put("MD", "011");
		destTable.put("DM", "011");
		destTable.put("A", "100");
		destTable.put("AM", "101");
		destTable.put("MA", "101");
		destTable.put("AD", "110");
		destTable.put("DA", "110");
		destTable.put("AMD", "111");
		destTable.put("ADM", "111");
		destTable.put("MAD", "111");
		destTable.put("MDA", "111");
		destTable.put("DMA", "111");
		destTable.put("DAM", "111");

		return destTable;
	}

	private static HashMap<String, String> createCompTable() {
		HashMap<String, String> compTable = new HashMap<String, String>();

		compTable.put("0", "101010");
		compTable.put("1", "111111");
		compTable.put("-1", "111010");
		compTable.put("D", "001100");
		compTable.put("A", "110000");
		compTable.put("M", "110000");
		compTable.put("!D", "001101");
		compTable.put("!A", "110001");
		compTable.put("!M", "110001");
		compTable.put("-D", "001111");
		compTable.put("-A", "110011");
		compTable.put("-M", "110011");
		compTable.put("D+1", "011111");
		compTable.put("A+1", "110111");
		compTable.put("M+1", "110111");
		compTable.put("D-1", "001110");
		compTable.put("A-1", "110010");
		compTable.put("M-1", "110010");
		compTable.put("D+A", "000010");
		compTable.put("A+D", "000010");
		compTable.put("D+M", "000010");
		compTable.put("M+D", "000010");
		compTable.put("D-A", "010011");
		compTable.put("D-M", "010011");
		compTable.put("A-D", "000111");
		compTable.put("M-D", "000111");
		compTable.put("D&A", "000000");
		compTable.put("A&D", "000000");
		compTable.put("D&M", "000000");
		compTable.put("M&D", "000000");
		compTable.put("D|A", "010101");
		compTable.put("A|D", "010101");
		compTable.put("D|M", "010101");
		compTable.put("M|D", "010101");

		return compTable;
	}

	private static HashMap<String, String> createJumpTable() {
		HashMap<String, String> jumpTable = new HashMap<String, String>();

		jumpTable.put("JGT", "001");
		jumpTable.put("JEQ", "010");
		jumpTable.put("JGE", "011");
		jumpTable.put("JLT", "100");
		jumpTable.put("JNE", "101");
		jumpTable.put("JLE", "110");
		jumpTable.put("JMP", "111");

		return jumpTable;
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
