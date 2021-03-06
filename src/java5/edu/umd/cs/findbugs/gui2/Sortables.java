/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.gui2;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import edu.umd.cs.findbugs.AppVersion;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.ProjectPackagePrefixes;
import edu.umd.cs.findbugs.gui2.BugAspects.SortableValue;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * A useful enum for dealing with all the types of filterable and sortable data in BugInstances
 * This is the preferred way for getting the information out of a BugInstance and formatting it for display
 * It also has the comparators for the different types of data
 * 
 * @author Reuven
 */

public enum Sortables implements Comparator<SortableValue>
{

	FIRSTVERSION(edu.umd.cs.findbugs.L10N.getLocalString("sort.first_version", "First Version"))
	{
		@Override
		public String getFrom(BugInstance bug)
		{
			return Long.toString(bug.getFirstVersion());
		}

		@Override
		public String formatValue(String value)
		{	
			int seqNum = Integer.parseInt(value);
			BugCollection bugCollection = MainFrame.getInstance().bugCollection;
			if (bugCollection == null) return "--";
			AppVersion appVersion = bugCollection.getAppVersionFromSequenceNumber(seqNum);
			String appendItem = "";
			if(appVersion != null)
			{
				String timestamp = new Timestamp(appVersion.getTimestamp()).toString();
				appendItem = appVersion.getReleaseName() + " (" + timestamp.substring(0, timestamp.indexOf(' ')) + ")";
			}
			if(appendItem == "")
				appendItem = "#" +seqNum;
			return  appendItem;
		}

		@Override
		public int compare(SortableValue one, SortableValue two)
		{
			// Numerical (zero is first)
			return Integer.valueOf(one.value).compareTo(Integer.valueOf(two.value));
		}
	},

	LASTVERSION(edu.umd.cs.findbugs.L10N.getLocalString("sort.last_version", "Last Version"))
	{
		@Override
		public String getFrom(BugInstance bug)
		{
			return Long.toString(bug.getLastVersion());
		}

		@Override
		public String formatValue(String value)
		{
			//System.out.println("Formatting last version value");
			if(value.equals("-1"))
				return "";
			int seqNum = Integer.parseInt(value);
			BugCollection bugCollection = MainFrame.getInstance().bugCollection;
			if (bugCollection == null) return "--";
			AppVersion appVersion = bugCollection.getAppVersionFromSequenceNumber(seqNum);
			String appendItem = "";
			if(appVersion != null)
			{
				String timestamp = new Timestamp(appVersion.getTimestamp()).toString();
				appendItem = appVersion.getReleaseName() + " (" + timestamp.substring(0, timestamp.indexOf(' ')) + ")";
			}
			if(appendItem == "")
				appendItem = "#" + seqNum;
			return appendItem;		
			}

		@Override
		public int compare(SortableValue one, SortableValue two)
		{
			if (one.value.equals(two.value)) return 0;

			// Numerical (except that -1 is last)
			int first = Integer.valueOf(one.value);
			int second = Integer.valueOf(two.value);
			if (first == second) return 0;
			if (first < 0) return 1;
			if (second < 0) return -1;
			if (first < second) return -1;
			return 1;
		}
	},

