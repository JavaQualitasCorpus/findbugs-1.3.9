/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

/**
 * Abstract base class for BugAnnotations describing constructs
 * which are contained in a Java package.  Specifically,
 * this includes classes, methods, and fields.
 *
 * @author David Hovemeyer
 * @see BugAnnotation
 */
public abstract class PackageMemberAnnotation extends BugAnnotationWithSourceLines  {
	private static final long serialVersionUID = -8208567669352996892L;

	protected final @DottedClassName String className;
	protected  String description;
	/**
	 * Constructor.
	 *
	 * @param className name of the class
	 */
	protected PackageMemberAnnotation(@DottedClassName String className, String description) {
		if (className.length() == 0)
			throw new IllegalArgumentException("Empty classname not allowed");
		if (className.indexOf('/') >= 0) {
			assert false: "classname " + className + " should be dotted";
			className = className.replace('/', '.');
		}
		this.className = DescriptorFactory.canonicalizeString(className);
		AnalysisContext context = AnalysisContext.currentAnalysisContext();
		if (context != null) this.sourceFileName = context.lookupSourceFile(className);
		else this.sourceFileName = SourceLineAnnotation.UNKNOWN_SOURCE_FILE;
		if (description != null)
			description = description.intern();
		this.description = description;
	}


	/**
	 * Get the class name.
	 */
	public final @DottedClassName String getClassName() {
		return className;
	}
	/**
	 * Get the class name.
	 */
	public final ClassDescriptor getClassDescriptor() {
		return DescriptorFactory.instance().getClassDescriptorForDottedClassName(className);
	}

	/**
	 * Get the package name.
	 */
	public final String getPackageName() {
		int lastDot = className.lastIndexOf('.');
		if (lastDot < 0)
			return "";
		else
			return className.substring(0, lastDot);
	}

	/**
	 * Format the annotation.
	 * Note that this version (defined by PackageMemberAnnotation)
	 * only handles the "class" and "package" keys, and calls
	 * formatPackageMember() for all other keys.
	 *
	 * @param key the key
	 * @return the formatted annotation
	 */
	public final String format(String key, ClassAnnotation primaryClass) {
		if (key.equals("class.givenClass"))
			return shorten(primaryClass.getPackageName(), className);
		if (key.equals("class"))
			return className;
		else if (key.equals("package"))
			return getPackageName();
		else if (key.equals("") && FindBugsDisplayFeatures.isAbridgedMessages() && primaryClass != null)
			return formatPackageMember("givenClass", primaryClass);
		else return formatPackageMember(key, primaryClass);
	}

	public void setDescription(String description) {
		this.description = description.intern();
	}

	public String getDescription() {
		return description;
	}

	/**
	 * Shorten a type name of remove extraneous components.
	 * Candidates for shortening are classes in same package as this annotation and
	 * classes in the <code>java.lang</code> package.
	 */
	protected static String shorten(String pkgName, String typeName) {
		int index = typeName.lastIndexOf('.');
		if (index >= 0) {
			String otherPkg = typeName.substring(0, index);
			if (otherPkg.equals(pkgName) || otherPkg.equals("java.lang"))
				typeName = typeName.substring(index + 1);
		}
		return typeName;
	}
	

	protected static String removePackage( String typeName) {
		int index = typeName.lastIndexOf('.');
		if (index >= 0) {
			return typeName.substring(index+1);
		}
		return typeName;
	}
	/**
	 * Shorten a type name by removing the package name
	 */
	protected static String removePackageName(String typeName) {
		int index = typeName.lastIndexOf('.');
		if (index >= 0) {
			typeName = typeName.substring(index + 1);
		}
		return typeName;
	}
	/**
	 * Do default and subclass-specific formatting.
	 *
	 * @param key the key specifying how to do the formatting
	 * @param primaryClass TODO
	 */
	protected abstract String formatPackageMember(String key, ClassAnnotation primaryClass);

	/**
	 * All PackageMemberAnnotation object share a common toString() implementation.
	 * It uses the annotation description as a pattern for FindBugsMessageFormat,
	 * passing a reference to this object as the single message parameter.
	 */
	@Override
	public String toString() {
		return toString(null);
	}

	
	public String toString(ClassAnnotation primaryClass) {
		String pattern = I18N.instance().getAnnotationDescription(description);
		FindBugsMessageFormat format = new FindBugsMessageFormat(pattern);
		return format.format(new BugAnnotation[]{this}, primaryClass);
	}
	public boolean isSignificant() {
		return true;
	}


}

// vim:ts=4
