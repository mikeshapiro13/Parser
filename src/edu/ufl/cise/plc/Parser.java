package edu.ufl.cise.plc;
import edu.ufl.cise.plc.Lexer;
import edu.ufl.cise.plc.ast.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import static edu.ufl.cise.plc.IToken.Kind.*;

public class Parser implements IParser
{

    ILexer lexer;
    IToken current;

    Parser(String input)
    {
        lexer = new Lexer(input);
    }


    @Override
    public ASTNode parse() throws LexicalException, SyntaxException
    {
        current = lexer.next();
        return Program();
    }

    public Expr PrimaryExpr() throws LexicalException, SyntaxException {
        IToken first = current;
        Expr e = null;

        if (first.getText().equals("console"))
        {
            current = lexer.next();
            return new ConsoleExpr(first);
        }

        switch(first.getKind())
        {
            case BOOLEAN_LIT -> {
                e = new BooleanLitExpr(first);
            }
            case STRING_LIT -> {
                e = new StringLitExpr(first);
            }
            case INT_LIT -> {
                e = new IntLitExpr(first);
            }
            case FLOAT_LIT -> {
                e = new FloatLitExpr(first);
            }
            case IDENT -> {
                e = new IdentExpr(first);
            }
            case LPAREN -> {
                e = Expr();
                if (current.getKind() == RPAREN) {
                    current = lexer.next();
                    return e;
                }
                else
                    throw new SyntaxException("Syntax Error");
            }
            case COLOR_CONST -> {
                e =  new ColorConstExpr(first);
            }
            case LANGLE -> {
                e = Expr();
                if (current.getKind() == COMMA)
                {
                    Expr f = Expr();
                    if (current.getKind() == COMMA)
                    {
                        Expr g = Expr();
                        if (current.getKind() == RANGLE)
                        {
                            current = lexer.next();
                            return new ColorExpr(first, e, f, g);
                        }
                        else
                            throw new SyntaxException("Error");
                    }
                    else
                        throw new SyntaxException("Error");

                }
                else
                    throw new SyntaxException("Error");
            }
            default -> {
                throw new SyntaxException("Syntax Error");
            }
        }
        current = lexer.next();
        return e;
    }
    public PixelSelector PixelSelector() throws LexicalException, SyntaxException
    {
        IToken first = current;
        Expr x = null;
        Expr y = null;
        if (first.getKind() == LSQUARE)
        {
            x = Expr();
            if (current.getKind() == COMMA)
            {
                y = Expr();
                if (current.getKind() == RSQUARE)
                {
                    current = lexer.next();
                }
                else
                {
                    throw new SyntaxException("Error");
                }
            }
            else
            {
                throw new SyntaxException("Error");
            }
        }
        else
        {
            System.out.println("error");
        }

        return new PixelSelector(first, x, y);
    }
    public Expr UnaryExprPostfix() throws LexicalException, SyntaxException {
        IToken first = current;
        Expr l = PrimaryExpr();
        PixelSelector selector = null;

        if (current.getKind() == LSQUARE) {
            selector = PixelSelector();
        }
        else{
            return l;
        }
        return new UnaryExprPostfix(first, l, selector);
    }
    public Expr UnaryExpr() throws LexicalException, SyntaxException {
        IToken first = current;
        IToken op = null;
        Expr e = null;
        if (first.getKind() == BANG || first.getKind() == MINUS || first.getKind() == COLOR_OP || first.getKind() == IMAGE_OP){
            op = first;
            current = lexer.next();
            e = UnaryExpr();
        }
        else {
            e = UnaryExprPostfix();
            return e;
        }
        return new UnaryExpr(first, op, e);

    }
    public Expr MultiplicativeExpr() throws LexicalException, SyntaxException {
        IToken first = current;
        IToken op = null;
        Expr l = UnaryExpr();
        Expr r = null;
        while (current.getKind() == DIV || current.getKind() == MOD || current.getKind() == TIMES){
            op = current;
            current = lexer.next();
            r = UnaryExpr();
            l = new BinaryExpr(first, l, op, r);
        }
        return l;
    }
    public Expr AdditiveExpr() throws LexicalException, SyntaxException {
        IToken first = current;
        IToken op = null;
        Expr l = MultiplicativeExpr();
        Expr r = null;

        while (current.getKind() == PLUS ||current.getKind() == MINUS){
            op = current;
            current = lexer.next();
            r = MultiplicativeExpr();
            l = new BinaryExpr(first, l, op, r);
        }
        return l;
    }
    public Expr ComparisonExpr() throws LexicalException, SyntaxException {
        IToken first = current;
        IToken op = null;
        Expr l = AdditiveExpr();
        Expr r = null;
        while (current.getKind() == LT || current.getKind() == GT || current.getKind() == EQUALS || current.getKind() == NOT_EQUALS || current.getKind() == LE || current.getKind() == GE){
            op = current;
            current = lexer.next();
            r = AdditiveExpr();
            l = new BinaryExpr(first, l, op, r);
        }
        return l;
    }
    public Expr LogicalAndExpr() throws LexicalException, SyntaxException {
        IToken first = current;
        IToken op = null;
        Expr l = ComparisonExpr();
        Expr r = null;
        while (current.getKind() == AND) {
            op = current;
            current = lexer.next();
            r = ComparisonExpr();
            l = new BinaryExpr(first, l, op, r);
        }
        return l;
    }
    public Expr LogicalOrExpr() throws LexicalException, SyntaxException {
        IToken first = current;
        IToken op = null;
        Expr l = LogicalAndExpr();
        Expr r = null;
        while(current.getKind() == OR){
            op = current;
            current = lexer.next();
            r = LogicalAndExpr();
            l = new BinaryExpr(first, l, op, r);
        }
        return l;
    }
    public Expr ConditionalExpr() throws LexicalException, SyntaxException
    {
        IToken first = current;
        Expr c = null;
        Expr t = null;
        Expr f = null;
        if (first.getKind() == KW_IF)
        {
            current = lexer.next();
            if (current.getKind() == LPAREN)
            {
                c = Expr();
                if (current.getKind() == RPAREN)
                {
                    t = Expr();
                    if (current.getKind() == KW_ELSE)
                    {
                        f = Expr();
                        if (current.getKind() == KW_FI)
                        {
                            current = lexer.next();
                        }
                        else
                            throw new SyntaxException("Error");
                    }
                    else
                        throw new SyntaxException("Error");

                }
                else
                    throw new SyntaxException("Error");
            }
            else
                throw new SyntaxException("Error");
        }
        else
            throw new SyntaxException("Error");

        return new ConditionalExpr(first, c, t, f);
    }
    public Expr Expr() throws LexicalException, SyntaxException {
        current = lexer.next();
        IToken first = current;
        Expr e = null;
        if (first.getKind() == KW_IF)
            e = ConditionalExpr();
        else
            e = LogicalOrExpr();
        return e;
    }

