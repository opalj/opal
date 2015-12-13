/**
 */
package org.opalj.bdl.bDL;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Issues Title Element</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.opalj.bdl.bDL.IssuesTitleElement#getElements <em>Elements</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.IssuesTitleElement#getName <em>Name</em>}</li>
 * </ul>
 *
 * @see org.opalj.bdl.bDL.BDLPackage#getIssuesTitleElement()
 * @model
 * @generated
 */
public interface IssuesTitleElement extends IssuesContainer
{
  /**
   * Returns the value of the '<em><b>Elements</b></em>' containment reference list.
   * The list contents are of type {@link org.opalj.bdl.bDL.IssueElement}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Elements</em>' containment reference list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Elements</em>' containment reference list.
   * @see org.opalj.bdl.bDL.BDLPackage#getIssuesTitleElement_Elements()
   * @model containment="true"
   * @generated
   */
  EList<IssueElement> getElements();

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
   * @see org.opalj.bdl.bDL.BDLPackage#getIssuesTitleElement_Name()
   * @model
   * @generated
   */
  String getName();

  /**
   * Sets the value of the '{@link org.opalj.bdl.bDL.IssuesTitleElement#getName <em>Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Name</em>' attribute.
   * @see #getName()
   * @generated
   */
  void setName(String value);

} // IssuesTitleElement
