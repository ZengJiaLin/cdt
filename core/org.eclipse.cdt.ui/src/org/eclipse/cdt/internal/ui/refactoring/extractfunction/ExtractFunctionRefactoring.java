/*******************************************************************************
 * Copyright (c) 2008 Institute for Software, HSR Hochschule fuer Technik  
 * Rapperswil, University of applied sciences and others
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html  
 *  
 * Contributors: 
 * Institute for Software - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.internal.ui.refactoring.extractfunction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionList;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializerExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IField;
import org.eclipse.cdt.core.dom.ast.IParameter;
import org.eclipse.cdt.core.dom.ast.cpp.CPPASTVisitor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConversionName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTOperatorName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateId;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPBinding;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.ui.CUIPlugin;

import org.eclipse.cdt.internal.core.dom.parser.c.CASTBinaryExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCompoundStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTExpressionList;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTExpressionStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionCallExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTIdExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTInitializerExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTQualifiedName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTReturnStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;

import org.eclipse.cdt.internal.ui.refactoring.AddDeclarationNodeToClassChange;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.Container;
import org.eclipse.cdt.internal.ui.refactoring.MethodContext;
import org.eclipse.cdt.internal.ui.refactoring.ModificationCollector;
import org.eclipse.cdt.internal.ui.refactoring.NodeContainer;
import org.eclipse.cdt.internal.ui.refactoring.MethodContext.ContextType;
import org.eclipse.cdt.internal.ui.refactoring.NodeContainer.NameInformation;
import org.eclipse.cdt.internal.ui.refactoring.utils.ASTHelper;
import org.eclipse.cdt.internal.ui.refactoring.utils.CPPASTAllVisitor;

public class ExtractFunctionRefactoring extends CRefactoring {

//  egtodo	
//	private static final String COMMA_SPACE = ", "; //$NON-NLS-1$
//	private static final String TYPENAME = "typename "; //$NON-NLS-1$
//	private static final String TEMPLATE_START = "template <"; //$NON-NLS-1$

	static final Integer NULL_INTEGER = Integer.valueOf(0);

	NodeContainer container;
	final ExtractFunctionInformation info;

	final Map<String, Integer> names;
	final Container<Integer> namesCounter;
	final Container<Integer> trailPos;
	private final Container<Integer> returnNumber;

	protected boolean hasNameResolvingForSimilarError = false;

	HashMap<String, Integer> nameTrail;

	private ExtractedFunctionConstructionHelper extractedFunctionConstructionHelper;

	public ExtractFunctionRefactoring(IFile file, ISelection selection,
			ExtractFunctionInformation info) {
		super(file, selection, null);
		this.info = info;
		name = Messages.ExtractFunctionRefactoring_ExtractFunction;
		names = new HashMap<String, Integer>();
		namesCounter = new Container<Integer>(NULL_INTEGER);
		trailPos = new Container<Integer>(NULL_INTEGER);
		returnNumber = new Container<Integer>(NULL_INTEGER);
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		SubMonitor sm = SubMonitor.convert(pm, 10);
		RefactoringStatus status = super.checkInitialConditions(sm.newChild(6));

		container = findExtractableNodes();
		sm.worked(1);

		if (isProgressMonitorCanceld(sm, initStatus))
			return initStatus;

		checkForNonExtractableStatements(container, status);
		sm.worked(1);

		if (isProgressMonitorCanceld(sm, initStatus))
			return initStatus;

		container.findAllNames();
		sm.worked(1);

		if (isProgressMonitorCanceld(sm, initStatus))
			return initStatus;

		container.getAllAfterUsedNames();
		info.setAllUsedNames(container.getUsedNamesUnique());

		if (container.size() < 1) {
			status
					.addFatalError(Messages.ExtractFunctionRefactoring_NoStmtSelected);
			sm.done();
			return status;
		}

		if (container.getAllDeclaredInScope().size() > 1) {
			status
					.addFatalError(Messages.ExtractFunctionRefactoring_TooManySelected);
		} else if (container.getAllDeclaredInScope().size() == 1) {
			info.setInScopeDeclaredVariable(container.getAllDeclaredInScope()
					.firstElement());
		}

		extractedFunctionConstructionHelper = ExtractedFunctionConstructionHelper
				.createFor(container.getNodesToWrite());

		boolean isExtractExpression = container.getNodesToWrite().get(0) instanceof IASTExpression;
		info.setExtractExpression(isExtractExpression);

		if (isExtractExpression && container.getNodesToWrite().size() > 1) {
			status
					.addFatalError(Messages.ExtractFunctionRefactoring_TooManySelected);
		}

		info.setDeclarator(getDeclaration(container.getNodesToWrite().get(0)));
		MethodContext context = findContext(container.getNodesToWrite().get(0));
		info.setMethodContext(context);
		sm.done();
		return status;
	}

	private void checkForNonExtractableStatements(NodeContainer cont,
			RefactoringStatus status) {

		NonExtractableStmtFinder vis = new NonExtractableStmtFinder();
		for (IASTNode node : cont.getNodesToWrite()) {
			node.accept(vis);
			if (vis.containsContinue()) {
				initStatus
						.addFatalError(Messages.ExtractFunctionRefactoring_Error_Continue);
				break;
			} else if (vis.containsBreak()) {
				initStatus
						.addFatalError(Messages.ExtractFunctionRefactoring_Error_Break);
				break;
			}
		}

		ReturnStatementFinder rFinder = new ReturnStatementFinder();
		for (IASTNode node : cont.getNodesToWrite()) {
			node.accept(rFinder);
			if (rFinder.containsReturn()) {
				initStatus
						.addFatalError(Messages.ExtractFunctionRefactoring_Error_Return);
				break;
			}
		}

	}

	private ICPPASTFunctionDeclarator getDeclaration(IASTNode node) {

		while (node != null && !(node instanceof IASTFunctionDefinition)) {
			node = node.getParent();
		}
		if (node != null) {
			IASTFunctionDeclarator declarator = ((IASTFunctionDefinition) node)
					.getDeclarator();
			if (declarator instanceof ICPPASTFunctionDeclarator) {
				return (ICPPASTFunctionDeclarator) declarator;
			}
		}
		return null;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = super.checkFinalConditions(pm);

		final IASTName astMethodName = new CPPASTName(info.getMethodName()
				.toCharArray());
		MethodContext context = findContext(container.getNodesToWrite().get(0));

		if (context.getType() == ContextType.METHOD) {
			ICPPASTCompositeTypeSpecifier classDeclaration = (ICPPASTCompositeTypeSpecifier) context
					.getMethodDeclaration().getParent();
			IASTSimpleDeclaration methodDeclaration = getDeclaration(astMethodName);

			if (isMethodAllreadyDefined(methodDeclaration, classDeclaration)) {
				status.addError(Messages.ExtractFunctionRefactoring_NameInUse);
				return status;
			}
		}
		for (NameInformation name : info.getAllUsedNames()) {
			if (name.isUserSetIsReturnValue()) {
				info.setReturnVariable(name);
			}

		}

		return status;
	}

	@Override
	protected void collectModifications(IProgressMonitor pm,
			ModificationCollector collector) throws CoreException,
			OperationCanceledException {
		final IASTName astMethodName = new CPPASTName(info.getMethodName()
				.toCharArray());

		MethodContext context = findContext(container.getNodesToWrite().get(0));

		// Create Declaration in Class
		if (context.getType() == ContextType.METHOD) {
			createMethodDeclaration(astMethodName, context, collector);
		}
		// Create Method Definition
		IASTNode firstNode = container.getNodesToWrite().get(0);
		IPath implPath = new Path(firstNode.getContainingFilename());
		final IFile implementationFile = ResourcesPlugin.getWorkspace()
				.getRoot().getFileForLocation(implPath);

		createMethodDefinition(astMethodName, context, firstNode,
				implementationFile, collector);

		createMethodCalls(astMethodName, implementationFile, context, collector);

	}

	private void createMethodCalls(final IASTName astMethodName,
			final IFile implementationFile, MethodContext context,
			ModificationCollector collector) throws CoreException {

		String title;
		if (context.getType() == MethodContext.ContextType.METHOD) {
			title = Messages.ExtractFunctionRefactoring_CreateMethodCall;
		} else {
			title = Messages.ExtractFunctionRefactoring_CreateFunctionCall;
		}

		IASTNode methodCall = getMethodCall(astMethodName);

		IASTNode firstNodeToWrite = container.getNodesToWrite().get(0);
		ASTRewrite rewriter = collector
				.rewriterForTranslationUnit(firstNodeToWrite
						.getTranslationUnit());
		TextEditGroup editGroup = new TextEditGroup(title);
		rewriter.replace(firstNodeToWrite, methodCall, editGroup);
		for (IASTNode node : container.getNodesToWrite()) {
			if (node != firstNodeToWrite) {
				rewriter.remove(node, editGroup);
			}
		}

		//Replace Dublicates not yet implemented
		/* 
		 * if(info.isReplaceDuplicates()){ replaceSimilar(astMethodName,
		 * changesTreeSet, implementationFile, context.getType()); }
		 */
	}

	private void createMethodDefinition(final IASTName astMethodName,
			MethodContext context, IASTNode firstNode,
			final IFile implementationFile, ModificationCollector collector) {

		IASTNode node = firstNode;
		boolean found = false;
		boolean templateFunction = false;

		while (node != null && !found) {
			node = node.getParent();
			if (node instanceof IASTFunctionDefinition) {
				found = true;
			}
		}

		if (found && node != null) {
			templateFunction = node.getParent() instanceof ICPPASTTemplateDeclaration;

			String title;
			if (context.getType() == MethodContext.ContextType.METHOD) {
				title = Messages.ExtractFunctionRefactoring_CreateMethodDef;
			} else {
				title = Messages.ExtractFunctionRefactoring_CreateFunctionDef;
			}

			ASTRewrite rewriter = collector.rewriterForTranslationUnit(node
					.getTranslationUnit());
			getMethod(astMethodName, context, rewriter, node,
					new TextEditGroup(title));

			if (templateFunction) {
				// egtodo
//				methodContent = getTemplateDeclarationString(
//						(ICPPASTTemplateDeclaration) node.getParent(),
//						getTemplateParameterNames())
//						+ methodContent;
			}
		}
	}

	private void createMethodDeclaration(final IASTName astMethodName,
			MethodContext context, ModificationCollector collector) {
		ICPPASTCompositeTypeSpecifier classDeclaration = (ICPPASTCompositeTypeSpecifier) context
				.getMethodDeclaration().getParent();

		IASTSimpleDeclaration methodDeclaration = getDeclaration(astMethodName);

		AddDeclarationNodeToClassChange.createChange(classDeclaration, info
				.getVisibility(), methodDeclaration, false, collector);
	}

