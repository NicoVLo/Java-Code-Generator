package edu.ufl.cise.plcsp23;

import java.util.Arrays;

public class StringLitToken implements IStringLitToken{
    final int pos;
    final int length;
    final char[] inputChars;
    String tokenString;
    SourceLocation tokenLocation;

    //constructor initializes final fields
    public StringLitToken(int pos, int line, int col, int length, char[] inputChars) {
        this.pos = pos;
        this.length = length;
        this.inputChars = Arrays.copyOfRange(inputChars, pos, pos+length);
        this.tokenLocation = new SourceLocation(line, col);
        this.tokenString = "";
        for(int i=0; i<length; i++)
            this.tokenString = this.tokenString.concat(Character.toString(this.inputChars[i]));
    }
    @Override public IToken.SourceLocation getSourceLocation() {return tokenLocation;}
    @Override public IToken.Kind getKind() {return Kind.STRING_LIT;}
    //returns the characters from the source belonging to the token
    @Override public String getTokenString() {return tokenString;}

    @Override public String getValue(){
        String stringLitValue ="";
        char c;
        for(int i = 1; i<length-1; i++){
            c = tokenString.charAt(i);
            if(tokenString.charAt(i) == '\\'){
                c = tokenString.charAt(++i);
                switch(c){
                    case 'b' -> {
                        c = '\b';
                    }
                    case 't' ->{
                        c = '\t';
                    }
                    case 'n' ->{
                        c = '\n';
                    }
                    case 'r' ->{
                        c = '\r';
                    }
                    case '\"' ->{
                        c = '\"';
                    }
                    case '\\' ->{
                        c = '\\';
                    }
                    default -> c = 0;
                }
            }
            stringLitValue = stringLitValue + c;
        }
        return stringLitValue;
    }
    //prints token, used during development
    //@Override  public String toString() {}
}
