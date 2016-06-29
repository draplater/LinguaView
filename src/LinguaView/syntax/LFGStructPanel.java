package LinguaView.syntax;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import LinguaView.UIutils.Utils;
import org.w3c.dom.*;

import LinguaView.TreePanel;
/**
 * LFGStructPanel loads in one specified LFG structure at a time.
 * Then it passes it to FeatureLayoutPanel and ConstTreePanel, respectively, to arrange their layout.
 * After individual layout of c-structure and f-structure is done,
 * LFGStructPanel adjust their y-loc to make it looks better, and adds correspondences between c-structures and f-structures.
 * 
 * @author shuoyang
 *
 */
@SuppressWarnings("serial")
public class LFGStructPanel extends TreePanel<Element> {
	/**
	 * fstruct arranges layout of f-structure
	 */
	private FeatureStructure fstruct = new FeatureStructure();
	/**
	 * cstruct arranges layout of c-structure
	 */
	private ConstStructure cstruct = new ConstStructure();
	/**
	 * cfGap is the margin (on x-axis) between c-structure and f-structure
	 */
	int cfGap = 50;
	/**
	 * the left and top margin of c-structure of f-structure before adjustment of LFGStructPanel
	 */
	int cLeftMarginOrig = cstruct.leftMargin;
	int fLeftMarginOrig = fstruct.leftMargin;
	int cTopMarginOrig = cstruct.topMargin;
	int fTopMarginOrig = fstruct.topMargin;
	/**
	 * the bottom margin of comment box.
	 */
	static final int commentMargin = 5;
	/**
	 * a mapping from TSNodeLabel to the index of AVM, represents the correspondences
	 */
	Map<ConstTree, Integer> CorrespondenceTable = new HashMap<ConstTree, Integer>();
	/**
	 * indicates whether the correspondence lines should be rendered in black or magenta
	 */
	public boolean isColor = true;
	/**
	 * indicates whether the correspondence lines should be shown
	 */
	public boolean showCorrespondingLine = true;

	boolean showComment = true;
	private List<String> commentLines;
	private int commentWidth;

	private String[] edsLines;
	private int edsWidth;

	/**
	 * FeatureStructure is a wrapper class for using FeatureLayoutPanel in LFGStructPanel.
	 * Except for overridden init() method and a new SetXStartPos() method for adjustment,
	 * this class is the same as FeatureLayoutPanel
	 * 
	 * @author shuoyang
	 *
	 */
	class FeatureStructure extends FeatureLayoutPanel {

		public void init() {
			textTopMargin = 0;
			levelSizeFactor = 0.5;
			XLeftMargin = 15;
			XBoarderLineMargin = 20;
			CurlyBracketMargin = 10;
			XRightMargin = 15;
			RefLineHeight = 10;
			loadFont();
			loadSentence();
			setPreferredSize(area);
		}

		public void setXStartPos(int XStartPos) {
			fLeftMarginOrig = leftMargin;
			leftMargin += XStartPos;
			init();
		}
	}

	/**
	 * ConstStructure is a wrapper class for using ConstTreePanel in LFGStructPanel.
	 * Except for overridden init() method and a new SetXStartPos() method for adjustment,
	 * this class is the same as ConstTreePanel
	 * 
	 * @author shuoyang
	 *
	 */
	class ConstStructure extends ConstTreePanel {

		public void init() {
			loadFont();
			loadSentence();
			setPreferredSize(area);
		}

		public void setXStartPos(int XStartPos) {
			cLeftMarginOrig = leftMargin;
			leftMargin += XStartPos;
			init();
		}
	}
	
	public void loadFont() {
		font = new Font("SansSerif", Font.PLAIN, fontSize);
		metrics = getFontMetrics(font);
		fontDescendent = metrics.getDescent();
		fontHight = metrics.getHeight();
		levelSize = fontHight * levelSizeFactor;

		cstruct.fontSize = this.fontSize;
		cstruct.font = this.font;
		cstruct.metrics = this.metrics;
		cstruct.fontDescendent = this.fontDescendent;
		cstruct.fontHight = this.fontHight;
		cstruct.levelSize = this.levelSize;

		fstruct.fontSize = this.fontSize;
		fstruct.font = this.font;
		fstruct.metrics = this.metrics;
		fstruct.fontDescendent = this.fontDescendent;
		fstruct.fontHight = this.fontHight;
		fstruct.levelSize = this.levelSize;
	}

	public void init() {
		cstruct.leftMargin = cLeftMarginOrig;
		fstruct.leftMargin = fLeftMarginOrig;
		cstruct.topMargin = cTopMarginOrig;
		fstruct.topMargin = fTopMarginOrig;
		if(commentLines != null)
			commentLines.clear();
		edsLines = null;
		loadFont();
		loadSentence();
		setPreferredSize(area);
		setSize(area);
		revalidate();
		repaint();
	}