//  egtodo
//	private Set<IASTName> getTemplateParameterNames() {
//		Set<IASTName> names = new TreeSet<IASTName>(new Comparator<IASTName>() {
//
//			public int compare(IASTName o1, IASTName o2) {
//				return o1.toString().compareTo(o2.toString());
//			}
//		});
//		for (NameInformation nameInfo : container.getNames()) {
//			IASTName declName = nameInfo.getDeclaration();
//			IBinding binding = declName.resolveBinding();
//			if (binding instanceof CPPVariable) {
//				CPPVariable cppVariable = (CPPVariable) binding;
//				IASTNode defNode = cppVariable.getDefinition();
//				if (defNode.getParent().getParent() instanceof IASTSimpleDeclaration) {
//					IASTSimpleDeclaration decl = (IASTSimpleDeclaration) defNode
//							.getParent().getParent();
//					if (decl.getDeclSpecifier() instanceof CPPASTNamedTypeSpecifier) {
//						CPPASTNamedTypeSpecifier namedSpecifier = (CPPASTNamedTypeSpecifier) decl
//								.getDeclSpecifier();
//						if (namedSpecifier.getName().resolveBinding() instanceof CPPTemplateTypeParameter) {
//							names.add(namedSpecifier.getName());
//						}
//					}
//				}
//
//			} else if (binding instanceof CPPParameter) {
//				CPPParameter parameter = (CPPParameter) binding;
//				IASTNode decNode = parameter.getDeclarations()[0];
//				if (decNode.getParent().getParent() instanceof ICPPASTParameterDeclaration) {
//					ICPPASTParameterDeclaration paraDecl = (ICPPASTParameterDeclaration) decNode
//							.getParent().getParent();
//					if (paraDecl.getDeclSpecifier() instanceof ICPPASTNamedTypeSpecifier) {
//						ICPPASTNamedTypeSpecifier namedDeclSpec = (ICPPASTNamedTypeSpecifier) paraDecl
//								.getDeclSpecifier();
//						if (namedDeclSpec.getName().resolveBinding() instanceof ICPPTemplateTypeParameter) {
//							names.add(namedDeclSpec.getName());
//						}
//					}
//				}
//			}
//		}
//		return names;
//	}
//
//	private String getTemplateDeclarationString(
//			ICPPASTTemplateDeclaration templateDeclaration, Set<IASTName> names) {
//		if (names.isEmpty()) {
//			return EMPTY_STRING;
//		} else {
//			StringBuffer buf = new StringBuffer();
//
//			buf.append(TEMPLATE_START);
//			for (Iterator<IASTName> it = names.iterator(); it.hasNext();) {
//				IASTName name = it.next();
//				buf.append(TYPENAME);
//				buf.append(name.toString());
//
//				if (it.hasNext()) {
//					buf.append(COMMA_SPACE);
//				}
//			}
//			buf.append('>');
//			buf.append(CRefactoring.NEWLINE);
//
//			return buf.toString();
//		}
//	}

	private boolean isMethodAllreadyDefined(
			IASTSimpleDeclaration methodDeclaration,
			ICPPASTCompositeTypeSpecifier classDeclaration) {
		TrailNodeEqualityChecker equalityChecker = new TrailNodeEqualityChecker(
				names, namesCounter);

		IBinding bind = classDeclaration.getName().resolveBinding();
		IASTStandardFunctionDeclarator declarator = (IASTStandardFunctionDeclarator) methodDeclaration
				.getDeclarators()[0];
		String name = new String(declarator.getName().toCharArray());
		if (bind instanceof ICPPClassType) {
			ICPPClassType classBind = (ICPPClassType) bind;
			try {
				IField[] fields = classBind.getFields();
				for (IField field : fields) {
					if (field.getName().equals(name)) {
						return true;
					}
				}
				ICPPMethod[] methods = classBind.getAllDeclaredMethods();
				for (ICPPMethod method : methods) {
					if (!method.takesVarArgs() && name.equals(method.getName())) {
						IParameter[] parameters = method.getParameters();
						if (parameters.length == declarator.getParameters().length) {
							for (int i = 0; i < parameters.length; i++) {
								IASTName[] origParameterName = unit
										.getDeclarationsInAST(parameters[i]);

								IASTParameterDeclaration origParameter = (IASTParameterDeclaration) origParameterName[0]
										.getParent().getParent();
								IASTParameterDeclaration newParameter = declarator
										.getParameters()[i];

								// if not the same break;
								if (!(equalityChecker.isEquals(origParameter
										.getDeclSpecifier(), newParameter
										.getDeclSpecifier()) && ASTHelper
										.samePointers(origParameter
												.getDeclarator()
												.getPointerOperators(),
												newParameter.getDeclarator()
														.getPointerOperators(),
												equalityChecker))) {
									break;
								}

								if (!(i < (parameters.length - 1))) {
									return true;
								}
							}
						}

					}
				}
				return false;
			} catch (DOMException e) {
				ILog logger = CUIPlugin.getDefault().getLog();
				IStatus status = new Status(IStatus.WARNING,
						CUIPlugin.PLUGIN_ID, IStatus.OK, e.getMessage(), e);

				logger.log(status);
			}
		}
		return true;
	}

