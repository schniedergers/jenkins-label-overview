
package org.jenkinsci.plugins.label_overview;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Label;
import hudson.model.ManagementLink;
import hudson.model.Node;
import hudson.model.Project;
import hudson.model.labels.LabelAtom;
import hudson.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Registers the plugin to be recognized by Jenkins as a management link.
 * @author Klaus Schniedergers &lt;klaus.schniedergers@lookout.com&gt;
 */
@Extension
public class LabelOverview extends ManagementLink implements Describable<LabelOverview> {

    private static final Logger logger = Logger.getLogger(LabelOverview.class.getName());
    /**
     * URL to the plugin.
     */
    protected static final String URL = "label-overview-plugin";

    /**
     * Icon used by this plugin.
     */
    protected static final String ICON = "/images/48x48/clipboard.png";
    protected static final String DISPLAYNAME = "LabelOverview";
    protected static final String DESCRIPTION = "Label Overview for nodes and jobs";
    
    private static LabelOverview instance;

    /**
     * Gets the descriptor.
     * @return descriptor.
     */
    public Descriptor<LabelOverview> getDescriptor() {
        return Hudson.getInstance().getDescriptorByType(Descriptor.class);
    }

    /**
     * Gets the icon for this plugin.
     * @return icon url
     */
    public String getIconFileName() {
        return ICON;
    }

    /**
     * The URL of this plugin.
     * @return url
     */
    public String getUrlName() {
        return URL;
    }

    /**
     * Gets the name of this plugin.
     * @return plugin name
     */
    public String getDisplayName() {
        return DISPLAYNAME;
    }

    /**
     * Gets the description of this plugin.
     * @return description
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * Returns required permission to use this plugin.
     * @return Hudson administer permission.
     */
    public Permission getRequiredPermission() {
        return Hudson.ADMINISTER;
    }

    /**
     * Returns the instance of NodeManageLink.
     * @return instance the NodeManageLink.
     */
    public static LabelOverview getInstance() {
        List<ManagementLink> list = Hudson.getInstance().getManagementLinks();
        for (ManagementLink link : list) {
            if (link instanceof LabelOverview) {
                instance = (LabelOverview)link;
                break;
            }
        }
        return instance;
    }

    /**
     * Gets all Jenkins registered nodes. Used for the jelly scripts
     * @return a list with all nodes
     */
    public List<Node> getAllNodes() {
        return Hudson.getInstance().getNodes();
    }
    
    /**
     * Get all projects within the instance.
     * @return a list of all projects
     */
    public List<Project> getAllProjects() {
    	return Hudson.getInstance().getAllItems(Project.class);
    }
    
    /**
     * return the string for a HTML table which contains the map
     * between labels, slaves/nodes, and projects.
     * @return string which contains the HTML markup for the table
     */
    public String getLabelMap() {
    	String returnString = "";
    	
    	List<Node> nodes = getAllNodes();
    	Map<LabelAtom, Set<Node>> labelNodeMap = new HashMap<LabelAtom, Set<Node>>();
    	Iterator<Node> nodeIterator = nodes.iterator();
    	while (nodeIterator.hasNext()) {
    		Node node = nodeIterator.next();
    		Iterator<LabelAtom> labels = node.getAssignedLabels().iterator();
    		while (labels.hasNext()) {
    			LabelAtom label = labels.next();
    			if (!label.isSelfLabel()) {
    				Set<Node> nodeSet = labelNodeMap.get(label);
    				if (nodeSet == null) {
    					nodeSet = new HashSet<Node>();
    				}
    				nodeSet.add(node);
    				//returnString += "<br/>" + label.getName()  + ": " + node.getNodeName() + "\n";
    				labelNodeMap.put(label, nodeSet);
    			}
    		}
    	}

    	List<Project> projects = getAllProjects();
    	Map<LabelAtom, Set<Project>> labelProjectMap = new HashMap<LabelAtom, Set<Project>>();
    	Iterator<Project> projectIterator = projects.iterator();
    	while (projectIterator.hasNext()) {
    		Project project = projectIterator.next();
    		Iterator<LabelAtom> labels = project.getRelevantLabels().iterator();
    		while (labels.hasNext()) {
    			LabelAtom label = labels.next();
    			//if (!label.isSelfLabel()) {
    				Set<Project> projectSet = labelProjectMap.get(label);
    				if (projectSet == null) {
    					projectSet = new HashSet<Project>();
    				}
    				projectSet.add(project);
    				//returnString += "<br/>" + label.getName()  + ": " + node.getNodeName() + "\n";
    				labelProjectMap.put(label, projectSet);
    			//}
    		}
    	}
    	
    	Iterator<Label> labels = Hudson.getInstance().getLabels().iterator();
    	while (labels.hasNext()) {
    		Label label = labels.next();
    		if (label.isSelfLabel()) {
    			continue;
    		}
    		
    		String nodesString = "";
    		Set<Node> nodeSet = labelNodeMap.get(label);
    		if (nodeSet == null) {
    			nodesString = "N/A";
    		} else {
    			ArrayList nodeArray = new java.util.ArrayList(nodeSet);
    			//List<String> nodeList = testSerializer();
    			Collections.sort(nodeArray, new NodeSorting());
    			Iterator<Node> nodeIter = nodeArray.iterator();
    			while (nodeIter.hasNext()) {
    				nodesString += nodeIter.next().getNodeName() + " "; 
    			}
    		}
    		
    		String projectsString = "";
    		Set<Project> projectSet = labelProjectMap.get(label);
    		if (projectSet == null) {
    			projectsString = "N/A";
    		} else {
    			ArrayList projectArray = new java.util.ArrayList(projectSet);
    			Collections.sort(projectArray, new ProjectSorting());
    			Iterator<Project> projectIter = projectArray.iterator();
    			while (projectIter.hasNext()) {
    				projectsString += projectIter.next().getName() + " ";
    			}
    		}
    		
    		returnString += "<tr><td>" + label.getName() + "</td><td>" + nodesString + "</td><td>" + projectsString + "</td></tr>";
    	}
    	return returnString;
    }
    
