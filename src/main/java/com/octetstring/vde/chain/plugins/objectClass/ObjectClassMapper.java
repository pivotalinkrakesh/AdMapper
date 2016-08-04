package com.octetstring.vde.chain.plugins.objectClass;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.asn1c.core.Bool;
import com.asn1c.core.Int8;
import com.octetstring.ldapv3.Filter;
import com.octetstring.vde.Credentials;
import com.octetstring.vde.Entry;
import com.octetstring.vde.EntryChange;
import com.octetstring.vde.EntrySet;
import com.octetstring.vde.chain.Chain;
import com.octetstring.vde.chain.ChainEntrySet;
import com.octetstring.vde.chain.ChainException;
import com.octetstring.vde.chain.ChainVector;
import com.octetstring.vde.chain.Plugin;
import com.octetstring.vde.chain.PluginInit;
import com.octetstring.vde.syntax.BinarySyntax;
import com.octetstring.vde.syntax.DirectoryString;
import com.octetstring.vde.syntax.Syntax;
import com.octetstring.vde.util.ActivationUtil;
import com.octetstring.vde.util.DirectoryException;
import com.octetstring.vde.util.DirectorySchemaViolation;
import com.octetstring.vde.util.ParseFilter;
import com.octetstring.vde.util.PluginUtil;

public class ObjectClassMapper implements Plugin {
/*	private static final VDELogger logger = VDELogger
			.getLogger(ObjectClassMapper.class.getName());
*/
	public static final String build = "11.1.1.1.0";

	private static final Map<String, Integer> dirTypeMap = new HashMap(4);

	static {
	//	logger.info("OVD-20156", new Object[] { "11.1.1.1.0" });
		dirTypeMap.put("adam", Integer.valueOf(2));
		dirTypeMap.put("sunone", Integer.valueOf(0));
		dirTypeMap.put("edirectory", Integer.valueOf(3));
		dirTypeMap.put("activedirectory", Integer.valueOf(1));
	}

	private static final DirectoryString OBJECT_CLASS = new DirectoryString(
			"objectClass");

	private static final DirectoryString USER_ACCT_CONTROL = new DirectoryString(
			"userAccountControl");

	private static final String CONFIG_OC = "mapObjectClass";

	private static final String CONFIG_ATTRIBUTE = "mapAttribute";

	private static final String CONFIG_ADD_OC_ATTRIB = "addAttribute";

	private static final String CONFIG_REM_OC_ATTRIB = "filterAttribute";

	private static final String CONFIG_AUX = "filterAuxiliaryClass";

	private static final String CONFIG_REM_OC = "filterObjectClassOnModify";

	private static final String CONFIG_ACTIVATION_DIRECTORY = "directoryType";
	private static final String CONFIG_ACTIVATION_ATTRIB = "activationAttribute";
	private static final String CONFIG_ACTIVATION_VAL_ACTIVE = "activationValue";
	private static final String CONFIG_ACTIVATION_VAL_DEACTIVE = "deactivationValue";
	private Map<DirectoryString, DirectoryString> inboundMap;
	private Map<DirectoryString, DirectoryString> outboundMap;
	private Map<DirectoryString, DirectoryString> ocInboundMap;
	private Map<DirectoryString, DirectoryString> ocOutboundMap;
	private Map<DirectoryString, Vector<Syntax>> addAttributes;
	private Map<DirectoryString, Map<DirectoryString, List<Syntax>>> addOcVals;
	private Set<DirectoryString> removeAttributes;
	private Map<DirectoryString, List<DirectoryString>> remOcAttribs;
	private Set<DirectoryString> auxClass;
	private boolean removeOcOnModify;
	private ActivationUtil activationUtil;
	private PluginUtil pluginUtil;
	private int directoryType;
	private DirectoryString activationAttribute;
	private List<DirectoryString> activeValues;
	private List<DirectoryString> deactiveValues;
	
