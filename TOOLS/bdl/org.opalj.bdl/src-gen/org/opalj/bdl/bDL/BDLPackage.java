/**
 */
package org.opalj.bdl.bDL;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * @see org.opalj.bdl.bDL.BDLFactory
 * @model kind="package"
 * @generated
 */
public interface BDLPackage extends EPackage
{
  /**
   * The package name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNAME = "bDL";

  /**
   * The package namespace URI.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNS_URI = "http://www.opalj.org/bdl/BDL";

  /**
   * The package namespace name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNS_PREFIX = "bDL";

  /**
   * The singleton instance of the package.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  BDLPackage eINSTANCE = org.opalj.bdl.bDL.impl.BDLPackageImpl.init();

  /**
   * The meta object id for the '{@link org.opalj.bdl.bDL.impl.ModelImpl <em>Model</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see org.opalj.bdl.bDL.impl.ModelImpl
   * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getModel()
   * @generated
   */
  int MODEL = 0;

  /**
   * The number of structural features of the '<em>Model</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int MODEL_FEATURE_COUNT = 0;

  /**
   * The meta object id for the '{@link org.opalj.bdl.bDL.impl.ModelContainerImpl <em>Model Container</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see org.opalj.bdl.bDL.impl.ModelContainerImpl
   * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getModelContainer()
   * @generated
   */
  int MODEL_CONTAINER = 1;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int MODEL_CONTAINER__NAME = MODEL_FEATURE_COUNT + 0;

  /**
   * The feature id for the '<em><b>Parameter</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int MODEL_CONTAINER__PARAMETER = MODEL_FEATURE_COUNT + 1;

  /**
   * The feature id for the '<em><b>Issues</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int MODEL_CONTAINER__ISSUES = MODEL_FEATURE_COUNT + 2;

  /**
   * The number of structural features of the '<em>Model Container</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int MODEL_CONTAINER_FEATURE_COUNT = MODEL_FEATURE_COUNT + 3;

  /**
   * The meta object id for the '{@link org.opalj.bdl.bDL.impl.ParameterContainerImpl <em>Parameter Container</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see org.opalj.bdl.bDL.impl.ParameterContainerImpl
   * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getParameterContainer()
   * @generated
   */
  int PARAMETER_CONTAINER = 2;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int PARAMETER_CONTAINER__NAME = 0;

  /**
   * The feature id for the '<em><b>Elements</b></em>' containment reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int PARAMETER_CONTAINER__ELEMENTS = 1;

  /**
   * The number of structural features of the '<em>Parameter Container</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int PARAMETER_CONTAINER_FEATURE_COUNT = 2;

  /**
   * The meta object id for the '{@link org.opalj.bdl.bDL.impl.ParameterElementImpl <em>Parameter Element</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see org.opalj.bdl.bDL.impl.ParameterElementImpl
   * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getParameterElement()
   * @generated
   */
  int PARAMETER_ELEMENT = 3;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int PARAMETER_ELEMENT__NAME = 0;

  /**
   * The number of structural features of the '<em>Parameter Element</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int PARAMETER_ELEMENT_FEATURE_COUNT = 1;

  /**
   * The meta object id for the '{@link org.opalj.bdl.bDL.impl.ParameterKeyValueElementImpl <em>Parameter Key Value Element</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see org.opalj.bdl.bDL.impl.ParameterKeyValueElementImpl
   * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getParameterKeyValueElement()
   * @generated
   */
  int PARAMETER_KEY_VALUE_ELEMENT = 4;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int PARAMETER_KEY_VALUE_ELEMENT__NAME = PARAMETER_ELEMENT__NAME;

  /**
   * The feature id for the '<em><b>Value</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int PARAMETER_KEY_VALUE_ELEMENT__VALUE = PARAMETER_ELEMENT_FEATURE_COUNT + 0;

  /**
   * The number of structural features of the '<em>Parameter Key Value Element</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int PARAMETER_KEY_VALUE_ELEMENT_FEATURE_COUNT = PARAMETER_ELEMENT_FEATURE_COUNT + 1;

  /**
   * The meta object id for the '{@link org.opalj.bdl.bDL.impl.ParameterKeyElementImpl <em>Parameter Key Element</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see org.opalj.bdl.bDL.impl.ParameterKeyElementImpl
   * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getParameterKeyElement()
   * @generated
   */
  int PARAMETER_KEY_ELEMENT = 5;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int PARAMETER_KEY_ELEMENT__NAME = PARAMETER_ELEMENT__NAME;

  /**
   * The number of structural features of the '<em>Parameter Key Element</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int PARAMETER_KEY_ELEMENT_FEATURE_COUNT = PARAMETER_ELEMENT_FEATURE_COUNT + 0;

  /**
   * The meta object id for the '{@link org.opalj.bdl.bDL.impl.IssuesContainerImpl <em>Issues Container</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see org.opalj.bdl.bDL.impl.IssuesContainerImpl
   * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getIssuesContainer()
   * @generated
   */
  int ISSUES_CONTAINER = 6;

