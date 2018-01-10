package project11;

/*
 *  Chung-An, Chiu 12172213 
 *  Project 11
 *  Nov 30, 2017
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class VMWriter {
	private BufferedWriter writeVMFile;

	/*
	 * Creates a new file and prepares it for writing
	 */
	public VMWriter(File outputFile) {
		try {
			this.writeVMFile = new BufferedWriter(new FileWriter(outputFile));
		} catch (IOException e) {
			System.err.println("Errors happened during file input/output! To help with this issue, please "
					+ "send your input file and the environment to chungan@uchicago.edu. Thank you!");
			System.out.println(outputFile.toString());
			e.printStackTrace();
		}
	}

	/*
	 * Writes a VM push command
	 */
	public void writePush(String segment, int index) {
		try {
			writeVMFile.write("push " + segment + " " + index + "\n");
		} catch (IOException e) {
			System.err.println("I/O problems when writing push");
			e.printStackTrace();
		}
	}

	/*
	 * Writes a VM pop command
	 */
	public void writePop(String segment, int index) {
		try {
			writeVMFile.write("pop " + segment + " " + index + "\n");
		} catch (IOException e) {
			System.err.println("I/O problems when writing pop");
			e.printStackTrace();
		}
	}

	/*
	 * Writes a VM arithmetic command
	 */
	public void writeArithmetic(String command) {
		try {
			writeVMFile.write(command + "\n");
		} catch (IOException e) {
			System.err.println("I/O problems when writing push");
			e.printStackTrace();
		}
	}

	/*
	 * Writes a VM label command
	 */
	public void writeLabel(String label) {
		try {
			writeVMFile.write("label " + label + "\n");
		} catch (IOException e) {
			System.err.println("I/O problems when writing push");
			e.printStackTrace();
		}
	}

	/*
	 * Writes a VM goto command
	 */
	public void writeGoto(String label) {
		try {
			writeVMFile.write("goto " + label + "\n");
		} catch (IOException e) {
			System.err.println("I/O problems when writing push");
			e.printStackTrace();
		}
	}

	/*
	 * Writes a VM if-goto command
	 */
	public void writeIf(String label) {
		try {
			writeVMFile.write("if-goto " + label + "\n");
		} catch (IOException e) {
			System.err.println("I/O problems when writing push");
			e.printStackTrace();
		}
	}

	/*
	 * Writes a VM call command
	 */
	public void writeCall(String name, int nArgs) {
		try {
			writeVMFile.write("call " + name + " " + nArgs + "\n");
		} catch (IOException e) {
			System.err.println("I/O problems when writing push");
			e.printStackTrace();
		}
	}

	/*
	 * Writes a VM function command
	 */
	public void writeFunction(String name, int nLocals) {
		try {
			writeVMFile.write("function " + name + " " + nLocals + "\n");
		} catch (IOException e) {
			System.err.println("I/O problems when writing push");
			e.printStackTrace();
		}
	}

	/*
	 * Writes a VM return command
	 */
	public void writeReturn() {
		try {
			writeVMFile.write("return\n");
		} catch (IOException e) {
			System.err.println("I/O problems when writing push");
			e.printStackTrace();
		}
	}

	/*
	 * Closes the output file
	 */
	public void close() {
		if (writeVMFile != null) {
			try {
				writeVMFile.close();
			} catch (IOException e) {
				System.err.println("I/O problems when closing file in VMWriter");
				e.printStackTrace();
			}
		}
	}
}
