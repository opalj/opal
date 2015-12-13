/**
 */
package org.opalj.bdl.bDL;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see org.opalj.bdl.bDL.BDLPackage
 * @generated
 */
public interface BDLFactory extends EFactory
{
  /**
   * The singleton instance of the factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  BDLFactory eINSTANCE = org.opalj.bdl.bDL.impl.BDLFactoryImpl.init();

  /**
   * Returns a new object of class '<em>Model</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>Model</em>'.
   * @generated
   */
  Model createModel();

  /**
   * Returns a new object of class '<em>Model Container</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>Model Container</em>'.
   * @generated
   */
  ModelContainer createModelContainer();

  /**
   * Returns a new object of class '<em>Parameter Container</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>Parameter Container</em>'.
   * @generated
   */
  ParameterContainer createParameterContainer();

  /**
   * Returns a new object of class '<em>Parameter Element</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>Parameter Element</em>'.
   * @generated
   */
  ParameterElement createParameterElement();

  /**
   * Returns a new object of class '<em>Parameter Key Value Element</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>Parameter Key Value Element</em>'.
   * @generated
   */
  ParameterKeyValueElement createParameterKeyValueElement();

  /**
   * Returns a new object of class '<em>Parameter Key Element</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>Parameter Key Element</em>'.
   * @generated
   */
  ParameterKeyElement createParameterKeyElement();

  /**
   * Returns a new object of class '<em>Issues Container</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>Issues Container</em>'.
   * @generated
   */
  IssuesContainer createIssuesContainer();

  /**
   * Returns a new object of class '<em>Issues Title Element</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>Issues Title Element</em>'.
   * @generated
   */
  IssuesTitleElement createIssuesTitleElement();

  /**
   * Returns a new object of class '<em>Issue Element</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>Issue Element</em>'.
   * @generated
   */
  IssueElement createIssueElement();

  /**
   * Returns a new object of class '<em>Issue Suppress Comment</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>Issue Suppress Comment</em>'.
   * @generated
   */
  IssueSuppressComment createIssueSuppressComment();

  /**
   * Returns a new object of class '<em>Issue Category Element</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>Issue Category Element</em>'.
   * @generated
   */
  IssueCategoryElement createIssueCategoryElement();

  /**
   * Returns a new object of class '<em>Issue Kind Element</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>Issue Kind Element</em>'.
   * @generated
   */
  IssueKindElement createIssueKindElement();

  /**
   * Returns a new object of class '<em>Issue Relevance Element</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>Issue Relevance Element</em>'.
   * @generated
   */
  IssueRelevanceElement createIssueRelevanceElement();

  /**
   * Returns a new object of class '<em>Issue Package Element</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>Issue Package Element</em>'.
   * @generated
   */
  IssuePackageElement createIssuePackageElement();

  /**
   * Returns a new object of class '<em>Issue Class Element</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>Issue Class Element</em>'.
   * @generated
   */
  IssueClassElement createIssueClassElement();

  /**
   * Returns a new object of class '<em>Issue Categories</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>Issue Categories</em>'.
   * @generated
   */
  IssueCategories createIssueCategories();

  /**
   * Returns the package supported by this factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the package supported by this factory.
   * @generated
   */
  BDLPackage getBDLPackage();

} //BDLFactory
