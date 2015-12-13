/**
 */
package org.opalj.bdl.bDL;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Issue Package Element</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.opalj.bdl.bDL.IssuePackageElement#getName <em>Name</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.IssuePackageElement#getPackage <em>Package</em>}</li>
 * </ul>
 *
 * @see org.opalj.bdl.bDL.BDLPackage#getIssuePackageElement()
 * @model
 * @generated
 */
public interface IssuePackageElement extends EObject
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
   * @see org.opalj.bdl.bDL.BDLPackage#getIssuePackageElement_Name()
   * @model
   * @generated
   */
  String getName();

  /**
   * Sets the value of the '{@link org.opalj.bdl.bDL.IssuePackageElement#getName <em>Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Name</em>' attribute.
   * @see #getName()
   * @generated
   */
  void setName(String value);

  /**
   * Returns the value of the '<em><b>Package</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Package</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Package</em>' attribute.
   * @see #setPackage(String)
   * @see org.opalj.bdl.bDL.BDLPackage#getIssuePackageElement_Package()
   * @model
   * @generated
   */
  String getPackage();

  /**
   * Sets the value of the '{@link org.opalj.bdl.bDL.IssuePackageElement#getPackage <em>Package</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Package</em>' attribute.
   * @see #getPackage()
   * @generated
   */
  void setPackage(String value);

} // IssuePackageElement
