package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.ASTNode;
import edu.ufl.cise.plc.ast.BooleanLitExpr;
import edu.ufl.cise.plc.ast.StringLitExpr;

import java.util.ArrayList;

public class Parser implements IParser{

    Lexer lexer;
    ArrayList<Token> tokens;

    Parser(String input)
    {
        lexer = new Lexer(input);
        tokens = lexer.tokens;
    }

    @Override
    public ASTNode parse()
    {
        ASTNode test = null;
        for (int i = 0; i < tokens.size(); ++i)
        {
            switch(tokens.get(i).getKind())
            {
                case BOOLEAN_LIT -> {
                    test = new BooleanLitExpr(tokens.get(i));
                }
                case STRING_LIT -> {
                    test = new StringLitExpr(tokens.get(i));
                }
            }
        }
        return test;
    }
}
