package edu.ufl.cise.plc;

import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.ast.*;
import edu.ufl.cise.plc.ast.Types.Type;

import static edu.ufl.cise.plc.ast.Types.Type.*;

public class TypeCheckVisitor implements ASTVisitor {

	SymbolTable symbolTable = new SymbolTable();  
	Program root;
	
	record Pair<T0,T1>(T0 t0, T1 t1){};  //may be useful for constructing lookup tables.
	
	private void check(boolean condition, ASTNode node, String message) throws TypeCheckException {
		if (!condition) {
			throw new TypeCheckException(message, node.getSourceLoc());
		}
	}
	
	//The type of a BooleanLitExpr is always BOOLEAN.  
	//Set the type in AST Node for later passes (code generation)
	//Return the type for convenience in this visitor.  
	@Override
	public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
		booleanLitExpr.setType(Type.BOOLEAN);
		return Type.BOOLEAN;
	}

	@Override
	public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
		stringLitExpr.setType(Type.STRING);
		return STRING;
	}

	@Override
	public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
		intLitExpr.setType(Type.INT);
		return INT;
	}

	@Override
	public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
		floatLitExpr.setType(Type.FLOAT);
		return Type.FLOAT;
	}

	@Override
	public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
		colorConstExpr.setType(Type.COLOR);
		return COLOR;
	}

	@Override
	public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
		consoleExpr.setType(Type.CONSOLE);
		return Type.CONSOLE;
	}
	
	//Visits the child expressions to get their type (and ensure they are correctly typed)
	//then checks the given conditions.
	@Override
	public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
		Type redType = (Type) colorExpr.getRed().visit(this, arg);
		Type greenType = (Type) colorExpr.getGreen().visit(this, arg);
		Type blueType = (Type) colorExpr.getBlue().visit(this, arg);
		check(redType == greenType && redType == blueType, colorExpr, "color components must have same type");
		check(redType == Type.INT || redType == Type.FLOAT, colorExpr, "color component type must be int or float");
		Type exprType = (redType == Type.INT) ? Type.COLOR : Type.COLORFLOAT;
		colorExpr.setType(exprType);
		return exprType;
	}	

	
	
	//Maps forms a lookup table that maps an operator expression pair into result type.  
	//This more convenient than a long chain of if-else statements. 
	//Given combinations are legal; if the operator expression pair is not in the map, it is an error. 
	Map<Pair<Kind,Type>, Type> unaryExprs = Map.of(
			new Pair<Kind,Type>(Kind.BANG,BOOLEAN), BOOLEAN,
			new Pair<Kind,Type>(Kind.MINUS, FLOAT), FLOAT,
			new Pair<Kind,Type>(Kind.MINUS, INT),INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,INT), INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,COLOR), INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,IMAGE), IMAGE,
			new Pair<Kind,Type>(Kind.IMAGE_OP,IMAGE), INT
			);
	
	//Visits the child expression to get the type, then uses the above table to determine the result type
	//and check that this node represents a legal combination of operator and expression type. 
	@Override
	public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws Exception {
		// !, -, getRed, getGreen, getBlue
		Kind op = unaryExpr.getOp().getKind();
		Type exprType = (Type) unaryExpr.getExpr().visit(this, arg);
		//Use the lookup table above to both check for a legal combination of operator and expression, and to get result type.
		Type resultType = unaryExprs.get(new Pair<Kind,Type>(op,exprType));
		check(resultType != null, unaryExpr, "incompatible types for unaryExpr");
		//Save the type of the unary expression in the AST node for use in code generation later. 
		unaryExpr.setType(resultType);
		//return the type for convenience in this visitor.
		return resultType;
	}

	//This method has several cases. Work incrementally and test as you go. 
	@Override
	public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
		Kind op = binaryExpr.getOp().getKind();
		Type left = (Type) binaryExpr.getLeft().visit(this, arg);
		Type right = (Type) binaryExpr.getRight().visit(this, arg);
