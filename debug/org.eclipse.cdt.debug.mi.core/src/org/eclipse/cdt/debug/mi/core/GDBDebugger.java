/*
 * (c) Copyright QNX Software System Ltd. 2002.
 * All Rights Reserved.
 */
package org.eclipse.cdt.debug.mi.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.debug.core.ICDebugger;
import org.eclipse.cdt.debug.core.cdi.CDIException;
import org.eclipse.cdt.debug.core.cdi.ICDISession;
import org.eclipse.cdt.debug.mi.core.cdi.CSession;
import org.eclipse.cdt.debug.mi.core.cdi.SourceManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import sun.security.krb5.internal.crypto.e;

public class GDBDebugger implements ICDebugger {

	void initializeLibraries(ILaunchConfiguration config, CSession session) throws CDIException {
		try {
			SourceManager mgr = (SourceManager)session.getSourceManager();
			boolean autolib = config.getAttribute(IMILaunchConfigurationConstants.ATTR_AUTO_SOLIB, false);
			if (autolib) {
				mgr.setAutoSolib();
			}
			List p = config.getAttribute(IMILaunchConfigurationConstants.ATTR_SOLIB_PATH, new ArrayList(1));
			if (p.size() > 0) {
				String[] paths = (String[])p.toArray(new String[0]);
				mgr.setLibraryPaths(paths);
			}
		} catch (CoreException e) {
			throw new CDIException("Error initializing: " + e.getMessage());
		}
	}

	public ICDISession createLaunchSession(ILaunchConfiguration config, IFile exe) throws CDIException {
		try {
			String gdb = config.getAttribute(IMILaunchConfigurationConstants.ATTR_DEBUG_NAME, "gdb");
			CSession session = (CSession)MIPlugin.getDefault().createCSession(gdb, exe.getLocation().toOSString());
			initializeLibraries(config, session);
			return session;
		} catch (IOException e) {
			throw new CDIException("Error initializing: " + e.getMessage());
		} catch (MIException e) {
			throw new CDIException("Error initializing: " + e.getMessage());
		} catch (CoreException e) {
			throw new CDIException("Error initializing: " + e.getMessage());
		}
	}

	public ICDISession createAttachSession(ILaunchConfiguration config, IFile exe, int pid) throws CDIException {
		try {
			String gdb = config.getAttribute(IMILaunchConfigurationConstants.ATTR_DEBUG_NAME, "gdb");
			CSession session = (CSession)MIPlugin.getDefault().createCSession(gdb, exe.getLocation().toOSString(), pid);
			initializeLibraries(config, session);
			return session;
		} catch (IOException e) {
			throw new CDIException("Error initializing: " + e.getMessage());
		} catch (MIException e) {
			throw new CDIException("Error initializing: " + e.getMessage());
		} catch (CoreException e) {
			throw new CDIException("Error initializing: " + e.getMessage());
		}

	}

	public ICDISession createCoreSession(ILaunchConfiguration config, IFile exe, IPath corefile) throws CDIException {
		try {
			String gdb = config.getAttribute(IMILaunchConfigurationConstants.ATTR_DEBUG_NAME, "gdb");
			CSession session = (CSession)MIPlugin.getDefault().createCSession(gdb, exe.getLocation().toOSString(), corefile.toOSString());
			initializeLibraries(config, session);
			return session;
		} catch (IOException e) {
			throw new CDIException("Error initializing: " + e.getMessage());
		} catch (MIException e) {
			throw new CDIException("Error initializing: " + e.getMessage());
		} catch (CoreException e) {
			throw new CDIException("Error initializing: " + e.getMessage());
		}
	}

}
