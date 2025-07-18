/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.drools.mvel.asm;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.drools.base.base.ClassFieldInspector;
import org.drools.util.ClassUtils;
import org.drools.util.MethodUtils;
import org.drools.drl.parser.MessageImpl;
import org.kie.api.io.Resource;
import org.kie.internal.builder.InternalMessage;
import org.kie.internal.builder.KnowledgeBuilderResult;
import org.kie.internal.builder.ResultSeverity;
import org.mvel2.asm.AnnotationVisitor;
import org.mvel2.asm.Attribute;
import org.mvel2.asm.ClassReader;
import org.mvel2.asm.ClassVisitor;
import org.mvel2.asm.FieldVisitor;
import org.mvel2.asm.MethodVisitor;
import org.mvel2.asm.Opcodes;
import org.mvel2.asm.Type;

import static org.drools.util.StringUtils.lcFirstForBean;

/**
 * Visit a POJO user class, and extract the property getter methods that are public, in the 
 * order in which they are declared actually in the class itself (not using introspection).
 *
 * This may be enhanced in the future to allow annotations or perhaps external meta data
 * configure the order of the indexes, as this may provide fine tuning options in special cases.
 */
public class ClassFieldInspectorImpl implements ClassFieldInspector {

    private final Map<String, Integer> fieldNames = new HashMap<>();
    private final Map<String, Class<?>> fieldTypes = new HashMap<>();
    private final Map<String, Field> fieldTypesField = new HashMap<>();
    private final Map<String, Method> getterMethods = new HashMap<>();
    private final Map<String, Method> setterMethods = new HashMap<>();
    private final Set<String> nonGetters = new HashSet<>();
    private final Class<?> classUnderInspection;
    private Map<String, Collection<KnowledgeBuilderResult>> results = null;

    /**
     * @param classUnderInspection The class that the fields to be shadowed are extracted for.
     * @throws IOException
     */
    public ClassFieldInspectorImpl( final Class< ? > classUnderInspection) throws IOException {
        this( classUnderInspection, true );
    }

    public ClassFieldInspectorImpl( final Class< ? > classUnderInspection,
                                    final boolean includeFinalMethods) throws IOException {
        this.classUnderInspection = classUnderInspection;
        this.fieldNames.put("this", 0);
        this.fieldTypes.put("this", classUnderInspection);

        final String name = getResourcePath( classUnderInspection );
        final InputStream stream = classUnderInspection.getResourceAsStream( name );

        if ( stream != null ) {
            try {
                processClassWithByteCode( classUnderInspection, stream, includeFinalMethods );
            } finally {
                stream.close();
            }
        } else {
            processClassWithoutByteCode( classUnderInspection, includeFinalMethods );
        }
    }

    /** Walk up the inheritance hierarchy recursively, reading in fields */
    private void processClassWithByteCode( final Class< ? > clazz,
                                           final InputStream stream,
                                           final boolean includeFinalMethods ) throws IOException {

        final ClassReader reader = new ClassReader( stream );
        final ClassFieldVisitor visitor = new ClassFieldVisitor( clazz, includeFinalMethods, this );
        reader.accept( visitor,
                       0 );
        if ( clazz.getSuperclass() != null ) {
            final String name = getResourcePath( clazz.getSuperclass() );
            final InputStream parentStream = clazz.getResourceAsStream( name );
            if ( parentStream != null ) {
                try {
                    processClassWithByteCode( clazz.getSuperclass(), parentStream, includeFinalMethods );
                } finally {
                    parentStream.close();
                }
            } else {
                processClassWithoutByteCode( clazz.getSuperclass(), includeFinalMethods );
            }
        }
        if ( clazz.isInterface() ) {
            final Class< ? >[] interfaces = clazz.getInterfaces();
            for ( Class<?> anInterface : interfaces ) {
                final String name = getResourcePath( anInterface );
                final InputStream parentStream = clazz.getResourceAsStream( name );
                if ( parentStream != null ) {
                    try {
                        processClassWithByteCode( anInterface, parentStream, includeFinalMethods );
                    } finally {
                        parentStream.close();
                    }
                } else {
                    processClassWithoutByteCode( anInterface, includeFinalMethods );
                }
            }
        }
    }

    private int currentFieldIndex() {
        return fieldNames.size();
    }

