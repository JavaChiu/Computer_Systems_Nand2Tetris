package project10;

/*
 *  Chung-An, Chiu 12172213 
 *  Project 10
 *  Nov 16, 2017
 */

import java.io.File;
import java.io.FilenameFilter;
import java.text.ParseException;

public class JackAnalyzer {

	public static void main(String[] args) {

		File[] inputFileList = readFile(args);

		for (File f : inputFileList) {
			String fileOutput = f.getAbsolutePath().substring(0, f.getAbsolutePath().lastIndexOf('.')) + ".xml";
			String tokensOutput = f.getAbsolutePath().substring(0, f.getAbsolutePath().lastIndexOf('.')) + "T.xml";

			CompilationEngine compilationEngine = new CompilationEngine(new JackTokenizer(f), new File(fileOutput),
					new File(tokensOutput));
			compilationEngine.CompileClass(); // directly call compile class first

		}

	}

	private static File[] readFile(String[] args) {
		File[] inputFileList = null;
		if (args.length == 1) {
			File inputFile = new File(args[0]);
			if (!inputFile.exists()) {
				System.err.println("Cannot find the file! Please check if it exist!");
				System.exit(1);
			} else {
				if (inputFile.isDirectory()) {
					inputFileList = inputFile.listFiles(new FilenameFilter() {
						@Override
						public boolean accept(File dir, String name) {
							return name.endsWith(".jack");
						}
					});
				} else {
					// check file name
					if (!args[0].endsWith(".jack")) {
						System.err.println("Please input .jack file! Thank you!");
						System.exit(1);
					}

					inputFileList = new File[] { inputFile };
				}
			}
		} else {
			System.err.println("The arguments passed in are wrong! Please see README.txt!");
			System.exit(1);
		}

		return inputFileList;
	}
}