  /**
   * The number of structural features of the '<em>Issues Container</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUES_CONTAINER_FEATURE_COUNT = 0;

  /**
   * The meta object id for the '{@link org.opalj.bdl.bDL.impl.IssuesTitleElementImpl <em>Issues Title Element</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see org.opalj.bdl.bDL.impl.IssuesTitleElementImpl
   * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getIssuesTitleElement()
   * @generated
   */
  int ISSUES_TITLE_ELEMENT = 7;

  /**
   * The feature id for the '<em><b>Elements</b></em>' containment reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUES_TITLE_ELEMENT__ELEMENTS = ISSUES_CONTAINER_FEATURE_COUNT + 0;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUES_TITLE_ELEMENT__NAME = ISSUES_CONTAINER_FEATURE_COUNT + 1;

  /**
   * The number of structural features of the '<em>Issues Title Element</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUES_TITLE_ELEMENT_FEATURE_COUNT = ISSUES_CONTAINER_FEATURE_COUNT + 2;

  /**
   * The meta object id for the '{@link org.opalj.bdl.bDL.impl.IssueElementImpl <em>Issue Element</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see org.opalj.bdl.bDL.impl.IssueElementImpl
   * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getIssueElement()
   * @generated
   */
  int ISSUE_ELEMENT = 8;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_ELEMENT__NAME = 0;

  /**
   * The feature id for the '<em><b>Comment</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_ELEMENT__COMMENT = 1;

  /**
   * The feature id for the '<em><b>Message</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_ELEMENT__MESSAGE = 2;

  /**
   * The feature id for the '<em><b>Categories</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_ELEMENT__CATEGORIES = 3;

  /**
   * The feature id for the '<em><b>Kinds</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_ELEMENT__KINDS = 4;

  /**
   * The feature id for the '<em><b>Relevance</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_ELEMENT__RELEVANCE = 5;

  /**
   * The feature id for the '<em><b>Package</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_ELEMENT__PACKAGE = 6;

  /**
   * The feature id for the '<em><b>Class</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_ELEMENT__CLASS = 7;

  /**
   * The number of structural features of the '<em>Issue Element</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_ELEMENT_FEATURE_COUNT = 8;

  /**
   * The meta object id for the '{@link org.opalj.bdl.bDL.impl.IssueSuppressCommentImpl <em>Issue Suppress Comment</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see org.opalj.bdl.bDL.impl.IssueSuppressCommentImpl
   * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getIssueSuppressComment()
   * @generated
   */
  int ISSUE_SUPPRESS_COMMENT = 9;

  /**
   * The feature id for the '<em><b>Value</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_SUPPRESS_COMMENT__VALUE = 0;

  /**
   * The number of structural features of the '<em>Issue Suppress Comment</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_SUPPRESS_COMMENT_FEATURE_COUNT = 1;

  /**
   * The meta object id for the '{@link org.opalj.bdl.bDL.impl.IssueCategoryElementImpl <em>Issue Category Element</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see org.opalj.bdl.bDL.impl.IssueCategoryElementImpl
   * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getIssueCategoryElement()
   * @generated
   */
  int ISSUE_CATEGORY_ELEMENT = 10;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_CATEGORY_ELEMENT__NAME = 0;

  /**
   * The feature id for the '<em><b>Elements</b></em>' containment reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_CATEGORY_ELEMENT__ELEMENTS = 1;

  /**
   * The number of structural features of the '<em>Issue Category Element</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_CATEGORY_ELEMENT_FEATURE_COUNT = 2;

  /**
   * The meta object id for the '{@link org.opalj.bdl.bDL.impl.IssueKindElementImpl <em>Issue Kind Element</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see org.opalj.bdl.bDL.impl.IssueKindElementImpl
   * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getIssueKindElement()
   * @generated
   */
  int ISSUE_KIND_ELEMENT = 11;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_KIND_ELEMENT__NAME = 0;

  /**
   * The feature id for the '<em><b>Elements</b></em>' attribute list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_KIND_ELEMENT__ELEMENTS = 1;

  /**
   * The number of structural features of the '<em>Issue Kind Element</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_KIND_ELEMENT_FEATURE_COUNT = 2;

  /**
   * The meta object id for the '{@link org.opalj.bdl.bDL.impl.IssueRelevanceElementImpl <em>Issue Relevance Element</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see org.opalj.bdl.bDL.impl.IssueRelevanceElementImpl
   * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getIssueRelevanceElement()
   * @generated
   */
  int ISSUE_RELEVANCE_ELEMENT = 12;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_RELEVANCE_ELEMENT__NAME = 0;

