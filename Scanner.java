package edu.ufl.cise.plcsp23;

import java.util.HashMap;


public class Scanner implements IScanner{
    final String input;
    //array containing input chars, terminated with extra char 0
    final char[] inputChars;
    //invariant ch == inputChars[pos]
    int pos; //position of ch
    char ch; //next char
    int line;
    int col;
    //constructor
    public Scanner(String input) {
        this.input = input;

        inputChars = new char[input.length() + 1];
        for(int i=0; i<input.length(); i++)
            inputChars[i] = input.charAt(i);
        inputChars[input.length()] = 0;

        line = 1;
        col = 1;
        pos = 0;
        ch = inputChars[pos];
    }

    private enum State{
        START,
        HAVE_EQ,
        IN_IDENT,
        IN_NUM_LIT,
        IN_STRING_LIT,
        ESC_SEQ,
        COMMENT,
        HAVE_LT,
        HAVE_EXCH,
        HAVE_GT,
        HAVE_AND,
        HAVE_OR,
        HAVE_MULT
    }

    private static HashMap<String, IToken.Kind> reservedWords;
    static{
        reservedWords = new HashMap<String, IToken.Kind>();
        reservedWords.put("image", IToken.Kind.RES_image);
        reservedWords.put("pixel", IToken.Kind.RES_pixel);
        reservedWords.put("int", IToken.Kind.RES_int);
        reservedWords.put("string", IToken.Kind.RES_string);
        reservedWords.put("void",IToken.Kind.RES_void);
        reservedWords.put("nil",IToken.Kind.RES_nil);
        reservedWords.put("load",IToken.Kind.RES_load);
        reservedWords.put("display",IToken.Kind.RES_display);
        reservedWords.put("write",IToken.Kind.RES_write);
        reservedWords.put("x",IToken.Kind.RES_x);
        reservedWords.put("y",IToken.Kind.RES_y);
        reservedWords.put("a",IToken.Kind.RES_a);
        reservedWords.put("r",IToken.Kind.RES_r);
        reservedWords.put("X",IToken.Kind.RES_X);
        reservedWords.put("Y",IToken.Kind.RES_Y);
        reservedWords.put("Z",IToken.Kind.RES_Z);
        reservedWords.put("x_cart",IToken.Kind.RES_x_cart);
        reservedWords.put("y_cart",IToken.Kind.RES_y_cart);
        reservedWords.put("a_polar",IToken.Kind.RES_a_polar);
        reservedWords.put("r_polar",IToken.Kind.RES_r_polar);
        reservedWords.put("rand",IToken.Kind.RES_rand);
        reservedWords.put("sin",IToken.Kind.RES_sin);
        reservedWords.put("cos",IToken.Kind.RES_cos);
        reservedWords.put("atan",IToken.Kind.RES_atan);
        reservedWords.put("if",IToken.Kind.RES_if);
        reservedWords.put("while",IToken.Kind.RES_while);
        reservedWords.put("red",IToken.Kind.RES_red);
        reservedWords.put("grn",IToken.Kind.RES_grn);
        reservedWords.put("blu",IToken.Kind.RES_blu);
    }

    private boolean isDigit(int ch){
        return '0' <= ch && ch <='9';
    }

    private boolean isLetter(int ch) {
        return ('A' <= ch && ch <= 'Z') || ('a' <= ch && ch <= 'z');
    }

    private boolean isIdentStart(int ch){
        return isLetter(ch) || (ch == '$') || (ch == '_');
    }

    private boolean escapeSeq(int ch){
        return (ch == 'b' || ch == 't' || ch == 'n' || ch == 'r' || ch== '"' || ch =='\\');
    }

