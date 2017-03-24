package com.shieldsbetter.scedel.expressions;

import com.shieldsbetter.scedel.ParseLocation;
import com.shieldsbetter.scedel.Scedel;
import com.shieldsbetter.scedel.ScriptEnvironment;
import com.shieldsbetter.scedel.values.VBoolean;
import com.shieldsbetter.scedel.values.VDict;
import com.shieldsbetter.scedel.values.VFunction;
import com.shieldsbetter.scedel.values.VNone;
import com.shieldsbetter.scedel.values.VNumber;
import com.shieldsbetter.scedel.values.VProxy;
import com.shieldsbetter.scedel.values.VSeq;
import com.shieldsbetter.scedel.values.VString;
import com.shieldsbetter.scedel.values.VToken;
import com.shieldsbetter.scedel.values.VUnavailable;
import com.shieldsbetter.scedel.values.Value;

public interface Expression {
    public ParseLocation getParseLocation();
    public Value evaluate(Scedel.HostEnvironment h, ScriptEnvironment s);
    public boolean yeildsBakedLValues();
    
    public void prettyRender(int indentUnit, int indentLevels, StringBuilder b);
    public void accept(Visitor v);
    
    public static interface Visitor {
        public void visitBinaryExpression(BinaryExpression e);
        public void visitClosureBindingExpression(ClosureBuildingExpression e);
        public void visitDictionaryExpression(DictionaryExpression e);
        public void visitFunctionCallExpression(FunctionCallExpression e);
        public void visitHostExpression(HostExpression e);
        public void visitLiteralExpression(LiteralExpression e);
        public void visitPickExpression(PickExpression e);
        public void visitSequenceExpression(SequenceExpression e);
        public void visitUnaryExpression(UnaryExpression e);
        public void visitVariableNameExpression(VariableNameExpression e);
        public void visitVBoolean(VBoolean v);
        public void visitVDict(VDict v);
        public void visitVFunction(VFunction v);
        public void visitVNone(VNone v);
        public void visitVNumber(VNumber v);
        public void visitVProxy(VProxy v);
        public void visitVSeq(VSeq v);
        public void visitVString(VString v);
        public void visitVToken(VToken v);
        public void visitVUnavailable(VUnavailable v);
    }
}
