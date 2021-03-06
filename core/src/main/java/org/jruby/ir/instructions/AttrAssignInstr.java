package org.jruby.ir.instructions;

import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.specialized.OneArgOperandAttrAssignInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.ir.IRFlags.*;

// Instruction representing Ruby code of the form: "a[i] = 5"
// which is equivalent to: a.[](i,5)
public class AttrAssignInstr extends NoResultCallInstr {
    public static AttrAssignInstr create(Operand obj, String attr, Operand[] args) {
        if (!containsArgSplat(args) && args.length == 1) {
            return new OneArgOperandAttrAssignInstr(obj, attr, args);
        }

        return new AttrAssignInstr(obj, attr, args);
    }

    public AttrAssignInstr(Operand obj, String attr, Operand[] args) {
        super(Operation.ATTR_ASSIGN, CallType.UNKNOWN, attr, obj, args, null);
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        // SSS FIXME: For now, forcibly require a frame for scopes
        // having attr-assign instructions. However, we can avoid this
        // by passing in the frame self explicitly to Helpers.invoke(..)
        // rather than try to get it off context.getFrameSelf()
        super.computeScopeFlags(scope);
        scope.getFlags().add(REQUIRES_FRAME);
        return true;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new AttrAssignInstr(getReceiver().cloneForInlining(ii), getName(), cloneCallArgs(ii));
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope dynamicScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) getReceiver().retrieve(context, self, currScope, dynamicScope, temp);
        IRubyObject[] values = prepareArguments(context, self, currScope, dynamicScope, temp);

        CallType callType = self == object ? CallType.FUNCTIONAL : CallType.NORMAL;
        Helpers.invoke(context, object, getName(), values, callType, Block.NULL_BLOCK);
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.AttrAssignInstr(this);
    }
}
