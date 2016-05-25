//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package LinguaView.syntax;

import SyntaxUtils.MapFactory;
import SyntaxUtils.MyMethod;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import jigsaw.syntax.Constituent;
import jigsaw.util.CollectionUtils;
import jigsaw.util.Pair;

public class Tree<L> implements Serializable, Comparable<Tree<L>>, Iterable<Tree<L>> {
    private static final long serialVersionUID = 1L;
    L label;
    List<Tree<L>> children;

    public void setChild(int i, Tree<L> child) {
        this.children.set(i, child);
    }

    public void setChildren(List<Tree<L>> c) {
        this.children = c;
    }

    public List<Tree<L>> getChildren() {
        return this.children;
    }

    public Tree<L> getChild(int i) {
        return (Tree)this.children.get(i);
    }

    public L getLabel() {
        return this.label;
    }

    public boolean isLeaf() {
        return this.getChildren().isEmpty();
    }

    public boolean isPreTerminal() {
        return this.getChildren().size() == 1 && ((Tree)this.getChildren().get(0)).isLeaf();
    }

    public List<L> getYield() {
        ArrayList yield = new ArrayList();
        appendYield(this, yield);
        return yield;
    }

    public Collection<Constituent<L>> getConstituentCollection() {
        ArrayList constituents = new ArrayList();
        appendConstituent(this, (Collection)constituents, 0);
        return constituents;
    }

    public Map<Tree<L>, Constituent<L>> getConstituents() {
        IdentityHashMap constituents = new IdentityHashMap();
        appendConstituent(this, (Map)constituents, 0);
        return constituents;
    }

    public Map<Pair<Integer, Integer>, List<Tree<L>>> getSpanMap() {
        Map cMap = this.getConstituents();
        HashMap spanMap = new HashMap();
        Iterator var4 = cMap.entrySet().iterator();

        while(var4.hasNext()) {
            Entry trees = (Entry)var4.next();
            Tree t = (Tree)trees.getKey();
            Constituent c = (Constituent)trees.getValue();
            Pair span = Pair.newPair(Integer.valueOf(c.getStart()), Integer.valueOf(c.getEnd() + 1));
            CollectionUtils.addToValueList(spanMap, span, t);
        }

        var4 = spanMap.values().iterator();

        while(var4.hasNext()) {
            List trees1 = (List)var4.next();
            Collections.sort(trees1, new Comparator<Tree<L>>() {
                public int compare(Tree<L> t1, Tree<L> t2) {
                    return t2.getDepth() - t1.getDepth();
                }
            });
        }

        return spanMap;
    }

    public Map<Tree<L>, Constituent<L>> getConstituents(MapFactory<Tree<L>, Constituent<L>> mf) {
        Map constituents = mf.buildMap();
        appendConstituent(this, (Map)constituents, 0);
        return constituents;
    }

    private static <L> int appendConstituent(Tree<L> tree, Map<Tree<L>, Constituent<L>> constituents, int index) {
        if(tree.isLeaf()) {
            Constituent nextIndex1 = new Constituent(tree.getLabel(), index, index);
            constituents.put(tree, nextIndex1);
            return 1;
        } else {
            int nextIndex = index;

            Tree c;
            for(Iterator var5 = tree.getChildren().iterator(); var5.hasNext(); nextIndex += appendConstituent(c, constituents, nextIndex)) {
                c = (Tree)var5.next();
            }

            Constituent c1 = new Constituent(tree.getLabel(), index, nextIndex - 1);
            constituents.put(tree, c1);
            return nextIndex - index;
        }
    }

    private static <L> int appendConstituent(Tree<L> tree, Collection<Constituent<L>> constituents, int index) {
        if(!tree.isLeaf() && !tree.isPreTerminal()) {
            int nextIndex1 = index;

            Tree c;
            for(Iterator var5 = tree.getChildren().iterator(); var5.hasNext(); nextIndex1 += appendConstituent(c, constituents, nextIndex1)) {
                c = (Tree)var5.next();
            }

            Constituent c1 = new Constituent(tree.getLabel(), index, nextIndex1 - 1);
            constituents.add(c1);
            return nextIndex1 - index;
        } else {
            Constituent nextIndex = new Constituent(tree.getLabel(), index, index);
            constituents.add(nextIndex);
            return 1;
        }
    }

