package project11;

/*
 *  Chung-An, Chiu 12172213 
 *  Project 11
 *  Nov 30, 2017
 */

import java.io.File;
import java.text.ParseException;

public class CompilationEngine {
	private JackTokenizer jackTokenizer;
	private VMWriter vmWriter;
	private SymbolTable symbolTable;
	private String className;
	private static int numberOfLabels;

	public CompilationEngine(JackTokenizer jackTokenizer, File fileOutput) {
		this.jackTokenizer = jackTokenizer;

		vmWriter = new VMWriter(fileOutput);
		symbolTable = new SymbolTable();
	}

	/*
	 * Compilers a complete class
	 */
	public void CompileClass() {
		try {
			// class
			jackTokenizer.advance();
			if (!jackTokenizer.tokenType().equals("KEYWORD") || !jackTokenizer.keyWord().equals("class")) {
				printError("class");
			}

			// class name
			jackTokenizer.advance();
			if (!jackTokenizer.tokenType().equals("IDENTIFIER")) {
				printError("class name");
			} else {
				className = jackTokenizer.identifier();
			}

			// symbol {
			compileSymbol("{");

			compileClassVarDec();
			compileSubroutine();

			// symbol }
			compileSymbol("}");

			if (jackTokenizer.hasMoreTokens()) {
				throw new IllegalStateException("Shouldn't have more lines after class section");
			}

			vmWriter.close();
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile class");
			e.printStackTrace();
		}
	}

	/*
	 * Compilers a static declaration or a field declaration
	 */
	public void compileClassVarDec() {
		try {
			jackTokenizer.advance();
			// means the end of class, need to go back and rewind the pointer
			if (jackTokenizer.tokenType().equals("SYMBOL") && jackTokenizer.keyWord().equals("}")) {
				jackTokenizer.rewind();
				return;
			}

			// field and static variables should be the next as usual
			// but can also skip to the subroutines
			if (!jackTokenizer.tokenType().equals("KEYWORD")) {
				printError("Keywords");
			}

			// if we encounter the subroutines first, means that
			// there are no variables declared and should return
			// and rewind the index
			if (jackTokenizer.keyWord().equals("constructor") || jackTokenizer.keyWord().equals("function")
					|| jackTokenizer.keyWord().equals("method")) {
				jackTokenizer.rewind();
				return;
			}

			// officially start class var declaration

			// now we clear out all the possibilities and left for variables
			if (!jackTokenizer.keyWord().equals("static") && !jackTokenizer.keyWord().equals("field")) {
				printError("static or field");
			}

			String kind = jackTokenizer.keyWord();

			// deal with variable types
			String type = compileType();

			// deal with variable names
			while (true) {
				jackTokenizer.advance();
				if (!jackTokenizer.tokenType().equals("IDENTIFIER")) {
					printError("identifier");
				}
				symbolTable.define(jackTokenizer.identifier(), type, kind);
				// numberOfClassVariables++;

				// determine if it's ',' or ';'
				jackTokenizer.advance();
				if (!jackTokenizer.tokenType().equals("SYMBOL")
						|| (jackTokenizer.symbol() != ',' && jackTokenizer.symbol() != ';')) {
					printError(", or ;");
				}

				// writeToFile("SYMBOL", String.valueOf(jackTokenizer.symbol()));
				if (jackTokenizer.symbol() == ';') { // end of declaration
					break;
				}
			}

			// call recursion and will automatically return and proceed to compileSubroutine
			compileClassVarDec();

		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile class variables declaration");
			e.printStackTrace();
		}
	}

