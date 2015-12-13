/**
 */
package org.opalj.bdl.bDL;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Model Container</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.opalj.bdl.bDL.ModelContainer#getName <em>Name</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.ModelContainer#getParameter <em>Parameter</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.ModelContainer#getIssues <em>Issues</em>}</li>
 * </ul>
 *
 * @see org.opalj.bdl.bDL.BDLPackage#getModelContainer()
 * @model
 * @generated
 */
public interface ModelContainer extends Model
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
   * @see org.opalj.bdl.bDL.BDLPackage#getModelContainer_Name()
   * @model
   * @generated
   */
  String getName();

  /**
   * Sets the value of the '{@link org.opalj.bdl.bDL.ModelContainer#getName <em>Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Name</em>' attribute.
   * @see #getName()
   * @generated
   */
  void setName(String value);

  /**
   * Returns the value of the '<em><b>Parameter</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Parameter</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Parameter</em>' containment reference.
   * @see #setParameter(ParameterContainer)
   * @see org.opalj.bdl.bDL.BDLPackage#getModelContainer_Parameter()
   * @model containment="true"
   * @generated
   */
  ParameterContainer getParameter();

  /**
   * Sets the value of the '{@link org.opalj.bdl.bDL.ModelContainer#getParameter <em>Parameter</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Parameter</em>' containment reference.
   * @see #getParameter()
   * @generated
   */
  void setParameter(ParameterContainer value);

  /**
   * Returns the value of the '<em><b>Issues</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Issues</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Issues</em>' containment reference.
   * @see #setIssues(IssuesContainer)
   * @see org.opalj.bdl.bDL.BDLPackage#getModelContainer_Issues()
   * @model containment="true"
   * @generated
   */
  IssuesContainer getIssues();

  /**
   * Sets the value of the '{@link org.opalj.bdl.bDL.ModelContainer#getIssues <em>Issues</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Issues</em>' containment reference.
   * @see #getIssues()
   * @generated
   */
  void setIssues(IssuesContainer value);

} // ModelContainer
