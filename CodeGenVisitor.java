package edu.ufl.cise.plcsp23;

import edu.ufl.cise.plcsp23.ast.*;
import edu.ufl.cise.plcsp23.runtime.FileURLIO;
import edu.ufl.cise.plcsp23.runtime.ImageOps;
import edu.ufl.cise.plcsp23.runtime.PixelOps;

import java.util.List;

public class CodeGenVisitor implements ASTVisitor{

    String packageName;
    int scopeCounter;
    public CodeGenVisitor(String packageName) {
        this.packageName = packageName;
    }

    public Object visitAssignmentStatement(AssignmentStatement statementAssign, Object arg) throws PLCException{
        StringBuilder sb = (StringBuilder) arg;
        LValue lv = statementAssign.getLv();
        Expr expr = statementAssign.getE();

        for(int i=0; i<scopeCounter; i++)
            sb.append("\t");
        /*if(lv.getType()==Type.PIXEL){

        }*/
        if(lv.getType() == Type.IMAGE) {
            if(lv.getPixelSelector() != null && lv.getColor() != null){
                sb.append("for(int y = 0; y != "+lv.getIdent().getName()+".getHeight(); y++) {\n");
                for(int i=0; i<scopeCounter; i++)
                    sb.append("\t");
                sb.append("\tfor(int x = 0; x != "+lv.getIdent().getName()+".getWidth(); x++) {\n");
                for(int i=0; i<scopeCounter; i++)
                    sb.append("\t");
                sb.append("\t\tImageOps.setRGB("+lv.getIdent().getName()+", x, y, ");
                String set = switch(lv.getColor()){
                    case red -> "PixelOps.setRed(";
                    case grn -> "PixelOps.setGrn(";
                    case blu -> "PixelOps.setBlu(";
                    default -> throw new RuntimeException("error in program Type , unexpected type");
                };
                sb.append(set+"ImageOps.getRGB(");
                lv.visit(this,sb);
                sb.append(", x, y), ");
                expr.visit(this,sb);
                sb.append("));\n");
                for(int i=0; i<scopeCounter; i++)
                    sb.append("\t");
                sb.append("\t}\n");
                for(int i=0; i<scopeCounter; i++)
                    sb.append("\t");
                sb.append("}\n");
            }
            else if(lv.getPixelSelector() != null && lv.getColor() == null){
                sb.append("for(int x = 0; x != "+lv.getIdent().getName()+".getWidth(); x++) {\n");
                for(int i=0; i<scopeCounter; i++)
                    sb.append("\t");
                sb.append("\tfor(int y = 0; y != "+lv.getIdent().getName()+".getHeight(); y++) {\n");
                for(int i=0; i<scopeCounter; i++)
                    sb.append("\t");
                sb.append("\t\tImageOps.setRGB("+lv.getIdent().getName()+", ");
                lv.getPixelSelector().getX().visit(this,sb);
                sb.append(", ");
                lv.getPixelSelector().getY().visit(this,sb);
                sb.append(", ");
                expr.visit(this,sb);
                sb.append(");\n");
                for(int i=0; i<scopeCounter; i++)
                    sb.append("\t");
                sb.append("\t}\n");
                for(int i=0; i<scopeCounter; i++)
                    sb.append("\t");
                sb.append("}\n");
            }
            else if(lv.getPixelSelector() == null && lv.getColor() != null){

            }
            else{
                if(expr.getType()==Type.STRING){
                    sb.append("ImageOps.copyInto(FileURLIO.readImage(");
                    expr.visit(this,sb);
                    sb.append("), "+lv.getIdent().getName()+");\n");
                }
                if(expr.getType()==Type.IMAGE){
                    sb.append("ImageOps.copyInto(");
                    expr.visit(this,sb);
                    sb.append(", ");
                    lv.visit(this,sb);
                    sb.append(");\n");
                }
                if(expr.getType()==Type.PIXEL){
                    lv.visit(this, sb);
                    sb.append(" = ");
                    sb.append("ImageOps.setAllPixels(");
                    lv.visit(this,sb);
                    sb.append(", ");
                    expr.visit(this,sb);
                    sb.append(");\n");
                }
            }
        }
        else {
            lv.visit(this, sb);
            sb.append(" = ");

            if (statementAssign.getLv().getType() == Type.STRING && statementAssign.getE().getType() == Type.INT) {
                sb.append("Integer.toString(");
                statementAssign.getE().visit(this, sb);
                sb.append(")");
            } else if (statementAssign.getLv().getType() == Type.INT && statementAssign.getE().getType() == Type.STRING) {
                sb.append("Integer.parseInt(");
                statementAssign.getE().visit(this, sb);
                sb.append(")");
            }else if(lv.getType() == Type.STRING && expr.getType()==Type.PIXEL){
                sb.append("PixelOps.packedToString(");
                statementAssign.getE().visit(this, sb);
                sb.append(")");
            } else statementAssign.getE().visit(this, sb);
            sb.append(";\n");
        }
        return sb;
    }

