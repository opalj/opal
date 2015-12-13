/**
 */
package org.opalj.bdl.bDL.util;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;

import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;

import org.eclipse.emf.ecore.EObject;

import org.opalj.bdl.bDL.*;

/**
 * <!-- begin-user-doc -->
 * The <b>Adapter Factory</b> for the model.
 * It provides an adapter <code>createXXX</code> method for each class of the model.
 * <!-- end-user-doc -->
 * @see org.opalj.bdl.bDL.BDLPackage
 * @generated
 */
public class BDLAdapterFactory extends AdapterFactoryImpl
{
  /**
   * The cached model package.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected static BDLPackage modelPackage;

  /**
   * Creates an instance of the adapter factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public BDLAdapterFactory()
  {
    if (modelPackage == null)
    {
      modelPackage = BDLPackage.eINSTANCE;
    }
  }

  /**
   * Returns whether this factory is applicable for the type of the object.
   * <!-- begin-user-doc -->
   * This implementation returns <code>true</code> if the object is either the model's package or is an instance object of the model.
   * <!-- end-user-doc -->
   * @return whether this factory is applicable for the type of the object.
   * @generated
   */
  @Override
  public boolean isFactoryForType(Object object)
  {
    if (object == modelPackage)
    {
      return true;
    }
    if (object instanceof EObject)
    {
      return ((EObject)object).eClass().getEPackage() == modelPackage;
    }
    return false;
  }

  /**
   * The switch that delegates to the <code>createXXX</code> methods.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected BDLSwitch<Adapter> modelSwitch =
    new BDLSwitch<Adapter>()
    {
      @Override
      public Adapter caseModel(Model object)
      {
        return createModelAdapter();
      }
      @Override
      public Adapter caseModelContainer(ModelContainer object)
      {
        return createModelContainerAdapter();
      }
      @Override
      public Adapter caseParameterContainer(ParameterContainer object)
      {
        return createParameterContainerAdapter();
      }
      @Override
      public Adapter caseParameterElement(ParameterElement object)
      {
        return createParameterElementAdapter();
      }
      @Override
      public Adapter caseParameterKeyValueElement(ParameterKeyValueElement object)
      {
        return createParameterKeyValueElementAdapter();
      }
      @Override
      public Adapter caseParameterKeyElement(ParameterKeyElement object)
      {
        return createParameterKeyElementAdapter();
      }
      @Override
      public Adapter caseIssuesContainer(IssuesContainer object)
      {
        return createIssuesContainerAdapter();
      }
      @Override
      public Adapter caseIssuesTitleElement(IssuesTitleElement object)
      {
        return createIssuesTitleElementAdapter();
      }
      @Override
      public Adapter caseIssueElement(IssueElement object)
      {
        return createIssueElementAdapter();
      }
      @Override
      public Adapter caseIssueSuppressComment(IssueSuppressComment object)
      {
        return createIssueSuppressCommentAdapter();
      }
      @Override
      public Adapter caseIssueCategoryElement(IssueCategoryElement object)
      {
        return createIssueCategoryElementAdapter();
      }
      @Override
      public Adapter caseIssueKindElement(IssueKindElement object)
      {
        return createIssueKindElementAdapter();
      }
      @Override
      public Adapter caseIssueRelevanceElement(IssueRelevanceElement object)
      {
        return createIssueRelevanceElementAdapter();
      }
      @Override
      public Adapter caseIssuePackageElement(IssuePackageElement object)
      {
        return createIssuePackageElementAdapter();
      }
      @Override
      public Adapter caseIssueClassElement(IssueClassElement object)
      {
        return createIssueClassElementAdapter();
      }
      @Override
      public Adapter caseIssueCategories(IssueCategories object)
      {
        return createIssueCategoriesAdapter();
      }
      @Override
      public Adapter defaultCase(EObject object)
      {
        return createEObjectAdapter();
      }
    };

  /**
   * Creates an adapter for the <code>target</code>.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param target the object to adapt.
   * @return the adapter for the <code>target</code>.
   * @generated
   */
  @Override
  public Adapter createAdapter(Notifier target)
  {
    return modelSwitch.doSwitch((EObject)target);
  }