	public void toggleComment() {
		showComment = !showComment;
		repaint();
	}

	/**
	 * render the LFG structure according to the layout arranged
	 */
	@SuppressWarnings("unused")
	public void render(Graphics2D g2) {
		g2.setFont(font);
		g2.setColor(Color.BLACK);
		g2.setStroke(new BasicStroke());
		
		// adjust the y-pos of c-structure or f-structure to get their middle line
		// on the same y level
		int cSpan = cstruct.getDimension().height - cstruct.topMargin;
		int fSpan = fstruct.getDimension().height - fstruct.topMargin;
		int cYMiddle = cstruct.topMargin + cSpan / 2;
		int fYMiddle = fstruct.topMargin + fSpan / 2;
		if (cYMiddle > fYMiddle) {
			int diff = cYMiddle - fYMiddle;
			fstruct.topMargin += diff;
		}
		else {
			int diff = fYMiddle - cYMiddle;
			cstruct.topMargin += diff;
		}
		cstruct.init();
		fstruct.init();
		
		// render the c-structure and f-structure
		cstruct.render(g2);
		fstruct.render(g2);

		// Draw comments
		if(showComment && commentLines != null &&
				!commentLines.isEmpty()) {
			// Y position of comment box.
			int yPos = fstruct.getDimension().height;
			if (cstruct.getDimension().height > yPos)
				yPos = cstruct.getDimension().height;

			// draw comment box
			g2.drawRect(cstruct.leftMargin, yPos,
					commentWidth,
					fontHight * commentLines.size() + commentMargin);
			yPos += fontHight; // String is drawn in the baseline.

			for (String line : commentLines) {
				g2.drawString(line, cstruct.leftMargin, yPos);
				yPos += fontHight;
			}
		}

		// draw eds
		Map<String, Dimension> edsPositionMap = new HashMap<>();
		if(edsLines != null) {
			int xPos = fstruct.getDimension().width;
			int edsSpan = fontHight * edsLines.length;
			int yPos = cYMiddle - edsSpan / 2;
			// extract eps tags.
			for (String line : edsLines) {
				Pattern p = Pattern.compile("\\b(\\w+):");
				Matcher m = p.matcher(line);
				if(m.find()) {
					String tag = m.group(1);
					edsPositionMap.put(tag, new Dimension(xPos, yPos));
				}
				g2.drawString(line, xPos, yPos);
				yPos += fontHight;
			}
		}

		// if the correspondences should be shown and are all valid,
		// draw the correspondence lines
		if (showCorrespondingLine) {
			if(isAllRefValid()) {
				int RefLineFringe = cstruct.getDimension().height;
				int RefLineCount = 1;

				for (int i = 0; i < cstruct.nodesCount; i++) {
					if (CorrespondenceTable.get(cstruct.nodesArray[i]) != null) {
						int fID = CorrespondenceTable.get(cstruct.nodesArray[i]);
						AttributeValueMatrix headNode = fstruct.treebank.get(0);
						AttributeValueMatrix targNode = headNode
								.getSpecifiedNode(fID);
						AttributeValueMatrix realTargNode = AttributeValueMatrix
								.getRealContent(targNode);
						int j = fstruct.indexTable.get(realTargNode);
						int Xs = this.cstruct.XMiddleArray[i] + this.cstruct.nodeLengthsArray[i] / 2 + 5;
						int Ys = this.cstruct.YTopArray[i] + this.fontHight / 2;
						int Xe = fstruct.XLeftArray[j];
						int Ye = (fstruct.YUpArray[j] + fstruct.YDownArray[j]) / 2;
						drawRefLine(Xs, Ys, Xe, Ye, g2);
						RefLineCount++;
					}
				}
			}

			// Draw correspondence line from f-structure to EDS
			for(Attribute i : fstruct.positionMap.keySet()) {
				Dimension attrPosition = fstruct.positionMap.get(i);
				String[] edsLinks = i.getEdsLinks();
				if(edsLinks != null) {
					for(String link : edsLinks) {
						if(!edsPositionMap.containsKey(link))
							continue;
						Dimension edsPosition = edsPositionMap.get(link);
						drawRefLine((int) attrPosition.getWidth(),
								(int) attrPosition.getHeight(),
								(int) edsPosition.getWidth(),
								// align to the middle of the line.
								(int) edsPosition.getHeight() - fontHight / 4,
								g2);
					}
				}
			}
		}
	}