    private void processClassWithoutByteCode( final Class< ? > clazz,
                                              final boolean includeFinalMethods ) {
        final List<Method> methods = Arrays.asList( clazz.getMethods() );
        // different JVMs might return the methods in different order, so has to be sorted in order 
        // to be compatible with all JVMs
        methods.sort((m1, m2) -> {
            String n1 = m1.getName();
            String n2 = m2.getName();
            if (n1.equals(n2) && m1.getDeclaringClass() != m2.getDeclaringClass()) {
                return m1.getDeclaringClass().isAssignableFrom(m2.getDeclaringClass()) ? -1 : 1;
            } else {
                return n1.compareTo(n2);
            }
        });

        final int mask = includeFinalMethods ? Modifier.PUBLIC : Modifier.PUBLIC | Modifier.FINAL;
        for ( Method method : methods ) {
            if ( ( method.getModifiers() & mask ) == Opcodes.ACC_PUBLIC ) {
                if ( method.getParameterTypes().length == 0 &&
                     !method.getName().equals( "<init>" ) && !method.getName().equals( "<clinit>" ) &&
                     method.getReturnType() != void.class ) {

                    // want public methods that start with 'get' or 'is' and have no args, and return a value
                    addToMapping( method, currentFieldIndex() );

                } else if ( method.getParameterTypes().length == 1 && method.getName().startsWith( "set" ) ) {

                    // want public methods that start with 'set' and have one arg
                    addToMapping( method, currentFieldIndex() );
                }
            }
        }

        final List<Field> flds = Arrays.asList( clazz.getFields() );
        Collections.sort( flds, Comparator.comparing(Field::getName) );

        for ( Field fld : flds ) {
            if ( ! Modifier.isStatic( fld.getModifiers() ) && ! fieldNames.containsKey( fld.getName() ) ) {
                this.fieldNames.put( fld.getName(), currentFieldIndex() );
                this.fieldTypes.put( fld.getName(), fld.getType() );
                this.fieldTypesField.put( fld.getName(), fld );
            }
        }
    }

    /**
     * Convert it to a form so we can load the bytes from the classpath.
     */
    private String getResourcePath( final Class< ? > clazz ) {
        return "/" + clazz.getCanonicalName() + ".class";
    }

    /**
     * Return a mapping of the field "names" (ie bean property name convention)
     * to the numerical index by which they can be accessed.
     */
    public Map<String, Integer> getFieldNames() {
        return this.fieldNames;
    }

    /**
     * sotty:
     * Checks whether a returned field is actually a getter or not
     *
     * @param name the field to test
     * @return true id the name does not correspond to a getter field
     */
    public boolean isNonGetter( String name ) {
        return nonGetters.contains( name );
    }

    /**
     * @return A mapping of field types (unboxed).
     */
    public Map<String, Field> getFieldTypesField() {
        return this.fieldTypesField;
    }

    /**
     * @return A mapping of field types (unboxed).
     */
    public Map<String, Class< ? >> getFieldTypes() {
        return this.fieldTypes;
    }

    public Class< ? > getFieldType(String name) {
        return this.fieldTypes.get(name);
    }

    /**
     * @return A mapping of methods for the getters. 
     */
    public Map<String, Method> getGetterMethods() {
        return this.getterMethods;
    }

    /**
     * @return A mapping of methods for the getters. 
     */
    public Map<String, Method> getSetterMethods() {
        return this.setterMethods;
    }

    private void addToMapping( Method method, int index ) {
        final String name = method.getName();
        int offset = 0;
        if ( name.startsWith( "is" ) ) {
            offset = 2;
        } else if ( name.startsWith( "get" ) || name.startsWith( "set" ) ) {
            offset = 3;
        }
        final String fieldName = calcFieldName( name, offset );
        if ( this.fieldNames.containsKey( fieldName ) ) {
            //only want it once, the first one thats found
            if ( offset != 0 && this.nonGetters.contains( fieldName ) ) {
                //replace the non getter method with the getter one
                Integer oldIndex = removeOldField( fieldName );
                storeField( oldIndex, fieldName );
                storeGetterSetter( method, fieldName );
                this.nonGetters.remove( fieldName );
            } else if ( offset != 0 ) {
                storeGetterSetter( method, fieldName );
            }
        } else {
            storeField( index, fieldName );
            storeGetterSetter( method, fieldName );

            if ( offset == 0 ) {
                // only if it is a non-standard getter method
                this.nonGetters.add( fieldName );
            }
        }
    }

    private Integer removeOldField( final String fieldName ) {
        Integer index = this.fieldNames.remove( fieldName );
        this.fieldTypes.remove( fieldName );
        this.getterMethods.remove( fieldName );
        return index;

    }

    private void storeField( final Integer index,
                             final String fieldName ) {
        this.fieldNames.put( fieldName,
                             index );
    }
    