	/*
	 * Compiles a complete method function, or constructor
	 */
	public void compileSubroutine() {
		try {
			jackTokenizer.advance();
			// means the end of class, need to go back and rewind the pointer
			if (jackTokenizer.tokenType().equals("SYMBOL") && jackTokenizer.symbol() == '}') {
				jackTokenizer.rewind();
				return;
			}

			// deal with error
			if (!jackTokenizer.tokenType().equals("KEYWORD") || (!jackTokenizer.keyWord().equals("constructor")
					&& !jackTokenizer.keyWord().equals("function") && !jackTokenizer.keyWord().equals("method"))) {
				printError("constructor, function or method");
			}

			String subroutineType = jackTokenizer.keyWord();

			// for every subroutine starts a new table
			symbolTable.startSubroutine();

			// the first parameter
			if (subroutineType.equals("method")) {
				symbolTable.define("this", className, "argument");
			}

			// deal with return type
			jackTokenizer.advance();
			if (jackTokenizer.tokenType().equals("KEYWORD") && jackTokenizer.keyWord().equals("void")) { // void

			} else { // return type
				jackTokenizer.rewind();
				compileType();
			}

			// deal with subroutine name
			String name = "";
			jackTokenizer.advance();
			if (!jackTokenizer.tokenType().equals("IDENTIFIER")) {
				name = jackTokenizer.keyWord();
				jackTokenizer.advance();
				if (!jackTokenizer.tokenType().equals("IDENTIFIER")) {
					printError("subroutine name");
				} else {
					name += jackTokenizer.identifier();
				}
			}
			if (name.isEmpty() || name.equals("")) {
				name = jackTokenizer.identifier();
			}

			// (
			compileSymbol("(");

			// parameters
			compileParameterList();

			// )
			compileSymbol(")");

			// body statements
			compileSymbol("{");
			compileVarDec();
			vmWriter.writeFunction(className + "." + name, symbolTable.varCount("local"));
			if (subroutineType.equals("constructor")) {
				vmWriter.writePush("constant", symbolTable.varCount("field"));
				vmWriter.writeCall("Memory.alloc", 1);
				vmWriter.writePop("pointer", 0); // set the address to THIS
				// } else if (subroutineType == "function") {

			} else if (subroutineType.equals("method")) {
				vmWriter.writePush("argument", 0);
				vmWriter.writePop("pointer", 0);
			}
			compileStatements();
			compileSymbol("}");

			// call recursion until there is no more subroutines
			compileSubroutine();
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile subroutine");
			e.printStackTrace();
		}
	}

	/*
	 * Compiles a (possibly empty) parameter list, not including the enclosing "()"
	 */
	public void compileParameterList() {
		try {
			jackTokenizer.advance();

			// first check if there are no parameters
			if (jackTokenizer.tokenType().equals("SYMBOL") && jackTokenizer.symbol() == ')') {
				jackTokenizer.rewind();
				return;
			}

			jackTokenizer.rewind(); // rewind first so we can advance each time in the loop
			while (true) {
				String type = compileType();

				// variable name
				jackTokenizer.advance();
				if (!jackTokenizer.tokenType().equals("IDENTIFIER")) {
					printError("var name");
				}
				symbolTable.define(jackTokenizer.identifier(), type, "argument");

				// next should be , for another or ) for the end
				jackTokenizer.advance();
				if (!jackTokenizer.tokenType().equals("SYMBOL")
						|| (jackTokenizer.symbol() != ',' && jackTokenizer.symbol() != ')')) {
					printError(", or )");
				}
				if (jackTokenizer.symbol() == ',') {
					// writeToFile("SYMBOL", ",");
				} else { // end with )
					jackTokenizer.rewind(); // rewind and let the upper level advance
					break;
				}
			}
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile parameter list");
			e.printStackTrace();
		}

	}

