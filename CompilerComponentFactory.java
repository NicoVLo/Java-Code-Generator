/*Copyright 2023 by Beverly A Sanders
 * 
 * This code is provided for solely for use of students in COP4020 Programming Language Concepts at the 
 * University of Florida during the spring semester 2023 as part of the course project.  
 * 
 * No other use is authorized. 
 * 
 * This code may not be posted on a public web site either during or after the course.  
 */

package edu.ufl.cise.plcsp23;

import java.util.ArrayList;
import java.util.List;
import edu.ufl.cise.plcsp23.ast.ASTVisitor;

public class CompilerComponentFactory {
	public static IScanner makeScanner(String input) {
		//Add statement to return an instance of your scanner
		return new Scanner(input);
	}

	/*public static IParser makeAssignment2Parser(String input) throws LexicalException {
		//add code to create a scanner and parser and return the parser.
		Scanner myScanner = new Scanner(input);
		List<IToken> tokens = new ArrayList<>();
		IToken token = myScanner.next();
		while(token.getKind() != IToken.Kind.EOF) {
			tokens.add(token);
			token = myScanner.next();
		}
		tokens.add(token);
		return new Parser(tokens);
	}*/

	public static IParser makeParser(String input) throws LexicalException {
		//add code to create a scanner and parser and return the parser.
		Scanner myScanner = new Scanner(input);
		List<IToken> tokens = new ArrayList<>();
		IToken token = myScanner.next();
		while(token.getKind() != IToken.Kind.EOF) {
			tokens.add(token);
			token = myScanner.next();
		}
		tokens.add(token);
		return new Parser(tokens);
	}

	public static ASTVisitor makeTypeChecker(){
		return new TypeCheckVisitor();
	}

	public static ASTVisitor makeCodeGenerator(String packageName) {
		//code to instantiate a return an ASTVisitor for code generation
		return new CodeGenVisitor(packageName);
	}
}