	/**
	 * split comment into lines for multiline display.
	 * @param commentList A List of comments
	 * @param lineWidth line width of comments block
     * @return a list of comment lines
     */
	private List<String> splitComment(List<String> commentList,
									  int lineWidth) {
		List<String> commentLines = new ArrayList<>();
		int prefixPadding = metrics.stringWidth(" • ");
		StringBuilder buf = new StringBuilder();
		for(String comment : commentList) {
            buf.append(" • "); // prefix of the first line
            int currentWidth = prefixPadding;
            for(int i=0; i<comment.length(); i++) {
                if(comment.charAt(i) == '\n') {
                    // create new line
                    commentLines.add(buf.toString());
                    buf.setLength(0);
                    buf.append("   "); // padding of the other line
                    currentWidth = prefixPadding;
                    continue;
                }

                int charWidth = metrics.charWidth(comment.charAt(i));
                if(currentWidth + charWidth > lineWidth) {
					// create new line
                    commentLines.add(buf.toString());
                    buf.setLength(0);
                    buf.append("   ");
                    currentWidth = prefixPadding;
                }
				// add char to this line
                buf.append(comment.charAt(i));
                currentWidth += charWidth;
            }
			// add the rest of the line
            commentLines.add(buf.toString());
            buf.setLength(0);
        }
		return commentLines;
	}