	PRIORITY(edu.umd.cs.findbugs.L10N.getLocalString("sort.priority", "Priority"))
	{
		@Override
		public String getFrom(BugInstance bug)
		{
			return String.valueOf(bug.getPriority());
		}

		@Override
		public String formatValue(String value)
		{
			if (value.equals(String.valueOf(Detector.HIGH_PRIORITY)))
				return edu.umd.cs.findbugs.L10N.getLocalString("sort.priority_high", "High");
			if (value.equals(String.valueOf(Detector.NORMAL_PRIORITY)))
				return edu.umd.cs.findbugs.L10N.getLocalString("sort.priority_normal", "Normal");
			if (value.equals(String.valueOf(Detector.LOW_PRIORITY)))
				return edu.umd.cs.findbugs.L10N.getLocalString("sort.priority_low", "Low");
			if (value.equals(String.valueOf(Detector.EXP_PRIORITY)))
				return edu.umd.cs.findbugs.L10N.getLocalString("sort.priority_experimental", "Experimental");
			return edu.umd.cs.findbugs.L10N.getLocalString("sort.priority_ignore", "Ignore"); // This probably shouldn't ever happen, but what the hell, let's be complete

		}

		@Override
		public int compare(SortableValue one, SortableValue two)
		{
			// Numerical
			return Integer.valueOf(one.value).compareTo(Integer.valueOf(two.value));
		}
	},
	CLASS(edu.umd.cs.findbugs.L10N.getLocalString("sort.class", "Class"))
	{
		@Override
		public String getFrom(BugInstance bug)
		{
			return bug.getPrimarySourceLineAnnotation().getClassName();
		}

		@Override
		public int compare(SortableValue one, SortableValue two)
		{
			// If both have dollar signs and are of the same outer class, compare the numbers after the dollar signs.
			try
			{
				if (one.value.contains("$") && two.value.contains("$")
						&& one.value.substring(0, one.value.lastIndexOf("$")).equals(two.value.substring(0, two.value.lastIndexOf("$"))))
					return Integer.valueOf(one.value.substring(one.value.lastIndexOf("$"))).compareTo(Integer.valueOf(two.value.substring(two.value.lastIndexOf("$"))));
			}
			catch (NumberFormatException e) {} // Somebody's playing silly buggers with dollar signs, just do it lexicographically

			// Otherwise, lexicographicalify it
			return one.value.compareTo(two.value);
		}
	},
	PACKAGE(edu.umd.cs.findbugs.L10N.getLocalString("sort.package", "Package"))
	{
		@Override
		public String getFrom(BugInstance bug)
		{
			return bug.getPrimarySourceLineAnnotation().getPackageName();
		}

		@Override
		public String formatValue(String value)
		{
			if (value.equals(""))
				return "(Default)";
			return value;
		}
	},
	PACKAGE_PREFIX(edu.umd.cs.findbugs.L10N.getLocalString("sort.package_prefix", "Package prefix")) {
		@Override
		public String getFrom(BugInstance bug)
		{
			int count = GUISaveState.getInstance().getPackagePrefixSegments();
			
			if (count < 1) 
				count = 1;
			String packageName = bug.getPrimarySourceLineAnnotation().getPackageName();
			return ClassName.extractPackagePrefix(packageName, count);
		}

		@Override
		public String formatValue(String value)
		{
			return value + "...";
		}
	},
	CATEGORY(edu.umd.cs.findbugs.L10N.getLocalString("sort.category", "Category"))
	{
		@Override
		public String getFrom(BugInstance bug)
		{

			BugPattern bugPattern = bug.getBugPattern();
			if (bugPattern == null) {
				return "?";
			}
			return bugPattern.getCategory();
		}

		@Override
		public String formatValue(String value)
		{			
			return I18N.instance().getBugCategoryDescription(value);
		}
		
		@Override
		public int compare(SortableValue one, SortableValue two)
		{
			String catOne = one.value;
			String catTwo = two.value;
			int compare = catOne.compareTo(catTwo);
			if (compare == 0)
				return 0;
			if (catOne.equals("CORRECTNESS"))
				return -1;
			if (catTwo.equals("CORRECTNESS"))
				return 1;
			return compare;
			
		}

		
	},
	DESIGNATION(edu.umd.cs.findbugs.L10N.getLocalString("sort.designation", "Designation"))
	{
		@Override
		public String getFrom(BugInstance bug)
		{
			return bug.getUserDesignationKey();
		}

		/**
		 * value is the key of the designations.
		 * @param value
		 * @return
		 */
		@Override
		public String formatValue(String value)
		{
			return I18N.instance().getUserDesignation(value);
		}

		@Override
		public String[] getAllSorted()
		{//FIXME I think we always want user to see all possible designations, not just the ones he has set in his project, Agreement?  -Dan
			List<String> sortedDesignations=I18N.instance().getUserDesignationKeys(true);
			return sortedDesignations.toArray(new String[sortedDesignations.size()]);
		}
	},
	BUGCODE(edu.umd.cs.findbugs.L10N.getLocalString("sort.bug_kind", "Bug Kind"))
	{
		@Override
		public String getFrom(BugInstance bug)
		{
			BugPattern bugPattern = bug.getBugPattern();
			if (bugPattern == null) return null;
			return bugPattern.getAbbrev();
		}

		@Override
		public String formatValue(String value)
		{
			return I18N.instance().getBugTypeDescription(value);
		}

		@Override
		public int compare(SortableValue one, SortableValue two)
		{
			return formatValue(one.value).compareTo(formatValue(two.value));
		}
	},
	TYPE(edu.umd.cs.findbugs.L10N.getLocalString("sort.bug_pattern", "Bug Pattern"))
	{
		@Override
		public String getFrom(BugInstance bug)
		{
			if((bug.getBugPattern()) == null)
				return "?";
			else return bug.getBugPattern().getType();
		}

		@Override
		public String formatValue(String value)
		{
			return I18N.instance().getShortMessageWithoutCode(value);
		}
	},
	
	
	BUG_RANK(edu.umd.cs.findbugs.L10N.getLocalString("sort.bug_bugrank", "Bug Rank"))
	{
		String [] values;
		{ values = new String[40];
		  for(int i = 0; i < values.length; i++)
			  values[i] = String.format("%2d", i);
		}
		@Override
		public String getFrom(BugInstance bug)
		{
			if((bug.getBugPattern()) == null)
				return "??";
			
			int rank = BugRanker.findRank(bug);
			return values[rank];
		}

		@Override
		public String formatValue(String value)
		{
			return value;
		}
	},
	
	
	PROJECT(edu.umd.cs.findbugs.L10N.getLocalString("sort.bug_project", "Project")) {

		@Override
        public String getFrom(BugInstance bug) {
	        ProjectPackagePrefixes p = MainFrame.getInstance().projectPackagePrefixes;
	        Collection<String> projects = p.getProjects(bug.getPrimaryClass().getClassName());
	        if (projects.size() == 0)
	        	return "unclassified";
	        String result = projects.toString();
	        
	        return result.substring(1, result.length() -1);
        }
		
		@Override 
		public boolean isAvailable(MainFrame mf) {
			return mf.projectPackagePrefixes.size() > 0;
		}
		
	},
	
