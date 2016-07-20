//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package LinguaView.syntax;

import LinguaView.syntax.Index;
import LinguaView.syntax.Tree;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ConstTree extends Tree<String> {
    private String[] la;
    ConstTree parent;

    private ConstTree(String label) {
        super(label);
    }

    public ConstTree(String label, String[] la) {
        super(label);
        this.setLa(la);
    }

    public ConstTree(String label, List<Tree<String>> children) {
        super(label, children);
    }

    public ConstTree(String label, List<Tree<String>> children, String[] la) {
        super(label, children);
        this.setLa(la);
    }

    public String[] getLa() {
        return this.la;
    }

    public void setLa(String[] la) {
        this.la = la;
    }

    public List<ConstTree> getConstChildren() {
        List cl = super.getChildren();
        ArrayList res = new ArrayList();
        Iterator var4 = cl.iterator();

        while(var4.hasNext()) {
            Tree c = (Tree)var4.next();
            res.add((ConstTree)c);
        }

        return res;
    }

    public List<ConstTree> constSubTreeList() {
        List cl = super.subTreeList();
        ArrayList res = new ArrayList();
        Iterator var4 = cl.iterator();

        while(var4.hasNext()) {
            Tree c = (Tree)var4.next();
            res.add((ConstTree)c);
        }

        return res;
    }

    public static class ConstTreeIO {
        private ConstTreeIO() {
        }

        public static ConstTree ReadConstTree(String PennTree) {
            PennTree = PennTree.replaceAll("[\n\t ]+", " "); // replace \n \t to space
            PennTree = PennTree.trim();
            ConstTree res;
            if(!PennTree.matches("^\\( *\\)$") && !PennTree.equals("")) { // not empty
                if(PennTree.startsWith("( ")) {
                    // remove outside parenthesis
                    // (ROOT(...)) -> ROOT(...)
                    PennTree = PennTree.substring(PennTree.indexOf('(') + 1, PennTree.lastIndexOf(')')).trim();
                }

                // extract tag like "np#2__!=^SUBJ"
                String tag = PennTree.substring(PennTree.indexOf('(') + 1, PennTree.indexOf(' '));
                // tags[0] = tag_name, tag[1:] = function expression
                String[] tags = tag.split("__");
                res = new ConstTree(tags[0], new String[0]); // create ConstTree object with specific tag name
                // has function expression(it's possible that not only one function expression is exist)
                if(tags.length > 1) {
                    String[] L = new String[tags.length - 1]; // expression list

                    for(int currentPos = 1; currentPos < tags.length; ++currentPos) {
                        L[currentPos - 1] = tags[currentPos].replace('^', '↑').replace('!', '↓').replace('@', '∊')
                                .replace("\\in", "∊");
                    }

                    res.setLa(L);
                }

                ArrayList var7 = new ArrayList();
                Index var8 = new Index(findNearestNonSpace(PennTree, PennTree.indexOf(32)));

                while(var8.value < PennTree.length() && PennTree.charAt(var8.value) != 41) {
                    if(PennTree.charAt(var8.value) != 32) {
                        ConstTree temp = ReadConstTree(PennTree, var8);
                        var7.add(temp);
                        temp.parent = res;
                    } else {
                        ++var8.value;
                    }
                }

                res.setChildren(var7);
                return res;
            } else {
                res = new ConstTree((String)null, (List)null, (String[])null);
                return res;
            }
        }

        private static ConstTree ReadConstTree(String PennTree, Index currentPos) {
            boolean isLexical = false;
            boolean startpt = false;
            int endpt = 0;
            currentPos.value = findNearestNonSpace(PennTree, currentPos.value);
            int tag;
            int var10;
            if(PennTree.charAt(currentPos.value) == 40) {
                var10 = currentPos.value + 1;

                for(tag = currentPos.value; tag < PennTree.length(); ++tag) {
                    if(PennTree.charAt(tag) == 32 && tag > var10) {
                        endpt = tag;
                        break;
                    }
                }
            } else {
                isLexical = true;
                var10 = currentPos.value;

                for(tag = currentPos.value; tag < PennTree.length(); ++tag) {
                    if((PennTree.charAt(tag) == 40 || PennTree.charAt(tag) == 41) && tag > var10) {
                        endpt = tag;
                        break;
                    }
                }
            }

            String var11 = PennTree.substring(var10, endpt);
            String[] tags = var11.split("__");
            ConstTree res = new ConstTree(tags[0], new String[0]);
            if(tags.length > 1) {
                String[] L = new String[tags.length - 1];

                for(int temp = 1; temp < tags.length; ++temp) {
                    L[temp - 1] = tags[temp].replace('^', '↑').replace('!', '↓').replace('@', '∊')
                            .replace("\\in", "∊");
                }

                res.setLa(L);
            }

            ArrayList var12 = new ArrayList();
            currentPos.value = findNearestNonSpace(PennTree, endpt);
            if(!isLexical) {
                while(currentPos.value < PennTree.length()) {
                    if(PennTree.charAt(currentPos.value) == 41) {
                        if(res.isLeaf()) {
                            ++currentPos.value;
                            currentPos.value = findNearestNonSpace(PennTree, currentPos.value);
                        }
                        break;
                    }

                    ConstTree var13 = ReadConstTree(PennTree, currentPos);
                    var12.add(var13);
                    var13.parent = res;
                }
            }

            res.setChildren(var12);
            return res;
        }

        private static int findNearestNonSpace(String PennTree, int pos) {
            while(PennTree.charAt(pos) == 32 && pos < PennTree.length()) {
                ++pos;
            }

            return pos;
        }
    }
}
