/*******************************************************************************
 * Copyright (c) 2012 MD PnP Program.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package org.mdpnp.apps.gui.swing;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.mdpnp.comms.Gateway;
import org.mdpnp.comms.Identifier;
import org.mdpnp.comms.data.identifierarray.IdentifierArrayUpdate;
import org.mdpnp.comms.nomenclature.Device;

public class DevicePanelFactory {
	private DevicePanelFactory() {}
	
	private static void _findPanel(Class<? extends Device> clazz, Collection<DevicePanel> panels, Set<Class<?>> handled) {
		if(null != clazz) {
			try {
				if(Device.class.isAssignableFrom(clazz)) {
					// we have a device!
					// Do we have a gui panel?
					String pkg = DevicePanel.class.getPackage().getName();
					Class<?> guiCls;
					guiCls = Class.forName(pkg+"."+clazz.getSimpleName() + "Panel");
					if(DevicePanel.class.isAssignableFrom(guiCls) && !handled.contains(guiCls)) {
						Constructor<?> ctor = guiCls.getConstructor(new Class<?>[0]);
						panels.add((DevicePanel) ctor.newInstance());
						handled.add(guiCls);
					}
				}

			} catch (ClassNotFoundException e) {
	//			e.printStackTrace();
			} catch (NoSuchMethodException e) {
	//			e.printStackTrace();
			} catch (SecurityException e) {
	//			e.printStackTrace();
			} catch (InstantiationException e) {
	//			e.printStackTrace();
			} catch (IllegalAccessException e) {
	//			e.printStackTrace();
			} catch (IllegalArgumentException e) {
	//			e.printStackTrace();
			} catch (InvocationTargetException e) {
	//			e.printStackTrace();
			} finally {
				
			}
			_findPanel((Class<? extends Device>) clazz.getSuperclass(), panels, handled);
			for(Class<?> cls : clazz.getInterfaces()) {
				_findPanel((Class<? extends Device>) cls, panels, handled);
			}
		}
	}
	
	
	
	public static Collection<DevicePanel> findPanel(IdentifierArrayUpdate iau, Gateway gateway, String source) {
		Collection<DevicePanel> panels = new ArrayList<DevicePanel>();
		Set<Identifier> identifiers = new HashSet<Identifier>();
		identifiers.addAll(Arrays.asList(iau.getValue()));
		
		if(ElectroCardioGramPanel.supported(identifiers)) {
			panels.add(new ElectroCardioGramPanel(gateway, source));
		}
		if(PulseOximeterPanel.supported(identifiers)) {
			panels.add(new PulseOximeterPanel(gateway, source));
		}
		if(BloodPressurePanel.supported(identifiers)) {
			panels.add(new BloodPressurePanel(gateway, source));
		}
		if(VentilatorPanel.supported(identifiers)) {
			panels.add(new VentilatorPanel(gateway, source));
		}
		if(WebcamPanel.supported(identifiers)) {
			panels.add(new WebcamPanel(gateway, source));
		}
		return panels;
	}
	
	public static Collection<DevicePanel> findPanel(Class<? extends Device> clazz) {
		Collection<DevicePanel> panels = new ArrayList<DevicePanel>();
		if(null == clazz) {
			return panels;
		}
		_findPanel(clazz, panels, new HashSet<Class<?>>());
//		System.out.println("There are " + panels.size() + " panels");
		return panels;
	}
}