    public Statement Statement() throws SyntaxException, LexicalException {
        IToken first = current;
        Expr e = null;
        PixelSelector p = null;
        String name = first.getText();
        if (first.getKind() == IDENT)
        {
            current = lexer.next();
            if (current.getKind() == LSQUARE)
            {
                p = PixelSelector();
            }

            if (current.getKind() == ASSIGN)
            {
                e = Expr();
                return new AssignmentStatement(first, name, p, e);
            }
            else if (current.getKind() == LARROW)
            {
                e = Expr();
                return new ReadStatement(first, name, p, e);
            }
            else
                throw new SyntaxException("Error in Statement");

        }
        else if (first.getText().equals("write"))
        {
            e = Expr();
            if (current.getKind() == RARROW)
            {
                Expr f = Expr();
                return new WriteStatement(first, e, f);
            }
            else
                throw new SyntaxException("No RARROW");
        }
        else if (first.getKind() == RETURN)
        {
            e = Expr();
            return new ReturnStatement(first, e);
        }
        else
            throw new SyntaxException("Error in Statement");
    }

    public Dimension Dimension() throws LexicalException, SyntaxException
    {
        IToken first = current;
        Expr width = null;
        Expr height = null;
        if (first.getKind() == LSQUARE)
        {
            width = Expr();
            if (current.getKind() == COMMA)
            {
                height = Expr();
                if (current.getKind() == RSQUARE)
                {
                    current = lexer.next();
                }
                else
                {
                    throw new SyntaxException("Error");
                }
            }
            else
            {
                throw new SyntaxException("Error");
            }
        }
        else
        {
            System.out.println("error");
        }

        return new Dimension(first, width, height);
    }