	public void add(Chain chain, Credentials creds, Entry entry, Int8 result)
			throws DirectorySchemaViolation, DirectoryException, ChainException {
		for (DirectoryString attributeType : removeAttributes) {
			entry.remove(attributeType);
		}
		
		//logger.debug("In add - entry", entry);
		
		pluginUtil.renameAdd(inboundMap, entry);
		
		//logger.debug("attribs to replace"+ inboundMap);
		
		//logger.debug("after replacing attribs", entry);

		for (Map.Entry<DirectoryString, DirectoryString> mapEntry : ocInboundMap.entrySet()) {
			pluginUtil.revalueAdd(OBJECT_CLASS, (Syntax) mapEntry.getKey(), (Syntax) mapEntry.getValue(), entry);
		}

		for (Syntax objClass : entry.get(OBJECT_CLASS)) {
			List<DirectoryString> vals = (List) remOcAttribs.get(objClass);
			if (vals != null) {
				for (DirectoryString attributeType : vals) {
					entry.remove(attributeType);
				}
			}
		}
	
		if (directoryType != -1) {
			activationUtil.activateAccount(directoryType, entry,
					activationAttribute, activeValues, deactiveValues);
		}

		addOcValues(entry);
		for (Map.Entry<DirectoryString, Vector<Syntax>> entryMap : addAttributes.entrySet()) {
			DirectoryString name = (DirectoryString) entryMap.getKey();
			if (!entry.containsKey(name))
				entry.put(name, (Vector) entryMap.getValue());
		}
		chain.nextAdd(creds, entry, result);
	}

	private void addOcValues(Entry entry) {
		Iterator<? extends Syntax> objClassIter = entry.get(OBJECT_CLASS).iterator();

		Vector<DirectoryString> extraObjectClassesToAdd = new Vector();
		DirectoryString objectClass;
		DirectoryString attributeType;
		while (objClassIter.hasNext()) {
			objectClass = (DirectoryString) objClassIter.next();
			if (auxClass.contains(objectClass)) {
				objClassIter.remove();

			} else {
				Map<DirectoryString, List<Syntax>> toAdd = (Map) addOcVals.get(objectClass);

				if (toAdd != null) {
					for (Map.Entry<DirectoryString, List<Syntax>> mapEntry : toAdd.entrySet()) {
						attributeType = (DirectoryString) mapEntry.getKey();

						if ((!entry.containsKey(attributeType))
								|| (attributeType.equals(OBJECT_CLASS))) {

							for (Syntax attrValue : (List<Syntax>) mapEntry.getValue()) {

								String sval = attrValue.toString();
								if (sval.charAt(0) == '%') {
									sval = sval.substring(1,sval.lastIndexOf('%'));
									DirectoryString fromAttr = new DirectoryString(sval);
									entry.put(attributeType,entry.get(fromAttr));

								} else if ((attributeType.equals(OBJECT_CLASS))
										&& (!objectClass.equals(attrValue))) {
									extraObjectClassesToAdd.add((DirectoryString) attrValue);
								} else {
									Vector<Syntax> values = entry.get(attributeType);
									if (values == null) {
										values = new Vector();
										entry.put(attributeType, values);
									}
									values.add(attrValue);
								}
							}
						}
					}
				}
			}
		}
		Vector<Syntax> objectClasses;
		if (extraObjectClassesToAdd.size() > 0) {
			objectClasses = entry.get(OBJECT_CLASS);
			if (objectClasses == null)
				objectClasses = new Vector();
			for (DirectoryString s : extraObjectClassesToAdd) {

				if (!objectClasses.contains(s)) {
					objectClasses.add(s);
				}
			}
		}
	}

	public void bind(Chain chain, Credentials creds, DirectoryString dn,
			BinarySyntax password, Bool result) throws DirectoryException,
			ChainException {
		writer.println("In Bind");
		chain.nextBind(creds, dn, password, result);
	}

	public void delete(Chain chain, Credentials creds, DirectoryString object,
			Int8 results) throws DirectoryException, ChainException {
		chain.nextDelete(creds, object, results);
	}

