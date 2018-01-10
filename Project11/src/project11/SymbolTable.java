package project11;

/*
 *  Chung-An, Chiu 12172213 
 *  Project 11
 *  Nov 30, 2017
 */

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
	private class TableElement {
		String type;
		String kind;
		int currentNumber;

		public TableElement(String type, String kind, int currentNumber) {
			this.type = type;
			this.kind = kind;
			this.currentNumber = currentNumber;
		}
	}

	private int fieldCount;
	private int staticCount;
	private int argumentCount;
	private int localCount;

	private Map<String, TableElement> classVariablesTable;
	private Map<String, TableElement> subroutineVariablesTable;

	/*
	 * Creates a new empty symbol table
	 */
	public SymbolTable() {
		classVariablesTable = new HashMap<>();
		subroutineVariablesTable = new HashMap<>();
	}

	/*
	 * Starts a new subroutine scope
	 */
	public void startSubroutine() {
		argumentCount = 0;
		localCount = 0;
		subroutineVariablesTable = new HashMap<>();
	}

	/*
	 * Defines a new identifier of a given name, type, and kind and assigns it a
	 * running index
	 */
	public void define(String name, String type, String kind) {
		if (kind.equals("static")) {
			classVariablesTable.put(name, new TableElement(type, kind, varCount(kind)));
			staticCount++;
		} else if (kind.equals("field")) {
			classVariablesTable.put(name, new TableElement(type, kind, varCount(kind)));
			fieldCount++;
		} else if (kind.equals("argument")) {
			subroutineVariablesTable.put(name, new TableElement(type, kind, varCount(kind)));
			argumentCount++;
		} else if (kind.equals("local")) {
			subroutineVariablesTable.put(name, new TableElement(type, kind, varCount(kind)));
			localCount++;
		} else {
			throw new IllegalStateException("No such kind when define");
		}
	}

	/*
	 * Returns the number of variables of the given kind already defined in the
	 * current scope
	 */
	public int varCount(String kind) {
		if (kind.equals("static")) {
			return staticCount;
		} else if (kind.equals("field")) {
			return fieldCount;
		} else if (kind.equals("argument")) {
			return argumentCount;
		} else if (kind.equals("local")) {
			return localCount;
		} else {
			throw new IllegalStateException("No such kind when varCount");
		}
	}

	/*
	 * Returns the kind of the named identifier in the current scope
	 */
	public String kindOf(String name) {
		TableElement temp = subroutineVariablesTable.get(name);
		if (temp == null) {
			temp = classVariablesTable.get(name);
		}
		if (temp == null) {
			// can be class name or subroutine name
			return "";
		} else {
			if (temp.kind.equals("field")) {
				return "this";
			} else {
				return temp.kind;
			}
		}
	}

	/*
	 * Returns the type of the named identifier in the current scope
	 */
	public String typeOf(String name) {
		TableElement temp = subroutineVariablesTable.get(name);
		if (temp == null) {
			temp = classVariablesTable.get(name);
		}
		if (temp == null) {
			// can be class name or subroutine name
			return "";
		} else {
			return temp.type;
		}
	}

	/*
	 * Returns the index assigned to the named identifier
	 */
	public int indexOf(String name) {
		TableElement temp = subroutineVariablesTable.get(name);
		if (temp == null) {
			temp = classVariablesTable.get(name);
		}
		if (temp == null) {
			// can be class name or subroutine name
			return 0;
		} else {
			return temp.currentNumber;
		}
	}
}
