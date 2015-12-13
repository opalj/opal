/**
 */
package org.opalj.bdl.bDL;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Issue Class Element</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.opalj.bdl.bDL.IssueClassElement#getName <em>Name</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.IssueClassElement#getClass_ <em>Class</em>}</li>
 * </ul>
 *
 * @see org.opalj.bdl.bDL.BDLPackage#getIssueClassElement()
 * @model
 * @generated
 */
public interface IssueClassElement extends EObject
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
   * @see org.opalj.bdl.bDL.BDLPackage#getIssueClassElement_Name()
   * @model
   * @generated
   */
  String getName();

  /**
   * Sets the value of the '{@link org.opalj.bdl.bDL.IssueClassElement#getName <em>Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Name</em>' attribute.
   * @see #getName()
   * @generated
   */
  void setName(String value);

  /**
   * Returns the value of the '<em><b>Class</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Class</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Class</em>' attribute.
   * @see #setClass(String)
   * @see org.opalj.bdl.bDL.BDLPackage#getIssueClassElement_Class()
   * @model
   * @generated
   */
  String getClass_();

  /**
   * Sets the value of the '{@link org.opalj.bdl.bDL.IssueClassElement#getClass_ <em>Class</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Class</em>' attribute.
   * @see #getClass_()
   * @generated
   */
  void setClass(String value);

} // IssueClassElement