    Type programType=null;

    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCException{
        StringBuilder sb = (StringBuilder) arg;
        IToken.Kind op = binaryExpr.getOp();
        Expr left = binaryExpr.getLeft();
        Expr right = binaryExpr.getRight();

        String operator = switch(op) {//AND, OR, PLUS, MINUS, TIMES, DIV, MOD, EQUALS, NOT_EQUALS, LT, LE, GT,GE
            case EQ -> "==";
            case PLUS -> "+";
            case MINUS -> "-";
            case TIMES -> "*";
            case DIV -> "/";
            case MOD -> "%";
            case EXP -> "Math.pow(";
            case LT -> "<";
            case LE -> "<=";
            case GT -> ">";
            case GE -> ">=";
            case OR -> "||";
            case AND -> "&&";
            case BITOR -> "|";
            case BITAND -> "&";

            default -> {
                throw new PLCException("compiler error");
            }
        };

        if((binaryExpr.getLeft().getType()==Type.IMAGE && binaryExpr.getRight().getType()==Type.IMAGE)){
            String OP = switch(op) {
                case PLUS -> "ImageOps.OP.PLUS";
                case MINUS -> "ImageOps.OP.MINUS";
                case TIMES -> "ImageOps.OP.TIMES";
                case DIV -> "ImageOps.OP.DIV";
                case MOD -> "ImageOps.OP.MOD";
                case EXP -> "**";
                default -> {
                    throw new PLCException("compiler error");
                }
            };
            if (operator == "+" || operator == "-" || operator == "*" || operator == "/" || operator == "%") {
                sb.append("ImageOps.binaryImageImageOp(");
                sb.append(OP + ", ");
                binaryExpr.getLeft().visit(this, sb);
                sb.append(", ");
                binaryExpr.getRight().visit(this, sb);
                sb.append(")");
            }
        }
        else if(binaryExpr.getLeft().getType()==Type.IMAGE && binaryExpr.getRight().getType()==Type.INT){
            String OP = switch(op) {
                case PLUS -> "ImageOps.OP.PLUS";
                case MINUS -> "ImageOps.OP.MINUS";
                case TIMES -> "ImageOps.OP.TIMES";
                case DIV -> "ImageOps.OP.DIV";
                case MOD -> "ImageOps.OP.MOD";
                case EXP -> "**";
                default -> {
                    throw new PLCException("compiler error");
                }
            };
            if (operator == "+" || operator == "-" || operator == "*" || operator == "/" || operator == "%") {
                sb.append("ImageOps.binaryImageScalarOp(");
                sb.append(OP + ", ");
                binaryExpr.getLeft().visit(this, sb);
                sb.append(", ");
                binaryExpr.getRight().visit(this, sb);
                sb.append(")");
            }
        }
        else if(binaryExpr.getLeft().getType()==Type.PIXEL && binaryExpr.getRight().getType()==Type.PIXEL){
            String OP = switch(op) {
                case PLUS -> "ImageOps.OP.PLUS";
                case MINUS -> "ImageOps.OP.MINUS";
                case TIMES -> "ImageOps.OP.TIMES";
                case DIV -> "ImageOps.OP.DIV";
                case MOD -> "ImageOps.OP.MOD";
                case EXP -> "**";
                case EQ -> "ImageOps.BoolOP.EQUALS";
                case BITAND -> "valid";
                case BITOR -> "valid";
                default -> {
                    throw new PLCException("compiler error");
                }
            };
            if (operator == "+" || operator == "-" || operator == "*" || operator == "/" || operator == "%") {
                sb.append("ImageOps.binaryPackedPixelPixelOp(");
                sb.append(OP + ", ");
                binaryExpr.getLeft().visit(this, sb);
                sb.append(", ");
                binaryExpr.getRight().visit(this, sb);
                sb.append(")");
            }
            if(operator == "|" || operator =="&"){
                sb.append("(");
                left.visit(this,sb);
                sb.append(" "+operator+" ");
                right.visit(this,sb);
                sb.append(")");
            }
        }
        else if(left.getType()==Type.PIXEL && right.getType() == Type.INT){
            String OP = switch(op) {
                case PLUS -> "ImageOps.OP.PLUS";
                case MINUS -> "ImageOps.OP.MINUS";
                case TIMES -> "ImageOps.OP.TIMES";
                case DIV -> "ImageOps.OP.DIV";
                case MOD -> "ImageOps.OP.MOD";
                case EXP -> "**";
                default -> {
                    throw new PLCException("compiler error");
                }
            };
            if (operator == "+" || operator == "-" || operator == "*" || operator == "/" || operator == "%") {
                sb.append("ImageOps.binaryPackedPixelIntOp(");
                sb.append(OP + ", ");
                binaryExpr.getLeft().visit(this, sb);
                sb.append(", ");
                binaryExpr.getRight().visit(this, sb);
                sb.append(")");
            }
            if(operator == "Math.pow("){
                sb.append("PixelOps.pack((int) Math.pow(PixelOps.red(");
                left.visit(this,sb);
                sb.append("), ");
                right.visit(this,sb);
                sb.append("), (int) Math.pow(PixelOps.grn(");
                left.visit(this,sb);
                sb.append("), ");
                right.visit(this,sb);
                sb.append("), (int) Math.pow(PixelOps.blu(");
                left.visit(this,sb);
                sb.append("), ");
                right.visit(this,sb);
                sb.append("))");
            }
        }
        else if(operator == "Math.pow("){
            sb.append("(int)" +operator);
            binaryExpr.getLeft().visit(this, arg);
            sb.append(", ");
            binaryExpr.getRight().visit(this, arg);
            sb.append(")");
        }
        else if(operator == ">" || operator== "<" || operator== ">=" || operator == "<=" || operator == "==" ) {
            sb.append("(");
            binaryExpr.getLeft().visit(this, arg);
            sb.append(" "+operator+" ");
            binaryExpr.getRight().visit(this, arg);
            sb.append(" ? 1 : 0)");
        }
        else if(operator == "&&" || operator == "||"){
            sb.append("(");
            sb.append("(");
            binaryExpr.getLeft().visit(this, arg);
            sb.append("!=0)");
            sb.append(" "+operator+" ");
            sb.append("(");
            binaryExpr.getRight().visit(this, arg);
            sb.append("!=0)");
            sb.append(" ? 1 : 0)");
        }
        else if(operator=="-"){
            binaryExpr.getLeft().visit(this, arg);
            sb.append(operator+"(");
            binaryExpr.getRight().visit(this, arg);
            sb.append(")");
        }
        else {
            binaryExpr.getLeft().visit(this, arg);
            sb.append(operator);
            binaryExpr.getRight().visit(this, arg);
        }

        return sb;
    }