  /**
   * Creates a new adapter for an object of class '{@link org.opalj.bdl.bDL.Model <em>Model</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see org.opalj.bdl.bDL.Model
   * @generated
   */
  public Adapter createModelAdapter()
  {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link org.opalj.bdl.bDL.ModelContainer <em>Model Container</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see org.opalj.bdl.bDL.ModelContainer
   * @generated
   */
  public Adapter createModelContainerAdapter()
  {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link org.opalj.bdl.bDL.ParameterContainer <em>Parameter Container</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see org.opalj.bdl.bDL.ParameterContainer
   * @generated
   */
  public Adapter createParameterContainerAdapter()
  {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link org.opalj.bdl.bDL.ParameterElement <em>Parameter Element</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see org.opalj.bdl.bDL.ParameterElement
   * @generated
   */
  public Adapter createParameterElementAdapter()
  {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link org.opalj.bdl.bDL.ParameterKeyValueElement <em>Parameter Key Value Element</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see org.opalj.bdl.bDL.ParameterKeyValueElement
   * @generated
   */
  public Adapter createParameterKeyValueElementAdapter()
  {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link org.opalj.bdl.bDL.ParameterKeyElement <em>Parameter Key Element</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see org.opalj.bdl.bDL.ParameterKeyElement
   * @generated
   */
  public Adapter createParameterKeyElementAdapter()
  {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link org.opalj.bdl.bDL.IssuesContainer <em>Issues Container</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see org.opalj.bdl.bDL.IssuesContainer
   * @generated
   */
  public Adapter createIssuesContainerAdapter()
  {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link org.opalj.bdl.bDL.IssuesTitleElement <em>Issues Title Element</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see org.opalj.bdl.bDL.IssuesTitleElement
   * @generated
   */
  public Adapter createIssuesTitleElementAdapter()
  {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link org.opalj.bdl.bDL.IssueElement <em>Issue Element</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see org.opalj.bdl.bDL.IssueElement
   * @generated
   */
  public Adapter createIssueElementAdapter()
  {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link org.opalj.bdl.bDL.IssueSuppressComment <em>Issue Suppress Comment</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see org.opalj.bdl.bDL.IssueSuppressComment
   * @generated
   */
  public Adapter createIssueSuppressCommentAdapter()
  {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link org.opalj.bdl.bDL.IssueCategoryElement <em>Issue Category Element</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see org.opalj.bdl.bDL.IssueCategoryElement
   * @generated
   */
  public Adapter createIssueCategoryElementAdapter()
  {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link org.opalj.bdl.bDL.IssueKindElement <em>Issue Kind Element</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see org.opalj.bdl.bDL.IssueKindElement
   * @generated
   */
  public Adapter createIssueKindElementAdapter()
  {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link org.opalj.bdl.bDL.IssueRelevanceElement <em>Issue Relevance Element</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see org.opalj.bdl.bDL.IssueRelevanceElement
   * @generated
   */
  public Adapter createIssueRelevanceElementAdapter()
  {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link org.opalj.bdl.bDL.IssuePackageElement <em>Issue Package Element</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see org.opalj.bdl.bDL.IssuePackageElement
   * @generated
   */
  public Adapter createIssuePackageElementAdapter()
  {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link org.opalj.bdl.bDL.IssueClassElement <em>Issue Class Element</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see org.opalj.bdl.bDL.IssueClassElement
   * @generated
   */
  public Adapter createIssueClassElementAdapter()
  {
    return null;
  }

  /**
   * Creates a new adapter for an object of class '{@link org.opalj.bdl.bDL.IssueCategories <em>Issue Categories</em>}'.
   * <!-- begin-user-doc -->
   * This default implementation returns null so that we can easily ignore cases;
   * it's useful to ignore a case when inheritance will catch all the cases anyway.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @see org.opalj.bdl.bDL.IssueCategories
   * @generated
   */
  public Adapter createIssueCategoriesAdapter()
  {
    return null;
  }

  /**
   * Creates a new adapter for the default case.
   * <!-- begin-user-doc -->
   * This default implementation returns null.
   * <!-- end-user-doc -->
   * @return the new adapter.
   * @generated
   */
  public Adapter createEObjectAdapter()
  {
    return null;
  }

} //BDLAdapterFactory