    //class.getDeclaredField(String) doesn't walk the inheritance tree; this does
    private Map<String, Field> getAllFields(Class<?> type) {
        Map<String, Field> fields = new HashMap<>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for(Field f : c.getDeclaredFields()) {
                fields.put(f.getName(), f);
            }
        }
        return fields;
    }

    private void storeGetterSetter( Method method,
                                    String fieldName ) {
        Field f =  getAllFields( classUnderInspection ).get( fieldName );
        if ( method.getName().startsWith( "set" ) && method.getParameterTypes().length == 1 ) {
            this.setterMethods.put( fieldName,
                                    method );
            if ( !fieldTypes.containsKey( fieldName ) ) {
                this.fieldTypes.put( fieldName,
                                     method.getParameterTypes()[0] );
            }
            if ( !fieldTypesField.containsKey( fieldName ) ) {
                this.fieldTypesField.put( fieldName,
                                          f );
            }
        } else if( ! void.class.isAssignableFrom( method.getReturnType() ) ) {
            Method existingMethod = getterMethods.get( fieldName );
            if ( existingMethod != null && !MethodUtils.isOverride( existingMethod, method ) ) {
                if (method.getReturnType() != existingMethod.getReturnType()) {
                    if (method.getReturnType().isAssignableFrom(existingMethod.getReturnType())) {
                        // a more specialized getter (covariant overload) has been already indexed, so skip this one
                        return;
                    } else if (existingMethod.getReturnType().isAssignableFrom(method.getReturnType())) {
                        // this method is a more specialized getter. Overwrite with this one
                    } else {
                        // returnType is different so it would likely produce a wrong result
                        addResult( fieldName, new IncompatibleGetterOverloadError( classUnderInspection,
                                                                         this.getterMethods.get( fieldName ).getName(), this.fieldTypes.get( fieldName ),
                                                                         method.getName(), method.getReturnType() ) );
                    }
                } else if (Modifier.isAbstract(method.getModifiers()) && Modifier.isAbstract(existingMethod.getModifiers())) {
                        // If both are abstract, no need of Warning
                } else {
                    Map<String, Integer> accessorPriorityMap = ClassUtils.accessorPriorityMap(fieldName);
                    if (accessorPriorityMap.get(existingMethod.getName()) > accessorPriorityMap.get(method.getName())) {
                        // existingMethod has higher priority
                        return;
                    }
                }
            }
            this.getterMethods.put( fieldName,
                                    method );
            this.fieldTypes.put( fieldName,
                                 method.getReturnType() );
            this.fieldTypesField.put( fieldName,
                                      f );
        }
    }

    private String calcFieldName( String name,
                                  final int offset ) {
        name = name.substring( offset );
        return lcFirstForBean( name );
    }

    public Collection<KnowledgeBuilderResult> getInspectionResults( String fieldName ) {
        return results != null && results.containsKey( fieldName ) ? results.get( fieldName ) : Collections.EMPTY_LIST;
    }

    private void addResult( String fieldName, KnowledgeBuilderResult result ) {
        Map<String, Collection<KnowledgeBuilderResult>> results = getResults();
        Collection<KnowledgeBuilderResult> fieldResults = results.computeIfAbsent(fieldName, k -> new ArrayList<>(3));
        fieldResults.add( result );
    }

    protected Map<String, Collection<KnowledgeBuilderResult>> getResults() {
        if ( results == null ) {
            results = new HashMap<>( );
        }
        return results;
    }

    /**
     * Using the ASM classfield extractor to pluck it out in the order they appear in the class file.
     */
    static class ClassFieldVisitor
            extends
            ClassVisitor {

        private final Class< ? >          clazz;
        private final ClassFieldInspectorImpl inspector;
        private final boolean             includeFinalMethods;

        ClassFieldVisitor(final Class< ? > cls,
                          final boolean includeFinalMethods,
                          final ClassFieldInspectorImpl inspector) {
            super(Opcodes.ASM5);
            this.clazz = cls;
            this.includeFinalMethods = includeFinalMethods;
            this.inspector = inspector;
        }

        public MethodVisitor visitMethod( final int access,
                                          final String name,
                                          final String desc,
                                          final String signature,
                                          final String[] exceptions ) {
            //only want public methods
            //and have no args, and return a value
            final int mask = this.includeFinalMethods ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL;
            if ( (access & mask) == Opcodes.ACC_PUBLIC ) {
                try {
                    if ( desc.startsWith( "()" ) && (!name.equals( "<init>" )) && (!name.equals( "<clinit>" )) ) {// && ( name.startsWith("get") || name.startsWith("is") ) ) {
                        final Method method = this.clazz.getMethod( name, (Class[]) null );
                        if ( method.getReturnType() != void.class ) {
                            this.inspector.addToMapping( method, inspector.currentFieldIndex() );
                        }
                    } else if ( name.startsWith( "set" ) ) {
                        // I found no safe way of getting the method object from the descriptor, so doing the other way around
                        Method[] methods = this.clazz.getMethods();
                        for ( Method method : methods ) {
                            if ( name.equals( method.getName() ) && desc.equals( Type.getMethodDescriptor( method ) ) ) {
                                this.inspector.addToMapping( method, inspector.currentFieldIndex() );
                                break;
                            }
                        }
                    }
                } catch ( final Exception e ) {
                    throw new RuntimeException( "Error getting field access method: " + name + ": " + e.getMessage(),
                                                e );
                }
            }
            return null;
        }

        public void visitInnerClass( final String arg0,
                                     final String arg1,
                                     final String arg2,
                                     final int arg3 ) {
        }

        public void visitAttribute( final Attribute arg0 ) {
        }

        public void visitEnd() {
        }

        public void visit( final int arg0,
                           final int arg1,
                           final String arg2,
                           final String arg3,
                           final String arg4,
                           final String[] arg5 ) {

        }

        public void visitSource( final String arg0,
                                 final String arg1 ) {

        }

        public void visitOuterClass( final String arg0,
                                     final String arg1,
                                     final String arg2 ) {

        }

        public AnnotationVisitor visitAnnotation( final String arg0,
                                                  final boolean arg1 ) {

            return new ClassFieldAnnotationVisitor();
        }

        public FieldVisitor visitField( final int arg0,
                                        final String arg1,
                                        final String arg2,
                                        final String arg3,
                                        final Object arg4 ) {

            return null;
        }

    }

    /**
     * This is required for POJOs that have annotations. 
     * It may also come in handy if we want to allow custom annotations for marking field numbers etc.
     */
    static class ClassFieldAnnotationVisitor
            extends
            AnnotationVisitor {

        ClassFieldAnnotationVisitor() {
            super(Opcodes.ASM5);
        }

        public void visit( final String arg0,
                           final Object arg1 ) {
        }

        public void visitEnum( final String arg0,
                               final String arg1,
                               final String arg2 ) {
        }

        public AnnotationVisitor visitAnnotation( final String arg0,
                                                  final String arg1 ) {
            return new ClassFieldAnnotationVisitor();
        }

        public AnnotationVisitor visitArray( final String arg0 ) {
            return new ClassFieldAnnotationVisitor();
        }

        public void visitEnd() {

        }

    }

    public static class GetterOverloadWarning implements KnowledgeBuilderResult {

        private final Class klass;
        private final String oldName;
        private final Class oldType;
        private final String newName;
        private final Class newType;

        public GetterOverloadWarning( Class klass, String oldName, Class oldType, String newName, Class newType ) {
            this.klass = klass;
            this.oldName = oldName;
            this.oldType = oldType;
            this.newName = newName;
            this.newType = newType;
        }

        public ResultSeverity getSeverity() {
            return ResultSeverity.WARNING;
        }


        public String getMessage() {
            return " Getter overloading detected in class " + klass.getName() + " : " + oldName + " (" + oldType + ") vs " + newName + " (" + newType + ") ";
        }


        public int[] getLines() {
            return new int[ 0 ];
        }

        public Resource getResource() {
            return null;
        }

        @Override
        public InternalMessage asMessage(long id) {
            return new MessageImpl(id, this);
        }

    }

    public static class IncompatibleGetterOverloadError implements KnowledgeBuilderResult {

        private final Class klass;
        private final String oldName;
        private final Class oldType;
        private final String newName;
        private final Class newType;

        public IncompatibleGetterOverloadError( Class klass, String oldName, Class oldType, String newName, Class newType ) {
            this.klass = klass;
            this.oldName = oldName;
            this.oldType = oldType;
            this.newName = newName;
            this.newType = newType;
        }

        public ResultSeverity getSeverity() {
            return ResultSeverity.ERROR;
        }


        public String getMessage() {
            return " Incompatible Getter overloading detected in class " + klass.getName() + " : " + oldName + " (" + oldType + ") vs " + newName + " (" + newType + ") ";
        }


        public int[] getLines() {
            return new int[ 0 ];
        }

        public Resource getResource() {
            return null;
        }

        @Override
        public InternalMessage asMessage(long id) {
            return new MessageImpl(id, this);
        }

    }

}