    public Object visitBlock(Block block, Object arg) throws PLCException{
        StringBuilder sb = (StringBuilder) arg;
        List<Declaration> decs = block.getDecList();
        List<Statement> statements = block.getStatementList();
        for (Declaration dec : decs) {
            dec.visit(this, sb);
        }
        for (Statement statement : statements) {
            statement.visit(this, sb);
        }

        return sb;
    }

    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCException{
        StringBuilder sb = (StringBuilder) arg;
        sb.append("((");
        conditionalExpr.getGuard().visit(this, sb);
        sb.append(">= 1");
        sb.append(")? ");
        conditionalExpr.getTrueCase().visit(this, sb);
        sb.append(" : ");
        conditionalExpr.getFalseCase().visit(this, sb);
        sb.append(')');
        return sb;
    }

    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCException{
        StringBuilder sb = (StringBuilder) arg;
        for(int i=0; i<scopeCounter; i++)
            sb.append("\t");
        NameDef namedef = declaration.getNameDef();
        Expr initializer = declaration.getInitializer();

        declaration.getNameDef().visit(this, sb);

        if(initializer!=null){
            sb.append(" = ");
            if(namedef.getType() == Type.IMAGE) {
                if(namedef.getDimension() == null){
                    if (initializer.getType() == Type.STRING) {
                        sb.append("FileURLIO.readImage(");
                        initializer.visit(this, sb);
                        sb.append(")");
                    }
                    else if(initializer.getType() == Type.IMAGE){
                        sb.append("ImageOps.cloneImage(");
                        initializer.visit(this,sb);
                        sb.append(")");
                    }
                }
                else{
                    if(initializer.getType()==Type.STRING){
                        sb.append("FileURLIO.readImage(");
                        initializer.visit(this, sb);
                        sb.append(", ");
                        namedef.getDimension().visit(this,sb);
                        sb.append(")");
                    }
                    else if(initializer.getType()==Type.IMAGE){
                        sb.append("ImageOps.copyAndResize(");
                        initializer.visit(this,sb);
                        sb.append(", ");
                        namedef.getDimension().visit(this,sb);
                        sb.append(")");
                    }
                    else if(initializer.getType() == Type.PIXEL){
                        sb.append("ImageOps.setAllPixels(");
                        sb.append("ImageOps.makeImage(");
                        namedef.getDimension().visit(this, sb);
                        sb.append("), ");
                        initializer.visit(this, sb);
                        sb.append(")");
                    }
                }
            }
            else if(namedef.getType()==Type.PIXEL){
                initializer.visit(this, sb);
            }
            else if(declaration.getNameDef().getType() == Type.STRING && initializer.getType() == Type.INT){
                sb.append("Integer.toString(");
                initializer.visit(this,sb);
                sb.append(")");
            }
            else if(declaration.getNameDef().getType() == Type.INT && initializer.getType() == Type.STRING){
                sb.append("Integer.parseInt(");
                initializer.visit(this,sb);
                sb.append(")");
            }
            else initializer.visit(this,arg);
        }
        else if(namedef.getType() == Type.IMAGE){
            if(namedef.getDimension()!=null) {
                sb.append(" = ImageOps.makeImage(");
                declaration.getNameDef().getDimension().visit(this, sb);
                sb.append(")");
            }
        }
        sb.append(";\n");
        return sb;
    }

