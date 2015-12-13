/**
 */
package org.opalj.bdl.bDL.impl;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.opalj.bdl.bDL.BDLPackage;
import org.opalj.bdl.bDL.IssueRelevanceElement;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Issue Relevance Element</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.opalj.bdl.bDL.impl.IssueRelevanceElementImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.impl.IssueRelevanceElementImpl#getRelevance <em>Relevance</em>}</li>
 * </ul>
 *
 * @generated
 */
public class IssueRelevanceElementImpl extends MinimalEObjectImpl.Container implements IssueRelevanceElement
{
  /**
   * The default value of the '{@link #getName() <em>Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getName()
   * @generated
   * @ordered
   */
  protected static final String NAME_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getName()
   * @generated
   * @ordered
   */
  protected String name = NAME_EDEFAULT;

  /**
   * The default value of the '{@link #getRelevance() <em>Relevance</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getRelevance()
   * @generated
   * @ordered
   */
  protected static final int RELEVANCE_EDEFAULT = 0;

  /**
   * The cached value of the '{@link #getRelevance() <em>Relevance</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getRelevance()
   * @generated
   * @ordered
   */
  protected int relevance = RELEVANCE_EDEFAULT;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected IssueRelevanceElementImpl()
  {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  protected EClass eStaticClass()
  {
    return BDLPackage.Literals.ISSUE_RELEVANCE_ELEMENT;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getName()
  {
    return name;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setName(String newName)
  {
    String oldName = name;
    name = newName;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, BDLPackage.ISSUE_RELEVANCE_ELEMENT__NAME, oldName, name));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public int getRelevance()
  {
    return relevance;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setRelevance(int newRelevance)
  {
    int oldRelevance = relevance;
    relevance = newRelevance;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, BDLPackage.ISSUE_RELEVANCE_ELEMENT__RELEVANCE, oldRelevance, relevance));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Object eGet(int featureID, boolean resolve, boolean coreType)
  {
    switch (featureID)
    {
      case BDLPackage.ISSUE_RELEVANCE_ELEMENT__NAME:
        return getName();
      case BDLPackage.ISSUE_RELEVANCE_ELEMENT__RELEVANCE:
        return getRelevance();
    }
    return super.eGet(featureID, resolve, coreType);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public void eSet(int featureID, Object newValue)
  {
    switch (featureID)
    {
      case BDLPackage.ISSUE_RELEVANCE_ELEMENT__NAME:
        setName((String)newValue);
        return;
      case BDLPackage.ISSUE_RELEVANCE_ELEMENT__RELEVANCE:
        setRelevance((Integer)newValue);
        return;
    }
    super.eSet(featureID, newValue);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public void eUnset(int featureID)
  {
    switch (featureID)
    {
      case BDLPackage.ISSUE_RELEVANCE_ELEMENT__NAME:
        setName(NAME_EDEFAULT);
        return;
      case BDLPackage.ISSUE_RELEVANCE_ELEMENT__RELEVANCE:
        setRelevance(RELEVANCE_EDEFAULT);
        return;
    }
    super.eUnset(featureID);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public boolean eIsSet(int featureID)
  {
    switch (featureID)
    {
      case BDLPackage.ISSUE_RELEVANCE_ELEMENT__NAME:
        return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
      case BDLPackage.ISSUE_RELEVANCE_ELEMENT__RELEVANCE:
        return relevance != RELEVANCE_EDEFAULT;
    }
    return super.eIsSet(featureID);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public String toString()
  {
    if (eIsProxy()) return super.toString();

    StringBuffer result = new StringBuffer(super.toString());
    result.append(" (name: ");
    result.append(name);
    result.append(", relevance: ");
    result.append(relevance);
    result.append(')');
    return result.toString();
  }

} //IssueRelevanceElementImpl
