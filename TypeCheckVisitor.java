package edu.ufl.cise.plcsp23;

import edu.ufl.cise.plcsp23.ast.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class TypeCheckVisitor implements ASTVisitor {

    public static class Pair{
        int scopeLevel;
        NameDef namedef;
        public Pair(int scope, NameDef namedef){
            scopeLevel = scope;
            this.namedef = namedef;
        }
        public int getScope(){return scopeLevel;}
        public NameDef getNameDef(){return namedef;}
    }
    public static class SymbolTable {
        HashMap<String, ArrayList<Pair>> entries = new HashMap<>();
        //HashMap<String, HashMap<Integer, NameDef>> entries = new HashMap<>();
        Stack<Integer> scope_stack  = new Stack<Integer>();
        List<List<String>> localNames = new ArrayList<>();
        int current_scope;
        int next_num;
        public SymbolTable(){
            current_scope=-1;
            next_num=0;
        }

        public boolean insert(String name, Pair pair) {
            if(entries.containsKey(name)){
                if(entries.get(name).get(0).getScope() == pair.getScope())
                    return false;
                else{
                    entries.get(name).add(0,pair);
                    return true;
                }
            }
            else {

                ArrayList<Pair> list = new ArrayList<>();
                list.add(pair);
                entries.put(name, list);
                return true;
            }
        }
        /*public boolean insert(String name, int scope, NameDef namedef){
            if(entries.containsKey(name)){
                if(entries.get(name).containsKey(scope)){
                    return false;
                }
                else {
                    entries.get(name).put(scope, namedef);
                    return true;
                }
            }
            HashMap<Integer, NameDef> second = new HashMap<>();
            second.put(scope, namedef);
            entries.put(name, second);
            return true;
        }*/

        public void delete(String name, Pair pair){
            entries.get(name).remove(pair);
        }

        public NameDef lookup(String name, int scope) throws TypeCheckException {
            if(entries.get(name) != null) {
                if(entries.get(name).size()!=0) {
                    int atScope = entries.get(name).get(0).getScope();
                    for (int num : scope_stack) {
                        if (atScope == num) return entries.get(name).get(0).getNameDef();
                    }
                }
                else throw new TypeCheckException("variable "+name+" has not been declared in this scope");
            }
            return null;
        }

        /*public NameDef lookup(String name, int scope){
            if(entries.containsKey(name)){
                if(entries.get(name).containsKey(scope)){
                    return entries.get(name).get(scope);
                }
            }
            return null;
        }*/

        public void enterScope(){
            current_scope = next_num;
            next_num++;
            scope_stack.push(current_scope);
        }
        public void closeScope(){
            int eraseNum=scope_stack.pop();
            if(scope_stack.size()!=0)
                current_scope = scope_stack.peek();
            else current_scope=-1;
            for (String key : entries.keySet()) {
                if(entries.get(key).size()!=0) {
                    if (entries.get(key).get(0).getScope() == eraseNum) {
                        delete(key, entries.get(key).get(0));
                    }
                }
            }
        }
        public int getScope(){return current_scope;}
    }

    SymbolTable symbolTable = new SymbolTable();
    Type programType;
    private void check(boolean condition, AST node, String message) throws TypeCheckException {
        if (!condition) {
            throw new TypeCheckException(message);
        }
    }

    private boolean assignmentCompatible(Type targetType, Type rhsType) {
        if(targetType == Type.VOID)
            return false;
        if(targetType == Type.IMAGE)
            return (rhsType == Type.IMAGE || rhsType == Type.PIXEL || rhsType == Type.STRING);
        if(targetType == Type.PIXEL)
            return (rhsType == Type.PIXEL || rhsType == Type.INT);
        if(targetType == Type.INT)
            return (rhsType == Type.PIXEL || rhsType == Type.INT);

        return (rhsType == Type.IMAGE || rhsType == Type.PIXEL || rhsType == Type.STRING || rhsType == Type.INT);
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCException {
        numLitExpr.setType(Type.INT);
        return Type.INT;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCException {
        stringLitExpr.setType(Type.STRING);
        return Type.STRING;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCException {
        String name = identExpr.getName();

        NameDef nd = symbolTable.lookup(name, symbolTable.getScope());
        check(nd != null, identExpr, "undefined identifier " + name);
        identExpr.appendName("_"+symbolTable.entries.get(name).get(0).getScope());
        Type type = nd.getType();
        identExpr.setType(type);
        return type;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCException {
        Expr expr = returnStatement.getE();
        Type exprType = (Type) expr.visit(this,arg);
        check(assignmentCompatible(programType, exprType), returnStatement, programType+ " type return expected");
        return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement statementAssign, Object arg) throws PLCException {
        LValue lv = statementAssign.getLv();

        Type lvType= (Type) lv.visit(this,arg);

        Expr expr = statementAssign.getE();
        Type exprType = (Type)expr.visit(this,arg);
        check(assignmentCompatible(lvType,exprType),statementAssign, "Lvalue type and expression type are not compatible");
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCException {
        IToken.Kind op = binaryExpr.getOp();
        Type leftType = (Type) binaryExpr.getLeft().visit(this, arg);
        Type rightType = (Type) binaryExpr.getRight().visit(this, arg);
        Type resultType = null;
        switch(op) {//AND, OR, PLUS, MINUS, TIMES, DIV, MOD, EQUALS, NOT_EQUALS, LT, LE, GT,GE
            case EQ -> {
                check(leftType == rightType, binaryExpr, "incompatible types for comparison");
                resultType = Type.INT;
            }
            case PLUS -> {
                if (leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
                else if (leftType == Type.STRING && rightType == Type.STRING) resultType = Type.STRING;
                else if (leftType == Type.PIXEL && rightType == Type.PIXEL) resultType = Type.PIXEL;
                else if (leftType == Type.IMAGE && rightType == Type.IMAGE) resultType = Type.IMAGE;
                else check(false, binaryExpr, "incompatible types for operator");
            }
            case MINUS -> {
                if (leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
                else if (leftType == Type.PIXEL && rightType == Type.PIXEL) resultType = Type.PIXEL;
                else if (leftType == Type.IMAGE && rightType == Type.IMAGE) resultType = Type.IMAGE;
                else check(false, binaryExpr, "incompatible types for operator");
            }
            case TIMES, DIV, MOD-> {
                if (leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
                else if (leftType == Type.IMAGE && rightType == Type.INT) resultType = Type.IMAGE;
                else if (leftType == Type.PIXEL && rightType == Type.PIXEL) resultType = Type.PIXEL;
                else if (leftType == Type.IMAGE && rightType == Type.IMAGE) resultType = Type.IMAGE;
                else if (leftType == Type.PIXEL && rightType == Type.INT) resultType = Type.PIXEL;
                else check(false, binaryExpr, "incompatible types for operator");
            }
            case EXP -> {
                if (leftType == Type.INT && rightType == Type.INT) resultType = Type.INT;
                else if (leftType == Type.PIXEL && rightType == Type.INT) resultType = Type.PIXEL;
                else check(false, binaryExpr, "incompatible types for operator");
            }
            case LT, LE, GT, GE, OR, AND -> {
                if (leftType == Type.INT && rightType==Type.INT) resultType = Type.INT;
                else check(false, binaryExpr, "incompatible types for operator");
            }
            case BITOR, BITAND -> {
                if (leftType == Type.PIXEL && rightType==Type.PIXEL) resultType = Type.PIXEL;
                else check(false, binaryExpr, "incompatible types for operator");
            }

            default -> {
                throw new PLCException("compiler error");
            }
        }
        binaryExpr.setType(resultType);
        return resultType;
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCException {
        List<Declaration> decs = block.getDecList();
        List<Statement> statements = block.getStatementList();
        for (Declaration dec : decs) {
            dec.visit(this, arg);
        }
        for (Statement statement : statements) {
            statement.visit(this, arg);
        }

        return block;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCException {
        Expr guard = conditionalExpr.getGuard();
        Expr trueCase= conditionalExpr.getTrueCase();
        Expr falseCase = conditionalExpr.getFalseCase();

        Type guardType = (Type) guard.visit(this, arg);
        Type trueType = (Type) trueCase.visit(this, arg);
        Type falseType = (Type) falseCase.visit(this, arg);
        check(guardType == Type.INT, conditionalExpr, "int type expected for guard");
        check(trueType == falseType, conditionalExpr, "true case type does not match false case type");

        conditionalExpr.setType(trueType);
        return trueType;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCException {
        Expr initializer = declaration.getInitializer();
        if(initializer != null){
            Type initType = (Type)initializer.visit(this,arg);
            check(assignmentCompatible(declaration.getNameDef().getType(),initType), declaration, "declared type and initializer type are not compatible");

        }
        NameDef namedef = declaration.getNameDef();
        namedef.visit(this,arg);
        Type nameType = namedef.getType();

        if(nameType == Type.IMAGE){
            if( initializer == null)
            check(namedef.getDimension() != null, declaration, "name type image expects dimension or initializer");
        }
        return null;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCException {
        Expr width = dimension.getWidth();
        Expr height = dimension.getHeight();

        Type widthType = (Type) width.visit(this,arg);
        Type heightType = (Type) height.visit(this,arg);
        check((widthType == Type.INT && heightType == Type.INT), dimension, "int types expected");
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCException {
        Type red = (Type) expandedPixelExpr.getRedExpr().visit(this,arg);
        Type green = (Type) expandedPixelExpr.getGrnExpr().visit(this,arg);
        Type blue = (Type) expandedPixelExpr.getBluExpr().visit(this,arg);
        check(red == Type.INT && green == Type.INT && blue == Type.INT, expandedPixelExpr, "expected int types for rgb values");
        expandedPixelExpr.setType(Type.PIXEL);
        return Type.PIXEL;
    }

    @Override
    public Object visitIdent(Ident ident, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCException {
        String name = lValue.getIdent().getName();
        NameDef nd = symbolTable.lookup(name, symbolTable.getScope());
        check(nd != null, lValue, "undefined identifier " + name);
        lValue.getIdent().appendName("_"+symbolTable.entries.get(name).get(0).getScope());
        Type type = nd.getType();
        PixelSelector pix = lValue.getPixelSelector();
        ColorChannel color = lValue.getColor();
        Type resultType = null;
        switch(type) {
            case IMAGE -> {
                if(pix == null && color == null) resultType = Type.IMAGE;
                else if(pix != null && color == null) resultType = Type.PIXEL;
                else if(pix == null && color != null) resultType = Type.IMAGE;
                else resultType = Type.INT;
            }
            case PIXEL -> {
                if(pix == null && color == null) resultType = Type.PIXEL;
                else if(pix == null && color != null) resultType = Type.INT;
                else check(false, lValue, "expect null pixel selector value for pixel identifier");
            }
            case STRING -> {
                check(pix == null && color == null, lValue, "expect no pixel selector or channel selector values for string type identifier");
                resultType = Type.STRING;
            }
            case INT -> {
                check(pix == null && color == null, lValue, "expect no pixel selector or channel selector values for int type identifier");
                resultType = Type.INT;
            }
            default -> {
                throw new PLCException("compiler error");
            }
        }
        lValue.setType(type);
        return resultType;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCException {
        Type type = nameDef.getType();
        Ident ident = nameDef.getIdent();
        Dimension dim = nameDef.getDimension();

        check(type != Type.VOID, nameDef, "invalid type void");
        if (dim != null) {
            check(type == Type.IMAGE, dim, ("expected image type"));
            dim.visit(this, arg);
        }
        String name = nameDef.getIdent().getName();
        nameDef.getIdent().appendName("_"+symbolTable.getScope());
        Pair pair = new Pair(symbolTable.getScope(), nameDef);
        boolean inserted = symbolTable.insert(name, pair);
        check(inserted, nameDef, "variable " + name + " already declared");
        return null;
    }

    @Override
    public Object visitPixelFuncExpr(PixelFuncExpr pixelFuncExpr, Object arg) throws PLCException {
        PixelSelector pix = pixelFuncExpr.getSelector();
        pix.visit(this,arg);
        pixelFuncExpr.setType(Type.INT);
        return Type.INT;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCException {
        Expr x = pixelSelector.getX();
        Expr y = pixelSelector.getY();
        Type xType = (Type) x.visit(this,arg);
        Type yType = (Type) y.visit(this,arg);
        check(xType == Type.INT && yType == Type.INT, pixelSelector, "expect int types for x and y values");
        return null;
    }

    @Override
    public Object visitPredeclaredVarExpr(PredeclaredVarExpr predeclaredVarExpr, Object arg) throws PLCException {
        predeclaredVarExpr.setType(Type.INT);
        return Type.INT;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCException {
        programType = (Type) program.getType();
        symbolTable.enterScope();
        List<NameDef> params = program.getParamList();
        for (NameDef namedef : params) {
            namedef.visit(this, arg);
        }
        Block block = program.getBlock();
        block.visit(this, arg);
        symbolTable.closeScope();
        return program;
    }

    @Override
    public Object visitRandomExpr(RandomExpr randomExpr, Object arg) throws PLCException{
        randomExpr.setType(Type.INT);
        return Type.INT;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCException{
        Expr expr = unaryExpr.getE();
        Type exprType = (Type) expr.visit(this,arg);
        IToken.Kind op = unaryExpr.getOp();
        Type resultType = null;
        switch(op){
            case BANG -> {
                if(exprType == Type.INT)resultType = Type.INT;
                else if(exprType == Type.PIXEL)resultType = Type.PIXEL;
                else check(false, unaryExpr, "int or pixel type expected after ! operator");
            }
            case MINUS, RES_cos, RES_sin, RES_atan -> {
                if(exprType == Type.INT)resultType = Type.INT;
                else check(false, unaryExpr, "int type expected after operator");
            }
            default -> {
                throw new PLCException("compiler error");
            }
        }
        unaryExpr.setType(resultType);
        return resultType;
    }

    @Override
    public Object visitUnaryExprPostFix(UnaryExprPostfix unaryExprPostfix, Object arg) throws PLCException{
        Expr primary = unaryExprPostfix.getPrimary();
        Type pType = (Type) primary.visit(this,arg);
        Type resultType =null;

        if(unaryExprPostfix.getPixel() == null){
            check(unaryExprPostfix.getColor() != null, unaryExprPostfix, "at least one of PixelSelector or ChannelSelector should be present in order to create a\n" +
                    "UnayrExprPostfix object");
            check(pType == Type.PIXEL || pType==Type.IMAGE, unaryExprPostfix, "Pixel or image type expected for unaryexprpostfix");
            if(pType==Type.PIXEL)resultType = Type.INT;
            else resultType = Type.IMAGE;
        }
        if(unaryExprPostfix.getPixel() != null){
            unaryExprPostfix.getPixel().visit(this,arg);
            check(pType==Type.IMAGE, unaryExprPostfix, "Image type expected for unaryexprpostfix, pixelselector exists");

            if(unaryExprPostfix.getColor() == null) resultType = Type.PIXEL;
            else resultType=Type.INT;
        }

        unaryExprPostfix.setType(resultType);
        return resultType;
    }

    @Override
    public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws PLCException{
        Expr expr = whileStatement.getGuard();
        Type exprType = (Type) expr.visit(this,arg);
        check(exprType == Type.INT, whileStatement, "while expression type expected to be int");
        symbolTable.enterScope();
        Block block = whileStatement.getBlock();
        block.visit(this,arg);
        symbolTable.closeScope();
        return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement statementWrite, Object arg) throws PLCException{
        Expr expr = statementWrite.getE();
        expr.visit(this,arg);
        return null;
    }

    @Override
    public Object visitZExpr(ZExpr zExpr, Object arg) throws PLCException{
        zExpr.setType(Type.INT);
        return Type.INT;
    }
}

