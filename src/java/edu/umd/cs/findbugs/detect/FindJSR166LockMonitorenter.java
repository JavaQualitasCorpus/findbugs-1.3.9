/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005 University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.detect;


import java.util.BitSet;
import java.util.Iterator;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.ObjectTypeFactory;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;

/**
 * Find places where ordinary (balanced) synchronization is performed
 * on JSR166 Lock objects.  Suggested by Doug Lea.
 *
 * @author David Hovemeyer
 */
public final class FindJSR166LockMonitorenter implements Detector, StatelessDetector {
	/**
     * 
     */
    private static final String UTIL_CONCURRRENT_SIG_PREFIX = "Ljava/util/concurrent/";

	private BugReporter bugReporter;

	private static final ObjectType LOCK_TYPE = ObjectTypeFactory.getInstance("java.util.concurrent.locks.Lock");

	public FindJSR166LockMonitorenter(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	public void visitClassContext(ClassContext classContext) {
		JavaClass jclass = classContext.getJavaClass();
		Method[] methodList = jclass.getMethods();

		for (Method method : methodList) {
			if (method.getCode() == null)
				continue;

			// We can ignore methods that don't contain a monitorenter
			BitSet bytecodeSet = classContext.getBytecodeSet(method);
			if (bytecodeSet == null) continue;
			if (!bytecodeSet.get(Constants.MONITORENTER))
				continue;


			analyzeMethod(classContext, method);

		}
	}

	private void analyzeMethod(ClassContext classContext, Method method) 
			 {
		ConstantPoolGen cpg = classContext.getConstantPoolGen();
		CFG cfg;
		try {
			cfg = classContext.getCFG(method);
		} catch (CFGBuilderException e1) {
			AnalysisContext.logError("Coult not get CFG", e1);
			return;
		}
		TypeDataflow typeDataflow;
		try {
			typeDataflow = classContext.getTypeDataflow(method);
		} catch (CheckedAnalysisException e1) {
			AnalysisContext.logError("Coult not get Type dataflow", e1);
			return;
		}

		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();

			InstructionHandle handle = location.getHandle();
			Instruction ins = handle.getInstruction();

			if (ins.getOpcode() != Constants.MONITORENTER)
				continue;
			Type type;
			try {
			TypeFrame frame = typeDataflow.getFactAtLocation(location);
			if (!frame.isValid())
				continue;
			 type = frame.getInstance(ins, cpg);
			} catch (CheckedAnalysisException e) {
				AnalysisContext.logError("Coult not get Type dataflow", e);
				continue;
			}

			if (!(type instanceof ReferenceType)) {
				// Something is deeply wrong if a non-reference type
				// is used for a monitorenter.  But, that's really a
				// verification problem.
				return;
			}

			boolean isSubtype = false;
			try {
				isSubtype = Hierarchy.isSubtype((ReferenceType) type, LOCK_TYPE);
			} catch (ClassNotFoundException e) {
				bugReporter.reportMissingClass(e);
			}
			String sig = type.getSignature();
			boolean isUtilConcurrentSig = sig.startsWith(UTIL_CONCURRRENT_SIG_PREFIX);
            
			if (isSubtype) {				
				bugReporter.reportBug(new BugInstance(this, "JLM_JSR166_LOCK_MONITORENTER", 
							isUtilConcurrentSig ? HIGH_PRIORITY : NORMAL_PRIORITY)
						.addClassAndMethod(classContext.getJavaClass(), method)
						.addType(sig)
						.addSourceForTopStackValue(classContext, method, location)
						.addSourceLine(classContext,method, location)
						);
			} else if (isUtilConcurrentSig) { 
	            		int priority = "Ljava/util/concurrent/CopyOnWriteArrayList;".equals(sig) ? HIGH_PRIORITY : NORMAL_PRIORITY;
	            		bugReporter.reportBug(new BugInstance(this, "JLM_JSR166_UTILCONCURRENT_MONITORENTER", priority)
	            				.addClassAndMethod(classContext.getJavaClass(), method)
	            				.addType(sig)
	            				.addSourceForTopStackValue(classContext, method, location)
	            				.addSourceLine(classContext,method, location));

	            
            }
		}
	}

	public void report() {
	}
}

// vim:ts=4
