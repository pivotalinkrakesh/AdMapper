package com.octetstring.vde.chain.plugins.objectClass;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.octetstring.vde.chain.ChainException;
import com.octetstring.vde.chain.PluginInit;

/**
 * @author cho922
 *
 */
public class ObjectClassMapperTest {

	private ObjectClassMapper _adMapper;
	
	private PluginInit pluginInit;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		_adMapper = new ObjectClassMapper();
		
		pluginInit = mock(PluginInit.class);
		
		String[] mapAttributeVals = {"attib1=newattrib1", "attib2=newattrib2"};
		
		when(pluginInit.getVals("mapAttribute")).thenReturn(mapAttributeVals);
		
		
		String[] mapObjectClassVals = {"class1=newclass1", "class2=newclass2"};
		
		when(pluginInit.getVals("mapObjectClass")).thenReturn(mapObjectClassVals);
		
		String[] addAttributeVals = {"ocAttrib1=[%val%, ocVal2]", "ocAttrib2=[ocVal2]"};
		
		when(pluginInit.getVals("addAttribute")).thenReturn(addAttributeVals);
		
		String[] filterAttributeVals = {};
		
		String[] initparams = {"addAttribute-objectClassToBeAdded", "filterAttribute-removeOC"};
		
		when(pluginInit.getInitParamNames()).thenReturn(initparams);
		
		String[] ocToBeAdded = {"ocAttrib1=[%val%, ocVal2]", "ocAttrib2=[ocVal2]"};
		
		when(pluginInit.getVals("addAttribute-objectClassToBeAdded")).thenReturn(ocToBeAdded);
		
		String[] removeMeVals = {"removeme1, removeme2"};
		when(pluginInit.getValsWithTokenizer("filterAttribute-removeOC", ",")).thenReturn(removeMeVals);
		
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test()  {
		
		try {
			_adMapper.init(pluginInit, "Raka Testing");
		} catch (ChainException e) {
			// TODO Auto-generated catch block
			System.out.println("Init Exception");
			e.printStackTrace();
		}
		
		fail("Not yet implemented");
	}

}