	/*
	 * Compiles a var declaration
	 */
	public void compileVarDec() {
		try {
			jackTokenizer.advance();
			if (!jackTokenizer.tokenType().equals("KEYWORD") || !jackTokenizer.keyWord().equals("var")) { // no more
																											// vars
				jackTokenizer.rewind();
				return;
			}

			String type = compileType();

			while (true) {
				jackTokenizer.advance();
				if (!jackTokenizer.tokenType().equals("IDENTIFIER")) {
					printError("var name");
				}
				// writeToFile("IDENTIFIER", jackTokenizer.identifier());
				symbolTable.define(jackTokenizer.identifier(), type, "local");

				// check if have more vars with , or end with ;
				jackTokenizer.advance();
				if (!jackTokenizer.tokenType().equals("SYMBOL")
						|| (jackTokenizer.symbol() != ',' && jackTokenizer.symbol() != ';')) {
					printError(", or ;");
				}
				// writeToFile("SYMBOL", String.valueOf(jackTokenizer.symbol()));
				if (jackTokenizer.symbol() == ';') {
					break;
				}
			}

			// writeToFile("END_VAR_DEC", null);

			compileVarDec(); // recursion until the end
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile var declaration");
			e.printStackTrace();
		}

	}

	/*
	 * Compiles a sequence of statements, not including the enclosing "{}"
	 */
	public void compileStatements() {
		try {
			jackTokenizer.advance();
			if (jackTokenizer.tokenType().equals("SYMBOL") && jackTokenizer.symbol() == '}') { // end of statements
				jackTokenizer.rewind();
				return;
			}

			// deal with legal statements
			if (!jackTokenizer.tokenType().equals("KEYWORD")) {
				printError("let, if, while, do, return");
			}

			switch (jackTokenizer.keyWord()) {
			case "let":
				compileLet();
				break;
			case "if":
				compileIf();
				break;
			case "while":
				compileWhile();
				break;
			case "do":
				compileDo();
				break;
			case "return":
				compileReturn();
				break;
			default:
				printError("let, if, while, do, return");
				break;
			}

			// call recursion until the end of statements
			compileStatements();
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile statements");
			e.printStackTrace();
		}
	}

	/*
	 * Compiles a do statement
	 */
	public void compileDo() {
		compileCallSubroutine();
		compileSymbol(";");
		// return value
		vmWriter.writePop("temp", 0);
	}

	/*
	 * Compiles a let statement
	 */
	public void compileLet() {
		try {
			// variable name
			jackTokenizer.advance();
			if (!jackTokenizer.tokenType().equals("IDENTIFIER")) {
				printError("var name");
			}
			// writeToFile("IDENTIFIER", jackTokenizer.identifier());
			String name = jackTokenizer.identifier();

			jackTokenizer.advance();
			if (!jackTokenizer.tokenType().equals("SYMBOL")
					|| (jackTokenizer.symbol() != '[' && jackTokenizer.symbol() != '=')) {
				printError("[ or =");
			}

			boolean isArray = false;
			if (jackTokenizer.symbol() == '[') { // array
				isArray = true;

				vmWriter.writePush(symbolTable.kindOf(name), symbolTable.indexOf(name));

				compileExpression();

				// end with ]
				jackTokenizer.advance();
				if (!jackTokenizer.tokenType().equals("SYMBOL") || jackTokenizer.symbol() != ']') {
					printError("]");
				}

				vmWriter.writeArithmetic("add");
			}

			if (isArray) {
				jackTokenizer.advance(); // after an array, advance
			}

			compileExpression();

			// end with ;
			compileSymbol(";");

			if (isArray) {
				// deal with the rest from the expression
				vmWriter.writePop("temp", 0);
				vmWriter.writePop("pointer", 1);
				vmWriter.writePush("temp", 0);
				vmWriter.writePop("that", 0);
			} else {
				// pop to the target location
				vmWriter.writePop(symbolTable.kindOf(name), symbolTable.indexOf(name));
			}

		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile let");
			e.printStackTrace();
		}
	}

	/*
	 * Compiles a while statement
	 */
	public void compileWhile() {

		String label1 = "Label" + numberOfLabels++;
		String label2 = "Label" + numberOfLabels++;

		vmWriter.writeLabel(label1); // for continue

		compileSymbol("(");
		compileExpression();
		vmWriter.writeArithmetic("not"); // negate the expression
		compileSymbol(")");
		compileSymbol("{");

		vmWriter.writeIf(label2);
		compileStatements();
		vmWriter.writeGoto(label1);

		compileSymbol("}");
		vmWriter.writeLabel(label2);

	}

