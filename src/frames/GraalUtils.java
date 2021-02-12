package frames;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import fr.lirmm.graphik.graal.api.core.Atom;
import fr.lirmm.graphik.graal.api.core.AtomSetException;
import fr.lirmm.graphik.graal.api.core.ConjunctiveQuery;
import fr.lirmm.graphik.graal.api.core.Constant;
import fr.lirmm.graphik.graal.api.core.Predicate;
import fr.lirmm.graphik.graal.api.core.RuleSet;
import fr.lirmm.graphik.graal.api.core.Substitution;
import fr.lirmm.graphik.graal.api.kb.KnowledgeBase;
import fr.lirmm.graphik.graal.api.kb.KnowledgeBaseException;
import fr.lirmm.graphik.graal.core.DefaultAtom;
import fr.lirmm.graphik.graal.core.atomset.graph.DefaultInMemoryGraphStore;
import fr.lirmm.graphik.graal.core.ruleset.LinkedListRuleSet;
import fr.lirmm.graphik.graal.core.term.DefaultTermFactory;
import fr.lirmm.graphik.graal.io.dlp.DlgpParser;
import fr.lirmm.graphik.graal.io.dlp.DlgpWriter;
import fr.lirmm.graphik.graal.kb.DefaultKnowledgeBase;
import fr.lirmm.graphik.graal.kb.KBBuilder;
import fr.lirmm.graphik.graal.kb.KBBuilderException;
import fr.lirmm.graphik.util.stream.CloseableIterator;
import fr.lirmm.graphik.util.stream.IteratorException;
import graph.StringEdge;
import graph.StringGraph;

/**
 * code where I started to mine for patterns in a knowledge base (conceptnet). Very slow with a prolog/datalog engine. Deprecated. The successor of this
 * work uses aaron bembenek's querykb in a project titled pattern miner.
 * 
 * @author jcfgonc@gmail.com
 *
 */
public class GraalUtils {
	private static void initializeGraal() throws KBBuilderException, KnowledgeBaseException, IOException {
		// 0 - Create a KBBuilder
		KBBuilder kbb = new KBBuilder();
		// 1 - Add a rule
		kbb.add(DlgpParser.parseRule("mortal(X) :- human(X)."));
		// 2 - Add a fact
		kbb.add(DlgpParser.parseAtom("human(socrate)."));
		// 3 - Generate the KB
		KnowledgeBase kb = kbb.build();
		// 4 - Create a DLGP writer to print data
		DlgpWriter writer = new DlgpWriter();
		// 5 - Parse a query from a Java String
		ConjunctiveQuery query = DlgpParser.parseQuery("?(X) :- mortal(X).");
		// 6 - Query the KB
		CloseableIterator<Substitution> resultIterator = kb.query(query);
		// 7 - Iterate and print results
		writer.write("\n= Answers =\n");
		if (resultIterator.hasNext()) {
			do {
				writer.write(resultIterator.next());
				writer.write("\n");
			} while (resultIterator.hasNext());
		} else {
			writer.write("No answers.\n");
		}
		// 8 - Close resources
		kb.close();
		writer.close();
	}

	public static KnowledgeBase createKnowledgeBase(StringGraph graph) throws AtomSetException, IteratorException {
//		Ticker t = new Ticker();
//		t.getTimeDeltaLastCall();
		System.out.println("creating graal KB...");
		DefaultInMemoryGraphStore store = new DefaultInMemoryGraphStore();
		RuleSet ruleset = new LinkedListRuleSet(); // =new MappedRuleSet()?
		KnowledgeBase kb = new DefaultKnowledgeBase(store, ruleset);

		for (StringEdge edge : graph.edgeSet()) {
			String source = edge.getSource();
			String relation = edge.getLabel();
			String target = edge.getTarget();

			// ObjectIndex<String> concepts;
//			int si = concepts.addObject(source);
//			int ti = concepts.addObject(target);

			Constant cs = DefaultTermFactory.instance().createConstant(source);
			Constant ct = DefaultTermFactory.instance().createConstant(target);
			Predicate p = new Predicate(relation, 2);
			Atom a = new DefaultAtom(p, cs, ct);

			store.add(a);
		}
//		System.out.println("graal KB creation took " + t.getTimeDeltaLastCall() + " s");
		return kb;
	}

	private static int countPatternMatches(StringGraph pattern, KnowledgeBase kb) throws IteratorException, KnowledgeBaseException {
		HashMap<String, String> conceptToVariable = new HashMap<>();
		// replace each concept in the pattern to a variable
		int varCounter = 0;
		for (String concept : pattern.getVertexSet()) {
			String varName = "X" + varCounter;
			conceptToVariable.put(concept, varName);
			varCounter++;
		}
		// convert each edge to a predicate
		String baseQuery = "";
		{
			Iterator<StringEdge> edgeIterator = pattern.edgeSet().iterator();
			while (edgeIterator.hasNext()) {
				StringEdge edge = edgeIterator.next();

				String edgeLabel = edge.getLabel();
				String sourceVar = conceptToVariable.get(edge.getSource());
				String targetVar = conceptToVariable.get(edge.getTarget());

				baseQuery += String.format("%s(%s,%s)", edgeLabel, sourceVar, targetVar);

				if (edgeIterator.hasNext())
					baseQuery += ",";
			}
		}
		String queryVars = "";
		{
			Iterator<String> variablesIterator = conceptToVariable.values().iterator();
			while (variablesIterator.hasNext()) {
				String var = variablesIterator.next();
				queryVars += var;
				if (variablesIterator.hasNext())
					queryVars += ",";
			}
		}

		// baseQuery= isa(X3,X0),isa(X3,X1),isa(X2,X1)
		// queryVars= X0,X1,X2,X3

		String queryString = String.format("?(%s):-%s.", queryVars, baseQuery);
		ConjunctiveQuery cq = DlgpParser.parseQuery(queryString);
		int matches = queryPattern(cq, kb, 1 << 20);
		return matches;
	}

	private static int queryPattern(ConjunctiveQuery query, KnowledgeBase kb, final int solutionLimit)
			throws IteratorException, KnowledgeBaseException {
		CloseableIterator<Substitution> resultIterator = kb.query(query);
//			// 7 - Iterate and print results
//			if (resultIterator.hasNext()) {
//				do {
//					Substitution next = resultIterator.next();
//					System.out.println(next);
//				} while (resultIterator.hasNext());
//			}
//			kb.close();
		int matches = 0;
		// try all answers
		while (resultIterator.hasNext()) {
			resultIterator.next();
			matches++;
			if (matches > solutionLimit)
				break;
		}
		kb.close();
		return matches;
	}
}
