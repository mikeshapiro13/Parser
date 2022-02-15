package edu.ufl.cise.plc;

import edu.ufl.cise.plc.IToken.Kind;

public class Token implements IToken {
    final Kind kind;
    final String input;
    final String rawInput;
    final int pos;
    final int length;
    final SourceLocation src;

    Token(Kind kind, String input, String rawInput, int pos, int length, SourceLocation src)
    {
        this.kind = kind;
        this.input = input;
        this.rawInput = rawInput;
        this.pos = pos;
        this.length = length;
        this.src = src;
    }

    @Override
    public Kind getKind()
    {
        return kind;
    }

    @Override
    public String getText()
    {
        return rawInput;
    }

    @Override
    public int getIntValue()
    {
        return Integer.parseInt(input);
    }

    @Override
    public float getFloatValue() {
        return Float.parseFloat(input);
    }

    @Override
    public boolean getBooleanValue() {
        return Boolean.parseBoolean(input);
    }

    @Override
    public String getStringValue() {
        return input;
    }

    @Override public SourceLocation getSourceLocation() {
        return src;
    }

}
