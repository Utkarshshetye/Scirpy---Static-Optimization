package analysis.PredicatePushdown;

import java.util.*;
import java.util.stream.Collectors;

// ==========================================================================
// PredicateTree — base class
//
// Represents a predicate expression over a dataframe as a tree of nodes.
// The tree is built during backward analysis and flattened to DNF groups
// in rewriteDataSource() before being pushed down to read_csv/read_parquet.
//
// Node types:
//   PredicateLeaf  — a single condition, e.g. df['age'] > 30
//   PredicateNot   — negation of a child tree
//   PredicateAnd   — conjunction of children (all must hold)
//   PredicateOr    — disjunction of children (at least one must hold)
//
// equals()/hashCode() are defined on all nodes so that:
//   1. Structural deduplication works during analysis.
//   2. PredicateFlowSet.signature() can rely on flatten() deduplication.
// ==========================================================================

abstract class PredicateTree {
    abstract List<Set<Predicate>> flatten();
    abstract String toDebugString(int indent);
    abstract String getDataframeName();

    protected String getIndent(int indent) {
        return String.join("", Collections.nCopies(indent, "  "));
    }

    protected String getConditionKey(Condition cond) {
        return "(" + cond.getOn() + ", " + cond.getOp() + ", " + cond.getWith() + ")";
    }

//    private String normalizeList(String value) {
//
//        value = value.trim();
//
//        // remove outer quotes if present
//        if (value.startsWith("\"") && value.endsWith("\""))
//            value = value.substring(1, value.length() - 1);
//
//        // remove extra outer brackets
//        if (value.startsWith("[[") && value.endsWith("]]"))
//            value = value.substring(1, value.length() - 1);
//
//        return value;
//    }

//    protected String getConditionKey(Condition cond) {
//        if (cond.getOp().equals("isin"))
//            cond.setOp("in");
//        return "(" + cond.getOn() + ", " + cond.getOp() + ", " + cond.getWith() + ")";
//    }
}

// --------------------------------------------------------------------------
// PredicateLeaf — single condition node
// --------------------------------------------------------------------------
class PredicateLeaf extends PredicateTree {
    Predicate predicate;

    public PredicateLeaf(Predicate predicate) {
        this.predicate = predicate;
    }

    @Override
    public List<Set<Predicate>> flatten() {
        Set<Predicate> group = new LinkedHashSet<>();
        group.add(predicate);
        return Collections.singletonList(group);
    }

    @Override
    public String getDataframeName() {
        return predicate.getDf_var();
    }

    @Override
    public String toDebugString(int indent) {
        Condition cond = predicate.getCond();
        return getIndent(indent) + "LEAF: " + getConditionKey(cond)
                + " [df=" + predicate.getDf_var() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PredicateLeaf)) return false;
        PredicateLeaf other = (PredicateLeaf) o;
        return Objects.equals(predicate.getDf_var(),         other.predicate.getDf_var())
                && Objects.equals(predicate.getCond().getOn(),   other.predicate.getCond().getOn())
                && Objects.equals(predicate.getCond().getOp(),   other.predicate.getCond().getOp())
                && Objects.equals(predicate.getCond().getWith(), other.predicate.getCond().getWith());
    }

    @Override
    public int hashCode() {
        return Objects.hash("LEAF",
                predicate.getDf_var(),
                predicate.getCond().getOn(),
                predicate.getCond().getOp(),
                predicate.getCond().getWith());
    }
}

// --------------------------------------------------------------------------
// PredicateNot — negation node
// Applies De Morgan's laws during flatten():
//   ~(A & B) => (~A | ~B)
//   ~(A | B) => (~A & ~B)
//   ~~A      => A
// --------------------------------------------------------------------------
class PredicateNot extends PredicateTree {
    PredicateTree child;

    public PredicateNot(PredicateTree child) {
        this.child = child;
    }

    @Override
    public List<Set<Predicate>> flatten() {
        return normalize(this).flatten();
    }

    @Override
    public String getDataframeName() {
        return child.getDataframeName();
    }

    private PredicateTree normalize(PredicateTree tree) {
        if (tree instanceof PredicateNot) {
            PredicateNot not = (PredicateNot) tree;
            if (not.child instanceof PredicateNot) {
                return normalize(((PredicateNot) not.child).child);
            } else if (not.child instanceof PredicateAnd) {
                List<PredicateTree> neg = new ArrayList<>();
                for (PredicateTree c : ((PredicateAnd) not.child).children)
                    neg.add(normalize(new PredicateNot(c)));
                return new PredicateOr(neg);
            } else if (not.child instanceof PredicateOr) {
                List<PredicateTree> neg = new ArrayList<>();
                for (PredicateTree c : ((PredicateOr) not.child).children)
                    neg.add(normalize(new PredicateNot(c)));
                return new PredicateAnd(neg);
            } else if (not.child instanceof PredicateLeaf) {
                return new PredicateLeaf(invertPredicate(((PredicateLeaf) not.child).predicate));
            }
        } else if (tree instanceof PredicateAnd) {
            List<PredicateTree> nc = new ArrayList<>();
            for (PredicateTree c : ((PredicateAnd) tree).children) nc.add(normalize(c));
            return new PredicateAnd(nc);
        } else if (tree instanceof PredicateOr) {
            List<PredicateTree> nc = new ArrayList<>();
            for (PredicateTree c : ((PredicateOr) tree).children) nc.add(normalize(c));
            return new PredicateOr(nc);
        }
        return tree;
    }

