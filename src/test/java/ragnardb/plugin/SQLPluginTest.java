package ragnardb.plugin;

import gw.lang.Gosu;
import gw.lang.reflect.IPropertyInfo;
import gw.lang.reflect.IType;
import gw.lang.reflect.ITypeLoader;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.java.IJavaType;
import gw.lang.reflect.java.JavaTypes;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class SQLPluginTest {

  @Before
  public void beforeMethod() {
    Gosu.init();
  }

  @Test
  public void getTypeExplicitly() {
    ITypeLoader sqlPlugin = new SQLPlugin(TypeSystem.getGlobalModule());
    TypeSystem.pushTypeLoader(TypeSystem.getGlobalModule(), sqlPlugin);
    IType result = sqlPlugin.getType("ragnardb.foo.Users.Contacts");
    assertNotNull(result);
    assertEquals("ragnardb.foo.Users.Contacts", result.getName());
  }

  @Test
  public void getNonExistantType() {
    ITypeLoader sqlPlugin = new SQLPlugin(TypeSystem.getGlobalModule());
    TypeSystem.pushTypeLoader(TypeSystem.getGlobalModule(), sqlPlugin);
    IType result = sqlPlugin.getType("ragnardb.foo.Unknown.DoesNotExist");
    assertNull(result);
  }

  @Test
  public void oneSourceWithMultipleTypes() {
    ITypeLoader sqlPlugin = new SQLPlugin(TypeSystem.getGlobalModule());
    TypeSystem.pushTypeLoader(TypeSystem.getGlobalModule(), sqlPlugin);

    IType result = sqlPlugin.getType("ragnardb.foo.Vehicles.Cars");
    assertNotNull(result);
    assertEquals("ragnardb.foo.Vehicles.Cars", result.getName());

    result = sqlPlugin.getType("ragnardb.foo.Vehicles.Motorcycles");
    assertNotNull(result);
    assertEquals("ragnardb.foo.Vehicles.Motorcycles", result.getName());
  }

  @Test
  public void getColumnDefs() {
    ITypeLoader sqlPlugin = new SQLPlugin(TypeSystem.getGlobalModule());
    TypeSystem.pushTypeLoader(TypeSystem.getGlobalModule(), sqlPlugin);
    ISQLType result = (ISQLType) sqlPlugin.getType("ragnardb.foo.Users.Contacts");
    assertNotNull(result);

    List<ColumnDefinition> colDefs = result.getColumnDefinitions();
    assertNotNull(colDefs);

    Set<String> expectedColumnNames = Stream.of("UserId", "LastName", "FirstName", "Age").collect(Collectors.toSet());
    Set<String> actualColumnNames = colDefs.stream().map(ColumnDefinition::getColumnName).collect(Collectors.toSet());

    assertEquals(expectedColumnNames, actualColumnNames);
  }

  @Test
  public void getTypeInfo() {
    ITypeLoader sqlPlugin = new SQLPlugin(TypeSystem.getGlobalModule());
    TypeSystem.pushTypeLoader(TypeSystem.getGlobalModule(), sqlPlugin);
    ISQLType result = (ISQLType) sqlPlugin.getType("ragnardb.foo.Users.Contacts");
    assertNotNull(result);
    assertEquals("ragnardb.foo.Users.Contacts", result.getName());
    assertEquals("ragnardb.foo.Users", result.getNamespace());
    assertEquals("Contacts", result.getRelativeName());

    SQLTypeInfo ti = (SQLTypeInfo) result.getTypeInfo();
    assertEquals("Contacts", ti.getName());

    //make a set of expected Name/IJavaType pairs
    Set<String> expectedPropertyNames = Stream.of("UserId", "LastName", "FirstName", "Age").collect(Collectors.toSet());
    Map<String, IJavaType> expectedPropertyNameAndType = new HashMap<>(expectedPropertyNames.size());

    expectedPropertyNameAndType.put("UserId", JavaTypes.pINT());
    expectedPropertyNameAndType.put("LastName", JavaTypes.STRING());
    expectedPropertyNameAndType.put("FirstName", JavaTypes.STRING());
    expectedPropertyNameAndType.put("Age", JavaTypes.pINT());

    //number of properties is what we expect
    assertEquals(expectedPropertyNameAndType.size(), ti.getProperties().size());

    //each property name has a match in the map, and the type is identical
    for(IPropertyInfo actualProp : ti.getProperties()) {
      IJavaType expectedType = expectedPropertyNameAndType.get(actualProp.getName());
      assertNotNull("expectedType was null, meaning the actualProp's name was not found in the map", expectedType);
      assertSame(expectedType, actualProp.getFeatureType());
    }
  }

}
