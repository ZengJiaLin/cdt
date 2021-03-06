/*******************************************************************************
 * Copyright (c) 2011, 2014 Ericsson and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ericsson - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.debug.gdbjtag.core.dsf.gdb.service;

import java.util.Map;

import org.eclipse.cdt.debug.gdbjtag.core.GDBJtagDSFFinalLaunchSequence_7_2;
import org.eclipse.cdt.dsf.concurrent.RequestMonitorWithProgress;
import org.eclipse.cdt.dsf.concurrent.Sequence;
import org.eclipse.cdt.dsf.gdb.service.command.GDBControl_7_2;
import org.eclipse.cdt.dsf.mi.service.command.CommandFactory;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Jtag control service which selects the Jtag CompleteInitializationSequence.
 * Use for GDB >= 7.2
 * @since 8.4
 */
public class GDBJtagControl_7_2 extends GDBControl_7_2 {

	public GDBJtagControl_7_2(DsfSession session, ILaunchConfiguration config, CommandFactory factory) {
		super(session, config, factory);
	}

	@Override
	protected Sequence getCompleteInitializationSequence(Map<String, Object> attributes,
			RequestMonitorWithProgress rm) {
		return new GDBJtagDSFFinalLaunchSequence_7_2(getSession(), attributes, rm);
	}
}