	/*
	 * Compiles a return statement
	 */
	public void compileReturn() {
		try {
			jackTokenizer.advance();
			if (jackTokenizer.tokenType().equals("SYMBOL") && jackTokenizer.symbol() == ';') { // void
				// push 0 for void subroutines
				vmWriter.writePush("constant", 0);
			} else {
				jackTokenizer.rewind();
				compileExpression();
				compileSymbol(";");
			}

			vmWriter.writeReturn();
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile return");
			e.printStackTrace();
		}
	}

	/*
	 * Compiles an if statement, possibly with a trailing else clause
	 */
	public void compileIf() {
		try {
			String label1 = "Label" + numberOfLabels++;
			String label2 = "Label" + numberOfLabels++;

			compileSymbol("(");
			compileExpression();
			vmWriter.writeArithmetic("not");
			vmWriter.writeIf(label1);
			compileSymbol(")");
			compileSymbol("{");

			// writeToFile("START_STATEMENTS", null);
			compileStatements();
			compileSymbol("}");
			vmWriter.writeGoto(label2);
			// writeToFile("END_STATEMENTS", null);

			vmWriter.writeLabel(label1);
			// else
			jackTokenizer.advance();
			if (jackTokenizer.tokenType().equals("KEYWORD") && jackTokenizer.keyWord().equals("else")) {
				compileSymbol("{");
				compileStatements();
				compileSymbol("}");
			} else {
				jackTokenizer.rewind();
			}
			vmWriter.writeLabel(label2);
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile if");
			e.printStackTrace();
		}
	}

	/*
	 * Compiles an expressioin, term (operation term)
	 */
	public void compileExpression() {
		try {
			compileTerm();

			while (true) {
				jackTokenizer.advance();
				if (jackTokenizer.tokenType().equals("SYMBOL") && (jackTokenizer.symbol() == '+'
						|| jackTokenizer.symbol() == '-' || jackTokenizer.symbol() == '*'
						|| jackTokenizer.symbol() == '/' || jackTokenizer.symbol() == '&'
						|| jackTokenizer.symbol() == '|' || jackTokenizer.symbol() == '>'
						|| jackTokenizer.symbol() == '<' || jackTokenizer.symbol() == '=')) {

					// Store it first to implement postfix
					String command = "";
					boolean isCommand = true;

					switch (jackTokenizer.symbol()) {
					case '+':
						command = "add";
						break;
					case '-':
						command = "sub";
						break;
					case '*':
						command = "Math.multiply 2";
						isCommand = false;
						break;
					case '/':
						command = "Math.divide 2";
						isCommand = false;
						break;
					case '&':
						command = "and";
						break;
					case '|':
						command = "or";
						break;
					case '>':
						command = "gt";
						break;
					case '<':
						command = "lt";
						break;
					case '=':
						command = "eq";
						break;
					default:
						throw new IllegalArgumentException("The operant is wrong while compileing expression!");
					}

					compileTerm();
					if (isCommand) {
						vmWriter.writeArithmetic(command);
					} else {
						vmWriter.writeCall(command.substring(0, command.length() - 2),
								Integer.parseInt(command.substring(command.length() - 1, command.length())));
					}
				} else {
					jackTokenizer.rewind();
					break;
				}
			}
			// writeToFile("END_EXPRESSION", null);
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile expression");
			e.printStackTrace();
		}
	}

