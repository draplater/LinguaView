package LinguaView.syntax;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.*;

import LinguaView.TreePanel;

/**
 * ConstTreePanel is originally a part of Federico Sangati's constituent viewer. I revised
 * the code to fit it into LinguaView.
 * ConstTreePanel loads one specified constituent tree at a time and arranges its layout in 
 * an recursive manner.
 * Note that ConstTreePanel doesn't load tree from strings. This work is left to TSNodeLabel.
 * 
 * @author Federico Sangati (original code)
 * @author shuoyang
 *
 */
@SuppressWarnings("serial")
public class ConstTreePanel extends TreePanel<ConstTree> {
	/**
	 * sentenceLength stores the number of tokens in the raw sentence
	 * nodesCount stores the number of all the nodes, both terminal and internal
	 */
	int sentenceLength, nodesCount;
	/**
	 * For the convenience of process, we assign an integer id to each TSNodeLabel
	 * indexTable maps each TSNodeLabel to this integer id
	 */
	IdentityHashMap<ConstTree, Integer> indexTable;
	/**
	 * nodesArray stores all the nodes in the constituent tree
	 * lexicalsArray stores all the terminal nodes in the constituent tree
	 */
	ConstTree[] nodesArray, lexicalsArray;
	/**
	 * labelArray stores the CFG label in string form for each corresponding constituent TSNodeLabel
	 */
	String[] labelArray;

	public boolean displayLA = true;
	int laYMargin = 0; // vertical margin of la

	/**
	 * This is a group of layout information.
	 * XLeftArray is the x-loc of the left edge of constituent nodes
	 * YArray is the y-loc of the lower edge of constituent nodes
	 * XMiddleArray is the x-loc of the mid of constituent nodes
	 * wordLengthsArray is the length on x-axis of each constituent node
	 * 
	 * Please note that, to get the layout information of a TSNodeLabel n,
	 * one should first call indexTable.get(n) to get its index in these arrays.
	 */
	int[] XLeftArray, YArray, XMiddleArray, wordLengthsArray;
	/**
	 * skewedLines indicate whether the lines between levels should be skew or straight
	 */
	public boolean skewedLines = true;
	protected int[] YTopArray;
	private int[] nodeHeightsArray;
	protected int[] nodeLengthsArray;

	/**
	 * the shift of width after arrangeAllTerminals
	 */
	private int totalWidthShift;

	public ConstTreePanel() {
		
	}
	
	public ConstTreePanel(ArrayList<ConstTree> treebank) {
		loadTreebank(treebank);
	}
	
	public void setSentenceNumber(int sn) {
		this.sentenceNumber = sn;
	}
	
	public void init() {
		loadFont();
		loadSentence();
        setPreferredSize(area);
        revalidate();
        repaint();
	}
	
	public void render(Graphics2D g2) {
		g2.setFont(font); 
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke());

