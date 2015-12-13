/**
 */
package org.opalj.bdl.bDL;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Issue Element</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.opalj.bdl.bDL.IssueElement#getName <em>Name</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.IssueElement#getComment <em>Comment</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.IssueElement#getMessage <em>Message</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.IssueElement#getCategories <em>Categories</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.IssueElement#getKinds <em>Kinds</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.IssueElement#getRelevance <em>Relevance</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.IssueElement#getPackage <em>Package</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.IssueElement#getClass_ <em>Class</em>}</li>
 * </ul>
 *
 * @see org.opalj.bdl.bDL.BDLPackage#getIssueElement()
 * @model
 * @generated
 */
public interface IssueElement extends EObject
{
  /**
   * Returns the value of the '<em><b>Name</b></em>' attribute list.
   * The list contents are of type {@link java.lang.String}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Name</em>' attribute list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Name</em>' attribute list.
   * @see org.opalj.bdl.bDL.BDLPackage#getIssueElement_Name()
   * @model unique="false"
   * @generated
   */
  EList<String> getName();

  /**
   * Returns the value of the '<em><b>Comment</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Comment</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Comment</em>' containment reference.
   * @see #setComment(IssueSuppressComment)
   * @see org.opalj.bdl.bDL.BDLPackage#getIssueElement_Comment()
   * @model containment="true"
   * @generated
   */
  IssueSuppressComment getComment();

  /**
   * Sets the value of the '{@link org.opalj.bdl.bDL.IssueElement#getComment <em>Comment</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Comment</em>' containment reference.
   * @see #getComment()
   * @generated
   */
  void setComment(IssueSuppressComment value);

  /**
   * Returns the value of the '<em><b>Message</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Message</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Message</em>' attribute.
   * @see #setMessage(String)
   * @see org.opalj.bdl.bDL.BDLPackage#getIssueElement_Message()
   * @model
   * @generated
   */
  String getMessage();

  /**
   * Sets the value of the '{@link org.opalj.bdl.bDL.IssueElement#getMessage <em>Message</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Message</em>' attribute.
   * @see #getMessage()
   * @generated
   */
  void setMessage(String value);

  /**
   * Returns the value of the '<em><b>Categories</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Categories</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Categories</em>' containment reference.
   * @see #setCategories(IssueCategoryElement)
   * @see org.opalj.bdl.bDL.BDLPackage#getIssueElement_Categories()
   * @model containment="true"
   * @generated
   */
  IssueCategoryElement getCategories();

  /**
   * Sets the value of the '{@link org.opalj.bdl.bDL.IssueElement#getCategories <em>Categories</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Categories</em>' containment reference.
   * @see #getCategories()
   * @generated
   */
  void setCategories(IssueCategoryElement value);

  /**
   * Returns the value of the '<em><b>Kinds</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Kinds</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Kinds</em>' containment reference.
   * @see #setKinds(IssueKindElement)
   * @see org.opalj.bdl.bDL.BDLPackage#getIssueElement_Kinds()
   * @model containment="true"
   * @generated
   */
  IssueKindElement getKinds();

  /**
   * Sets the value of the '{@link org.opalj.bdl.bDL.IssueElement#getKinds <em>Kinds</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Kinds</em>' containment reference.
   * @see #getKinds()
   * @generated
   */
  void setKinds(IssueKindElement value);

  /**
   * Returns the value of the '<em><b>Relevance</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Relevance</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Relevance</em>' containment reference.
   * @see #setRelevance(IssueRelevanceElement)
   * @see org.opalj.bdl.bDL.BDLPackage#getIssueElement_Relevance()
   * @model containment="true"
   * @generated
   */
  IssueRelevanceElement getRelevance();

  /**
   * Sets the value of the '{@link org.opalj.bdl.bDL.IssueElement#getRelevance <em>Relevance</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Relevance</em>' containment reference.
   * @see #getRelevance()
   * @generated
   */
  void setRelevance(IssueRelevanceElement value);

  /**
   * Returns the value of the '<em><b>Package</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Package</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Package</em>' containment reference.
   * @see #setPackage(IssuePackageElement)
   * @see org.opalj.bdl.bDL.BDLPackage#getIssueElement_Package()
   * @model containment="true"
   * @generated
   */
  IssuePackageElement getPackage();

  /**
   * Sets the value of the '{@link org.opalj.bdl.bDL.IssueElement#getPackage <em>Package</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Package</em>' containment reference.
   * @see #getPackage()
   * @generated
   */
  void setPackage(IssuePackageElement value);

  /**
   * Returns the value of the '<em><b>Class</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Class</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Class</em>' containment reference.
   * @see #setClass(IssueClassElement)
   * @see org.opalj.bdl.bDL.BDLPackage#getIssueElement_Class()
   * @model containment="true"
   * @generated
   */
  IssueClassElement getClass_();

  /**
   * Sets the value of the '{@link org.opalj.bdl.bDL.IssueElement#getClass_ <em>Class</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Class</em>' containment reference.
   * @see #getClass_()
   * @generated
   */
  void setClass(IssueClassElement value);

} // IssueElement
