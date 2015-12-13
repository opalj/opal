/**
 */
package org.opalj.bdl.bDL.impl;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.opalj.bdl.bDL.BDLPackage;
import org.opalj.bdl.bDL.IssueCategories;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Issue Categories</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.opalj.bdl.bDL.impl.IssueCategoriesImpl#getBug <em>Bug</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.impl.IssueCategoriesImpl#getSmell <em>Smell</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.impl.IssueCategoriesImpl#getPerformance <em>Performance</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.impl.IssueCategoriesImpl#getComprehensibility <em>Comprehensibility</em>}</li>
 * </ul>
 *
 * @generated
 */
public class IssueCategoriesImpl extends MinimalEObjectImpl.Container implements IssueCategories
{
  /**
   * The default value of the '{@link #getBug() <em>Bug</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getBug()
   * @generated
   * @ordered
   */
  protected static final String BUG_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getBug() <em>Bug</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getBug()
   * @generated
   * @ordered
   */
  protected String bug = BUG_EDEFAULT;

  /**
   * The default value of the '{@link #getSmell() <em>Smell</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getSmell()
   * @generated
   * @ordered
   */
  protected static final String SMELL_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getSmell() <em>Smell</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getSmell()
   * @generated
   * @ordered
   */
  protected String smell = SMELL_EDEFAULT;

  /**
   * The default value of the '{@link #getPerformance() <em>Performance</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getPerformance()
   * @generated
   * @ordered
   */
  protected static final String PERFORMANCE_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getPerformance() <em>Performance</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getPerformance()
   * @generated
   * @ordered
   */
  protected String performance = PERFORMANCE_EDEFAULT;

  /**
   * The default value of the '{@link #getComprehensibility() <em>Comprehensibility</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getComprehensibility()
   * @generated
   * @ordered
   */
  protected static final String COMPREHENSIBILITY_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getComprehensibility() <em>Comprehensibility</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getComprehensibility()
   * @generated
   * @ordered
   */
  protected String comprehensibility = COMPREHENSIBILITY_EDEFAULT;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected IssueCategoriesImpl()
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
    return BDLPackage.Literals.ISSUE_CATEGORIES;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getBug()
  {
    return bug;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setBug(String newBug)
  {
    String oldBug = bug;
    bug = newBug;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, BDLPackage.ISSUE_CATEGORIES__BUG, oldBug, bug));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getSmell()
  {
    return smell;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setSmell(String newSmell)
  {
    String oldSmell = smell;
    smell = newSmell;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, BDLPackage.ISSUE_CATEGORIES__SMELL, oldSmell, smell));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getPerformance()
  {
    return performance;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setPerformance(String newPerformance)
  {
    String oldPerformance = performance;
    performance = newPerformance;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, BDLPackage.ISSUE_CATEGORIES__PERFORMANCE, oldPerformance, performance));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getComprehensibility()
  {
    return comprehensibility;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setComprehensibility(String newComprehensibility)
  {
    String oldComprehensibility = comprehensibility;
    comprehensibility = newComprehensibility;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, BDLPackage.ISSUE_CATEGORIES__COMPREHENSIBILITY, oldComprehensibility, comprehensibility));
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
      case BDLPackage.ISSUE_CATEGORIES__BUG:
        return getBug();
      case BDLPackage.ISSUE_CATEGORIES__SMELL:
        return getSmell();
      case BDLPackage.ISSUE_CATEGORIES__PERFORMANCE:
        return getPerformance();
      case BDLPackage.ISSUE_CATEGORIES__COMPREHENSIBILITY:
        return getComprehensibility();
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
      case BDLPackage.ISSUE_CATEGORIES__BUG:
        setBug((String)newValue);
        return;
      case BDLPackage.ISSUE_CATEGORIES__SMELL:
        setSmell((String)newValue);
        return;
      case BDLPackage.ISSUE_CATEGORIES__PERFORMANCE:
        setPerformance((String)newValue);
        return;
      case BDLPackage.ISSUE_CATEGORIES__COMPREHENSIBILITY:
        setComprehensibility((String)newValue);
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
      case BDLPackage.ISSUE_CATEGORIES__BUG:
        setBug(BUG_EDEFAULT);
        return;
      case BDLPackage.ISSUE_CATEGORIES__SMELL:
        setSmell(SMELL_EDEFAULT);
        return;
      case BDLPackage.ISSUE_CATEGORIES__PERFORMANCE:
        setPerformance(PERFORMANCE_EDEFAULT);
        return;
      case BDLPackage.ISSUE_CATEGORIES__COMPREHENSIBILITY:
        setComprehensibility(COMPREHENSIBILITY_EDEFAULT);
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
      case BDLPackage.ISSUE_CATEGORIES__BUG:
        return BUG_EDEFAULT == null ? bug != null : !BUG_EDEFAULT.equals(bug);
      case BDLPackage.ISSUE_CATEGORIES__SMELL:
        return SMELL_EDEFAULT == null ? smell != null : !SMELL_EDEFAULT.equals(smell);
      case BDLPackage.ISSUE_CATEGORIES__PERFORMANCE:
        return PERFORMANCE_EDEFAULT == null ? performance != null : !PERFORMANCE_EDEFAULT.equals(performance);
      case BDLPackage.ISSUE_CATEGORIES__COMPREHENSIBILITY:
        return COMPREHENSIBILITY_EDEFAULT == null ? comprehensibility != null : !COMPREHENSIBILITY_EDEFAULT.equals(comprehensibility);
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
    result.append(" (bug: ");
    result.append(bug);
    result.append(", smell: ");
    result.append(smell);
    result.append(", performance: ");
    result.append(performance);
    result.append(", comprehensibility: ");
    result.append(comprehensibility);
    result.append(')');
    return result.toString();
  }

} //IssueCategoriesImpl
