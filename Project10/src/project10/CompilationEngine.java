package project10;

/*
 *  Chung-An, Chiu 12172213 
 *  Project 10
 *  Nov 16, 2017
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;

public class CompilationEngine {
	private JackTokenizer jackTokenizer;
	private BufferedWriter writeFile;
	private BufferedWriter writeTokens;

	public CompilationEngine(JackTokenizer jackTokenizer, File fileOutput, File tokensOutput) {
		this.jackTokenizer = jackTokenizer;

		try {
			writeFile = new BufferedWriter(new FileWriter(fileOutput));
			writeTokens = new BufferedWriter(new FileWriter(tokensOutput));
		} catch (IOException e) {
			System.err.println("Errors happened during file input/output! To help with this issue, please "
					+ "send your input file and the environment to chungan@uchicago.edu. Thank you!");
			System.out.println(fileOutput.toString());
			System.out.println(tokensOutput.toString());
			e.printStackTrace();
		}
	}

	/*
	 * Compilers a complete class
	 */
	public void CompileClass() {
		try {
			writeToFile("START_CLASS", null);

			// class
			jackTokenizer.advance();
			if (!jackTokenizer.tokenType().equals("KEYWORD") || !jackTokenizer.keyWord().equals("class")) {
				printError("class");
			} else {
				writeToFile("KEYWORD", "class");
			}

			// class name
			jackTokenizer.advance();
			if (!jackTokenizer.tokenType().equals("IDENTIFIER")) {
				printError("class name");
			} else {
				writeToFile("IDENTIFIER", jackTokenizer.identifier());
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

			writeToFile("END_CLASS", null);

			// close file, program ends
			if (writeFile != null) {
				writeFile.close();
			}
			if (writeTokens != null) {
				writeTokens.close();
			}
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile class");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("I/O problems when advance in compile class");
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
			writeToFile("START_CLASS_VAR_DEC", null);

			// now we clear out all the possibilities and left for variables
			if (!jackTokenizer.keyWord().equals("static") && !jackTokenizer.keyWord().equals("field")) {
				printError("static or field");
			}

			writeToFile("KEYWORD", jackTokenizer.keyWord());

			// deal with variable types
			compileType();

			// deal with variable names
			while (true) {
				jackTokenizer.advance();
				if (!jackTokenizer.tokenType().equals("IDENTIFIER")) {
					printError("identifier");
				}
				writeToFile("IDENTIFIER", jackTokenizer.identifier());

				// determine if it's ',' or ';'
				jackTokenizer.advance();
				if (!jackTokenizer.tokenType().equals("SYMBOL")
						|| (jackTokenizer.symbol() != ',' && jackTokenizer.symbol() != ';')) {
					printError(", or ;");
				}

				writeToFile("SYMBOL", String.valueOf(jackTokenizer.symbol()));
				if (jackTokenizer.symbol() == ';') { // end of declaration
					break;
				}
			}

			writeToFile("END_CLASS_VAR_DEC", null);

			// call recursion and will automatically return and proceed to compileSubroutine
			compileClassVarDec();

		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile class variables declaration");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("I/O problems when advance in compile class variables declaration");
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

			writeToFile("START_SUBROUTINE_DEC", null);
			writeToFile("KEYWORD", jackTokenizer.keyWord());

			// deal with return type
			jackTokenizer.advance();
			if (jackTokenizer.tokenType().equals("KEYWORD") && jackTokenizer.keyWord().equals("void")) { // void
				writeToFile("KEYWORD", "void");
			} else { // return type
				jackTokenizer.rewind();
				compileType();
			}

			// deal with name
			jackTokenizer.advance();
			if (!jackTokenizer.tokenType().equals("IDENTIFIER")) {
				printError("routine name");
			}

			writeToFile("IDENTIFIER", jackTokenizer.identifier());

			// (
			compileSymbol("(");

			// parameters
			writeToFile("START_PARAMETER_LIST", null);
			compileParameterList();
			writeToFile("END_PARAMETER_LIST", null);

			// )
			compileSymbol(")");

			// body statements
			writeToFile("START_SUBROUTINE_BODY", null);
			compileSymbol("{");
			compileVarDec();
			writeToFile("START_STATEMENTS", null);
			compileStatements();
			writeToFile("END_STATEMENTS", null);
			compileSymbol("}");
			writeToFile("END_SUBROUTINE_BODY", null);
			writeToFile("END_SUBROUTINE_DEC", null);

			// call recursion until there is no more subroutines
			compileSubroutine();
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile subroutine");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("I/O problems when advance in compile subroutine");
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
				compileType();

				// variable name
				jackTokenizer.advance();
				if (!jackTokenizer.tokenType().equals("IDENTIFIER")) {
					printError("var name");
				}
				writeToFile("IDENTIFIER", jackTokenizer.identifier());

				// next should be , for another or ) for the end
				jackTokenizer.advance();
				if (!jackTokenizer.tokenType().equals("SYMBOL")
						|| (jackTokenizer.symbol() != ',' && jackTokenizer.symbol() != ')')) {
					printError(", or )");
				}
				if (jackTokenizer.symbol() == ',') {
					writeToFile("SYMBOL", ",");
				} else { // end with )
					jackTokenizer.rewind(); // rewind and let the upper level advance
					break;
				}
			}
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile parameter list");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("I/O problems when advance in compile parameter list");
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

			writeToFile("START_VAR_DEC", null);
			writeToFile("KEYWORD", "var");

			compileType();

			while (true) {
				jackTokenizer.advance();
				if (!jackTokenizer.tokenType().equals("IDENTIFIER")) {
					printError("var name");
				}
				writeToFile("IDENTIFIER", jackTokenizer.identifier());

				// check if have more vars with , or end with ;
				jackTokenizer.advance();
				if (!jackTokenizer.tokenType().equals("SYMBOL")
						|| (jackTokenizer.symbol() != ',' && jackTokenizer.symbol() != ';')) {
					printError(", or ;");
				}
				writeToFile("SYMBOL", String.valueOf(jackTokenizer.symbol()));
				if (jackTokenizer.symbol() == ';') {
					break;
				}
			}

			writeToFile("END_VAR_DEC", null);

			compileVarDec(); // recursion until the end
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile var declaration");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("I/O problems when advance in compile var declaration");
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
		try {
			writeToFile("START_DO", null);
			writeToFile("KEYWORD", "do");
			compileCallSubroutine();
			compileSymbol(";");
			writeToFile("END_DO", null);
		} catch (IOException e) {
			System.err.println("I/O problems when advance in compile do");
			e.printStackTrace();
		}
	}

	/*
	 * Compiles a let statement
	 */
	public void compileLet() {
		try {
			writeToFile("START_LET", null);
			writeToFile("KEYWORD", "let");

			jackTokenizer.advance();
			if (!jackTokenizer.tokenType().equals("IDENTIFIER")) {
				printError("var name");
			}
			writeToFile("IDENTIFIER", jackTokenizer.identifier());

			jackTokenizer.advance();
			if (!jackTokenizer.tokenType().equals("SYMBOL")
					|| (jackTokenizer.symbol() != '[' && jackTokenizer.symbol() != '=')) {
				printError("[ or =");
			}

			if (jackTokenizer.symbol() == '[') { // array
				writeToFile("SYMBOL", "[");

				compileExpression();

				// end with ]
				jackTokenizer.advance();
				if (!jackTokenizer.tokenType().equals("SYMBOL") || jackTokenizer.symbol() != ']') {
					printError("]");
				}
				writeToFile("SYMBOL", "]");

				jackTokenizer.advance(); // after an array, advance
			}

			writeToFile("SYMBOL", "=");

			compileExpression();

			// end with ;
			compileSymbol(";");
			writeToFile("END_LET", null);
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile let");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("I/O problems when advance in compile let");
			e.printStackTrace();
		}
	}

	/*
	 * Compiles a while statement
	 */
	public void compileWhile() {
		try {
			writeToFile("START_WHILE", null);

			writeToFile("KEYWORD", "while");

			compileSymbol("(");
			compileExpression();
			compileSymbol(")");
			compileSymbol("{");

			writeToFile("START_STATEMENTS", null);
			compileStatements();
			writeToFile("END_STATEMENTS", null);

			compileSymbol("}");

			writeToFile("END_WHILE", null);
		} catch (IOException e) {
			System.err.println("I/O problems when advance in compile while");
			e.printStackTrace();
		}

	}

	/*
	 * Compiles a return statement
	 */
	public void compileReturn() {
		try {
			writeToFile("START_RETURN", null);
			writeToFile("KEYWORD", "return");
			while (true) {
				jackTokenizer.advance();
				if (jackTokenizer.tokenType().equals("SYMBOL") && jackTokenizer.symbol() == ';') { // void
					writeToFile("SYMBOL", ";");
					break;
				}

				jackTokenizer.rewind();

				compileExpression();

				compileSymbol(";");
				break;
			}

			writeToFile("END_RETURN", null);
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile return");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("I/O problems when advance in compile return");
			e.printStackTrace();
		}
	}

	/*
	 * Compiles an if statement, possibly with a trailing else clause
	 */
	public void compileIf() {
		try {
			writeToFile("START_IF", null);

			writeToFile("KEYWORD", "if");

			compileSymbol("(");
			compileExpression();
			compileSymbol(")");
			compileSymbol("{");

			writeToFile("START_STATEMENTS", null);
			compileStatements();
			writeToFile("END_STATEMENTS", null);

			compileSymbol("}");

			// else
			jackTokenizer.advance();
			if (jackTokenizer.tokenType().equals("KEYWORD") && jackTokenizer.keyWord().equals("else")) {
				writeToFile("KEYWORD", "else");

				compileSymbol("{");

				writeToFile("START_STATEMENTS", null);
				compileStatements();
				writeToFile("END_STATEMENTS", null);

				compileSymbol("}");
			} else {
				jackTokenizer.rewind();
			}

			writeToFile("END_IF", null);
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile if");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("I/O problems when advance in compile if");
			e.printStackTrace();
		}

	}

	/*
	 * Compiles an expressioin, term (operation term)
	 */
	public void compileExpression() {
		try {
			writeToFile("START_EXPRESSION", null);

			compileTerm();

			while (true) {
				jackTokenizer.advance();
				if (jackTokenizer.tokenType().equals("SYMBOL") && (jackTokenizer.symbol() == '+'
						|| jackTokenizer.symbol() == '-' || jackTokenizer.symbol() == '*'
						|| jackTokenizer.symbol() == '/' || jackTokenizer.symbol() == '&'
						|| jackTokenizer.symbol() == '|' || jackTokenizer.symbol() == '>'
						|| jackTokenizer.symbol() == '<' || jackTokenizer.symbol() == '=')) {
					if (jackTokenizer.symbol() == '>') {
						writeToFile("SYMBOL", "&gt;");
					} else if (jackTokenizer.symbol() == '<') {
						writeToFile("SYMBOL", "&lt;");
					} else if (jackTokenizer.symbol() == '&') {
						writeToFile("SYMBOL", "&amp;");
					} else {
						// other symbol
						writeToFile("SYMBOL", String.valueOf(jackTokenizer.symbol()));
					}

					compileTerm();
				} else {
					jackTokenizer.rewind();
					break;
				}
			}
			writeToFile("END_EXPRESSION", null);
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile expression");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("I/O problems when advance in compile expression");
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
			writeToFile("START_TERM", null);

			jackTokenizer.advance();
			if (jackTokenizer.tokenType().equals("IDENTIFIER")) {
				// Var name, Array constant or Call subroutine
				String identifier = jackTokenizer.identifier();

				jackTokenizer.advance(); // have to use the next symbol to determine
				if (jackTokenizer.tokenType().equals("SYMBOL") && jackTokenizer.symbol() == '[') {
					// array
					writeToFile("IDENTIFIER", identifier);
					writeToFile("SYMBOL", "[");
					compileExpression();
					compileSymbol("]");
				} else if (jackTokenizer.tokenType().equals("SYMBOL")
						&& (jackTokenizer.symbol() == '(' || jackTokenizer.symbol() == '.')) {
					// call subroutine, rewind twice to make the compile function right
					jackTokenizer.rewind();
					jackTokenizer.rewind();
					compileCallSubroutine();
				} else {
					// var name
					writeToFile("IDENTIFIER", identifier);
					jackTokenizer.rewind();
				}

			} else {
				// Simple constant, Expression or Simple operation
				if (jackTokenizer.tokenType().equals("INT_CONST")) {
					writeToFile("INT_CONST", String.valueOf(jackTokenizer.intVal()));
				} else if (jackTokenizer.tokenType().equals("STRING_CONST")) {
					writeToFile("STRING_CONST", jackTokenizer.stringVal());
				} else if (jackTokenizer.tokenType().equals("KEYWORD")
						&& (jackTokenizer.keyWord().equals("true") || jackTokenizer.keyWord().equals("false")
								|| jackTokenizer.keyWord().equals("null") || jackTokenizer.keyWord().equals("this"))) {
					writeToFile("KEYWORD", jackTokenizer.keyWord());
				} else if (jackTokenizer.tokenType().equals("SYMBOL")
						&& (jackTokenizer.symbol() == '-' || jackTokenizer.symbol() == '~')) {
					writeToFile("SYMBOL", "(");
					compileExpression();
					writeToFile("SYMBOL", ")");
				} else if (jackTokenizer.tokenType().equals("SYMBOL") && jackTokenizer.symbol() == '(') {
					// expression in parenthesis
					writeToFile("SYMBOL", "(");
					compileExpression();
					compileSymbol(")");
				} else {
					printError("int, string, keyword, (expression) or operation");
				}
			}

			writeToFile("END_TERM", null);
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile if");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("I/O problems when advance in compile if");
			e.printStackTrace();
		}
	}

	/*
	 * Compiles a comma-separated list of expression
	 */
	public void compileExpressionList() {
		try {
			jackTokenizer.advance();
			if (jackTokenizer.tokenType().equals("SYMBOL") && jackTokenizer.symbol() == ')') {
				// no expression
				jackTokenizer.rewind();
				return;
			}

			// recalibrate for while loop
			jackTokenizer.rewind();
			compileExpression();

			// can have more expressions
			while (true) {
				jackTokenizer.advance();
				if (jackTokenizer.tokenType().equals("SYMBOL") && jackTokenizer.symbol() == ',') {
					writeToFile("SYMBOL", ",");
					compileExpression(); // every turn is an expression
				} else {
					jackTokenizer.rewind();
					break;
				}
			}

		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile expression list");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("I/O problems when advance in compile expression list");
			e.printStackTrace();
		}
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
			writeToFile("IDENTIFIER", jackTokenizer.identifier());

			jackTokenizer.advance();
			if (jackTokenizer.tokenType().equals("SYMBOL") && jackTokenizer.symbol() == '(') {
				// with no prefix
				writeToFile("SYMBOL", "(");

				writeToFile("START_EXPRESSION_LIST", null);
				compileExpressionList();
				writeToFile("END_EXPRESSION_LIST", null);

				// )
				compileSymbol(")");
			} else if (jackTokenizer.tokenType().equals("SYMBOL") && jackTokenizer.symbol() == '.') {
				// the first identifier is prefix, another class or a var
				writeToFile("SYMBOL", ".");

				jackTokenizer.advance();
				if (!jackTokenizer.tokenType().equals("IDENTIFIER")) {
					printError("subroutine name, class name or var name");
				}
				writeToFile("IDENTIFIER", jackTokenizer.identifier());

				compileSymbol("(");

				writeToFile("START_EXPRESSION_LIST", null);
				compileExpressionList();
				writeToFile("END_EXPRESSION_LIST", null);
				compileSymbol(")");

			} else {
				printError("( or .");
			}
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile var declaration");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("I/O problems when advance in compile var declaration");
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
			writeToFile("SYMBOL", symbol);
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile symbol");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("I/O problems when advance in compile symbol");
			e.printStackTrace();
		}
	}

	private void writeToFile(String tokenType, String value) throws IOException {
		switch (tokenType) {
		case "KEYWORD":
			writeFile.write("<keyword> " + value + " </keyword>\n");
			writeTokens.write("<keyword> " + value + " </keyword>\n");
			break;
		case "SYMBOL":
			writeFile.write("<symbol> " + value + " </symbol>\n");
			writeTokens.write("<symbol> " + value + " </symbol>\n");
			break;
		case "IDENTIFIER":
			writeFile.write("<identifier> " + value + " </identifier>\n");
			writeTokens.write("<identifier> " + value + " </identifier>\n");
			break;
		case "INT_CONST":
			writeFile.write("<integerConstant> " + value + " </integerConstant>\n");
			writeTokens.write("<integerConstant> " + value + " </integerConstant>\n");
			break;
		case "STRING_CONST":
			writeFile.write("<stringConstant> " + value + " </stringConstant>\n");
			writeTokens.write("<stringConstant> " + value + " </stringConstant>\n");
			break;
		case "START_CLASS":
			writeFile.write("<class>\n");
			writeTokens.write("<tokens>\n");
			break;
		case "END_CLASS":
			writeFile.write("</class>");
			writeTokens.write("</tokens>");
			break;
		case "START_CLASS_VAR_DEC":
			writeFile.write("<classVarDec>\n");
			break;
		case "END_CLASS_VAR_DEC":
			writeFile.write("</classVarDec>\n");
			break;
		case "START_SUBROUTINE_DEC":
			writeFile.write("<subroutineDec>\n");
			break;
		case "END_SUBROUTINE_DEC":
			writeFile.write("</subroutineDec>\n");
			break;
		case "START_SUBROUTINE_BODY":
			writeFile.write("<subroutineBody>\n");
			break;
		case "END_SUBROUTINE_BODY":
			writeFile.write("</subroutineBody>\n");
			break;
		case "START_LET":
			writeFile.write("<letStatement>\n");
			break;
		case "END_LET":
			writeFile.write("</letStatement>\n");
			break;
		case "START_STATEMENTS":
			writeFile.write("<statements>\n");
			break;
		case "END_STATEMENTS":
			writeFile.write("</statements>\n");
			break;
		case "START_VAR_DEC":
			writeFile.write("<varDec>\n");
			break;
		case "END_VAR_DEC":
			writeFile.write("</varDec>\n");
			break;
		case "START_EXPRESSION_LIST":
			writeFile.write("<expressionList>\n");
			break;
		case "END_EXPRESSION_LIST":
			writeFile.write("</expressionList>\n");
			break;
		case "START_EXPRESSION":
			writeFile.write("<expression>\n");
			break;
		case "END_EXPRESSION":
			writeFile.write("</expression>\n");
			break;
		case "START_TERM":
			writeFile.write("<term>\n");
			break;
		case "END_TERM":
			writeFile.write("</term>\n");
			break;
		case "START_WHILE":
			writeFile.write("<whileStatement>\n");
			break;
		case "END_WHILE":
			writeFile.write("</whileStatement>\n");
			break;
		case "START_DO":
			writeFile.write("<doStatement>\n");
			break;
		case "END_DO":
			writeFile.write("</doStatement>\n");
			break;
		case "START_RETURN":
			writeFile.write("<returnStatement>\n");
			break;
		case "END_RETURN":
			writeFile.write("</returnStatement>\n");
			break;
		case "START_IF":
			writeFile.write("<ifStatement>\n");
			break;
		case "END_IF":
			writeFile.write("</ifStatement>\n");
			break;
		case "START_PARAMETER_LIST":
			writeFile.write("<parameterList>\n");
			break;
		case "END_PARAMETER_LIST":
			writeFile.write("</parameterList>\n");
			break;
		default:
			throw new IllegalStateException("invalid token passed into writeToFile");
		}
	}

	/*
	 * shared function for determine variable type
	 */
	private void compileType() {
		try {
			jackTokenizer.advance();
			boolean isType = false;
			if (jackTokenizer.tokenType().equals("KEYWORD") && (jackTokenizer.keyWord().equals("int")
					|| jackTokenizer.keyWord().equals("char") || jackTokenizer.keyWord().equals("boolean"))) {
				writeToFile("KEYWORD", jackTokenizer.keyWord());
				isType = true;
			}

			if (jackTokenizer.tokenType().equals("IDENTIFIER")) {
				writeToFile("IDENTIFIER", jackTokenizer.identifier());
				isType = true;
			}

			// not matching any variable types
			if (!isType) {
				printError("int or char or coolean or class name");
			}
		} catch (ParseException e) {
			System.err.println("Parsing problems when advance in compile type");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("I/O problems when advance in compile type");
			e.printStackTrace();
		}
	}

	/*
	 * shared error message function, print out the expected value
	 */
	private void printError(String expect) {
		throw new IllegalStateException("The token should be " + expect + " instead of " + jackTokenizer.tokenType());
	}

}
