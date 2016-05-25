//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package LinguaView.syntax;

import LinguaView.TreePanel;
import LinguaView.syntax.Atomic;
import LinguaView.syntax.AttributeValueMatrix;
import LinguaView.syntax.SemanticForm;
import LinguaView.syntax.SetOfAttributeValueMatrix;
import LinguaView.syntax.Value;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;

public class FeatureLayoutPanel extends TreePanel<AttributeValueMatrix> {
	int nodesCount;
	IdentityHashMap<AttributeValueMatrix, Integer> indexTable;
	AttributeValueMatrix[] nodesArray;
	int[] XLeftArray;
	int[] XBoarderLineArray;
	int[] XRightArray;
	int[] YUpArray;
	int[] YDownArray;
	int XLeftMargin;
	int XBoarderLineMargin;
	int CurlyBracketMargin;
	int XRightMargin;
	int RefLineMargin;
	int RefLineHeight;
	HashMap<Value, Integer> YFeatureTable = new HashMap();
	ArrayList<AttributeValueMatrix> RefList = new ArrayList();

	public FeatureLayoutPanel() {
	}

	public void init() {
		this.loadFont();
		this.loadSentence();
		this.setPreferredSize(this.area);
		this.revalidate();
		this.repaint();
	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		this.render(g2);
	}

	public void render(Graphics2D g2) {
		g2.setFont(this.font);
		g2.setColor(Color.BLACK);
		g2.setStroke(new BasicStroke());

		label81:
		for(int i = 0; i < this.nodesCount; ++i) {
			AttributeValueMatrix avm;
			int YPos;
			int var19;
			if(this.containedInRefList(this.nodesArray[i])) {
				avm = AttributeValueMatrix.getRealContent(this.nodesArray[i]);
				int var15 = ((Integer)this.indexTable.get(this.nodesArray[i])).intValue();
				int var16 = ((Integer)this.indexTable.get(avm)).intValue();
				int var17 = this.XRightArray[var15];
				int var18 = (this.YUpArray[var15] + this.YDownArray[var15]) / 2;
				YPos = this.XRightArray[var16];
				var19 = (this.YUpArray[var16] + this.YDownArray[var16]) / 2;
				this.drawRefLine(YPos, var19, var17, var18, this.RefLineHeight, g2);
			} else if(this.YUpArray[i] != 0) {
				avm = this.nodesArray[i];
				Set Keys = avm.getAllAttributeNames();
				if(!this.containedInRefList(avm)) {
					this.drawLeftSquareBracket(this.XLeftArray[i], this.YUpArray[i], this.YDownArray[i], g2);
					this.drawRightSquareBracket(this.XRightArray[i], this.YUpArray[i], this.YDownArray[i], g2);
				}

				Iterator x2 = Keys.iterator();

				while(true) {
					while(true) {
						if(!x2.hasNext()) {
							continue label81;
						}

						String Key = (String)x2.next();
						Object Val = avm.getAttributeValue(Key);
						YPos = ((Integer)this.YFeatureTable.get(Val)).intValue();
						if(Val instanceof Atomic) {
							YPos += this.fontHight;
							g2.drawString(Key, this.XLeftArray[i] + this.XLeftMargin, YPos);
							g2.drawString(((Atomic)Val).getValue(), this.XBoarderLineArray[i], YPos);
						} else {
							int YDownPos;
							if(Val instanceof SemanticForm) {
								YPos += this.fontHight;
								String var20 = "\'";
								var20 = var20 + ((SemanticForm)Val).getPred();
								var20 = var20 + " <";
								String[] var23;
								int var22 = (var23 = ((SemanticForm)Val).getStringArgs()).length;

								for(YDownPos = 0; YDownPos < var22; ++YDownPos) {
									String var21 = var23[YDownPos];
									var20 = var20 + var21;
									var20 = var20 + ", ";
								}

								if(var20.lastIndexOf(", ") != -1) {
									var20 = var20.substring(0, var20.lastIndexOf(", "));
								}

								var20 = var20 + ">\'";
								g2.drawString(Key, this.XLeftArray[i] + this.XLeftMargin, YPos);
								g2.drawString(var20, this.XBoarderLineArray[i], YPos);
							} else if(Val instanceof AttributeValueMatrix) {
								if(!this.containedInRefList(Val)) {
									Val = AttributeValueMatrix.getRealContent((AttributeValueMatrix)Val);
								}

								var19 = ((Integer)this.indexTable.get((AttributeValueMatrix)Val)).intValue();
								YPos = (this.YUpArray[var19] + this.YDownArray[var19]) / 2;
								g2.drawString(Key, this.XLeftArray[i] + this.XLeftMargin, YPos);
							} else if(Val instanceof SetOfAttributeValueMatrix) {
								Set avmset = ((SetOfAttributeValueMatrix)Val).getSet();
								int XRightPos = 0;
								YDownPos = 0;
								Iterator var13 = avmset.iterator();

								while(var13.hasNext()) {
									AttributeValueMatrix e = (AttributeValueMatrix)var13.next();
									e = AttributeValueMatrix.getRealContent(e);
									int j = ((Integer)this.indexTable.get(e)).intValue();
									if(this.YDownArray[j] > YDownPos) {
										YDownPos = this.YDownArray[j];
									}

									if(this.XRightArray[j] > XRightPos) {
										XRightPos = this.XRightArray[j];
									}
								}

								YDownPos = (int)((double)YDownPos + this.levelSize);
								g2.drawString(Key, this.XLeftArray[i] + this.XLeftMargin, (YPos + YDownPos) / 2);
								this.drawLeftCurlyBracket(this.XBoarderLineArray[i], YPos, (int)((double)YDownPos - this.levelSize), g2);
								this.drawRightCurlyBracket(XRightPos + this.CurlyBracketMargin, YPos, (int)((double)YDownPos - this.levelSize), g2);
							}
						}
					}
				}
			}
		}

	}

