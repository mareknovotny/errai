/*
 * Copyright 2011 JBoss, a divison Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.ioc.rebind.ioc;

import java.lang.annotation.Annotation;

import org.jboss.errai.ioc.rebind.IOCGenerator;
import org.jboss.errai.ioc.rebind.IOCProcessingContext;
import org.jboss.errai.ioc.rebind.ioc.codegen.AnnotationEncoder;
import org.jboss.errai.ioc.rebind.ioc.codegen.Statement;
import org.jboss.errai.ioc.rebind.ioc.codegen.meta.MetaClass;
import org.jboss.errai.ioc.rebind.ioc.codegen.meta.MetaField;
import org.jboss.errai.ioc.rebind.ioc.codegen.meta.MetaParameter;
import org.jboss.errai.ioc.rebind.ioc.codegen.meta.MetaParameterizedType;
import org.jboss.errai.ioc.rebind.ioc.codegen.meta.MetaType;
import org.jboss.errai.ioc.rebind.ioc.codegen.meta.impl.java.JavaReflectionField;
import org.jboss.errai.ioc.rebind.ioc.codegen.meta.impl.java.JavaReflectionParameterizedType;
import org.jboss.errai.ioc.rebind.ioc.codegen.util.Refs;
import org.jboss.errai.ioc.rebind.ioc.codegen.util.Stmt;

public class ContextualProviderInjector extends TypeInjector {
  private final Injector providerInjector;

  public ContextualProviderInjector(MetaClass type, MetaClass providerType) {
    super(type);
    this.providerInjector = new TypeInjector(providerType);
  }

  @Override
  public Statement getType(InjectionContext injectContext, InjectionPoint injectionPoint) {
    injected = true;

    MetaClass type = null;
    MetaParameterizedType pType = null;

    switch (injectionPoint.getTaskType()) {
      case PrivateField:
      case Field:
        MetaField field = injectionPoint.getField();
        type = field.getType();

        pType = type.getParameterizedType();
        // TODO refactor!
        if (pType == null && field instanceof JavaReflectionField) {
          pType = (JavaReflectionParameterizedType) field.getGenericType();
        }
        break;

      case Parameter:
        MetaParameter parm = injectionPoint.getParm();
        type = parm.getType();

        pType = type.getParameterizedType();
        break;
    }

    IOCProcessingContext processingContext = injectContext.getProcessingContext();

    if (pType == null) {
      processingContext.append(
              Stmt.create().nestedCall(providerInjector.getType(injectContext, injectionPoint))
                      .invoke("provide", new Class[0])
      );
    }
    else {
      MetaType[] typeArgs = pType.getTypeParameters();

      Annotation[] qualifiers = injectionPoint.getQualifiers();
      if (qualifiers.length != 0) {

        processingContext.append(
                Stmt.create().nestedCall(providerInjector.getType(injectContext, injectionPoint))
                        .invoke("provide", typeArgs, qualifiers)
        );
      }
      else {
        processingContext.append(
                Stmt.create().nestedCall(providerInjector.getType(injectContext, injectionPoint))
                        .invoke("provide", typeArgs, null)
        );
      }
    }

    return Refs.get(varName);
  }

  @Override
  public Statement instantiateOnly(InjectionContext injectContext, InjectionPoint injectionPoint) {
    injected = true;
    return providerInjector.getType(injectContext, injectionPoint);
  }
}