	/*
	 * Compiles a term, LL(2), term can mean a lot of things 1. Simple constant 2.
	 * Var name 3. Array constant 4. Call subroutine 5. Expression 6. Single
	 * operation
	 */
	public void compileTerm() {
		try {
			jackTokenizer.advance();
			if (jackTokenizer.tokenType().equals("IDENTIFIER")) {
				// Var name, Array constant or Call subroutine
				String identifier = jackTokenizer.identifier();

				jackTokenizer.advance(); // have to use the next symbol to determine
				if (jackTokenizer.tokenType().equals("SYMBOL") && jackTokenizer.symbol() == '[') {
					// array
					vmWriter.writePush(symbolTable.kindOf(identifier), symbolTable.indexOf(identifier));

					compileExpression();
					compileSymbol("]");

					vmWriter.writeArithmetic("add");
					vmWriter.writePop("pointer", 1); // array uses that
					vmWriter.writePush("that", 0);

				} else if (jackTokenizer.tokenType().equals("SYMBOL")
						&& (jackTokenizer.symbol() == '(' || jackTokenizer.symbol() == '.')) {
					// call subroutine, rewind twice to make the compile function right
					jackTokenizer.rewind();
					jackTokenizer.rewind();
					compileCallSubroutine();
				} else {
					// var name
					jackTokenizer.rewind();
					vmWriter.writePush(symbolTable.kindOf(identifier), symbolTable.indexOf(identifier));
				}

			} else {
				// Simple constant, Expression or Simple operation
				if (jackTokenizer.tokenType().equals("INT_CONST")) {
					vmWriter.writePush("constant", jackTokenizer.intVal());
				} else if (jackTokenizer.tokenType().equals("STRING_CONST")) {
					// writeToFile("STRING_CONST", jackTokenizer.stringVal());

					// allocate memory in heap for a string
					vmWriter.writePush("constant", jackTokenizer.stringVal().length());
					vmWriter.writeCall("String.new", 1);

					for (int i = 0; i < jackTokenizer.stringVal().length(); i++) {
						vmWriter.writePush("constant", jackTokenizer.stringVal().charAt(i));
						vmWriter.writeCall("String.appendChar", 2);
					}
				} else if (jackTokenizer.tokenType().equals("KEYWORD")
						&& (jackTokenizer.keyWord().equals("true") || jackTokenizer.keyWord().equals("false")
								|| jackTokenizer.keyWord().equals("null") || jackTokenizer.keyWord().equals("this"))) {
					// writeToFile("KEYWORD", jackTokenizer.keyWord());
					switch (jackTokenizer.keyWord()) {
					case "true":
						// can't push constant -1
						vmWriter.writePush("constant", 0);
						vmWriter.writeArithmetic("not");
						break;
					case "false":
					case "null":
						vmWriter.writePush("constant", 0);
						break;
					case "this":
						vmWriter.writePush("pointer", 0);
						break;
					default:
						throw new IllegalArgumentException("token should be true/false/null/this");
					}
				} else if (jackTokenizer.tokenType().equals("SYMBOL")
						&& (jackTokenizer.symbol() == '-' || jackTokenizer.symbol() == '~')) {
					char symbol = jackTokenizer.symbol();
					compileTerm();
					switch (symbol) {
					case '-':
						vmWriter.writeArithmetic("neg");
						break;
					case '~':
						vmWriter.writeArithmetic("not");
						break;
					default:
						break;
					}
				} else if (jackTokenizer.tokenType().equals("SYMBOL") && jackTokenizer.symbol() == '(') {
					// expression in parenthesis
					compileExpression();
					compileSymbol(")");
				} else {
					printError("int, string, keyword, (expression) or operation");
				}
			}

			// writeToFile("END_TERM", null);
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile if");
			e.printStackTrace();
		}
	}