	private boolean containedInRefList(Object avm) {
		Iterator var3 = this.RefList.iterator();

		while(var3.hasNext()) {
			AttributeValueMatrix e = (AttributeValueMatrix)var3.next();
			if(avm == e) {
				return true;
			}
		}

		return false;
	}

	private void drawLeftSquareBracket(int x, int y1, int y2, Graphics2D g2) {
		Font g2font = g2.getFont();
		int tailLength = g2font.getSize() / 2;
		this.drawSquareBracket(x, x + tailLength, y1, y2, g2);
	}

	private void drawRightSquareBracket(int x, int y1, int y2, Graphics2D g2) {
		Font g2font = g2.getFont();
		int tailLength = g2font.getSize() / 2;
		this.drawSquareBracket(x, x - tailLength, y1, y2, g2);
	}

	private void drawSquareBracket(int x1, int x2, int y1, int y2, Graphics2D g2) {
		g2.drawLine(x1, y1, x1, y2);
		g2.drawLine(x1, y1, x2, y1);
		g2.drawLine(x1, y2, x2, y2);
	}

	private void drawLeftCurlyBracket(int x, int y1, int y2, Graphics2D g2) {
		Font g2font = g2.getFont();
		int d = (int)((double)g2font.getSize() * 0.618D);
		g2.drawArc(x, y1 + d / 2, d, d, 90, 90);
		g2.drawArc(x - d, (y1 + y2) / 2 - d / 2, d, d, -90, 90);
		g2.drawArc(x - d, (y1 + y2) / 2 + d / 2, d, d, 0, 90);
		g2.drawArc(x, y2 - d / 2, d, d, 180, 90);
		g2.drawLine(x, y1 + d, x, (y1 + y2) / 2 + 1);
		g2.drawLine(x, y2, x, (y1 + y2) / 2 + d - 1);
	}