// egtodo
//	private int calculateLength(IASTFunctionDefinition node) {
//		int diff = 0;
//		if (node.getParent() instanceof ICPPASTTemplateDeclaration) {
//			ICPPASTTemplateDeclaration tempDec = (ICPPASTTemplateDeclaration) node
//					.getParent();
//			diff = node.getFileLocation().getNodeOffset()
//					- tempDec.getFileLocation().getNodeOffset();
//		}
//
//		return diff;
//	}
//
//	private void replaceSimilar(final IASTName astMethodName,
//			final ChangeTreeSet changesTreeSet, final IFile implementationFile,
//			final ContextType contextType) {
//		// Find similar code
//		final List<IASTNode> nodesToRewriteWithoutComments = new LinkedList<IASTNode>();
//
//		for (IASTNode node : container.getNodesToWrite()) {
//			if (!(node instanceof IASTComment)) {
//				nodesToRewriteWithoutComments.add(node);
//			}
//		}
//
//		final Vector<IASTNode> initTrail = getTrail(nodesToRewriteWithoutComments);
//		final String title;
//		if (contextType == MethodContext.ContextType.METHOD) {
//			title = Messages.ExtractFunctionRefactoring_CreateMethodCall;
//		} else {
//			title = Messages.ExtractFunctionRefactoring_CreateFunctionCall;
//		}
//
//		if (!hasNameResolvingForSimilarError) {
//			unit.accept(new SimilarFinderVisitor(this, changesTreeSet,
//					initTrail, implementationFile, astMethodName,
//					nodesToRewriteWithoutComments, title));
//		}
//	}

	protected Vector<IASTNode> getTrail(List<IASTNode> stmts) {
		final Vector<IASTNode> trail = new Vector<IASTNode>();

		nameTrail = new HashMap<String, Integer>();
		final Container<Integer> trailCounter = new Container<Integer>(
				NULL_INTEGER);

		for (IASTNode node : stmts) {
			node.accept(new CPPASTAllVisitor() {
				@Override
				public int visitAll(IASTNode node) {

					if (node instanceof IASTComment) {
						// Visit Comment, but don't add them to the trail
						return super.visitAll(node);
					} else if (node instanceof IASTNamedTypeSpecifier) {
						// Skip if somewhere is a named Type Specifier
						trail.add(node);
						return PROCESS_SKIP;
					} else if (node instanceof IASTName) {
						if (node instanceof ICPPASTConversionName
								&& node instanceof ICPPASTOperatorName
								&& node instanceof ICPPASTTemplateId) {
							trail.add(node);
							return super.visitAll(node);
						}
						// Save Name Sequenz Number
						IASTName name = (IASTName) node;
						TrailName trailName = new TrailName();
						int actCount = trailCounter.getObject().intValue();
						if (nameTrail.containsKey(name.getRawSignature())) {
							Integer value = nameTrail.get(name
									.getRawSignature());
							actCount = value.intValue();
						} else {
							trailCounter.setObject(Integer
									.valueOf(++actCount));
							nameTrail.put(name.getRawSignature(),
									trailCounter.getObject());
						}
						trailName.setNameNumber(actCount);
						trailName.setRealName(name);

						if (info.getReturnVariable() != null
								&& info.getReturnVariable().getName()
										.getRawSignature().equals(
												name.getRawSignature())) {
							returnNumber.setObject(Integer
									.valueOf(actCount));
						}

						// Save type informations for the name
						IBinding bind = name.resolveBinding();
						IASTName[] declNames = name.getTranslationUnit()
								.getDeclarationsInAST(bind);
						if (declNames.length > 0) {
							IASTNode tmpNode = ASTHelper
									.getDeclarationForNode(declNames[0]);

							IBinding declbind = declNames[0]
									.resolveBinding();
							if (declbind instanceof ICPPBinding) {
								ICPPBinding cppBind = (ICPPBinding) declbind;
								try {
									trailName.setGloballyQualified(cppBind
											.isGloballyQualified());
								} catch (DOMException e) {
									ILog logger = CUIPlugin.getDefault()
											.getLog();
									IStatus status = new Status(
											IStatus.WARNING,
											CUIPlugin.PLUGIN_ID,
											IStatus.OK, e.getMessage(), e);

									logger.log(status);
								}
							}

							if (tmpNode != null) {
								trailName.setDeclaration(tmpNode);
							} else {
								hasNameResolvingForSimilarError = true;
							}
						}

						trail.add(trailName);
						return PROCESS_SKIP;
					} else {
						trail.add(node);
						return super.visitAll(node);
					}
				}
			});

		}

		return trail;
	}

	protected boolean isStatementInTrail(IASTStatement stmt,
			final Vector<IASTNode> trail) {
		final Container<Boolean> same = new Container<Boolean>(Boolean.TRUE);
		final TrailNodeEqualityChecker equalityChecker = new TrailNodeEqualityChecker(
				names, namesCounter);

		stmt.accept(new CPPASTAllVisitor() {
			@Override
			public int visitAll(IASTNode node) {

				int pos = trailPos.getObject().intValue();

				if (trail.size() <= 0 || pos >= trail.size()) {
					same.setObject(Boolean.FALSE);
					return PROCESS_ABORT;
				}

				if (node instanceof IASTComment) {
					// Visit Comment, but they are not in the trail
					return super.visitAll(node);
				}

				IASTNode trailNode = trail.get(pos);
				trailPos.setObject(Integer.valueOf(pos + 1));

				if (equalityChecker.isEquals(trailNode, node)) {
					if (node instanceof ICPPASTQualifiedName
							|| node instanceof IASTNamedTypeSpecifier) {
						return PROCESS_SKIP;
					}
					return super.visitAll(node);

				}
				same.setObject(new Boolean(false));
				return PROCESS_ABORT;
			}
		});

		return same.getObject().booleanValue();
	}

	private void getMethod(IASTName astMethodName, MethodContext context,
			ASTRewrite rewriter, IASTNode insertpoint, TextEditGroup group) {
		ICPPASTQualifiedName qname = new CPPASTQualifiedName();
		if (context.getType() == ContextType.METHOD) {
			for (int i = 0; i < (context.getMethodQName().getNames().length - 1); i++) {
				qname.addName(context.getMethodQName().getNames()[i]);
			}
		}
		qname.addName(astMethodName);

		IASTFunctionDefinition func = new CPPASTFunctionDefinition();
		func.setParent(unit);
		func.setDeclSpecifier(getReturnType());
		func.setDeclarator(extractedFunctionConstructionHelper
				.createFunctionDeclarator(qname, info.getDeclarator(), info
						.getReturnVariable(), container.getNodesToWrite(), info
						.getAllUsedNames()));

		IASTCompoundStatement compound = new CPPASTCompoundStatement();
		func.setBody(compound);

		ASTRewrite insertRW = rewriter.insertBefore(insertpoint.getParent(),
				insertpoint, func, group);
		insertRW = rewriter;

		extractedFunctionConstructionHelper.constructMethodBody(compound,
				container.getNodesToWrite(), insertRW, group);

		// Set return value
		if (info.getReturnVariable() != null) {
			IASTReturnStatement returnStmt = new CPPASTReturnStatement();
			if (info.getReturnVariable().getDeclaration().getParent() instanceof IASTExpression) {
				IASTExpression returnValue = (IASTExpression) info
						.getReturnVariable().getDeclaration().getParent();
				returnStmt.setReturnValue(returnValue);
			} else {
				IASTIdExpression expr = new CPPASTIdExpression();
				if (info.getReturnVariable().getUserSetName() == null) {
					expr.setName(newName(info.getReturnVariable().getName()));
				} else {
					expr.setName(new CPPASTName(info.getReturnVariable()
							.getUserSetName().toCharArray()));
				}
				returnStmt.setReturnValue(expr);
			}
			insertRW.insertBefore(compound, null, returnStmt, group);
		}

	}

	private IASTName newName(IASTName declaration) {
		return new CPPASTName(declaration.toCharArray());
	}

	private IASTDeclSpecifier getReturnType() {

		IASTNode firstNodeToWrite = container.getNodesToWrite().get(0);
		NameInformation returnVariable = info.getReturnVariable();

		return extractedFunctionConstructionHelper.determineReturnType(
				firstNodeToWrite, returnVariable);
	}

	protected IASTNode getMethodCall(IASTName astMethodName,
			Map<String, Integer> trailNameTable,
			Map<String, Integer> similarNameTable, NodeContainer myContainer,
			NodeContainer mySimilarContainer) {
		IASTExpressionStatement stmt = new CPPASTExpressionStatement();
		IASTFunctionCallExpression callExpression = new CPPASTFunctionCallExpression();
		IASTIdExpression idExpression = new CPPASTIdExpression();
		idExpression.setName(astMethodName);
		IASTExpressionList paramList = new CPPASTExpressionList();

		Vector<IASTName> declarations = new Vector<IASTName>();
		IASTName retName = null;
		boolean theRetName = false;

		for (NameInformation nameInfo : myContainer.getNames()) {
			Integer trailSeqNumber = trailNameTable.get(nameInfo
					.getDeclaration().getRawSignature());
			String orgName = null;
			for (Entry<String, Integer> entry : similarNameTable.entrySet()) {
				if (entry.getValue().equals(trailSeqNumber)) {
					orgName = entry.getKey();
					if (info.getReturnVariable() != null
							&& trailSeqNumber.equals(returnNumber.getObject())) {
						theRetName = true;
					}
				}
			}

			if (orgName != null) {
				boolean found = false;
				for (NameInformation simNameInfo : mySimilarContainer
						.getNames()) {
					if (orgName.equals(simNameInfo.getDeclaration()
							.getRawSignature())) {
						addAParameterIfPossible(paramList, declarations,
								simNameInfo);
						found = true;

						if (theRetName) {
							theRetName = false;
							retName = new CPPASTName(simNameInfo
									.getDeclaration().getRawSignature()
									.toCharArray());
						}
					}
				}

				if (!found) {
					// should be a field, use the old name
					IASTIdExpression expression = new CPPASTIdExpression();
					CPPASTName fieldName = new CPPASTName(orgName.toCharArray());
					expression.setName(fieldName);
					paramList.addExpression(expression);

					if (theRetName) {
						theRetName = false;
						retName = fieldName;
					}
				}
			}
		}

		callExpression.setParameterExpression(paramList);
		callExpression.setFunctionNameExpression(idExpression);

		if (info.getReturnVariable() == null) {
			return getReturnAssignment(stmt, callExpression);
		}
		return getReturnAssignment(stmt, callExpression, retName);
	}

	private IASTNode getMethodCall(IASTName astMethodName) {
		IASTExpressionStatement stmt = new CPPASTExpressionStatement();

		IASTFunctionCallExpression callExpression = new CPPASTFunctionCallExpression();
		IASTIdExpression idExpression = new CPPASTIdExpression();
		idExpression.setName(astMethodName);
		IASTExpressionList paramList = getCallParameters();
		callExpression.setParameterExpression(paramList);
		callExpression.setFunctionNameExpression(idExpression);

		if (info.getReturnVariable() == null) {
			return getReturnAssignment(stmt, callExpression);
		}
		IASTName retname = newName(info.getReturnVariable().getName());
		return getReturnAssignment(stmt, callExpression, retname);

	}

	private IASTNode getReturnAssignment(IASTExpressionStatement stmt,
			IASTFunctionCallExpression callExpression, IASTName retname) {
		if (info.getReturnVariable().equals(info.getInScopeDeclaredVariable())) {
			IASTSimpleDeclaration orgDecl = findSimpleDeclarationInParents(info
					.getReturnVariable().getDeclaration());
			IASTSimpleDeclaration decl = new CPPASTSimpleDeclaration();

			decl.setDeclSpecifier(orgDecl.getDeclSpecifier());

			IASTDeclarator declarator = new CPPASTDeclarator();

			declarator.setName(retname);

			for (IASTPointerOperator pointer : orgDecl.getDeclarators()[0]
					.getPointerOperators()) {
				declarator.addPointerOperator(pointer);
			}

			IASTInitializerExpression initializer = new CPPASTInitializerExpression();
			initializer.setExpression(callExpression);
			declarator.setInitializer(initializer);

			decl.addDeclarator(declarator);

			return decl;
		}
		IASTBinaryExpression binaryExpression = new CASTBinaryExpression();
		binaryExpression.setOperator(IASTBinaryExpression.op_assign);
		IASTIdExpression nameExpression = new CPPASTIdExpression();

		nameExpression.setName(retname);
		binaryExpression.setOperand1(nameExpression);
		binaryExpression.setOperand2(callExpression);

		return getReturnAssignment(stmt, binaryExpression);
	}

	private IASTNode getReturnAssignment(IASTExpressionStatement stmt,
			IASTExpression callExpression) {

		IASTNode node = container.getNodesToWrite().get(0);
		return extractedFunctionConstructionHelper.createReturnAssignment(node,
				stmt, callExpression);

	}

	private IASTSimpleDeclaration getDeclaration(IASTName name) {
		IASTSimpleDeclaration simpleDecl = new CPPASTSimpleDeclaration();
		simpleDecl.setParent(unit);
		IASTDeclSpecifier declSpec = getReturnType();
		simpleDecl.setDeclSpecifier(declSpec);
		IASTStandardFunctionDeclarator declarator = extractedFunctionConstructionHelper
				.createFunctionDeclarator(name, info.getDeclarator(), info
						.getReturnVariable(), container.getNodesToWrite(), info
						.getAllUsedNames());
		simpleDecl.addDeclarator(declarator);
		return simpleDecl;
	}

	private NodeContainer findExtractableNodes() {
		final NodeContainer container = new NodeContainer();
		unit.accept(new CPPASTVisitor() {
			{
				shouldVisitStatements = true;
				shouldVisitExpressions = true;
			}

			@Override
			public int visit(IASTStatement stmt) {
				if (!(stmt instanceof IASTCompoundStatement)
						&& isSelectedFile(region, stmt)) {
					container.add(stmt);
					return PROCESS_SKIP;
				}
				return super.visit(stmt);
			}

			@Override
			public int visit(IASTExpression expression) {
				if (isSelectedFile(region, expression)) {
					container.add(expression);
					return PROCESS_SKIP;
				}
				return super.visit(expression);
			}
		});
		return container;
	}

	public IASTExpressionList getCallParameters() {
		IASTExpressionList paramList = new CPPASTExpressionList();
		Vector<IASTName> declarations = new Vector<IASTName>();
		for (NameInformation nameInf : container.getNames()) {
			addAParameterIfPossible(paramList, declarations, nameInf);
		}
		return paramList;
	}

	private void addAParameterIfPossible(IASTExpressionList paramList,
			Vector<IASTName> declarations, NameInformation nameInf) {
		if (!nameInf.isDeclarationInScope()) {
			IASTName declaration = nameInf.getDeclaration();
			if (!declarations.contains(declaration)) {
				declarations.add(declaration);
				IASTIdExpression expression = new CPPASTIdExpression();
				expression.setName(newName(declaration));
				paramList.addExpression(expression);
			}
		}
	}

}
