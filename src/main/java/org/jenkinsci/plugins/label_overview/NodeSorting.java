package org.jenkinsci.plugins.label_overview;

import hudson.model.Node;

import java.util.Comparator;

public class NodeSorting implements Comparator<Node> {
	public int compare(Node l1, Node l2) {
		return l1.getNodeName().compareTo(l2.getNodeName());
	}
}