	private void drawRightCurlyBracket(int x, int y1, int y2, Graphics2D g2) {
		Font g2font = g2.getFont();
		int d = (int)((double)g2font.getSize() * 0.618D);
		g2.drawArc(x - d, y1 + d / 2, d, d, 0, 90);
		g2.drawArc(x, (y1 + y2) / 2 - d / 2, d, d, 180, 90);
		g2.drawArc(x, (y1 + y2) / 2 + d / 2, d, d, 90, 90);
		g2.drawArc(x - d, y2 - d / 2, d, d, 270, 90);
		g2.drawLine(x, y1 + d, x, (y1 + y2) / 2 + 1);
		g2.drawLine(x, y2, x, (y1 + y2) / 2 + d - 1);
	}

	private void drawRefLine(int x1, int y1, int x2, int y2, int height, Graphics2D g2) {
		int x = x1 > x2?x1 + height:x2 + height;
		GeneralPath shape = new GeneralPath();
		Point p1 = new Point(x1, y1);
		Point p2 = new Point(x, y1);
		Point p3 = new Point(x, y2);
		Point p4 = new Point(x2, y2);
		shape.moveTo((float)p1.x, (float)p1.y);
		shape.curveTo((float)p2.x, (float)p2.y, (float)p2.x, (float)p2.y, (float)p2.x, (float)(p2.y + (p3.y - p2.y) / 2));
		shape.curveTo((float)p3.x, (float)p3.y, (float)p3.x, (float)p3.y, (float)p4.x, (float)p4.y);
		shape.moveTo((float)p3.x, (float)p3.y);
		shape.closePath();
		g2.draw(shape);
	}

	public void loadSentence() {
		AttributeValueMatrix headNode = (AttributeValueMatrix)this.treebank.get(this.sentenceNumber);
		this.RefList = headNode.getRefList();
		this.indexTable = new IdentityHashMap();
		this.nodesCount = headNode.countAllNodes();
		Collection nodesList = headNode.collectAllNodes();
		this.nodesArray = new AttributeValueMatrix[nodesList.size()];
		nodesList.toArray(this.nodesArray);

		int Width;
		for(Width = 0; Width < this.nodesCount; ++Width) {
			AttributeValueMatrix Height = this.nodesArray[Width];
			this.indexTable.put(Height, Integer.valueOf(Width));
		}

		this.XLeftArray = new int[this.nodesCount];
		this.XBoarderLineArray = new int[this.nodesCount];
		this.XRightArray = new int[this.nodesCount];
		this.YUpArray = new int[this.nodesCount];
		this.YDownArray = new int[this.nodesCount];
		Arrays.fill(this.XLeftArray, -1);
		Width = this.recursiveUpdateX(headNode, this.leftMargin) + this.rightMargin;
		int var5 = this.recursiveUpdateY(headNode, this.topMargin) + this.bottomMargin;
		this.area = new Dimension(Width, var5);
	}