//		if (left == STRING || right == STRING)
//			throw new TypeCheckException("No Strings in binary");
		switch (op)
		{
			case AND, OR -> {
				check(left == BOOLEAN && right == BOOLEAN, binaryExpr, "Both sides not bool");
				binaryExpr.setType(BOOLEAN);
				return BOOLEAN;
			}
			case EQUALS, NOT_EQUALS -> {
				check(left == right, binaryExpr, "LHS != RHS");
				binaryExpr.setType(BOOLEAN);
				return BOOLEAN;
			}
			case PLUS, MINUS -> {
				if (left == right)
				{
					binaryExpr.setType(left);
					return left;
				}
				else if (left == INT && right == FLOAT)
				{
					binaryExpr.setType(FLOAT);
					binaryExpr.getLeft().setCoerceTo(FLOAT);
					return FLOAT;
				}
				else if (left == FLOAT && right == INT)
				{
					binaryExpr.setType(FLOAT);
					binaryExpr.getRight().setCoerceTo(FLOAT);
					return FLOAT;
				}
				else if (left == COLORFLOAT && right == COLOR)
				{
					binaryExpr.setType(COLORFLOAT);
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					return COLORFLOAT;
				}
				else if (left == COLOR && right == COLORFLOAT)
				{
					binaryExpr.setType(COLORFLOAT);
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					return COLORFLOAT;
				}
			}
			case TIMES, DIV, MOD -> {
				if (left == right)
				{
					binaryExpr.setType(left);
					return left;
				}
				else if (left == INT && right == FLOAT)
				{
					binaryExpr.setType(FLOAT);
					binaryExpr.getLeft().setCoerceTo(FLOAT);
					return FLOAT;
				}
				else if (left == FLOAT && right == INT)
				{
					binaryExpr.setType(FLOAT);
					binaryExpr.getRight().setCoerceTo(FLOAT);
					return FLOAT;
				}
				else if (left == COLORFLOAT && right == COLOR)
				{
					binaryExpr.setType(COLORFLOAT);
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					return COLORFLOAT;
				}
				else if (left == COLOR && right == COLORFLOAT)
				{
					binaryExpr.setType(COLORFLOAT);
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					return COLORFLOAT;
				}
				else if (left == IMAGE && right == INT)
				{
					binaryExpr.setType(IMAGE);
					return IMAGE;
				}
				else if (left == IMAGE && right == FLOAT)
				{
					binaryExpr.setType(IMAGE);
					return IMAGE;
				}
				else if (left == INT && right == COLOR)
				{
					binaryExpr.setType(COLOR);
					binaryExpr.getLeft().setCoerceTo(COLOR);
					return COLOR;
				}
				else if (left == COLOR && right == INT)
				{
					binaryExpr.setType(COLOR);
					binaryExpr.getRight().setCoerceTo(COLOR);
					return COLOR;
				}
				else if ((left == FLOAT && right == COLOR) || (left == COLOR && right == FLOAT))
				{
					binaryExpr.setType(COLORFLOAT);
					binaryExpr.getRight().setCoerceTo(COLORFLOAT);
					binaryExpr.getLeft().setCoerceTo(COLORFLOAT);
					return COLORFLOAT;
				}
			}
			case LT, LE, GT, GE -> {
				if (left == INT && right == INT)
				{
					binaryExpr.setType(BOOLEAN);
					return BOOLEAN;
				}
				else if (left == FLOAT && right == FLOAT)
				{
					binaryExpr.setType(FLOAT);
					return FLOAT;
				}
				else if (left == INT && right == FLOAT)
				{
					binaryExpr.setType(BOOLEAN);
					binaryExpr.getLeft().setCoerceTo(FLOAT);
					return BOOLEAN;
				}
				else if (left == FLOAT && right == INT)
				{
					binaryExpr.setType(BOOLEAN);
					binaryExpr.getRight().setCoerceTo(FLOAT);
					return FLOAT;
				}
			}
			default -> {
				throw new TypeCheckException("Invalid Binary");
			}
		}
		return null;
	}

	@Override
	public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
		String ident = identExpr.getText();
		Declaration d = symbolTable.lookup(ident);
		check(d != null, identExpr, ident + " is not defined");
		identExpr.setDec(d);
		identExpr.setType(d.getType());
		return d.getType();
	}

	@Override
	public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
		Type check = (Type) conditionalExpr.getCondition().visit(this, arg);
		check(check == BOOLEAN, conditionalExpr, "Condition is not boolean");
		Type t = (Type) conditionalExpr.getTrueCase().visit(this, arg);
		Type f = (Type) conditionalExpr.getFalseCase().visit(this, arg);
		check(t == f, conditionalExpr, "True and false case not same type");
		conditionalExpr.setType(t);
		return t;
	}

	@Override
	public Object visitDimension(Dimension dimension, Object arg) throws Exception {
		Type height = (Type) dimension.getHeight().visit(this, arg);
		Type width = (Type) dimension.getWidth().visit(this, arg);
		check(width == INT && height == INT, dimension, "Dimensions not both integers");
		return null;
	}

	@Override
	//This method can only be used to check PixelSelector objects on the right hand side of an assignment. 
	//Either modify to pass in context info and add code to handle both cases, or when on left side
	//of assignment, check fields from parent assignment statement.
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
		Type xType = (Type) pixelSelector.getX().visit(this, arg);
		check(xType == Type.INT, pixelSelector.getX(), "only ints as pixel selector components");
		Type yType = (Type) pixelSelector.getY().visit(this, arg);
		check(yType == Type.INT, pixelSelector.getY(), "only ints as pixel selector components");
		return null;
	}

	@Override
	//This method several cases--you don't have to implement them all at once.
	//Work incrementally and systematically, testing as you go.  
	public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
		String name = assignmentStatement.getName();
		Declaration target = symbolTable.lookup(name);
		check(target != null, assignmentStatement, "Ident not initialized");
		target.setInitialized(true);
		Type varType = target.getType();
		assignmentStatement.setTargetDec(target);
		if (varType != IMAGE)
		{
			check(assignmentStatement.getSelector() == null, assignmentStatement, "Pixel selector not allowed for non-image");
			Type expType = (Type) assignmentStatement.getExpr().visit(this, arg);
			if (expType == varType)
				assignmentStatement.getExpr().setCoerceTo(null);
			else if (expType == INT && varType == FLOAT)
				assignmentStatement.getExpr().setCoerceTo(FLOAT);
			else if (expType == FLOAT && varType == INT)
				assignmentStatement.getExpr().setCoerceTo(INT);
			else if (expType == COLOR && varType == INT)
				assignmentStatement.getExpr().setCoerceTo(INT);
			else if (expType == INT && varType == COLOR)
				assignmentStatement.getExpr().setCoerceTo(COLOR);
			else
				throw new TypeCheckException("Invalid assignment 1");
		}
		else if (assignmentStatement.getSelector() == null)
		{
			Type expType = (Type) assignmentStatement.getExpr().visit(this, arg);
			if (expType == varType || expType == COLOR || expType == COLORFLOAT)
				assignmentStatement.getExpr().setCoerceTo(null);
			else if (expType == INT)
				assignmentStatement.getExpr().setCoerceTo(COLOR);
			else if (expType == FLOAT)
				assignmentStatement.getExpr().setCoerceTo(COLORFLOAT);
			else
				throw new TypeCheckException("Invalid Assignment 2");
		}
		else if (assignmentStatement.getSelector() != null)
		{
			Expr xTemp = assignmentStatement.getSelector().getX();
			Expr yTemp = assignmentStatement.getSelector().getY();
			if (symbolTable.lookup(xTemp.getText()) != null || symbolTable.lookup(yTemp.getText()) != null)
				throw new TypeCheckException("Pixel selector variable already defined globally");
			if (xTemp instanceof IdentExpr && yTemp instanceof IdentExpr)
			{
				NameDef x = new NameDef(null, "int", xTemp.getText());
				NameDef  y = new NameDef(null, "int", yTemp.getText());
				symbolTable.insert(xTemp.getText(), x);
				symbolTable.insert(yTemp.getText(), y);
				Type checkRHS = (Type) assignmentStatement.getExpr().visit(this, arg);
				if (checkRHS == COLOR ||checkRHS == COLORFLOAT ||checkRHS == FLOAT ||checkRHS == INT)
					assignmentStatement.getExpr().setCoerceTo(COLOR);
				else
					throw new TypeCheckException("Invalid rhs");
				assignmentStatement.getSelector().visit(this, arg);
				symbolTable.remove(xTemp.getText());
				symbolTable.remove(yTemp.getText());
			}
			else
				throw new TypeCheckException("Not IdentExpr");
		}
		else
			throw new TypeCheckException("I have no idea");

		return null;
	}


	@Override
	public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
		Type sourceType = (Type) writeStatement.getSource().visit(this, arg);
		Type destType = (Type) writeStatement.getDest().visit(this, arg);
		check(destType == Type.STRING || destType == Type.CONSOLE, writeStatement,
				"illegal destination type for write");
		check(sourceType != Type.CONSOLE, writeStatement, "illegal source type for write");
		//writeStatement.getDest().setCoerceTo(sourceType);
		return null;
	}

	@Override
	public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
		String name = readStatement.getName();
		Declaration target = symbolTable.lookup(name);
		check(target != null, readStatement, "Ident not initialized");
		check(readStatement.getSelector() == null, readStatement, "No pixel selectors allowed");
		Type check = (Type) readStatement.getSource().visit(this, arg);
		check(check == CONSOLE || check == STRING, readStatement, "Invalid type");
		if (check == CONSOLE)
			readStatement.getSource().setCoerceTo(target.getType());
		target.setInitialized(true);
		readStatement.setTargetDec(target);
		symbolTable.insert(name, target);
		return null;
	}

	@Override
	public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
		Type ndType = (Type) declaration.getNameDef().visit(this, arg);
		declaration.setInitialized(true);
		if (ndType == IMAGE)
		{

			if (declaration.getExpr() != null)
			{
				declaration.getNameDef().setInitialized(true);
				if (declaration.getDim() == null)
				{
					Type inType = (Type) declaration.getExpr().visit(this, arg);

					if (inType != IMAGE && inType != STRING)
						throw new TypeCheckException("Init type not IMAGE");
				}
				else if (declaration.getDim() != null)
				{
					Type height = (Type) declaration.getDim().getHeight().visit(this, arg);
					Type width = (Type) declaration.getDim().getWidth().visit(this, arg);
					if (height == INT || width == INT)
						if (height != width)
							throw new TypeCheckException("Both dims not INTS");
				}
				else
					throw new TypeCheckException("Idk");
			}
		}
		else if (declaration.getExpr() != null)
		{
			if (declaration.getOp().getKind() == Kind.ASSIGN)
			{
				Type varType = declaration.getNameDef().getType();
				Type expType = (Type) declaration.getExpr().visit(this, arg);
				if (expType == varType)
					declaration.getExpr().setCoerceTo(null);
				else if (expType == INT && varType == FLOAT)
					declaration.getExpr().setCoerceTo(FLOAT);
				else if (expType == FLOAT && varType == INT)
					declaration.getExpr().setCoerceTo(INT);
				else if (expType == COLOR && varType == INT)
					declaration.getExpr().setCoerceTo(INT);
				else if (expType == INT && varType == COLOR)
					declaration.getExpr().setCoerceTo(COLOR);
				else
					throw new TypeCheckException("Invalid VarDec nonimage assignment");
			}
			else if (declaration.getOp().getKind() == Kind.LARROW)
			{
				Type expType = (Type) declaration.getExpr().visit(this, arg);
				if (expType == CONSOLE)
					declaration.getExpr().setCoerceTo(declaration.getNameDef().getType());
				if (expType != CONSOLE && expType != STRING)
					throw new TypeCheckException("Invalid VarDec nonimage read");
			}
			declaration.getNameDef().setInitialized(true);
		}
		if (declaration.getExpr() == null)
		{
			declaration.setInitialized(false);
		}
		return ndType;
	}


	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {		
		List<NameDef> list = program.getParams();
		for (NameDef nd : list)
		{
			if (symbolTable.lookup(nd.getName()) == null)
			{
				nd.setInitialized(true);
				symbolTable.insert(nd.getName(), nd);
			}
			else
				throw new TypeCheckException("Name already in use");
		}
		//Save root of AST so return type can be accessed in return statements
		root = program;
		
		//Check declarations and statements
		List<ASTNode> decsAndStatements = program.getDecsAndStatements();
		for (ASTNode node : decsAndStatements) {
			node.visit(this, arg);
		}
		return program;
	}

	@Override
	public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
		symbolTable.insert(nameDef.getName(), nameDef);
		return nameDef.getType();
	}

	@Override
	public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
		symbolTable.insert(nameDefWithDim.getName(), nameDefWithDim);
		return nameDefWithDim.getType();
	}
 
	@Override
	public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
		Type returnType = root.getReturnType();  //This is why we save program in visitProgram.
		Type expressionType = (Type) returnStatement.getExpr().visit(this, arg);
		check(returnType == expressionType, returnStatement, "return statement with invalid type");
		if (returnStatement.getExpr() instanceof IdentExpr)
		{
			Declaration test = symbolTable.lookup(returnStatement.getExpr().getText());
			if (test != null)
				if (!test.isInitialized())
					throw new TypeCheckException("Bad Error");
		}
		return null;
	}

	@Override
	public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
		Type expType = (Type) unaryExprPostfix.getExpr().visit(this, arg);
		check(expType == Type.IMAGE, unaryExprPostfix, "pixel selector can only be applied to image");
		unaryExprPostfix.getSelector().visit(this, arg);
		unaryExprPostfix.setType(Type.INT);
		unaryExprPostfix.setCoerceTo(COLOR);
		return Type.IMAGE;
	}

}
