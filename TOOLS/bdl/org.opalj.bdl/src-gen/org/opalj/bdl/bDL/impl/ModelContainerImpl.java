/**
 */
package org.opalj.bdl.bDL.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

import org.opalj.bdl.bDL.BDLPackage;
import org.opalj.bdl.bDL.IssuesContainer;
import org.opalj.bdl.bDL.ModelContainer;
import org.opalj.bdl.bDL.ParameterContainer;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Model Container</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.opalj.bdl.bDL.impl.ModelContainerImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.impl.ModelContainerImpl#getParameter <em>Parameter</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.impl.ModelContainerImpl#getIssues <em>Issues</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ModelContainerImpl extends ModelImpl implements ModelContainer
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
   * The cached value of the '{@link #getParameter() <em>Parameter</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getParameter()
   * @generated
   * @ordered
   */
  protected ParameterContainer parameter;

  /**
   * The cached value of the '{@link #getIssues() <em>Issues</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getIssues()
   * @generated
   * @ordered
   */
  protected IssuesContainer issues;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected ModelContainerImpl()
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
    return BDLPackage.Literals.MODEL_CONTAINER;
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
      eNotify(new ENotificationImpl(this, Notification.SET, BDLPackage.MODEL_CONTAINER__NAME, oldName, name));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ParameterContainer getParameter()
  {
    return parameter;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain basicSetParameter(ParameterContainer newParameter, NotificationChain msgs)
  {
    ParameterContainer oldParameter = parameter;
    parameter = newParameter;
    if (eNotificationRequired())
    {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, BDLPackage.MODEL_CONTAINER__PARAMETER, oldParameter, newParameter);
      if (msgs == null) msgs = notification; else msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setParameter(ParameterContainer newParameter)
  {
    if (newParameter != parameter)
    {
      NotificationChain msgs = null;
      if (parameter != null)
        msgs = ((InternalEObject)parameter).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - BDLPackage.MODEL_CONTAINER__PARAMETER, null, msgs);
      if (newParameter != null)
        msgs = ((InternalEObject)newParameter).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - BDLPackage.MODEL_CONTAINER__PARAMETER, null, msgs);
      msgs = basicSetParameter(newParameter, msgs);
      if (msgs != null) msgs.dispatch();
    }
    else if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, BDLPackage.MODEL_CONTAINER__PARAMETER, newParameter, newParameter));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public IssuesContainer getIssues()
  {
    return issues;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain basicSetIssues(IssuesContainer newIssues, NotificationChain msgs)
  {
    IssuesContainer oldIssues = issues;
    issues = newIssues;
    if (eNotificationRequired())
    {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, BDLPackage.MODEL_CONTAINER__ISSUES, oldIssues, newIssues);
      if (msgs == null) msgs = notification; else msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setIssues(IssuesContainer newIssues)
  {
    if (newIssues != issues)
    {
      NotificationChain msgs = null;
      if (issues != null)
        msgs = ((InternalEObject)issues).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - BDLPackage.MODEL_CONTAINER__ISSUES, null, msgs);
      if (newIssues != null)
        msgs = ((InternalEObject)newIssues).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - BDLPackage.MODEL_CONTAINER__ISSUES, null, msgs);
      msgs = basicSetIssues(newIssues, msgs);
      if (msgs != null) msgs.dispatch();
    }
    else if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, BDLPackage.MODEL_CONTAINER__ISSUES, newIssues, newIssues));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs)
  {
    switch (featureID)
    {
      case BDLPackage.MODEL_CONTAINER__PARAMETER:
        return basicSetParameter(null, msgs);
      case BDLPackage.MODEL_CONTAINER__ISSUES:
        return basicSetIssues(null, msgs);
    }
    return super.eInverseRemove(otherEnd, featureID, msgs);
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
      case BDLPackage.MODEL_CONTAINER__NAME:
        return getName();
      case BDLPackage.MODEL_CONTAINER__PARAMETER:
        return getParameter();
      case BDLPackage.MODEL_CONTAINER__ISSUES:
        return getIssues();
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
      case BDLPackage.MODEL_CONTAINER__NAME:
        setName((String)newValue);
        return;
      case BDLPackage.MODEL_CONTAINER__PARAMETER:
        setParameter((ParameterContainer)newValue);
        return;
      case BDLPackage.MODEL_CONTAINER__ISSUES:
        setIssues((IssuesContainer)newValue);
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
      case BDLPackage.MODEL_CONTAINER__NAME:
        setName(NAME_EDEFAULT);
        return;
      case BDLPackage.MODEL_CONTAINER__PARAMETER:
        setParameter((ParameterContainer)null);
        return;
      case BDLPackage.MODEL_CONTAINER__ISSUES:
        setIssues((IssuesContainer)null);
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
      case BDLPackage.MODEL_CONTAINER__NAME:
        return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
      case BDLPackage.MODEL_CONTAINER__PARAMETER:
        return parameter != null;
      case BDLPackage.MODEL_CONTAINER__ISSUES:
        return issues != null;
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
    result.append(')');
    return result.toString();
  }

} //ModelContainerImpl