    public Object visitDimension(Dimension dimension, Object arg) throws PLCException{
        StringBuilder sb =(StringBuilder) arg;
        dimension.getWidth().visit(this, sb);
        sb.append(", ");
        dimension.getHeight().visit(this, sb);
        return sb;
    }

    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCException{
        StringBuilder sb = (StringBuilder) arg;
        sb.append("PixelOps.pack(");
        expandedPixelExpr.getRedExpr().visit(this,sb);
        sb.append(", ");
        expandedPixelExpr.getGrnExpr().visit(this,sb);
        sb.append(", ");
        expandedPixelExpr.getBluExpr().visit(this,sb);
        sb.append(")");
        return sb;
    }

    public Object visitIdent(Ident ident, Object arg) throws PLCException{
        StringBuilder sb = (StringBuilder) arg;
        sb.append(ident.getName());
        return sb;
    }

    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCException{
        StringBuilder sb = (StringBuilder) arg;
        sb.append(identExpr.getName());
        return sb;
    }

    public Object visitLValue(LValue lValue, Object arg) throws PLCException{
        StringBuilder sb = (StringBuilder) arg;
        sb.append(lValue.getIdent().getName());
        return sb;
    }

    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCException{
        StringBuilder sb = (StringBuilder) arg;
        String paramType = switch(nameDef.getType()){
            case IMAGE -> "BufferedImage";
            case PIXEL -> "int";
            case INT -> "int";
            case STRING -> "String";
            case VOID -> "void";
            default -> throw new RuntimeException("error in program Type , unexpected type");
        };
        sb.append(paramType).append(" ").append(nameDef.getIdent().getName());
        return null;
    }

    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCException{
        StringBuilder sb = (StringBuilder) arg;
        sb.append(numLitExpr.getValue());
        return sb;
    }