  /**
   * The feature id for the '<em><b>Relevance</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_RELEVANCE_ELEMENT__RELEVANCE = 1;

  /**
   * The number of structural features of the '<em>Issue Relevance Element</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_RELEVANCE_ELEMENT_FEATURE_COUNT = 2;

  /**
   * The meta object id for the '{@link org.opalj.bdl.bDL.impl.IssuePackageElementImpl <em>Issue Package Element</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see org.opalj.bdl.bDL.impl.IssuePackageElementImpl
   * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getIssuePackageElement()
   * @generated
   */
  int ISSUE_PACKAGE_ELEMENT = 13;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_PACKAGE_ELEMENT__NAME = 0;

  /**
   * The feature id for the '<em><b>Package</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_PACKAGE_ELEMENT__PACKAGE = 1;

  /**
   * The number of structural features of the '<em>Issue Package Element</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_PACKAGE_ELEMENT_FEATURE_COUNT = 2;

  /**
   * The meta object id for the '{@link org.opalj.bdl.bDL.impl.IssueClassElementImpl <em>Issue Class Element</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see org.opalj.bdl.bDL.impl.IssueClassElementImpl
   * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getIssueClassElement()
   * @generated
   */
  int ISSUE_CLASS_ELEMENT = 14;

  /**
   * The feature id for the '<em><b>Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_CLASS_ELEMENT__NAME = 0;

  /**
   * The feature id for the '<em><b>Class</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_CLASS_ELEMENT__CLASS = 1;

  /**
   * The number of structural features of the '<em>Issue Class Element</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_CLASS_ELEMENT_FEATURE_COUNT = 2;

  /**
   * The meta object id for the '{@link org.opalj.bdl.bDL.impl.IssueCategoriesImpl <em>Issue Categories</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see org.opalj.bdl.bDL.impl.IssueCategoriesImpl
   * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getIssueCategories()
   * @generated
   */
  int ISSUE_CATEGORIES = 15;

  /**
   * The feature id for the '<em><b>Bug</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_CATEGORIES__BUG = 0;

  /**
   * The feature id for the '<em><b>Smell</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_CATEGORIES__SMELL = 1;

  /**
   * The feature id for the '<em><b>Performance</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_CATEGORIES__PERFORMANCE = 2;

  /**
   * The feature id for the '<em><b>Comprehensibility</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_CATEGORIES__COMPREHENSIBILITY = 3;

  /**
   * The number of structural features of the '<em>Issue Categories</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ISSUE_CATEGORIES_FEATURE_COUNT = 4;


  /**
   * Returns the meta object for class '{@link org.opalj.bdl.bDL.Model <em>Model</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>Model</em>'.
   * @see org.opalj.bdl.bDL.Model
   * @generated
   */
  EClass getModel();

  /**
   * Returns the meta object for class '{@link org.opalj.bdl.bDL.ModelContainer <em>Model Container</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>Model Container</em>'.
   * @see org.opalj.bdl.bDL.ModelContainer
   * @generated
   */
  EClass getModelContainer();

  /**
   * Returns the meta object for the attribute '{@link org.opalj.bdl.bDL.ModelContainer#getName <em>Name</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see org.opalj.bdl.bDL.ModelContainer#getName()
   * @see #getModelContainer()
   * @generated
   */
  EAttribute getModelContainer_Name();

  /**
   * Returns the meta object for the containment reference '{@link org.opalj.bdl.bDL.ModelContainer#getParameter <em>Parameter</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the containment reference '<em>Parameter</em>'.
   * @see org.opalj.bdl.bDL.ModelContainer#getParameter()
   * @see #getModelContainer()
   * @generated
   */
  EReference getModelContainer_Parameter();

  /**
   * Returns the meta object for the containment reference '{@link org.opalj.bdl.bDL.ModelContainer#getIssues <em>Issues</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the containment reference '<em>Issues</em>'.
   * @see org.opalj.bdl.bDL.ModelContainer#getIssues()
   * @see #getModelContainer()
   * @generated
   */
  EReference getModelContainer_Issues();

  /**
   * Returns the meta object for class '{@link org.opalj.bdl.bDL.ParameterContainer <em>Parameter Container</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>Parameter Container</em>'.
   * @see org.opalj.bdl.bDL.ParameterContainer
   * @generated
   */
  EClass getParameterContainer();

  /**
   * Returns the meta object for the attribute '{@link org.opalj.bdl.bDL.ParameterContainer#getName <em>Name</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see org.opalj.bdl.bDL.ParameterContainer#getName()
   * @see #getParameterContainer()
   * @generated
   */
  EAttribute getParameterContainer_Name();

  /**
   * Returns the meta object for the containment reference list '{@link org.opalj.bdl.bDL.ParameterContainer#getElements <em>Elements</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the containment reference list '<em>Elements</em>'.
   * @see org.opalj.bdl.bDL.ParameterContainer#getElements()
   * @see #getParameterContainer()
   * @generated
   */
  EReference getParameterContainer_Elements();

  /**
   * Returns the meta object for class '{@link org.opalj.bdl.bDL.ParameterElement <em>Parameter Element</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>Parameter Element</em>'.
   * @see org.opalj.bdl.bDL.ParameterElement
   * @generated
   */
  EClass getParameterElement();

