/**
 */
package org.opalj.bdl.bDL;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Parameter Container</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.opalj.bdl.bDL.ParameterContainer#getName <em>Name</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.ParameterContainer#getElements <em>Elements</em>}</li>
 * </ul>
 *
 * @see org.opalj.bdl.bDL.BDLPackage#getParameterContainer()
 * @model
 * @generated
 */
public interface ParameterContainer extends EObject
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
   * @see org.opalj.bdl.bDL.BDLPackage#getParameterContainer_Name()
   * @model
   * @generated
   */
  String getName();

  /**
   * Sets the value of the '{@link org.opalj.bdl.bDL.ParameterContainer#getName <em>Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Name</em>' attribute.
   * @see #getName()
   * @generated
   */
  void setName(String value);

  /**
   * Returns the value of the '<em><b>Elements</b></em>' containment reference list.
   * The list contents are of type {@link org.opalj.bdl.bDL.ParameterElement}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Elements</em>' containment reference list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Elements</em>' containment reference list.
   * @see org.opalj.bdl.bDL.BDLPackage#getParameterContainer_Elements()
   * @model containment="true"
   * @generated
   */
  EList<ParameterElement> getElements();

} // ParameterContainer