	public int recursiveUpdateX(AttributeValueMatrix avm, int lastX) {
		AttributeValueMatrix OrigAVM = new AttributeValueMatrix();
		if(!avm.isContentOrPointer) {
			OrigAVM = avm;
			avm = AttributeValueMatrix.getRealContent(avm);
		}

		int i;
		if(this.containedInRefList(OrigAVM)) {
			i = ((Integer)this.indexTable.get(OrigAVM)).intValue();
			this.XLeftArray[i] = lastX;
			this.XBoarderLineArray[i] = lastX;
			return this.XRightArray[i] = this.XBoarderLineArray[i] + this.RefLineMargin;
		} else {
			i = ((Integer)this.indexTable.get(avm)).intValue();
			Set Keys = avm.getAllAttributeNames();
			this.XLeftArray[i] = lastX;
			int MaxAttributeNameLength = 0;
			Iterator ValueEndPos = Keys.iterator();

			while(ValueEndPos.hasNext()) {
				String MaxAttributeValuePos = (String)ValueEndPos.next();
				if(this.metrics.stringWidth(MaxAttributeValuePos) > MaxAttributeNameLength) {
					MaxAttributeNameLength = this.metrics.stringWidth(MaxAttributeValuePos);
				}
			}

			this.XBoarderLineArray[i] = this.XLeftArray[i] + MaxAttributeNameLength + this.XBoarderLineMargin;
			int currentX = this.XBoarderLineArray[i];
			int var18 = currentX;
			boolean var19 = false;
			Iterator var11 = Keys.iterator();

			while(true) {
				while(var11.hasNext()) {
					String Key = (String)var11.next();
					Value Val = avm.getAttributeValue(Key);
					int var20;
					if(Val instanceof Atomic) {
						var20 = currentX + this.metrics.stringWidth(((Atomic)Val).getValue());
						if(var20 > var18) {
							var18 = var20;
						}
					} else if(!(Val instanceof SemanticForm)) {
						if(Val instanceof AttributeValueMatrix) {
							var20 = this.recursiveUpdateX((AttributeValueMatrix)Val, currentX);
							if(var20 > var18) {
								var18 = var20;
							}
						} else if(Val instanceof SetOfAttributeValueMatrix) {
							Iterator var21 = ((SetOfAttributeValueMatrix)Val).getSet().iterator();

							while(var21.hasNext()) {
								AttributeValueMatrix var22 = (AttributeValueMatrix)var21.next();
								var20 = this.recursiveUpdateX(var22, currentX + this.CurlyBracketMargin) + this.CurlyBracketMargin;
								if(var20 > var18) {
									var18 = var20;
								}
							}
						}
					} else {
						String e = "\'";
						e = e + ((SemanticForm)Val).getPred();
						e = e + " <";
						String[] var17;
						int var16 = (var17 = ((SemanticForm)Val).getStringArgs()).length;

						for(int var15 = 0; var15 < var16; ++var15) {
							String arg = var17[var15];
							e = e + arg;
							e = e + ", ";
						}

						if(e.lastIndexOf(", ") != -1) {
							e = e.substring(0, e.lastIndexOf(", "));
						}

						e = e + ">\'";
						var20 = currentX + this.metrics.stringWidth(e);
						if(var20 > var18) {
							var18 = var20;
						}
					}
				}

				this.XRightArray[i] = var18 + this.XRightMargin;
				currentX = this.XRightArray[i];
				return currentX;
			}
		}
	}

	public int recursiveUpdateY(AttributeValueMatrix avm, int lastY) {
		int currentY = (int)((double)lastY + this.levelSize);
		if(!avm.isContentOrPointer) {
			avm = AttributeValueMatrix.getRealContent(avm);
		}

		int i = ((Integer)this.indexTable.get(avm)).intValue();
		Set Keys = avm.getAllAttributeNames();
		this.YUpArray[i] = currentY;
		Iterator var7 = Keys.iterator();

		while(true) {
			while(var7.hasNext()) {
				String Key = (String)var7.next();
				Value Val = avm.getAttributeValue(Key);
				this.YFeatureTable.put(Val, Integer.valueOf(currentY));
				if(this.containedInRefList(Val)) {
					int e1 = ((Integer)this.indexTable.get(Val)).intValue();
					this.YUpArray[e1] = (int)((double)currentY + this.levelSize);
					this.YDownArray[e1] = this.YUpArray[e1] + this.fontHight;
					currentY = (int)((double)currentY + (double)this.fontHight + this.levelSize);
				} else if(!(Val instanceof Atomic) && !(Val instanceof SemanticForm)) {
					if(Val instanceof AttributeValueMatrix) {
						currentY = this.recursiveUpdateY((AttributeValueMatrix)Val, currentY);
					} else if(Val instanceof SetOfAttributeValueMatrix) {
						currentY = (int)((double)currentY + this.levelSize);

						AttributeValueMatrix e;
						for(Iterator var10 = ((SetOfAttributeValueMatrix)Val).getSet().iterator(); var10.hasNext(); currentY = this.recursiveUpdateY(e, currentY)) {
							e = (AttributeValueMatrix)var10.next();
						}

						currentY = (int)((double)currentY + this.levelSize);
					}
				} else {
					currentY = (int)((double)currentY + (double)this.fontHight + 0.5D * this.levelSize);
				}
			}

			this.YDownArray[i] = currentY;
			return currentY;
		}
	}
}