  /**
   * Returns the meta object for the attribute '{@link org.opalj.bdl.bDL.ParameterElement#getName <em>Name</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see org.opalj.bdl.bDL.ParameterElement#getName()
   * @see #getParameterElement()
   * @generated
   */
  EAttribute getParameterElement_Name();

  /**
   * Returns the meta object for class '{@link org.opalj.bdl.bDL.ParameterKeyValueElement <em>Parameter Key Value Element</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>Parameter Key Value Element</em>'.
   * @see org.opalj.bdl.bDL.ParameterKeyValueElement
   * @generated
   */
  EClass getParameterKeyValueElement();

  /**
   * Returns the meta object for the attribute '{@link org.opalj.bdl.bDL.ParameterKeyValueElement#getValue <em>Value</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Value</em>'.
   * @see org.opalj.bdl.bDL.ParameterKeyValueElement#getValue()
   * @see #getParameterKeyValueElement()
   * @generated
   */
  EAttribute getParameterKeyValueElement_Value();

  /**
   * Returns the meta object for class '{@link org.opalj.bdl.bDL.ParameterKeyElement <em>Parameter Key Element</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>Parameter Key Element</em>'.
   * @see org.opalj.bdl.bDL.ParameterKeyElement
   * @generated
   */
  EClass getParameterKeyElement();

  /**
   * Returns the meta object for class '{@link org.opalj.bdl.bDL.IssuesContainer <em>Issues Container</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>Issues Container</em>'.
   * @see org.opalj.bdl.bDL.IssuesContainer
   * @generated
   */
  EClass getIssuesContainer();

  /**
   * Returns the meta object for class '{@link org.opalj.bdl.bDL.IssuesTitleElement <em>Issues Title Element</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>Issues Title Element</em>'.
   * @see org.opalj.bdl.bDL.IssuesTitleElement
   * @generated
   */
  EClass getIssuesTitleElement();

  /**
   * Returns the meta object for the containment reference list '{@link org.opalj.bdl.bDL.IssuesTitleElement#getElements <em>Elements</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the containment reference list '<em>Elements</em>'.
   * @see org.opalj.bdl.bDL.IssuesTitleElement#getElements()
   * @see #getIssuesTitleElement()
   * @generated
   */
  EReference getIssuesTitleElement_Elements();

  /**
   * Returns the meta object for the attribute '{@link org.opalj.bdl.bDL.IssuesTitleElement#getName <em>Name</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see org.opalj.bdl.bDL.IssuesTitleElement#getName()
   * @see #getIssuesTitleElement()
   * @generated
   */
  EAttribute getIssuesTitleElement_Name();

  /**
   * Returns the meta object for class '{@link org.opalj.bdl.bDL.IssueElement <em>Issue Element</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>Issue Element</em>'.
   * @see org.opalj.bdl.bDL.IssueElement
   * @generated
   */
  EClass getIssueElement();

  /**
   * Returns the meta object for the attribute list '{@link org.opalj.bdl.bDL.IssueElement#getName <em>Name</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute list '<em>Name</em>'.
   * @see org.opalj.bdl.bDL.IssueElement#getName()
   * @see #getIssueElement()
   * @generated
   */
  EAttribute getIssueElement_Name();

  /**
   * Returns the meta object for the containment reference '{@link org.opalj.bdl.bDL.IssueElement#getComment <em>Comment</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the containment reference '<em>Comment</em>'.
   * @see org.opalj.bdl.bDL.IssueElement#getComment()
   * @see #getIssueElement()
   * @generated
   */
  EReference getIssueElement_Comment();

  /**
   * Returns the meta object for the attribute '{@link org.opalj.bdl.bDL.IssueElement#getMessage <em>Message</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Message</em>'.
   * @see org.opalj.bdl.bDL.IssueElement#getMessage()
   * @see #getIssueElement()
   * @generated
   */
  EAttribute getIssueElement_Message();

  /**
   * Returns the meta object for the containment reference '{@link org.opalj.bdl.bDL.IssueElement#getCategories <em>Categories</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the containment reference '<em>Categories</em>'.
   * @see org.opalj.bdl.bDL.IssueElement#getCategories()
   * @see #getIssueElement()
   * @generated
   */
  EReference getIssueElement_Categories();

  /**
   * Returns the meta object for the containment reference '{@link org.opalj.bdl.bDL.IssueElement#getKinds <em>Kinds</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the containment reference '<em>Kinds</em>'.
   * @see org.opalj.bdl.bDL.IssueElement#getKinds()
   * @see #getIssueElement()
   * @generated
   */
  EReference getIssueElement_Kinds();

  /**
   * Returns the meta object for the containment reference '{@link org.opalj.bdl.bDL.IssueElement#getRelevance <em>Relevance</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the containment reference '<em>Relevance</em>'.
   * @see org.opalj.bdl.bDL.IssueElement#getRelevance()
   * @see #getIssueElement()
   * @generated
   */
  EReference getIssueElement_Relevance();

