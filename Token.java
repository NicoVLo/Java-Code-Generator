package edu.ufl.cise.plcsp23;

import java.util.Arrays;

public class Token implements IToken{
    final Kind kind;
    final int pos;
    final int length;
    final char[] inputChars;
    String tokenString;
    SourceLocation tokenLocation;

    //constructor initializes final fields
    public Token(Kind kind, int pos, int line, int col, int length, char[] inputChars) {
        super();
        this.kind = kind;
        this.pos = pos;
        this.length = length;
        this.inputChars = Arrays.copyOfRange(inputChars, pos, pos+length);
        this.tokenLocation = new SourceLocation(line, col);
        this.tokenString = "";
        for(int i=0; i<length; i++)
            this.tokenString = this.tokenString.concat(Character.toString(this.inputChars[i]));
    }
    @Override public SourceLocation getSourceLocation() {return tokenLocation;}
    @Override public Kind getKind() {return kind;}
    //returns the characters from the source belonging to the token
    @Override public String getTokenString() {return tokenString;}
    //prints token, used during development
    //@Override  public String toString() {}
}
