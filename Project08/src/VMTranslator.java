
/*
 *  Chung-An, Chiu 12172213 
 *  Project 8
 *  Nov 2, 2017
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

public class VMTranslator {
	private static String progName;
	private static int numOfCompare;

	public static void main(String[] args) {
		File[] inputFileList = null;
		File inputFile = null;
		String outputFile = null;

		// Check arguments
		if (args.length > 0) {
			inputFile = new File(args[0]);
			if (!inputFile.exists()) {
				System.err.println("Cannot find the file! Please check if it exist!");
				System.exit(1);
			} else {
				if (inputFile.isDirectory()) {
					inputFileList = inputFile.listFiles(new FilenameFilter() {
						@Override
						public boolean accept(File dir, String name) {
							return name.endsWith(".vm");
						}
					});
					outputFile = inputFile + "/" + inputFile + ".asm"; // use the directory's name
				} else {
					// check file name
					if (!args[0].endsWith(".vm")) {
						System.err.println("Please input .vm file! Thank you!");
						System.exit(1);
					}

					inputFileList = new File[] { inputFile };
					String[] pathArray = args[0].split("/");
					progName = pathArray[pathArray.length - 1].substring(0,
							pathArray[pathArray.length - 1].length() - 3);
					if (pathArray.length == 1) { // relative
						outputFile = progName + ".asm"; // .vm->.asm
					} else { // absolute
						outputFile = args[0].substring(0, args[0].length() - 3) + ".asm";
					}
				}
			}
		} else {
			System.err.println("The arguments passed in are wrong! Please see README.txt!");
			System.exit(1);
		}

		BufferedReader bufferedReader = null;
		BufferedWriter bufferedWriter = null;
		try {
			bufferedWriter = new BufferedWriter(new FileWriter(outputFile));
			if (inputFileList.length > 1) { // multiple files needs bootstrap and sys.init
				ArrayList<String> assemblyCode = doBootstrap();
				bufferedWriter.write("//bootstrap\n");
				for (int i = 0; i < assemblyCode.size(); i++) {
					bufferedWriter.write(assemblyCode.get(i) + "\n");
				}
				bufferedWriter.write("//callSys.init0\n");
				assemblyCode = doCallFunction("callSys.init0");
				for (int i = 0; i < assemblyCode.size(); i++) {
					bufferedWriter.write(assemblyCode.get(i) + "\n");
				}
			}
			for (File f : inputFileList) {
				if (inputFileList.length > 1) {
					String temp = f.toString();
					String[] pathArray = temp.split("/");
					progName = pathArray[pathArray.length - 1].substring(0,
							pathArray[pathArray.length - 1].length() - 3);
				}
				bufferedReader = new BufferedReader(new FileReader(f));
				String currentLine;
				while ((currentLine = bufferedReader.readLine()) != null) {
					currentLine = getRidOfWhiteSpaceAndComment(currentLine, true);
					if (currentLine.equals("")) {
						continue;
					}
					bufferedWriter.write("//" + currentLine + "\n"); // comment for reading

					ArrayList<String> assemblyCode = null;

					if (currentLine.startsWith("push") || currentLine.startsWith("pop")) {
						String firstArg = null;
						if (currentLine.startsWith("push")) {
							firstArg = "push";
						} else {
							firstArg = "pop";
						}
						if (currentLine.indexOf("argument") >= 0) {
							int secondArgIndex = currentLine.indexOf("argument");
							assemblyCode = doMemoryAccess(firstArg, "argument",
									currentLine.substring(secondArgIndex + 8, currentLine.length()));
						}
						if (currentLine.indexOf("local") >= 0) {
							int secondArgIndex = currentLine.indexOf("local");
							assemblyCode = doMemoryAccess(firstArg, "local",
									currentLine.substring(secondArgIndex + 5, currentLine.length()));
						}
						if (currentLine.indexOf("static") >= 0) {
							int secondArgIndex = currentLine.indexOf("static");
							assemblyCode = doMemoryAccess(firstArg, "static",
									currentLine.substring(secondArgIndex + 6, currentLine.length()));
						}
						if (currentLine.indexOf("constant") >= 0) {
							int secondArgIndex = currentLine.indexOf("constant");
							assemblyCode = doMemoryAccess(firstArg, "constant",
									currentLine.substring(secondArgIndex + 8, currentLine.length()));
						}
						if (currentLine.indexOf("this") >= 0) {
							int secondArgIndex = currentLine.indexOf("this");
							assemblyCode = doMemoryAccess(firstArg, "this",
									currentLine.substring(secondArgIndex + 4, currentLine.length()));
						}
						if (currentLine.indexOf("that") >= 0) {
							int secondArgIndex = currentLine.indexOf("that");
							assemblyCode = doMemoryAccess(firstArg, "that",
									currentLine.substring(secondArgIndex + 4, currentLine.length()));
						}
						if (currentLine.indexOf("pointer") >= 0) {
							int secondArgIndex = currentLine.indexOf("pointer");
							assemblyCode = doMemoryAccess(firstArg, "pointer",
									currentLine.substring(secondArgIndex + 7, currentLine.length()));
						}
						if (currentLine.indexOf("temp") >= 0) {
							int secondArgIndex = currentLine.indexOf("temp");
							assemblyCode = doMemoryAccess(firstArg, "temp",
									currentLine.substring(secondArgIndex + 4, currentLine.length()));
						}
					} else if (currentLine.startsWith("function")) {
						assemblyCode = doFunction(currentLine);
					} else if (currentLine.startsWith("return")) {
						assemblyCode = doReturn(currentLine);
					} else if (currentLine.startsWith("call")) {
						assemblyCode = doCallFunction(currentLine);
					} else if (currentLine.startsWith("label")) {
						assemblyCode = doLabel(currentLine);
					} else if (currentLine.startsWith("goto")) {
						assemblyCode = doGoTo(currentLine);
					} else if (currentLine.startsWith("if-goto")) {
						assemblyCode = doIfGoTo(currentLine);
					} else {
						assemblyCode = doStackArithmetic(currentLine);
					}

					for (int i = 0; i < assemblyCode.size(); i++) {
						bufferedWriter.write(assemblyCode.get(i) + "\n");
					}
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

	private static ArrayList<String> doBootstrap() {
		ArrayList<String> assemblyCode = new ArrayList<String>();
		assemblyCode.add("@256");
		assemblyCode.add("D=A");
		assemblyCode.add("@SP");
		assemblyCode.add("M=D");
		return assemblyCode;
	}

	private static ArrayList<String> doFunction(String s) {
		ArrayList<String> assemblyCode = new ArrayList<String>();

		int len = s.length();
		assemblyCode.add("(" + s.substring(8, len - 1) + ")"); // create label

		int n = Integer.parseInt(s.substring(len - 1, len)); // count the local variables
		for (int i = 0; i < n; i++) { // initialize them to 0
			assemblyCode.add("@SP");
			assemblyCode.add("A=M");
			assemblyCode.add("M=0");
			assemblyCode.add("@SP");
			assemblyCode.add("M=M+1");
		}

		return assemblyCode;
	}

	private static ArrayList<String> doReturn(String s) {
		ArrayList<String> assemblyCode = new ArrayList<String>();

		assemblyCode.add("@LCL");
		assemblyCode.add("D=M");
		assemblyCode.add("@endFrame");
		assemblyCode.add("M=D"); // store LCL in endFrame cause if no argument the return address will be
									// override
		assemblyCode.add("@5");
		assemblyCode.add("D=D-A"); // endFrame - 5
		assemblyCode.add("A=D");
		assemblyCode.add("D=M");
		assemblyCode.add("@retAddr");
		assemblyCode.add("M=D");

		assemblyCode.add("@SP"); // store the return value in D and also decrement 1
		assemblyCode.add("AM=M-1");
		assemblyCode.add("D=M");
		assemblyCode.add("@ARG"); // put the return value in ARG
		assemblyCode.add("A=M");
		assemblyCode.add("M=D");

		assemblyCode.add("@ARG");
		assemblyCode.add("D=M+1");
		assemblyCode.add("@SP");
		assemblyCode.add("M=D"); // sp should be at the place where arg+1

		assemblyCode.add("@endFrame");
		assemblyCode.add("D=M-1");
		assemblyCode.add("A=D");
		assemblyCode.add("D=M");
		assemblyCode.add("@THAT");
		assemblyCode.add("M=D"); // @THAT = *(endFrame-1)

		assemblyCode.add("@endFrame");
		assemblyCode.add("D=M");
		assemblyCode.add("@2");
		assemblyCode.add("D=D-A");
		assemblyCode.add("A=D");
		assemblyCode.add("D=M");
		assemblyCode.add("@THIS");
		assemblyCode.add("M=D");

		assemblyCode.add("@endFrame");
		assemblyCode.add("D=M");
		assemblyCode.add("@3");
		assemblyCode.add("D=D-A");
		assemblyCode.add("A=D");
		assemblyCode.add("D=M");
		assemblyCode.add("@ARG");
		assemblyCode.add("M=D");

		assemblyCode.add("@endFrame");
		assemblyCode.add("D=M");
		assemblyCode.add("@4");
		assemblyCode.add("D=D-A");
		assemblyCode.add("A=D");
		assemblyCode.add("D=M");
		assemblyCode.add("@LCL");
		assemblyCode.add("M=D");

		assemblyCode.add("@retAddr"); // jump back to the returnAddress
		assemblyCode.add("A=M"); // dereference
		assemblyCode.add("0;JMP");

		return assemblyCode;
	}

	private static ArrayList<String> doCallFunction(String s) {
		ArrayList<String> assemblyCode = new ArrayList<String>();
		int len = s.length();
		int numOfArgs = Integer.parseInt(s.substring(len - 1, len));
		String functionName = s.substring(4, len - 1);

		numOfCompare++;
		assemblyCode.add("@RETURN" + numOfCompare);
		assemblyCode.add("D=A");
		assemblyCode.add("@SP");
		assemblyCode.add("A=M");
		assemblyCode.add("M=D"); // push return address to stack
		assemblyCode.add("@SP");
		assemblyCode.add("M=M+1");

		assemblyCode.add("@LCL");
		assemblyCode.add("D=M");
		assemblyCode.add("@SP");
		assemblyCode.add("A=M");
		assemblyCode.add("M=D"); // push LCL
		assemblyCode.add("@SP");
		assemblyCode.add("M=M+1");

		assemblyCode.add("@ARG");
		assemblyCode.add("D=M");
		assemblyCode.add("@SP");
		assemblyCode.add("A=M");
		assemblyCode.add("M=D"); // push ARG
		assemblyCode.add("@SP");
		assemblyCode.add("M=M+1");

		assemblyCode.add("@THIS");
		assemblyCode.add("D=M");
		assemblyCode.add("@SP");
		assemblyCode.add("A=M");
		assemblyCode.add("M=D"); // push THIS
		assemblyCode.add("@SP");
		assemblyCode.add("M=M+1");

		assemblyCode.add("@THAT");
		assemblyCode.add("D=M");
		assemblyCode.add("@SP");
		assemblyCode.add("A=M");
		assemblyCode.add("M=D"); // push THAT
		assemblyCode.add("@SP");
		assemblyCode.add("M=M+1");

		assemblyCode.add("D=M"); // set ARG = sp-5-numOfArgs
		assemblyCode.add("@5");
		assemblyCode.add("D=D-A");
		assemblyCode.add("@" + numOfArgs);
		assemblyCode.add("D=D-A");
		assemblyCode.add("@ARG");
		assemblyCode.add("M=D");

		assemblyCode.add("@SP"); // set LCL = sp
		assemblyCode.add("D=M");
		assemblyCode.add("@LCL");
		assemblyCode.add("M=D");

		assemblyCode.add("@" + functionName);
		assemblyCode.add("0;JMP");
		assemblyCode.add("(RETURN" + numOfCompare + ")");

		return assemblyCode;
	}

	private static ArrayList<String> doGoTo(String s) {
		ArrayList<String> assemblyCode = new ArrayList<String>();
		assemblyCode.add("@" + s.substring(4));
		assemblyCode.add("0;JMP");
		return assemblyCode;
	}

	/*
	 * This function deals with if-goto
	 */
	private static ArrayList<String> doIfGoTo(String s) {
		ArrayList<String> assemblyCode = new ArrayList<String>();
		assemblyCode.add("@SP");
		assemblyCode.add("AM=M-1"); // dereference and decrement 1
		assemblyCode.add("D=M");
		assemblyCode.add("@" + s.substring(7)); // find the end of if-goto, append string
		assemblyCode.add("D;JNE");
		return assemblyCode;
	}

	/*
	 * This function deals with label command
	 */
	private static ArrayList<String> doLabel(String s) {
		ArrayList<String> assemblyCode = new ArrayList<String>();
		assemblyCode.add("(" + s.substring(5) + ")");
		return assemblyCode;
	}

	/*
	 * This function deals with memory access command
	 */
	private static ArrayList<String> doMemoryAccess(String pushOrPop, String location, String numString) {
		ArrayList<String> assemblyCode = new ArrayList<String>();

		if (pushOrPop == "push") {
			switch (location) {
			case "argument":
				assemblyCode.add("@ARG");
				assemblyCode.add("D=M");
				assemblyCode.add("@" + numString);
				assemblyCode.add("A=D+A");
				assemblyCode.add("D=M");
				break;
			case "local":
				assemblyCode.add("@LCL");
				assemblyCode.add("D=M");
				assemblyCode.add("@" + numString);
				assemblyCode.add("A=D+A");
				assemblyCode.add("D=M");
				break;
			case "static":
				assemblyCode.add("@" + progName + "." + numString);
				assemblyCode.add("D=M");
				break;
			case "constant":
				assemblyCode.add("@" + numString);
				assemblyCode.add("D=A");
				break;
			case "this":
				assemblyCode.add("@THIS");
				assemblyCode.add("D=M");
				assemblyCode.add("@" + numString);
				assemblyCode.add("A=D+A");
				assemblyCode.add("D=M");
				break;
			case "that":
				assemblyCode.add("@THAT");
				assemblyCode.add("D=M");
				assemblyCode.add("@" + numString);
				assemblyCode.add("A=D+A");
				assemblyCode.add("D=M");
				break;
			case "pointer":
				if (numString.equals("0")) {
					assemblyCode.add("@3");
					assemblyCode.add("D=M");
				} else if (numString.equals("1")) {
					assemblyCode.add("@4");
					assemblyCode.add("D=M");
				}
				break;
			case "temp":
				assemblyCode.add("@" + ((int) 5 + Integer.parseInt(numString)));
				assemblyCode.add("D=M");
				break;
			default:
				break;
			}
			assemblyCode.add("@SP");
			assemblyCode.add("A=M");
			assemblyCode.add("M=D");
			assemblyCode.add("@SP");
			assemblyCode.add("M=M+1");
		} else if (pushOrPop == "pop") {
			switch (location) {
			case "argument":
				if (numString.equals("0")) {
					assemblyCode.add("@SP");
					assemblyCode.add("AM=M-1");
					assemblyCode.add("D=M");
					assemblyCode.add("@ARG");
					assemblyCode.add("A=M");
				} else {
					assemblyCode.add("@" + numString);
					assemblyCode.add("D=A");
					assemblyCode.add("@ARG");
					assemblyCode.add("A=M");
					assemblyCode.add("D=D+A");
					assemblyCode.add("@ARG");
					assemblyCode.add("M=D");
					assemblyCode.add("@SP");
					assemblyCode.add("AM=M-1");
					assemblyCode.add("D=M");
					assemblyCode.add("@ARG");
					assemblyCode.add("A=M");
					assemblyCode.add("M=D");
					assemblyCode.add("@" + numString);
					assemblyCode.add("D=A");
					assemblyCode.add("@ARG");
					assemblyCode.add("A=M");
					assemblyCode.add("D=A-D");
					assemblyCode.add("@ARG");
				}
				assemblyCode.add("M=D");
				break;
			case "local":
				if (numString.equals("0")) {
					assemblyCode.add("@SP");
					assemblyCode.add("AM=M-1");
					assemblyCode.add("D=M");
					assemblyCode.add("@LCL");
					assemblyCode.add("A=M");
				} else {
					assemblyCode.add("@" + numString);
					assemblyCode.add("D=A");
					assemblyCode.add("@LCL");
					assemblyCode.add("A=M");
					assemblyCode.add("D=D+A");
					assemblyCode.add("@LCL");
					assemblyCode.add("M=D");
					assemblyCode.add("@SP");
					assemblyCode.add("AM=M-1");
					assemblyCode.add("D=M");
					assemblyCode.add("@LCL");
					assemblyCode.add("A=M");
					assemblyCode.add("M=D");
					assemblyCode.add("@" + numString);
					assemblyCode.add("D=A");
					assemblyCode.add("@LCL");
					assemblyCode.add("A=M");
					assemblyCode.add("D=A-D");
					assemblyCode.add("@LCL");
				}
				assemblyCode.add("M=D");
				break;
			case "static":
				assemblyCode.add("@SP");
				assemblyCode.add("AM=M-1");
				assemblyCode.add("D=M");
				assemblyCode.add("@" + progName + "." + numString);
				assemblyCode.add("M=D");
				break;
			// case "constant":
			// break;
			case "this":
				if (numString.equals("0")) {
					assemblyCode.add("@SP");
					assemblyCode.add("AM=M-1");
					assemblyCode.add("D=M");
					assemblyCode.add("@THIS");
					assemblyCode.add("A=M");
				} else {
					assemblyCode.add("@" + numString);
					assemblyCode.add("D=A");
					assemblyCode.add("@THIS");
					assemblyCode.add("A=M");
					assemblyCode.add("D=D+A");
					assemblyCode.add("@THIS");
					assemblyCode.add("M=D");
					assemblyCode.add("@SP");
					assemblyCode.add("AM=M-1");
					assemblyCode.add("D=M");
					assemblyCode.add("@THIS");
					assemblyCode.add("A=M");
					assemblyCode.add("M=D");
					assemblyCode.add("@" + numString);
					assemblyCode.add("D=A");
					assemblyCode.add("@THIS");
					assemblyCode.add("A=M");
					assemblyCode.add("D=A-D");
					assemblyCode.add("@THIS");
				}
				assemblyCode.add("M=D");
				break;
			case "that":
				assemblyCode.add("@THAT");
				if (numString.equals("0")) {
					assemblyCode.add("@SP");
					assemblyCode.add("AM=M-1");
					assemblyCode.add("D=M");
					assemblyCode.add("@THAT");
					assemblyCode.add("A=M");
				} else {
					assemblyCode.add("@" + numString);
					assemblyCode.add("D=A");
					assemblyCode.add("@THAT");
					assemblyCode.add("A=M");
					assemblyCode.add("D=D+A");
					assemblyCode.add("@THAT");
					assemblyCode.add("M=D");
					assemblyCode.add("@SP");
					assemblyCode.add("AM=M-1");
					assemblyCode.add("D=M");
					assemblyCode.add("@THAT");
					assemblyCode.add("A=M");
					assemblyCode.add("M=D");
					assemblyCode.add("@" + numString);
					assemblyCode.add("D=A");
					assemblyCode.add("@THAT");
					assemblyCode.add("A=M");
					assemblyCode.add("D=A-D");
					assemblyCode.add("@THAT");
				}
				assemblyCode.add("M=D");
				break;
			case "pointer":
				assemblyCode.add("@SP");
				assemblyCode.add("AM=M-1");
				assemblyCode.add("D=M");
				if (numString.equals("0")) {
					assemblyCode.add("@3");
				} else if (numString.equals("1")) {
					assemblyCode.add("@4");
				}
				assemblyCode.add("M=D");
				break;
			case "temp":
				assemblyCode.add("@SP");
				assemblyCode.add("AM=M-1");
				assemblyCode.add("D=M");
				assemblyCode.add("@" + ((int) Integer.parseInt(numString) + 5));
				assemblyCode.add("M=D");
				break;
			default:
				break;
			}
		}

		return assemblyCode;
	}

	private static ArrayList<String> doStackArithmetic(String arithmetic) {
		ArrayList<String> assemblyCode = new ArrayList<String>();

		switch (arithmetic) {
		case "add":
			assemblyCode.add("@SP");
			assemblyCode.add("AM=M-1");
			assemblyCode.add("D=M");
			assemblyCode.add("A=A-1");
			assemblyCode.add("M=M+D");
			break;
		case "sub":
			assemblyCode.add("@SP");
			assemblyCode.add("AM=M-1");
			assemblyCode.add("D=M");
			assemblyCode.add("A=A-1");
			assemblyCode.add("M=M-D");
			break;
		case "neg":
			assemblyCode.add("@SP");
			assemblyCode.add("A=M-1");
			assemblyCode.add("M=-M");
			break;
		case "eq":
			assemblyCode.add("@SP");
			assemblyCode.add("AM=M-1");
			assemblyCode.add("D=M");
			assemblyCode.add("A=A-1");
			assemblyCode.add("D=D-M");
			assemblyCode.add("@TRUE" + numOfCompare);
			assemblyCode.add("D;JEQ");
			assemblyCode.add("@SP"); // FALSE
			assemblyCode.add("A=M-1");
			assemblyCode.add("M=0");
			assemblyCode.add("@END" + numOfCompare);
			assemblyCode.add("0;JMP");
			assemblyCode.add("(TRUE" + numOfCompare + ")");
			assemblyCode.add("@SP");
			assemblyCode.add("A=M-1");
			assemblyCode.add("M=-1");
			assemblyCode.add("(END" + numOfCompare + ")");
			numOfCompare++;
			break;
		case "gt":
			assemblyCode.add("@SP");
			assemblyCode.add("AM=M-1"); // D=y
			assemblyCode.add("D=M");
			assemblyCode.add("A=A-1");
			assemblyCode.add("D=M-D"); // D=x-y
			assemblyCode.add("@TRUE" + numOfCompare);
			assemblyCode.add("D;JGT");
			assemblyCode.add("@SP"); // FALSE
			assemblyCode.add("A=M-1");
			assemblyCode.add("M=0");
			assemblyCode.add("@END" + numOfCompare);
			assemblyCode.add("0;JMP");
			assemblyCode.add("(TRUE" + numOfCompare + ")");
			assemblyCode.add("@SP");
			assemblyCode.add("A=M-1");
			assemblyCode.add("M=-1");
			assemblyCode.add("(END" + numOfCompare + ")");
			numOfCompare++;
			break;
		case "lt":
			assemblyCode.add("@SP");
			assemblyCode.add("AM=M-1");
			assemblyCode.add("D=M"); // D=y
			assemblyCode.add("A=A-1");
			assemblyCode.add("D=M-D"); // D=x-y
			assemblyCode.add("@TRUE" + numOfCompare);
			assemblyCode.add("D;JLT");
			assemblyCode.add("@SP"); // FALSE
			assemblyCode.add("A=M-1");
			assemblyCode.add("M=0");
			assemblyCode.add("@END" + numOfCompare);
			assemblyCode.add("0;JMP");
			assemblyCode.add("(TRUE" + numOfCompare + ")");
			assemblyCode.add("@SP");
			assemblyCode.add("A=M-1");
			assemblyCode.add("M=-1");
			assemblyCode.add("(END" + numOfCompare + ")");
			numOfCompare++;
			break;
		case "and":
			assemblyCode.add("@SP");
			assemblyCode.add("AM=M-1");
			assemblyCode.add("D=M");
			assemblyCode.add("A=A-1");
			assemblyCode.add("M=M&D");
			break;
		case "or":
			assemblyCode.add("@SP");
			assemblyCode.add("AM=M-1");
			assemblyCode.add("D=M");
			assemblyCode.add("A=A-1");
			assemblyCode.add("M=M|D");
			break;
		case "not":
			assemblyCode.add("@SP");
			assemblyCode.add("A=M-1");
			assemblyCode.add("M=!M");
			break;
		default:
			break;
		}
		return assemblyCode;
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