    private static <L> void appendNonTerminals(Tree<L> tree, List<Tree<L>> yield) {
        if(!tree.isLeaf()) {
            yield.add(tree);
            Iterator var3 = tree.getChildren().iterator();

            while(var3.hasNext()) {
                Tree child = (Tree)var3.next();
                appendNonTerminals(child, yield);
            }

        }
    }

    public List<Tree<L>> getTerminals() {
        ArrayList yield = new ArrayList();
        appendTerminals(this, yield);
        return yield;
    }

    public List<Tree<L>> getNonTerminals() {
        ArrayList yield = new ArrayList();
        appendNonTerminals(this, yield);
        return yield;
    }

    private static <L> void appendTerminals(Tree<L> tree, List<Tree<L>> yield) {
        if(tree.isLeaf()) {
            yield.add(tree);
        } else {
            Iterator var3 = tree.getChildren().iterator();

            while(var3.hasNext()) {
                Tree child = (Tree)var3.next();
                appendTerminals(child, yield);
            }

        }
    }

    public Tree<L> shallowClone() {
        ArrayList newChildren = new ArrayList(this.children.size());
        Iterator var3 = this.children.iterator();

        while(var3.hasNext()) {
            Tree child = (Tree)var3.next();
            newChildren.add(child.shallowClone());
        }

        return new Tree(this.label, newChildren);
    }

    public Tree<L> shallowCloneJustRoot() {
        return new Tree(this.label);
    }

    private static <L> void appendYield(Tree<L> tree, List<L> yield) {
        if(tree.isLeaf()) {
            yield.add(tree.getLabel());
        } else {
            Iterator var3 = tree.getChildren().iterator();

            while(var3.hasNext()) {
                Tree child = (Tree)var3.next();
                appendYield(child, yield);
            }

        }
    }

    public List<L> getPreTerminalYield() {
        ArrayList yield = new ArrayList();
        appendPreTerminalYield(this, yield);
        return yield;
    }

    public List<L> getTerminalYield() {
        List terms = this.getTerminals();
        ArrayList yield = new ArrayList();
        Iterator var4 = terms.iterator();

        while(var4.hasNext()) {
            Tree term = (Tree)var4.next();
            yield.add(term.getLabel());
        }

        return yield;
    }

    public List<Tree<L>> getPreTerminals() {
        ArrayList preterms = new ArrayList();
        appendPreTerminals(this, preterms);
        return preterms;
    }

    public List<Tree<L>> getTreesOfDepth(int depth) {
        ArrayList trees = new ArrayList();
        appendTreesOfDepth(this, trees, depth);
        return trees;
    }

    private static <L> void appendPreTerminalYield(Tree<L> tree, List<L> yield) {
        if(tree.isPreTerminal()) {
            yield.add(tree.getLabel());
        } else {
            Iterator var3 = tree.getChildren().iterator();

            while(var3.hasNext()) {
                Tree child = (Tree)var3.next();
                appendPreTerminalYield(child, yield);
            }

        }
    }

    private static <L> void appendPreTerminals(Tree<L> tree, List<Tree<L>> yield) {
        if(tree.isPreTerminal()) {
            yield.add(tree);
        } else {
            Iterator var3 = tree.getChildren().iterator();

            while(var3.hasNext()) {
                Tree child = (Tree)var3.next();
                appendPreTerminals(child, yield);
            }

        }
    }

    private static <L> void appendTreesOfDepth(Tree<L> tree, List<Tree<L>> yield, int depth) {
        if(tree.getDepth() == depth) {
            yield.add(tree);
        } else {
            Iterator var4 = tree.getChildren().iterator();

            while(var4.hasNext()) {
                Tree child = (Tree)var4.next();
                appendTreesOfDepth(child, yield, depth);
            }

        }
    }

    public List<Tree<L>> getPreOrderTraversal() {
        ArrayList traversal = new ArrayList();
        traversalHelper(this, traversal, true);
        return traversal;
    }