  /**
   * Returns the meta object for the containment reference '{@link org.opalj.bdl.bDL.IssueElement#getPackage <em>Package</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the containment reference '<em>Package</em>'.
   * @see org.opalj.bdl.bDL.IssueElement#getPackage()
   * @see #getIssueElement()
   * @generated
   */
  EReference getIssueElement_Package();

  /**
   * Returns the meta object for the containment reference '{@link org.opalj.bdl.bDL.IssueElement#getClass_ <em>Class</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the containment reference '<em>Class</em>'.
   * @see org.opalj.bdl.bDL.IssueElement#getClass_()
   * @see #getIssueElement()
   * @generated
   */
  EReference getIssueElement_Class();

  /**
   * Returns the meta object for class '{@link org.opalj.bdl.bDL.IssueSuppressComment <em>Issue Suppress Comment</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>Issue Suppress Comment</em>'.
   * @see org.opalj.bdl.bDL.IssueSuppressComment
   * @generated
   */
  EClass getIssueSuppressComment();

  /**
   * Returns the meta object for the attribute '{@link org.opalj.bdl.bDL.IssueSuppressComment#getValue <em>Value</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Value</em>'.
   * @see org.opalj.bdl.bDL.IssueSuppressComment#getValue()
   * @see #getIssueSuppressComment()
   * @generated
   */
  EAttribute getIssueSuppressComment_Value();

  /**
   * Returns the meta object for class '{@link org.opalj.bdl.bDL.IssueCategoryElement <em>Issue Category Element</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>Issue Category Element</em>'.
   * @see org.opalj.bdl.bDL.IssueCategoryElement
   * @generated
   */
  EClass getIssueCategoryElement();

  /**
   * Returns the meta object for the attribute '{@link org.opalj.bdl.bDL.IssueCategoryElement#getName <em>Name</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see org.opalj.bdl.bDL.IssueCategoryElement#getName()
   * @see #getIssueCategoryElement()
   * @generated
   */
  EAttribute getIssueCategoryElement_Name();

  /**
   * Returns the meta object for the containment reference list '{@link org.opalj.bdl.bDL.IssueCategoryElement#getElements <em>Elements</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the containment reference list '<em>Elements</em>'.
   * @see org.opalj.bdl.bDL.IssueCategoryElement#getElements()
   * @see #getIssueCategoryElement()
   * @generated
   */
  EReference getIssueCategoryElement_Elements();

  /**
   * Returns the meta object for class '{@link org.opalj.bdl.bDL.IssueKindElement <em>Issue Kind Element</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>Issue Kind Element</em>'.
   * @see org.opalj.bdl.bDL.IssueKindElement
   * @generated
   */
  EClass getIssueKindElement();

  /**
   * Returns the meta object for the attribute '{@link org.opalj.bdl.bDL.IssueKindElement#getName <em>Name</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see org.opalj.bdl.bDL.IssueKindElement#getName()
   * @see #getIssueKindElement()
   * @generated
   */
  EAttribute getIssueKindElement_Name();

  /**
   * Returns the meta object for the attribute list '{@link org.opalj.bdl.bDL.IssueKindElement#getElements <em>Elements</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute list '<em>Elements</em>'.
   * @see org.opalj.bdl.bDL.IssueKindElement#getElements()
   * @see #getIssueKindElement()
   * @generated
   */
  EAttribute getIssueKindElement_Elements();

  /**
   * Returns the meta object for class '{@link org.opalj.bdl.bDL.IssueRelevanceElement <em>Issue Relevance Element</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>Issue Relevance Element</em>'.
   * @see org.opalj.bdl.bDL.IssueRelevanceElement
   * @generated
   */
  EClass getIssueRelevanceElement();

  /**
   * Returns the meta object for the attribute '{@link org.opalj.bdl.bDL.IssueRelevanceElement#getName <em>Name</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see org.opalj.bdl.bDL.IssueRelevanceElement#getName()
   * @see #getIssueRelevanceElement()
   * @generated
   */
  EAttribute getIssueRelevanceElement_Name();

  /**
   * Returns the meta object for the attribute '{@link org.opalj.bdl.bDL.IssueRelevanceElement#getRelevance <em>Relevance</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Relevance</em>'.
   * @see org.opalj.bdl.bDL.IssueRelevanceElement#getRelevance()
   * @see #getIssueRelevanceElement()
   * @generated
   */
  EAttribute getIssueRelevanceElement_Relevance();

  /**
   * Returns the meta object for class '{@link org.opalj.bdl.bDL.IssuePackageElement <em>Issue Package Element</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>Issue Package Element</em>'.
   * @see org.opalj.bdl.bDL.IssuePackageElement
   * @generated
   */
  EClass getIssuePackageElement();

  /**
   * Returns the meta object for the attribute '{@link org.opalj.bdl.bDL.IssuePackageElement#getName <em>Name</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see org.opalj.bdl.bDL.IssuePackageElement#getName()
   * @see #getIssuePackageElement()
   * @generated
   */
  EAttribute getIssuePackageElement_Name();

