package org.ardulink.core.linkmanager.viaservices;

import static org.mockito.Mockito.mock;

import org.ardulink.core.convenience.LinkDelegate;
import org.ardulink.core.linkmanager.LinkConfig;

public class AlLinkWithoutArealLinkFactoryWithoutConfig extends LinkDelegate {

	public AlLinkWithoutArealLinkFactoryWithoutConfig(LinkConfig config) {
		super(mock(LinkDelegate.class));
	}

}