    public List<Tree<L>> getPostOrderTraversal() {
        ArrayList traversal = new ArrayList();
        traversalHelper(this, traversal, false);
        return traversal;
    }

    private static <L> void traversalHelper(Tree<L> tree, List<Tree<L>> traversal, boolean preOrder) {
        if(preOrder) {
            traversal.add(tree);
        }

        Iterator var4 = tree.getChildren().iterator();

        while(var4.hasNext()) {
            Tree child = (Tree)var4.next();
            traversalHelper(child, traversal, preOrder);
        }

        if(!preOrder) {
            traversal.add(tree);
        }

    }

    public int getDepth() {
        int maxDepth = 0;
        Iterator var3 = this.children.iterator();

        while(var3.hasNext()) {
            Tree child = (Tree)var3.next();
            int depth = child.getDepth();
            if(depth > maxDepth) {
                maxDepth = depth;
            }
        }

        return maxDepth + 1;
    }

    public int size() {
        int sum = 0;

        Tree child;
        for(Iterator var3 = this.children.iterator(); var3.hasNext(); sum += child.size()) {
            child = (Tree)var3.next();
        }

        return sum + 1;
    }

    public List<Tree<L>> getAtDepth(int depth) {
        ArrayList yield = new ArrayList();
        appendAtDepth(depth, this, yield);
        return yield;
    }

    private static <L> void appendAtDepth(int depth, Tree<L> tree, List<Tree<L>> yield) {
        if(depth >= 0) {
            if(depth == 0) {
                yield.add(tree);
            } else {
                Iterator var4 = tree.getChildren().iterator();

                while(var4.hasNext()) {
                    Tree child = (Tree)var4.next();
                    appendAtDepth(depth - 1, child, yield);
                }

            }
        }
    }

    public void setLabel(L label) {
        this.label = label;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        this.toStringBuilder(sb);
        return sb.toString();
    }

    public void toStringBuilder(StringBuilder sb) {
        if(!this.isLeaf()) {
            sb.append('(');
        }

        if(this.getLabel() != null) {
            sb.append(this.getLabel());
        }

        if(!this.isLeaf()) {
            Iterator var3 = this.getChildren().iterator();

            while(var3.hasNext()) {
                Tree child = (Tree)var3.next();
                sb.append(' ');
                child.toStringBuilder(sb);
            }

            sb.append(')');
        }

    }

    public String toEscapedString() {
        StringBuilder sb = new StringBuilder();
        this.toStringBuilderEscaped(sb);
        return sb.toString();
    }

    public void toStringBuilderEscaped(StringBuilder sb) {
        if(!this.isLeaf()) {
            sb.append('(');
        }

        if(this.getLabel() != null) {
            if(this.isLeaf()) {
                String child = this.getLabel().toString();
                child = child.replaceAll("\\(", "-LRB-");
                child = child.replaceAll("\\)", "-RRB-");
                child = child.replaceAll("\\\\", "-BACKSLASH-");
                sb.append(child);
            } else {
                sb.append(this.getLabel());
            }
        }

        if(!this.isLeaf()) {
            Iterator var3 = this.getChildren().iterator();

            while(var3.hasNext()) {
                Tree child1 = (Tree)var3.next();
                sb.append(' ');
                child1.toStringBuilderEscaped(sb);
            }

            sb.append(')');
        }

    }

    public Tree(L label, List<Tree<L>> children) {
        this.label = label;
        this.children = children;
    }

    public Tree(L label) {
        this.label = label;
        this.children = Collections.emptyList();
    }

    public Set<Tree<L>> subTrees() {
        return (Set)this.subTrees(new HashSet());
    }

    public List<Tree<L>> subTreeList() {
        return (List)this.subTrees(new ArrayList());
    }

    public Collection<Tree<L>> subTrees(Collection<Tree<L>> n) {
        n.add(this);
        List kids = this.getChildren();
        Iterator var4 = kids.iterator();

        while(var4.hasNext()) {
            Tree kid = (Tree)var4.next();
            kid.subTrees(n);
        }

        return n;
    }

    public Iterator<Tree<L>> iterator() {
        return new Tree.TreeIterator();
    }