  /**
   * Returns the meta object for the attribute '{@link org.opalj.bdl.bDL.IssuePackageElement#getPackage <em>Package</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Package</em>'.
   * @see org.opalj.bdl.bDL.IssuePackageElement#getPackage()
   * @see #getIssuePackageElement()
   * @generated
   */
  EAttribute getIssuePackageElement_Package();

  /**
   * Returns the meta object for class '{@link org.opalj.bdl.bDL.IssueClassElement <em>Issue Class Element</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>Issue Class Element</em>'.
   * @see org.opalj.bdl.bDL.IssueClassElement
   * @generated
   */
  EClass getIssueClassElement();

  /**
   * Returns the meta object for the attribute '{@link org.opalj.bdl.bDL.IssueClassElement#getName <em>Name</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Name</em>'.
   * @see org.opalj.bdl.bDL.IssueClassElement#getName()
   * @see #getIssueClassElement()
   * @generated
   */
  EAttribute getIssueClassElement_Name();

  /**
   * Returns the meta object for the attribute '{@link org.opalj.bdl.bDL.IssueClassElement#getClass_ <em>Class</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Class</em>'.
   * @see org.opalj.bdl.bDL.IssueClassElement#getClass_()
   * @see #getIssueClassElement()
   * @generated
   */
  EAttribute getIssueClassElement_Class();

  /**
   * Returns the meta object for class '{@link org.opalj.bdl.bDL.IssueCategories <em>Issue Categories</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>Issue Categories</em>'.
   * @see org.opalj.bdl.bDL.IssueCategories
   * @generated
   */
  EClass getIssueCategories();

  /**
   * Returns the meta object for the attribute '{@link org.opalj.bdl.bDL.IssueCategories#getBug <em>Bug</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Bug</em>'.
   * @see org.opalj.bdl.bDL.IssueCategories#getBug()
   * @see #getIssueCategories()
   * @generated
   */
  EAttribute getIssueCategories_Bug();

  /**
   * Returns the meta object for the attribute '{@link org.opalj.bdl.bDL.IssueCategories#getSmell <em>Smell</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Smell</em>'.
   * @see org.opalj.bdl.bDL.IssueCategories#getSmell()
   * @see #getIssueCategories()
   * @generated
   */
  EAttribute getIssueCategories_Smell();

  /**
   * Returns the meta object for the attribute '{@link org.opalj.bdl.bDL.IssueCategories#getPerformance <em>Performance</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Performance</em>'.
   * @see org.opalj.bdl.bDL.IssueCategories#getPerformance()
   * @see #getIssueCategories()
   * @generated
   */
  EAttribute getIssueCategories_Performance();

  /**
   * Returns the meta object for the attribute '{@link org.opalj.bdl.bDL.IssueCategories#getComprehensibility <em>Comprehensibility</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the attribute '<em>Comprehensibility</em>'.
   * @see org.opalj.bdl.bDL.IssueCategories#getComprehensibility()
   * @see #getIssueCategories()
   * @generated
   */
  EAttribute getIssueCategories_Comprehensibility();

  /**
   * Returns the factory that creates the instances of the model.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the factory that creates the instances of the model.
   * @generated
   */
  BDLFactory getBDLFactory();

  /**
   * <!-- begin-user-doc -->
   * Defines literals for the meta objects that represent
   * <ul>
   *   <li>each class,</li>
   *   <li>each feature of each class,</li>
   *   <li>each enum,</li>
   *   <li>and each data type</li>
   * </ul>
   * <!-- end-user-doc -->
   * @generated
   */
  interface Literals
  {
    /**
     * The meta object literal for the '{@link org.opalj.bdl.bDL.impl.ModelImpl <em>Model</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see org.opalj.bdl.bDL.impl.ModelImpl
     * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getModel()
     * @generated
     */
    EClass MODEL = eINSTANCE.getModel();

    /**
     * The meta object literal for the '{@link org.opalj.bdl.bDL.impl.ModelContainerImpl <em>Model Container</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see org.opalj.bdl.bDL.impl.ModelContainerImpl
     * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getModelContainer()
     * @generated
     */
    EClass MODEL_CONTAINER = eINSTANCE.getModelContainer();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute MODEL_CONTAINER__NAME = eINSTANCE.getModelContainer_Name();

    /**
     * The meta object literal for the '<em><b>Parameter</b></em>' containment reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference MODEL_CONTAINER__PARAMETER = eINSTANCE.getModelContainer_Parameter();

    /**
     * The meta object literal for the '<em><b>Issues</b></em>' containment reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference MODEL_CONTAINER__ISSUES = eINSTANCE.getModelContainer_Issues();

    /**
     * The meta object literal for the '{@link org.opalj.bdl.bDL.impl.ParameterContainerImpl <em>Parameter Container</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see org.opalj.bdl.bDL.impl.ParameterContainerImpl
     * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getParameterContainer()
     * @generated
     */
    EClass PARAMETER_CONTAINER = eINSTANCE.getParameterContainer();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute PARAMETER_CONTAINER__NAME = eINSTANCE.getParameterContainer_Name();