	public void get(Chain chain, Credentials creds, DirectoryString base,
			Int8 scope, Filter filter, Bool typesonly,
			Vector<DirectoryString> attributes, Vector<EntrySet> result)
			throws DirectoryException, ChainException {
		Filter clonedFilter = filter.copy();
		Vector<DirectoryString> clonedAttributes = (Vector) attributes.clone();

		pluginUtil.renameGet(inboundMap, clonedAttributes, clonedFilter);
		pluginUtil.removeFilter(clonedFilter, removeAttributes);

		for (Map.Entry<DirectoryString, DirectoryString> mapEntry : ocInboundMap.entrySet()) {
			pluginUtil.revalueGet(OBJECT_CLASS, (Syntax) mapEntry.getKey(),
					(Syntax) mapEntry.getValue(), clonedFilter);
		}

		chain.nextGet(creds, base, scope, clonedFilter, typesonly,
				clonedAttributes, result);
	}

	public void modify(Chain chain, Credentials creds, DirectoryString name,
			Vector<EntryChange> changeEntries) throws DirectoryException,
			ChainException {
		Iterator<EntryChange> entryChangeIter = changeEntries.iterator();
		while (entryChangeIter.hasNext()) {
			DirectoryString attributeType = ((EntryChange) entryChangeIter.next()).getAttr();
			if (((attributeType.equals(OBJECT_CLASS)) && (removeOcOnModify) && (directoryType != 0))
					|| (removeAttributes.contains(attributeType))) {

				entryChangeIter.remove();
			}
		}

		ChainVector chainVector = new ChainVector();
		Vector<DirectoryString> reqAttribs = new Vector(2);
		reqAttribs.add(OBJECT_CLASS);
		reqAttribs.add(USER_ACCT_CONTROL);

		Entry entry = null;
		try {
			chain.getVSI().get(chain.getRequest(), creds, name,
					new Int8((byte) 0), ParseFilter.parse("(objectClass=*)"),
					Bool.FALSE, reqAttribs, chainVector);

			if (chainVector.size() > 0) {
				EntrySet entrySet = (EntrySet) chainVector.get(0);
				if (entrySet.hasMore())
					entry = entrySet.getNext();
			}
		} finally {
			Iterator<EntrySet> i$=chainVector.iterator();
			EntrySet entrySet;
			while(i$.hasNext()){
				entrySet = i$.next();
				entrySet.cancelEntrySet();
			}
//			for (EntrySet entry : chainVector) {
//				entry.cancelEntrySet();
//			}
		}
		if (entry != null) {
			Set<DirectoryString> toRemove = new HashSet();
			for (Syntax objClass : entry.get(OBJECT_CLASS)) {
				List<DirectoryString> values = (List) remOcAttribs.get(objClass);
				if (values != null) {
					toRemove.addAll(values);
				}
			}
			entryChangeIter = changeEntries.listIterator();
			while (entryChangeIter.hasNext()) {
				DirectoryString attrib = ((EntryChange) entryChangeIter.next()).getAttr();
				if (toRemove.contains(attrib)) {
					entryChangeIter.remove();
				}
			}
		}
		pluginUtil.renameModify(inboundMap, changeEntries);

		if (directoryType != -1) {
			activationUtil.activateAccount(directoryType, changeEntries, chain,
					creds, name, activationAttribute, activeValues,
					deactiveValues, entry);
		}
		chain.nextModify(creds, name, changeEntries);
	}

	public void rename(Chain chain, Credentials creds, DirectoryString oldName,
			DirectoryString newName, DirectoryString newSuffix,
			Bool removeOldRdn, Int8 results) throws DirectoryException,
			ChainException {
		chain.nextRename(creds, oldName, newName, newSuffix, removeOldRdn,
				results);
	}

