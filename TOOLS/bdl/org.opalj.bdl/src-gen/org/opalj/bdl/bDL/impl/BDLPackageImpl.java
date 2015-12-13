/**
 */
package org.opalj.bdl.bDL.impl;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

import org.eclipse.emf.ecore.impl.EPackageImpl;

import org.opalj.bdl.bDL.BDLFactory;
import org.opalj.bdl.bDL.BDLPackage;
import org.opalj.bdl.bDL.IssueCategories;
import org.opalj.bdl.bDL.IssueCategoryElement;
import org.opalj.bdl.bDL.IssueClassElement;
import org.opalj.bdl.bDL.IssueElement;
import org.opalj.bdl.bDL.IssueKindElement;
import org.opalj.bdl.bDL.IssuePackageElement;
import org.opalj.bdl.bDL.IssueRelevanceElement;
import org.opalj.bdl.bDL.IssueSuppressComment;
import org.opalj.bdl.bDL.IssuesContainer;
import org.opalj.bdl.bDL.IssuesTitleElement;
import org.opalj.bdl.bDL.Model;
import org.opalj.bdl.bDL.ModelContainer;
import org.opalj.bdl.bDL.ParameterContainer;
import org.opalj.bdl.bDL.ParameterElement;
import org.opalj.bdl.bDL.ParameterKeyElement;
import org.opalj.bdl.bDL.ParameterKeyValueElement;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class BDLPackageImpl extends EPackageImpl implements BDLPackage
{
  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass modelEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass modelContainerEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass parameterContainerEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass parameterElementEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass parameterKeyValueElementEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass parameterKeyElementEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass issuesContainerEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass issuesTitleElementEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass issueElementEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass issueSuppressCommentEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass issueCategoryElementEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass issueKindElementEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass issueRelevanceElementEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass issuePackageElementEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass issueClassElementEClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EClass issueCategoriesEClass = null;

  /**
   * Creates an instance of the model <b>Package</b>, registered with
   * {@link org.eclipse.emf.ecore.EPackage.Registry EPackage.Registry} by the package
   * package URI value.
   * <p>Note: the correct way to create the package is via the static
   * factory method {@link #init init()}, which also performs
   * initialization of the package, or returns the registered package,
   * if one already exists.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see org.eclipse.emf.ecore.EPackage.Registry
   * @see org.opalj.bdl.bDL.BDLPackage#eNS_URI
   * @see #init()
   * @generated
   */
  private BDLPackageImpl()
  {
    super(eNS_URI, BDLFactory.eINSTANCE);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private static boolean isInited = false;

  /**
   * Creates, registers, and initializes the <b>Package</b> for this model, and for any others upon which it depends.
   * 
   * <p>This method is used to initialize {@link BDLPackage#eINSTANCE} when that field is accessed.
   * Clients should not invoke it directly. Instead, they should simply access that field to obtain the package.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #eNS_URI
   * @see #createPackageContents()
   * @see #initializePackageContents()
   * @generated
   */
  public static BDLPackage init()
  {
    if (isInited) return (BDLPackage)EPackage.Registry.INSTANCE.getEPackage(BDLPackage.eNS_URI);

    // Obtain or create and register package
    BDLPackageImpl theBDLPackage = (BDLPackageImpl)(EPackage.Registry.INSTANCE.get(eNS_URI) instanceof BDLPackageImpl ? EPackage.Registry.INSTANCE.get(eNS_URI) : new BDLPackageImpl());

    isInited = true;

    // Create package meta-data objects
    theBDLPackage.createPackageContents();

    // Initialize created meta-data
    theBDLPackage.initializePackageContents();

    // Mark meta-data to indicate it can't be changed
    theBDLPackage.freeze();

  
    // Update the registry and return the package
    EPackage.Registry.INSTANCE.put(BDLPackage.eNS_URI, theBDLPackage);
    return theBDLPackage;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getModel()
  {
    return modelEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getModelContainer()
  {
    return modelContainerEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getModelContainer_Name()
  {
    return (EAttribute)modelContainerEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getModelContainer_Parameter()
  {
    return (EReference)modelContainerEClass.getEStructuralFeatures().get(1);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getModelContainer_Issues()
  {
    return (EReference)modelContainerEClass.getEStructuralFeatures().get(2);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getParameterContainer()
  {
    return parameterContainerEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getParameterContainer_Name()
  {
    return (EAttribute)parameterContainerEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getParameterContainer_Elements()
  {
    return (EReference)parameterContainerEClass.getEStructuralFeatures().get(1);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getParameterElement()
  {
    return parameterElementEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getParameterElement_Name()
  {
    return (EAttribute)parameterElementEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getParameterKeyValueElement()
  {
    return parameterKeyValueElementEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getParameterKeyValueElement_Value()
  {
    return (EAttribute)parameterKeyValueElementEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getParameterKeyElement()
  {
    return parameterKeyElementEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getIssuesContainer()
  {
    return issuesContainerEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getIssuesTitleElement()
  {
    return issuesTitleElementEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getIssuesTitleElement_Elements()
  {
    return (EReference)issuesTitleElementEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getIssuesTitleElement_Name()
  {
    return (EAttribute)issuesTitleElementEClass.getEStructuralFeatures().get(1);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getIssueElement()
  {
    return issueElementEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getIssueElement_Name()
  {
    return (EAttribute)issueElementEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getIssueElement_Comment()
  {
    return (EReference)issueElementEClass.getEStructuralFeatures().get(1);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getIssueElement_Message()
  {
    return (EAttribute)issueElementEClass.getEStructuralFeatures().get(2);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getIssueElement_Categories()
  {
    return (EReference)issueElementEClass.getEStructuralFeatures().get(3);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getIssueElement_Kinds()
  {
    return (EReference)issueElementEClass.getEStructuralFeatures().get(4);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getIssueElement_Relevance()
  {
    return (EReference)issueElementEClass.getEStructuralFeatures().get(5);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getIssueElement_Package()
  {
    return (EReference)issueElementEClass.getEStructuralFeatures().get(6);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getIssueElement_Class()
  {
    return (EReference)issueElementEClass.getEStructuralFeatures().get(7);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getIssueSuppressComment()
  {
    return issueSuppressCommentEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getIssueSuppressComment_Value()
  {
    return (EAttribute)issueSuppressCommentEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getIssueCategoryElement()
  {
    return issueCategoryElementEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getIssueCategoryElement_Name()
  {
    return (EAttribute)issueCategoryElementEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EReference getIssueCategoryElement_Elements()
  {
    return (EReference)issueCategoryElementEClass.getEStructuralFeatures().get(1);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getIssueKindElement()
  {
    return issueKindElementEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getIssueKindElement_Name()
  {
    return (EAttribute)issueKindElementEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getIssueKindElement_Elements()
  {
    return (EAttribute)issueKindElementEClass.getEStructuralFeatures().get(1);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getIssueRelevanceElement()
  {
    return issueRelevanceElementEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getIssueRelevanceElement_Name()
  {
    return (EAttribute)issueRelevanceElementEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getIssueRelevanceElement_Relevance()
  {
    return (EAttribute)issueRelevanceElementEClass.getEStructuralFeatures().get(1);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getIssuePackageElement()
  {
    return issuePackageElementEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getIssuePackageElement_Name()
  {
    return (EAttribute)issuePackageElementEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getIssuePackageElement_Package()
  {
    return (EAttribute)issuePackageElementEClass.getEStructuralFeatures().get(1);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getIssueClassElement()
  {
    return issueClassElementEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getIssueClassElement_Name()
  {
    return (EAttribute)issueClassElementEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getIssueClassElement_Class()
  {
    return (EAttribute)issueClassElementEClass.getEStructuralFeatures().get(1);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClass getIssueCategories()
  {
    return issueCategoriesEClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getIssueCategories_Bug()
  {
    return (EAttribute)issueCategoriesEClass.getEStructuralFeatures().get(0);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getIssueCategories_Smell()
  {
    return (EAttribute)issueCategoriesEClass.getEStructuralFeatures().get(1);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getIssueCategories_Performance()
  {
    return (EAttribute)issueCategoriesEClass.getEStructuralFeatures().get(2);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EAttribute getIssueCategories_Comprehensibility()
  {
    return (EAttribute)issueCategoriesEClass.getEStructuralFeatures().get(3);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public BDLFactory getBDLFactory()
  {
    return (BDLFactory)getEFactoryInstance();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private boolean isCreated = false;

  /**
   * Creates the meta-model objects for the package.  This method is
   * guarded to have no affect on any invocation but its first.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void createPackageContents()
  {
    if (isCreated) return;
    isCreated = true;

    // Create classes and their features
    modelEClass = createEClass(MODEL);

    modelContainerEClass = createEClass(MODEL_CONTAINER);
    createEAttribute(modelContainerEClass, MODEL_CONTAINER__NAME);
    createEReference(modelContainerEClass, MODEL_CONTAINER__PARAMETER);
    createEReference(modelContainerEClass, MODEL_CONTAINER__ISSUES);

    parameterContainerEClass = createEClass(PARAMETER_CONTAINER);
    createEAttribute(parameterContainerEClass, PARAMETER_CONTAINER__NAME);
    createEReference(parameterContainerEClass, PARAMETER_CONTAINER__ELEMENTS);

    parameterElementEClass = createEClass(PARAMETER_ELEMENT);
    createEAttribute(parameterElementEClass, PARAMETER_ELEMENT__NAME);

    parameterKeyValueElementEClass = createEClass(PARAMETER_KEY_VALUE_ELEMENT);
    createEAttribute(parameterKeyValueElementEClass, PARAMETER_KEY_VALUE_ELEMENT__VALUE);

    parameterKeyElementEClass = createEClass(PARAMETER_KEY_ELEMENT);

    issuesContainerEClass = createEClass(ISSUES_CONTAINER);

    issuesTitleElementEClass = createEClass(ISSUES_TITLE_ELEMENT);
    createEReference(issuesTitleElementEClass, ISSUES_TITLE_ELEMENT__ELEMENTS);
    createEAttribute(issuesTitleElementEClass, ISSUES_TITLE_ELEMENT__NAME);

    issueElementEClass = createEClass(ISSUE_ELEMENT);
    createEAttribute(issueElementEClass, ISSUE_ELEMENT__NAME);
    createEReference(issueElementEClass, ISSUE_ELEMENT__COMMENT);
    createEAttribute(issueElementEClass, ISSUE_ELEMENT__MESSAGE);
    createEReference(issueElementEClass, ISSUE_ELEMENT__CATEGORIES);
    createEReference(issueElementEClass, ISSUE_ELEMENT__KINDS);
    createEReference(issueElementEClass, ISSUE_ELEMENT__RELEVANCE);
    createEReference(issueElementEClass, ISSUE_ELEMENT__PACKAGE);
    createEReference(issueElementEClass, ISSUE_ELEMENT__CLASS);

    issueSuppressCommentEClass = createEClass(ISSUE_SUPPRESS_COMMENT);
    createEAttribute(issueSuppressCommentEClass, ISSUE_SUPPRESS_COMMENT__VALUE);

    issueCategoryElementEClass = createEClass(ISSUE_CATEGORY_ELEMENT);
    createEAttribute(issueCategoryElementEClass, ISSUE_CATEGORY_ELEMENT__NAME);
    createEReference(issueCategoryElementEClass, ISSUE_CATEGORY_ELEMENT__ELEMENTS);

    issueKindElementEClass = createEClass(ISSUE_KIND_ELEMENT);
    createEAttribute(issueKindElementEClass, ISSUE_KIND_ELEMENT__NAME);
    createEAttribute(issueKindElementEClass, ISSUE_KIND_ELEMENT__ELEMENTS);

    issueRelevanceElementEClass = createEClass(ISSUE_RELEVANCE_ELEMENT);
    createEAttribute(issueRelevanceElementEClass, ISSUE_RELEVANCE_ELEMENT__NAME);
    createEAttribute(issueRelevanceElementEClass, ISSUE_RELEVANCE_ELEMENT__RELEVANCE);

    issuePackageElementEClass = createEClass(ISSUE_PACKAGE_ELEMENT);
    createEAttribute(issuePackageElementEClass, ISSUE_PACKAGE_ELEMENT__NAME);
    createEAttribute(issuePackageElementEClass, ISSUE_PACKAGE_ELEMENT__PACKAGE);

    issueClassElementEClass = createEClass(ISSUE_CLASS_ELEMENT);
    createEAttribute(issueClassElementEClass, ISSUE_CLASS_ELEMENT__NAME);
    createEAttribute(issueClassElementEClass, ISSUE_CLASS_ELEMENT__CLASS);

    issueCategoriesEClass = createEClass(ISSUE_CATEGORIES);
    createEAttribute(issueCategoriesEClass, ISSUE_CATEGORIES__BUG);
    createEAttribute(issueCategoriesEClass, ISSUE_CATEGORIES__SMELL);
    createEAttribute(issueCategoriesEClass, ISSUE_CATEGORIES__PERFORMANCE);
    createEAttribute(issueCategoriesEClass, ISSUE_CATEGORIES__COMPREHENSIBILITY);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private boolean isInitialized = false;

  /**
   * Complete the initialization of the package and its meta-model.  This
   * method is guarded to have no affect on any invocation but its first.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void initializePackageContents()
  {
    if (isInitialized) return;
    isInitialized = true;

    // Initialize package
    setName(eNAME);
    setNsPrefix(eNS_PREFIX);
    setNsURI(eNS_URI);

    // Create type parameters

    // Set bounds for type parameters

    // Add supertypes to classes
    modelContainerEClass.getESuperTypes().add(this.getModel());
    parameterKeyValueElementEClass.getESuperTypes().add(this.getParameterElement());
    parameterKeyElementEClass.getESuperTypes().add(this.getParameterElement());
    issuesTitleElementEClass.getESuperTypes().add(this.getIssuesContainer());

    // Initialize classes and features; add operations and parameters
    initEClass(modelEClass, Model.class, "Model", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

    initEClass(modelContainerEClass, ModelContainer.class, "ModelContainer", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getModelContainer_Name(), ecorePackage.getEString(), "name", null, 0, 1, ModelContainer.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEReference(getModelContainer_Parameter(), this.getParameterContainer(), null, "parameter", null, 0, 1, ModelContainer.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEReference(getModelContainer_Issues(), this.getIssuesContainer(), null, "issues", null, 0, 1, ModelContainer.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(parameterContainerEClass, ParameterContainer.class, "ParameterContainer", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getParameterContainer_Name(), ecorePackage.getEString(), "name", null, 0, 1, ParameterContainer.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEReference(getParameterContainer_Elements(), this.getParameterElement(), null, "elements", null, 0, -1, ParameterContainer.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(parameterElementEClass, ParameterElement.class, "ParameterElement", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getParameterElement_Name(), ecorePackage.getEString(), "name", null, 0, 1, ParameterElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(parameterKeyValueElementEClass, ParameterKeyValueElement.class, "ParameterKeyValueElement", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getParameterKeyValueElement_Value(), ecorePackage.getEString(), "value", null, 0, 1, ParameterKeyValueElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(parameterKeyElementEClass, ParameterKeyElement.class, "ParameterKeyElement", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

    initEClass(issuesContainerEClass, IssuesContainer.class, "IssuesContainer", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);

    initEClass(issuesTitleElementEClass, IssuesTitleElement.class, "IssuesTitleElement", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEReference(getIssuesTitleElement_Elements(), this.getIssueElement(), null, "elements", null, 0, -1, IssuesTitleElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEAttribute(getIssuesTitleElement_Name(), ecorePackage.getEString(), "name", null, 0, 1, IssuesTitleElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(issueElementEClass, IssueElement.class, "IssueElement", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getIssueElement_Name(), ecorePackage.getEString(), "name", null, 0, -1, IssueElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEReference(getIssueElement_Comment(), this.getIssueSuppressComment(), null, "comment", null, 0, 1, IssueElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEAttribute(getIssueElement_Message(), ecorePackage.getEString(), "message", null, 0, 1, IssueElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEReference(getIssueElement_Categories(), this.getIssueCategoryElement(), null, "categories", null, 0, 1, IssueElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEReference(getIssueElement_Kinds(), this.getIssueKindElement(), null, "kinds", null, 0, 1, IssueElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEReference(getIssueElement_Relevance(), this.getIssueRelevanceElement(), null, "relevance", null, 0, 1, IssueElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEReference(getIssueElement_Package(), this.getIssuePackageElement(), null, "package", null, 0, 1, IssueElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEReference(getIssueElement_Class(), this.getIssueClassElement(), null, "class", null, 0, 1, IssueElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(issueSuppressCommentEClass, IssueSuppressComment.class, "IssueSuppressComment", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getIssueSuppressComment_Value(), ecorePackage.getEString(), "value", null, 0, 1, IssueSuppressComment.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(issueCategoryElementEClass, IssueCategoryElement.class, "IssueCategoryElement", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getIssueCategoryElement_Name(), ecorePackage.getEString(), "name", null, 0, 1, IssueCategoryElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEReference(getIssueCategoryElement_Elements(), this.getIssueCategories(), null, "elements", null, 0, -1, IssueCategoryElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(issueKindElementEClass, IssueKindElement.class, "IssueKindElement", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getIssueKindElement_Name(), ecorePackage.getEString(), "name", null, 0, 1, IssueKindElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEAttribute(getIssueKindElement_Elements(), ecorePackage.getEString(), "elements", null, 0, -1, IssueKindElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(issueRelevanceElementEClass, IssueRelevanceElement.class, "IssueRelevanceElement", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getIssueRelevanceElement_Name(), ecorePackage.getEString(), "name", null, 0, 1, IssueRelevanceElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEAttribute(getIssueRelevanceElement_Relevance(), ecorePackage.getEInt(), "relevance", null, 0, 1, IssueRelevanceElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(issuePackageElementEClass, IssuePackageElement.class, "IssuePackageElement", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getIssuePackageElement_Name(), ecorePackage.getEString(), "name", null, 0, 1, IssuePackageElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEAttribute(getIssuePackageElement_Package(), ecorePackage.getEString(), "package", null, 0, 1, IssuePackageElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(issueClassElementEClass, IssueClassElement.class, "IssueClassElement", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getIssueClassElement_Name(), ecorePackage.getEString(), "name", null, 0, 1, IssueClassElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEAttribute(getIssueClassElement_Class(), ecorePackage.getEString(), "class", null, 0, 1, IssueClassElement.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    initEClass(issueCategoriesEClass, IssueCategories.class, "IssueCategories", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
    initEAttribute(getIssueCategories_Bug(), ecorePackage.getEString(), "bug", null, 0, 1, IssueCategories.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEAttribute(getIssueCategories_Smell(), ecorePackage.getEString(), "smell", null, 0, 1, IssueCategories.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEAttribute(getIssueCategories_Performance(), ecorePackage.getEString(), "performance", null, 0, 1, IssueCategories.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
    initEAttribute(getIssueCategories_Comprehensibility(), ecorePackage.getEString(), "comprehensibility", null, 0, 1, IssueCategories.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

    // Create resource
    createResource(eNS_URI);
  }

} //BDLPackageImpl