    public <O> Tree<O> transformNodes(MyMethod<L, O> trans) {
        ArrayList newChildren = new ArrayList(this.children.size());
        Iterator var4 = this.children.iterator();

        while(var4.hasNext()) {
            Tree child = (Tree)var4.next();
            newChildren.add(child.transformNodes(trans));
        }

        return new Tree(trans.call(this.label), newChildren);
    }

    public <O> Tree<O> transformNodesUsingNode(MyMethod<Tree<L>, O> trans) {
        ArrayList newChildren = new ArrayList(this.children.size());
        Object newLabel = trans.call(this);
        Iterator var5 = this.children.iterator();

        while(var5.hasNext()) {
            Tree child = (Tree)var5.next();
            newChildren.add(child.transformNodesUsingNode(trans));
        }

        return new Tree(newLabel, newChildren);
    }

    public <O> Tree<O> transformNodesUsingNodePostOrder(MyMethod<Tree<L>, O> trans) {
        ArrayList newChildren = new ArrayList(this.children.size());
        Iterator var4 = this.children.iterator();

        while(var4.hasNext()) {
            Tree newLabel = (Tree)var4.next();
            newChildren.add(newLabel.transformNodesUsingNode(trans));
        }

        Object newLabel1 = trans.call(this);
        return new Tree(newLabel1, newChildren);
    }

