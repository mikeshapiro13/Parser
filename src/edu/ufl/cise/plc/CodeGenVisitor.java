package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;

import java.util.List;
import java.util.Locale;

public class CodeGenVisitor implements ASTVisitor
{
    //StringBuilder prog = new StringBuilder("");
    private final String packageName;
    public CodeGenVisitor(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
        StringBuilder bool = new StringBuilder();
        bool.append(booleanLitExpr.getText());
        return bool;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
        StringBuilder string = new StringBuilder();
        string.append("\"\"\"").append(stringLitExpr.getValue()).append("\"\"\"");
        return string;
    }

    @Override
    public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
        StringBuilder intLit = new StringBuilder();
        if (intLitExpr.getCoerceTo() != null && intLitExpr.getCoerceTo() != Types.Type.INT)
        {
            intLit.append("(").append(intLitExpr.getCoerceTo()).append(")");
        }
        intLit.append(intLitExpr.getText());
        return intLit;
    }

    @Override
    public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
        StringBuilder f = new StringBuilder();
        if (floatLitExpr.getCoerceTo() != null && floatLitExpr.getCoerceTo() != Types.Type.FLOAT)
        {
            f.append("(").append(floatLitExpr.getType()).append(")");
        }
        f.append(floatLitExpr.getText());
        return f;
    }

    @Override
    public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
        return null;
    }

    @Override
    public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
        return null;
    }

    @Override
    public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpression, Object arg) throws Exception {
        StringBuilder u = new StringBuilder();
        u.append("(").append(unaryExpression.getOp().getText());
        u.append(" ").append(unaryExpression.getExpr().getText()).append(")");
        return u;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
        StringBuilder b = new StringBuilder();
        b.append("(").append(binaryExpr.getLeft().getText());
        b.append(" ").append(binaryExpr.getOp().getText());
        b.append(" ").append(binaryExpr.getRight().getText()).append(")");
        return b;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
        StringBuilder id = new StringBuilder();
        if (identExpr.getCoerceTo() != null && identExpr.getCoerceTo() != identExpr.getType())
        {
            id.append("(").append(identExpr.getCoerceTo()).append(")");
        }
        id.append(identExpr.getText());
        return id;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
        StringBuilder c = new StringBuilder();
        c.append("(").append(conditionalExpr.getCondition().getText()).append(")");
        c.append(" ? ").append(conditionalExpr.getTrueCase().getText());
        c.append(" : ").append(conditionalExpr.getFalseCase().getText());
        return c;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws Exception {
        return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
        return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
        StringBuilder a = new StringBuilder();
        a.append(assignmentStatement.getName()).append(" = ");
        a.append(assignmentStatement.getExpr().getText()).append(";");
        return a;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
        StringBuilder w = new StringBuilder();
        w.append("Console.IO.console.println(").append(writeStatement.getSource().getText()).append(");");
        return w;
    }

    @Override
    public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
        StringBuilder r = new StringBuilder();
        r.append(readStatement.getName()).append(" = ").append(readStatement.getSource().getText()).append(";");
        return r;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception {
        StringBuilder p = new StringBuilder();
        p.append("package ").append(packageName).append(";\n");
        //Add imports
        p.append("public class ").append(program.getName()).append(" {\n").append("public static ").append(program.getReturnType().toString().toLowerCase()).append(" apply(");
        List<NameDef> params = program.getParams();
        for(NameDef nd : params)
            nd.visit(this, arg);
        p.append("){\n");
        List<ASTNode> das = program.getDecsAndStatements();
        for (ASTNode d : das)
            p.append(d.visit(this, arg));
        p.append("\n}\n}");
        return p.toString();
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
        StringBuilder nd = new StringBuilder();
        nd.append(nameDef.getType()).append(" ").append(nameDef.getName());
        return nd;
    }

    @Override
    public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
        return null;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
        StringBuilder r = new StringBuilder();
        r.append("return ").append(returnStatement.getExpr().getText()).append(";");
        return r;
    }

    @Override
    public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
        StringBuilder v = (StringBuilder) arg;
        v.append(declaration.getName());
        if (declaration.getExpr() != null)
        {
            v.append(" = ").append(declaration.getExpr().getText());
        }
        v.append(";");
        return v;
    }

    @Override
    public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
        return null;
    }
}
