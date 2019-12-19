package guidedtreeoperators.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;

public class TreeGuideUtils {
	
	private static int[] dummy = new int[1];
	private static String[] conditionalCladeStrings;
	private static int stringIndex = 0;
	private static StringBuilder builderLeft = new StringBuilder();
	private static StringBuilder builderRight = new StringBuilder();

	
	
	// Returns all the clades of a tree 
	public static synchronized String[] getClades(Tree tree) {
		
		
		
		// Reset string array, as opposed to reallocating memory
		int numStrings = tree.getLeafNodeCount();
		if (TreeGuideUtils.conditionalCladeStrings == null || TreeGuideUtils.conditionalCladeStrings.length != numStrings) {
			TreeGuideUtils.conditionalCladeStrings = new String[numStrings];
		}
		for (int i = 0; i < numStrings; i ++) TreeGuideUtils.conditionalCladeStrings[i] = "";
		
		TreeGuideUtils.stringIndex = 0;
		TreeGuideUtils.getConditionalCladeString(tree.getRoot(), conditionalCladeStrings);
		
		
		return conditionalCladeStrings;
		

	}
	

	
	// Generates a list of sorted strings describing conditional clades in the tree
	// For example if the tree is (((1,0),3),2)
	// Then the conditional clade strings are:
	// 		0,1|3 and 0,1,3|2
	// This is a succinct form of:
	// 		0,1|0,1,3 and 0,1,3|0,1,2,3
	// These are also sorted across the split, so it will never be
	//		3,4,5|0,1,2
	private static synchronized List<Integer> getConditionalCladeString(Node node, String[] strings) {

		
		// Return a string of the current node label
		if (node.isLeaf()) {
			List<Integer> nr = new ArrayList<>();
			nr.add(node.getNr());
			return nr;
		}
		
		
		
		List<Integer> leftChildNodes = getConditionalCladeString(node.getChild(0), strings);
		List<Integer> rightChildNodes = getConditionalCladeString(node.getChild(1), strings);
		
		
		
		TreeGuideUtils.builderLeft.setLength(0);
		TreeGuideUtils.builderRight.setLength(0);
		
		
		// Clade string of left clade
		for (int i = 0; i < leftChildNodes.size(); i ++) {
			builderLeft.append(leftChildNodes.get(i));
			if (i < leftChildNodes.size() - 1) builderLeft.append(",");
		}
		
		
		// Clade string of right clade
		for (int i = 0; i < rightChildNodes.size(); i ++) {
			builderRight.append(rightChildNodes.get(i));
			if (i < rightChildNodes.size() - 1) builderRight.append(",");
		}
		
		
		boolean bothChildrenHaveChildren = !node.getChild(0).isLeaf() && !node.getChild(1).isLeaf();
		if (bothChildrenHaveChildren) {
			
			if (leftChildNodes.get(0) < leftChildNodes.get(1)) {
				strings[TreeGuideUtils.stringIndex] = builderLeft.toString() + "|" + builderRight.toString();
			}else {
				strings[TreeGuideUtils.stringIndex] = builderRight.toString() + "|" + builderLeft.toString();
			}
			
			TreeGuideUtils.stringIndex++;
			
		}
		
		else {
		
			// Conditional clade string of left clade
			if (!node.getChild(0).isLeaf()) {
				strings[TreeGuideUtils.stringIndex] = builderLeft.toString() + "|" + builderRight.toString();
				TreeGuideUtils.stringIndex++;
			}
			
			// Conditional clade string of left clade
			if (!node.getChild(1).isLeaf()) {
				strings[TreeGuideUtils.stringIndex] = builderRight.toString() + "|" + builderLeft.toString();
				TreeGuideUtils.stringIndex++;
			}
				
		}
		
		
		leftChildNodes.addAll(rightChildNodes);
		Collections.sort(leftChildNodes);
		return leftChildNodes;
		
		
	}
	
	
	
	// Returns a unique identifier of the tree topology
	// The identifier will not describe branch lengths or other meta data, topology only.
	public static synchronized String serialiseNode(Node node) {
		TreeGuideUtils.dummy[0] = 1;
		return serialiseNode(node, dummy);
	}
	
    private static synchronized String serialiseNode(Node node, int[] maxNodeInClade) {
        StringBuilder buf = new StringBuilder();

        if (!node.isLeaf()) {

            if (node.getChildCount() <= 2) {
                // Computationally cheap method for special case of <=2 children

                buf.append("(");
                String child1 = TreeGuideUtils.serialiseNode(node.getChild(0), maxNodeInClade);
                int child1Index = maxNodeInClade[0];
                if (node.getChildCount() > 1) {
                    String child2 = TreeGuideUtils.serialiseNode(node.getChild(1), maxNodeInClade);
                    int child2Index = maxNodeInClade[0];
                    if (child1Index > child2Index) {
                        buf.append(child2);
                        buf.append(",");
                        buf.append(child1);
                    } else {
                        buf.append(child1);
                        buf.append(",");
                        buf.append(child2);
                        maxNodeInClade[0] = child1Index;
                    }
                } else {
                    buf.append(child1);
                }
                buf.append(")");
                if (node.getID() != null) {
                    buf.append(node.getNr()+1);
                }

            } else {
                // General method for >2 children

                String[] childStrings = new String[node.getChildCount()];
                int[] maxNodeNrs = new int[node.getChildCount()];
                Integer[] indices = new Integer[node.getChildCount()];
                for (int i = 0; i < node.getChildCount(); i++) {
                    childStrings[i] = TreeGuideUtils.serialiseNode(node.getChild(i), maxNodeInClade);
                    maxNodeNrs[i] = maxNodeInClade[0];
                    indices[i] = i;
                }

                Arrays.sort(indices, (i1, i2) -> {
                    if (maxNodeNrs[i1] < maxNodeNrs[i2])
                        return -1;

                    if (maxNodeNrs[i1] > maxNodeNrs[i2])
                        return 1;

                    return 0;
                });

                maxNodeInClade[0] = maxNodeNrs[maxNodeNrs.length - 1];

                buf.append("(");
                for (int i = 0; i < indices.length; i++) {
                    if (i > 0)
                        buf.append(",");

                    buf.append(childStrings[indices[i]]);
                }

                buf.append(")");

                if (node.getID() != null) {
                    buf.append(node.getNr() + 1);
                }
            }

        } else {
            maxNodeInClade[0] = node.getNr();
            buf.append(node.getNr() + 1);
        }

        return buf.toString();
        
    }

	

}
