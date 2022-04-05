package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.Declaration;

import java.util.HashMap;

public class SymbolTable
{
    HashMap<String, Declaration> table = new HashMap<>();
    public boolean insert(String name, Declaration dec)
    {
        return (table.putIfAbsent(name, dec) == null);
    }
    public Declaration lookup(String name) throws Exception
    {
        if (table.get(name) == null)
            return null;
        else
            return table.get(name);
    }
    public void remove(String name)
    {
        table.remove(name);
    }

}