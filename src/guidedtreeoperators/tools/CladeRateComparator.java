package guidedtreeoperators.tools;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


import beast.app.treeannotator.TreeAnnotator;
import beast.app.treeannotator.TreeAnnotator.MemoryFriendlyTreeSet;
import beast.app.util.Application;
import beast.app.util.OutFile;
import beast.app.util.TreeFile;
import beast.core.Description;
import beast.core.Input;
import beast.core.Runnable;
import beast.core.util.Log;
import beast.evolution.tree.Tree;
import beast.evolution.tree.Node;
import beast.util.Randomizer;

@Description("Match clades from two tree sets and print support for both sets so "
		+ "they can be plotted in an X-Y plot")
public class CladeRateComparator extends Runnable {
	final public Input<TreeFile> srcInput = new Input<>("tree","source tree (set) file");
	final public Input<OutFile> outputInput = new Input<>("out", "output file, or stdout if not specified",
			new OutFile("[[none]]"));
	final public Input<OutFile> svgOutputInput = new Input<>("svg", "svg output file. if not specified, no SVG output is produced.",
			new OutFile("[[none]]"));
	final public Input<Integer> burnInPercentageInput = new Input<>("burnin", "percentage of trees to used as burn-in (and will be ignored)", 10);
	final public Input<String> annotationInput = new Input<>("annotation", "annotation to compare with clade probabilities", "");
	
	
	double n;
	
	final String header = "<svg version=\"1.2\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" class=\"graph\" aria-labelledby=\"title\" role=\"img\" height=\"1200\">\n" + 
			"<g class=\"grid x-grid\" id=\"xGrid\">\n" + 
			"  <line x1=\"90\" x2=\"90\" y1=\"10\" y2=\"1010\" style=\"stroke:#000;stroke-width:2\"></line>\n" + 
			"</g>\n" + 
			"<g class=\"grid y-grid\" id=\"yGrid\">\n" + 
			"  <line x1=\"90\" x2=\"1090\" y1=\"1010\" y2=\"1010\" style=\"stroke:#000;stroke-width:2\"></line>\n" + 
			"</g>\n" + 
			"<line x1=\"90\" x2=\"1090\" y1=\"1010\" y2=\"10\" style=\"stroke:#000;stroke-width:2\"></line>\n" + 
			"<line x1=\"90\" x2=\"840\" y1=\"760\" y2=\"10\" style=\"stroke:#00f;stroke-width:1\"></line>\n" + 
			"<line x1=\"340\" x2=\"1090\" y1=\"1010\" y2=\"260\" style=\"stroke:#00f;stroke-width:1\"></line>\n" + 
			"  <g class=\"labels x-labels\">\n" + 
			"  <text x=\"90\" y=\"1030\">0.0</text>\n" + 
			"  <text x=\"290\" y=\"1030\">0.2</text>\n" + 
			"  <text x=\"490\" y=\"1030\">0.4</text>\n" + 
			"  <text x=\"690\" y=\"1030\">0.6</text>\n" + 
			"  <text x=\"890\" y=\"1030\">0.8</text>\n" + 
			"  <text x=\"1090\" y=\"1030\">1.0</text>\n" + 
			"  <text x=\"520\" y=\"1040\" class=\"label-title\">file1</text>\n" + 
			"</g>\n" + 
			"<g class=\"labels y-labels\">\n" + 
			"  <text x=\"60\" y=\"15\">1.0</text>\n" + 
			"  <text x=\"60\" y=\"215\">0.8</text>\n" + 
			"  <text x=\"60\" y=\"415\">0.6</text>\n" + 
			"  <text x=\"60\" y=\"615\">0.4</text>\n" + 
			"  <text x=\"60\" y=\"815\">0.2</text>\n" + 
			"  <text x=\"60\" y=\"1015\">0.0</text>\n" + 
			"  <text x=\"40\" y=\"540\" class=\"label-title\" transform=\"rotate(90,40,540)\">file2</text>\n" + 
			"</g>\n" + 
			"<g class=\"data\" data-setname=\"Our first data set\">\n"; 
//			"  <circle cx=\"90\" cy=\"192\" data-value=\"7.2\" r=\"4\"></circle>\n" + 
//			"  <circle cx=\"240\" cy=\"141\" data-value=\"8.1\" r=\"4\"></circle>\n" + 
//			"  <circle cx=\"388\" cy=\"179\" data-value=\"7.7\" r=\"4\"></circle>\n" + 
//			"  <circle cx=\"531\" cy=\"200\" data-value=\"6.8\" r=\"4\"></circle>\n" + 
//			"  <circle cx=\"677\" cy=\"104\" data-value=\"6.7\" r=\"4\"></circle>\n" + 
		final String footer =	"</g>\n" + 
			"</svg>\n" + 
			"\n";
	
	
	@Override
	public void initAndValidate() {
	}