	public void postSearchEntry(Chain chain, Credentials creds,
			Vector<DirectoryString> returnAttribs, Filter filter, Int8 scope,
			DirectoryString base, Entry entry, ChainEntrySet entrySet)
			throws DirectoryException, ChainException {
		chain.nextPostSearchEntry(creds, returnAttribs, filter, scope, base,
				entry, entrySet);

		for (DirectoryString attributeType : removeAttributes) {
			entry.remove(attributeType);
		}
		for (Map.Entry<DirectoryString, DirectoryString> mapEntry : ocOutboundMap
				.entrySet()) {
			pluginUtil.revalueAdd(OBJECT_CLASS, (Syntax) mapEntry.getKey(),
					(Syntax) mapEntry.getValue(), entry);
		}

		pluginUtil.renameAdd(outboundMap, entry);
	}
	
        @Override
	public void postSearchComplete(Chain chain, ChainEntrySet es)
			throws ChainException, DirectoryException {
		chain.nextPostSearchComplete(es);
	}

	public boolean available(Chain chain, DirectoryString base) {
		return true;
	}
	PrintWriter writer = null;

	@Override
	public void init(PluginInit initParams, String name) throws ChainException {
		
		try {
			writer = new PrintWriter("pluginout3.txt", "UTF-8");
		} catch (Exception e1) {
			// TODO Auto-generated catch blockls
			e1.printStackTrace();
		} 
		for (int i = 0; i < 10; i++) {
			writer.println();
		}
		writer.println("BEGINING Object Mapper");
		writer.println("Time: " + Calendar.getInstance().getTime().toString());
		writer.println("***************************************");
		writer.flush();
		
		writer.println("init invoked...");
		
		inboundMap = new HashMap();
		outboundMap = new HashMap();

		String[] attributePairs = initParams.getVals("mapAttribute");
		
		writer.println("param[mapAttribute]");
		
		writer.println((attributePairs != null) ? Arrays.asList(attributePairs) : "### mapAttribute not provided ");
		
		fillAttributeMaps(attributePairs, inboundMap, outboundMap);

		ocInboundMap = new HashMap();
		ocOutboundMap = new HashMap();

		attributePairs = initParams.getVals("mapObjectClass");
		
		writer.println("param[mapObjectClass]");
		
		writer.println((attributePairs != null) ? Arrays.asList(attributePairs) : "### mapObjectClass not provided ");
		
		fillAttributeMaps(attributePairs, ocInboundMap, ocOutboundMap);

		addOcVals = new HashMap();
		for (String paramName : initParams.getInitParamNames()) {
			
			writer.println("paramName-" + paramName);
			
			if (paramName.startsWith("addAttribute")) {
				int hyphenIndex = paramName.indexOf('-');
				if (hyphenIndex >= 0) {

					String objClass = paramName.substring(hyphenIndex + 1);
					Map<DirectoryString, List<Syntax>> ocAttribs = new HashMap();

					addOcVals.put(new DirectoryString(objClass), ocAttribs);
					for (String paramValue : initParams.getVals(paramName)) {
						int equalsIndex = paramValue.indexOf('=');
						DirectoryString attributeType = new DirectoryString(
								paramValue.substring(0, equalsIndex));

						Syntax attributeValue = new DirectoryString(
								paramValue.substring(equalsIndex + 1));

						List<Syntax> attribVals = (List) ocAttribs
								.get(attributeType);
						if (attribVals == null) {
							attribVals = new Vector();
							ocAttribs.put(attributeType, attribVals);
						}
						attribVals.add(attributeValue);
					}
				}
			}
		}
		
		writer.println("objectClass Attributes to Add - " + addOcVals.toString());
		
		writer.flush();
		
		remOcAttribs = new HashMap();
		for (String paramName : initParams.getInitParamNames()) {
			
			writer.println("paramName " + paramName);
			
			if (paramName.startsWith("filterAttribute")) {
				int hyphenIndex = paramName.indexOf('-');
				if (hyphenIndex >= 0) {

					String objClass = paramName.substring(hyphenIndex + 1);
					List<DirectoryString> attributeList = new ArrayList();

					remOcAttribs.put(new DirectoryString(objClass),
							attributeList);
					String[] attributes = initParams.getValsWithTokenizer(
							paramName, ",");

					for (String attribute : attributes)
						attributeList.add(new DirectoryString(attribute));
				}
			}
		}
		
		writer.println("objectClass Attributes to remove" + remOcAttribs.toString());
		
		addAttributes = new HashMap();
		attributePairs = initParams.getVals("addAttribute");

		if (attributePairs != null) {
			for (String attributePair : attributePairs) {
				int equalsIndex = attributePair.indexOf('=');
				DirectoryString attrib1 = new DirectoryString(
						attributePair.substring(0, equalsIndex));

				DirectoryString attrib2 = new DirectoryString(
						attributePair.substring(equalsIndex + 1));

				Vector<Syntax> toadd = (Vector) addAttributes.get(attrib1);
				if (toadd == null) {
					toadd = new Vector();
					addAttributes.put(attrib1, toadd);
				}
				toadd.add(attrib2);
			}
		}
		
		writer.println("attributes to add" + addAttributes.toString());

		writer.flush();
		
		auxClass = new HashSet();
		String[] auxObjClasses = initParams.getValsWithTokenizer(
				"filterAuxiliaryClass", ",");
		if (auxObjClasses != null) {
			
			writer.println("filterAuxiliaryClass -" + Arrays.asList(auxObjClasses));
			
			for (String auxObjClass : auxObjClasses) {
				auxClass.add(new DirectoryString(auxObjClass));
			}
		}
		String removeObjClass = initParams.get("filterObjectClassOnModify");
		removeOcOnModify = ((removeObjClass == null) || (Boolean
				.parseBoolean(removeObjClass)));

		removeAttributes = new HashSet();
		String[] attributeTypes = initParams.getValsWithTokenizer(
				"filterAttribute", ",");

		if (attributeTypes != null) {
			for (String attributeType : attributeTypes) {
				removeAttributes.add(new DirectoryString(attributeType));
			}
		}
		
		writer.println("removeAttributes" + removeAttributes.toString());
		
		pluginUtil = PluginUtil.getInstance();
		activationUtil = new ActivationUtil();

		directoryType = -1;
		String dirType = initParams.get("directoryType");
		if (dirType == null) {
			return;
		}
		Integer dType = (Integer) dirTypeMap.get(dirType.toLowerCase());
		if (dType != null) {
			directoryType = dType.intValue();
		} else {
			//logger.warn("OVD-40096", new Object[] { dirType });
		}
		String activationAttrib = initParams.get("activationAttribute");
		if (activationAttrib == null) {
			//logger.error("OVD-60308");
			throw new ChainException(
					"If the directoryType is configured, an activation attribute is required");
		}

		activationAttribute = new DirectoryString(activationAttrib);

		String[] activationValues = initParams.getValsWithTokenizer(
				"activationValue", ",");

		if (activationValues == null) {
			//logger.error("OVD-60309");
			throw new ChainException(
					"If activation is configured, activation values are required");
		}

		activeValues = new ArrayList(activationValues.length);
		for (String activationValue : activationValues) {
			activeValues.add(new DirectoryString(activationValue));
		}
		
		String[] deactivationValues = initParams.getValsWithTokenizer("deactivationValue", ",");

		if (deactivationValues == null) {
			//logger.error("OVD-60310");
			throw new ChainException(
					"If activation is configured, deactivation values are required");
		}

		deactiveValues = new ArrayList(deactivationValues.length);

		for (String deactivationValue : deactivationValues) {
			deactiveValues.add(new DirectoryString(deactivationValue));
		}
		
		writer.println("init done");
		
	}

	private static void fillAttributeMaps(String[] attributePairs,
			Map<DirectoryString, DirectoryString> inboundMap,
			Map<DirectoryString, DirectoryString> outboundMap) {
		if (attributePairs != null) {
			for (String attributePair : attributePairs) {
				int equalsIndex = attributePair.indexOf('=');
				DirectoryString attrib1 = new DirectoryString(
						attributePair.substring(0, equalsIndex));

				DirectoryString attrib2 = new DirectoryString(
						attributePair.substring(equalsIndex + 1));

				inboundMap.put(attrib1, attrib2);
				outboundMap.put(attrib2, attrib1);
			}
		}
	}
        @Override
	public void destroy() {
	}
	
}
