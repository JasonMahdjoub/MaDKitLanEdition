package com.distrimind.madkit.kernel.network;

import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import com.distrimind.madkit.kernel.KernelAddress;

public class NetworkPropertiesTests {
	@Test
	public void testAcceptedSerializedClasses()
	{
		NetworkProperties np=new NetworkProperties();
		
		AssertJUnit.assertTrue(np.isDeniedClassForSerializationUsingPatterns("org.apache.commons.collections.functors.InvokerTransformer"));
		AssertJUnit.assertTrue(np.isDeniedClassForSerializationUsingPatterns("org.apache.commons.collections.functors.InstantiateTransformer"));
		AssertJUnit.assertTrue(np.isDeniedClassForSerializationUsingPatterns("org.apache.commons.collections4.functors.InvokerTransformer"));
		AssertJUnit.assertTrue(np.isDeniedClassForSerializationUsingPatterns("org.apache.commons.collections4.functors.InstantiateTransformer"));
		AssertJUnit.assertTrue(np.isDeniedClassForSerializationUsingPatterns("org.codehaus.groovy.runtime.ConvertedClosure"));
		AssertJUnit.assertTrue(np.isDeniedClassForSerializationUsingPatterns("org.codehaus.groovy.runtime.MethodClosure"));
		AssertJUnit.assertTrue(np.isDeniedClassForSerializationUsingPatterns("org.springframework.beans.factory.ObjectFactory"));
		AssertJUnit.assertTrue(np.isDeniedClassForSerializationUsingPatterns("com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl"));
		AssertJUnit.assertFalse(np.isDeniedClassForSerializationUsingPatterns("com.distrimind.madkit.kernel.Madkit"));
		
		AssertJUnit.assertFalse(np.isDeniedClassForSerializationUsingDenyClassList(KernelAddress.class));

		AssertJUnit.assertFalse(np.isAcceptedClassForSerializationUsingPatterns("org.apache.commons.collections.functors.InvokerTransformer"));
		AssertJUnit.assertFalse(np.isAcceptedClassForSerializationUsingPatterns("org.apache.commons.collections.functors.InstantiateTransformer"));
		AssertJUnit.assertFalse(np.isAcceptedClassForSerializationUsingPatterns("org.apache.commons.collections4.functors.InvokerTransformer"));
		AssertJUnit.assertFalse(np.isAcceptedClassForSerializationUsingPatterns("org.apache.commons.collections4.functors.InstantiateTransformer"));
		AssertJUnit.assertFalse(np.isAcceptedClassForSerializationUsingPatterns("org.codehaus.groovy.runtime.ConvertedClosure"));
		AssertJUnit.assertFalse(np.isAcceptedClassForSerializationUsingPatterns("org.codehaus.groovy.runtime.MethodClosure"));
		AssertJUnit.assertFalse(np.isAcceptedClassForSerializationUsingPatterns("org.springframework.beans.factory.ObjectFactory"));
		AssertJUnit.assertFalse(np.isAcceptedClassForSerializationUsingPatterns("com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl"));
		AssertJUnit.assertTrue(np.isAcceptedClassForSerializationUsingPatterns("com.distrimind.madkit.kernel.Madkit"));
		
		AssertJUnit.assertTrue(np.isAcceptedClassForSerializationUsingAllowClassList(Long.class));
		AssertJUnit.assertTrue(np.isAcceptedClassForSerializationUsingAllowClassList(KernelAddress.class));
		AssertJUnit.assertFalse(np.isAcceptedClassForSerializationUsingAllowClassList(NetworkPropertiesTests.class));
	
	}

}