    private void error(String message) throws LexicalException{
        throw new LexicalException("Error at pos "+pos+": " + message);
    }
    @Override
    public IToken next() throws LexicalException {
        State state = State.START;
        int tokenStart = -1;
        while(true){
            ch = inputChars[pos];
            switch(state){
                case START ->{
                    tokenStart = pos;
                    switch(ch){
                    //empty string
                        case 0 -> {
                            return new Token(IToken.Kind.EOF, tokenStart, line, col, 0, inputChars);
                        }
                    //whitspace
                        case ' ','\n','\r','\t','\f' -> {
                            pos++;
                            if(ch == '\n') {
                                line++;
                                col = 1;
                            }
                            else col++;
                        }
                    //single character tokens
                        case '.' ->{
                            pos++;
                            col++;
                            return new Token(IToken.Kind.DOT, tokenStart, line, col-1, 1, inputChars);
                        }
                        case ',' -> {
                            pos++;
                            col++;
                            return new Token(IToken.Kind.COMMA, tokenStart, line, col-1, 1, inputChars);
                        }
                        case '?' -> {
                            pos++;
                            col++;
                            return new Token(IToken.Kind.QUESTION, tokenStart, line, col-1, 1, inputChars);
                        }
                        case ':' -> {
                            pos++;
                            col++;
                            return new Token(IToken.Kind.COLON, tokenStart, line, col-1, 1, inputChars);
                        }
                        case '(' -> {
                            pos++;
                            col++;
                            return new Token(IToken.Kind.LPAREN, tokenStart, line, col-1, 1, inputChars);
                        }
                        case ')' -> {
                            pos++;
                            col++;
                            return new Token(IToken.Kind.RPAREN, tokenStart, line, col-1, 1, inputChars);
                        }
                        case '[' -> {
                            pos++;
                            col++;
                            return new Token(IToken.Kind.LSQUARE, tokenStart, line, col-1, 1, inputChars);
                        }
                        case ']' -> {
                            pos++;
                            col++;
                            return new Token(IToken.Kind.RSQUARE, tokenStart, line, col-1, 1, inputChars);
                        }
                        case '{' -> {
                            pos++;
                            col++;
                            return new Token(IToken.Kind.LCURLY, tokenStart, line, col-1, 1, inputChars);
                        }
                        case '}' -> {
                            pos++;
                            col++;
                            return new Token(IToken.Kind.RCURLY, tokenStart, line, col-1, 1, inputChars);
                        }
                        case '!' -> {
                            pos++;
                            col++;
                            return new Token(IToken.Kind.BANG, tokenStart, line, col-1, 1, inputChars);
                        }
                        case '&' -> {
                            state = State.HAVE_AND;
                            pos++;
                            col++;
                        }
                        case '|' -> {
                            state = State.HAVE_OR;
                            pos++;
                            col++;
                        }
                        case '+' -> {
                            pos++;
                            col++;
                            return new Token(IToken.Kind.PLUS, tokenStart, line, col-1, 1, inputChars);
                        }
                        case '-' -> {
                            pos++;
                            col++;
                            return new Token(IToken.Kind.MINUS, tokenStart, line, col-1, 1,inputChars);
                        }
                        case '*' -> {
                            state = State.HAVE_MULT;
                            pos++;
                            col++;
                        }
                        case '/' -> {
                            pos++;
                            col++;
                            return new Token(IToken.Kind.DIV, tokenStart, line, col-1, 1, inputChars);
                        }
                        case '%' -> {
                            pos++;
                            col++;
                            return new Token(IToken.Kind.MOD, tokenStart, line, col-1, 1, inputChars);
                        }
                        case '0' -> {
                            pos++;
                            col++;
                            return new NumLitToken(tokenStart, line, col-1, 1, inputChars);
                        }
                //handle ==
                        case '=' -> {
                            state = State.HAVE_EQ;
                            pos++;
                            col++;
                        }
                        case '"' -> {
                            state = State.IN_STRING_LIT;
                            pos++;
                            col++;
                        }
                        case '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                            state = State.IN_NUM_LIT;
                            pos++;
                            col++;
                        }
                        case '~' -> {
                            state = State.COMMENT;
                            pos++;
                            col++;
                        }
                        case '<' ->{
                            state = State.HAVE_LT;
                            pos++;
                            col++;
                        }
                        case '>' -> {
                            state = State.HAVE_GT;
                            pos++;
                            col++;
                        }
                        default -> {
                            if(isIdentStart(ch)){
                                state = State.IN_IDENT;
                                pos++;
                                col++;
                            }
                            else error("illegal char with ascii value: "+(int)ch);
                        }
                    }
                }
                case HAVE_AND -> {
                    if(ch == '&'){
                        state =State.START;
                        pos++;
                        col++;
                        return new Token(IToken.Kind.AND, tokenStart, line, col-2, 2, inputChars);
                    }
                    else{
                        return new Token(IToken.Kind.BITAND, tokenStart, line, col-1, 1, inputChars);
                    }
                }
                case HAVE_OR -> {
                    if(ch == '|'){
                        state =State.START;
                        pos++;
                        col++;
                        return new Token(IToken.Kind.OR, tokenStart, line, col-2, 2, inputChars);
                    }
                    else{
                        return new Token(IToken.Kind.BITOR, tokenStart, line, col-1, 1, inputChars);
                    }
                }
                case HAVE_MULT -> {
                    if(ch == '*'){
                        state = State.START;
                        pos++;
                        col++;
                        return new Token(IToken.Kind.EXP, tokenStart, line, col-2, 2, inputChars);
                    }
                    else{
                        return new Token(IToken.Kind.TIMES, tokenStart, line, col-1, 1, inputChars);
                    }
                }
                case HAVE_EQ -> {
                    if(ch == '='){
                        state = State.START;
                        pos++;
                        col++;
                        return new Token(IToken.Kind.EQ, tokenStart,line, col-2,2, inputChars);
                    }
                    else{
                        return new Token(IToken.Kind.ASSIGN, tokenStart, line, col-1, 1, inputChars);
                    }
                }
                case HAVE_LT -> {
                    if(ch == '-'){
                        state = State.HAVE_EXCH;
                        pos++;
                        col++;
                    }
                    else if(ch == '='){
                        state = State.START;
                        pos++;
                        col++;
                        return new Token(IToken.Kind.LE, tokenStart, line, col-2, 2, inputChars);
                    }
                    else{
                        state =State.START;
                        return new Token(IToken.Kind.LT, tokenStart, line, col-1, 1, inputChars);
                    }
                }
                case HAVE_GT -> {
                    if(ch == '='){
                        state = State.START;
                        pos++;
                        col++;
                        return new Token(IToken.Kind.GE, tokenStart, line, col-2, 2, inputChars);
                    }
                    else{
                        state = State.START;
                        return new Token(IToken.Kind.GT, tokenStart, line, col-1, 1, inputChars);
                    }
                }
                case HAVE_EXCH -> {
                    if(ch == '>'){
                        state = State.START;
                        pos++;
                        col++;
                        return new Token(IToken.Kind.EXCHANGE, tokenStart, line, col-3, 3, inputChars);
                    }
                    else{
                        error("> expected");
                    }
                }
                case IN_NUM_LIT->{
                    if(isDigit(ch)) {
                        pos++;
                        col++;
                    }
                    else{
                        int length = pos-tokenStart;

                        NumLitToken numLitToken = new NumLitToken(tokenStart, line, col-length, length, inputChars);
                        try{
                            int numLitVal = numLitToken.getValue();
                            return numLitToken;
                        }
                        catch(Exception e){
                            error("Not a legal num lit");
                        }
                    }
                }
                case IN_IDENT -> {
                    if(isIdentStart(ch) || isDigit(ch)){
                        pos++;
                        col++;
                    }
                    else{
                        int length = pos-tokenStart;
                        String text = input.substring(tokenStart, tokenStart+length);
                        IToken.Kind kind = reservedWords.get(text);
                        if(kind == null){kind = IToken.Kind.IDENT;}
                        return new Token(kind, tokenStart, line, col-length, length, inputChars);
                    }
                }
                case IN_STRING_LIT -> {
                    if(ch == '\\') {
                        state = State.ESC_SEQ;
                        pos++;
                        col++;
                    }
                    else if(ch == '\n' || ch == '\r'){
                        error("illegal LF/CR");
                    }
                    else if(ch == '"') {
                        pos++;
                        int length = pos-tokenStart;
                        col++;
                        return new StringLitToken(tokenStart, line, col - length, length, inputChars);
                    }
                    else{
                        pos++;
                        col++;
                    }
                }
                case ESC_SEQ -> {
                    if(!escapeSeq(ch)) {
                        error("expected escape sequence");
                    }
                    state = State.IN_STRING_LIT;
                    pos++;
                    col++;
                }
                case COMMENT -> {
                    pos++;
                    if(ch == '\n') {
                        state = State.START;
                        line++;
                        col = 1;
                    }
                    else col++;
                }
                default -> {
                    throw new UnsupportedOperationException("Bug in Scanner");
                }
            }
        }
    }
}