	@Override
	public void run() throws Exception {
		PrintStream out = System.out;
		if (outputInput.get() != null && !outputInput.get().getName().equals("[[none]]")) {
			Log.warning("Writing to file " + outputInput.get().getPath());
			out = new PrintStream(outputInput.get());
		}
		PrintStream svg = null;
		if (svgOutputInput.get() != null && !svgOutputInput.get().getName().equals("[[none]]")) {
			Log.warning("Writing to file " + svgOutputInput.get().getPath());
			svg = new PrintStream(svgOutputInput.get());
			svg.println(header.replaceAll("file1", srcInput.get().getPath()));
		}

		String path = srcInput.get().getPath();
		Log.warning("Processing " + path);
		MemoryFriendlyTreeSet srcTreeSet = new TreeAnnotator().new MemoryFriendlyTreeSet(path, burnInPercentageInput.get());
		
		
		// Find the clade sets
		srcTreeSet.reset();
		Tree tree = srcTreeSet.next();
		RateCladeSet cladeSet = new RateCladeSet(tree);
		n = 1;
		while (srcTreeSet.hasNext()) {
			tree = srcTreeSet.next();
			cladeSet.add(tree);
			n++;
		}
		
		
		// Iterate through each tree and clade
		int treeNum = 1;
		srcTreeSet.reset();
		tree = srcTreeSet.next();
		Map<String, Double> cladeMap = new LinkedHashMap<>();
		Map<String, Double> cladeHeightMap = new LinkedHashMap<>();
		Map<String, List<Double>> cladeAnnotationMap = new HashMap<>();
		String annotation = annotationInput.get();
		while (srcTreeSet.hasNext()) {
			//if (treeNum % 100 == 0) Log.warning("Tree number: " + treeNum);
			treeNum++;
			for (int i = 0; i < cladeSet.getCladeCount(); i++) {
				String clade = cladeSet.getClade(i);
				int support = cladeSet.getFrequency(i);
				cladeMap.put(clade, support/ n);
				cladeHeightMap.put(clade, cladeSet.getMeanNodeHeight(i));
				
				if (annotation != "") {
					
					Node mrca = cladeSet.getClade(i, tree);
					if (mrca != null) {
						
						double annotationVal = (double) mrca.getMetaData(annotation);
						
						if (cladeAnnotationMap.containsKey(clade)) {
							List<Double> rates = cladeAnnotationMap.get(clade);
							rates.add(annotationVal);
						}else {
							List<Double> rates = new ArrayList();
							rates.add(annotationVal);
							cladeAnnotationMap.put(clade, rates);
						}
						
					}
					
				}
			
			}
			
			tree = srcTreeSet.next();
		}
		
		
		
		// Print
		for (int i = 0; i < cladeSet.getCladeCount(); i++) {
			String clade = cladeSet.getClade(i);
			List<Double> rates = cladeAnnotationMap.get(clade);
			String str = "";
			int support = cladeSet.getFrequency(i);
			for (int j = 0; j < rates.size(); j ++) {
				str += rates.get(j);
				if (j < rates.size() - 1) str += ",";
			}
			out.println(clade.replaceAll(" ", "") + "|" + 1.0 * support/n + "|" + str);
		}
		
		
		
		
		

		/*
		
		
		// create map of clades to support values in set1
		Map<String, Double> cladeMap = new LinkedHashMap<>();
		Map<String, Double> cladeHeightMap = new LinkedHashMap<>();
		for (int i = 0; i < cladeSet1.getCladeCount(); i++) {
			String clade = cladeSet1.getClade(i);
			int support = cladeSet1.getFrequency(i);
			cladeMap.put(clade, support/ n1);
			cladeHeightMap.put(clade, cladeSet1.getMeanNodeHeight(i));
		}
		
		// process clades in set2
		for (int i = 0; i < cladeSet2.getCladeCount(); i++) {			
			String clade = cladeSet2.getClade(i);
			int support = cladeSet2.getFrequency(i);
			if (cladeMap.containsKey(clade)) {
				// clade is also in set1
				output(out, svg, clade,cladeMap.get(clade),support/n2);
				double h1 = cladeHeightMap.get(clade);
				double h2 = cladeSet2.getMeanNodeHeight(i);
				System.out.println((h1 - h2) + " " + (100 * (h1 - h2) / h1));
				
				cladeMap.remove(clade);
			} else {
				// clade is not in set1
				output(out, svg, clade, 0.0, support/n2);
			}
		}		
		
		// process left-overs of clades in set1 that are not in set2 
		for (String clade : cladeMap.keySet()) {
			output(out, svg, clade, cladeMap.get(clade), 0.0);
		}

		if (svg != null) {
			svg.println(footer);
		}
		
		*/
		Log.info.println("Done");
	}

	private void output(PrintStream out, PrintStream svg, String clade, Double support1, double support2) {
		out.println(clade.replaceAll(" ", "") + " " + support1 + " " + support2);
		if ((support1 < 0.1 && support2 > 0.9) ||
			(support2 < 0.1 && support1 > 0.9)) {
			Log.warning("Problem clade: " + clade.replaceAll(" ", "") + " " + support1 + " " + support2);
		}
		
		if (Math.abs(support1 - support2) > 0.25) {
				Log.warning("Clade of interest (>25% difference): " + clade.replaceAll(" ", "") + " " + support1 + " " + support2);
			}

		if (svg != null) {
			svg.println("  <circle style=\"opacity:0.25;fill:#a00000\" cx=\""+ (90 +1000* support1 + Randomizer.nextInt(10) - 5) + 
					"\" cy=\""+ (10 + 1000 - 1000 * support2 + Randomizer.nextInt(10) - 5) +"\" "
							+ "data-value=\"7.2\" r=\"" + (support1 + support2) * 10 + "\"></circle>");
		}
	}



	public static void main(String[] args) throws Exception {
		new Application(new CladeRateComparator(), "Clade Set Comparator", args);

	}

}