    /**
     * The meta object literal for the '<em><b>Elements</b></em>' containment reference list feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference PARAMETER_CONTAINER__ELEMENTS = eINSTANCE.getParameterContainer_Elements();

    /**
     * The meta object literal for the '{@link org.opalj.bdl.bDL.impl.ParameterElementImpl <em>Parameter Element</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see org.opalj.bdl.bDL.impl.ParameterElementImpl
     * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getParameterElement()
     * @generated
     */
    EClass PARAMETER_ELEMENT = eINSTANCE.getParameterElement();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute PARAMETER_ELEMENT__NAME = eINSTANCE.getParameterElement_Name();

    /**
     * The meta object literal for the '{@link org.opalj.bdl.bDL.impl.ParameterKeyValueElementImpl <em>Parameter Key Value Element</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see org.opalj.bdl.bDL.impl.ParameterKeyValueElementImpl
     * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getParameterKeyValueElement()
     * @generated
     */
    EClass PARAMETER_KEY_VALUE_ELEMENT = eINSTANCE.getParameterKeyValueElement();

    /**
     * The meta object literal for the '<em><b>Value</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute PARAMETER_KEY_VALUE_ELEMENT__VALUE = eINSTANCE.getParameterKeyValueElement_Value();

    /**
     * The meta object literal for the '{@link org.opalj.bdl.bDL.impl.ParameterKeyElementImpl <em>Parameter Key Element</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see org.opalj.bdl.bDL.impl.ParameterKeyElementImpl
     * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getParameterKeyElement()
     * @generated
     */
    EClass PARAMETER_KEY_ELEMENT = eINSTANCE.getParameterKeyElement();

    /**
     * The meta object literal for the '{@link org.opalj.bdl.bDL.impl.IssuesContainerImpl <em>Issues Container</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see org.opalj.bdl.bDL.impl.IssuesContainerImpl
     * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getIssuesContainer()
     * @generated
     */
    EClass ISSUES_CONTAINER = eINSTANCE.getIssuesContainer();

    /**
     * The meta object literal for the '{@link org.opalj.bdl.bDL.impl.IssuesTitleElementImpl <em>Issues Title Element</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see org.opalj.bdl.bDL.impl.IssuesTitleElementImpl
     * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getIssuesTitleElement()
     * @generated
     */
    EClass ISSUES_TITLE_ELEMENT = eINSTANCE.getIssuesTitleElement();

    /**
     * The meta object literal for the '<em><b>Elements</b></em>' containment reference list feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference ISSUES_TITLE_ELEMENT__ELEMENTS = eINSTANCE.getIssuesTitleElement_Elements();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ISSUES_TITLE_ELEMENT__NAME = eINSTANCE.getIssuesTitleElement_Name();

    /**
     * The meta object literal for the '{@link org.opalj.bdl.bDL.impl.IssueElementImpl <em>Issue Element</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see org.opalj.bdl.bDL.impl.IssueElementImpl
     * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getIssueElement()
     * @generated
     */
    EClass ISSUE_ELEMENT = eINSTANCE.getIssueElement();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute list feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ISSUE_ELEMENT__NAME = eINSTANCE.getIssueElement_Name();

    /**
     * The meta object literal for the '<em><b>Comment</b></em>' containment reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference ISSUE_ELEMENT__COMMENT = eINSTANCE.getIssueElement_Comment();

    /**
     * The meta object literal for the '<em><b>Message</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ISSUE_ELEMENT__MESSAGE = eINSTANCE.getIssueElement_Message();

    /**
     * The meta object literal for the '<em><b>Categories</b></em>' containment reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference ISSUE_ELEMENT__CATEGORIES = eINSTANCE.getIssueElement_Categories();

    /**
     * The meta object literal for the '<em><b>Kinds</b></em>' containment reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference ISSUE_ELEMENT__KINDS = eINSTANCE.getIssueElement_Kinds();

    /**
     * The meta object literal for the '<em><b>Relevance</b></em>' containment reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference ISSUE_ELEMENT__RELEVANCE = eINSTANCE.getIssueElement_Relevance();

    /**
     * The meta object literal for the '<em><b>Package</b></em>' containment reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference ISSUE_ELEMENT__PACKAGE = eINSTANCE.getIssueElement_Package();

    /**
     * The meta object literal for the '<em><b>Class</b></em>' containment reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference ISSUE_ELEMENT__CLASS = eINSTANCE.getIssueElement_Class();

    /**
     * The meta object literal for the '{@link org.opalj.bdl.bDL.impl.IssueSuppressCommentImpl <em>Issue Suppress Comment</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see org.opalj.bdl.bDL.impl.IssueSuppressCommentImpl
     * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getIssueSuppressComment()
     * @generated
     */
    EClass ISSUE_SUPPRESS_COMMENT = eINSTANCE.getIssueSuppressComment();

