package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.ast.Dimension;
import edu.ufl.cise.plc.runtime.ColorTuple;
import edu.ufl.cise.plc.runtime.ImageOps;
import java.awt.Color;

import java.awt.*;
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
        string.append("\"\"\"\n").append(stringLitExpr.getValue()).append("\"\"\"");
        return string;
    }

    @Override
    public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
        StringBuilder intLit = new StringBuilder();
        if (intLitExpr.getCoerceTo() != null && intLitExpr.getCoerceTo() != Types.Type.INT)
        {
            intLit.append("(").append(intLitExpr.getCoerceTo().toString().toLowerCase()).append(")");
        }
        intLit.append(intLitExpr.getText());
        return intLit;
    }

    @Override
    public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
        StringBuilder f = new StringBuilder();
        if (floatLitExpr.getCoerceTo() != null && floatLitExpr.getCoerceTo() != Types.Type.FLOAT)
        {
            f.append("(").append(floatLitExpr.getType().name().toLowerCase()).append(")");
        }
        f.append(floatLitExpr.getText()).append("f");
        return f;
    }

    @Override
    public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
        StringBuilder c = new StringBuilder();
        c.append("ColorTuple.unpack(Color.");
        c.append(colorConstExpr.getText());
        c.append(".getRGB())");
        return c;
    }

    @Override
    public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
        StringBuilder c = new StringBuilder();
        c.append("(");
        switch(consoleExpr.getCoerceTo().toString()) {
            case "INT" -> {
                c.append("Integer").append(") ");
            }
            case "STRING" -> {
                c.append("String").append(") ");
            }
            case "BOOLEAN" -> {
                c.append("Boolean").append(") ");
            }
            case "FLOAT" -> {
                c.append("Float").append(") ");
            }
            case "IMAGE" -> {
                c.append("BufferedImage").append(") ");
            }
            case "COLOR" -> {
                c.append("ColorTuple").append(") ");
            }
        }
        c.append("ConsoleIO.readValueFromConsole(\"").append(consoleExpr.getCoerceTo().toString()).append("\", ").append("\"Enter ");
        switch (consoleExpr.getCoerceTo().toString())
        {
            case "INT" -> {
                c.append("integer:").append("\")");
            }
            case "STRING" -> {
                c.append("string:").append("\")");
            }
            case "BOOLEAN" -> {
                c.append("boolean:").append("\")");
            }
            case "FLOAT" -> {
                c.append("float:").append("\")");
            }
            case "COLOR" -> {
                c.append("RGB values:").append("\")");
            }
        }
        c.append(";\n");
        return c;
    }

    @Override
    public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
        StringBuilder c = new StringBuilder();
        c.append("new ColorTuple(");
        c.append(colorExpr.getRed().visit(this, arg)).append(", ");
        c.append(colorExpr.getGreen().visit(this, arg)).append(", ");
        c.append(colorExpr.getBlue().visit(this, arg)).append(")");
        return c;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpression, Object arg) throws Exception {
        StringBuilder u = new StringBuilder();
        if (unaryExpression.getType() == Types.Type.INT || unaryExpression.getType() == Types.Type.COLOR)
        {
            u.append("ColorTuple.").append(unaryExpression.getOp().getText()).append("(");
            u.append(unaryExpression.getExpr().visit(this, arg)).append(")");

        }
        else if (unaryExpression.getType() == Types.Type.IMAGE)
        {
            u.append("ImageOps.extract");
            switch(unaryExpression.getOp().getText())
            {
                case "getRed" -> {
                    u.append("Red(");
                }
                case "getBlue" -> {
                    u.append("Blue(");
                }
                case "getGreen" -> {
                    u.append("Green(");
                }
            }
            u.append(unaryExpression.getExpr().visit(this,arg)).append(")");
        }
        else
        {
            if (unaryExpression.getCoerceTo() != null)
                u.append("(").append(unaryExpression.getCoerceTo().toString().toLowerCase()).append(")");
            u.append("(").append(unaryExpression.getOp().getText());
            u.append(" ").append(unaryExpression.getExpr().visit(this, arg)).append(")");
        }
        return u;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
        StringBuilder b = new StringBuilder();
        String Op = "";
        switch(binaryExpr.getOp().getKind())
        {
            case PLUS -> Op = "ImageOps.OP.PLUS";
            case MINUS -> Op = "ImageOps.OP.MINUS";
            case TIMES -> Op = "ImageOps.OP.TIMES";
            case MOD -> Op = "ImageOps.OP.MOD";
            case DIV -> Op = "ImageOps.OP.DIV";
            case EQUALS -> Op = "ImageOps.BoolOP.EQUALS";
            case NOT_EQUALS -> Op= "ImageOps.BoolOP.NOT_EQUALS";
        }
        if (binaryExpr.getLeft().getType() == Types.Type.COLOR && binaryExpr.getRight().getType() == Types.Type.COLOR)
        {
            b.append("ImageOps.binaryTupleOp(").append(Op).append(", ").append(binaryExpr.getLeft().visit(this, arg)).append(", ").append(binaryExpr.getRight().visit(this, arg)).append(")");
        }
        else if (binaryExpr.getLeft().getType() == Types.Type.IMAGE && binaryExpr.getRight().getType() == Types.Type.IMAGE)
        {
            b.append("ImageOps.binaryImageImageOp(").append(Op).append(binaryExpr.getOp().getText());
            b.append(", ").append(binaryExpr.getLeft().visit(this, arg)).append(", ").append(binaryExpr.getRight().visit(this, arg));
            b.append(")");
        }
        else if ((binaryExpr.getLeft().getType() == Types.Type.IMAGE && binaryExpr.getRight().getType() == Types.Type.COLOR) || (binaryExpr.getLeft().getType() == Types.Type.COLOR && binaryExpr.getRight().getType() == Types.Type.IMAGE))
        {
            b.append("(ImageOps.binaryImageScalarOp(").append(Op).append(binaryExpr.getText());
            b.append(", ").append(binaryExpr.getLeft().visit(this, arg)).append(", ");
            b.append("ColorTuple.makePackedColor()(ColorTuple.getRed(").append(binaryExpr.getRight().visit(this, arg));
            b.append("),ColorTuple.getGreen(").append(binaryExpr.getRight().visit(this, arg));
            b.append("),ColorTuple.getBlue(").append(binaryExpr.getRight().visit(this, arg)).append(")))");
        }
        else if ((binaryExpr.getLeft().getType() == Types.Type.IMAGE && binaryExpr.getRight().getType() == Types.Type.INT) || (binaryExpr.getLeft().getType() == Types.Type.INT && binaryExpr.getRight().getType() == Types.Type.IMAGE))
        {

            b.append("(ImageOps.binaryImageScalarOp(").append(Op);
            b.append(", ").append(binaryExpr.getLeft().visit(this, arg)).append(", ").append(binaryExpr.getRight().visit(this, arg));
            b.append("))");
        }
        else
        {
            if (binaryExpr.getCoerceTo() != null) {
                String test = binaryExpr.getCoerceTo().toString().toLowerCase();
                switch (test) {
                    case "image" -> {
                        b.append("(BufferedImage)");
                    }
                    case "color" -> {
                        b.append("(ColorTuple)");
                    }
                    case "float" -> {
                        b.append("(float)");
                    }
                    case "boolean" -> {
                        b.append("(boolean)");
                    }
                    case "int" -> {
                        b.append("(int)");
                    }
                }
            }
            String check = binaryExpr.getOp().getText();
            if (check.equals("==") && binaryExpr.getLeft().getType() == Types.Type.STRING) {
                b.append("(").append(binaryExpr.getLeft().visit(this, arg));
                b.append(".equals(").append(binaryExpr.getRight().visit(this, arg)).append("))");
            } else if (check.equals("!=") && binaryExpr.getLeft().getType() == Types.Type.STRING) {
                b.append("!(").append(binaryExpr.getLeft().visit(this, arg));
                b.append(".equals(").append(binaryExpr.getRight().visit(this, arg)).append("))");
            } else {
                b.append("(").append(binaryExpr.getLeft().visit(this, arg));
                b.append(" ").append(binaryExpr.getOp().getText());
                b.append(" ").append(binaryExpr.getRight().visit(this, arg)).append(")");
            }
        }
        return b;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
        StringBuilder id = new StringBuilder();
        if (identExpr.getCoerceTo() != null && identExpr.getCoerceTo() != identExpr.getType())
        {
            id.append("(").append(identExpr.getCoerceTo().toString().toLowerCase()).append(")");
        }
        id.append(identExpr.getText());
        return id;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
        StringBuilder c = new StringBuilder();
//        if (conditionalExpr.getCoerceTo() != null)
//        {
//            String check = conditionalExpr.getCoerceTo().toString().toLowerCase();
//            if (check.equals("color"))
//                check = "Color";
//            c.append("(").append(check).append(")");
//        }
        //c.append("(");
        c.append("(").append(conditionalExpr.getCondition().visit(this, arg)).append(")");
        c.append(" ? (").append(conditionalExpr.getTrueCase().visit(this, arg));
        c.append(") : (").append(conditionalExpr.getFalseCase().visit(this, arg));
        c.append(")");
        return c;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws Exception {
        StringBuilder d = new StringBuilder();
        d.append(dimension.getWidth().visit(this, arg));
        d.append(", ");
        d.append(dimension.getHeight().visit(this, arg));
        return d;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
        StringBuilder p = new StringBuilder();
//        //if(){ // left
//            p.append("for(int i = 0; i < ");
//            p.append(p.append(pixelSelector.getX().visit(this, arg)));
//            p.append(p.append("; i++) {\n\t"));
//            p.append("for(int j; j < ");
//            p.append(pixelSelector.getY().visit(this, arg));
//            p.append(p.append("; j++) {\n\t"));
//            //inside of for loops
//            p.append("\n\t}\n\n}");
//       // } else if (){ //right
            p.append(pixelSelector.getX().visit(this, arg));
            p.append(",");
            p.append(pixelSelector.getY().visit(this, arg));
        //}

        return p;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
        StringBuilder a = new StringBuilder();
        if(assignmentStatement.getTargetDec().getType() == Types.Type.IMAGE && assignmentStatement.getExpr().getType() == Types.Type.IMAGE)
        {
            if(assignmentStatement.getTargetDec().getDim() != null)
            {
                a.append(assignmentStatement.getName()).append(" = ImageOps.resize(");
                a.append(assignmentStatement.getExpr().visit(this, arg));
                a.append(", ");
                a.append(assignmentStatement.getTargetDec().getDim().getWidth().visit(this, arg));
                a.append(", ");
                a.append(assignmentStatement.getTargetDec().getDim().getHeight().visit(this, arg));
                a.append(");");

            }
            else
            {
                a.append(assignmentStatement.getName()).append(" = ImageOps.resize(");
                a.append(assignmentStatement.getTargetDec().visit(this, arg)).append(", ");
                //a.append(assignmentStatement.);
                if(assignmentStatement.getExpr() instanceof IdentExpr)
                {
                    a.append(assignmentStatement.getName());
                    a.append("= ImageOps.clone(");
                    a.append(assignmentStatement.getExpr().visit(this, arg)).append(");");
                }
            }
        }
        else if(assignmentStatement.getTargetDec().getType() == Types.Type.IMAGE && assignmentStatement.getTargetDec().getDim() != null)
        {
            a.append("for(int ").append(assignmentStatement.getSelector().getX().getText());
            a.append(" = 0; ").append(assignmentStatement.getSelector().getX().getText());
            a.append(" < ").append(assignmentStatement.getTargetDec().getDim().getWidth().getText());
            a.append("; ").append(assignmentStatement.getSelector().getX().getText()).append("++){for(int ");
            a.append(assignmentStatement.getSelector().getY().getText()).append(" = 0; ");
            a.append(assignmentStatement.getSelector().getY().getText()).append(" < ");
            a.append(assignmentStatement.getTargetDec().getDim().getHeight().getText()).append("; ");
            a.append(assignmentStatement.getSelector().getY().getText()).append("++){ImageOps.setColor(").append(assignmentStatement.getName()).append(", ");
            a.append(assignmentStatement.getSelector().getX().getText()).append(", ");
            a.append(assignmentStatement.getSelector().getY().getText()).append(", ");
            if(assignmentStatement.getExpr().getCoerceTo() == Types.Type.COLOR)
            {
                a.append(assignmentStatement.getExpr().visit(this, arg)).append(");}}");

            }
            else if(assignmentStatement.getExpr().getCoerceTo() == Types.Type.INT)
            {
                a.append("ColorTuple.unpack(ColorTuple.truncate(").append(assignmentStatement.getExpr().visit(this, arg));
                a.append(")));}}");
            }
        }
        else
        {
            a.append(assignmentStatement.getName()).append(" = ");
            a.append(assignmentStatement.getExpr().visit(this, arg)).append(";");
        }
        a.append("\n");
        return a;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
        StringBuilder w = new StringBuilder();
        if (writeStatement.getSource().getType() == Types.Type.IMAGE)
        {
            if (writeStatement.getDest().getType() == Types.Type.CONSOLE)
                w.append("ConsoleIO.displayImageOnScreen(").append(writeStatement.getSource().getText()).append(");\n");
            else if (writeStatement.getDest().getType() == Types.Type.STRING)
                w.append("FileURLIO.writeImage(").append(writeStatement.getSource().getText()).append(", ").append(writeStatement.getDest().getText()).append(");\n");
        }
        else if (writeStatement.getDest().getType() == Types.Type.STRING)
            w.append("FileURLIO.writeValue(").append(writeStatement.getSource().getText()).append(", ").append(writeStatement.getDest().getText()).append(");\n");
        else
            w.append("ConsoleIO.console.println(").append(writeStatement.getSource().visit(this ,arg)).append(");\n");
        return w;
    }

    @Override
    public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
        StringBuilder r = new StringBuilder();
        String check = "";
        switch (readStatement.getTargetDec().getType())
        {
            case COLOR -> check = "ColorTuple";
            case STRING -> check = "String";
            case COLORFLOAT -> check = "ColorTuple";
            default -> check = readStatement.getTargetDec().getType().toString().toLowerCase();
        }
        if (readStatement.getTargetDec().getType() == Types.Type.IMAGE)
        {
            r.append(readStatement.getTargetDec().visit(this, arg)).append(" = FileURLIO.readImage(");
            r.append(readStatement.getSource().visit(this, arg));
            if (readStatement.getTargetDec().getDim() != null)
                r.append(", ").append(readStatement.getTargetDec().getDim().visit(this, arg));
            r.append(");\nFileURLIO.closeFiles();");
        }
        else if (readStatement.getSource() instanceof ConsoleExpr)
            r.append(readStatement.getName()).append(" = ").append(readStatement.getSource().visit(this, arg));
        else
            r.append(readStatement.getName()).append(" = (").append(check).append(") FileURLIO.readValueFromFile(").append(readStatement.getSource().visit(this, arg)).append(");\n");
        return r;
    }

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception {
        StringBuilder p = new StringBuilder();
        p.append("package ").append(packageName).append(";\n");
        String test = program.getDecsAndStatements().toString();
        //if (program.getDecsAndStatements().toString().lastIndexOf("ConsoleExpr") > 0)
            p.append("import edu.ufl.cise.plc.runtime.ConsoleIO;\n");
        String check = program.getReturnType().toString().toLowerCase();
        //if (program.getReturnType() == Types.Type.IMAGE)
            p.append("import java.awt.image.BufferedImage;\n");
        //else if (program.getReturnType() == Types.Type.COLOR)
            p.append("import edu.ufl.cise.plc.runtime.ColorTuple;\n");
        //if (program.getDecsAndStatements().toString().contains("VarDeclaration") && program.getDecsAndStatements().toString().contains("LARROW"))
            p.append("import edu.ufl.cise.plc.runtime.FileURLIO;\n");
            p.append("import edu.ufl.cise.plc.runtime.ImageOps;\n");
            p.append("import java.awt.Color;");
        if (check.equals("string"))
            check = "String";
        else if (check.equals("image"))
            check = "BufferedImage";
        else if (check.equals("color"))
            check = "ColorTuple";
        p.append("public class ").append(program.getName()).append(" {\n").append("public static ").append(check).append(" apply(");
        List<NameDef> params = program.getParams();
        int size = 0;
        for(NameDef nd : params)
        {
            ++size;
            if (size < params.size())
                p.append(nd.visit(this, arg)).append(", ");
            else
                p.append(nd.visit(this,arg));
        }
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
        String check = nameDef.getType().name().toLowerCase();
        if (check.equals("string"))
            check = "String";
        nd.append(check).append(" ").append(nameDef.getName());
        return nd;
    }

    @Override
    public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
        StringBuilder nd = new StringBuilder();
        nd.append("BufferedImage ");
        nd.append(nameDefWithDim.getName()).append(" = newBufferedImage(");
        nd.append(nameDefWithDim.getDim().visit(this, arg)).append(", BufferedImage.TYPE_INT_RGB);\n");
        return nd;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
        StringBuilder r = new StringBuilder();
        r.append("return ").append(returnStatement.getExpr().visit(this, arg)).append(";");
        return r;
    }

    @Override
    public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
        StringBuilder v = new StringBuilder();
        if (declaration.getType() == Types.Type.IMAGE)
        {
            v.append("BufferedImage ").append(declaration.getName());
            if (declaration.getExpr() != null && declaration.getOp().getKind() == IToken.Kind.LARROW)
            {
                if (declaration.getDim() != null)
                {
                    v.append(" = ").append("FileURLIO.readImage(").append(declaration.getExpr().visit(this, arg)).append(", ");
                    v.append(declaration.getDim().visit(this, arg)).append(")");
                }
                else
                {
                    v.append(" = ").append("FileURLIO.readImage(").append(declaration.getExpr().visit(this, arg)).append(")");
                }
            }
            else if (declaration.getExpr() != null && declaration.getOp().getKind() == IToken.Kind.ASSIGN)
            {
                v.append(" = ").append(declaration.getExpr().visit(this, arg));
            }
            else
            {
                v.append(" = ").append("new BufferedImage(").append(declaration.getDim().visit(this, arg)).append(", BufferedImage.TYPE_INT_RGB)");
            }
        }
        else if (declaration.getType() == Types.Type.COLOR)
        {
            v.append("ColorTuple ").append(declaration.getName());
            if (declaration.getOp() != null)
            {
                if (declaration.getOp().getKind() == IToken.Kind.LARROW)
                    v.append(" = ").append("(ColorTuple)").append("FileURLIO.readValueFromFile(").append(declaration.getExpr().visit(this, arg)).append(")");
                else
                    v.append(" = ").append(declaration.getExpr().visit(this, arg));
            }
        }
        else
        {
            String check = declaration.getType().toString().toLowerCase();
            if (check.equals("string"))
                check = "String";
            v.append(check).append(" ").append(declaration.getName());
            if (declaration.getOp() != null)
                if (declaration.getOp().getKind() != IToken.Kind.LARROW)
                    v.append(" = ").append(declaration.getExpr().visit(this, arg));
            else
            {
                if (declaration.getExpr() instanceof ConsoleExpr)
                    v.append(" = ").append(declaration.getExpr().visit(this, arg));
                else
                {
                    v.append(" = (").append(check).append(")");
                    v.append("FileURLIO.readValueFromFile(").append(declaration.getExpr().visit(this, arg)).append(")");
                }
            }
        }
        v.append(";\n");
        return v;
    }

    @Override
    public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
        StringBuilder u = new StringBuilder();
        u.append("ColorTuple.unpack(");
        u.append(unaryExprPostfix.getText()).append(".getRGB(");
        u.append(unaryExprPostfix.getSelector().visit(this, arg)).append("))");
        return u;
    }
}