    public Declaration Declaration()  throws LexicalException, SyntaxException
    {
        IToken first = current;
        IToken op = null;
        Expr e = null;
        NameDef name = null;

        name = NameDef();
        if (current.getKind() == ASSIGN || current.getKind() == LARROW)
        {
            op = current;
            e = Expr();
        }
        return new VarDeclaration(first, name, op, e);
    }

    public NameDef NameDef() throws SyntaxException, LexicalException
    {
        IToken first = current;
        String type = first.getText();
        if (type.equals("void"))
            throw new SyntaxException("Oi");
        String name = null;
        Dimension d = null;
        current = lexer.next();

        try
        {
            Types.Type test = Types.toType(type);
        }
        catch (IllegalArgumentException r)
        {
            throw new SyntaxException("LMAO");
        }

        if (current.getKind() == IDENT)
        {
            name = current.getText();
            current = lexer.next();
            return new NameDef(first, type, name);
        }
        else if (current.getKind() == LSQUARE)
        {
            d = Dimension();
            if (current.getKind() == IDENT)
            {
                name = current.getText();
            }
        }
        else
            throw new SyntaxException("Error");
        current = lexer.next();
        return new NameDefWithDim(first, type, name, d);
    }

    public Program Program() throws SyntaxException, LexicalException
    {
        IToken first = current;
        Types.Type returnType;
        try
        {
            returnType = Types.Type.toType(first.getText());
        }
        catch(IllegalArgumentException e)
        {
           throw new SyntaxException("Oi");
        }

        String name = null;
        List<NameDef> params = new ArrayList<NameDef>();
        List<ASTNode> decsAndStatements = new ArrayList<ASTNode>();
        current = lexer.next();
        if (current.getKind() == IDENT)
        {
            name = current.getText();
            current = lexer.next();
            if (current.getKind() == LPAREN)
            {
                current = lexer.next();
                if (current.getKind() != RPAREN)
                {
                    params.add(NameDef());
                    while (current.getKind() == COMMA)
                    {
                        current = lexer.next();
                        if (current.getKind() == RPAREN)
                            throw new SyntaxException("Oi");
                        params.add(NameDef());
                    }
                    if (current.getKind() == RPAREN)
                    {
                        current = lexer.next();
                        if (current.getKind() == EOF)
                        {
                            return new Program(first, returnType, name, params, decsAndStatements);
                        }
                        else
                        {
                            //current = lexer.next();
                            while (current.getKind() != EOF)
                            {
                                if (current.getKind() == IDENT || current.getKind() == RETURN || current.getText().equals("write"))
                                {
                                    decsAndStatements.add(Statement());
                                }
                                else
                                {
                                    decsAndStatements.add(Declaration());
                                }
                                if (current.getKind() == SEMI)
                                    current = lexer.next();
                                else
                                    throw new SyntaxException("Oi");
                            }
                        }
                    }
                }
                else
                {
                    current = lexer.next();
                    while (current.getKind() != EOF)
                    {
                        if (current.getKind() == IDENT || current.getKind() == RETURN || current.getText().equals("write"))
                        {
                            decsAndStatements.add(Statement());
                        }
                        else if (current.getText().equals("boolean") || current.getText().equals("color") || current.getText().equals("float") || current.getText().equals("image") || current.getText().equals("int") || current.getText().equals("string"))
                        {
                            decsAndStatements.add(Declaration());
                        }
                        else
                            throw new SyntaxException("Oi");
                        if (current.getKind() == SEMI)
                            current = lexer.next();
                        else
                            throw new SyntaxException("Oi");
                    }
                }
            }
        }
        else
            throw new SyntaxException("Invalid Program");
        return new Program(first, returnType, name, params, decsAndStatements);
    }
}