	/*
	 * Compiles a comma-separated list of expression
	 */
	public int compileExpressionList() {
		int numberOfArguments = 0;

		try {
			jackTokenizer.advance();
			if (jackTokenizer.tokenType().equals("SYMBOL") && jackTokenizer.symbol() == ')') {
				// no expression
				jackTokenizer.rewind();
				return numberOfArguments;
			}

			// recalibrate for while loop
			jackTokenizer.rewind();
			compileExpression();
			numberOfArguments++; // each expression increment 1

			// can have more expressions
			while (true) {
				jackTokenizer.advance();
				if (jackTokenizer.tokenType().equals("SYMBOL") && jackTokenizer.symbol() == ',') {
					// writeToFile("SYMBOL", ",");
					compileExpression(); // every turn is an expression
					numberOfArguments++;
				} else {
					jackTokenizer.rewind();
					break;
				}
			}

		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile expression list");
			e.printStackTrace();
		}

		return numberOfArguments;
	}

	/*
	 * Call to a subroutine
	 */
	private void compileCallSubroutine() {
		try {
			jackTokenizer.advance();
			if (jackTokenizer.tokenType() != "IDENTIFIER") {
				printError("subroutine name, class name or var name");
			}
			String name = jackTokenizer.identifier();
			int nArgs = 0;

			jackTokenizer.advance();
			if (jackTokenizer.tokenType().equals("SYMBOL") && jackTokenizer.symbol() == '(') {
				// push 0
				vmWriter.writePush("pointer", 0);

				// with no prefix
				nArgs = compileExpressionList() + 1;

				// )
				compileSymbol(")");

				vmWriter.writeCall(className + "." + name, nArgs);
			} else if (jackTokenizer.tokenType().equals("SYMBOL") && jackTokenizer.symbol() == '.') {
				// the first identifier is prefix, another class or a var
				String subroutineName = "";
				jackTokenizer.advance();
				if (!jackTokenizer.tokenType().equals("IDENTIFIER")) {
					subroutineName = jackTokenizer.keyWord();
					jackTokenizer.advance();
					if (!jackTokenizer.tokenType().equals("IDENTIFIER")) {
						printError("subroutine name, class name or var name");
					} else {
						subroutineName += jackTokenizer.identifier();
					}
				}
				if (subroutineName.isEmpty() || subroutineName == null) {
					subroutineName = jackTokenizer.identifier();
				}

				String type = symbolTable.typeOf(name);
				if (type.equals("int") || type.equals("char") || type.equals("boolean") || type.equals("void")) {
					printError("can't use the build-in type");
				} else if (type.equals("")) {
					name = name + "." + subroutineName;
				} else {
					vmWriter.writePush(symbolTable.kindOf(name), symbolTable.indexOf(name));
					name = symbolTable.typeOf(name) + "." + subroutineName;
					nArgs = 1;
				}

				compileSymbol("(");

				nArgs += compileExpressionList();
				compileSymbol(")");

				vmWriter.writeCall(name, nArgs);
			} else {
				printError("( or .");
			}
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile var declaration");
			e.printStackTrace();
		}
	}

	/*
	 * shared function for symbol
	 */
	private void compileSymbol(String symbol) {
		try {
			jackTokenizer.advance();
			if (!jackTokenizer.tokenType().equals("SYMBOL") || !String.valueOf(jackTokenizer.symbol()).equals(symbol)) {
				printError(symbol);
			}
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile symbol");
			e.printStackTrace();
		}
	}

	/*
	 * shared function for determine variable type
	 */
	private String compileType() {
		try {
			jackTokenizer.advance();
			// boolean isType = false;
			if (jackTokenizer.tokenType().equals("KEYWORD") && (jackTokenizer.keyWord().equals("int")
					|| jackTokenizer.keyWord().equals("char") || jackTokenizer.keyWord().equals("boolean"))) {
				// isType = true;
				return jackTokenizer.keyWord();
			}

			if (jackTokenizer.tokenType().equals("IDENTIFIER")) {
				// isType = true;
				return jackTokenizer.identifier();

			}

			// not matching any variable types
			printError("int or char or boolean or class name");

		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile type");
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * shared error message function, print out the expected value
	 */
	private void printError(String expect) {
		throw new IllegalStateException("The token should be " + expect + " instead of " + jackTokenizer.tokenType());
	}
}