    public int hashCode() {
        boolean prime = true;
        byte result = 1;
        int result1 = 31 * result + (this.label == null?0:this.label.hashCode());

        Tree child;
        for(Iterator var4 = this.children.iterator(); var4.hasNext(); result1 = 31 * result1 + (child == null?0:child.hashCode())) {
            child = (Tree)var4.next();
        }

        return result1;
    }

    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        } else if(obj == null) {
            return false;
        } else if(this.getClass() != obj.getClass()) {
            return false;
        } else if(!(obj instanceof Tree)) {
            return false;
        } else {
            Tree other = (Tree)obj;
            if(!this.label.equals(other.label)) {
                return false;
            } else if(this.getChildren().size() != other.getChildren().size()) {
                return false;
            } else {
                for(int i = 0; i < this.getChildren().size(); ++i) {
                    if(!((Tree)this.getChildren().get(i)).equals(other.getChildren().get(i))) {
                        return false;
                    }
                }

                return true;
            }
        }
    }

    public int compareTo(Tree<L> o) {
        if(o.getLabel() instanceof Comparable && this.getLabel() instanceof Comparable) {
            int cmp = ((Comparable)o.getLabel()).compareTo(this.getLabel());
            if(cmp != 0) {
                return cmp;
            } else {
                int cmp2 = Double.compare((double)this.getChildren().size(), (double)o.getChildren().size());
                if(cmp2 != 0) {
                    return cmp2;
                } else {
                    for(int i = 0; i < this.getChildren().size(); ++i) {
                        int cmp3 = ((Tree)this.getChildren().get(i)).compareTo((Tree)o.getChildren().get(i));
                        if(cmp3 != 0) {
                            return cmp3;
                        }
                    }

                    return 0;
                }
            }
        } else {
            throw new IllegalArgumentException("Tree labels are not comparable");
        }
    }

    public boolean isPhrasal() {
        return this.getYield().size() > 1;
    }

    public Constituent<L> getLeastCommonAncestorConstituent(int i, int j) {
        List yield = this.getYield();
        Constituent leastCommonAncestorConstituentHelper = getLeastCommonAncestorConstituentHelper(this, 0, yield.size(), i, j);
        return leastCommonAncestorConstituentHelper;
    }

    public Tree<L> getTopTreeForSpan(int i, int j) {
        List yield = this.getYield();
        return getTopTreeForSpanHelper(this, 0, yield.size(), i, j);
    }

    private static <L> Tree<L> getTopTreeForSpanHelper(Tree<L> tree, int start, int end, int i, int j) {
        assert i <= j;

        if(start == i && end == j) {
            assert tree.getLabel().toString().matches("\\w+");

            return tree;
        } else {
            LinkedList queue = new LinkedList();
            queue.addAll(tree.getChildren());

            List currYield;
            for(int currStart = start; !queue.isEmpty(); currStart += currYield.size()) {
                Tree remove = (Tree)queue.remove();
                currYield = remove.getYield();
                int currEnd = currStart + currYield.size();
                if(currStart <= i && currEnd >= j) {
                    return getTopTreeForSpanHelper(remove, currStart, currEnd, i, j);
                }
            }

            return null;
        }
    }

    private static <L> Constituent<L> getLeastCommonAncestorConstituentHelper(Tree<L> tree, int start, int end, int i, int j) {
        if(start == i && end == j) {
            return new Constituent(tree.getLabel(), start, end);
        } else {
            LinkedList queue = new LinkedList();
            queue.addAll(tree.getChildren());

            List currYield;
            for(int currStart = start; !queue.isEmpty(); currStart += currYield.size()) {
                Tree remove = (Tree)queue.remove();
                currYield = remove.getYield();
                int currEnd = currStart + currYield.size();
                if(currStart <= i && currEnd >= j) {
                    Constituent leastCommonAncestorConstituentHelper = getLeastCommonAncestorConstituentHelper(remove, currStart, currEnd, i, j);
                    if(leastCommonAncestorConstituentHelper != null) {
                        return leastCommonAncestorConstituentHelper;
                    }
                    break;
                }
            }

            return new Constituent(tree.getLabel(), start, end);
        }
    }

    public boolean hasUnariesOtherThanRoot() {
        assert this.children.size() == 1;

        return this.hasUnariesHelper((Tree)this.children.get(0));
    }

    private boolean hasUnariesHelper(Tree<L> tree) {
        if(tree.isPreTerminal()) {
            return false;
        } else if(tree.getChildren().size() == 1) {
            return true;
        } else {
            Iterator var3 = tree.getChildren().iterator();

            while(var3.hasNext()) {
                Tree child = (Tree)var3.next();
                if(this.hasUnariesHelper(child)) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean hasUnaryChain() {
        return this.hasUnaryChainHelper(this, false);
    }

    private boolean hasUnaryChainHelper(Tree<L> tree, boolean unaryAbove) {
        boolean result = false;
        if(tree.getChildren().size() == 1) {
            return unaryAbove?true:(((Tree)tree.getChildren().get(0)).isPreTerminal()?false:this.hasUnaryChainHelper((Tree)tree.getChildren().get(0), true));
        } else {
            Iterator var5 = tree.getChildren().iterator();

            while(true) {
                Tree child;
                do {
                    if(!var5.hasNext()) {
                        return result;
                    }

                    child = (Tree)var5.next();
                } while(child.isPreTerminal());

                result = result || this.hasUnaryChainHelper(child, false);
            }
        }
    }

    public void removeUnaryChains() {
        this.removeUnaryChainHelper(this, (Tree)null);
    }

    private void removeUnaryChainHelper(Tree<L> tree, Tree<L> parent) {
        if(!tree.isLeaf()) {
            if(tree.getChildren().size() == 1 && !tree.isPreTerminal()) {
                if(parent != null) {
                    tree = (Tree)tree.getChildren().get(0);
                    parent.getChildren().set(0, tree);
                    this.removeUnaryChainHelper(tree, parent);
                } else {
                    this.removeUnaryChainHelper((Tree)tree.getChildren().get(0), tree);
                }
            } else {
                Iterator var4 = tree.getChildren().iterator();

                while(var4.hasNext()) {
                    Tree child = (Tree)var4.next();
                    if(!child.isPreTerminal()) {
                        this.removeUnaryChainHelper(child, (Tree)null);
                    }
                }
            }

        }
    }

    private class TreeIterator implements Iterator<Tree<L>> {
        private List<Tree<L>> treeStack;

        private TreeIterator() {
            this.treeStack = new ArrayList();
            this.treeStack.add(Tree.this);
        }

        public boolean hasNext() {
            return !this.treeStack.isEmpty();
        }

        public Tree<L> next() {
            int lastIndex = this.treeStack.size() - 1;
            Tree tr = (Tree)this.treeStack.remove(lastIndex);
            List kids = tr.getChildren();

            for(int i = kids.size() - 1; i >= 0; --i) {
                this.treeStack.add((Tree)kids.get(i));
            }

            return tr;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
