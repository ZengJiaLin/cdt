/*******************************************************************************
 * Copyright (c) 2007 Nokia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nokia - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.debug.core.breakpointactions;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.model.IBreakpoint;

/**
 * Interface implemented by plug-ins that wish to contribute breakpoint actions.
 * 
 * THIS INTERFACE IS PROVISIONAL AND WILL CHANGE IN THE FUTURE BREAKPOINT ACTION
 * CONTRIBUTIONS USING THIS INTERFACE WILL NEED TO BE REVISED TO WORK WITH
 * FUTURE VERSIONS OF CDT.
 * 
 */
public interface IBreakpointAction {

	public void execute(IBreakpoint breakpoint, IAdaptable context);

	public String getMemento();

	public void initializeFromMemento(String data);

	public String getDefaultName();

	public String getSummary();

	public String getTypeName();

	public String getIdentifier();

	public String getName();

	public void setName(String name);

}
