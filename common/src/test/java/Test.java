import java.lang.reflect.Type;      
import java.lang.reflect.ParameterizedType;
import java.util.List;

public class Test<T>{
    private List<String> list;
  
    public static void testA() throws NoSuchFieldException {
        Type t = Test.class.getDeclaredField("list").getGenericType();  
        if (ParameterizedType.class.isAssignableFrom(t.getClass())) {              
            for (Type t1:((ParameterizedType)t).getActualTypeArguments()) {          
                System.out.print(t1 + ",");          
            }          
            System.out.println();          
        }   
   }  
   public static void main(String args[]) throws Exception{
            testA();
            System.out.println("======getSuperclass======:");      
            System.out.println(Test.class.getSuperclass().getName());     
            System.out.println("======getGenericSuperclass======:");     
            Type t = Test.class.getGenericSuperclass();      
            System.out.println(t);       
            if (ParameterizedType.class.isAssignableFrom(t.getClass())) {    
                     System.out.print("----------->getActualTypeArguments:");       
                     for (Type t1:((ParameterizedType)t).getActualTypeArguments()) {       
                                    System.out.print(t1 + ",");       
                      }       
                     System.out.println();       
            }    
   }     
  
  }    