    /**
     * The meta object literal for the '<em><b>Value</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ISSUE_SUPPRESS_COMMENT__VALUE = eINSTANCE.getIssueSuppressComment_Value();

    /**
     * The meta object literal for the '{@link org.opalj.bdl.bDL.impl.IssueCategoryElementImpl <em>Issue Category Element</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see org.opalj.bdl.bDL.impl.IssueCategoryElementImpl
     * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getIssueCategoryElement()
     * @generated
     */
    EClass ISSUE_CATEGORY_ELEMENT = eINSTANCE.getIssueCategoryElement();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ISSUE_CATEGORY_ELEMENT__NAME = eINSTANCE.getIssueCategoryElement_Name();

    /**
     * The meta object literal for the '<em><b>Elements</b></em>' containment reference list feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference ISSUE_CATEGORY_ELEMENT__ELEMENTS = eINSTANCE.getIssueCategoryElement_Elements();

    /**
     * The meta object literal for the '{@link org.opalj.bdl.bDL.impl.IssueKindElementImpl <em>Issue Kind Element</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see org.opalj.bdl.bDL.impl.IssueKindElementImpl
     * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getIssueKindElement()
     * @generated
     */
    EClass ISSUE_KIND_ELEMENT = eINSTANCE.getIssueKindElement();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ISSUE_KIND_ELEMENT__NAME = eINSTANCE.getIssueKindElement_Name();

    /**
     * The meta object literal for the '<em><b>Elements</b></em>' attribute list feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ISSUE_KIND_ELEMENT__ELEMENTS = eINSTANCE.getIssueKindElement_Elements();

    /**
     * The meta object literal for the '{@link org.opalj.bdl.bDL.impl.IssueRelevanceElementImpl <em>Issue Relevance Element</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see org.opalj.bdl.bDL.impl.IssueRelevanceElementImpl
     * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getIssueRelevanceElement()
     * @generated
     */
    EClass ISSUE_RELEVANCE_ELEMENT = eINSTANCE.getIssueRelevanceElement();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ISSUE_RELEVANCE_ELEMENT__NAME = eINSTANCE.getIssueRelevanceElement_Name();

    /**
     * The meta object literal for the '<em><b>Relevance</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ISSUE_RELEVANCE_ELEMENT__RELEVANCE = eINSTANCE.getIssueRelevanceElement_Relevance();

    /**
     * The meta object literal for the '{@link org.opalj.bdl.bDL.impl.IssuePackageElementImpl <em>Issue Package Element</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see org.opalj.bdl.bDL.impl.IssuePackageElementImpl
     * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getIssuePackageElement()
     * @generated
     */
    EClass ISSUE_PACKAGE_ELEMENT = eINSTANCE.getIssuePackageElement();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ISSUE_PACKAGE_ELEMENT__NAME = eINSTANCE.getIssuePackageElement_Name();

    /**
     * The meta object literal for the '<em><b>Package</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ISSUE_PACKAGE_ELEMENT__PACKAGE = eINSTANCE.getIssuePackageElement_Package();

    /**
     * The meta object literal for the '{@link org.opalj.bdl.bDL.impl.IssueClassElementImpl <em>Issue Class Element</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see org.opalj.bdl.bDL.impl.IssueClassElementImpl
     * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getIssueClassElement()
     * @generated
     */
    EClass ISSUE_CLASS_ELEMENT = eINSTANCE.getIssueClassElement();

    /**
     * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ISSUE_CLASS_ELEMENT__NAME = eINSTANCE.getIssueClassElement_Name();

    /**
     * The meta object literal for the '<em><b>Class</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ISSUE_CLASS_ELEMENT__CLASS = eINSTANCE.getIssueClassElement_Class();

    /**
     * The meta object literal for the '{@link org.opalj.bdl.bDL.impl.IssueCategoriesImpl <em>Issue Categories</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see org.opalj.bdl.bDL.impl.IssueCategoriesImpl
     * @see org.opalj.bdl.bDL.impl.BDLPackageImpl#getIssueCategories()
     * @generated
     */
    EClass ISSUE_CATEGORIES = eINSTANCE.getIssueCategories();

    /**
     * The meta object literal for the '<em><b>Bug</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ISSUE_CATEGORIES__BUG = eINSTANCE.getIssueCategories_Bug();

    /**
     * The meta object literal for the '<em><b>Smell</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ISSUE_CATEGORIES__SMELL = eINSTANCE.getIssueCategories_Smell();

    /**
     * The meta object literal for the '<em><b>Performance</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ISSUE_CATEGORIES__PERFORMANCE = eINSTANCE.getIssueCategories_Performance();

    /**
     * The meta object literal for the '<em><b>Comprehensibility</b></em>' attribute feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EAttribute ISSUE_CATEGORIES__COMPREHENSIBILITY = eINSTANCE.getIssueCategories_Comprehensibility();

  }

} //BDLPackage
