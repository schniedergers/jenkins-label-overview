package org.jenkinsci.plugins.label_overview;

import hudson.model.Project;

import java.util.Comparator;

public class ProjectSorting implements Comparator<Project> {
	public int compare(Project l1, Project l2) {
		return l1.getName().compareTo(l2.getName());
	}
}