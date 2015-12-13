/**
 */
package org.opalj.bdl.bDL;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Issue Relevance Element</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.opalj.bdl.bDL.IssueRelevanceElement#getName <em>Name</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.IssueRelevanceElement#getRelevance <em>Relevance</em>}</li>
 * </ul>
 *
 * @see org.opalj.bdl.bDL.BDLPackage#getIssueRelevanceElement()
 * @model
 * @generated
 */
public interface IssueRelevanceElement extends EObject
{
  /**
   * Returns the value of the '<em><b>Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Name</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Name</em>' attribute.
   * @see #setName(String)
   * @see org.opalj.bdl.bDL.BDLPackage#getIssueRelevanceElement_Name()
   * @model
   * @generated
   */
  String getName();

  /**
   * Sets the value of the '{@link org.opalj.bdl.bDL.IssueRelevanceElement#getName <em>Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Name</em>' attribute.
   * @see #getName()
   * @generated
   */
  void setName(String value);

  /**
   * Returns the value of the '<em><b>Relevance</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Relevance</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Relevance</em>' attribute.
   * @see #setRelevance(int)
   * @see org.opalj.bdl.bDL.BDLPackage#getIssueRelevanceElement_Relevance()
   * @model
   * @generated
   */
  int getRelevance();

  /**
   * Sets the value of the '{@link org.opalj.bdl.bDL.IssueRelevanceElement#getRelevance <em>Relevance</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Relevance</em>' attribute.
   * @see #getRelevance()
   * @generated
   */
  void setRelevance(int value);

} // IssueRelevanceElement
