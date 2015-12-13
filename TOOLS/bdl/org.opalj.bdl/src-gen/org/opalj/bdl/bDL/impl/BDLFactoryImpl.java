/**
 */
package org.opalj.bdl.bDL.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.eclipse.emf.ecore.plugin.EcorePlugin;

import org.opalj.bdl.bDL.*;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class BDLFactoryImpl extends EFactoryImpl implements BDLFactory
{
  /**
   * Creates the default factory implementation.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static BDLFactory init()
  {
    try
    {
      BDLFactory theBDLFactory = (BDLFactory)EPackage.Registry.INSTANCE.getEFactory(BDLPackage.eNS_URI);
      if (theBDLFactory != null)
      {
        return theBDLFactory;
      }
    }
    catch (Exception exception)
    {
      EcorePlugin.INSTANCE.log(exception);
    }
    return new BDLFactoryImpl();
  }

  /**
   * Creates an instance of the factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public BDLFactoryImpl()
  {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public EObject create(EClass eClass)
  {
    switch (eClass.getClassifierID())
    {
      case BDLPackage.MODEL: return createModel();
      case BDLPackage.MODEL_CONTAINER: return createModelContainer();
      case BDLPackage.PARAMETER_CONTAINER: return createParameterContainer();
      case BDLPackage.PARAMETER_ELEMENT: return createParameterElement();
      case BDLPackage.PARAMETER_KEY_VALUE_ELEMENT: return createParameterKeyValueElement();
      case BDLPackage.PARAMETER_KEY_ELEMENT: return createParameterKeyElement();
      case BDLPackage.ISSUES_CONTAINER: return createIssuesContainer();
      case BDLPackage.ISSUES_TITLE_ELEMENT: return createIssuesTitleElement();
      case BDLPackage.ISSUE_ELEMENT: return createIssueElement();
      case BDLPackage.ISSUE_SUPPRESS_COMMENT: return createIssueSuppressComment();
      case BDLPackage.ISSUE_CATEGORY_ELEMENT: return createIssueCategoryElement();
      case BDLPackage.ISSUE_KIND_ELEMENT: return createIssueKindElement();
      case BDLPackage.ISSUE_RELEVANCE_ELEMENT: return createIssueRelevanceElement();
      case BDLPackage.ISSUE_PACKAGE_ELEMENT: return createIssuePackageElement();
      case BDLPackage.ISSUE_CLASS_ELEMENT: return createIssueClassElement();
      case BDLPackage.ISSUE_CATEGORIES: return createIssueCategories();
      default:
        throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
    }
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public Model createModel()
  {
    ModelImpl model = new ModelImpl();
    return model;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ModelContainer createModelContainer()
  {
    ModelContainerImpl modelContainer = new ModelContainerImpl();
    return modelContainer;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ParameterContainer createParameterContainer()
  {
    ParameterContainerImpl parameterContainer = new ParameterContainerImpl();
    return parameterContainer;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ParameterElement createParameterElement()
  {
    ParameterElementImpl parameterElement = new ParameterElementImpl();
    return parameterElement;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ParameterKeyValueElement createParameterKeyValueElement()
  {
    ParameterKeyValueElementImpl parameterKeyValueElement = new ParameterKeyValueElementImpl();
    return parameterKeyValueElement;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ParameterKeyElement createParameterKeyElement()
  {
    ParameterKeyElementImpl parameterKeyElement = new ParameterKeyElementImpl();
    return parameterKeyElement;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public IssuesContainer createIssuesContainer()
  {
    IssuesContainerImpl issuesContainer = new IssuesContainerImpl();
    return issuesContainer;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public IssuesTitleElement createIssuesTitleElement()
  {
    IssuesTitleElementImpl issuesTitleElement = new IssuesTitleElementImpl();
    return issuesTitleElement;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public IssueElement createIssueElement()
  {
    IssueElementImpl issueElement = new IssueElementImpl();
    return issueElement;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public IssueSuppressComment createIssueSuppressComment()
  {
    IssueSuppressCommentImpl issueSuppressComment = new IssueSuppressCommentImpl();
    return issueSuppressComment;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public IssueCategoryElement createIssueCategoryElement()
  {
    IssueCategoryElementImpl issueCategoryElement = new IssueCategoryElementImpl();
    return issueCategoryElement;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public IssueKindElement createIssueKindElement()
  {
    IssueKindElementImpl issueKindElement = new IssueKindElementImpl();
    return issueKindElement;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public IssueRelevanceElement createIssueRelevanceElement()
  {
    IssueRelevanceElementImpl issueRelevanceElement = new IssueRelevanceElementImpl();
    return issueRelevanceElement;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public IssuePackageElement createIssuePackageElement()
  {
    IssuePackageElementImpl issuePackageElement = new IssuePackageElementImpl();
    return issuePackageElement;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public IssueClassElement createIssueClassElement()
  {
    IssueClassElementImpl issueClassElement = new IssueClassElementImpl();
    return issueClassElement;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public IssueCategories createIssueCategories()
  {
    IssueCategoriesImpl issueCategories = new IssueCategoriesImpl();
    return issueCategories;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public BDLPackage getBDLPackage()
  {
    return (BDLPackage)getEPackage();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @deprecated
   * @generated
   */
  @Deprecated
  public static BDLPackage getPackage()
  {
    return BDLPackage.eINSTANCE;
  }

} //BDLFactoryImpl
