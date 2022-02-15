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
        return Expr();
    }

    public Expr PrimaryExpr() throws LexicalException, SyntaxException {
        IToken first = current;
        Expr e = null;

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
                if (lexer.next().getKind() == RPAREN)
                    return e;
                else
                    throw new SyntaxException("Syntax Error");
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
}