package edu.ufl.cise.plcsp23;

import edu.ufl.cise.plcsp23.ast.*;

import java.util.ArrayList;
import java.util.List;

public class Parser implements IParser{
    private final List<IToken> tokens;
    private int current = 0;

    Parser(List<IToken> tokens) {
        this.tokens = tokens;
    }

    private boolean match(IToken.Kind... kinds) {
        for (IToken.Kind kind : kinds) {
            if (check(kind)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(IToken.Kind kind) {
        if (isAtEnd()) return false;
        return peek().getKind() == kind;
    }

     private IToken advance() {
        if (!isAtEnd()) current++;
        return previous();
    }
    private IToken retreat() {
        current--;
        return peek();
    }

    private boolean isAtEnd() {
        return peek().getKind() == IToken.Kind.EOF;
    }

    private IToken peek() {
        return tokens.get(current);
    }
    private IToken previous() {
        return tokens.get(current - 1);
    }
    private IToken consume(IToken.Kind kind, String message) throws SyntaxException {
        if (check(kind)) return advance();

        else error(message);
        return null;
    }

    boolean isType(IToken token){
        return token.getKind() == IToken.Kind.RES_image || token.getKind() == IToken.Kind.RES_pixel || token.getKind() == IToken.Kind.RES_int || token.getKind() == IToken.Kind.RES_string || token.getKind() == IToken.Kind.RES_void;
    }

    private void error(String message) throws SyntaxException{
        throw new SyntaxException("Error at pos :" + peek().getSourceLocation()+ " " + message);
    }

    private Program Program() throws SyntaxException, LexicalException {
        IToken firstToken = peek();
        List<NameDef> params = new ArrayList<NameDef>();
        if(!isType(firstToken)) error("program should start with a Type");
        Type returnType = Type.getType(peek());

        advance();

        if(!match(IToken.Kind.IDENT)){
            error("not a valid program name");
        }
        Ident name = new Ident(previous());

        consume(IToken.Kind.LPAREN, "Expect '(' after identifier.");
        if(peek().getKind() != IToken.Kind.RPAREN) {
            params.add(NameDef());
                /*if (peek().getKind() != IToken.Kind.RES_void) {
                    params.add(NameDef());
                } else {
                    error("void parameter");
                }*/
        }

        while(!match(IToken.Kind.RPAREN)){
            if(!match(IToken.Kind.COMMA)) {
                error("',' expected after param");
            }
            else if (peek().getKind() != IToken.Kind.RES_void) {
                params.add(NameDef());
            } else {
                throw new SyntaxException("void parameter");
            }
        }

        Block block = Block();
        if(!isAtEnd()) error("EOF expected");
        return new Program(firstToken, returnType, name, params, block);
    }

    Block Block() throws SyntaxException, LexicalException {
        IToken firstToken = peek();
        List<Declaration> decList = new ArrayList<Declaration>();
        List<Statement> statementList = new ArrayList<Statement>();

        consume(IToken.Kind.LCURLY, "{ expected ");

        while(isType(peek())) {
            decList.add(Declaration());
            consume(IToken.Kind.DOT, ". expected");
        }
        while(peek().getKind() == IToken.Kind.IDENT || peek().getKind() == IToken.Kind.RES_write|| peek().getKind() == IToken.Kind.RES_while || peek().getKind() == IToken.Kind.COLON){
            statementList.add(Statement());
            consume(IToken.Kind.DOT, ". expected");
        }
        consume(IToken.Kind.RCURLY, "} expected");

        return new Block(firstToken, decList, statementList);
    }

    /*List<Declaration> DecList() throws SyntaxException{

    }

    List<Statement> StatementList() throws SyntaxException{

    }*/

    NameDef NameDef() throws SyntaxException, LexicalException {
        IToken firstToken = peek();

        if(!isType(peek())){
            error("invalid type");
        }
        Type type = Type.getType(firstToken);
        advance();

        if(peek().getKind() == IToken.Kind.IDENT){
            Ident name = new Ident(peek());
            advance();
            return new NameDef(firstToken, type, null, name);
        }else{
            Dimension dim = Dimension();
            if(peek().getKind() != IToken.Kind.IDENT){
                throw new SyntaxException("invalid namedef!");
            }
            Ident name = new Ident(peek());
            advance();
            return new NameDef(firstToken, type, dim, name);
        }
    }

    Declaration Declaration() throws LexicalException, SyntaxException {
        IToken firstToken = peek();
        NameDef nameDef = NameDef();
        IToken operator = null;
        Expr expr = null;

        if(peek().getKind() == IToken.Kind.ASSIGN){
            operator = peek();
            advance();
            expr = expression();
        }
        return new Declaration(firstToken, nameDef, expr);
    }

    Statement Statement() throws LexicalException, SyntaxException {
        IToken firstToken = peek();
        if(peek().getKind() == IToken.Kind.COLON) {
            advance();
            Expr expr = expression();
            return new ReturnStatement(firstToken, expr);
        }else if(peek().getKind() == IToken.Kind.IDENT){
            LValue lv = LValue();
            if(match(IToken.Kind.ASSIGN)){
                Expr expr = expression();
                return new AssignmentStatement(firstToken, lv, expr);
            }else{
                error("Invalid statement!");
            }
        }else if(peek().getKind() == IToken.Kind.RES_write){
            advance();
            Expr expr = expression();
            return new WriteStatement(firstToken, expr);
        }else if(peek().getKind() == IToken.Kind.RES_while){
            advance();
            Expr expr = expression();
            Block block = Block();
            return new WhileStatement(firstToken, expr, block);
        }
        throw new SyntaxException("Invalid Statement!");
    }

    Dimension Dimension() throws LexicalException, SyntaxException {
        IToken firstToken = peek();

        consume(IToken.Kind.LSQUARE, "[ expected");
        Expr width = expression();
        consume(IToken.Kind.COMMA, ", expected");
        Expr height = expression();
        consume(IToken.Kind.RSQUARE, "] expected");
        return new Dimension(firstToken, width, height);
    }

    private Expr expression() throws SyntaxException, LexicalException {
        IToken firstToken = peek();
        if(firstToken.getKind() == IToken.Kind.RES_if) return conditional_expr();
        return or_expression();
    }
    private Expr conditional_expr() throws SyntaxException, LexicalException {
        IToken firstToken =peek();
        if(match(IToken.Kind.RES_if)){
            //Expr guard = previous();
            Expr guard = expression();
            advance();
            Expr trueCase = expression();
            advance();
            Expr falseCase = expression();
            return new ConditionalExpr(firstToken, guard, trueCase, falseCase);
        }

        return conditional_expr();
    }
    private Expr or_expression() throws SyntaxException, LexicalException {

        IToken firstToken = peek();
        Expr expr = and_expr();
        while (match(IToken.Kind.BITOR, IToken.Kind.OR)) {
            IToken operator = previous();
            Expr right = and_expr();
            expr = new BinaryExpr(firstToken,expr, operator.getKind(), right);
        }

        return expr;
    }

    private Expr and_expr() throws SyntaxException, LexicalException {
        IToken firstToken = peek();
        Expr expr = comparison_expr();

        while(match(IToken.Kind.BITAND, IToken.Kind.AND)){
            IToken operator = previous();
            Expr right = comparison_expr();
            expr = new BinaryExpr(firstToken, expr, operator.getKind(),right);
        }
        return expr;
    }

    private Expr comparison_expr() throws SyntaxException, LexicalException {
        IToken firstToken = peek();
        Expr expr = power_expr();

        while(match(IToken.Kind.LT, IToken.Kind.GT, IToken.Kind.EQ, IToken.Kind.LE, IToken.Kind.GE)){
            IToken operator = previous();
            Expr right = power_expr();
            expr = new BinaryExpr(firstToken, expr, operator.getKind(),right);
        }
        return expr;
    }

    private Expr power_expr() throws SyntaxException, LexicalException {
        IToken firstToken = peek();
        Expr expr = additive_expr();
        if(match(IToken.Kind.EXP)){
            IToken operator = previous();
            Expr right = power_expr();
            expr = new BinaryExpr(firstToken, expr, operator.getKind(), right);
            return expr;
        }
        return expr;
    }

    private Expr additive_expr() throws SyntaxException, LexicalException {
        IToken firstToken = peek();
        Expr expr = multiplicative_expr();

        while(match(IToken.Kind.PLUS, IToken.Kind.MINUS)){
            IToken operator = previous();
            Expr right = multiplicative_expr();
            expr = new BinaryExpr(firstToken, expr, operator.getKind(),right);
        }
        return expr;
    }
    private Expr multiplicative_expr() throws SyntaxException, LexicalException {
        IToken firstToken = peek();
        Expr expr = unary_expr();

        while(match(IToken.Kind.TIMES, IToken.Kind.DIV, IToken.Kind.MOD)){
            IToken operator = previous();
            Expr right = unary_expr();
            expr = new BinaryExpr(firstToken, expr, operator.getKind(),right);
        }
        return expr;
    }
    private Expr unary_expr() throws SyntaxException, LexicalException {
        IToken firstToken = peek();
        Expr expr = null;
        if (match(IToken.Kind.BANG, IToken.Kind.MINUS, IToken.Kind.RES_cos, IToken.Kind.RES_sin, IToken.Kind.RES_atan)) {
            IToken operator = previous();
            expr = unary_expr();
            return new UnaryExpr(firstToken,operator.getKind(), expr);
        }else{
            expr = UnaryExprPostfix();
        }

        return expr;
    }

    private Expr UnaryExprPostfix() throws SyntaxException, LexicalException {
        IToken firstToken = peek();
        Expr expr = primary_expr();
        PixelSelector pixel = null;
        ColorChannel color = null;
        boolean postFix = false;
        if(match(IToken.Kind.LSQUARE)) {
            retreat();
            pixel = PixelSelector();
            postFix = true;
        }
        if(match(IToken.Kind.COLON)){
            retreat();
            color = ChannelSelector();
            postFix =true;
        }
        if(postFix) return new UnaryExprPostfix(firstToken, expr, pixel, color);
        return expr;
    }
    private Expr primary_expr() throws SyntaxException, LexicalException {
        IToken firstToken = peek();
        if (match(IToken.Kind.IDENT)) return new IdentExpr(firstToken);
        if (match(IToken.Kind.STRING_LIT)) return new StringLitExpr(firstToken);
        if (match(IToken.Kind.NUM_LIT)) return new NumLitExpr(firstToken);

        if (match(IToken.Kind.RES_Z)) {
            return new ZExpr(firstToken);
        }
        if(match(IToken.Kind.RES_rand)){
            return new RandomExpr(firstToken);
        }

        if (match(IToken.Kind.LPAREN)) {
            Expr expr = expression();
            consume(IToken.Kind.RPAREN, "Expect ')' after expression.");
            return expr;
        }
        if(match(IToken.Kind.RES_x, IToken.Kind.RES_y, IToken.Kind.RES_a, IToken.Kind.RES_r))
            return new PredeclaredVarExpr(firstToken);

        if (match(IToken.Kind.LSQUARE)) {
            Expr red = expression();
            consume(IToken.Kind.COMMA, "Expect ',' after expression.");
            Expr grn = expression();
            consume(IToken.Kind.COMMA, "Expect ',' after expression.");
            Expr blu = expression();
            consume(IToken.Kind.RSQUARE, "Expect '['");
            return new ExpandedPixelExpr(firstToken, red, grn, blu);
        }
        if(match(IToken.Kind.RES_x_cart, IToken.Kind.RES_y_cart, IToken.Kind.RES_a_polar, IToken.Kind.RES_r_polar)){
            IToken function = previous();
            PixelSelector pixel = PixelSelector();
            return new PixelFuncExpr(firstToken, function.getKind(), pixel);
        }
        error("Invalid token");
        return expression();
    }
    ColorChannel ChannelSelector() throws LexicalException, SyntaxException {
        IToken firstToken = peek();

        consume(IToken.Kind.COLON, ": expected");
        if(!match(IToken.Kind.RES_red, IToken.Kind.RES_grn, IToken.Kind.RES_blu))
            error("invalid color");
        return ColorChannel.getColor(previous());
    }
    PixelSelector PixelSelector() throws LexicalException, SyntaxException {
        IToken firstToken = peek();

        consume(IToken.Kind.LSQUARE, "[ expected");
        Expr width = expression();
        consume(IToken.Kind.COMMA, ", expected");
        Expr height = expression();
        consume(IToken.Kind.RSQUARE, "] expected");
        return new PixelSelector(firstToken, width, height);
    }

    LValue LValue() throws SyntaxException, LexicalException {
        IToken firstToken = peek();
        PixelSelector pixel = null;
        ColorChannel color = null;

        if(!match(IToken.Kind.IDENT)) error("Ident expected");
        Ident name = new Ident(previous());

        if(match(IToken.Kind.LSQUARE)) {
            retreat();
            pixel = PixelSelector();
        }
        if(match(IToken.Kind.COLON)){
            retreat();
            color = ChannelSelector();
        }
        return new LValue(firstToken, name, pixel, color);
    }

    public AST parse() throws PLCException{
        return Program();
    }
}
