/**
 */
package org.opalj.bdl.bDL.util;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.util.Switch;

import org.opalj.bdl.bDL.*;

/**
 * <!-- begin-user-doc -->
 * The <b>Switch</b> for the model's inheritance hierarchy.
 * It supports the call {@link #doSwitch(EObject) doSwitch(object)}
 * to invoke the <code>caseXXX</code> method for each class of the model,
 * starting with the actual class of the object
 * and proceeding up the inheritance hierarchy
 * until a non-null result is returned,
 * which is the result of the switch.
 * <!-- end-user-doc -->
 * @see org.opalj.bdl.bDL.BDLPackage
 * @generated
 */
public class BDLSwitch<T> extends Switch<T>
{
  /**
   * The cached model package
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected static BDLPackage modelPackage;

  /**
   * Creates an instance of the switch.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public BDLSwitch()
  {
    if (modelPackage == null)
    {
      modelPackage = BDLPackage.eINSTANCE;
    }
  }

  /**
   * Checks whether this is a switch for the given package.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param ePackage the package in question.
   * @return whether this is a switch for the given package.
   * @generated
   */
  @Override
  protected boolean isSwitchFor(EPackage ePackage)
  {
    return ePackage == modelPackage;
  }

  /**
   * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields that result.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the first non-null result returned by a <code>caseXXX</code> call.
   * @generated
   */
  @Override
  protected T doSwitch(int classifierID, EObject theEObject)
  {
    switch (classifierID)
    {
      case BDLPackage.MODEL:
      {
        Model model = (Model)theEObject;
        T result = caseModel(model);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case BDLPackage.MODEL_CONTAINER:
      {
        ModelContainer modelContainer = (ModelContainer)theEObject;
        T result = caseModelContainer(modelContainer);
        if (result == null) result = caseModel(modelContainer);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case BDLPackage.PARAMETER_CONTAINER:
      {
        ParameterContainer parameterContainer = (ParameterContainer)theEObject;
        T result = caseParameterContainer(parameterContainer);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case BDLPackage.PARAMETER_ELEMENT:
      {
        ParameterElement parameterElement = (ParameterElement)theEObject;
        T result = caseParameterElement(parameterElement);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case BDLPackage.PARAMETER_KEY_VALUE_ELEMENT:
      {
        ParameterKeyValueElement parameterKeyValueElement = (ParameterKeyValueElement)theEObject;
        T result = caseParameterKeyValueElement(parameterKeyValueElement);
        if (result == null) result = caseParameterElement(parameterKeyValueElement);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case BDLPackage.PARAMETER_KEY_ELEMENT:
      {
        ParameterKeyElement parameterKeyElement = (ParameterKeyElement)theEObject;
        T result = caseParameterKeyElement(parameterKeyElement);
        if (result == null) result = caseParameterElement(parameterKeyElement);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case BDLPackage.ISSUES_CONTAINER:
      {
        IssuesContainer issuesContainer = (IssuesContainer)theEObject;
        T result = caseIssuesContainer(issuesContainer);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case BDLPackage.ISSUES_TITLE_ELEMENT:
      {
        IssuesTitleElement issuesTitleElement = (IssuesTitleElement)theEObject;
        T result = caseIssuesTitleElement(issuesTitleElement);
        if (result == null) result = caseIssuesContainer(issuesTitleElement);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case BDLPackage.ISSUE_ELEMENT:
      {
        IssueElement issueElement = (IssueElement)theEObject;
        T result = caseIssueElement(issueElement);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case BDLPackage.ISSUE_SUPPRESS_COMMENT:
      {
        IssueSuppressComment issueSuppressComment = (IssueSuppressComment)theEObject;
        T result = caseIssueSuppressComment(issueSuppressComment);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case BDLPackage.ISSUE_CATEGORY_ELEMENT:
      {
        IssueCategoryElement issueCategoryElement = (IssueCategoryElement)theEObject;
        T result = caseIssueCategoryElement(issueCategoryElement);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case BDLPackage.ISSUE_KIND_ELEMENT:
      {
        IssueKindElement issueKindElement = (IssueKindElement)theEObject;
        T result = caseIssueKindElement(issueKindElement);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case BDLPackage.ISSUE_RELEVANCE_ELEMENT:
      {
        IssueRelevanceElement issueRelevanceElement = (IssueRelevanceElement)theEObject;
        T result = caseIssueRelevanceElement(issueRelevanceElement);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case BDLPackage.ISSUE_PACKAGE_ELEMENT:
      {
        IssuePackageElement issuePackageElement = (IssuePackageElement)theEObject;
        T result = caseIssuePackageElement(issuePackageElement);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case BDLPackage.ISSUE_CLASS_ELEMENT:
      {
        IssueClassElement issueClassElement = (IssueClassElement)theEObject;
        T result = caseIssueClassElement(issueClassElement);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      case BDLPackage.ISSUE_CATEGORIES:
      {
        IssueCategories issueCategories = (IssueCategories)theEObject;
        T result = caseIssueCategories(issueCategories);
        if (result == null) result = defaultCase(theEObject);
        return result;
      }
      default: return defaultCase(theEObject);
    }
  }

  /**
   * Returns the result of interpreting the object as an instance of '<em>Model</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpreting the object as an instance of '<em>Model</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public T caseModel(Model object)
  {
    return null;
  }

  /**
   * Returns the result of interpreting the object as an instance of '<em>Model Container</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpreting the object as an instance of '<em>Model Container</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public T caseModelContainer(ModelContainer object)
  {
    return null;
  }

  /**
   * Returns the result of interpreting the object as an instance of '<em>Parameter Container</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpreting the object as an instance of '<em>Parameter Container</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public T caseParameterContainer(ParameterContainer object)
  {
    return null;
  }

  /**
   * Returns the result of interpreting the object as an instance of '<em>Parameter Element</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpreting the object as an instance of '<em>Parameter Element</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public T caseParameterElement(ParameterElement object)
  {
    return null;
  }

  /**
   * Returns the result of interpreting the object as an instance of '<em>Parameter Key Value Element</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpreting the object as an instance of '<em>Parameter Key Value Element</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public T caseParameterKeyValueElement(ParameterKeyValueElement object)
  {
    return null;
  }

  /**
   * Returns the result of interpreting the object as an instance of '<em>Parameter Key Element</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpreting the object as an instance of '<em>Parameter Key Element</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public T caseParameterKeyElement(ParameterKeyElement object)
  {
    return null;
  }

  /**
   * Returns the result of interpreting the object as an instance of '<em>Issues Container</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpreting the object as an instance of '<em>Issues Container</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public T caseIssuesContainer(IssuesContainer object)
  {
    return null;
  }

  /**
   * Returns the result of interpreting the object as an instance of '<em>Issues Title Element</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpreting the object as an instance of '<em>Issues Title Element</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public T caseIssuesTitleElement(IssuesTitleElement object)
  {
    return null;
  }

  /**
   * Returns the result of interpreting the object as an instance of '<em>Issue Element</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpreting the object as an instance of '<em>Issue Element</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public T caseIssueElement(IssueElement object)
  {
    return null;
  }

  /**
   * Returns the result of interpreting the object as an instance of '<em>Issue Suppress Comment</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpreting the object as an instance of '<em>Issue Suppress Comment</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public T caseIssueSuppressComment(IssueSuppressComment object)
  {
    return null;
  }

  /**
   * Returns the result of interpreting the object as an instance of '<em>Issue Category Element</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpreting the object as an instance of '<em>Issue Category Element</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public T caseIssueCategoryElement(IssueCategoryElement object)
  {
    return null;
  }

  /**
   * Returns the result of interpreting the object as an instance of '<em>Issue Kind Element</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpreting the object as an instance of '<em>Issue Kind Element</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public T caseIssueKindElement(IssueKindElement object)
  {
    return null;
  }

  /**
   * Returns the result of interpreting the object as an instance of '<em>Issue Relevance Element</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpreting the object as an instance of '<em>Issue Relevance Element</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public T caseIssueRelevanceElement(IssueRelevanceElement object)
  {
    return null;
  }

  /**
   * Returns the result of interpreting the object as an instance of '<em>Issue Package Element</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpreting the object as an instance of '<em>Issue Package Element</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public T caseIssuePackageElement(IssuePackageElement object)
  {
    return null;
  }

  /**
   * Returns the result of interpreting the object as an instance of '<em>Issue Class Element</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpreting the object as an instance of '<em>Issue Class Element</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public T caseIssueClassElement(IssueClassElement object)
  {
    return null;
  }

  /**
   * Returns the result of interpreting the object as an instance of '<em>Issue Categories</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpreting the object as an instance of '<em>Issue Categories</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
   * @generated
   */
  public T caseIssueCategories(IssueCategories object)
  {
    return null;
  }

  /**
   * Returns the result of interpreting the object as an instance of '<em>EObject</em>'.
   * <!-- begin-user-doc -->
   * This implementation returns null;
   * returning a non-null result will terminate the switch, but this is the last case anyway.
   * <!-- end-user-doc -->
   * @param object the target of the switch.
   * @return the result of interpreting the object as an instance of '<em>EObject</em>'.
   * @see #doSwitch(org.eclipse.emf.ecore.EObject)
   * @generated
   */
  @Override
  public T defaultCase(EObject object)
  {
    return null;
  }

} //BDLSwitch