    public Object visitPixelFuncExpr(PixelFuncExpr pixelFuncExpr, Object arg) throws PLCException{
        return null;
    }

    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCException{
        StringBuilder sb = (StringBuilder) arg;
        pixelSelector.getX().visit(this,sb);
        sb.append(", ");
        pixelSelector.getY().visit(this,sb);
        return sb;
    }

    public Object visitPredeclaredVarExpr(PredeclaredVarExpr predeclaredVarExpr, Object arg) throws PLCException{
        StringBuilder sb = (StringBuilder) arg;
        Token.Kind var = predeclaredVarExpr.getKind();
        if(var == Token.Kind.RES_x)
            sb.append("x");
        if(var == Token.Kind.RES_y)
            sb.append("y");
        return sb;
    }

    public Object visitProgram(Program program, Object arg) throws PLCException{
        StringBuilder sb = new StringBuilder();
        programType = program.getType();
        List<NameDef> params = program.getParamList();
        if(!packageName.equals("")){
            sb.append("package ").append(packageName).append(';').append('\n');
        }

        sb.append("import edu.ufl.cise.plcsp23.runtime.*;\n");
        sb.append("import java.awt.image.BufferedImage;\n");
        sb.append("import java.lang.Math;\n\n");

        sb.append("public class ").append(program.getIdent().getName()).append('{').append('\n');
        scopeCounter =1;
        String returnType = switch(program.getType()){
            case IMAGE -> "BufferedImage";
            case PIXEL -> "int";
            case INT -> "int";
            case STRING -> "String";
            case VOID -> "void";
            default -> throw new RuntimeException("error in program Type , unexpected type");
        };

        sb.append('\t').append("public static "). append(returnType).append(" apply(");
        for(NameDef param : params){
            visitNameDef(param, sb);
            if(params.indexOf(param) != (params.size()-1))
                sb.append(", ");
        }
        sb.append("){\n");
        scopeCounter++;
        visitBlock(program.getBlock(), sb);
        sb.append("\t}\n").append("}");
        return sb.toString();
    }

