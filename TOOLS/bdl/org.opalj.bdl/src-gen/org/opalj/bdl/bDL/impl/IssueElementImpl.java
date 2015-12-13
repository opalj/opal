/**
 */
package org.opalj.bdl.bDL.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EDataTypeEList;

import org.opalj.bdl.bDL.BDLPackage;
import org.opalj.bdl.bDL.IssueCategoryElement;
import org.opalj.bdl.bDL.IssueClassElement;
import org.opalj.bdl.bDL.IssueElement;
import org.opalj.bdl.bDL.IssueKindElement;
import org.opalj.bdl.bDL.IssuePackageElement;
import org.opalj.bdl.bDL.IssueRelevanceElement;
import org.opalj.bdl.bDL.IssueSuppressComment;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Issue Element</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.opalj.bdl.bDL.impl.IssueElementImpl#getName <em>Name</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.impl.IssueElementImpl#getComment <em>Comment</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.impl.IssueElementImpl#getMessage <em>Message</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.impl.IssueElementImpl#getCategories <em>Categories</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.impl.IssueElementImpl#getKinds <em>Kinds</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.impl.IssueElementImpl#getRelevance <em>Relevance</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.impl.IssueElementImpl#getPackage <em>Package</em>}</li>
 *   <li>{@link org.opalj.bdl.bDL.impl.IssueElementImpl#getClass_ <em>Class</em>}</li>
 * </ul>
 *
 * @generated
 */
public class IssueElementImpl extends MinimalEObjectImpl.Container implements IssueElement
{
  /**
   * The cached value of the '{@link #getName() <em>Name</em>}' attribute list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getName()
   * @generated
   * @ordered
   */
  protected EList<String> name;

  /**
   * The cached value of the '{@link #getComment() <em>Comment</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getComment()
   * @generated
   * @ordered
   */
  protected IssueSuppressComment comment;

  /**
   * The default value of the '{@link #getMessage() <em>Message</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getMessage()
   * @generated
   * @ordered
   */
  protected static final String MESSAGE_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getMessage() <em>Message</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getMessage()
   * @generated
   * @ordered
   */
  protected String message = MESSAGE_EDEFAULT;

  /**
   * The cached value of the '{@link #getCategories() <em>Categories</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getCategories()
   * @generated
   * @ordered
   */
  protected IssueCategoryElement categories;

  /**
   * The cached value of the '{@link #getKinds() <em>Kinds</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getKinds()
   * @generated
   * @ordered
   */
  protected IssueKindElement kinds;

  /**
   * The cached value of the '{@link #getRelevance() <em>Relevance</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getRelevance()
   * @generated
   * @ordered
   */
  protected IssueRelevanceElement relevance;

  /**
   * The cached value of the '{@link #getPackage() <em>Package</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getPackage()
   * @generated
   * @ordered
   */
  protected IssuePackageElement package_;

  /**
   * The cached value of the '{@link #getClass_() <em>Class</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getClass_()
   * @generated
   * @ordered
   */
  protected IssueClassElement class_;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected IssueElementImpl()
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
    return BDLPackage.Literals.ISSUE_ELEMENT;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EList<String> getName()
  {
    if (name == null)
    {
      name = new EDataTypeEList<String>(String.class, this, BDLPackage.ISSUE_ELEMENT__NAME);
    }
    return name;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public IssueSuppressComment getComment()
  {
    return comment;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain basicSetComment(IssueSuppressComment newComment, NotificationChain msgs)
  {
    IssueSuppressComment oldComment = comment;
    comment = newComment;
    if (eNotificationRequired())
    {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, BDLPackage.ISSUE_ELEMENT__COMMENT, oldComment, newComment);
      if (msgs == null) msgs = notification; else msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setComment(IssueSuppressComment newComment)
  {
    if (newComment != comment)
    {
      NotificationChain msgs = null;
      if (comment != null)
        msgs = ((InternalEObject)comment).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - BDLPackage.ISSUE_ELEMENT__COMMENT, null, msgs);
      if (newComment != null)
        msgs = ((InternalEObject)newComment).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - BDLPackage.ISSUE_ELEMENT__COMMENT, null, msgs);
      msgs = basicSetComment(newComment, msgs);
      if (msgs != null) msgs.dispatch();
    }
    else if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, BDLPackage.ISSUE_ELEMENT__COMMENT, newComment, newComment));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getMessage()
  {
    return message;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setMessage(String newMessage)
  {
    String oldMessage = message;
    message = newMessage;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, BDLPackage.ISSUE_ELEMENT__MESSAGE, oldMessage, message));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public IssueCategoryElement getCategories()
  {
    return categories;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain basicSetCategories(IssueCategoryElement newCategories, NotificationChain msgs)
  {
    IssueCategoryElement oldCategories = categories;
    categories = newCategories;
    if (eNotificationRequired())
    {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, BDLPackage.ISSUE_ELEMENT__CATEGORIES, oldCategories, newCategories);
      if (msgs == null) msgs = notification; else msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setCategories(IssueCategoryElement newCategories)
  {
    if (newCategories != categories)
    {
      NotificationChain msgs = null;
      if (categories != null)
        msgs = ((InternalEObject)categories).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - BDLPackage.ISSUE_ELEMENT__CATEGORIES, null, msgs);
      if (newCategories != null)
        msgs = ((InternalEObject)newCategories).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - BDLPackage.ISSUE_ELEMENT__CATEGORIES, null, msgs);
      msgs = basicSetCategories(newCategories, msgs);
      if (msgs != null) msgs.dispatch();
    }
    else if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, BDLPackage.ISSUE_ELEMENT__CATEGORIES, newCategories, newCategories));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public IssueKindElement getKinds()
  {
    return kinds;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain basicSetKinds(IssueKindElement newKinds, NotificationChain msgs)
  {
    IssueKindElement oldKinds = kinds;
    kinds = newKinds;
    if (eNotificationRequired())
    {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, BDLPackage.ISSUE_ELEMENT__KINDS, oldKinds, newKinds);
      if (msgs == null) msgs = notification; else msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setKinds(IssueKindElement newKinds)
  {
    if (newKinds != kinds)
    {
      NotificationChain msgs = null;
      if (kinds != null)
        msgs = ((InternalEObject)kinds).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - BDLPackage.ISSUE_ELEMENT__KINDS, null, msgs);
      if (newKinds != null)
        msgs = ((InternalEObject)newKinds).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - BDLPackage.ISSUE_ELEMENT__KINDS, null, msgs);
      msgs = basicSetKinds(newKinds, msgs);
      if (msgs != null) msgs.dispatch();
    }
    else if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, BDLPackage.ISSUE_ELEMENT__KINDS, newKinds, newKinds));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public IssueRelevanceElement getRelevance()
  {
    return relevance;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain basicSetRelevance(IssueRelevanceElement newRelevance, NotificationChain msgs)
  {
    IssueRelevanceElement oldRelevance = relevance;
    relevance = newRelevance;
    if (eNotificationRequired())
    {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, BDLPackage.ISSUE_ELEMENT__RELEVANCE, oldRelevance, newRelevance);
      if (msgs == null) msgs = notification; else msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setRelevance(IssueRelevanceElement newRelevance)
  {
    if (newRelevance != relevance)
    {
      NotificationChain msgs = null;
      if (relevance != null)
        msgs = ((InternalEObject)relevance).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - BDLPackage.ISSUE_ELEMENT__RELEVANCE, null, msgs);
      if (newRelevance != null)
        msgs = ((InternalEObject)newRelevance).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - BDLPackage.ISSUE_ELEMENT__RELEVANCE, null, msgs);
      msgs = basicSetRelevance(newRelevance, msgs);
      if (msgs != null) msgs.dispatch();
    }
    else if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, BDLPackage.ISSUE_ELEMENT__RELEVANCE, newRelevance, newRelevance));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public IssuePackageElement getPackage()
  {
    return package_;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain basicSetPackage(IssuePackageElement newPackage, NotificationChain msgs)
  {
    IssuePackageElement oldPackage = package_;
    package_ = newPackage;
    if (eNotificationRequired())
    {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, BDLPackage.ISSUE_ELEMENT__PACKAGE, oldPackage, newPackage);
      if (msgs == null) msgs = notification; else msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setPackage(IssuePackageElement newPackage)
  {
    if (newPackage != package_)
    {
      NotificationChain msgs = null;
      if (package_ != null)
        msgs = ((InternalEObject)package_).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - BDLPackage.ISSUE_ELEMENT__PACKAGE, null, msgs);
      if (newPackage != null)
        msgs = ((InternalEObject)newPackage).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - BDLPackage.ISSUE_ELEMENT__PACKAGE, null, msgs);
      msgs = basicSetPackage(newPackage, msgs);
      if (msgs != null) msgs.dispatch();
    }
    else if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, BDLPackage.ISSUE_ELEMENT__PACKAGE, newPackage, newPackage));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public IssueClassElement getClass_()
  {
    return class_;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain basicSetClass(IssueClassElement newClass, NotificationChain msgs)
  {
    IssueClassElement oldClass = class_;
    class_ = newClass;
    if (eNotificationRequired())
    {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, BDLPackage.ISSUE_ELEMENT__CLASS, oldClass, newClass);
      if (msgs == null) msgs = notification; else msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setClass(IssueClassElement newClass)
  {
    if (newClass != class_)
    {
      NotificationChain msgs = null;
      if (class_ != null)
        msgs = ((InternalEObject)class_).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - BDLPackage.ISSUE_ELEMENT__CLASS, null, msgs);
      if (newClass != null)
        msgs = ((InternalEObject)newClass).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - BDLPackage.ISSUE_ELEMENT__CLASS, null, msgs);
      msgs = basicSetClass(newClass, msgs);
      if (msgs != null) msgs.dispatch();
    }
    else if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, BDLPackage.ISSUE_ELEMENT__CLASS, newClass, newClass));
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
      case BDLPackage.ISSUE_ELEMENT__COMMENT:
        return basicSetComment(null, msgs);
      case BDLPackage.ISSUE_ELEMENT__CATEGORIES:
        return basicSetCategories(null, msgs);
      case BDLPackage.ISSUE_ELEMENT__KINDS:
        return basicSetKinds(null, msgs);
      case BDLPackage.ISSUE_ELEMENT__RELEVANCE:
        return basicSetRelevance(null, msgs);
      case BDLPackage.ISSUE_ELEMENT__PACKAGE:
        return basicSetPackage(null, msgs);
      case BDLPackage.ISSUE_ELEMENT__CLASS:
        return basicSetClass(null, msgs);
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
      case BDLPackage.ISSUE_ELEMENT__NAME:
        return getName();
      case BDLPackage.ISSUE_ELEMENT__COMMENT:
        return getComment();
      case BDLPackage.ISSUE_ELEMENT__MESSAGE:
        return getMessage();
      case BDLPackage.ISSUE_ELEMENT__CATEGORIES:
        return getCategories();
      case BDLPackage.ISSUE_ELEMENT__KINDS:
        return getKinds();
      case BDLPackage.ISSUE_ELEMENT__RELEVANCE:
        return getRelevance();
      case BDLPackage.ISSUE_ELEMENT__PACKAGE:
        return getPackage();
      case BDLPackage.ISSUE_ELEMENT__CLASS:
        return getClass_();
    }
    return super.eGet(featureID, resolve, coreType);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @SuppressWarnings("unchecked")
  @Override
  public void eSet(int featureID, Object newValue)
  {
    switch (featureID)
    {
      case BDLPackage.ISSUE_ELEMENT__NAME:
        getName().clear();
        getName().addAll((Collection<? extends String>)newValue);
        return;
      case BDLPackage.ISSUE_ELEMENT__COMMENT:
        setComment((IssueSuppressComment)newValue);
        return;
      case BDLPackage.ISSUE_ELEMENT__MESSAGE:
        setMessage((String)newValue);
        return;
      case BDLPackage.ISSUE_ELEMENT__CATEGORIES:
        setCategories((IssueCategoryElement)newValue);
        return;
      case BDLPackage.ISSUE_ELEMENT__KINDS:
        setKinds((IssueKindElement)newValue);
        return;
      case BDLPackage.ISSUE_ELEMENT__RELEVANCE:
        setRelevance((IssueRelevanceElement)newValue);
        return;
      case BDLPackage.ISSUE_ELEMENT__PACKAGE:
        setPackage((IssuePackageElement)newValue);
        return;
      case BDLPackage.ISSUE_ELEMENT__CLASS:
        setClass((IssueClassElement)newValue);
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
      case BDLPackage.ISSUE_ELEMENT__NAME:
        getName().clear();
        return;
      case BDLPackage.ISSUE_ELEMENT__COMMENT:
        setComment((IssueSuppressComment)null);
        return;
      case BDLPackage.ISSUE_ELEMENT__MESSAGE:
        setMessage(MESSAGE_EDEFAULT);
        return;
      case BDLPackage.ISSUE_ELEMENT__CATEGORIES:
        setCategories((IssueCategoryElement)null);
        return;
      case BDLPackage.ISSUE_ELEMENT__KINDS:
        setKinds((IssueKindElement)null);
        return;
      case BDLPackage.ISSUE_ELEMENT__RELEVANCE:
        setRelevance((IssueRelevanceElement)null);
        return;
      case BDLPackage.ISSUE_ELEMENT__PACKAGE:
        setPackage((IssuePackageElement)null);
        return;
      case BDLPackage.ISSUE_ELEMENT__CLASS:
        setClass((IssueClassElement)null);
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
      case BDLPackage.ISSUE_ELEMENT__NAME:
        return name != null && !name.isEmpty();
      case BDLPackage.ISSUE_ELEMENT__COMMENT:
        return comment != null;
      case BDLPackage.ISSUE_ELEMENT__MESSAGE:
        return MESSAGE_EDEFAULT == null ? message != null : !MESSAGE_EDEFAULT.equals(message);
      case BDLPackage.ISSUE_ELEMENT__CATEGORIES:
        return categories != null;
      case BDLPackage.ISSUE_ELEMENT__KINDS:
        return kinds != null;
      case BDLPackage.ISSUE_ELEMENT__RELEVANCE:
        return relevance != null;
      case BDLPackage.ISSUE_ELEMENT__PACKAGE:
        return package_ != null;
      case BDLPackage.ISSUE_ELEMENT__CLASS:
        return class_ != null;
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
    result.append(", message: ");
    result.append(message);
    result.append(')');
    return result.toString();
  }

} //IssueElementImpl