	DIVIDER(" ")
	{
		@Override
		public String getFrom(BugInstance bug)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public String[] getAll()
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public String formatValue(String value)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		public int compare(SortableValue one, SortableValue two)
		{
			throw new UnsupportedOperationException();
		}
	};


	String prettyName;

	Sortables(String prettyName)
	{
		this.prettyName = prettyName;
	}

	@Override
	public String toString()
	{
		return prettyName;
	}

	public abstract String getFrom(BugInstance bug);
	public String[] getAll()
	{
		return getAll(BugSet.getMainBugSet());
	}
	public String[] getAll(BugSet set)
	{
		return set.getAll(this);
	}

	public String formatValue(String value)
	{
		return value;
	}

	public int compare(SortableValue one, SortableValue two)
	{
		// Lexicographical by default
		return one.value.compareTo(two.value);
	}

	public String[] getAllSorted()
	{
		return getAllSorted(BugSet.getMainBugSet());
	}

	public String[] getAllSorted(BugSet set)
	{
		String[] values = getAll(set);
		SortableValue[] pairs = new SortableValue[values.length];
		for (int i = 0; i < values.length; i++)
			pairs[i] = new SortableValue(this, values[i]);
		Arrays.sort(pairs, this);
		for (int i = 0; i < values.length; i++)
			values[i] = pairs[i].value;
		return values;
	}

	private SortableStringComparator comparator = new SortableStringComparator(this);
	
	public SortableStringComparator getComparator() {
		return comparator;
	}
	public Comparator<BugLeafNode> getBugLeafNodeComparator()
	{
		final Sortables key = this;
		return new Comparator<BugLeafNode>()
		{
			public int compare(BugLeafNode one, BugLeafNode two)
			{
				return key.compare(new SortableValue(key, key.getFrom(one.getBug())), new SortableValue(key, key.getFrom(two.getBug())));
			}
		};
	}
	
	public boolean isAvailable(MainFrame frame) {
		return true;
	}

	public static Sortables getSortableByPrettyName(String name)
	{
		for (Sortables s: values())
		{
			if (s.prettyName.equals(name))
				return s;
		}
			return null;
	}
}