    /**
     * for strings that end in numbers, get everything before the number.
     * E.g. item="node17" would return "node". "node17b" would return "node17b".
     * "28" would return null.
     * Return null if no prefix was found.
     * @param item - name of the item to find the prefix for
     * @return Either the prefix before the trailing numbers, or null if non found.
     */
    private String getPrefix(String item) {
       Pattern pattern = Pattern.compile("^(.*[^0-9])[0-9]*$");
       //TODO Compile the pattern just once to improve runtime 
       Matcher matcher = pattern.matcher(item);
       if (matcher.find()) {
    	   return matcher.group(1);
       } else {
    	   return null;
       }
    }
    
    /**
     * Return the trailing number in a string. E.g. item="node17" would return 17,
     * "node38b" would return null, "28" would return 28.
     * @param item - name of the item to find the trailing number for
     * @return Trailing number
     */
    private Integer getCounter(String item) {
        Pattern pattern = Pattern.compile("^.*[^0-9]([0-9]+)$");
        Matcher matcher = pattern.matcher(item);
        logger.warning("Parsing " + item);
        if (matcher.find()) {
        	if (matcher.group(1) != "") {
        		return Integer.parseInt(matcher.group(1));
        	} else {
        		return null;
        	}
        } else {
        	return null;
        }
    }
    
    /**
     * Collapse a list of strings so that series in trailing numbers are represented
     * by one entry. E.g. "node12", "node14", "node13" would be represented as "node{12..14}".
     * @param list
     * @return
     */
    private List<String> serializeStrings(List<String> list) {    	
    	// TODO: Handle node09 before node10
    	Collections.sort(list);
    	
    	/**
    	 * Discover series of strings, e.g. node1, node2, node3 should be represented as node{1..3}.
    	 * Also find breaks, so node1, node2, node3, node5, node6 should be node{1..3} node{5..6}.
    	 * Equally strip of leading zeros, so node08, node09, node10 should be node{8..10}.
    	 */
    	
    	boolean inSeries = false;
    	List<String> returnSeries = new ArrayList<String>();
    	String seriesPrefix = null;
    	Integer seriesStartCounter = null;
    	Integer seriesCurrentCounter = null;
    	Iterator<String> iter = list.iterator();
    	while (iter.hasNext()) {
    		String item = iter.next();
    		String prefix = getPrefix(item);
    		Integer counter = getCounter(item);
    		if (counter != null) {
    			if (seriesCurrentCounter != null 
    					&& counter == seriesCurrentCounter + 1 
    					&& seriesPrefix != null 
    					&& prefix == seriesPrefix) {
    				// still in series
    				seriesCurrentCounter = counter;
    			} else {
    				//break in series numbers, continue with higher number or new prefix
    				String series;
    				if (seriesPrefix != null) {
    					if (seriesStartCounter != seriesCurrentCounter) {
    						series = seriesPrefix + "{" + seriesStartCounter + ".." + seriesCurrentCounter + "}";
    					} else {
    						series = seriesPrefix + seriesCurrentCounter;
    					}
        				returnSeries.add(series);
    				}
    				seriesStartCounter = counter;
    				seriesCurrentCounter = counter;
    				seriesPrefix = prefix;
    			}
    		} else {
    			//series = null;
    			returnSeries.add(item);
    		}
    	}
    	
    	return returnSeries;
    }
    
    /**
     * Test the serializer function.
     * TODO: Actually verify the result against expected result.
     * @return
     */
    private List<String> testSerializer() {
    	List<String> tester = new ArrayList<String>();
    	tester.add("node1");
    	tester.add("node2");
    	tester.add("node3");
    	tester.add("node5");
    	tester.add("node6");
    	tester.add("nodb7");
    	tester.add("nodb08");
    	tester.add("something");
    	tester.add("node9");
    	return serializeStrings(tester);
    }
}