    public Object visitRandomExpr(RandomExpr randomExpr, Object arg) throws PLCException{
        /*Generate code for a random int in [0,256)
        using Math.floor(Math.random() * 256)
        This will require an import statement.*/

        StringBuilder sb = (StringBuilder) arg;
        sb.append("(int)Math.floor(Math.random()*256)");
        return sb;
    }

    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg)throws PLCException{
        StringBuilder sb = (StringBuilder) arg;
        for(int i=0; i<scopeCounter; i++)
            sb.append("\t");
        sb.append("return ");
        if(programType == Type.STRING && returnStatement.getE().getType() == Type.INT){
            sb.append("Integer.toString(");
            returnStatement.getE().visit(this,sb);
            sb.append(")");
        }
        else if(programType == Type.STRING && returnStatement.getE().getType() == Type.PIXEL){
            sb.append("PixelOps.packedToString(");
            returnStatement.getE().visit(this,sb);
            sb.append(")");
        }
        else if(programType == Type.INT && returnStatement.getE().getType() == Type.STRING){
            sb.append("Integer.parseInt(");
            returnStatement.getE().visit(this,sb);
            sb.append(")");
        }
        else returnStatement.getE().visit(this,sb);
        sb.append(";\n");


        return sb;
    }

    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCException{
        StringBuilder sb = (StringBuilder) arg;
        sb.append("\"").append(stringLitExpr.getValue()).append("\"");
        return sb;
    }

    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCException{
        StringBuilder sb = (StringBuilder) arg;
        Expr expr = unaryExpr.getE();
        Token.Kind op = unaryExpr.getOp();

        if(expr.getType() == Type.INT){
            if(op == Token.Kind.BANG) {
                unaryExpr.getE().visit(this,sb);
                sb.append("==0 ? 1 : 0");
            }
            else if( op == Token.Kind.MINUS) {
                sb.append("-(");
                unaryExpr.getE().visit(this, sb);
                sb.append(")");
            }
        }

        return sb;
    }

    public Object visitUnaryExprPostFix(UnaryExprPostfix unaryExprPostfix, Object arg) throws PLCException{
        StringBuilder sb = (StringBuilder) arg;
        if(unaryExprPostfix.getPrimary().getType() == Type.IMAGE){
            if(unaryExprPostfix.getColor() == null) {
                sb.append("ImageOps.getRGB(");
                unaryExprPostfix.getPrimary().visit(this, sb);
                sb.append(", ");
                unaryExprPostfix.getPixel().visit(this, sb);
                sb.append(")");
            }
            else if(unaryExprPostfix.getPixel() == null){
                String extract = switch(unaryExprPostfix.getColor()){
                    case red -> "ImageOps.extractRed(";
                    case grn -> "ImageOps.extractGrn(";
                    case blu -> "ImageOps.extractBlu(";
                    default -> throw new RuntimeException("error in program Type , unexpected type");
                };
                sb.append(extract);
                unaryExprPostfix.getPrimary().visit(this, sb);
                sb.append(")");
            }
            else{
                String pixelOp = switch(unaryExprPostfix.getColor()){
                    case red -> "PixelOps.red(";
                    case grn -> "PixelOps.grn(";
                    case blu -> "PixelOps.blu(";
                    default -> throw new RuntimeException("error in program Type , unexpected type");
                };
                sb.append(pixelOp);
                sb.append("ImageOps.getRGB(");
                unaryExprPostfix.getPrimary().visit(this, sb);
                sb.append(", ");
                unaryExprPostfix.getPixel().visit(this, sb);
                sb.append("))");
            }
        }
        else if(unaryExprPostfix.getPrimary().getType() == Type.PIXEL){
            String pixelOp = switch(unaryExprPostfix.getColor()){
                case red -> "PixelOps.red(";
                case grn -> "PixelOps.grn(";
                case blu -> "PixelOps.blu(";
                default -> throw new RuntimeException("error in program Type , unexpected type");
            };
            sb.append(pixelOp);
            unaryExprPostfix.getPrimary().visit(this, sb);
            sb.append(")");
        }
        return sb;
    }

    public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws PLCException{
        StringBuilder sb = (StringBuilder) arg;
        for(int i=0; i<scopeCounter; i++)
            sb.append("\t");
        sb.append("while(");
        whileStatement.getGuard().visit(this,sb);
        sb.append("!= 0){\n");
        //sb.append("){\n");
        scopeCounter++;
        whileStatement.getBlock().visit(this,sb);
        scopeCounter--;
        for(int i=0; i<scopeCounter; i++)
            sb.append("\t");
        sb.append("}\n");
        return sb;
    }

    public Object visitWriteStatement(WriteStatement statementWrite, Object arg) throws PLCException{
        StringBuilder sb = (StringBuilder) arg;
        for(int i=0; i<scopeCounter; i++)
            sb.append("\t");
        if(statementWrite.getE().getType()==Type.PIXEL)
            sb.append("ConsoleIO.writePixel(");
        else sb.append("ConsoleIO.write(");
        statementWrite.getE().visit(this, sb);
        sb.append(");\n");
        return sb;
    }

    public Object visitZExpr(ZExpr zExpr, Object arg) throws PLCException{
        StringBuilder sb = (StringBuilder)arg;
        sb.append("255");
        return sb;
    }

}