	public boolean isAllRefValid() {
		AttributeValueMatrix headNode = fstruct.treebank.get(0);
		for (int i = 0; i < cstruct.nodesCount; i++) {
			if (CorrespondenceTable.get(cstruct.nodesArray[i]) != null) {
				int fID = CorrespondenceTable.get(cstruct.nodesArray[i]);
				AttributeValueMatrix targNode = headNode
						.getSpecifiedNode(fID);
				if(targNode == null) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * wrapper of AttributeValueMatrix.isFStructValid
	 * @return
     */
	public List<AttributeValueMatrix.FStructCheckResult> isPredValid() {
		AttributeValueMatrix headNode = fstruct.treebank.get(0);
		return headNode.isFStructValid();
	}

	/**
	 * wrapper pf AttributeValueMatrix.checkGovernable
	 * @param meta
	 * @return
     */
	public List<AttributeValueMatrix.FStructCheckResult> checkGovernable(MetadataManager meta) {
		AttributeValueMatrix headNode = fstruct.treebank.get(0);
		return headNode.checkGovernable(meta);
	}

	/**
	 * wrapper of AttributeValueMatrix.checkCoherence
	 * @param meta
	 * @return
     */
	public List<String> checkCoherence(MetadataManager meta) {
		AttributeValueMatrix headNode = fstruct.treebank.get(0);
		return headNode.checkCoherence(meta);
	}

	/**
	 * wrapper of AttributeValueMatrix.checkGramFuncName
	 * @param meta
	 * @return
     */
	public List<String> checkGramFuncName(MetadataManager meta) {
		AttributeValueMatrix headNode = fstruct.treebank.get(0);
		return headNode.checkGramFuncName(meta);
	}

	/**
	 * wrapper of AttributeValueMatrix.checkFeatureName
	 * @param meta
	 * @return
	 */
	public List<String> checkFeatureName(MetadataManager meta) {
		AttributeValueMatrix headNode = fstruct.treebank.get(0);
		return headNode.checkFeatureName(meta);
	}
	
	/**
	 * draw correspondence line with arrow from (x1, y1) to (x2, y2)
	 * 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @param g2
	 */
	private void drawRefLine(int x1, int y1, int x2, int y2, Graphics2D g2) {
		if (isColor) {
			g2.setColor(Color.MAGENTA);
		}
		GeneralPath shape = new GeneralPath();
		Point p1 = new Point(x1, y1);
		Point p2 = new Point((x1 + x2) / 2, (y1 + y2) / 2);
		Point p3 = new Point(x2, y2);
		shape.moveTo(p1.x, p1.y);
		shape.curveTo(p1.x, p1.y, (p1.x + p2.x) / 2, p1.y, p2.x, p2.y);
		shape.curveTo(p2.x, p2.y, (p2.x + p3.x) / 2, p3.y, p3.x, p3.y);
		shape.moveTo(p3.x, p3.y);
		shape.closePath();
		g2.draw(shape);

		int arrowSize = g2.getFont().getSize() / 5;
		int[] arrowX = new int[4];
		int[] arrowY = new int[4];
		arrowX[0] = x2 - arrowSize;
		arrowX[1] = arrowX[0] - arrowSize;
		arrowX[2] = x2;
		arrowX[3] = arrowX[0] - arrowSize;
		arrowY[0] = y2;
		arrowY[1] = y2 - arrowSize;
		arrowY[2] = y2;
		arrowY[3] = y2 + arrowSize;
		g2.fillPolygon(arrowX, arrowY, 4);
		g2.setColor(Color.BLACK);
	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		render(g2);
	}

	/**
	 * call the loadSentence() function of c-structure and f-structure to
	 * arrange the layout of LFG structure
	 * 
	 * note that when f-structure is arranged, its x-pos needs to be shifted
	 */
	public void loadSentence() {
		
		Element headNode = treebank.get(sentenceNumber);
		NodeList children = headNode.getChildNodes();
		String constStr = new String();
		Element cstructNode = null, fstructNode = null;

		List<String> commentList = new ArrayList<>();
		if(Utils.isDebug)
			commentList.add("LinguaView is in debug mode.");

		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if(! (child instanceof Element)) {
				continue;
			}
			if (child.getNodeName().equals("cstruct")) {
				cstructNode = (Element) child;
			} else if (child.getNodeName().equals("fstruct")) {
				fstructNode = (Element) child;
			} else if (child.getNodeName().equals("eds")) {
				// load EDS
				String edsString = child.getTextContent().trim();
				edsLines = edsString.split("\n");
				edsWidth = 0;
				for(String line : edsLines) {
					int lineWidth = metrics.stringWidth(line);
					if(lineWidth > edsWidth)
						edsWidth = lineWidth;
				}
			} else if (child.getNodeName().equals("comment")) {
				// add comments
				NodeList commentItems = child.getChildNodes();
				for(int j=0; j < commentItems.getLength(); j++) {
					Node item = commentItems.item(j);
					if( (item instanceof Element) &&
							item.getNodeName().equals("item")) {
						commentList.add(item.getTextContent().trim());
					}
				}
			}
		}
		
		// load the c-structure and arrange its layout
		if (cstructNode != null) {
			children = cstructNode.getChildNodes();
		}
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Text) {
				constStr = ((Text) child).getTextContent();
			}
		}
		if (!constStr.isEmpty()) {
			ArrayList<ConstTree> cStructbank = new ArrayList<ConstTree>();
			ConstTree cStructHead = ConstTree.ConstTreeIO.ReadConstTree(constStr);
			if(cStructHead.label != null) {
				buildCorrepondenceTable(cStructHead);
			}
			cStructbank.add(cStructHead);
			cstruct.loadTreebank(cStructbank);
			cstruct.setXStartPos(0);
		}

		// load the f-structure and arrange its layout
		Dimension cArea = cstruct.getDimension();
		int fstructStartPos = 0;
		if(cArea.width != 0) {
			int cstructEndPos = cArea.width;
			fstructStartPos = cstructEndPos + cfGap;
		}
		if (fstructNode != null) {
			ArrayList<AttributeValueMatrix> fStructbank = new ArrayList<AttributeValueMatrix>();
			AttributeValueMatrix fStructHead = AttributeValueMatrix
					.parseXMLSentence(fstructNode);
			fStructbank.add(fStructHead);
			fstruct.loadTreebank(fStructbank);
			fstruct.setXStartPos(fstructStartPos);
		}

		// set the layout size
		Dimension fArea = fstruct.getDimension();
		commentWidth = cArea.width > fArea.width ? cArea.width : fArea.width - fLeftMarginOrig;
		area.width = cArea.width > fArea.width ? cArea.width : fArea.width
				+ edsWidth + leftMargin;
		commentLines = splitComment(commentList,
				commentWidth - metrics.charWidth(' '));
		// split long comment into lines
		area.height = (cArea.height > fArea.height ? cArea.height : fArea.height) +
				( commentLines.size() + 1) * fontHight;
	}

	/**
	 * parse the correspondence representation like "NP#1" and
	 * build correspondence table
	 * 
	 * @param headNode
	 */
	private void buildCorrepondenceTable(ConstTree headNode) {
		/*
		ArrayList<TSNodeLabel> L = headNode.collectAllNodes();
		for (TSNodeLabel n : L) {
			if (n.label().trim().matches("^[A-Za-z]*#[0-9]*$")) {
				String[] splittedParts = n.label.trim().split("#");
				String labelOrig = splittedParts[0];
				String idStr = splittedParts[1];
				n.label = labelOrig;
				CorrespondenceTable.put(n, Integer.parseInt(idStr));
			}
		}*/
		List L = headNode.constSubTreeList();
		Iterator var4 = L.iterator();

		while(var4.hasNext()) {
			ConstTree n = (ConstTree)var4.next();
			if(((String)n.getLabel()).trim().matches("^[A-Za-z]*#[0-9]*$")) {
				String[] splittedParts = ((String)n.getLabel()).trim().split("#");
				String labelOrig = splittedParts[0];
				String idStr = splittedParts[1];
				n.setLabel(labelOrig);
				this.CorrespondenceTable.put(n, Integer.valueOf(Integer.parseInt(idStr)));
			}
		}

	}
}