		if(this.indexTable != null) {
			ConstTree t = this.treebank.get(this.sentenceNumber);
			this.drawTree(g2, t);
		}
	}

	private void drawLine(int x1, int y1, int x2, int y2, Graphics2D g2) {
		if (!skewedLines) {
			int yM = y1 + (y2-y1)/2;
			g2.drawLine(x1,y1,x1,yM);
			g2.drawLine(x1,yM,x2,yM);
			g2.drawLine(x2,yM,x2,y2);
		}
		else {
			g2.drawLine(x1,y1,x2,y2);
		}
	}


	public void drawTree(Graphics2D g2, ConstTree n) {
		int i = indexTable.get(n);
		int thisY = this.YTopArray[i] + this.textTipMargin + this.fontHight;
		g2.drawString(n.getLabel(), this.XMiddleArray[i] - this.metrics.stringWidth(n.getLabel()) / 2, thisY);
		int ci;
		if(this.displayLA) {
			String[] cx;
			ci = (cx = n.getLa()).length;

			for(int var6 = 0; var6 < ci; ++var6) {
				String c = cx[var6];
				g2.setColor(Color.GRAY);
				thisY += this.fontHight + this.laYMargin;
				g2.drawString(c, this.XMiddleArray[i] - this.metrics.stringWidth(c) / 2, thisY);
				g2.setColor(Color.BLACK);
			}
		}

		Iterator var11 = n.getConstChildren().iterator();

		while(var11.hasNext()) {
			ConstTree var10 = (ConstTree)var11.next();
			ci = this.indexTable.get(var10);
			int var12 = this.XMiddleArray[ci];
			int cy = this.YTopArray[ci];
			this.drawLine(this.XMiddleArray[i], thisY + this.textTipMargin, var12, cy, g2);
			this.drawTree(g2, var10);
		}

	}

	protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        render(g2);
    }

	/**
	 * arranges the layout in an recursive manner
	 */
    public void loadSentence() {
    	ConstTree t = treebank.get(sentenceNumber);
    	// deals with the empty node
    	if(t.label == null) {
    		sentenceLength = 0;
    		nodesCount = 0;
    		indexTable = null;
    		nodesArray = null;
    		lexicalsArray = null;
    		labelArray = null;
    		XLeftArray = null;
    		YArray = null;
    		XMiddleArray = null;
    		wordLengthsArray = null;
    		area.width = 0;
            area.height = 0;
    		return;
    	}
    	// initialization
		levelSize = 15.0D;
		totalWidthShift = 0;
		indexTable = new IdentityHashMap<ConstTree, Integer>();
		this.nodesCount = t.constSubTreeList().size();
		this.nodesArray = (ConstTree[])t.constSubTreeList().toArray(new ConstTree[0]);
		this.XMiddleArray = new int[this.nodesCount];
		this.YTopArray = new int[this.nodesCount];
		this.nodeLengthsArray = new int[this.nodesCount];
		this.nodeHeightsArray = new int[this.nodesCount];
		int i = 0;

		int height;
		for(height = 0; height < nodesArray.length; ++height, ++i) {
			ConstTree width = nodesArray[height];
			this.indexTable.put(width, i);
		}

		this.calculateNodeLengthsAndHeights(this.nodesArray);
		int treeWidth = this.arrangeAllTerminals(this.nodesArray) - this.leftMargin;
		this.updateValues(t, this.topMargin);
		height = this.alignVertical(t);
		this.area.width = treeWidth + this.leftMargin + this.rightMargin + totalWidthShift;
		this.area.height = height + this.bottomMargin;
    }

	private void calculateNodeLengthsAndHeights(ConstTree[] nodesArray) {
		for(int i = 0; i < nodesArray.length; ++i) {
			ConstTree n = nodesArray[i];
			int nodeLength = this.metrics.stringWidth(n.getLabel());
			if(this.displayLA) {
				for(int k = 0; k < n.getLa().length; ++k) {
					String nodeHeight = n.getLa()[k];
					if(nodeLength < this.metrics.stringWidth(nodeHeight)) {
						nodeLength = this.metrics.stringWidth(nodeHeight);
					}
				}
			}

			this.nodeLengthsArray[i] = nodeLength;
			int nodeHeight = this.fontHight + this.textTipMargin * 2;
			if(this.displayLA) {
				for(int k = 0; k < n.getLa().length; ++k) {
					nodeHeight += this.fontHight + this.laYMargin;
				}
			}

			this.nodeHeightsArray[i] = nodeHeight;
		}

	}

	private int arrangeAllTerminals(ConstTree[] nodesArray) {
		int thisXLeft = this.leftMargin;

		for(int node = 0; node < nodesArray.length; ++node) {
			ConstTree n = nodesArray[node];
			if(n.isLeaf()) {
				int i = this.indexTable.get(n);
				int columnLength = this.getColumnLengthColumn(n);
				this.XMiddleArray[i] = thisXLeft + columnLength / 2;
				thisXLeft = thisXLeft + columnLength + this.wordSpace;
			}
		}

		return thisXLeft - this.wordSpace;
	}

	private int getColumnLengthColumn(ConstTree n) {
		int nodeLength = this.nodeLengthsArray[indexTable.get(n)];

		while(n.parent != null && n.parent.getConstChildren().size() == 1) {
			n = n.parent;
			int i = indexTable.get(n);
			int length = this.nodeLengthsArray[i];
			if(length > nodeLength) {
				nodeLength = length;
			}
		}

		return nodeLength;
	}

	/**
	 * update the location of internal nodes ???
	 * @param n corresponding const-tree
	 * @param thisY starting from @param thisY position
     * @return x start position
     */
	private int updateValues(ConstTree n, int thisY) {
		int i = indexTable.get(n);
		YTopArray[i] = thisY;
		if(n.isLeaf()) {
			return XMiddleArray[i] - nodeLengthsArray[i] / 2;
		} else {
			int k = 0;
			int nc = n.getChildren().size();
			int[] cXLeft = new int[nc]; // x left list of children
			List<ConstTree> cs = n.getConstChildren();

			for(Iterator xSpan = cs.iterator(); xSpan.hasNext(); ++k) {
				ConstTree child = (ConstTree)xSpan.next();
				cXLeft[k] = this.updateValues(child, thisY +
						this.nodeHeightsArray[i] + (int)this.levelSize);
			}

			int nodeLength = this.metrics.stringWidth(n.getLabel());
			if(this.displayLA) {
				for (int m = 0; m < n.getLa().length; ++m) {
					String nodeHeight = n.getLa()[m];
					if (nodeLength < this.metrics.stringWidth(nodeHeight)) {
						nodeLength = this.metrics.stringWidth(nodeHeight);
					}
				}
			}

			int subnodeLength = cXLeft[nc - 1] +
					this.nodeLengthsArray[indexTable.get(cs.get(nc - 1))] -
					cXLeft[0];

			XMiddleArray[i] = cXLeft[0] + subnodeLength / 2;


			if(nc > 1 && nodeLength > subnodeLength) {
				int widthShift = nodeLength - subnodeLength;
				for(int p=indexTable.get(cs.get(0)); p < nodesArray.length; p++) {
					// assert: node p is in the right of node i.
					// In fact, we only need to shift the subnodes of i and all succeed leaves.
					// The modification of succeed internal nodes is not needed.
					// Even if we modify it, I won't have any effect because it will be overridden.
					XMiddleArray[p] += widthShift;
				}
				XMiddleArray[i] += widthShift;
				totalWidthShift += widthShift;
			}


			return this.XMiddleArray[i] - this.nodeLengthsArray[i] / 2;
		}
	}

	private int alignVertical(ConstTree t) {
		int depth = t.getDepth();
		int lastYDown = this.topMargin;

		for(int d = 0; d < depth; ++d) {
			List<Tree<String>> nl = t.getAtDepth(d);
			int[] LevelYTopArray = new int[nl.size()];
			int[] LevelYDownArray = new int[nl.size()];
			int k = 0;

			for(Iterator n = nl.iterator(); n.hasNext(); ++k) {
				Tree alignY = (Tree)n.next();
				int i = indexTable.get(alignY);
				LevelYTopArray[k] = this.YTopArray[i];
			}

			int var13 = this.getMaximal(LevelYTopArray) > lastYDown?this.getMaximal(LevelYTopArray):lastYDown;

			int i1;
			Tree var14;
			Iterator i;
			for(i = nl.iterator(); i.hasNext();) {
				var14 = (Tree)i.next();
				i1 = this.indexTable.get(var14);
				// align Y-top of the same depth at vertical.
				this.YTopArray[i1] = var13;
			}

			k = 0;

			for(i = nl.iterator(); i.hasNext(); ++k) {
				var14 = (Tree)i.next();
				i1 = ((Integer)this.indexTable.get(var14)).intValue();
				LevelYDownArray[k] = this.YTopArray[i1] + this.nodeHeightsArray[i1] + (int)this.levelSize;
			}

			// calculate Y-down(including levelSize) of this depth.
			lastYDown = this.getMaximal(LevelYDownArray);
		}

		return lastYDown;
	}

	private int getMaximal(int[] Array) {
		int min = Array[0];
		for(int i: Array) {
			if(i > min) {
				min = i;
			}
		}
		return min;
	}
}
