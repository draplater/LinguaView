package LinguaView.syntax;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.sql.*;
import java.util.*;

import LinguaView.UIutils.Utils;
import fig.basic.DeepCloneable;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
/**
 * The attribute value matrix(avm in the following context) is the core structure 
 * in the implementation of LFG parser. The core of the structural design is to 
 * facilitate the unification operation between the two structures.
 * 
 * This implementation has referred to <Speech & Language Processing> by
 * D.Jurafsky and J.H.Martin, concretely 15.4--Implementing Unification.
 * 
 * @author shuoyang
 * 
 */
public class AttributeValueMatrix extends Value implements
		DeepCloneable<AttributeValueMatrix> {
	/**
	 * A map from String to Attribute objects.
	 */
	private Map<String, Attribute> Attrs = new HashMap<String, Attribute>();
	/**
	 * A map from Attribute objects to Values.
	 */
	private Map<Attribute, Value> Pairs = new HashMap<Attribute, Value>();
	/**
	 * A temporary and shared chart used to record all the AVMs seen when building
	 * a AVM structure It is cleared as soon as the building work is accomplished.
	 */
	private static Map<Integer, AttributeValueMatrix> AVMChart = new HashMap<Integer, AttributeValueMatrix>();
	/**
	 * A stable chart used to record all the AVMs under the same f-structure.
	 * Note that this is only available on root nodes.
	 */
	private Map<Integer, AttributeValueMatrix> Nodes = new HashMap<Integer, AttributeValueMatrix>();
	/**
	 * A temporary and shared chart used to record all the AVMs that are not fully defined
	 * in the input.  These AVMs will be checked a second time after the recursive process
	 * has been accomplished.  If the AVM is actually a reference to another AVM, then it will
	 * be discarded and the reference will be used instead.  If not, the AVM will be set blank.
	 */
	private static ArrayList<AttributeValueMatrix> Checklist = new ArrayList<AttributeValueMatrix>();
	/**
	 * A stable chart recording all the references to AVMs under the f-structure.
	 * Note that this is only available on root nodes.
	 */
	private ArrayList<AttributeValueMatrix> RefList = new ArrayList<AttributeValueMatrix>();

	/**
	 * An unique id of an avm structure. Please use positive integer as id in
	 * inputs only, the negative ones are occupied to dynamically add avms such
	 * as "content" or "pointer" during the recursive build-up.
	 */
	private int id = 0;
	/**
	 * Id in the XML file.
	 */
	private int realID = 0;
	/**
	 * The largest absolute value of ID ever met. This field is created in order
	 * to keep dynamically created new IDs unique.
	 */
	private static int LargestID = 0;
	/**
	 * Indicator. Indicating whether this avm is a "content" structure or
	 * "pointer" structure.
	 */
	public boolean isContentOrPointer = false;
	/**
	 * Indicator. Indicating whether this avm is on the Path of "real content".
	 * For the meaning of real content please refer to <Speech & Language
	 * Processing>.
	 */
	public boolean isRealContent = true;
	/**
	 * Indicator.  Indicating whether this avm is a root node.
	 */
	public boolean isRoot = false;
	/**
	 * store the link of EDS.
	 */
	private String[] edsLinks = null;
	
	public AttributeValueMatrix() {

	}

	/**
	 * Get the value of a certain attribute
	 * 
	 * @param AttrName
	 * @return
	 */
	public Value getAttributeValue(String AttrName) {
		return Pairs.get(Attrs.get(AttrName));
	}

	/**
	 * Get the name of all the attributes
	 * 
	 * @return
	 */
	public Set<String> getAllAttributeNames() {
		return Attrs.keySet();
	}

	/**
	 * Get the all attribute
	 *
	 * @return
	 */
	public Set<Attribute> getAllAttributes() {
		return Pairs.keySet();
	}

	/**
	 * Get the value of all attributes
	 * 
	 * @return
	 */
	public Set<Value> getAllAttributeValues() {
		return (Set<Value>) Pairs.values();
	}

	/**
	 * Get all the AttributeNodes as a set;
	 * Note that this is only available on root nodes.
	 * 
	 * @return
	 */
	public Collection<AttributeValueMatrix> collectAllNodes() {
		return Nodes.values();
	}
	
	/**
	 * Count the number of nodes.
	 * Note that this is only available on root nodes.
	 * 
	 * @return
	 */
	public int countAllNodes() {
		return Nodes.size();
	}
	
	/**
	 * Get a specified node according to its id
	 * Note that this is only available on root nodes.
	 * @param id
	 * @return
	 */
	public AttributeValueMatrix getSpecifiedNode(int id) {
		return Nodes.get(id);
	}
	
	/**
	 * Get a list of referencing avms in the f-structure
	 * Note that this is only available on root nodes.
	 */
	public ArrayList<AttributeValueMatrix> getRefList() {
		return RefList;
	}

	public int getRealID() {
		return realID;
	}

	/**
	 * Import a complete functional structure with a list of structured
	 * xmlString This is designed to facilitate importing data from files.
	 *
	 * @param lines
	 * @return
	 */
	public static AttributeValueMatrix parseXMLSentence(List<String> lines) {
		String xmlString = "";
		for (String line : lines) {
			xmlString += line;
		}
		DocumentBuilderFactory factory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
			InputStream stream = new ByteArrayInputStream(
					xmlString.getBytes("UTF-8"));
			Document doc = builder.parse(stream);
			Element docRoot = doc.getDocumentElement();
			return parseXMLSentence(docRoot);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}

	/**
	 * Import a complete functional structure with a parsed doctree node.
	 * @param e
	 * @return
	 */
	public static AttributeValueMatrix parseXMLSentence(Element e) {
		AttributeValueMatrix res = recursiveParse(e);
		
		for(AttributeValueMatrix avm: Checklist) {
			Attribute contentAttr = avm.Attrs.get("content");
			Attribute pointerAttr = avm.Attrs.get("pointer");
			if(AVMChart.get(avm.id) != null) {
				avm.Pairs.put(pointerAttr, AVMChart.get(avm.id));
				AttributeValueMatrix content = (AttributeValueMatrix) avm.Pairs.get(contentAttr);
				content.isRealContent = false;
				avm.Pairs.put(contentAttr, content);
				avm.id = -(++LargestID);
				res.RefList.add(avm);
			}
			AttributeValueMatrix content = (AttributeValueMatrix) avm.Pairs.get(contentAttr);
			AttributeValueMatrix pointer = (AttributeValueMatrix) avm.Pairs.get(pointerAttr);
			AVMChart.put(avm.id, avm);
			AVMChart.put(content.id, content);
			AVMChart.put(pointer.id, pointer);
		}
		
		res.isRoot = true;
		res.Nodes.putAll(AVMChart);
		Checklist.clear();
		AVMChart.clear();
		return res;
	}
	
	/**
	 * recursively parse the xmlString and build AVM structures from an Element
	 * object
	 * 
	 * @param e
	 * @return
	 */
	private static AttributeValueMatrix recursiveParse(Element e) {
		AttributeValueMatrix res = new AttributeValueMatrix();
		res.id = res.realID = Integer.parseInt(e.getAttribute("id"));

		String edsAttr = e.getAttribute("eds_link");

		LargestID = res.id > LargestID ? res.id : LargestID;
		AttributeValueMatrix content = new AttributeValueMatrix();
		content.isContentOrPointer = true;
		content.isRealContent = true;
		content.id = -(++LargestID);
		content.realID = res.id;
		if(edsAttr != null && ! edsAttr.trim().isEmpty()) {
			content.setEdsLinks(edsAttr.split(","));
		}
		AttributeValueMatrix pointer = new AttributeValueMatrix();
		pointer.isContentOrPointer = true;
		pointer.isRealContent = false;
		pointer.id = -(++LargestID);
		pointer.realID = res.id;
		NodeList children = e.getChildNodes();
		content.Attrs = new HashMap<String, Attribute>();
		boolean childExists = false;
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				// assert: child.tag is "attr"
				childExists = true;
				String attrName = ((Element) child).getAttribute("name");
				String attrValType = ((Element) child)
						.getAttribute("valtype");
				String attrEDSLink = ((Element) child)
						.getAttribute("eds_link");
				Attribute Attr = new Attribute(attrName);
				if (attrEDSLink != null &&
						!attrEDSLink.trim().isEmpty()) {
					Attr.setEdsLinks(attrEDSLink.split(","));
				}
				if (attrValType.trim().equals("fstruct")) {
					AttributeValueMatrix val = null;
					NodeList grandChildren = ((Element) child)
							.getChildNodes();
					for (int j = 0; j < grandChildren.getLength(); j++) {
						if (grandChildren.item(j) instanceof Element) {
							val = recursiveParse((Element) grandChildren
									.item(j));
							break;
						}
					}
					content.Pairs.put(Attr, val);
				}
				else if (attrValType.trim().equals("set")) {
					AttributeValueMatrix val = new AttributeValueMatrix();
					SetOfAttributeValueMatrix savm = new SetOfAttributeValueMatrix();
					NodeList fstructNodes = child.getChildNodes();
					for (int j = 0; j < fstructNodes.getLength(); j++) {
						if (fstructNodes.item(j) instanceof Element) {
							val = recursiveParse((Element) fstructNodes
									.item(j));
							savm.addAVM(val);
						}
					}
					content.Pairs.put(Attr, savm);
				}
				else if (attrValType.trim().equals("sem")) {
					SemanticForm sem = new SemanticForm();
					Text TextNode = null;
					NodeList grandChildren = ((Element) child)
							.getChildNodes();
					for (int j = 0; j < grandChildren.getLength(); j++) {
						if (grandChildren.item(j) instanceof Text) {
							TextNode = (Text) grandChildren.item(j);
							break;
						}
					}
					String text = TextNode.getData().trim();
					if (text.indexOf('(') != -1) {
						sem.setPred(text.substring(text.indexOf('\'') + 1,
								text.indexOf('(')));
						String semanticRole = text.substring(text.indexOf('(') + 1,
								text.lastIndexOf(')'));
						String[] args = semanticRole.split(",");
						for (String arg : args) {
							if(!arg.trim().isEmpty())
								sem.addArgs(new Argument(arg.trim(), true));
						}
						String nonSemanticRole = text.substring(text.indexOf(')') + 1,
								text.length() - 1);
						for (String arg : nonSemanticRole.split(",")) {
							if(!arg.trim().isEmpty())
								sem.addArgs(new Argument(arg.trim(), false));
						}
					}
					else {
						if (text.indexOf('\'') != -1) {
							text = text.substring(text.indexOf('\'') + 1,
									text.lastIndexOf('\''));
							sem.setPred(text);
						}
						else {
							sem.setPred(text);
						}
					}
					content.Pairs.put(Attr, sem);
				}
				else if (attrValType.trim().equals("atomic")) {
					Text TextNode = null;
					NodeList grandChildren = ((Element) child)
							.getChildNodes();
					for (int j = 0; j < grandChildren.getLength(); j++) {
						if (grandChildren.item(j) instanceof Text) {
							TextNode = (Text) grandChildren.item(j);
							break;
						}
					}
					Atomic atom = new Atomic(TextNode.getData().trim());
					content.Pairs.put(Attr, atom);
				}
				content.Attrs.put(attrName, Attr);
			}
		}
		res.Attrs.put("content", new Attribute("content"));
		res.Attrs.put("pointer", new Attribute("pointer"));
		res.Pairs.put(res.Attrs.get("content"), content);
		res.Pairs.put(res.Attrs.get("pointer"), pointer);
		res.isContentOrPointer = false;
		res.isRealContent = true;
		if(!childExists) {
			Checklist.add(res);
		}
		else {
			AVMChart.put(res.id, res);
			AVMChart.put(content.id, content);
			AVMChart.put(pointer.id, pointer);
		}
		return res;
	}

	/**
	 * unify two avms Please note that this unification is destructive. That is,
	 * the original copy of f1 and f2 will be modified. The return value only
	 * tells whether the unification is successful or not.
	 * 
	 * @param f1
	 * @param f2
	 * @return
	 */
	public static boolean unify(AttributeValueMatrix f1, AttributeValueMatrix f2) {
		if (f1.isContentOrPointer || f2.isContentOrPointer) {
			return false;
		}
		else {
			AttributeValueMatrix f1_real = getRealContent(f1);
			AttributeValueMatrix f2_real = getRealContent(f2);
			if (f1_real == null) {
				f1.Pairs.put(f1.Attrs.get("pointer"), f2);
				Value f1_content = f1.Pairs.get(f1.Attrs.get("content"));
				if (f1_content instanceof AttributeValueMatrix) {
					((AttributeValueMatrix) f1_content).isRealContent = false;
					f1.Pairs.put(f1.Attrs.get("content"), f1_content);
				}
				return true;
			}
			else if (f2_real == null) {
				f2.Pairs.put(f2.Attrs.get("pointer"), f1);
				Value f2_content = f2.Pairs.get(f2.Attrs.get("content"));
				if (f2_content instanceof AttributeValueMatrix) {
					((AttributeValueMatrix) f2_content).isRealContent = false;
					f2.Pairs.put(f2.Attrs.get("content"), f2_content);
				}
				return true;
			}
			else if (f1_real.equals(f2_real)) {
				f1.Pairs.put(f1.Attrs.get("pointer"), f2);
				Value f1_content = f1.Pairs.get(f1.Attrs.get("content"));
				if (f1_content instanceof AttributeValueMatrix) {
					((AttributeValueMatrix) f1_content).isRealContent = false;
					f1.Pairs.put(f1.Attrs.get("content"), f1_content);
				}
				return true;
			}
			else {
				f2.Pairs.put(f2.Attrs.get("pointer"), f1);
				Value f2_content = f2.Pairs.get(f2.Attrs.get("content"));
				if (f2_content instanceof AttributeValueMatrix) {
					((AttributeValueMatrix) f2_content).isRealContent = false;
					f2.Pairs.put(f2.Attrs.get("content"), f2_content);
				}
				Attribute otherAttr;
				for (Attribute attr : f2_real.Pairs.keySet()) {
					otherAttr = f1_real.Attrs.get(attr.getName());
					Value otherFeature = f1_real.Pairs.get(otherAttr);
					Value feature = f2_real.Pairs.get(attr);
					if (feature instanceof AttributeValueMatrix) {
						if (otherFeature == null) {
							AttributeValueMatrix newFeature = new AttributeValueMatrix();
							newFeature.isContentOrPointer = false;
							newFeature.isRealContent = true;
							newFeature.id = -(++LargestID);
							if (!unify((AttributeValueMatrix) feature,
									newFeature)) {
								return false;
							}
							else {
								f1_real.Pairs.put(otherAttr, newFeature);
							}
						}
						else if (otherFeature instanceof AttributeValueMatrix) {
							if (!unify((AttributeValueMatrix) feature,
									(AttributeValueMatrix) otherFeature)) {
								return false;
							}
						}
						else {
							return false;
						}
					}
					else if (feature instanceof SetOfAttributeValueMatrix) {
						if (otherFeature == null) {
							SetOfAttributeValueMatrix newFeature = new SetOfAttributeValueMatrix();
							newFeature
									.addAll((SetOfAttributeValueMatrix) feature);
						}
						else if (otherFeature instanceof SetOfAttributeValueMatrix) {
							SetOfAttributeValueMatrix.union(
									(SetOfAttributeValueMatrix) feature,
									(SetOfAttributeValueMatrix) otherFeature);
						}
						else {
							return false;
						}
					}
					else if (feature instanceof Atomic) {
						if (otherFeature == null) {
							Atomic newFeature = new Atomic(
									((Atomic) feature).getValue());
							f1_real.Pairs.put(otherAttr, newFeature);
						}
						else if (otherFeature instanceof Atomic) {
							if (!((Atomic) feature).equals(otherFeature)) {
								return false;
							}
						}
						else {
							return false;
						}
					}
					else if (feature instanceof SemanticForm) {
						if (otherFeature == null) {
							SemanticForm newFeature = new SemanticForm(
									((SemanticForm) feature).getPred(),
									((SemanticForm) feature).getArgs());
							f1_real.Pairs.put(otherAttr, newFeature);
						}
						else if (otherFeature instanceof SemanticForm) {
							if (!((SemanticForm) feature).equals(otherFeature)) {
								return false;
							}
						}
						else {
							return false;
						}
					}
				}
				return true;
			}
		}
	}

	/**
	 * return the avm containing the "real content" of a non-content &
	 * non-pointer avm f
	 * 
	 * @param f
	 * @return
	 */
	public static AttributeValueMatrix getRealContent(AttributeValueMatrix f) {
		AttributeValueMatrix res;
		while (true) {
			if (!(f.Pairs.get(f.Attrs.get("pointer")) instanceof AttributeValueMatrix)) {
				return null;
			}
			else {
				res = (AttributeValueMatrix) f.Pairs
						.get(f.Attrs.get("pointer"));
				if (!res.isRealContent) {
					res = (AttributeValueMatrix) f.Pairs.get(f.Attrs
							.get("content"));
					if (res.isContentOrPointer && res.isRealContent) {
						return (AttributeValueMatrix) f.Pairs.get(f.Attrs.get("content"));
					}
					else {
						return null;
					}
				}
				else {
					if (res.isContentOrPointer) {
						return (AttributeValueMatrix) f.Pairs.get(f.Attrs.get("pointer"));
					}
					else {
						f = (AttributeValueMatrix) f.Pairs.get(f.Attrs.get("pointer"));
					}
				}
			}
		}
	}

	/**
	 * recursively judges whether this avm equals to the other with respect to
	 * THE CONTENT ONLY This function overrides the Object.equals() function
	 * 
	 * @param other
	 * @return
	 */
	public boolean equals(Object other) {
		AttributeValueMatrix otheravm = new AttributeValueMatrix();
		if (other instanceof AttributeValueMatrix) {
			otheravm = (AttributeValueMatrix) other;
		}
		else {
			return false;
		}

		if (isContentOrPointer && otheravm.isContentOrPointer) {
			for (Attribute key : Pairs.keySet()) {
				Attribute otherKey = new Attribute();
				if (!(otheravm.Attrs.containsKey(key.getName()))) {
					return false;
				}
				else {
					otherKey = otheravm.Attrs.get(key.getName());
				}

				if (!(otheravm.Pairs.containsKey(otherKey))) {
					return false;
				}
				else if (otheravm.Pairs.get(otherKey) instanceof Atomic) {
					if (!((Atomic) otheravm.Pairs.get(otherKey)).equals(Pairs
							.get(key))) {
						return false;
					}
				}
				else if (otheravm.Pairs.get(otherKey) instanceof SemanticForm) {
					if (!((SemanticForm) otheravm.Pairs.get(otherKey))
							.equals(Pairs.get(key))) {
						return false;
					}
				}
				else if (otheravm.Pairs.get(otherKey) instanceof AttributeValueMatrix) {
					if (!(Pairs.get(key) instanceof AttributeValueMatrix)) {
						return false;
					}
					else if (!((AttributeValueMatrix) Pairs.get(key))
							.equals(otheravm.Pairs.get(otherKey))) {
						return false;
					}
				}
				else if (otheravm.Pairs.get(otherKey) instanceof SetOfAttributeValueMatrix) {
					if (!(Pairs.get(key) instanceof SetOfAttributeValueMatrix)) {
						return false;
					}
					else if (!((SetOfAttributeValueMatrix) Pairs.get(key))
							.equals(otheravm.Pairs.get(otherKey))) {
						return false;
					}
				}
			}
			return true;
		}
		else if(!(isContentOrPointer || otheravm.isContentOrPointer)) {
			return getRealContent(this).equals(getRealContent(otheravm));
		}
		else {
			return false;
		}
	}

	/**
	 * clones the current avm
	 * 
	 * @param
	 * @return
	 */
	public AttributeValueMatrix deepClone() {
		return recursiveClone(this);
	}

	/**
	 * recursively clones an avm
	 * 
	 * @param stub
	 * @return
	 */
	private AttributeValueMatrix recursiveClone(AttributeValueMatrix stub) {
		AttributeValueMatrix res = new AttributeValueMatrix();

		Map<String, Attribute> Attrs = new HashMap<String, Attribute>();
		for (String key : stub.Attrs.keySet()) {
			Attribute cloneAttr = new Attribute(stub.Attrs.get(key).getName());
			Attrs.put(key, cloneAttr);
		}
		res.Attrs = Attrs;

		Map<Attribute, Value> Pairs = new HashMap<Attribute, Value>();
		for (Attribute attr : stub.Pairs.keySet()) {
			Attribute cloneAttr = res.Attrs.get(attr.getName());
			Value cloneVal;
			if (stub.Pairs.get(attr) instanceof AttributeValueMatrix) {
				cloneVal = recursiveClone((AttributeValueMatrix) stub.Pairs
						.get(attr));
			}
			else if (stub.Pairs.get(attr) instanceof SetOfAttributeValueMatrix) {
				cloneVal = recursiveClone((SetOfAttributeValueMatrix) stub.Pairs
						.get(attr));
			}
			else if (stub.Pairs.get(attr) instanceof SemanticForm) {
				cloneVal = recursiveClone((SemanticForm) stub.Pairs.get(attr));
			}
			else {
				cloneVal = recursiveClone((Atomic) stub.Pairs.get(attr));
			}
			Pairs.put(cloneAttr, cloneVal);
		}
		res.Pairs = Pairs;

		res.id = stub.id;
		res.isContentOrPointer = stub.isContentOrPointer;
		res.isRealContent = stub.isRealContent;
		return res;
	}

	/**
	 * clones an avm set
	 * 
	 * @param stub
	 * @return
	 */
	private SetOfAttributeValueMatrix recursiveClone(
			SetOfAttributeValueMatrix stub) {
		SetOfAttributeValueMatrix res = new SetOfAttributeValueMatrix();
		for (AttributeValueMatrix avm : stub.getSet()) {
			res.addAVM(recursiveClone(avm));
		}
		return res;
	}

	/**
	 * clones a semantic form
	 * 
	 * @param stub
	 * @return
	 */
	private SemanticForm recursiveClone(SemanticForm stub) {
		return new SemanticForm(stub.getPred(), stub.getArgs());
	}

	/**
	 * clones an atomic value
	 * 
	 * @param stub
	 * @return
	 */
	private Atomic recursiveClone(Atomic stub) {
		return new Atomic(stub.getValue());
	}

	/**
	 * Convert AVM to relational representation.
	 * @return a sqlite memory database represent this F-structure.
     */
	public Connection toRelational() {
		AttributeValueMatrix avm = this;
		if(!avm.isContentOrPointer)
			avm = getRealContent(avm);
		return toRelational(avm, null, null);
	}

	/**
	 * Internal realization of toRelational.
	 * @param target F-structure to convert.
	 * @param idMap a map storing uid of each node.
	 * @param connection sqlite memory database to store relation representation.
     * @return the same with @param connection.
     */
	private static Connection toRelational(AttributeValueMatrix target,
									Map<Value, String> idMap,
									Connection connection) {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		try {
			if(connection == null) {
				if(Utils.isDebug)
					connection = DriverManager.getConnection(String.format("jdbc:sqlite:%s.db",
							randomString()));
				else
					connection = DriverManager.getConnection("jdbc:sqlite::memory:");
				connection.setAutoCommit(false);
				Statement statement = connection.createStatement();
				statement.executeUpdate("create table tree(source_id char(32), target_id char(32), key char(32));" +
						"create table atomic(uid char(32), value char(32));" +
						"create table sem(uid char(32), pred char(32));" +
						"create table fstruct(uid char(32));" +
						"create table `set`(source_id char(32), target_id char(32));" +
						"create table arg(uid char(32), value char(32));"
				);
			}
			PreparedStatement addTree = connection.prepareStatement("insert into tree values(? ,?, ?)");
			PreparedStatement addAtomic = connection.prepareStatement("insert into atomic values(? ,?)");
			PreparedStatement addSem = connection.prepareStatement("insert into sem values(?, ?)");
			PreparedStatement addFstruct = connection.prepareStatement("insert into fstruct values(?)");
			PreparedStatement addSet = connection.prepareStatement("insert into `set` values(? ,?)");
			PreparedStatement addArg = connection.prepareStatement("insert into arg values(? ,?)");

			String targetID;
			if(!target.isRealContent) return null;
			if(idMap == null) {
				idMap = new HashMap<>();
				targetID = logToIDMap(idMap, target);
				Utils.logger.info(String.format("head: %s", targetID));
			} else {
				targetID = logToIDMap(idMap, target);
			}

			for (String key : target.getAllAttributeNames()) {
				Value val = target.getAttributeValue(key);
				if(val instanceof AttributeValueMatrix) {
					if(!((AttributeValueMatrix)val).isContentOrPointer)
						val = getRealContent((AttributeValueMatrix) val);
				}
				String valueID = logToIDMap(idMap, val);
				Utils.logger.info(String.format("tree: %s %s %s", targetID, valueID, key));
				addTree.setString(1, targetID);
				addTree.setString(2, valueID);
				addTree.setString(3, key);
				addTree.executeUpdate();
				if(val instanceof Atomic) {
					Atomic atoVal = (Atomic) val;
					Utils.logger.info(String.format("node: atomic %s %s", valueID, atoVal.getValue()));
					addAtomic.setString(1, valueID);
					addAtomic.setString(2, atoVal.getValue());
					addAtomic.executeUpdate();
				} else if(val instanceof SemanticForm) {
					SemanticForm semVal = (SemanticForm) val;
					Utils.logger.info(String.format("node: sem %s %s", valueID, semVal.getPred()));
					addSem.setString(1, valueID);
					addSem.setString(2, semVal.getPred());
					addSem.executeUpdate();
					List<String> argList = Arrays.asList(
							((SemanticForm) val).getStringArgs());
					for(String i: argList) {
						Utils.logger.info(String.format("arg: %s %s", valueID, i));
						addArg.setString(1, valueID);
						addArg.setString(2, i);
						addArg.executeUpdate();
					}
				} else if(val instanceof AttributeValueMatrix) {
					AttributeValueMatrix avmVal = (AttributeValueMatrix) val;
					Utils.logger.info(String.format("node: fstruct %s", valueID));
					addFstruct.setString(1, valueID);
					addFstruct.executeUpdate();
					toRelational(avmVal, idMap, connection);
				} else if(val instanceof SetOfAttributeValueMatrix) {
					Set<AttributeValueMatrix> avmset = ((SetOfAttributeValueMatrix) val)
							.getSet();
					for (AttributeValueMatrix i : avmset) {
						if(!i.isContentOrPointer)
							i = getRealContent(i);
						String matID = logToIDMap(idMap, i);
						Utils.logger.info(String.format("tree: %s %s %s", targetID, matID, key));
						addTree.setString(1, targetID);
						addTree.setString(2, valueID);
						addTree.setString(3, key);
						addTree.executeUpdate();
						Utils.logger.info(String.format("node: fstruct %s", matID));
						addFstruct.setString(1, matID);
						addFstruct.executeUpdate();
						toRelational(i, idMap, connection);
					}
				}
			}

			connection.commit();
			return connection;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * get the id of tree object
	 * @param idMap
	 * @param val
     * @return
     */
	private static String logToIDMap(Map<Value, String> idMap, Value val) {
		String valueID;
		if(!idMap.containsKey(val)) {
			valueID = randomString();
			idMap.put(val, valueID);
		} else {
			valueID = idMap.get(val);
		}
		return valueID;
	}

	public String[] getEdsLinks() {
		return edsLinks;
	}

	public void setEdsLinks(String[] edsLinks) {
		this.edsLinks = edsLinks;
	}

	/**
	 * A tuple represent an error of incomplete pred.
	 */
	public class FStructCheckResult {
		/**
		 * The pred which lack argument correspondence.
		 */
		public String pred;
		/**
		 * The argument whose correspondence is lacked.
		 */
		public String reason;
		FStructCheckResult(String _pred, String _reason) {
			pred = _pred;
			reason = _reason;
		}
	}


	/**
	 * Check whether the governable grammatical functions of predicate
	 * sin this F-structure is complete.
	 * @return A list represent error. If no error is found, will @return null;
     */
	public List<FStructCheckResult> isFStructValid() {
		List<FStructCheckResult> ret = new ArrayList<>();
		Connection con = toRelational();
		try {
			Statement statement = con.createStatement();
			ResultSet rs = statement.executeQuery(
					"select sem.pred, arg.value from " +
							"arg left join sem on arg.uid = sem.uid " +
							"left join tree A on target_id = sem.uid " +
							"where not exists( " +
							"select * from tree B " +
							"where A.source_id == B.source_id " +
							"and arg.value = B.key);");
			while(rs.next()) {
				ret.add(new FStructCheckResult(rs.getString(1), rs.getString(2)));
			}
			if(ret.isEmpty()) {
				return null;
			}
			return ret;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Check whether grammatical functions which is governed by predicate is governable.
	 * @return A list represent error. If no error is found, will @return null;
	 */
	public List<FStructCheckResult> checkGovernable(MetadataManager meta) {
		List<FStructCheckResult> ret = new ArrayList<>();
		Connection con = toRelational();
		try {
			Set<MetadataManager.GramFunc> gfList = meta.getGramFuncList();
			List<String> nonGovernableList = new ArrayList<>();
			for(MetadataManager.GramFunc i: gfList) {
				if(!i.isGovernable()) {
					nonGovernableList.add(i.getAbbreviate().toUpperCase());
					nonGovernableList.add(i.getFull().toUpperCase());
				}
			}
			Statement statement = con.createStatement();
			String wordListString = "\"" + Utils.join("\",\"", nonGovernableList) + "\"";
			ResultSet rs = statement.executeQuery(String.format("select sem.pred, arg.value " +
					"from arg left join sem on arg.uid = sem.uid where arg.value in (%s);",
					wordListString));
			while(rs.next()) {
				ret.add(new FStructCheckResult(rs.getString(1), rs.getString(2)));
			}
			if(ret.isEmpty()) {
				return null;
			}
			return ret;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Check whether there are redundant governable grammatical functions.
	 * @return A list represent error. If no error is found, will @return null;
	 */
	public List<String> checkCoherence(MetadataManager meta) {
		List<String> ret = new ArrayList<>();
		Connection con = toRelational();
		try {
			Set<MetadataManager.GramFunc> gfList = meta.getGramFuncList();
			List<String> governableList = new ArrayList<>();
			for(MetadataManager.GramFunc i: gfList) {
				if(i.isGovernable()) {
					governableList.add(i.getAbbreviate().toUpperCase());
					governableList.add(i.getFull().toUpperCase());
				}
			}
			String wordListString = "\"" + Utils.join("\",\"", governableList) + "\"";
			Statement statement = con.createStatement();
			// select nodes that are governable and not governed by a predicate.
			ResultSet rs = statement.executeQuery(String.format("select tree.key from tree where not exists " +
					"(select tree.key from sem left join arg on sem.uid = arg.uid where " +
					"(select B.source_id from tree B where B.target_id = sem.uid) = tree.source_id " +
					"and arg.value = tree.key) and tree.key in (%s);", wordListString));
			while(rs.next()) {
				ret.add(rs.getString(1));
			}
			if(ret.isEmpty()) {
				return null;
			}
			return ret;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Check whether the names of grammatical functions are defined.
	 * @return A list represent error. If no error is found, will @return null;
	 */
	public List<String> checkGramFuncName(MetadataManager meta) {
		List<String> ret = new ArrayList<>();
		Connection con = toRelational();
		try {
			Set<MetadataManager.GramFunc> gfList = meta.getGramFuncList();
			Set<String> gfStringList = new LinkedHashSet<>();
			for(MetadataManager.GramFunc i: gfList) {
				gfStringList.add(i.getAbbreviate().toUpperCase());
				gfStringList.add(i.getFull().toUpperCase());
			}
			String wordListString = "\"" + Utils.join("\",\"", gfStringList) + "\"";
			Statement statement = con.createStatement();
			// select fscruct nodes whose names are not in grammatical functions list.
			ResultSet rs = statement.executeQuery(String.format(
					"select key from tree where target_id in " +
							"(select * from fstruct) and " +
							"UPPER(key) not in (%s)", wordListString));
			while(rs.next()) {
				ret.add(rs.getString(1));
			}
			if(ret.isEmpty()) {
				return null;
			}
			return ret;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Check whether the names of features are defined.
	 * @return A list represent error. If no error is found, will @return null;
	 */
	public List<String> checkFeatureName(MetadataManager meta) {
		List<String> ret = new ArrayList<>();
		Connection con = toRelational();
		try {
			Set<MetadataManager.Feature> gfList = meta.getFeatureList();
			Set<String> ftStringList = new LinkedHashSet<>();
			for(MetadataManager.Feature i: gfList) {
				ftStringList.add(i.getAbbreviate().toUpperCase());
				ftStringList.add(i.getFull().toUpperCase());
			}
			String wordListString = "\"" + Utils.join("\",\"", ftStringList) + "\"";
			Statement statement = con.createStatement();
			// select atomic nodes whose names are not in feature list.
			ResultSet rs = statement.executeQuery(String.format(
					"select key from tree where target_id in " +
							"(select uid from atomic) and " +
							"UPPER(key) not in (%s)", wordListString));
			while(rs.next()) {
				ret.add(rs.getString(1));
			}
			if(ret.isEmpty()) {
				return null;
			}
			return ret;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static String randomString() {
		char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
		StringBuilder sb = new StringBuilder();
		Random random = new Random();
		for (int i = 0; i < 20; i++) {
			char c = chars[random.nextInt(chars.length)];
			sb.append(c);
		}
		return sb.toString();
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		try {
			/**
			 * example1: input written by wsun
			 */
			String filename = "wsj001_1.xml";
			BufferedReader in = new BufferedReader(new FileReader(filename));
			ArrayList<String> xmlStringList = new ArrayList<String>();
			String Line = new String();
			Line = in.readLine();
			xmlStringList.add(Line);
			Line = in.readLine();
			boolean addFlag = false;
			while (Line != null) {
				if (Line.trim().equals("<lfg>")) {
					while (Line != null
							&& !Line.trim().matches(
									"<fstruct *id *= *\"[0-9]\">")) {
						Line = in.readLine();
					}
					addFlag = true;
				}
				if (Line.trim().equals("</lfg>")) {
					addFlag = false;
				}
				if (addFlag) {
					xmlStringList.add(Line.trim());
				}
				Line = in.readLine();
			}
			in.close();
			AttributeValueMatrix avm1 = AttributeValueMatrix
					.parseXMLSentence(xmlStringList);
			AttributeValueMatrix avm2 = avm1.deepClone();
			boolean success = AttributeValueMatrix.unify(avm1, avm2);

			// /**
			// * example 2: unification on SLP Chap16, Page26
			// */
			// String xmlString1 =
			// "<?xml version=\"1.0\" encoding=\"utf-8\" ?> <fstruct id=\"1\"> <attr name=\"agreement\" valtype=\"fstruct\"> "
			// +
			// "<fstruct id=\"2\"> <attr name=\"number\" valtype=\"atomic\"> SG </attr> </fstruct> </attr> "
			// +
			// "<attr name=\"subject\" valtype=\"fstruct\"> <fstruct id=\"3\"> <attr name=\"agreement\" valtype=\"fstruct\"> "
			// +
			// "<fstruct id=\"2\"> </fstruct> </attr> </fstruct> </attr> </fstruct>";
			// String xmlString2 =
			// "<?xml version=\"1.0\" encoding=\"utf-8\" ?> <fstruct id=\"1\"> <attr name=\"subject\" valtype=\"fstruct\">"
			// +
			// "<fstruct id=\"2\"> <attr name=\"agreement\" valtype=\"fstruct\"> <fstruct id=\"3\"> <attr name=\"person\" valtype=\"atomic\"> "
			// + "3 </attr> </fstruct> </attr> </fstruct> </attr> </fstruct>";
			// AttributeValueMatrix avm1 = AttributeValueMatrix
			// .parseXMLSentence(xmlString1);
			// AttributeValueMatrix avm2 = AttributeValueMatrix
			// .parseXMLSentence(xmlString2);
			// boolean success = AttributeValueMatrix.unify(avm1, avm2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}