    private Predicate invertPredicate(Predicate pred) {
        Condition c  = pred.getCond();
        String value = c.getWith();
        String invOp = invertOperator(c.getOp());
        String invVal = value;
        if ((c.getOp().equals("==") || c.getOp().equals("!="))
                && (value.equals("True") || value.equals("False"))) {
            invVal = value.equals("True") ? "False" : "True";
        }
        return new Predicate(new Condition(c.getOn(), invOp, invVal),
                pred.getParentUnit(), pred.getUnit(), pred.getDf_var());
    }

    private String invertOperator(String op) {
        switch (op) {
            case ">":       return "<=";
            case ">=":      return "<";
            case "<":       return ">=";
            case "<=":      return ">";
            case "==":      return "!=";
            case "!=":      return "==";
            case "isin":
            case "in":
                return "not_in";
            case "not_in":  return "in";
            default:        return "not_" + op;
        }
    }

    @Override
    public String toDebugString(int indent) {
        return getIndent(indent) + "NOT\n" + child.toDebugString(indent + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PredicateNot)) return false;
        return Objects.equals(child, ((PredicateNot) o).child);
    }

    @Override
    public int hashCode() {
        return Objects.hash("NOT", child);
    }
}

// --------------------------------------------------------------------------
// PredicateAnd — conjunction node
// flatten() = Cartesian product of children's groups, deduplicated
// --------------------------------------------------------------------------
class PredicateAnd extends PredicateTree {
    List<PredicateTree> children;

    public PredicateAnd(List<PredicateTree> children) {
        this.children = new ArrayList<>(children);
    }

    public PredicateAnd(PredicateTree... children) {
        this.children = new ArrayList<>(Arrays.asList(children));
    }

    public void addChild(PredicateTree child) { this.children.add(child); }

    @Override
    public String getDataframeName() {
        return children.isEmpty() ? null : children.get(0).getDataframeName();
    }

    @Override
    public List<Set<Predicate>> flatten() {
        if (children.isEmpty()) return new ArrayList<>();
        List<Set<Predicate>> result = children.get(0).flatten();
        for (int i = 1; i < children.size(); i++) {
            List<Set<Predicate>> next = new ArrayList<>();
            for (Set<Predicate> existing : result) {
                for (Set<Predicate> childGroup : children.get(i).flatten()) {
                    Map<String, Predicate> merged = new LinkedHashMap<>();
                    for (Predicate p : existing)    merged.put(key(p), p);
                    for (Predicate p : childGroup)  merged.put(key(p), p);
                    next.add(new LinkedHashSet<>(merged.values()));
                }
            }
            result = next;
        }
        return dedup(result);
    }

    @Override
    public String toDebugString(int indent) {
        StringBuilder sb = new StringBuilder(getIndent(indent) + "AND\n");
        for (PredicateTree c : children) sb.append(c.toDebugString(indent + 1)).append("\n");
        return sb.toString().trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PredicateAnd)) return false;
        return Objects.equals(children, ((PredicateAnd) o).children);
    }

    @Override
    public int hashCode() { return Objects.hash("AND", children); }

    private String key(Predicate p) {
        return p.getDf_var() + ":" + getConditionKey(p.getCond());
    }

    static List<Set<Predicate>> dedup(List<Set<Predicate>> groups) {
        List<Set<Predicate>> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Set<Predicate> g : groups) {
            String sig = g.stream()
                    .map(p -> p.getDf_var() + ":("
                            + p.getCond().getOn() + ","
                            + p.getCond().getOp() + ","
                            + p.getCond().getWith() + ")")
                    .sorted().collect(Collectors.joining(","));
            if (seen.add(sig)) result.add(g);
        }
        return result;
    }
}

// --------------------------------------------------------------------------
// PredicateOr — disjunction node
// flatten() = union of children's groups, deduplicated
// --------------------------------------------------------------------------
class PredicateOr extends PredicateTree {
    List<PredicateTree> children;

    public PredicateOr(List<PredicateTree> children) {
        this.children = new ArrayList<>(children);
    }

    public PredicateOr(PredicateTree... children) {
        this.children = new ArrayList<>(Arrays.asList(children));
    }

    public void addChild(PredicateTree child) { this.children.add(child); }

    @Override
    public String getDataframeName() {
        return children.isEmpty() ? null : children.get(0).getDataframeName();
    }

    @Override
    public List<Set<Predicate>> flatten() {
        List<Set<Predicate>> result = new ArrayList<>();
        for (PredicateTree c : children) result.addAll(c.flatten());
        return PredicateAnd.dedup(result);
    }

    @Override
    public String toDebugString(int indent) {
        StringBuilder sb = new StringBuilder(getIndent(indent) + "OR\n");
        for (PredicateTree c : children) sb.append(c.toDebugString(indent + 1)).append("\n");
        return sb.toString().trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PredicateOr)) return false;
        return Objects.equals(children, ((PredicateOr) o).children);
    }

    @Override
    public int hashCode() { return Objects.hash("OR", children); }
}