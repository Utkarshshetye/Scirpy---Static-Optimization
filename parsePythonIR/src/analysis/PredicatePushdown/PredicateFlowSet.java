package analysis.PredicatePushdown;

import soot.toolkits.scalar.ArraySparseSet;

import java.util.*;

/**
 * Extends ArraySparseSet with a signature-based equals() so Soot's
 * fixed-point check can detect loop convergence.
 *
 * WHY THIS EXISTS (one reason only):
 * Soot's BackwardFlowAnalysis convergence loop (unmodifiable library code):
 *
 *     FlowSet prev = out.clone();
 *     flowThrough(in, node, out);
 *     if (out.equals(prev)) stopIterating();   // <-- only hook we have
 *
 * ArraySparseSet.equals() uses == (reference identity) on elements.
 * Each iteration builds new PredicateTree objects so == always fails,
 * Soot never stops, and the analysis loops forever.
 *
 * This class overrides equals() to compare by DNF signature instead,
 * so two flow sets containing logically equivalent trees — regardless
 * of structural nesting depth — are treated as equal and convergence fires.
 *
 * Everything else (add, remove, copy, union, iterator, size …) is
 * inherited from ArraySparseSet unchanged.
 */
public class PredicateFlowSet extends ArraySparseSet<PredicateTree> {

    // ------------------------------------------------------------------ //
    //  Fixed-point convergence hook                                        //
    // ------------------------------------------------------------------ //

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PredicateFlowSet)) return false;
        PredicateFlowSet other = (PredicateFlowSet) o;
        if (this.size() != other.size()) return false;
        Iterator<PredicateTree> it = this.iterator();
        while (it.hasNext()) {
            if (!other.containsSignature(signature(it.next()))) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 0;
        Iterator<PredicateTree> it = iterator();
        while (it.hasNext()) h += signature(it.next()).hashCode();
        return h;
    }

    private boolean containsSignature(String sig) {
        Iterator<PredicateTree> it = iterator();
        while (it.hasNext()) {
            if (signature(it.next()).equals(sig)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ //
    //  DNF signature                                                        //
    //                                                                       //
    //  Flattens the tree to DNF groups and sorts everything                //
    //  lexicographically so the signature is:                              //
    //    - order-insensitive (AND(A,B) == AND(B,A))                       //
    //    - nesting-insensitive (AND(A,AND(A,B)) == AND(A,B))              //
    //  Used by equals() above and by PredicatePushdownAnalysis             //
    //  to detect when a new AND/OR node adds no new information.           //
    // ------------------------------------------------------------------ //

    static String signature(PredicateTree tree) {
        if (tree == null) return "null";
        try {
            List<Set<Predicate>> groups = tree.flatten();
            List<String> groupSigs = new ArrayList<>();
            for (Set<Predicate> group : groups) {
                List<String> condKeys = new ArrayList<>();
                for (Predicate p : group) {
                    condKeys.add(p.getDf_var()
                            + ":(" + p.getCond().getOn()
                            + "," + p.getCond().getOp()
                            + "," + p.getCond().getWith() + ")");
                }
                Collections.sort(condKeys);
                groupSigs.add(condKeys.toString());
            }
            Collections.sort(groupSigs);
            return groupSigs.toString();
        } catch (Exception e) {
            return tree.toDebugString(0);
        }
    }
}