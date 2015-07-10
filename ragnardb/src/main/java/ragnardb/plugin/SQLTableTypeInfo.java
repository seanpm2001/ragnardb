package ragnardb.plugin;

import gw.lang.reflect.*;
import gw.lang.reflect.gs.IGosuObject;
import gw.lang.reflect.features.PropertyReference;
import gw.lang.reflect.java.JavaTypes;
import ragnardb.api.ISQLResult;
import ragnardb.runtime.SQLConstraint;
import ragnardb.runtime.SQLMetadata;
import ragnardb.runtime.SQLQuery;
import ragnardb.runtime.SQLRecord;
import ragnardb.utils.NounHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class SQLTableTypeInfo extends SQLBaseTypeInfo {
  private SQLMetadata _md = new SQLMetadata();
  private String _classTableName;
  private IType _domainLogic;

  public SQLTableTypeInfo(ISQLTableType type) {
    super(type);
    resolveProperties(type);
    _classTableName = type.getName();
  }

  private void resolveProperties( ISQLTableType type ) {
    _propertiesList = new ArrayList<>();
    _propertiesMap = new HashMap<>();

    List<ColumnDefinition> columns = type.getColumnDefinitions();
    for(ColumnDefinition column : columns) {
      SQLColumnPropertyInfo prop = new SQLColumnPropertyInfo(column.getColumnName(), column.getPropertyName(),
        getGosuType(column.getSQLType()), this, column.getOffset(), column.getLength());
      _propertiesMap.put(prop.getName(), prop);
      _propertiesList.add( prop );
    }
    _domainLogic = maybeGetDomainLogic();
    createMethodInfos();
    createConstructorInfos();
  }

  @Override
  public int getOffset() {
    return ((ISQLTableType) getOwnersType()).getTable().getOffset();
  }

  @Override
  public int getTextLength() {
    return ((ISQLTableType) getOwnersType()).getTable().getTypeName().length();
  }

  private void createConstructorInfos() {
    List<IConstructorInfo> constructorInfos = new ArrayList<>();

    IConstructorInfo constructorMethod = new ConstructorInfoBuilder()
      .withDescription( "Creates a new Table object" )
      .withParameters()
      .withConstructorHandler((args) -> new SQLRecord(((ISQLTableType) getOwnersType()).getTable().getTableName(),
        "id")).build(this);

    constructorInfos.add( constructorMethod );

    //now add domain logic constructor, if applicable
    if(_domainLogic != null) {
      IConstructorInfo domainLogicConstructorMethod = new ConstructorInfoBuilder()
          .withDescription("")
//          .withAccessibility(IRelativeTypeInfo.Accessibility.PRIVATE)
          .withParameters(new ParameterInfoBuilder()
              .withName(getOwnersType().getRelativeName())
//              .withType(getOwnersType())
              .withType(ISQLResult.class)
              .withDescription("Constructor for domain logic class related to a SQLTableType; to be instantiated reflectively"))
          .build(_domainLogic.getTypeInfo());

      constructorInfos.add(domainLogicConstructorMethod);
    }

    _constructorList = constructorInfos;
  }

  private void createMethodInfos() {
    MethodList methodList = new MethodList();

    for (String propertyName : _propertiesMap.keySet()) {
      SQLColumnPropertyInfo prop = (SQLColumnPropertyInfo) _propertiesMap.get(propertyName);

      methodList.add(generateFindByMethod(prop));
      methodList.add(generateFindByAllMethod(prop));
    }

    methodList.add(generateCreateMethod());
    methodList.add(generateInitMethod());
    methodList.add(generateWhereMethod());
    methodList.add(generateSelectMethod());
    methodList.add(generateGetNameMethod());

    List<? extends IMethodInfo> domainMethods = maybeGetDomainMethods();
    List<? extends IPropertyInfo> domainProperties = maybeGetDomainProperties();

    methodList.addAll(domainMethods);

    _methodList = methodList;

    for(IPropertyInfo domainProperty : domainProperties) {
      _propertiesMap.put(domainProperty.getName(), domainProperty);
      _propertiesList.add(domainProperty);
    }

  }

  private IMethodInfo generateFindByMethod(IPropertyInfo prop) {
    final String propertyName = prop.getName();
    return new MethodInfoBuilder()
        .withName( "findBy" + propertyName )
        .withDescription("Find single match based on the value of the " + propertyName + " column.")
        .withParameters(new ParameterInfoBuilder()
            .withName(propertyName)
            .withType(prop.getFeatureType())
            .withDescription("Performs strict matching on this argument"))
        .withReturnType(this.getOwnersType())
        .withStatic(true)
        .withCallHandler(( ctx, args ) -> {
          SQLQuery query = new SQLQuery(_md, getOwnersType());
          SQLConstraint constraint = SQLConstraint.isComparator(prop, args[0],"=");
          query = query.where(constraint);
          return query.iterator().hasNext() ? query.iterator().next() : null;
        })
        .build(this);
  }

  private IMethodInfo generateFindByAllMethod(IPropertyInfo prop) {
    final String propertyName = prop.getName();
    return new MethodInfoBuilder()
        .withName("findAllBy" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1))
        .withDescription("Find all matches based on the value of the " + propertyName + " column.")
        .withParameters(new ParameterInfoBuilder()
            .withName(propertyName)
            .withType(prop.getFeatureType())
            .withDescription("Performs strict matching on this argument"))
        .withReturnType(JavaTypes.ITERABLE().getParameterizedType(this.getOwnersType()))
        .withStatic(true)
        .withCallHandler(( ctx, args ) -> {
          SQLQuery query = new SQLQuery(_md, getOwnersType());
          SQLConstraint constraint = SQLConstraint.isComparator(prop, args[0], "=");
          query = query.where(constraint);
          return query;
        })
        .build(this);
  }

  private IMethodInfo generateCreateMethod() {
    return new MethodInfoBuilder()
        .withName("create")
        .withDescription("Creates a new table entry")
        .withParameters()
        .withReturnType(this.getOwnersType())
        .withCallHandler(( ctx, args ) -> ((SQLRecord) ctx).create())
        .build(this);
  }

  private IMethodInfo generateInitMethod() {
    return new MethodInfoBuilder()
        .withName("init")
        .withDescription("Creates a new table entry")
        .withParameters()
        .withReturnType(this.getOwnersType())
        .withStatic(true)
        .withCallHandler(( ctx, args ) -> new SQLRecord(((ISQLTableType) getOwnersType()).getTable().getTableName(), "id"))
        .build(this);
  }

  private IMethodInfo generateWhereMethod() {
    return new MethodInfoBuilder()
        .withName("where")
        .withDescription("Creates a new table query")
        .withParameters(new ParameterInfoBuilder().withName("condition").withType(TypeSystem.get(SQLConstraint.class)))
        .withReturnType(JavaTypes.ITERABLE().getParameterizedType(this.getOwnersType()))
        .withStatic(true)
        .withCallHandler(( ctx, args ) -> new SQLQuery<SQLRecord>(_md, this.getOwnersType()).where((SQLConstraint) args[0]))
        .build(this);
  }

  private IMethodInfo generateSelectMethod() {
    return new MethodInfoBuilder()
        .withName("select")
        .withDescription("Creates a new table query")
        .withParameters()
        .withReturnType(JavaTypes.getGosuType(SQLQuery.class).getParameterizedType(this.getOwnersType()))
        .withStatic(true)
        .withCallHandler((ctx, args) -> new SQLQuery<SQLRecord>(_md, this.getOwnersType()))
        .build(this);
  }

  private IMethodInfo generateSingleObjectSelectMethod() {
    return new MethodInfoBuilder()
      .withName("select")
      .withDescription("Creates a new table query")
      .withParameters(new ParameterInfoBuilder().withName("Column").withType(TypeSystem.get(PropertyReference.class)))
        .withReturnType(JavaTypes.getGosuType(SQLQuery.class).getParameterizedType(this.getOwnersType()))
        .withStatic(true)
        .withCallHandler((ctx, args) -> new SQLQuery<SQLRecord>(_md, this.getOwnersType()))
        .build(this);
  }

  private IMethodInfo generateJoinMethod() {
    return new MethodInfoBuilder()
      .withName("join")
      .withDescription("Creates a new table query")
      .withParameters()
      .withReturnType(JavaTypes.getGosuType(SQLQuery.class).getParameterizedType(this.getOwnersType()))
      .withStatic(true)
      .withCallHandler((ctx, args) -> new SQLQuery<SQLRecord>(_md, this.getOwnersType()))
      .build(this);
  }

  private IMethodInfo generateGetNameMethod() {
    return new MethodInfoBuilder()
        .withName("getName")
        .withDescription("Returns Table Name")
        .withParameters()
        .withReturnType(JavaTypes.STRING())
        .withStatic(true)
        .withCallHandler((ctx, args) -> _classTableName)
        .build(this);
  }

  private IType maybeGetDomainLogic() {
    ISQLTableType tableType = (ISQLTableType) getOwnersType();
    ISQLDdlType ddlType = (ISQLDdlType) tableType.getEnclosingType();
    final String singularizedDdlType = new NounHandler(ddlType.getRelativeName()).getSingular();
    final String domainLogicPackageSuffix = "Extensions.";
    final String domainLogicTableSuffix = "Ext";
    final String domainLogicFqn = ddlType.getNamespace() + '.' +
        singularizedDdlType + domainLogicPackageSuffix + tableType.getRelativeName() + domainLogicTableSuffix;

    return TypeSystem.getByFullNameIfValid(domainLogicFqn);
  }

  private List<? extends IMethodInfo> maybeGetDomainMethods() {
    List<IMethodInfo> methodList = Collections.emptyList();

    final IType domainLogic = _domainLogic;

    if (domainLogic != null) {
      methodList = new ArrayList<>();
      final IRelativeTypeInfo domainLogicTypeInfo = (IRelativeTypeInfo) domainLogic.getTypeInfo();
      List<? extends IMethodInfo> domainMethods = domainLogicTypeInfo.getDeclaredMethods()
          .stream()
          .filter(IAttributedFeatureInfo::isPublic)
          .filter(method -> !method.getName().startsWith("@"))
          .collect(Collectors.toList());

      for (IMethodInfo method : domainMethods) {
        final IParameterInfo[] params = method.getParameters();
        ParameterInfoBuilder[] paramInfos = new ParameterInfoBuilder[params.length];
        for(int i = 0; i < params.length; i++) {
          IParameterInfo param = params[i];
          paramInfos[i] = new ParameterInfoBuilder().like(param);
        }
        IMethodInfo syntheticMethod = new MethodInfoBuilder().like(method)
            .withCallHandler((ctx, args) -> {
              //instantiate the domain logic class reflectively, passing this TypeInfo's owner's instance (!) as constructor arg
              IGosuObject domainLogicObject = ReflectUtil.constructGosuClassInstance(domainLogic.getName(),  ctx.getClass().cast(getOwnersType())); //ctx is a SQLRecord at runtime
              //reflectively call and return the return value of the... method we are currently generating??! Face. Melted.
              return ReflectUtil.invokeMethod(domainLogicObject, method.getName(), args);
            })
            .build(this);

        methodList.add(syntheticMethod);
      }
    }
    return methodList;
  }

  private List<? extends IPropertyInfo> maybeGetDomainProperties() {
    List<IPropertyInfo> propertyList = Collections.emptyList();

    final IType domainLogic = _domainLogic;

    if (domainLogic != null) {
      propertyList = new ArrayList<>();
      final IRelativeTypeInfo domainLogicTypeInfo = (IRelativeTypeInfo) domainLogic.getTypeInfo();
      List<? extends IPropertyInfo> domainProperties = domainLogicTypeInfo.getDeclaredProperties()
          .stream()
          .filter(IAttributedFeatureInfo::isPublic)
          .collect(Collectors.toList());

      for (IPropertyInfo prop : domainProperties) {
        IPropertyInfo syntheticProperty = new PropertyInfoBuilder().like(prop)
//            .withName(prop.getName())
//            .withDescription(prop.getDescription())
//            .withStatic(prop.isStatic())
//            .withWritable(prop.isWritable())
//            .withType(prop.getFeatureType())
//            .withAccessor(prop.getAccessor()) //TODO needs some kind of special accessor in order to invoke a property defined on another class
            .build(this);

        propertyList.add(syntheticProperty);
      }
    }
    return propertyList;
  }

}
